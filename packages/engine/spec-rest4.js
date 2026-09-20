import { sel, yn, num } from './golden-part1.js';
// ABCD2 (mutually-exclusive weakness/speech via group), IPSS-R, MMSE, MoCA.
export const REST4SPEC = [
  { id: 'abcd2', name: 'ABCD2', category: 'Neurology', tier: 1,
    purpose: 'TIA stroke risk (App B Row 186; weakness/speech exclusive).',
    formula: 'Age>=60(1)+BP>=140/90(1)+Weakness(2)/Speech(1)+Dur(0/1/2)+DM(1); 0-7',
    engine: 'points-sum', citation: 'App B Row 186',
    inputs: [yn('age','Age >=60',1),yn('bp','BP >=140/90',1),
      sel('clin','Clinical feature',[['o','Other/neither',0],['s','Speech disturbance, no weakness',1],['w','Unilateral weakness',2]],'clin'),
      sel('dur','Duration',[['s','<10 min',0],['m','10-59 min',1],['l','>=60 min',2]]),
      yn('dm','Diabetes',1)],
    bands: [{ range:'0-3',label:'Low',meaning:'2-day stroke ~1%.'},{ range:'4-5',label:'Moderate',meaning:'~4%.'},{ range:'6-7',label:'High',meaning:'~8%.'}] },
  { id: 'ipssr', name: 'IPSS-R', category: 'Oncology', tier: 2,
    // App B Row 75: cyto 0-4 + blasts 0-3 + Hb 0-1.5 + PLT 0-1 + ANC 0-0.5 = 0-10.
    // Row 75's printed "0-9" conflicts with its own '>20% = 4' blast row; the
    // golden vector (blasts 11-20% = 3) constructs to 10. Pin behavior + flag.
    purpose: 'MDS prognosis (App B Row 75; doc 0-9, constructed 0-10).', formula: 'Cyto(0-4)+Blasts(0-3)+Hb(0/1/1.5)+PLT(0/0.5/1)+ANC(0/0.5); constructed 0-10',
    engine: 'points-sum', citation: 'App B Row 75',
    inputs: [sel('cyto','Cytogenetics',[['vg','Very good',0],['g','Good',1],['i','Intermediate',2],['p','Poor',3],['vp','Very poor',4]]),
      sel('blast','Marrow blasts',[['a','<=2%',0],['b','>2-<5%',1],['c','5-10%',2],['d','>10%',3]]),
      sel('hb','Hemoglobin',[['a','>=10',0],['b','8-<10',1],['c','<8',1.5]]),
      sel('plt','Platelets',[['a','>=100',0],['b','50-<100',0.5],['c','<50',1]]),
      sel('anc','ANC',[['a','>=0.8',0],['b','<0.8',0.5]])],
    bands: [{ range:'0-1.5',label:'Very low',meaning:'Median 8.8y.'},{ range:'1.5-3',label:'Low',meaning:'5.3y.'},{ range:'3-4.5',label:'Intermediate',meaning:'3.0y.'},{ range:'4.5-6',label:'High',meaning:'1.6y.'},{ range:'>6',label:'Very high',meaning:'0.8y.'}] },
  { id: 'mmse', name: 'MMSE', category: 'Psychiatry/Cognition', tier: 1,
    purpose: 'Cognitive screen structural (App D #203; PAR restricted).',
    formula: 'Sum of items 0-30; 24-30 normal, 18-23 mild, 0-17 severe',
    engine: 'points-sum', citation: 'App D #203; license: PAR restricted',
    inputs: [num('score','Raw total (0-30)','points')],
    bands: [{ range:'24-30',label:'Normal',meaning:'No impairment suggested.'},{ range:'18-23',label:'Mild',meaning:'Mild impairment.'},{ range:'0-17',label:'Severe',meaning:'Severe impairment.'}],
    compute: 'raw-score' },
  { id: 'moca', name: 'MoCA', category: 'Psychiatry/Cognition', tier: 1,
    purpose: 'Mild impairment screen (App D #204; +1 if <=12y education).',
    formula: 'raw + 1 if education<=12y, cap 30; <26 abnormal',
    engine: 'equation', citation: 'App D #204',
    inputs: [num('raw','Raw total (0-30)','points'),yn('lowedu','Education <=12 years',1)],
    bands: [{ range:'>=26',label:'Normal',meaning:'No impairment suggested.'},{ range:'<26',label:'Abnormal',meaning:'Further evaluation.'}],
    compute: 'moca' },
];
