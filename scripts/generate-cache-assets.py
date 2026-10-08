import json
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants'


def write(relative, data):
    (root/relative).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')


def cube(bottom, top, texture):
    return {'textures':{'food':texture,'particle':texture},'elements':[{'from':bottom,'to':top,
            'faces':{face:{'texture':'#food','uv':[0,0,16,16]} for face in ['up','down','north','south','east','west']}}]}


# The inventory's exact count is a bounded heap; kinds are independent visible samples. 13 * 2^6 = 832 states.
image=Image.new('RGBA',(16,16),(171,125,63,255))
for x in range(16):
    for z in range(16):
        shade=(x*17+z*31+x*z*7)%5
        image.putpixel((x,z),[(186,142,72,255),(139,95,44,255),(209,173,100,255),(171,125,63,255),(159,111,50,255)][shade])
image.save(root/'textures/block/cache_heap.png')
parts=[]
for units in range(1,13):
    name=f'cache_heap_{units}'
    write(f'models/block/{name}.json',cube([3,0,3],[13,0.5+units*0.2,13],'prime_ants:block/cache_heap'))
    parts.append({'when':{'units':str(units)},'apply':{'model':f'prime_ants:block/{name}'}})
for slot,(kind,item) in enumerate([('apple','apple'),('chicken','chicken'),('berries','sweet_berries'),('nectar','flower_nectar'),('flesh','rotten_flesh'),('prey','small_prey')]):
    # Reuse the established food sprite geometry and aliases; retain old models for saved model references.
    old=json.loads((root/f'models/block/cache_{item}_{slot}.json').read_text(encoding='utf-8'))
    name=f'cache_sample_{kind}'
    write(f'models/block/{name}.json',old)
    parts.append({'when':{kind:'true'},'apply':{'model':f'prime_ants:block/{name}'}})
write('blockstates/nest_cache.json',{'multipart':parts})
