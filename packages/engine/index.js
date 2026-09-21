export { CONVERTERS, convert, convertBack, sumPoints } from './golden-part1.js';
export { BEDSIDESPEC } from './spec-bedside.js';
export { CARDIOSPEC } from './spec-cardio.js';
export { REST1SPEC } from './spec-rest1.js';
export { REST2SPEC } from './spec-rest2.js';
export { REST3SPEC } from './spec-rest3.js';
export { REST4SPEC } from './spec-rest4.js';
export { computeScore } from './compute.js';
import { BEDSIDESPEC } from './spec-bedside.js';
import { CARDIOSPEC } from './spec-cardio.js';
import { REST1SPEC } from './spec-rest1.js';
import { REST2SPEC } from './spec-rest2.js';
import { REST3SPEC } from './spec-rest3.js';
import { REST4SPEC } from './spec-rest4.js';
export const ALLSPECS = [...BEDSIDESPEC, ...CARDIOSPEC, ...REST1SPEC, ...REST2SPEC, ...REST3SPEC, ...REST4SPEC];
export const EXTERNAL = [
  { id: 'euroscore2', name: 'EuroSCORE II', category: 'Cardiovascular', note: 'Logistic model; use euroscore.org calculator.', link: 'https://www.euroscore.org' },
  { id: 'qrisk3', name: 'QRISK3', category: 'Cardiovascular', note: 'Licensed model; use qrisk.org calculator.', link: 'https://www.qrisk.org' },
  { id: 'sts', name: 'STS risk', category: 'Cardiovascular', note: 'Licensed model; use sts.org calculator.', link: 'https://www.sts.org' },
  { id: 'garfield', name: 'GARFIELD-AF', category: 'Cardiovascular', note: 'External coefficient model.', link: '' },
  { id: 'apache4', name: 'APACHE IV', category: 'Critical care', note: 'Proprietary coefficients; use vendor calculator.', link: '' },
  { id: 'oasis', name: 'OASIS', category: 'Critical care', note: 'External severity model; reference card.', link: '' },
  { id: 'phoenix', name: 'Phoenix sepsis', category: 'Pediatrics', note: 'Use published Phoenix criteria tool.', link: '' },
  { id: 'clifc', name: 'CLIF-C ACLF', category: 'GI/Hepatology', note: 'External calculator required.', link: 'https://www.clifresearch.com' },
  { id: 'reynolds', name: 'Reynolds risk', category: 'Cardiovascular', note: 'Licensed; use external calculator.', link: '' },
  { id: 'forrest', name: 'Forrest classification', category: 'GI/Hepatology', note: 'Categorical ulcer-bleed classification (not a sum score).', link: '' },
];
