import sys,math
if '--brood-only' in sys.argv:
 import pathlib,json
 from PIL import Image
 root=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/assets/prime_ants'
 faces=('up','down','north','south','east','west')
 def oval(cx,cy,cz,w,h,d,slices=5):
  out=[]
  for i in range(slices):
   axial=math.sqrt(max(.08,1-((i+.5)/slices*2-1)**2))
   for j,(lo,hi,width) in enumerate(((-.5,-.22,.68),(-.22,.22,1),(.22,.5,.68))):
    x0=cx-w*axial*width/2;x1=cx+w*axial*width/2
    y0=cy+h*axial*lo;y1=cy+h*axial*hi;z0=cz-d/2+i*d/slices;z1=z0+d/slices
    # Block textures retain vanilla density: UV extents match element dimensions.
    uv={'up':[x0,z0,x1,z1],'down':[x0,z0,x1,z1],'north':[x0,16-y1,x1,16-y0],'south':[x0,16-y1,x1,16-y0],'east':[z0,16-y1,z1,16-y0],'west':[z0,16-y1,z1,16-y0]}
    out.append({'from':[x0,y0,z0],'to':[x1,y1,z1],'faces':{f:{'texture':'#all','uv':uv[f]} for f in faces}})
  return out
 for slot,(x,z) in zip('abc',((2,3),(9,3),(5,10))):
  for stage in ('egg','larva','cocoon'):
   if stage=='egg':elements=oval(x+1.5,1,z+1.5,2.6,2,3)
   elif stage=='cocoon':elements=oval(x+2.5,1.5,z+2,4.5,3,4,7)
   else:
    elements=[]
    for i,(cx,cz,w,h) in enumerate(((x+1,z+1.6,1.6,1.7),(x+2.1,z+1.3,1.9,2.3),(x+3.3,z+1.6,1.8,2.5),(x+4.1,z+2.2,1.3,1.8))):elements+=oval(cx,h/2,cz,w,h,1.25,3)
   model={'textures':{'all':f'prime_ants:block/brood_{stage}','particle':f'prime_ants:block/brood_{stage}'},'elements':elements}
   (root/f'models/block/brood_{stage}_{slot}.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
 for stage,base in (('egg',(236,227,198)),('larva',(231,212,159)),('cocoon',(183,149,103))):
  im=Image.new('RGB',(16,16))
  for y in range(16):
   for x in range(16):
    shade=round(5*math.sin(x*.8)+3*math.cos(y*.7))
    if stage=='larva':shade-=12 if x in (3,5,7,11,13) else 0
    if stage=='cocoon':shade+=7 if (x+2*y)%5==0 else -2
    im.putpixel((x,y),tuple(max(0,min(255,v+shade)) for v in base))
  im.save(root/f'textures/block/brood_{stage}.png')
 raise SystemExit(0)

import pathlib,json
from PIL import Image
root=pathlib.Path('src/main/resources/assets/prime_ants')
(root/'blockstates').mkdir(exist_ok=True);(root/'models/block').mkdir(exist_ok=True);(root/'textures/block').mkdir(exist_ok=True)
def write(p,obj):p.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
write(root/'blockstates/nest_soil.json',{'variants':{'':{'model':'prime_ants:block/nest_soil'}}})
write(root/'models/block/nest_soil.json',{'parent':'minecraft:block/cube_all','textures':{'all':'prime_ants:block/nest_soil'}})
parts=[]
for i,slot in enumerate('abc'):
 for stage in ['egg','larva','cocoon']:
  parts.append({'when':{slot:stage},'apply':{'model':f'prime_ants:block/brood_{stage}_{slot}'}})
  x,z=[(2,3),(9,3),(5,10)][i]; w,h,d={'egg':(3,2,3),'larva':(5,2.5,3),'cocoon':(5,3,4)}[stage]
  element={'from':[x,0,z],'to':[x+w,h,z+d],'faces':{face:{'texture':'#all','uv':[0,0,16,16]} for face in ['up','down','north','south','east','west']}}
  write(root/f'models/block/brood_{stage}_{slot}.json',{'textures':{'all':f'prime_ants:block/brood_{stage}','particle':f'prime_ants:block/brood_{stage}'},'elements':[element]})
write(root/'blockstates/brood_pile.json',{'multipart':parts})
colors={'nest_soil':(103,73,44),'brood_egg':(231,221,181),'brood_larva':(225,204,137),'brood_cocoon':(183,147,91)}
for name,c in colors.items():
 im=Image.new('RGB',(16,16))
 for y in range(16):
  for x in range(16):
   n=((x*17+y*29+x*y*7)%23)-11
   if name=='brood_larva' and x%5==0:n-=18
   if name=='brood_cocoon' and (x+y)%4==0:n+=17
   im.putpixel((x,y),tuple(max(0,min(255,v+n)) for v in c))
 im.save(root/f'textures/block/{name}.png')
im=Image.open(root/'textures/entity/lasius_niger_worker.png').convert('RGBA')
for y in range(im.height):
 for x in range(im.width):
  r,g,b,a=im.getpixel((x,y))
  if a and max(r,g,b)>28:
   shade=min(1,0.74+max(r,g,b)/800)
   im.putpixel((x,y),(int(229*shade),int(198*shade),int(143*shade),a))
im.save(root/'textures/entity/lasius_niger_callow.png')
for lang in ['en_us','ru_ru']:
 p=root/f'lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'))
 d.update({'block.prime_ants.nest_soil':'Nest soil' if lang=='en_us' else '\u0413\u0440\u0443\u043d\u0442 \u0433\u043d\u0435\u0437\u0434\u0430','block.prime_ants.brood_pile':'Lasius niger brood pile' if lang=='en_us' else '\u041a\u0443\u0447\u043a\u0430 \u0432\u044b\u0432\u043e\u0434\u043a\u0430 Lasius niger'})
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
