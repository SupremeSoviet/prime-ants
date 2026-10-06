"""Surface-mapped adult/callow cuticle atlas, one texel per raw AntModel unit.

The 0.5 ant subtree and vanilla 16 model units/block give 32 texels/block.
Ovoid UV cells and dimensions match AntModel. Every exposed cube face is shaded
from body coordinates, so highlights, hairs and gaster bands follow the surfaces.
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
    # Longitudinal dorsal polish, restrained and continuous across UV islands.
    shine = 15 * math.exp(-((nx + .12) / .17) ** 2) * max(0, -ny * 2)
    shade = 4 - 11 * max(0, ny) + shine
    grain = ((math.floor(x * 3) * 17 + math.floor(y * 3) * 11 + math.floor(z * 3) * 7) % 5) - 2
    if segment == 'gaster':
        phase = nz + .5
        if min(abs(phase - band) for band in (.36, .60, .80)) < .023:
            shade -= 13  # Dark transverse intersegment sutures, including side faces.
        elif min(abs(phase - band) for band in (.39, .63, .83)) < .018:
            shade += 5
    base = (179, 155, 112) if callow else (32, 28, 24)
    rgb = tuple(max(0, min(255, round(c + shade + grain))) for c in base)
    # Sparse fine pale strokes on exposed dorsal/lateral cuticle, in body coordinates.
    cell_x, cell_z = math.floor(x / 2.5), math.floor(z / 3)
    hair = (cell_x * 37 + cell_z * 71 + 20261006) % 7
    across, along = x - cell_x * 2.5, z - cell_z * 3
    if hair == 0 and .85 < across < 1.35 and .45 < along < 2.25 and ny < .15:
        rgb = (209, 188, 147) if callow else (100, 90, 73)
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
        for i,taper in enumerate(TAPER):
            for j,width in enumerate(WIDTHS):
                sw,sh = w*taper,h*taper
                paint_cube(image,i*52,v+j*18,-sw*width/2,sh*EDGES[j],-d/2+i*d/9,
                           sw*width,sh*(EDGES[j+1]-EDGES[j]),d/9,body,segment,callow)
    draw = ImageDraw.Draw(image)
    # Actual appendage islands: femur, tibia/tarsus and both antenna sections.
    limb = (190,157,101,255) if callow else (59,43,29,255)
    for box in ((0,400,50,453),(64,400,116,425)):
        draw.rectangle(box,fill=limb)
    for x in range(0,48,3):
        draw.line((x,400,x,452),fill=(201,168,112,255) if callow else (66,48,32,255))
    draw.rectangle((160,400,185,420),fill=(137,102,60,255) if callow else (47,32,21,255))
    draw.rectangle((220,400,238,415),fill=(10,10,11,255))
    for y in range(400,414,2):
        for x in range(220+(y%4),237,3):draw.point((x,y),fill=(37,36,33,255))
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
