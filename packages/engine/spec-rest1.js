import { sel, yn, num } from './golden-part1.js';
export const REST1SPEC = [
  { id: 'four-at', name: '4AT delirium', category: 'Critical care', tier: 1,
    purpose: 'Delirium screen (App B Row 152).',
    formula: 'Alertness(0/1/2/4)+AMT4(0/1/2)+Attention(0/1/2)+Acute(0/4); 0-12',
    engine: 'points-sum', citation: 'App B Row 152',
    inputs: [sel('alert','Alertness',[['n','Normal',0],['d','Mild sleepiness',1],['a','Clearly abnormal',2],['u','Unarousable',4]]),
      sel('amt','AMT4 errors',[['e0','No errors',0],['e1','1 error',1],['e2','2+ errors/untestable',2]]),
      sel('att','Attention months backwards',[['ok','7+ months correct',0],['part','Starts but <7',1],['un','Untestable/refuses',2]]),
      sel('acute','Acute change/fluctuation',[['no','Absent',0],['yes','Present',4]])],
    bands: [{ range:'0',label:'No impairment',meaning:'Delirium unlikely.'},{ range:'1-3',label:'Possible impairment',meaning:'Cognitive impairment possible.'},{ range:'>=4',label:'Possible delirium',meaning:'High specificity for delirium.'}] },
];
