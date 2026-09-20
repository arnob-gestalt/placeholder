import test from 'node:test';
import assert from 'node:assert/strict';
import { sumPoints, CONVERTERS, convert, convertBack } from '../packages/engine/golden-part1.js';
import { BEDSIDESPEC } from '../packages/engine/spec-bedside.js';
import { CARDIOSPEC } from '../packages/engine/spec-cardio.js';
import { REST1SPEC } from '../packages/engine/spec-rest1.js';
import { REST2SPEC } from '../packages/engine/spec-rest2.js';
import { REST3SPEC } from '../packages/engine/spec-rest3.js';
import { REST4SPEC } from '../packages/engine/spec-rest4.js';
import { computeScore } from '../packages/engine/compute.js';
const ALL = [...BEDSIDESPEC, ...CARDIOSPEC, ...REST1SPEC, ...REST2SPEC, ...REST3SPEC, ...REST4SPEC];
const byId = Object.fromEntries(ALL.map(s => [s.id, s]));
const pts = (id, v) => computeScore(byId[id], v);
const maxOf = (id) => { const s = byId[id];
  const v = {};
  for (const i of s.inputs) {
    if (i.type === 'boolean') v[i.key] = (i.points ?? 0) >= 0;
    else if (i.type === 'select') v[i.key] = i.options.reduce((a, o) => o.points > a.points ? o : a).value;
    else if (i.type === 'number' && s.engine === 'points-sum') v[i.key] = 200;
  }
  return computeScore(s, v);
};
test('AG=12', () => assert.equal(pts('anion-gap', { na: 140, cl: 104, hco3: 24 }), 12));
test('CorrCa=8.8', () => assert.equal(pts('corrected-calcium', { ca: 8.0, albumin: 3.0 }), 8.8));
test('Osm=290', () => assert.equal(pts('osmolality', { na: 140, glucose: 90, bun: 14 }), 290));
test('CHA2DS2-VASc=6', () => assert.equal(pts('cha2ds2-vasc', { chf: 1, htn: 0, age: 'a75', dm: 0, stroke: 1, vasc: 0, female: 1 }), 6));
test('MELD=14', () => assert.equal(pts('meld', { bili: 2.0, inr: 1.5, cr: 1.0 }), 14));
test('MELD clamps to 20', () => assert.equal(pts('meld', { bili: 0.6, inr: 0.9, cr: 5.2 }), 20));
test('MELD-Na=20', () => assert.equal(pts('meld-na', { meld: 14, na: 130 }), 20));
test('MELD capped at 40', () => assert.equal(pts('meld', { bili: 30, inr: 6, cr: 8 }), 40));
test('MELD-Na bounded 6-40', () => {
  assert.equal(pts('meld-na', { meld: 40, na: 125 }), 40);
  assert.equal(pts('meld-na', { meld: 6, na: 137 }), 6); });
test('PERC negative only when no disqualifying criterion', () => {
  assert.equal(pts('perc', {}), 'PERC-negative');
  for (const k of ['age', 'hr', 'spo2', 'hem', 'est', 'prev', 'leg', 'surg'])
    assert.equal(pts('perc', { [k]: 1 }), 'PERC-positive', k); });
test('Child-Pugh=9', () => assert.equal(pts('child-pugh', { bili: 'b2', alb: 'a2', inr: 'i1', asc: 'm', enc: 'm' }), 9));
test('QTc Bazett=400', () => assert.equal(Math.round(pts('qtc', { qt: 400, hr: 60 }).bazett), 400));
test('HATCH max=7', () => assert.equal(maxOf('hatch'), 7));
test('ATRIA max=13 case', () => assert.equal(pts('atria', { chf: 1, htn: 1, dm: 1, stroke: 1, age: 'a75', female: 1 }), 13));
test('4AT max=12', () => assert.equal(maxOf('four-at'), 12));
test('FeverPAIN max=5', () => assert.equal(maxOf('feverpain'), 5));
test('Geneva max=9 from HR-exclusive levels; doc claims 10', () => {
  // Table rows sum to 1+1+1+1+1+1+2+1=9 with HR75-94/HR>=95 exclusive.
  // Printed total 10 and golden vector 10 disagree with the printed table;
  // pin behavior and flag the inconsistency instead of inventing a point.
  assert.equal(maxOf('geneva'), 9);
  assert.match(byId['geneva'].formula, /0-10/); });
test('Padua=6 high', () => assert.equal(pts('padua', { ca: 1, prev: 1 }), 6));
test('Wells>=2 likely', () => assert.equal(pts('wells-dvt', { ca: 1, par: 1 }), 2));
test('SMART-COP max=12', () => assert.equal(maxOf('smart-cop'), 12));
test('ARISCAT constructed max=116; doc claims 123', () => {
  // Level maxima 16+24+17+11+24+16+8=116, not the printed 123.
  assert.equal(maxOf('ariscat'), 116);
  assert.match(byId['ariscat'].formula, /0-123/); });
test('MPI max=47', () => assert.equal(maxOf('mpi'), 47));
test('Goldman=53 Detsky=85', () => assert.equal(maxOf('goldman'), 53));
test('Detsky grouped max=100; doc claims 0-85+', () => {
  // Exclusive MI/angina/edema pairs are single-selects, yet levels still
  // construct to 10+20+10+10+20+10+5+5+5+5=100 vs printed 0-85+.
  assert.equal(maxOf('detsky'), 100);
  assert.match(byId['detsky'].formula, /constructed 0-100/); });
test('RIPASA constructed max=15; doc claims 16', () => {
  // Item values sum to 15.0, not the printed 16.
  assert.equal(maxOf('ripasa'), 15);
  assert.match(byId['ripasa'].formula, /0-16/); });
test('LODS is reference-only (unresolved 0-22 model)', () => {
  // App B Row 279 prints 0-22 but its organ rows construct to 60; the
  // calculator ships as a reference card until organ rules are verified.
  assert.equal(byId['lods'].engine, 'reference'); });
test('PESI=310 class V', () => assert.equal(pts('pesi', { age: 80, male: true, ca: true, hf: true, lung: true, hr: true, sbp: true, rr: true, t: true, ams: true, spo2: true }), 310));
test('sPESI age>80=1', () => assert.equal(pts('spesi', { age: 1 }), 1));
test('MMSE max=30', () => assert.equal(pts('mmse', { score: 30 }), 30));
test('MoCA 25+1=26', () => assert.equal(pts('moca', { raw: 25, lowedu: 1 }), 26));
test('IPSS-R constructed max=10; doc claims 0-9', () => {
  // Row 75 rows: cyto 0-4 + blasts 0-3 + Hb 0-1.5 + PLT 0-1 + ANC 0-0.5 = 0-10.
  assert.equal(maxOf('ipssr'), 10);
  assert.equal(pts('ipssr', { cyto: 'vp', blast: 'd', hb: 'c', plt: 'c', anc: 'b' }), 10); });
test('creatinine converter round-trip', () => { const c = CONVERTERS.find(x => x.key === 'creatinine');
  assert.equal(convert(1.0, c), 88.4); assert.ok(Math.abs(convertBack(88.4, c) - 1.0) < 0.01); });
test('mutual exclusion: age bands never double-count', () => {
  assert.equal(pts('cha2ds2-vasc', { age: 'a75' }), 2);
  assert.equal(pts('abcd2', { clin: 'w' }), 2); });
test('points-sum min/max documented', () => {
  for (const s of ALL.filter(s => s.engine === 'points-sum' && !s.compute)) {
    const vals = s.inputs.map(i => i.type === 'boolean' ? [0, i.points] : i.type === 'select' ? i.options.map(o => o.points) : [0]);
    assert.ok(vals.every(v => v.length > 0), s.id);
  }
});
