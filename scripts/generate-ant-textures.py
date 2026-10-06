"""Surface-mapped adult/callow cuticle atlas, one texel per raw AntModel unit.

The 0.5 ant subtree and vanilla 16 model units/block give 32 texels/block.
Ovoid UV cells and dimensions match AntModel. Every exposed cube face is shaded
from body coordinates, so highlights and gaster bands follow the surfaces.
"""
from pathlib import Path
import math
from PIL import Image, ImageDraw

target = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants/textures/entity'
target.mkdir(parents=True, exist_ok=True)
TAPER = (.34, .68, .88, .98, 1, .98, .88, .68, .34)
WIDTHS = (.50, .84, 1, .84, .50)
EDGES = (-.50, -.36, -.15, .15, .36, .50)
DIMENSIONS = {
    'worker': ((7.3, 6.8, 7.5), (5, 4.5, 10), (9, 7.4, 12.5), (1.3, 3.1, 1.2)),
    'queen': ((10.5, 9, 11), (13, 12, 19), (19, 15, 31), (2.4, 5, 1.8)),
}


def cuticle(x, y, z, w, h, d, segment, callow):
    nx, ny, nz = x / w, y / h, z / d
    # Smooth polish in body coordinates; no independent grain or pale speckle strokes.
    # Broad dorsal/side lobes stay coherent across each small geometry tier.
    dorsal = math.exp(-((nx + .12) / .20) ** 2) * max(0, -ny * 2)
    lateral = math.exp(-((ny + .16) / .19) ** 2) * min(1, abs(nx) * 2)
    shine = (48 if segment in ('head','gaster') else 36) * dorsal + 14 * lateral
    shade = 5 - 10 * max(0, ny) + shine
    if segment == 'gaster':
        phase = nz + .5
        shade -= sum(7 * math.exp(-((phase-band)/.013)**2) for band in (.36,.60,.80))
    base = (179, 155, 112) if callow else (34, 28, 23)
    rgb = tuple(max(0, min(255, round(c + shade))) for c in base)
    return (*rgb, 255)


def paint_cube(image, u, v, x, y, z, w, h, d, body, segment, callow):
    """Pinned ModelPart.Cube polygon UV order (Minecraft 26.3 sources)."""
    t0, t1 = (x, y, z), (x+w, y, z)
    t2, t3 = (x+w, y+h, z), (x, y+h, z)
    l0, l1 = (x, y, z+d), (x+w, y, z+d)
    l2, l3 = (x+w, y+h, z+d), (x, y+h, z+d)
    u0, u1, u2, u22, u3, u4 = u, u+d, u+d+w, u+d+2*w, u+2*d+w, u+2*d+2*w
    faces = [((l1,l0,t0,t1),u1,v,u2,v+d), ((t2,t3,l3,l2),u2,v+d,u22,v),
             ((t0,l0,l3,t3),u0,v+d,u1,v+d+h), ((t1,t0,t3,t2),u1,v+d,u2,v+d+h),
             ((l1,t1,t2,l2),u2,v+d,u3,v+d+h), ((l0,l1,l2,l3),u3,v+d,u4,v+d+h)]
    for verts, a, b, c, e in faces:
        for py in range(max(0, math.floor(min(b,e))), math.ceil(max(b,e))):
            for px in range(max(0, math.floor(min(a,c))), math.ceil(max(a,c))):
                # Include boundary texels: nearest sampling can hit fractional islands.
                fu = max(0, min(1, (px+.5-a)/(c-a)))
                fv = max(0, min(1, (py+.5-b)/(e-b)))
                point = [verts[1][i]+fu*(verts[0][i]-verts[1][i])+fv*(verts[2][i]-verts[1][i]) for i in range(3)]
                image.putpixel((px,py), cuticle(*point,*body,segment,callow))


for form in ('worker', 'queen', 'callow'):
    callow = form == 'callow'
    image = Image.new('RGBA', (512,512), (179,155,112,255) if callow else (32,28,24,255))
    for segment, v, body in zip(('head','mesosoma','gaster','petiole'), (0,96,192,288), DIMENSIONS['worker' if callow else form]):
        w,h,d = body
        if segment == 'gaster':
            tapers = (.27,.52,.72,.855,.94,.985,1,.985,.94,.855,.72,.52,.27)
            widths = (.26,.50,.72,.90,1,.90,.72,.50,.26)
            edges = (-.50,-.48,-.42,-.30,-.12,.12,.30,.42,.48,.50)
            cell_u,cell_v = 45,10
        else:
            tapers,widths,edges,cell_u,cell_v = TAPER,WIDTHS,EDGES,52,18
        u=0
        for i,taper in enumerate(tapers):
            for j,width in enumerate(widths):
                sw,sh = w*taper,h*taper
                paint_cube(image,u if segment=='gaster' else i*cell_u,v+j*cell_v,-sw*width/2,sh*edges[j],-d/2+i*d/len(tapers),
                           sw*width,sh*(edges[j+1]-edges[j]),d/len(tapers),body,segment,callow)
            if segment=='gaster':u+=math.ceil(2*(w*taper+d/len(tapers)))+1
    draw = ImageDraw.Draw(image)
    # Actual appendage islands: femur, tibia/tarsus and both antenna sections.
    limb = (190,157,101,255) if callow else (59,43,29,255)
    for box in ((0,400,50,453),(64,400,116,425)):
        draw.rectangle(box,fill=limb)
    for x in range(0,48,3):
        draw.line((x,400,x,452),fill=(201,168,112,255) if callow else (66,48,32,255))
    draw.rectangle((160,400,185,420),fill=(137,102,60,255) if callow else (47,32,21,255))
    # Eye cells have their own UVs and a coherent small upper-side glint.
    eye_size=1.3 if form=='queen' else 1
    def eye_cuticle(x,y,z,w,h,d,segment,callow):
        glint=44*math.exp(-((y/h+.28)/.21)**2-((z/d+.15)/.26)**2)
        shade=round(11+glint+6*max(0,-y/h))
        return (shade,shade,shade+1,255)
    body_cuticle=cuticle
    cuticle=eye_cuticle
    for i,taper in enumerate((.68,1,.68)):
        for j,(lo,hi,width) in enumerate(((-.5,-.22,.70),(-.22,.22,1),(.22,.5,.70))):
            ew=1.15*eye_size*taper*width;eh=1.7*eye_size*taper
            paint_cube(image,220+i*12,400+j*6,-ew/2,eh*lo,-1.2*eye_size+i*.8*eye_size,ew,eh*(hi-lo),.8*eye_size,(1.15*eye_size,1.7*eye_size,2.4*eye_size),'eye',False)
    cuticle=body_cuticle
    draw.rectangle((260,400,275,415),fill=(75,57,39,255))
    draw.line((264,401,264,410),fill=(30,24,18,255))
    draw.rectangle((380,400,389,408),fill=(49,35,24,255))
    image.save(target / f'lasius_niger_{form}.png')

# Items retain vanilla 16x16 density. This egg is intentionally absent from loot/recipes.
item_target = target.parent / 'item'
item_target.mkdir(exist_ok=True)
egg = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(egg)
draw.ellipse((3, 1, 12, 14), fill=(41, 35, 27, 255), outline=(19, 17, 16, 255))
for x, y in ((6, 4), (9, 6), (5, 9), (9, 12), (7, 7)):
    draw.rectangle((x, y, x + 1, y + 1), fill=(110, 88, 63, 255))
draw.line((5, 4, 5, 6), fill=(66, 54, 41, 255))
egg.save(item_target / 'debug_lasius_niger_queen_egg.png')
