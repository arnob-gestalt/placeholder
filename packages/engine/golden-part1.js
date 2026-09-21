// Hand-verified calculator specs (Appendix B corrections + Tier-1 equations).
export const CONVERTERS = [
  { key: 'creatinine', label: 'Creatinine', from: 'mg/dL', to: 'µmol/L', factor: 88.4 },
  { key: 'glucose', label: 'Glucose', from: 'mg/dL', to: 'mmol/L', factor: 0.0555 },
  { key: 'urea-bun', label: 'BUN to urea', from: 'BUN mg/dL', to: 'urea mmol/L', factor: 0.357 },
  { key: 'cholesterol', label: 'Cholesterol (total/HDL/LDL)', from: 'mg/dL', to: 'mmol/L', factor: 0.0259 },
  { key: 'triglycerides', label: 'Triglycerides', from: 'mg/dL', to: 'mmol/L', factor: 0.0113 },
  { key: 'bilirubin', label: 'Bilirubin', from: 'mg/dL', to: 'µmol/L', factor: 17.1 },
  { key: 'albumin', label: 'Albumin / protein', from: 'g/dL', to: 'g/L', factor: 10 },
  { key: 'calcium', label: 'Calcium', from: 'mg/dL', to: 'mmol/L', factor: 0.25 },
  { key: 'hemoglobin', label: 'Hemoglobin', from: 'g/dL', to: 'mmol/L', factor: 0.6206 },
  { key: 'lactate', label: 'Lactate', from: 'mg/dL', to: 'mmol/L', factor: 0.111 },
  { key: 'crp', label: 'CRP', from: 'mg/dL', to: 'mg/L', factor: 10 },
  { key: 'pressure', label: 'Pressure (BP, PaO2)', from: 'mmHg', to: 'kPa', factor: 0.133322 },
  { key: 'weight', label: 'Weight', from: 'kg', to: 'lb', factor: 2.20462 },
  { key: 'length', label: 'Length', from: 'cm', to: 'in', factor: 1/2.54 },
  { key: 'volume', label: 'Volume', from: 'mL', to: 'L', factor: 1/1000 },
  { key: 'temp', label: 'Temperature', from: 'C', to: 'F', f: 'temp' },
  { key: 'fio2', label: 'FiO2', from: 'fraction', to: '%', factor: 100 },
];
export function convert(value, spec) {
  if (spec.f) return value * 9/5 + 32;
  return value * spec.factor;
}
export function convertBack(value, spec) {
  if (spec.f) return (value - 32) * 5/9;
  return value / spec.factor;
}
export function sumPoints(values, inputs) {
  const groups = {}; let total = 0;
  for (const inp of inputs) {
    const v = values[inp.key]; let pts = 0;
    if (inp.type === 'boolean') pts = v ? inp.points : 0;
    else if (inp.type === 'select' && v != null && v !== '') {
      const opt = inp.options.find(o => String(o.value) === String(v));
      pts = opt ? opt.points : 0;
    }
    else if (inp.type === 'number' && Number.isFinite(Number(v))) pts = Number(v);
    if (inp.group) groups[inp.group] = Math.max(groups[inp.group] || 0, pts);
    else total += pts;
  }
  for (const g of Object.values(groups)) total += g;
  return total;
}
function sel(key, label, options, group) {
  return { key, label, type: 'select', options: options.map(o => ({ value: o[0], label: o[1], points: o[2] })), group };
}
function yn(key, label, points) { return { key, label, type: 'boolean', points }; }
function num(key, label, unit, extra) { return Object.assign({ key, label, type: 'number', unit }, extra || {}); }
export { sel, yn, num };
