"""Visible projection of six canonical cache units using vanilla 16x16 food sprites."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/prime_ants'
parts = []
for slot in range(6):
    x, z = 2 + (slot % 3) * 4, 2 + (slot // 3) * 7
    for value, item in [(1, 'apple'), (2, 'chicken'), (3, 'sweet_berries')]:
        name = f'cache_{item}_{slot}'
        model = {'textures': {'food': f'prime_ants:block/cache_{item}', 'particle': f'prime_ants:block/cache_{item}'}, 'elements': [
            {'from': [x, 0.5, z], 'to': [x + 3, 0.6, z + 3],
             'faces': {face: {'texture': '#food', 'uv': [0, 0, 16, 16]} for face in ['up', 'down']}},
            {'from': [x, 0.5, z + 1.5], 'to': [x + 3, 3.5, z + 1.6],
             'faces': {face: {'texture': '#food', 'uv': [0, 0, 16, 16]} for face in ['north', 'south']}}]}
        (root / 'models/block' / f'{name}.json').write_text(json.dumps(model, indent=2), encoding='utf-8')
        parts.append({'when': {f'slot{slot}': str(value)}, 'apply': {'model': f'prime_ants:block/{name}'}})
(root / 'blockstates/nest_cache.json').write_text(json.dumps({'multipart': parts}, indent=2), encoding='utf-8')
# 26.3 separates item and block atlases. Alias existing vanilla 16x16 sprites into the block atlas;
# do not copy stock, create display entities, or replace the vanilla atlas sources.
atlas = root.parent / 'minecraft/atlases/blocks.json'
atlas.parent.mkdir(parents=True, exist_ok=True)
atlas.write_text(json.dumps({'sources': [{'type': 'minecraft:single', 'resource': f'minecraft:item/{item}',
                                        'sprite': f'prime_ants:block/cache_{item}'}
                                       for item in ['apple', 'chicken', 'sweet_berries']]}, indent=2), encoding='utf-8')
for lang, caption in [('en_us', 'Nest food cache'), ('ru_ru', 'Запас пищи в гнезде')]:
    file = root / 'lang' / f'{lang}.json'
    data = json.loads(file.read_text(encoding='utf-8-sig'))
    data['block.prime_ants.nest_cache'] = caption
    file.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
