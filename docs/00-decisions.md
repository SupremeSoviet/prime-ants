# 00. Решения для перезапуска

> Приняты 25.09.2026: ответы на 12 вопросов из `11-lessons-and-restart.md` и то, что из них следует.
> **Этот документ главнее разделов 01–11.** Те описывают старый мод, здесь задано направление нового. Где старые разделы
> противоречат этому документу, действует он. Пометка «предложение» означает мой вариант, который ещё нужно подтвердить.

## Главный принцип: реализм

Почти все ответы сводятся к одному: **как у настоящих муравьёв, насколько это позволяет Minecraft**. Анатомия, поведение,
развитие, гнёзда и конфликты берутся из биологии муравьёв. Где реализм мешает игре, мы упрощаем (сжимаем время, ограничиваем
численность), но не придумываем магию и условности.

## Решения

| # | Вопрос | Решение | Что это значит |
|---|---|---|---|
| 1 | Модель населения | **муравьи-мобы и есть население** | каждый муравей — сущность; работа, запасы и бой делаются только руками муравьёв; убитый муравей потерян |
| 2 | Облик | **реалистичные насекомые** | настоящая анатомия и посадка, касты различаются морфологией вида |
| 3 | Время | **реалистично** (шкалу предлагаю ниже) | предложение: 1 игровой день ≈ 1 месяц жизни колонии |
| 4 | Выводок | **нужен, с яслями** | яйцо → личинка → куколка → молодой муравей в выводковых камерах, с уходом нянек |
| 5 | Ресурсы | **физические** | еда и материалы — предметы, которые муравьи носят и складывают; запасы видны в гнезде |
| 6 | Постройки | **встраивание в рельеф** | гнездо выкапывают сами муравьи: ходы и камеры под землёй, выброс грунта на поверхности |
| 7 | Материалы | **свои блоки** | грунт гнезда, стенки ходов, хвойный купол, грибной сад, кучки выводка и т. д. |
| 8 | Появление колоний | **случайно в мире; для отладки — яйцо призыва** | колонии при генерации мира и после брачных лётов; яйцо только для отладки |
| 9 | Управление | **продумать роли, пока не реализовывать** | ниже проект ролей игрока по отношению к колонии |
| 10 | Вражда | **сама собой, через события** | стычки на границах, войны, набеги идут без участия игрока |
| 11 | Культуры | **реализм** | вместо выдуманных культур — настоящие виды муравьёв |
| 12 | Версия | **текущая** | Minecraft Java 26.3 и актуальный Fabric |

## Что реализм меняет в старом контенте

Это самое крупное следствие решений: от «муравьиной деревни с торговлей и дипломатией» мод переходит к **реалистичной экологии
муравьёв**. Строки с пометкой «подтвердить» — это то, что я предлагаю убрать или сильно поменять.

| Система | В старом моде | В новом | Действие |
|---|---|---|---|
| Население | числа плюс декоративные мобы | только мобы (п. 1) | переделать |
| Касты | рабочий, разведчик, шахтёр, солдат, мажор, гигант, матка | матка; рабочие по размерам, как у вида (минор, медиа, мажор/солдат); крылатые самки и самцы. «Разведчик» и «шахтёр» — занятия, а не касты | переделать |
| Рост | 1 муравей в секунду из запасов | яйца матки → выводок → молодые муравьи (п. 4) | переделать |
| Ресурсы | 7 абстрактных чисел (пища, руда, хитин, смола, грибы, яд, знания) | физическая еда и материалы (п. 5). Руда, хитин, знания и яд как ресурсы уходят | переделать |
| Постройки | 18 надземных зданий-курганов по чертежам | выкопанное гнездо: ходы и камеры; холмик из грунта; купол из хвои (п. 6) | переделать; формат чертежей становится форматом «планов гнёзд» |
| Развитие | ранги Форпост…Цитадель, стадии за секунды, мегапроекты | жизненный цикл колонии: основание → рост → зрелость (брачные лёты) → угасание (п. 3) | переделать |
| Исследования | 7 узлов за «знания» | в природе нет. Предложение: колония «открывает» новое по мере созревания, а игрок ведёт полевой дневник | **вопрос** |
| Культуры | Янтарная, Листорезы, Огненные, Древоточцы | настоящие виды (п. 11). Янтарной культуры в природе нет | переделать |
| Инстинкты | порядок приоритетов, пыль на муравья | колония сама распределяет работу по нуждам; игрок влияет подкормкой и приманками | переделать |
| Дипломатия | послы, дань, перемирие, военный пакт за жетоны | между колониями дипломатии не бывает: отношения определяют запах, территория, вид и стычки (п. 10) | **убрать, подтвердить** |
| Торговля | 27 обменов за феромонные жетоны | нет | **убрать, подтвердить** |
| Контракты | заявки с наградой | нужды колонии видны в мире (голодные муравьи, пустые камеры) и в дневнике; помощь — это подкормка | переделать |
| Рейды | формула плюс декоративные солдаты | настоящие бои мобов, набеги и войны (п. 10) | переделать |
| События | 7 событий, в основном декорации | брачный лёт, переезд гнезда, стычки, войны, набеги, нашествие кочевых муравьёв | переделать |
| Предметы | хитиновая броня, сабля, копьё, жетоны, печати, стяги | продукты муравьёв (муравьиная кислота, смола, падь, грибная культура, шёлк) и снаряжение мирмеколога (пробирка, эксгаустер, формикарий, лупа) | **переделать, подтвердить** |
| Интерфейс | планшет колонии с 8 разделами | полевой дневник: что игрок узнал о колониях и видах. Без «пульта управления» | **вопрос** |
| Облик поселения | курган 20–30 блоков, здания с интерьерами | реалистичная надземная часть плюс подземные гнёзда | переделать |

Что остаётся полезным почти без изменений: опыт с форматом чертежей и тестами формы, язык поз и состояний работы муравьёв,
идея «помогать, не командуя», идеи событий, рубрика визуальной оценки и процессные уроки (раздел 10).

## 1. Муравьи — это население

**Правила:**
- Каждый муравей колонии — сущность в мире. Численность колонии = живые муравьи плюс выводок.
- Любая работа физическая: найти, взять, донести, выкопать, накормить. Абстрактного дохода нет.
- Убитый муравей потерян навсегда, замена только через выводок.
- Муравьи **стареют**: у рабочего есть срок жизни, поэтому колонии постоянно нужен выводок.
- Матка — сущность. Если она погибла, новых яиц нет, и колония постепенно вымирает (кроме видов с несколькими матками).
- «Мозг» колонии остаётся данными: карта гнезда, список муравьёв, запасы по камерам, карта феромонных троп, задачи.
  Но это **описание реальности, а не параллельная реальность**.

**Роли муравьёв, как у настоящих (возрастной полиэтизм):**

| Этап жизни рабочего | Где | Что делает |
|---|---|---|
| Молодой (только вышел из куколки, светлый и мягкий) | в гнезде | почти ничего, темнеет и твердеет |
| Нянька | выводковые камеры | кормит личинок и матку, переносит и чистит выводок |
| Строитель, уборщик | гнездо | копает, выносит грунт, мусор и мёртвых, раскладывает запасы |
| Фуражир, охранник | снаружи | ищет еду, прокладывает следы, носит добычу, охраняет входы и территорию |

- Роли гибкие, как в природе: если фуражиры погибли, их место раньше срока занимают молодые.
- Морфология задаёт склонности: миноры чаще няньки и фуражиры, мажоры и солдаты — оборона, дробление семян, тяжёлые грузы.
- Отдельные занятия: разведчики (ищут новую еду и места), «похоронщики» (выносят мёртвых), садовники грибов (у листорезов).

**Ограничения ради производительности (предложение):**
- Лимит взрослых на колонию: 30–120 в зависимости от вида и зрелости, настраивается. Реальные тысячи недостижимы, поэтому берём
  **реалистичные пропорции** (доли каст, отношение выводка к взрослым), а не численность.
- Колония живёт, когда её чанки загружены, как деревни. При загрузке выполняется «догоняющий» расчёт: развитие выводка, расход еды, старение.
- Муравьи глубоко в гнезде без задачи переходят в упрощённый режим ИИ.

## 2. Реалистичный облик

**Анатомия (обязательный минимум):**
- голова с **коленчатыми усиками** (длинный первый членик), жвалами и сложными глазами;
- мезосома (грудь) с 6 ногами, у каждой ноги бедро, голень и лапка;
- **стебелёк** из одного членика (формицины: Lasius, Formica, Camponotus) или двух (мирмицины: Atta, Messor, Solenopsis);
- брюшко (гастер); у формицин ацидопора (брызгают муравьиной кислотой), у многих мирмицин жало.

**Касты по облику:**
- рабочие одного вида различаются размером (у листорезов от крошечных до огромных солдат);
- матка крупнее, с массивной грудью (мышцы крыльев) и следами сброшенных крыльев;
- крылатые самки (гины) и самцы перед брачным лётом; у самцов маленькая голова, большие глаза, они живут недолго.

**Поведение и анимации:**
- походка «тремя опорами», подёргивание усиков, ощупывание друг друга усиками при встрече;
- **груз несут в жвалах** перед головой (листорезы держат кусок листа над собой), а не на спине и не в поднятых лапах;
- **трофаллаксис** (передача пищи изо рта в рот) как видимое действие;
- молодые муравьи светлее и постепенно темнеют;
- у каждого вида свой цвет и пропорции.

Масштаб (насколько муравьи крупные) и то, может ли игрок спуститься в гнездо, пока не решены: см. «Новые вопросы».

## 3. Шкала времени (предложение)

Старый мод жил секундами. Реальная колония живёт месяцами и годами. Предлагаю **1 игровой день (20 минут) ≈ 1 месяц жизни
колонии** с настраиваемым множителем.

Ориентир — чёрный садовый муравей (Lasius niger):

| Событие | В природе | В игре | Реального времени |
|---|---|---|---|
| Развитие от яйца до взрослого | 6–8 недель | ~1,5–2 игровых дня | 30–40 мин |
| Потемнение молодого муравья | несколько дней | ~0,2 дня | ~5 мин |
| Первые рабочие после основания | 6–10 недель | ~2 дня | ~40 мин |
| Жизнь рабочего | от нескольких месяцев до года и больше | ~6–12 дней | 2–4 ч |
| Первые десятки рабочих | первый год | ~10–12 дней | ~4 ч |
| Зрелость (появляются крылатые) | 2–4 года | ~24–48 дней | 8–16 ч |
| Брачный лёт | раз в год, в тёплый влажный день, часто после дождя | раз в «год» (~12 дней), во время или после дождя | — |
| Жизнь матки | 15–28 лет | фактически бессмертна, пока её не убили | — |

- Сезонов в ванилле нет, поэтому «год» — это цикл из 12 игровых дней, а брачные лёты привязаны к дождю.
- Скорость развития выводка можно менять температурой биома (в тепле быстрее, в холоде медленнее), это реалистично.
- Генерация мира создаёт колонии на разных стадиях, включая зрелые, так что брачные лёты игрок увидит без ожидания в 10+ часов.
- Для отладки нужны множитель времени (конфиг или gamerule) и команды оператора.

## 4. Выводок и ясли

- **Стадии:** яйцо → личинка (растёт, её кормят) → куколка (в коконе у формицин, голая у мирмицин) → молодой муравей → взрослый.
- Матка откладывает яйца в выводковой камере. **Темп кладки зависит от того, как её кормят.**
- Няньки кормят личинок (белок плюс сладкое), переносят выводок между камерами (где суше и теплее), помогают выходить из коконов.
- **Каста определяется питанием личинки:** хорошо накормленные вырастают крупными рабочими или солдатами. В зрелой колонии
  в сезон из части личинок растят крылатых самок и самцов.
- При голоде личинки не растут, а при сильном голоде колония съедает часть выводка, как в природе.
- **Представление (предложение):** выводок хранится в «кучках» — блоках с содержимым в выводковых камерах, с видимыми стадиями
  (кладка яиц, личинки, коконы) и вместимостью. Отдельная сущность на каждое яйцо слишком дорога.
- **Ясли — это выводковые камеры** в гнезде, которые выкапывают сами муравьи, а не здание на поверхности.

## 5. Физические ресурсы

| Ресурс | Откуда в мире | Кто собирает | Как хранится |
|---|---|---|---|
| Сладкое: падь тлей, нектар, сладкие плоды | тли на растениях (отдельная механика), цветы, сладкие ягоды, яблоки | почти все виды | в зобике муравьёв («общественный желудок»), передаётся трофаллаксисом |
| Белок: насекомые, падаль | мелкие существа, дроп с мобов, трупы | почти все | не хранится долго, сразу идёт личинкам |
| Семена | семена растений, трава | муравьи-жнецы | зернохранилища (кучки семян в камерах) |
| Листья | листва, трава, цветы | листорезы | несут на грибной сад, не едят сами |
| Гриб | растёт в грибных садах на листьях | листорезы | сам грибной сад и есть запас |
| Хвоя и веточки | ель, сосна, опавшие ветки | рыжие лесные | материал купола |
| Смола хвойных | брёвна ели и сосны | рыжие лесные | комочки в гнезде (защита от плесени) |
| Грунт | выкопанные блоки | все | выносится наружу, из него растёт холмик |

- **Запас распределён физически:** в зобиках муравьёв, в камерах-хранилищах (блоки с ограниченной вместимостью), в грибных садах.
  Вместимость ограничена выкопанными камерами, это естественное ограничение роста.
- **Потребление:** каждый муравей питается из своего зобика и просит еду у других; личинкам нужен белок; матке много еды.
  При долгом голоде взрослые гибнут, выводок останавливается.
- **Отходы:** мусор и мёртвых выносят в мусорную камеру или кучу снаружи.
- Переносимый предмет **виден в жвалах**.

## 6. Гнездо в рельефе

- **Основание:** матка после брачного лёта (или при генерации мира) ищет подходящий грунт своего вида, сбрасывает крылья,
  выкапывает камеру основания и запечатывается.
- **Рост:** рабочие копают по «плану гнезда» вида. Это вертикальные шахты и горизонтальные камеры на разной глубине, как на
  реальных слепках гнёзд. Функции камер: королевская, выводковая, хранилище или зернохранилище, грибной сад, мусорная.
- **Надземная часть вырастает из реального материала:** выкопанный грунт образует холмик или кратер у входа; у рыжих лесных
  над гнездом растёт купол из собранной хвои и веточек.
- **Что можно копать:** только природный грунт по тегу (земля, трава, песок, гравий, подзол, глина), камень медленно или
  вовсе нет, в зависимости от вида. **Блоки, поставленные игроком, — никогда.** Никакой массовой расчистки.
- **Тропы:** на постоянных маршрутах фуражиров трава вытаптывается; у листорезов и рыжих лесных получаются заметные «дороги».
- **Погода:** в дождь входы закрываются (затопление ходов можно добавить позже).
- **Как пригодится старый опыт:** формат чертежей и его проверки превращаются в «планы гнёзд» по видам (глубина шахт, шаг и размеры
  камер, их функции). Компилятор выдаёт не готовые блоки, а **очередь задач копания**, которую муравьи выполняют по блоку.
  Тесты формы (связность, проходимость) остаются.

## 7. Свои материалы (предварительный список)

| Блок | Что это | Где встречается |
|---|---|---|
| Грунт гнезда | выкопанная земля комочками | холмики и кратеры у входов |
| Стенка хода | утрамбованный грунт | стенки ходов и камер |
| Хвойная подстилка | хвоя и веточки | купол рыжих лесных муравьёв |
| Древесная труха | опилки от прогрызания древесины | у гнёзд древоточцев |
| Мусорная куча | отходы и останки | мусорные камеры и кучи снаружи |
| Грибной сад | губчатая масса гриба на листьях | камеры листорезов |
| Кучки выводка | кладка яиц, личинки, коконы | выводковые камеры |
| Зернохранилище | кучка семян | камеры жнецов |
| Смоляные комочки | собранная смола | гнёзда рыжих лесных муравьёв |
| Позже | листовое гнездо на шёлке (ткачи), картон (Lasius fuliginosus) | кроны деревьев, дупла |

Разрешение текстур (16×16 как ванилла или 32×32, как требовал старый проект) пока не решено: см. «Новые вопросы».

## 8. Появление колоний

- **Генерация мира:** колонии разных видов в подходящих биомах, на разных стадиях (молодые и зрелые), с уже выкопанными гнёздами.
- **Динамика:** зрелые колонии в дождь устраивают брачный лёт. Самки приземляются и пытаются основать колонии, и большинство гибнет,
  как в природе. Несколько приживаются рядом, так что в мире со временем «случайно» появляются новые колонии.
- **Смерть колоний:** гибель матки ведёт к вымиранию; брошенное гнездо остаётся в мире.
- **Отладка:** яйцо призыва (spawn egg), только в креативе и для операторов. По одному на вид; ставит молодую матку, которая сразу
  начинает основание. Плюс команды оператора: создать колонию нужного вида и стадии, ускорить время.

## 9. Роли игрока (проект, реализация позже)

П. 9 я понял как роли игрока по отношению к колонии. Раз колонии дикие, «управлять» ими нельзя, можно заслужить разное отношение.
Муравьи узнают своих и чужих по запаху, поэтому роль игрока — это то, **как он пахнет для колонии и что она о нём «помнит»**.
Роль своя у каждого игрока (это заодно решает старую проблему общей на всех репутации).

| Роль | Как её получить | Как ведут себя муравьи | Что может игрок |
|---|---|---|---|
| Чужак | по умолчанию | вдали не обращают внимания; у входа настороже; при контакте кусают | наблюдать издалека |
| Враг | ранил муравьёв или ломал гнездо | по аналогии с тревожным феромоном: нападают всей колонией, преследуют, «помнят» несколько дней | — |
| Кормилец | регулярно оставлял еду на тропах | терпят рядом с гнездом, не нападают, забирают еду | подкармливать, наблюдать вблизи |
| «Свой по запаху» | натёрся запахом колонии (предмет из материала её гнезда), как настоящие жуки-мирмекофилы | принимают как своего, запах со временем выветривается | спускаться в гнездо, изучать его изнутри |
| Опекун (будущее) | своя колония из пойманной матки в формикарии | — | управляет условиями (корм, влажность), а не муравьями |
| Оператор | права сервера | — | яйца призыва, команды |

**Инструменты косвенного влияния** (прямых приказов нет): подкормка; феромонная приманка (искусственный след), чтобы направить
фуражиров; перенос тлей на растения у гнезда; защита колонии от врагов.

## 10. Вражда через события

Отношения между колониями — это не дипломатия, а **запах** (свои и чужие; колонии одной суперколонии друг для друга свои),
**пересечение территорий**, **агрессивность вида** и **история стычек**. Всё развивается само.

| Событие | Реальная основа | Что запускает | Последствия |
|---|---|---|---|
| Пограничная стычка | встречи фуражиров соседних колоний | пересечение территорий | драки небольших групп, потери, граница сдвигается |
| Территориальная война | массовые сражения дерновых муравьёв | накопились стычки | большое сражение на границе; победитель расширяет территорию |
| Спор за источник пищи | борьба за крупную добычу или сладкое | ценная еда на границе | кто сильнее, тот забирает |
| Набег за куколками | муравьи-амазонки (Polyergus) грабят гнёзда Formica | вид-рабовладелец рядом с «хозяйским» видом | куколки уносят, из них вырастают рабочие налётчика |
| Нашествие кочевых муравьёв | колонны Eciton в джунглях | случайно в джунглях | временная орда проходит и грабит всё живое и гнёзда |
| Узурпация (позже) | матка-паразит (например, Lasius umbratus у Lasius niger) | молодая матка-паразит | чужая матка заменяет законную |
| Реакция на игрока | тревожный феромон | игрок ранил муравьёв или ломал гнездо | роль «Враг» |

## 11. Виды вместо культур

**Предложение для первой версии (4 вида):**

| Вид | Биом | Гнездо | Питание | Особенности | Зачем в игре |
|---|---|---|---|---|---|
| Чёрный садовый муравей (Lasius niger) | равнины, луга, сады | подземное, небольшие земляные холмики | падь тлей, насекомые, сладкое | рабочие почти одного размера; матка основывает колонию одна; массовые брачные лёты | базовый вид, на нём строится весь движок |
| Рыжий лесной муравей (Formica rufa) | тайга, хвойные леса | купол из хвои и веточек над подземной частью | падь тлей на деревьях, насекомые | брызгает муравьиной кислотой, территориальный, тропы к деревьям, собирает смолу; молодая матка захватывает гнездо Formica fusca | зрелищный купол и территории |
| Муравей-жнец (Messor) | пустыня, саванна, бесплодные земли | глубокое, с зернохранилищами | семена | мажоры дробят семена, запасы видны в камерах | наглядные физические запасы |
| Муравей-листорез (Atta) | джунгли | огромное подземное, с грибными садами и мусорными камерами | гриб, выращенный на нарезанных листьях | сильный полиморфизм (от крошечных до солдат), «шоссе»-тропы | самый сложный и эффектный |

**Позже:** древоточец (Camponotus, гнёзда в древесине), красный огненный муравей (Solenopsis invicta, агрессивный, жалит),
ткач (Oecophylla, гнёзда из листьев на шёлке в кронах), кочевые муравьи (Eciton, как событие), амазонка (Polyergus, набеги),
медовые муравьи (Myrmecocystus, «живые бочки» с пищей).

## 12. Версия

| Что | Версия на 25.09.2026 |
|---|---|
| Minecraft Java | **26.3** (релиз 15.09.2026), требует **Java 25** |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 (18.09.2026) |
| Fabric Loom | 1.18.2 |

- Проект создать через официальный генератор шаблонов Fabric для 26.3. Настройки сборки старого мода (1.21.11, Loom 1.16)
  не переносить: между 1.21.11 и 26.x сменилась схема версий и многое в API.
- Перед стартом ещё раз проверить, не вышла ли новая версия.

## Дополнительные решения (03.10.2026)

Из решений 1–12 возникли новые вопросы. Пользователь принял по ним мои рекомендации. Там, где рекомендации не было,
вариант выбрал я; такие строки помечены «выбрано мной», их можно пересмотреть.

| # | Вопрос | Решение |
|---|---|---|
| 13 | Масштаб муравьёв и вход в гнездо | **«Крупные, но реалистичные»**: рабочий около 1 блока в длину, солдат около 1,5, матка 2–2,5. Ходы и камеры высотой 2 блока, так что игрок может спуститься и изучать гнездо |
| 14 | Что вместо исследований | **Дерева исследований нет.** Колония «открывает» новое созреванием: мажоры, крылатые, новые камеры появляются по размеру и возрасту. Игрок ведёт полевой дневник: наблюдения открывают записи и снаряжение |
| 15 | Что игрок получает от колоний (выбрано мной) | **Игрок как натуралист:** полевой дневник и отношения с дикими колониями (роли, п. 9), плюс продукты муравьёв с настоящим применением (какие и зачем — решить в GDD v2). Своя колония из пойманной матки — возможное расширение позже, потому что сейчас колонии только дикие (п. 8). Защита территории — позже, если понадобится |
| 16 | Порядок видов | **Lasius niger → Formica rufa → Messor → Atta** |
| 17 | Лимит муравьёв | **30–120 взрослых** на колонию по виду и зрелости, настраивается |
| 18 | Выгруженные чанки | **Как в ванилле:** колония живёт только в загруженных чанках, при загрузке догоняющий расчёт |
| 19 | Разрешение текстур (выбрано мной) | **Блоки и предметы 16×16, как в ванилле:** гнездо встроено в ванильный рельеф, и разное разрешение там бросается в глаза. Текстуры муравьёв с удвоенной плотностью ради деталей анатомии. **Это отменяет требование 32×32 из старого проекта** |
| 20 | Брачный лёт | **По дождю**, раз в «год» (12 игровых дней) |
| 21 | Старый нереалистичный контент | **Убрать полностью:** жетоны, торговлю, дипломатию, броню и оружие, планшет |

## Что дальше

1. Выбрать модель (или связку моделей) для разработки и процесс приёмки.
2. Написать короткий GDD v2 на основе этого документа (он заменит описания старого мода в разделах 01–08).
3. Создать проект на 26.3 и начать с вертикального среза: **одна колония Lasius niger** — матка, выводок, рабочие, гнездо
   в земле, фуражировка, брачный лёт.


## Implementation notes - 2026-10-05 (T16 owner-authorized terrain and nestmate rules)

The latest owner terrain/pass-through decisions permit bounded removal of witnessed short native plants only in declared queen excavation columns, untouched witnessed mineral floor support, one-block supported exterior adaptation and a 64-column (offsets 4..11) search. Excavation remains 24 soil cells, a connected two-high chamber and intact dry shell; plants provide no material/food. Exact native generation authority is revoked by later writes. Matching queen/worker and worker/worker lineage pairs pass without pushing/cramming; foreign contacts, terrain, gravity, damage, death and occupied-body block placement stay physical.

Five automatic queens began protected soil excavation in all three fixed sample regions. Production overworld placement is therefore enabled by default as instructed; explicit opt-out is JVM option -Dprime_ants.naturalPlacement=false (legacy explicit setting compatible). This promotes early founding, not the complete unattended life cycle. Two native queens later stop conservatively on changed soil. Fresh full acceptance passes 15 unit/120 server/24 model cases; the two retained traffic reproducers recover without a wider throat. Details and limitations are in natural-placement-feasibility.md and T16/report.md; earlier design text remains historical.

## Implementation notes - 2026-10-06 (T21 owner-authorized food policy)

Physical crop sharing uses twenty loaded mouth-action ticks, up to 1,500 existing sugar and a 1,000 donor reserve, retaining work/cargo and separate protein. The owner-authorized prospective maintenance choice is one sugar prepaid for four covered loaded ticks, with persisted remainder and unchanged historical spending, lifespan, fasting grace and 39,000 founding budget. A descriptive 2,400-loaded-tick actual-intake window gates new laying against current adult/brood commitments and a 25% maintenance margin; unavailable data pauses growth. Physical protein stays available for larvae once the queen has an egg's 2,000 protein, rather than being trapped in her ingested store. Native survival and player evidence must be judged from the T21 report, independently of green component tests; no broad persistence, defense or performance claim is added.


## Implementation notes - 2026-10-06 (T22 actual-stock funding and local defense)

Recent receipts remain a separate laying-rate observation; only current stores fund existing brood commitments, a new egg/larva and the unchanged 25% maintenance margin. Sharing, physical costs, food values, adult lifespan/fasting and the 39,000 founding reserve are unchanged. T21 native survival is historical evidence under its earlier admission rule.

Actual accepted survival-player ant damage or a successful break of a currently owned component signals that colony. Mature registered workers respond within 12 blocks of the current harm anchor, pursue only its provoking player within 16, and bite at physical reach/visibility for one normal health point with a 20-loaded-tick cooldown. Alarm duration is 600 loaded ticks; repeated genuine harm may renew it. Ownership is captured before block mutation and consumed only after successful break. The existing worker owner suspends/resumes tasks and retains equipment/claims; no reputation, automatic repair or resource credit is introduced.

Fresh-client checks use ordinary vanilla overworld generation/survival, seed 2026100501 declared in advance, render/simulation 8/8 and no experiment FULL limits. Imported dimension-transfer recovery is excluded. The current report records actual acceptance/client results and their limits; performance, feeding and broad restart remain T23.

## Решения 22–28 (06.10.2026): фэнтези-слой, «цивилизация из экологии»

После выпуска 0.1.0 пользователь выбрал вариант «Millénaire на муравьях» с фэнтези-слоем. Подробности, игровой цикл и этапы —
в [`12-gdd-v2-civilization.md`](12-gdd-v2-civilization.md). Где эти решения расходятся с 1–21, главнее они.

| # | Решение | Что меняет |
|---|---|---|
| 22 | **Фэнтези-слой поверх реалистичного движка.** Правило: фэнтези можно, абстракции нельзя. Всё, что есть у колонии, физически лежит в мире; у каждой системы есть исход; у каждого эффекта один владелец | смягчает «главный принцип: реализм»; решения 1, 4, 5, 6, 18 остаются в силе |
| 23 | **Развитие стадиями:** основание → молодая → зрелая → великая колония. Переход по реальным порогам (население, построенные камеры, запасы на складах), возможен откат назад | уточняет 14: дерева исследований и «очков знаний» по-прежнему нет |
| 24 | **Камеры с назначением и уровнями** (земляная → глина/смола → камень) и **наземные постройки по планам гнёзд**; лимит взрослых растёт со стадией до 120 | уточняет 6 и 17 |
| 25 | **Добыча ресурсов:** камень, глина, гравий, песок; руда (уголь, медь, железо, позже золото) как физический ресурс | меняет 5: руда возвращается |
| 26 | **Ремесло и броня:** кузнечная камера, плавка, видимая броня на солдатах, которая реально меняет бой | меняет 21: броня возвращается для муравьёв; игрок может получить муравьиную броню через обмен |
| 27 | **Дипломатия:** отношения между колониями (вражда → настороженность → нейтралитет → дружба → союз) от событий и действий — послы с дарами, дань, союзы, войны | меняет 10 и 21; дипломатия держится на дарах и поступках, без валюты |
| 28 | **Игрок:** отношение у каждой колонии своё, обмен натурой на изделия колонии, гостевой дом от колонии-союзника | дополняет 9 и 15; жетоны, валюта, лавки с ценами и планшет-пульт по-прежнему убраны (21 в этой части в силе) |

Порядок работ: этап 1 «Развитие и дома» (20 ходов turnloop) → этап 2 «Шахты, кузница, броня» → этап 3 «Дипломатия и игрок».

## Implementation notes - 2026-10-07 (stage-1 T01 colony stages)

Decision 23 is implemented as one live rule table, `dev.primeants.colony.StageRules`. The model and the extension plan are in [stage-1-design.md](stage-1-design.md).

**Counting rule.** Every stage cap and adult threshold counts living adults including the queen. Brood reserves cap space exactly as in 0.1.0: laying is refused at workers + brood ≥ cap − 1, and emergence at workers + queen ≥ cap. The caps stay **5 / 30 / 60 / 120**, so 0.1.0's 30 keeps its meaning.

**Deadlock fix.** The GDD's "Young: 5+ workers" was unreachable under a five-adult Founding cap, which holds at most four workers. The default fix is adopted:

- **Young** needs **5 adults (queen + 4 workers)**, a confirmed nursery and a confirmed food store.
- **Mature** needs 25 adults, a queen's hall, a material store, 4 stored food units and 16 clay units.
- **Great** needs 50 adults, all four functions at tier 2 or higher, and 32 stone units, counted as stored or laid in the colony's own tier-3 walls.

Stages are cumulative, and the stock numbers are starting values. Invariant: each next threshold fits under the current cap (5 ≤ 5, 25 ≤ 30, 50 ≤ 60), including right after a regression. It is checked at class load and by unit tests.

**Only live state counts.**

- A function needs its owned marker (brood pile or cache) in an open, enclosed chamber.
- An adult needs a found, living body. A failed lookup is unknown: a colony is promoted only when confirmed state meets the stage, and demoted only when even its unknowns could not.
- A lower cap never removes adults.
- 0.1.0 founding chambers are recognized as built (tier 1, nursery and food store), with no terrain change.
- Mature and Great stay unreachable until later turns add queen's halls, material stores and tiers.

**Configured limit.** `prime_ants.colonyAdultCapacity` is now an upper bound (decision 17): default 120, valid 4..120, and the effective cap is min(stage cap, bound). A bound below a threshold deliberately holds that stage.

**0.1.0 migration.**

- New saves write `ColonyAdultCapacityBound` on the queen and `AdultCapacityBound` on the pile.
- A 0.1.0 save's `ColonyAdultCapacity` or `AdultCapacity` is read only when the new key is absent, and is still validated 4..30.
- It migrates once. A saved 30, the 0.1.0 default and maximum, becomes 120, so the colony can pass 30 once Mature is reachable. A saved 4..29 is kept as a deliberate reduction.

## Implementation notes - 2026-10-07 (stage-1 T03 first dug chamber)

Decisions 6 and 24 now have concrete numbers. The nest plan (`dev.primeants.founding.NestBlueprint`) is a declarative table of rooms and passages in the founding nest's frame, compiled into a dig-task queue that real workers carry out block by block; it never places blocks.

- **Bounds.** Every planned and shell cell lies within 10 blocks of the entrance forward or sideways, and from 3 blocks below the entrance level up to that level. One placement digs at most 32 cells. The 0.1.0 widening keeps its own radius 6 and depth 3.
- **First chamber.** A Young colony that certainly lacks a material store digs a two-high 3x3 store room and a passage through the founding chamber's back wall (24 cells), on the left or right of the passage, keeping the other side free for the queen's hall. Only witnessed natural or colony-prepared soil is dug; the 0.1.0 widening goes first.
- **Confirmation.** A fault seen in loaded blocks now beats unavailable terrain in every chamber check and in brood care, so an unloaded cell can no longer hide observed damage. Only reason labels changed for mixed cases.

## Implementation notes - 2026-10-07 (stage-1 T04 queen's hall and material hauling)

- **Store capacity.** A tier-1 material store holds 32 units. Sixteen of them are kept for clay, so Mature's 16 clay always fit beside up to 16 units of stone, gravel, sand and ore. For the stage, clay and stone count as known only in a confirmed store; an unknown store adds 32 clay or 16 stone to the possible count only.
- **Units.** One item is one unit: clay ball (clay); cobblestone and stone (stone); gravel; sand; coal, raw copper and raw iron (ore, kept for stage 2). Nothing else is accepted. A forager hauls only items lying on the ground in its search area, one per trip, and only once its trip found no dropped or native food.
- **Queen's hall.** It is the 0.1.0 widening side the colony's widening did not take: three by two columns, two high, 12 cells, dug from the queen's own chamber, so the queen and her brood pile never move. It registers once as its own tier-1 chamber without a marker block; it counts while its cells and the habitat are intact and the living queen is settled inside her room.

## Implementation notes - 2026-10-07 (stage-1 T05 nursery upgrade and brood capacity)

- **Walls and tiers.** A chamber's wall cells are the horizontal faces of its open cells that the nest plan never opens; floor and roof are not walls (the floor may be witnessed stone, the roof is the surface layer). Its confirmed tier is the lowest tier of its wall cells, read live: anything solid counts 1, the colony's own packed clay or resin masonry 2, its own nest-cut stone 3. A block a player places never raises a tier. The founding chamber, which holds the nursery, has eight wall cells.
- **Upgrade work.** Mature unlocks tier 2 (Great tier 3). One worker at a time takes one clay ball from the confirmed store, carries it and rams it into the next wall cell's own earth: one clay per packed-clay cell, and no soil goes to the mound. A cell that is no longer natural or colony earth stops the job. Clay in the colony's own walls of a confirmed chamber, and in the builder's mandibles, keeps counting toward Mature's 16 clay.
- **Brood capacity.** At default timing (144,000-tick adult lives, three 12,000-tick brood stages, one egg per 1,200 ticks) a nursery sustains slots x speed x 4 workers, and never more than 120 (the laying limit). The founding chamber gives 3 slots at x1 (12 workers, enough for Young's 5); the queen's hall adds 4 slots (28 workers, enough for Mature's 25); tier-2 nursery walls add 3 slots and x1.5 (60 workers, enough for Great's 50); tier-3 walls, built from T06, add 8 more slots and x2 (120 workers, Great's cap). Speed applies to running and new brood stages alike.
- **Store display.** The material store shows a clay heap of up to eight levels, a heap of other stock of up to four, four units a level, and one lump for each other material present, so two stores whose totals differ by eight units or whose materials differ always look different.

## Implementation notes - 2026-10-07 (stage-1 T06 stage mound and food shares)

- **Stage mound.** Soil dug out of nest-plan rooms (the material store, the queen's hall, later rooms) is laid on the mound plan of the colony's current stage, its first plan-based surface structure. The plan uses the old blueprint's tiers primitive (stacked elliptical truncated cones) and lies behind the entrance in two lobes, one either side of the approach lane; the lane, the exterior standing spot and the stairs stay clear. Young: two lobes three layers high, a footprint 15 columns wide (side -7 to 7) and 8 deep (forward -8 to -1), 98 columns and 214 cells. Mature: two lobes five layers high, 23 columns wide (side -11 to 11) and 13 deep (forward -13 to -1), 242 columns and 798 cells, containing the Young mound. Great keeps Mature's plan for now. A founding colony has no plan: its 0.1.0 founding deposits and the 0.1.0 widening keep their own lists, inside the Young lobes.
- **Mound cells.** Each column sits on its own ground: the highest witnessed natural soil or stone within two blocks of the entrance's level. A cell takes soil only if it is air or a witnessed short native plant, which is buried without drops, and rests on natural ground or the colony's own mound soil. Fluids, blocks a player placed, block entities, logs, leaves, crops and any other block stop the column beneath them and are never covered or removed. A room is planned only if the plan's free cells hold its soil; a builder with nowhere to lay keeps its soil and waits. Mound soil is the colony's own nest soil, a counted stock; nothing removes it when the colony regresses.
- **Food shares.** The six-slot food cache keeps two slots for each kind of food: it holds at most four sugar units (apples, berries, nectar) and at most four protein units (chicken, rotten flesh, prey). A forager picks up dropped food only while its kind is below that share. Caches saved before this change load as they were.
- **Upgrade ledger.** An upgrade job counts the units dead builders released to transfer custody cumulatively (its equation: taken = carried + built + released) and names each release's custody transfer, so the units still in custody now are a separate figure.

## Implementation notes - 2026-10-07 (stage-1 T07 stage hysteresis, foragers, plug soil and legacy caches)

- **Stage hysteresis (the owner's decision of 2026-10-07).** Promotion stays immediate. A stage the colony holds drops only once one of its requirements, or a lower stage's, has been certainly unmet for 24,000 loaded ticks in a row. Each requirement has its own unmet-since clock, counted in the colony's nursery's loaded ticks and saved with the colony's chambers and stage, so it survives save and reload. Shortfalls that wait for their clock: food, clay and stone; a chamber still confirmed below the tier a stage needs; adults below the held stage's threshold but not below the threshold of the stage beneath it (a Young colony's queen alone is not below Founding's one adult). Losses that apply at once: the queen observed dead; a chamber function observed absent, held by no confirmed or unknown chamber (a breached shell, a missing or foreign marker, the hall without its living queen); adults certainly below the threshold of the stage beneath the one held, which drop the colony to the stage its adults support. Unknown terrain or members never demote and run no clock; the clock starts again once the shortfall is certain.
- **Foragers.** A colony keeps one forager for every ten living workers, and at least one: up to 19 workers exactly one, as 0.1.0's test colonies (at most ten workers) and stage-1's earlier test colonies (at most seventeen) do; 20 to 29 workers two, 30 to 39 three, five at Mature's cap of 60 adults and eleven at Great's 120. A further forager is claimed only while the colony keeps two caregivers without it and, unless a builder already works, one more worker for the builder slot. A further forager that comes back from a trip when the colony no longer keeps it returns to the colony's care. Each forager carries its own cargo, and every claim is saved with the queen. The T22 growth gate is unchanged.
- **Plug soil.** When the queen's founding deposit list is full, the forager that opens the nest lays the two plug units on the colony's stage mound plan; a Founding colony has no plan of its own and uses Young's, its first. Founding-site validation (at least 22 deposits) is unchanged.
- **Caches saved before the food shares.** A cache that holds more of one kind than its share sheds the units beyond the share when the other kind arrives: they leave through transfer custody onto the chamber floor beside the cache, where they stay in the world until their kind has room again, and the arriving unit gets in.


## Implementation notes - 2026-10-08 (stage-1 T08 chamber tier two)

- **Catastrophic interpretation.** Observed queen death independently forces Founding/cap five at the next production
  evaluation, clears grace clocks and prevents promotion, even with five surviving workers and intact nursery/cache.
  Catastrophic population loss retains immediate regression to the stage supported by remaining adults; the owner may
  override this interpretation. An unavailable entrance/plug is unknown with an observed living queen; loaded damage wins.
- **Physical ownership.** Founding walls serve nursery and food store through one upgrade job; material store and hall
  follow. Shared physical walls count once, each converted with one clay fetched from the confirmed store. One builder
  and two retained caregivers, player-wall preservation, saved claims/cargo/work and exact custody accounting remain.
  A low store heap permits a supported diagonal work stand only with actual ray visibility and the existing reach.
- **Tier-two numbers.** Nursery retains +3 slots and ×1.5 development. Food capacity 6→12, reservation 2→4 per kind;
  material capacity 32→64, clay reservation 16→32. Hall base cadence 1,200→600 loaded ticks uses the existing time multiplier
  and live confirmed tier. Brood capacity calculations agree. Food gates, costs, nutrition, stage caps/thresholds,
  founding reserve/site validation and adult lifespan/fasting remain unchanged.
- **Inventory and display.** Canonical inventory format 2 extends only the declared ranges; old unversioned format 1
  retains old-range validation. Unknown terrain and capacity loss never truncate contents. Overfull stores wait; food
  share shedding uses physical, exactly-once custody. Food total plus six kind samples gives 832 runtime states/eighteen
  model parts; material clay 0..16, other stock 0..8 and four samples gives 2,448 runtime states/twenty-nine parts. Equal
  material projections preserve kind sets and totals within seven units. Meaningful asset and 16×16 guards remain.
- **Evidence boundary.** q1 and restored f2 each passed with two identical concurrent copies in attempt 2. r22's XML,
  exit metadata and milestones prove three safety passes; they passed fresh revalidation after the shared-fixture edit. Deterministic
  five-record serialization remains separate from natural growth. Fresh tier-two original and two copies completed all
  unchanged acceptances at 51,328 / 55,689 / 53,127 ticks, versus r23's original pass and two 60,000-tick timeouts. A finite
  player-style twelve-apple wave after stocks fall below 24,000 restored actual sugar receipts for the stalled colony;
  production growth/care/cadence and acceptance predicates were not changed. HallAvailability's original and two copies
  passed the 20,000-tick bound, 600-loaded-tick unknown interval and two restored confirmations, with both actual hall
  clock suffixes checked. Full-build and older-regression results are recorded separately in T08's report.
- **Retention.** Fresh server UUIDs retain the newest ten direct runs, current included, with Gradle leases/session locks
  protecting active runs and escaping/link/reparse paths rejected. Non-run entries, client worlds and evidence are untouched.
  The existing legacy client deletion action is skipped for server runs. The prior disposable selection/junction/lock
  fixture and initial cleanup evidence are retained; only automatic rolling retention runs now. Console evidence is
  explicitly UTF-8; prior UTF-16 originals remain preserved.
- **Next scope.** Placement-survey walking-route/dig-timeline exclusions and the unattended pre-Mature clay gap remain.
  Mature-gated mining does not fill the earlier supply gap. T09 prioritizes mining/player contributions and precisely
  diagnosed reliability carryovers; tier three and Great remain after mining.

### Implementation validation addendum - 2026-10-08 (T08 forager recovery)

The first unfiltered T08 build executed all 236 server/94 unit cases but failed f2 at 30,000 and f3 at 60,000 ticks; its model task did not execute. A full protein share retained a forager's food cargo; f3 also plateaued in Young at 21 adults with a full cache before a late clay drop. The fixture now opens with three apples/two chickens and counts all ground/cache/worker-cargo/custody food before finite refills, leaving one actual delivery slot and kind margins. It offers f2's same eight clay only when both real claimed foragers are empty and searching. No cargo, AI, nutrients, stage/growth rules, assertion or bound is changed. An intermediate recovery still missed overlap in one copy despite storing all eight clay. The final targeted recovery passes all three identical f2 bodies (ends 19,853 / 20,004 / 20,027), and f3 reaches 31 adults at 33,317 (Mature 28,083). The additional unfiltered build follows this positive recovery; its result is recorded in T08's report. Tier-two/safety production dependencies are unchanged by this test-only repair; their full-build and concurrent passes remain separate evidence.

## Implementation notes - 2026-10-08 (stage-1 T09 bounded physical mining)

- **Concrete mining limits.** Horizontal Euclidean radius **16 blocks from the entrance**, edited cells **1–6 blocks below
  entrance level**, at most **64 new mining excavation cells per colony**, including connector soil. Existing founding,
  widening and chamber bounds are unchanged. This first conservative compiler supplies one connected, supported,
  two-high route beyond the store passage (forward 8..15, side zero, depths one/two), at most sixteen cells; it does not
  search arbitrary deposits or replan a partially dug stopped route.
- **Deterministic units.** One successfully removed natural resource block produces one carried item, with no vanilla
  loot as another producer: **stone → cobblestone; clay → clay ball; gravel → gravel; sand → sand; coal ore → coal;
  copper ore → raw copper; iron ore → raw iron**. Ore remains stockpiled without processing. Connector soil remains dirt
  cargo on the physical soil-to-mound path and joins the existing soil ledger.
- **Authority and work.** Only a held Mature/Great production evaluation admits mining. Separate `NaturalMaterials`
  observations at genuine witnessed ProtoChunk-to-LevelChunk conversion cover the surface through sixteen blocks below;
  they do not broaden `NaturalSoil` eligibility. Loading, old/retrogen terrain and geological appearance grant no mining
  authority. Later writes, including same-state replacement, revoke it. A real claimed worker approaches, performs a
  twenty-loaded-tick action, carries one unit and deposits within physical reach of the confirmed owned store. Loaded
  state, expected block, positive origin, bounds, budget, ray reach, exposure, support and room are rechecked at removal.
  Gravity stays enabled; falling roofs and survival violations are refused. Unknown or changed availability holds work
  and cargo. A lower held stage stops new removal while ordinary authorized delivery remains possible.
- **Priority and persistence.** One exclusive builder and two retained caregivers, existing excavation/upgrade priority
  and food foraging remain. An empty miner yields to available upgrade work; a worker on the surface then walks through
  the existing stair before fetching clay. Plans, claims, successful edits, deliveries and named death releases are saved,
  without abstract mining income. Current ground/carried/store/custody/incorporated units are counted independently of
  cumulative history; clay wall incorporation and connector soil are included. Only actual declared worker openings join
  integrity checks. Planning inspects a bounded fixed queue on a slow cadence; mining terrain is never force-loaded.
- **Evidence boundary.** Permanent mining/interruption bodies keep 60,000 total ticks, negative windows 600 nursery
  loaded ticks, restoration two fresh production evaluations plus actual work, and settlement 200 loaded ticks. Test-only
  food openings change 12 apples/10 chickens to three/two for new mining fixtures, with counted finite later waves;
  all legacy supplies and assertions remain. Unavailable-caregiver, preparation, handoff and other diagnostic results
  remain explicit in T09's report. Mature-gated mining does not resolve unattended pre-Mature clay supply. Placement
  survey walking routes/dig timelines, unattended development and arbitrary deposit reliability remain uncovered.

## Implementation notes - 2026-10-09 (T09 strict interruption diagnosis)

- **Preserved physical scope.** Radius sixteen, depths one-six and sixty-four new mining edits including connector
  soil remain the runtime guards. The supplied fixed gallery still exercises sixteen actual cells, fourteen resource
  units and two mound-soil units; it does not establish excavation of the whole sixty-four-cell budget or arbitrary veins.
  The seven deterministic natural-block/item mappings and unchanged soil authority are in the preceding T09 notes.
- **Unknown habitat.** The next face is also the shell of the already excavated connector. Its unavailability correctly
  makes the connected habitat unknown and pauses nursing authorization. Fresh diagnosis records zero active caregivers
  but twenty-seven saved nursery roles in the latest unfiltered run (the earlier isolated diagnosis had thirty-one);
  the unchanged two-active-caregiver assertion fails. It is not silently reinterpreted
  as retained roles. The permanent failure and its incomplete 600-tick window remain. Later unavailable/foreign-store and
  same-state-replacement phases in that body are unverified. A new shape guard confirms that this unknown cannot hide a
  separately loaded gallery breach. No production protection or acceptance assertion is relaxed.
- **Recovery evidence.** The main gallery and two identical isolated concurrent copies pass at 45,444 / 46,766 / 54,041
  total ticks with every material, exact custody/soil/clay-wall accounting, confirmed functions and two-hundred settled
  loaded ticks. The last copy performs the surface-soil/available-hall-upgrade handoff and continues actual work. The
  tier-two diagnostic passes at 49,146, including four natural brood, normal reload/first resumed step and 600 unknown
  loaded ticks plus two restored evaluations. Separate safety and unfiltered build outcomes are recorded in T09's report.
- **Restored-recipe evidence.** Final targeted main copies pass at 46,049 / 46,371 / 48,638; tier two at 51,452 and
  reload/death at 46,790, with current stone stock eight, ground/carried/custody zero and the release entity absent.
  r02's successful gravity and stage-regression windows remain separate evidence. The restored targeted full-store
  preparation timed out, then passed in the unfiltered r06 build at 56,913. All fifty mining assertions and all original bounds stay identical; only diagnostic
  observers and the shape guard remain in the recovery diff. Fresh unfiltered acceptance is reported independently.
- **Abandoned supply experiment.** A temporary fixture-only late hall-wave trigger 24,000 to 8,000 retained the same
  twelve physical apples, initial supplies, production gates, nutrition and bounds. The r01 natural-four/reload recovery
  occurred during unchanged work meals before any late hall wave, so it did not establish a causal repair. r02 added a
  final death-unit recovery failure and a 60,000-tick full-store preparation timeout. The accepted 24,000 trigger was
  restored before final recovery and acceptance. No supply change remains in the delivery. A test-only observer records current
  ground/carried/store/custody, transfer-entity state, food cache and real foragers at the unchanged death endpoint.
- **Limits retained.** Mature-gated mining does not resolve unattended pre-Mature clay supply. Placement-survey walking
  routes/dig timelines remain uncovered; supplied fixtures do not establish unattended development or arbitrary deposit
  reliability. Tier-three construction, Great growth acceptance and release work remain deferred.

### Closeout addendum - 2026-10-09 (T09 retry)

The recovered r06 build executed 244 server cases (243 passing; strict caregiver failure at 43,175) and 98 passing unit
cases in 7,574.56 seconds, exit 1. Source stayed unchanged. Its model XML predates that invocation and is not fresh model
execution. Gallery, tier two, reload/death, full-store backpressure and contributions passed supplied scenarios; T09's
report records the retry independently. Unknown gallery terrain still pauses nursing authorization while the miner
retains its claim. The unanswered requirement is preserved: two currently authorized caregivers, not merely saved roles.
Later store/foreign-store/worker replacement phases in the failing body remain unverified.

Public client evidence now serializes repository-relative capture/save paths and placeholders for external inputs;
absolute runtime properties remain rooted for the client's different working directory. The build checks newly indexed
and existing tracked text for profile absolute paths without printing their contents, and preserves binary assets.
Model/validator tasks precede the server workload so even a red build can record fresh model execution. Acceptance names,
assertions and bounds are unchanged. No surface-structure checkpoint or semantic caregiver repair is included.

## Implementation notes - 2026-10-09 (T09 owner caregiver decision, 15:25)

The owner decided that during an **unknown habitat** pause, living colony members retaining valid nursing roles count
toward the requirement for at least two caregivers; their work remains paused. This supersedes the formerly unanswered
interpretation recorded in the strict-interruption and retry notes above. Historical evidence and those notes are retained.

The fixture uses `Findings.Verdict.UNKNOWN`, so loaded damage retains precedence. It counts real loaded, ticking, living,
mature, enabled members with the correct brood lineage, home and nursing task plan, an owned operational nursery and
living physically present queen, excluding every forager, digging/mining builder and upgrade claim. It requires ordinary
authorized caregivers again after restoration and keeps that predicate for `DAMAGED` habitat. Production nursing,
feeding, excavation, upgrade and delivery authorization is unchanged; unknown terrain grants no work authority.

The targeted recovery completed the two unknown windows and their restored evaluations, then exposed the prescribed
observed-damage stop condition at tick 42,849. A loaded foreign-owned store is a revoked completed chamber opening
(`material_store_completed_opening_revoked`), hence `DAMAGED`: nursing authorization pauses while the miner retains
one clay ball and its existing claim. The ordinary two-authorized-caregiver assertion remains red. The recovery stops
without extending the decision to damage or changing existing production handling; later interruption phases are unverified.

## Implementation notes - 2026-10-09 (T10 owner damage-pause decision)

The owner decided that while loaded damage pauses all nursing, an existing miner retains its cargo and claim until
habitat restoration even with zero authorized caregivers. The two-caregiver admission guard prevents taking nurses
away for work; no worker is newly taken during this pause. This supersedes the historical unanswered damage
interpretation in the T09 notes above, which remain preserved.

The expectation changes only in the foreign-store interruption phase. Its snapshot proves prior productive mining,
the same living worker, claim and exact cargo, loaded DAMAGED habitat, foreign-store and nursing nonauthorization,
unchanged removal/delivery/release history, no replacement builder or nurse diversion, one-builder exclusivity and
all physical food, clay, soil and mining ledgers. The shared UNKNOWN nurse qualifications and loaded-damage precedence
remain unchanged. Ordinary authorized caregivers apply again immediately after restoration, with two fresh production
evaluations and actual retained-miner delivery before the same-state replacement phase. Production handling is unchanged.
The permanent interruption now observes a genuinely resumed Mature mining action before replacing its next target;
a delivery below Mature alone is insufficient. Its replacement observation continues after the empty claim clears,
correcting a fixture completion guard that skipped the negative window. Fresh recovery finishes at 44,755 total ticks:
four separate 600-nursery-loaded-tick windows, two fresh restored evaluations and 200 settled ticks. During foreign
damage the same living miner holds one clay ball and its claim with zero authorized caregivers, unchanged removal /
delivery / release counts 5 / 4 / 0; restored delivery is observed with 30 authorized caregivers. All physical ledgers hold.

The instrumented full-store original offered 32 stone at 28,711, first picked up at 45,644 and filled its share at 55,782;
upgrade completion was already 42,698. The supported contribution moves from forward -5 to -1 on the existing clear
approach lane, at the same nursery-tier trigger. Quantities, one-unit trips, food/clay waves and the 24,000 hall trigger
remain unchanged. Original and two identical concurrent copies finish at 56,141 / 54,110 / 55,527 with all frozen windows.
The three eighteen-unit food bodies also pass; the historical 10,323 cause remains unresolved. The broader diagnostic
exposes untouched regression/death endpoint assertions and a gravity preparation timeout. Their negative evidence remains
in T10's report; no further assertion relaxation, production change or deadline extension is claimed.
Fresh T10 dependency revalidation is separate from the passing mining interruption/full-store bodies. The isolated
tier-two body times out at 60,000: all four functions are tier two by 41,738, but natural expanded brood, fractional
reload, faster hall laying and the later unknown/restoration window remain unobserved. At 59,000 it holds Mature,
41 adults, no brood, eight food units and 36 stored clay; the recent-income gate is closed. Its accepted recipe,
24,000 hall trigger, growth/care predicates and bound are preserved. Store-upgrade safety passes fresh. Final unfiltered
acceptance and any unresolved red results are recorded independently in T10's report. No further expectation is relaxed.

## Implementation notes - 2026-10-10 (stage-1 T11 mining recovery)

- **Safety and assignment.** Actual production consideration refuses natural stone beneath sand before worker selection.
  An empty yielded claim need not be reacquired for that refusal. A scoped test observer returns the original guard result
  unchanged. Assigned-miner gravity coverage now prepares real chamber upgrades first, using only remote unopened-cell
  unavailability; habitat, origins, supplies and completed work remain physical. A separate declared first unsafe face
  proves refusal with an eligible living worker and two authorized caregivers, without an assignment. Both original/copy
  sets retain 60,000 total ticks and complete 600 negative plus 200 settled nursery-loaded ticks with intact blocks/ledgers.
- **Full-store preparation.** The same 32 cobble are offered at the same nursery-tier trigger/location. Only their pickup
  waits for the live store's actual tier-two capacity; each unit remains on the ground and is then normally hauled. The
  diagnosed tier-one share had trapped two cobble carriers while upgrades waited for clay. Smallest recovery and permanent
  mining revalidation pass every unchanged backpressure, release, restored-evaluation, cargo/delivery and settlement check.
  Food/clay waves, the 24,000 hall trigger, production priorities and all substantive assertions remain unchanged.
- **Evidence limits.** Regression and tier-two original/two-copy campaigns pass; their historical causes remain unresolved.
  Three death bodies fail immediate recovery after gallery completion with seven stored/one named ground cobble, zero
  pending and eight conserved. Later real foragers restore eight stored. No deficit is demonstrated; the fixture couples
  independent completion times. The frozen assertion is retained, not replaced by a wait. A later original pass is separate
  evidence. The eighteen-unit historical failure is still unexplained. No production/growth/nursing/terrain-policy change,
  unsafe assignment or deadline increase is included. Final build outcome and exact fresh discovery belong to T11's report.
  Pre-Mature clay, arbitrary veins/all64 edits, survey walking/dig timelines and unattended development remain uncovered;
  surface construction stays deferred and T12 is at risk.

## Implementation notes - 2026-10-10 (stage-1 T12 bounded surface work and death ordering)

- **Owner's death ordering.** Gallery completion is a frozen milestone followed by at most 6,000 nursery-loaded recovery ticks inside the unchanged 60,000 total. The original 600 unavailable-custody and 200 settled windows, reload/lethal/repeated-death/claim/custody/removal/delivery assertions remain. Read-only test wrappers forward the actual pickup hand write and owned-store deposit once; every canonical hand write invalidates the active source identity. Named pickup and deposit, rather than stock totals or entity disappearance, are required. The isolated original passes with early recovery; a concurrent copy passes after 2,237 recovery ticks. The cohort's original and second copy time out in earlier preparation/remaining mining. T11's 47,187 / 47,078 / 46,424 negatives remain: eight conserved cobble at those endpoints establish an event-order conflict, not demonstrated material loss. Existing mining priorities remain.
- **Plans and recipe.** Bundled semantic earth plans reuse the mound/IR/shape approach. Young's 214-cell and Mature's 798-cell spoil envelopes remain capacities. Mature desires 19 paid cells: supported three-high open entrance arch and one four-high mound crest with one-block steps. Great desires 57 unique cells including Mature, two paid gate conversions, connected two-high ramparts and two five-high posts with one-block step access. Bounds remain back fourteen, side twelve, six structural layers, ground search plus/minus two; new structural locations stay below sixty-four. Dimensions, costs and queue order were frozen before physical execution. Each structural block incorporates one recovered soil unit, including each mound-gate jamb. Gate masonry is compacted earthen structure around the open two-high passage; conversion keeps the soil already paid at that location. It consumes no additional unit or mining stone/clay.
- **Physical owner.** One ordinary worker shares the existing builder, retaining two caregivers and all earlier food/dig/upgrade/mining priorities. It takes only a currently owned removable mound unit, retains canonical mandible cargo, walks to an actually supported stand and acts for twenty loaded ticks. Loaded state, ownership, stage, support, body collision and the same ray/reach checks are revalidated. Sources under other blocks or bodies and protected access are refused. Previously selected supported goals persist through normal stair jumps and normal actor/data reload; their current validity remains checked. A surface-only 1,200-controller-tick route limit releases an empty stalled builder or names and preserves its carried unit in custody; it does not widen any acceptance deadline. Unknown terrain pauses, same-state player writes revoke permission and a blocked essential component cannot complete. A regression stops newly admitted work while already paid blocks remain. Descriptive plan/binding, actual receipts, claim, cargo and named releases persist. Historical excavation deposits remain unchanged when deposited soil moves; current soil locations include surface soil and gate units.
- **Measured partial delivery.** Controlled starting habitats declare real registered adults, actual tier-two chambers/stores, eighty owned soil units and finite existing food. Production computes their stage; scoped initial opening ownership grants no excavation, upgrade or surface work receipt. A first nursery initialization failed before actions; its original identities now refer to three actual declared initial adults. The first placement exposed a receipt-index crash; the one-index correction precedes focused recovery. The cached-stand repair prevents treating a normal airborne path query as a missing work stand. Fifteen actual Mature placements then conserve all eighty units, but the next raised mound step has no reachable stand in the declared habitat. Complete Mature/Great forms are cut at this measured component; the arch lintel, finished crest, gate conversions, ramparts and watch posts are unobserved. The supplied founder reaches real Mature, but its first physical attempt builds zero surface units. No footprint, cost, reach, supply or deadline is widened and no completed form is claimed.
- **Protection and reload boundary.** Same-state target revocation survives actual disk reload through 600 loaded ready ticks and 200 settled ticks, with zero recovery/placement and the empty builder released. The separate protection body observes 600 target-unavailable ticks, two fresh restored confirmations, actual hauling disk/data and canonical actor reload, resumed placement, lethal worker death, one named soil release and a replacement hauling worker. Construction/settlement is incomplete at 12,000. Final unfiltered execution and dependency checks are recorded separately in `<turnloop>/directions/prime-ants-stage1/turns/T12/report.md`.
- **Limits retained.** Historical regression/tier-two timing causes and the eighteen-unit failure at 10,323 remain unresolved. Mature mining still cannot supply unattended pre-Mature clay. The fixed sixteen-cell gallery proves neither arbitrary veins nor all sixty-four edits; placement-survey walking/dig timelines and unattended development remain uncovered. Tier three, established colonies, 120-adult performance, broad persistence and release acceptance require later turns. The T12 surface checkpoint is partially delivered.
