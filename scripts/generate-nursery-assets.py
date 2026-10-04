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
