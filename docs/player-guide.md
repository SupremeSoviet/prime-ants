# Player guide / Руководство игрока

[English](#english) · [Русский](#русский)

## English

### Installing

You need Minecraft Java Edition 26.3, Java 25, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3. Put the Prime Ants
jar and Fabric API into the `mods` folder of your Fabric profile. For multiplayer, the server and every player need
both. Back up existing worlds before adding the mod.

### Finding a colony

Colonies live in **plains, meadows and flower forests**, in newly generated terrain. You may find:

- a **founding queen** digging alone, or a small soil mound next to a stair entrance;
- a **Young or Mature colony** with an established nest, a bigger mound and worker traffic.

Terrain has to suit a nest (dry natural soil, enough support), so not every meadow has ants, and there may be none
right at spawn.

### How long things take

At normal speed each brood stage (egg, larva, cocoon) takes about ten minutes. A queen's first workers appear about
half an hour after she lays her first eggs, plus the time she needs to dig. After that, the colony grows as fast as its
food allows. It moves to a new stage when it has enough adults, the chambers it needs and enough supplies.

Colonies are fully alive only while their chunks are loaded. Pausing a singleplayer game stops them too. When you
come back to an area, the colonies there catch up on the time you were away, in a simplified way.

### Feeding

Drop food near a trail, then step back at least four blocks so you don't pick it up again:

- **sugar**: apples or sweet berries;
- **protein**: raw chicken or rotten flesh.

Foragers pick the food up and carry it home, and nurses pass it on to the queen and the larvae. Larvae need protein:
sugar alone cannot raise them. Feeding helps a colony, but you can't give it orders.

### Materials

Once a colony has a material store, its foragers also carry home clay, stone, gravel, sand and ore that you drop near
a trail, one item per trip. Colonies use these to upgrade chambers and build on the surface, and they depend on you
for most of it:

- A Young colony needs **16 clay balls** to become Mature, and it doesn't mine yet.
- A Mature colony needs about **40 more clay balls** to rebuild its chambers in packed clay. It mines and builds on
  the surface only after that.
- Stone chambers need **smooth stone**. Cobblestone counts towards the Great stage but can't be used for the walls.

### Visiting

When the workers have opened the entrance, you can walk down the stairs, which are two blocks high. Give the ants room
to pass.

Supported dry torches, wall torches, soul torches and lanterns can light openings already belonging to the colony.
Lanterns may stand or hang from a solid support in workers' added rooms or generated nest rooms. The queen's original
founding room still accepts torches as before. Ants collide with lanterns, so leave another route. Removing a light
restores the original opening. Do not replace walls, stores or undug cells with lights; lighting does not give those
cells to the colony. Broken walls, solid blocks and flooding can still stop development.

### Defence

Watching and feeding are always fine. If you hurt an ant or break part of the nest in Survival, the colony raises an
alarm:

- adult workers within twelve blocks come after you and bite when they can reach you;
- each bite takes half a heart, about once per second;
- the alarm lasts about thirty seconds, and the ants give up the chase beyond sixteen blocks.

Back away and stop causing harm. Ordinary hostile mobs are still dangerous while you watch.

### Limitations

- One species, *Lasius niger*. Nuptial flights and fruit harvesting are not in yet.
- Awkward terrain, food shortages and crowding can stall a colony. Damage to the nest is not always repaired.
- Solid blocks revoke the colony's openings permanently. Removing them or placing lights does not restore ownership.
  Damage from lighting already saved in 0.2.0 cannot recover automatically either.
- Large colonies are heavy on the server; see [performance](performance.md).
- Long sessions and multiplayer have had limited testing.

## Русский

### Установка

Нужны Minecraft Java Edition 26.3, Java 25, Fabric Loader 0.19.5 и Fabric API 0.161.0+26.3. Положите jar Prime Ants и
Fabric API в папку `mods` вашего профиля Fabric. Для мультиплеера оба файла нужны и на сервере, и у каждого игрока.
Перед установкой мода сделайте резервную копию существующих миров.

### Где найти колонию

Колонии живут на **равнинах, лугах и в цветочных лесах**, в новом рельефе. Можно встретить:

- **матку-основательницу**, которая копает в одиночку, или маленький курган земли рядом со входом-лесенкой;
- **молодую или зрелую колонию** с готовым гнездом, курганом побольше и движением рабочих.

Для гнезда нужен подходящий рельеф (сухая естественная почва, надёжная опора), так что муравьи живут не на каждом
лугу, и рядом с точкой появления их может не оказаться.

### Сколько ждать

На обычной скорости каждая стадия выводка (яйцо, личинка, кокон) длится около десяти минут. Первые рабочие у матки
появляются примерно через полчаса после первых яиц, плюс время на копание. Дальше колония растёт так быстро, как
позволяет еда. На новую стадию она переходит, когда у неё хватает взрослых, нужных камер и запасов.

Колонии полностью живут, только пока их чанки загружены; пауза в одиночной игре тоже их останавливает. Когда вы
возвращаетесь, колонии наверстывают время вашего отсутствия в упрощённом виде.

### Подкормка

Бросьте еду рядом с тропой и отойдите хотя бы на четыре блока, чтобы не подобрать её обратно:

- **сахар**: яблоки или сладкие ягоды;
- **белок**: сырая курица или гнилая плоть.

Фуражиры подберут еду и унесут домой, а няньки передадут её матке и личинкам. Личинкам нужен белок: на одном сахаре
их не вырастить. Подкормка помогает колонии, но приказывать ей нельзя.

### Материалы

Когда у колонии появится склад материалов, фуражиры будут уносить туда и глину, камень, гравий, песок и руду,
брошенные у тропы, по одному предмету за ходку. Из них колония улучшает камеры и строит на поверхности, и почти всё это
зависит от вас:

- молодой колонии нужно **16 комков глины**, чтобы стать зрелой, а копать шахту она ещё не умеет;
- зрелой колонии нужно ещё около **40 комков глины**, чтобы перестроить камеры в утрамбованной глине; только после
  этого она копает шахту и строит на поверхности;
- для каменных камер нужен **гладкий камень**; булыжник засчитывается для великой стадии, но для стен не годится.

### В гостях

Когда рабочие откроют вход, можно спуститься по ступенькам высотой в два блока. Пропускайте муравьёв.

В уже принадлежащих колонии проёмах можно ставить сухие факелы и фонари с опорой, включая настенные факелы и варианты
с огнём душ. Фонари можно ставить на пол или подвешивать в добавленных рабочими камерах или готовых сгенерированных
гнёздах. В исходной камере матки по-прежнему можно ставить факелы. Оставляйте обход: муравьи сталкиваются с фонарями.
Удаление
света возвращает исходный проём. Не заменяйте светом стены, склады и ещё не выкопанные клетки: колония не получит их
во владение. Сломанные стены, полные блоки и затопление по-прежнему могут остановить развитие.

### Защита

Смотреть и кормить можно всегда. Если в режиме выживания ударить муравья или сломать часть гнезда, колония поднимает
тревогу:

- взрослые рабочие в радиусе двенадцати блоков бросаются к вам и кусают, когда могут дотянуться;
- каждый укус отнимает полсердца, примерно раз в секунду;
- тревога длится около тридцати секунд, а дальше шестнадцати блоков муравьи погоню бросают.

Отойдите и перестаньте вредить. Обычные враждебные мобы рядом с колонией по-прежнему опасны.

### Ограничения

- Пока есть один вид, *Lasius niger*. Брачных лётов и сбора плодов ещё нет.
- Сложный рельеф, нехватка еды и теснота могут остановить колонию. Повреждения гнезда чинятся не всегда.
- Полные блоки навсегда отнимают проёмы у колонии. Удаление блока или установка света владение не возвращает.
  Повреждения от освещения, уже сохранённые в 0.2.0, тоже не восстанавливаются автоматически.
- Большие колонии нагружают сервер; см. [производительность](performance.md) (на английском).
- Долгие сеансы и мультиплеер проверены мало.
