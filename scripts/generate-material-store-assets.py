import json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants'
def write(relative, data):
    (root / relative).write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
# Material heaps keep the four-unit step and presence samples while the range doubles; all geometry fits one cell.
parts=[{'apply':{'model':'prime_ants:block/material_store'}}]
for kind,maximum,step in [('clay',24,0.75),('stock',12,1.5)]:
    base=json.loads((root/f'models/block/material_store_{kind}_1.json').read_text(encoding='utf-8'))
    for level in range(1,maximum+1):
        model=json.loads(json.dumps(base)); model['elements'][0]['to'][1]=min(16.0,4+step*level)
        if 4+step*level>16:
            # Wider upper heaps distinguish larger totals without taller collision/reach geometry.
            extra=(4+step*level-16)/(6 if kind=='clay' else 6)
            model['elements'][0]['from'][0]=max(0,model['elements'][0]['from'][0]-extra)
            model['elements'][0]['to'][0]=min(16,model['elements'][0]['to'][0]+extra)
        # UVs remain within the pinned 16x16 sprite; geometry is the visible quantity.
        name=f'material_store_{kind}_{level}'
        write(f'models/block/{name}.json',model)
        parts.append({'when':{kind:str(level)},'apply':{'model':f'prime_ants:block/{name}'}})
for kind in ['stone','gravel','sand','ore']:
    parts.append({'when':{kind:'true'},'apply':{'model':f'prime_ants:block/material_store_lump_{kind}'}})
write('blockstates/material_store.json',{'multipart':parts})
print('Generated food 24 parts / 1216 states and material 41 parts / 5200 states; 16x16 food heap texture')
