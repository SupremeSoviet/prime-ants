import json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants'
def write(relative, data):
    (root / relative).write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
# Material heaps keep the four-unit step and presence samples while the range doubles; all geometry fits one cell.
parts=[{'apply':{'model':'prime_ants:block/material_store'}}]
for kind,maximum,step in [('clay',16,0.75),('stock',8,1.5)]:
    base=json.loads((root/f'models/block/material_store_{kind}_1.json').read_text(encoding='utf-8'))
    for level in range(1,maximum+1):
        model=json.loads(json.dumps(base)); model['elements'][0]['to'][1]=4+step*level
        # UVs remain within the pinned 16x16 sprite; geometry is the visible quantity.
        name=f'material_store_{kind}_{level}'
        write(f'models/block/{name}.json',model)
        parts.append({'when':{kind:str(level)},'apply':{'model':f'prime_ants:block/{name}'}})
for kind in ['stone','gravel','sand','ore']:
    parts.append({'when':{kind:'true'},'apply':{'model':f'prime_ants:block/material_store_lump_{kind}'}})
write('blockstates/material_store.json',{'multipart':parts})
print('Generated food 18 parts / 832 states and material 29 parts / 2448 states; 16x16 food heap texture')
