import { sumPoints } from './golden-part1.js';
export function computeScore(spec, v) {
  switch (spec.compute) {
    case 'anion-gap': { const ag = v.na - (v.cl + v.hco3);
      return v.albumin ? ag + 2.5 * (4 - v.albumin) : ag; }
    case 'corrected-calcium': return v.ca + 0.8 * (4 - v.albumin);
    case 'osmolality': return 2 * v.na + v.glucose / 18 + v.bun / 2.8;
    case 'meld': { let b = Math.max(v.bili, 1), i = Math.max(v.inr, 1);
      let c = Math.min(Math.max(v.cr, 1), 4); if (v.dialysis) c = 4;
      return Math.min(40, Math.max(6,
        Math.round(3.78 * Math.log(b) + 11.2 * Math.log(i) + 9.57 * Math.log(c) + 6.43))); }
    case 'meld-na': { const na = Math.min(137, Math.max(125, v.na));
      return Math.min(40, Math.max(6,
        Math.round(v.meld + 1.32 * (137 - na) - 0.033 * v.meld * (137 - na)))); }
    case 'qtc': { const rr = 60 / v.hr;
      return { bazett: v.qt / Math.sqrt(rr), fridericia: v.qt / Math.cbrt(rr),
        hodges: v.qt + 1.75 * (v.hr - 60), framingham: v.qt + 0.154 * (1 - rr) * 1000 }; }
    case 'perc': { const keys = ['age','hr','spo2','hem','est','prev','leg','surg'];
      return keys.every(k => !v[k]) ? 'PERC-negative' : 'PERC-positive'; }
    case 'raw-score': return v.score;
    case 'moca': return Math.min(30, v.raw + (v.lowedu ? 1 : 0));
    default: return sumPoints(v, spec.inputs);
  }
}
