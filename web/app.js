import { ALLSPECS, EXTERNAL, computeScore } from '../packages/engine/index.js';
import { CONVERTERS, convert } from '../packages/engine/golden-part1.js';
// Hand-verified spec id -> registry entry id (1:1 coverage for all 30 specs).
const SPEC_REG = {
  'anion-gap': 'MSC-0002', 'corrected-calcium': 'MSC-0004', 'osmolality': 'MSC-0137',
  'meld': 'MSC-0047', 'meld-na': 'MSC-0048', 'child-pugh': 'MSC-0043',
  'qtc': 'MSC-0024', 'cha2ds2-vasc': 'MSC-0015', 'atria': 'MSC-0139',
  'hatch': 'MSC-0274', 'wells-dvt': 'MSC-0134', 'padua': 'MSC-0133',
  'improve': 'MSC-0272', 'pesi': 'MSC-E107', 'spesi': 'MSC-0252',
  'geneva': 'MSC-0115', 'perc': 'MSC-0113', 'four-at': 'MSC-0152',
  'feverpain': 'MSC-0163', 'smart-cop': 'MSC-0251', 'ariscat': 'MSC-0257',
  'mpi': 'MSC-0259', 'goldman': 'MSC-0295', 'detsky': 'MSC-0295',
  'ripasa': 'MSC-0165', 'lods': 'MSC-0279', 'abcd2': 'MSC-0186',
  'ipssr': 'MSC-0075', 'mmse': 'MSC-0095', 'moca': 'MSC-0096' };
// Shared catalog row hosts two specs (PESI/sPESI share MSC-E107/PESI row 252
// family; Goldman/Detsky share MSC-0295) - both render from the same card.
const app = document.querySelector('#app');
const state = { screen: 'home', query: '', tools: [],
  saved: (() => { try { const v = JSON.parse(localStorage.getItem('clinicalc-web-saved') || '[]');
    return Array.isArray(v) ? v : []; } catch { return []; } })() };
async function load() {
  try {
    const d = await (await fetch('../data/registry.json')).json();
    const byId = Object.fromEntries(d.entries.map(e => [e.id, e]));
    const seen = new Set();
    state.tools = [];
    // One card per hand-verified spec, keyed by registry id.
    for (const s of ALLSPECS) {
      const rid = SPEC_REG[s.id];
      const e = byId[rid];
      seen.add(rid + '|' + s.id);
      state.tools.push({ name: s.name, regId: rid, category: e?.category || s.category,
        summary: s.purpose, ready: s.engine !== 'reference', spec: s });
    }
    // Every other registry entry renders as a reference card.
    for (const e of d.entries)
      if (!Object.values(SPEC_REG).includes(e.id))
        state.tools.push({ name: e.name, regId: e.id,
          category: e.category || 'Clinical tools',
          summary: e.purpose || e.formula_human || 'Clinical scoring system',
          ready: false, spec: null });
  } catch (e) {
    state.tools = ALLSPECS.map(s => ({ name: s.name, regId: 'spec:' + s.id,
      category: s.category, summary: s.purpose,
      ready: s.engine !== 'reference', spec: s }));
  }
  for (const x of EXTERNAL) state.tools.push({ name: x.name, category: x.category, summary: x.note, ready: false, external: x });
  render();
}
function esc(v) { return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function card(t) { return `<article class="tool" data-name="${esc(t.name)}" tabindex="0" role="button" aria-label="${esc(t.name)}${t.ready ? ', calculator' : ', reference'}"><div class="tool-icon">${t.ready ? 'OK' : 'i'}</div><h3>${esc(t.name)}</h3><p>${esc(t.summary)}</p><div class="tool-foot"><span>${esc(t.category)}</span><span class="tag ${t.ready ? '' : 'reference'}">${t.ready ? 'Calculator' : 'Reference'}</span></div></article>`; }
function home() {
  const list = state.tools.filter(t => t.ready).slice(0, 8);
  return `<section class="hero"><div><div class="eyebrow">Clinical decision support</div><h1>Clarity when the clinical moment is moving fast.</h1><p>A calm, traceable home for bedside scores, risk tools, and essential calculations.</p></div><label class="search"><span><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg></span><input id="search" placeholder="Search ${state.tools.length || '300+'} tools" value="${esc(state.query)}"></label></section><section class="bento"><article class="main"><div class="meta">QUICK CALC</div><h2>Start with a score</h2><p>Search the catalog or open a verified calculator.</p><div class="orb"></div></article><article class="peach"><div class="meta">CATALOG</div><h2>${state.tools.length || '300+'} tools indexed</h2><p>Verified calculators plus reference cards.</p></article><article class="gold"><div class="meta">SAFETY</div><h2>Traceable by design</h2><p>Formula, citation, and reference status stay visible.</p></article></section><div class="section-head"><h2>Bedside essentials</h2><a data-screen="browse">Browse all</a></div><section class="grid">${list.map(card).join('')}</section>`;
}
function browse() {
  const shown = state.tools.filter(t => `${t.name} ${t.category} ${t.summary}`.toLowerCase().includes(state.query.toLowerCase()));
  return `<section class="page-head"><div><div class="eyebrow">The catalog</div><h1>Find your score.</h1></div><label class="search"><span><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg></span><input id="search" placeholder="Search by name" value="${esc(state.query)}"></label></section><p style="color:var(--muted);font-size:12px">${shown.length} of ${state.tools.length} indexed tools</p><section class="grid">${shown.length ? shown.map(card).join('') : '<div class="empty"><h2>No matches</h2></div>'}</section>`;
}
function saved() {
  const list = state.tools.filter(t => state.saved.includes(t.name));
  return `<section class="page-head"><div><div class="eyebrow">Personal workspace</div><h1>Saved tools.</h1></div></section>${list.length ? `<section class="grid">${list.map(card).join('')}</section>` : '<div class="empty"><h2>Nothing here yet</h2><p>Open a tool and save it for quick access.</p></div>'}`;
}
function fieldHtml(inp) {
  if (inp.type === 'boolean') return `<label class="check"><input type="checkbox" data-key="${inp.key}"> ${esc(inp.label)} (+${inp.points})</label>`;
  if (inp.type === 'select') return `<label>${esc(inp.label)}<select data-key="${inp.key}">${inp.options.map(o => `<option value="${esc(o.value)}">${esc(o.label)} (+${o.points})</option>`).join('')}</select></label>`;
  return `<label>${esc(inp.label)}${inp.unit ? ` (${esc(inp.unit)})` : ''}<input type="number" step="any" data-key="${inp.key}" placeholder="Enter value"></label>`;
}
function detail(t) {
  const s = (t.spec && t.ready) ? t.spec : null, ext = t.external;
  const body = s ? `<div class="fields">${s.inputs.map(fieldHtml).join('')}</div><div class="result"><label>Live result</label><strong id="result">-</strong><small>Verify against current local protocol.</small></div><div class="converters"><h3>Unit converter</h3><label>Value <input type="number" id="conv-v" step="any"></label><label>Unit <select id="conv-u">${CONVERTERS.map((c, i) => `<option value="${i}">${esc(c.label)} (${esc(c.from)} to ${esc(c.to)})</option>`).join('')}</select></label><strong id="conv-r"></strong></div>`
    : `<div class="result"><label>${ext ? 'External-only' : 'Reference only'}</label><strong>${ext ? esc(ext.name) : 'No computed output'}</strong><small>${esc(ext ? ext.note : 'Published coefficients still need review.')}</small></div>${ext && ext.link ? `<p><a href="${ext.link}" target="_blank" rel="noopener">Open external calculator</a></p>` : ''}`;
  return `<section class="detail"><article class="panel"><button class="back" data-screen="browse">Back to catalog</button><div class="eyebrow" style="margin-top:25px">${esc(t.category)}</div><h1>${esc(t.name)}</h1><p>${esc(t.summary)}</p><button id="save">${state.saved.includes(t.name) ? 'Saved' : 'Save'}</button>${body}</article><aside class="panel side"><div><h3>Formula</h3><code>${esc(s ? s.formula : 'Published scoring table or external model required.')}</code></div><div><h3>Source status</h3><p>${s ? esc(s.citation || 'Catalog formula') : 'Reference heading. Computation blocked until reviewed.'}</p></div><div><h3>Clinical disclaimer</h3><p>Decision support for qualified professionals. Verify every result against current guidance.</p></div></aside></section>`;
}
function render() {
  const active = state.tools.find(t => t.name === state.screen);
  app.innerHTML = active ? detail(active) : state.screen === 'browse' ? browse() : state.screen === 'saved' ? saved() : home();
  bind();
}
function readVals(s) {
  const v = {};
  for (const inp of s.inputs) {
    const el = document.querySelector(`[data-key="${inp.key}"]`);
    if (!el) continue;
    if (inp.type === 'boolean') v[inp.key] = el.checked;
    else if (inp.type === 'select') v[inp.key] = el.value;
    else v[inp.key] = el.value === '' ? NaN : Number(el.value);
  }
  return v;
}
function fmt(r) {
  if (r == null || Number.isNaN(r)) return '-';
  if (typeof r === 'string') return r;
  if (typeof r === 'object') return Object.entries(r).map(([k, val]) => `${k} ${Math.round(val)}`).join(' | ');
  return Math.round(r * 100) / 100;
}
function bind() {
  document.querySelectorAll('[data-screen]').forEach(x => x.onclick = () => { state.screen = x.dataset.screen; render(); });
  const input = document.querySelector('#search');
  if (input) input.oninput = e => { state.query = e.target.value; state.screen = 'browse';
    render(true);
    const n = document.querySelector('#search');
    if (n) { n.focus(); n.setSelectionRange(n.value.length, n.value.length); } };
  const open = (x) => { state.screen = x.dataset.name; render(); };
  document.querySelectorAll('.tool').forEach(x => { x.onclick = () => open(x);
    x.onkeydown = e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); open(x); } }; });
  const sv = document.querySelector('#save');
  if (sv) sv.onclick = () => { const n = state.screen; state.saved = state.saved.includes(n) ? state.saved.filter(x => x !== n) : [...state.saved, n]; localStorage.setItem('clinicalc-web-saved', JSON.stringify(state.saved)); render(); };
  const t = state.tools.find(x => x.name === state.screen);
  if (t && t.spec && t.ready) {
    const upd = () => { try { document.querySelector('#result').textContent = fmt(computeScore(t.spec, readVals(t.spec))); } catch (e) { /* empty */ } };
    document.querySelectorAll('[data-key]').forEach(el => { el.oninput = upd; el.onchange = upd; });
    upd();
    const cv = document.querySelector('#conv-v'), cu = document.querySelector('#conv-u');
    if (cv) { const c = () => { const s = CONVERTERS[Number(cu.value)];
        document.querySelector('#conv-r').textContent = cv.value === '' ? '-' : fmt(convert(Number(cv.value), s)) + ' ' + s.to; };
      cv.oninput = c; cu.onchange = c; }
  }
}
document.querySelector('#theme').onclick = () => document.body.classList.toggle('dark');
load();
