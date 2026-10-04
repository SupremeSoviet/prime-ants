"""Reproducible chitin atlas for the production AntModel (32 texels per block)."""
from pathlib import Path
import random
from PIL import Image, ImageDraw

target = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants/textures/entity'
target.mkdir(parents=True, exist_ok=True)
for form in ('worker', 'queen'):
    rng = random.Random(20261004)
    image = Image.new('RGBA', (256, 128))
    pixels = image.load()
    for y in range(128):
        for x in range(256):
            noise = rng.randrange(-3, 4)
            shine = max(0, 5 - abs(x % 24 - 7))
            pixels[x, y] = (38 + noise + shine, 34 + noise + shine, 30 + noise + shine, 255)
    draw = ImageDraw.Draw(image)
    # Natural dorsal chitin polish on the actual faceted ModelPart cube UV islands.
    # Model-local min-Y faces become upper faces after the renderer's Y flip.
    dimensions = ((12, 9, 12), (13, 11, 20), (3.5, 5, 3), (18, 13, 29)) if form == 'queen' else (
        (7, 6, 7), (5, 4.5, 10), (1.8, 3.5, 1.8), (8, 6.5, 11))
    for (u, v), (w, h, d) in zip(((0, 0), (0, 48), (40, 90), (64, 0)), dimensions):
        depth = d / 5
        for taper in (0.48, 0.8, 1):
            width = w * taper * 0.76
            draw.rectangle((u + depth, v, u + depth + width, v + depth), fill=(57, 51, 45, 255))
    # Eye faceting, jaw tips and pale wing attachment scars occupy their actual UV islands.
    draw.rectangle((220, 0, 238, 12), fill=(9, 10, 11, 255))
    for y in range(0, 12, 2):
        for x in range(220 + y % 4, 238, 3):
            draw.point((x, y), fill=(48, 49, 48, 255))
    draw.rectangle((190, 0, 215, 14), fill=(51, 34, 22, 255))
    draw.rectangle((220, 24, 234, 34), fill=(107, 87, 64, 255))
    draw.line((224, 25, 224, 31), fill=(40, 31, 24, 255), width=1)
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
