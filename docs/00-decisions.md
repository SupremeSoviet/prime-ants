# 00. Решения для перезапуска

> Приняты 25.09.2026: ответы на 12 главных вопросов перезапуска и то, что из них следует.
> **Этот документ главнее разделов 01–09.** Те описывают старый мод, здесь задано направление нового. Где старые разделы
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

GDD v2 написан ([12-gdd-v2-civilization.md](12-gdd-v2-civilization.md)). Первая колония Lasius niger (матка, выводок,
рабочие, гнездо в земле, фуражировка) вышла в 0.1.0, развитие колонии по стадиям — в 0.2.0. Ближайшие планы — в
[README](../README.ru.md).


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

Порядок работ: этап 1 «Развитие и дома» → этап 2 «Шахты, кузница, броня» → этап 3 «Дипломатия и игрок».

## Implementation notes

These notes cover implementation work from 2026-10-05 to 2026-10-11. Each subsection gives the rule as the code implements it now; where a rule changed over time, only the final rule is described. Times are in loaded ticks: game ticks counted only while the chunks involved are loaded (20 per second). Positions in the nest use the nest's frame: forward along the nest direction from the entrance (negative values lie behind it, where the mound is), side positive to the right of that direction, and height relative to the entrance level.

A block counts as natural only if the mod saw it in freshly generated terrain ("witnessed"). Any later write to that block, even one that leaves the same block in place, cancels this. Old, upgraded or retro-generated terrain is never witnessed.

### Founding and natural placement

- A founding queen digs 24 soil cells: the entrance stairs and a connected chamber two blocks high, inside an intact, dry shell.
- She digs only witnessed natural soil. Short natural plants (grass, ferns, small flowers) standing on the soil she digs may be removed, but only in her own excavation columns; they drop nothing and give no material or food.
- Natural stone is allowed only as untouched support under the chamber floor, never as a dug cell, wall or roof.
- Behind the entrance, the standing spots and the cells that receive her soil may sit one block higher or lower than planned, as long as each stands on natural soil and connects to the entrance. A site needs at least 22 such deposit cells; she keeps the last two soil units to close the entrance passage with a two-block plug.
- If soil in her plan is replaced, or loses its natural status, before she digs it, her founding fails rather than dig changed terrain.

Natural placement is on by default in the overworld. Turn it off with the JVM option `-Dprime_ants.naturalPlacement=false`; the older `prime_ants.experimentalNaturalPlacement` setting is still read when the new one is absent. Only freshly generated chunks are candidates, and the site search tries 64 columns at offsets 4–11 inside the candidate chunk. Pending placements saved by earlier versions keep their original 8-column search. Natural placement starts a colony's founding; it does not by itself carry an unattended colony through its whole life cycle (see the clay note under Mining).

### Nestmate contacts

- Ants of the same colony (queen and worker, or two workers) do not push each other and take no cramming damage from each other.
- Everything else stays physical: ants of other colonies, terrain, gravity, damage and death.
- A block is never placed into a space where any body stands, nestmate or not.

### Food sharing, upkeep and laying

**Crop sharing.** A worker with more than 1,000 sugar in its crop can feed a fasting nestmate of its own colony (a worker or the queen) within 16 blocks. The two must stay in mouth-to-mouth reach for 20 loaded ticks; then up to 1,500 sugar moves, never leaving the donor with less than 1,000 and never filling the recipient past 8,000. A recipient qualifies when it holds less than 1,500 sugar and has fasted for at least a quarter of its fasting grace. Only sugar is shared; protein stays separate. Sharing interrupts the donor's work without changing its role or what it carries.

**Upkeep.** Each adult pays its upkeep from the sugar it has eaten: one unit of sugar covers four loaded ticks and is paid in advance, and the unused remainder is saved with the ant. An adult without sugar fasts, and is harmed once its fasting grace runs out. By default workers live 144,000 loaded ticks and the fasting grace is 24,000 loaded ticks; the JVM options `prime_ants.adultLifespanTicks` and `prime_ants.adultFastingTicks` change them for ants born afterwards. Spending recorded in older saves is kept as it was.

**Founding queen.** She raises her first clutch from her body reserve of 39,000 units (enough for three eggs and three larvae) and pays no upkeep until then. Afterwards nurses feed her protein only while she holds less than 2,000 (one egg's worth), so further protein goes to the larvae instead of staying in her store.

**Laying gate.** The queen lays a new egg only when all of these hold:

- The colony's food data are complete: every member is loaded and ticking, and the food cache is loaded. Otherwise growth pauses.
- No adult has fasted for a quarter of its fasting grace.
- Sugar actually received over the last 2,400 loaded ticks covers the upkeep of all adults and brood plus the new egg over that window, with a 25% margin (750 sugar per head). Receipts only measure the colony's income; they never pay for anything.
- Food that is here now (eaten but not yet used by adults and brood, carried by workers, or stored in the cache) covers what the existing eggs and larvae still need, a new egg and larva (1,000 + 4,000 sugar and 2,000 + 8,000 protein) and the same upkeep margin.
- The laying interval has passed, a nursery slot is free, and the colony has room under its adult cap (see the next section).

### Colony stages and caps

Decision 23 is implemented as one rule table, `dev.primeants.colony.StageRules`. [colony-development.md](colony-development.md) describes the model in more detail.

| Stage | Requirements (each stage also needs everything below it) | Adult cap |
|---|---|---|
| Founding | founding chamber registered: the queen has settled and laid her first clutch | 5 |
| Young | 5 adults (the queen and 4 workers), a nursery, a food store | 30 |
| Mature | 25 adults, a queen's hall, a material store, 4 stored food units, 16 clay units | 60 |
| Great | 50 adults; nursery, food store, material store and queen's hall all at tier 2 or higher; 32 stone units | 120 |

- Caps and thresholds count living adults including the queen, so 0.1.0's cap of 30 keeps its meaning. As in 0.1.0, brood reserve room under the cap: laying is refused once workers plus brood reach the cap minus one, and emergence once workers plus the queen reach the cap. A lower cap never removes adults.
- Young counts the queen by design: the GDD's "5+ workers" could never be reached under a five-adult Founding cap, which holds at most four workers. Each next threshold fits under the current cap (5 ≤ 5, 25 ≤ 30, 50 ≤ 60), also right after a regression; this is checked when the class loads and by unit tests.
- The stock numbers are starting values.
- Clay counts while it lies in a confirmed material store, is carried by an upgrade builder, or is built into the colony's own walls of a confirmed chamber. Stone (cobblestone or stone) counts while stored, carried by a tier-3 builder, or laid into the colony's own tier-3 walls.

**Only live state counts.**

- A chamber function counts only with its owned marker block (brood pile, food cache or material store) in an open, enclosed chamber. The queen's hall has no marker block; it counts while the living queen is settled in her room.
- An adult counts only as a found, living body. A failed lookup is unknown, not dead; only a recorded death removes an adult.
- A colony is promoted only when confirmed state meets the stage, and demoted only when even its unknowns could not. An unknown store adds its largest possible content (18 food units; 96 clay or 48 other units) to the possible count only.

**Adult limit setting.** `prime_ants.colonyAdultCapacity` is an upper bound (decision 17): default 120, valid 4–120. The effective cap is the smaller of the stage cap and this bound, so a bound below a threshold holds the colony at that stage.

**0.1.0 saves.**

- 0.1.0 founding chambers count as built: tier 1, with a nursery and a food store. No terrain changes.
- New saves write `ColonyAdultCapacityBound` on the queen and `AdultCapacityBound` on the brood pile. A 0.1.0 save's `ColonyAdultCapacity` or `AdultCapacity` is read only when the new key is absent, is still validated as 4–30, and migrates once: a saved 30 (0.1.0's default and maximum) becomes 120, so the colony can grow past 30 later; a saved 4–29 is kept as a deliberate reduction.

### Stage hysteresis and regression

- Promotion is immediate.
- A stage the colony holds drops only after one of its requirements, or a lower stage's, has been certainly unmet for 24,000 loaded ticks in a row (one Minecraft day).
- Each requirement has its own unmet-since clock. It counts the nursery's loaded ticks and is saved with the colony's chambers and stage, so it survives save and reload. A saved clock that is ahead of the current tick restarts.
- Shortfalls that wait for their clock: food, clay and stone; a chamber that is still confirmed but below the tier a stage needs; adults below the held stage's threshold but not below the threshold of the stage beneath it. A Young colony reduced to its queen waits too, because one adult is not below Founding's threshold.
- Losses that apply at once:
  - The queen observed dead. The colony drops to Founding (cap 5) at the next evaluation, all clocks clear, and it cannot be promoted, even with five living workers and an intact nursery and cache.
  - A chamber function observed absent, held by no confirmed or unknown chamber: a breached shell, a missing or foreign marker, or the hall without its living queen.
  - Adults certainly below the threshold of the stage beneath the one held. The colony drops to the stage its adults support.
- Unknown terrain or members never demote and run no clock; a clock starts again once its shortfall is certain.
- While the queen is observed alive, an unloaded entrance or plug cell leaves the hall unknown rather than lost; damage seen in loaded blocks still counts as a loss.

### Caregivers, builders and foragers

- **One builder.** A colony has one builder at a time, shared by the 0.1.0 widening, room digging, wall upgrades, mining and surface work, and it takes work in that order. An empty miner gives way to upgrade work as soon as there is some.
- **Two caregivers.** A worker is taken as a builder, or as an extra forager, only if at least two authorized caregivers remain without it. Nursing is authorized only while the habitat is confirmed (see the next section).
- **Foragers.** A colony keeps one forager for every ten living workers, and at least one: one forager up to 19 workers, two from 20 to 29, three from 30 to 39, five at Mature's cap of 60 adults and eleven at Great's 120. A further forager is claimed only while the colony keeps two caregivers without it and, unless a builder is already at work, one more worker for the builder slot. A further forager that comes back from a trip when the colony no longer needs it returns to the colony's care; the first forager always keeps its claim. Each forager carries its own cargo, and every claim is saved with the queen.

### Damaged and unknown habitat

The founding chamber's habitat covers the stairs, the widening and every dug chamber and gallery: one connected nest that brood care and every chamber check read live.

- A fault seen in loaded blocks always wins over unloaded terrain, in every chamber check and in brood care, so an unloaded cell cannot hide damage seen elsewhere. Unloaded terrain alone only makes a result unknown.
- Nursing pauses while the habitat is damaged or unknown. For example, if the next mining face is unloaded and is also part of the shell of the gallery already dug, the habitat is unknown and nursing pauses.
- By design, a pause costs the colony neither its caregivers nor its builder. While the habitat is only unknown, workers that keep valid nursing roles still count as caregivers, though their work stays paused. While loaded damage pauses nursing, a builder already at work (for example a miner) keeps its claim and cargo until the habitat is restored, even with no authorized caregivers. In both cases no worker is newly taken for work, and the ordinary rule of two authorized caregivers applies again as soon as the habitat is restored.
- Unknown terrain never grants permission to work.

### The nest plan and the first dug chamber

The nest plan (`dev.primeants.founding.NestBlueprint`) is a declarative table of rooms and passages in the founding nest's frame. It compiles into a queue of dig tasks that real workers carry out block by block; it never places blocks. Every space is two blocks high.

- **Bounds.** Every planned cell and shell cell lies within 10 blocks of the entrance forward or sideways, and between 3 blocks below the entrance level and that level. One placement digs at most 32 cells. The 0.1.0 widening keeps its own radius of 6 and depth of 3.
- **First chamber.** A Young colony that certainly lacks a material store digs a 3 × 3 store room, two high, and a passage to it through the founding chamber's back wall: 24 cells. The room goes to the left of the passage if the terrain allows, otherwise to the right, and the room on the other side is kept free for a later room. Only witnessed natural soil, or soil the colony prepared itself, is dug, and the 0.1.0 widening goes first.
- A room is planned only if the free cells of the colony's mound plan can take all of its soil (see Stage mound and plug soil).
- When the room is dug, its worker sets up the colony's store block on the floor and the chamber is registered.

### Material store and hauling

- **Units.** One item is one unit: clay ball (clay); cobblestone and stone (stone); gravel; sand; coal, raw copper and raw iron (ore, stored for the planned forge). Nothing else is accepted.
- **Capacity.** It depends on the store chamber's confirmed tier: 32 units at tier 1, 64 at tier 2, 96 at tier 3. Other materials may fill at most half of the store, so the clay half always stays available: at tier 1, Mature's 16 clay always fit beside up to 16 units of stone, gravel, sand and ore.
- **Hauling.** Food comes first: a forager looks for dropped material only after its trip found no dropped food and no natural food (nectar or prey). It takes only items lying on the ground in its search area (24 blocks around the nest's exterior standing spot, 3 blocks up or down), one unit per trip, and only while the confirmed store has room for that material, checked again at the moment of pickup. If collectable dropped food appears before then, the forager leaves the material where it lies and goes for the food.
- **Capacity changes.** While the store's terrain is unknown, its capacity is held. A loaded fault or a lower confirmed tier reduces what it accepts at once but never removes contents; an overfull store accepts nothing until it has room again.

### Queen's hall

- The hall extends the queen's own chamber on the 0.1.0 widening side that the colony's widening did not take (the left side if there was no widening): 3 × 2 columns, two high, 12 cells, dug from the chamber itself, so the queen and her brood pile never move.
- A Young colony digs it once its material store is confirmed and the hall is certainly missing.
- It registers once as its own tier-1 chamber without a marker block, and counts while its cells and the habitat are intact and the living queen is settled in her room.

### Food cache shares

- **Capacity.** It depends on the founding chamber's confirmed tier: 6 slots at tier 1, 12 at tier 2, 18 at tier 3. A third of the slots is kept for each kind of food, so one kind fills at most two thirds: at tier 1, at most 4 sugar units (apples, sweet berries, nectar) and 4 protein units (chicken, rotten flesh, prey); 8 of each at tier 2; 12 of each at tier 3.
- A forager picks up dropped food only while its kind is below that share; at tier 3, food already being carried back to the cache counts toward it. A forager that reaches a full cache waits there with its unit.
- **Older caches.** Caches saved before the shares existed load as they were. If such a cache holds more of one kind than its share, then when the other kind arrives, the units beyond the share leave through transfer custody onto the chamber floor beside the cache, where they stay until their kind has room again, and the arriving unit gets in.

### Chamber walls, tiers and upgrades

- **Walls.** A chamber's wall cells are the horizontal faces of its open cells that the nest plan never opens. Floor and roof are not walls: the floor may be witnessed stone, and the roof is the surface layer. The founding chamber, which holds the nursery and the food store, has eight wall cells.
- **Tiers.** A wall cell is tier 1 if it is anything solid (natural or colony earth, natural stone, a block a player placed), tier 2 if it is the colony's own packed clay (resin masonry, also tier 2, is meant for other species), and tier 3 if it is the colony's own nest-cut stone. A chamber's confirmed tier is the lowest tier of its walls, read live. A block a player places never raises a tier and is never converted.
- **Unlocks and order.** Mature unlocks tier 2 and Great unlocks tier 3. Upgrades go chamber by chamber: the founding chamber first (one job serves both the nursery and the food store), then the material store, then the queen's hall; all three reach tier 2 before tier-3 work starts. A wall shared by two chambers is converted and counted once.
- **Tier-2 work.** One builder takes one clay ball from the confirmed store, carries it and rams it into the next wall cell's own earth, which becomes packed clay: one clay per cell, no soil leaves the nest and nothing goes to the mound. A cell that is no longer natural or colony earth stops the job, and the unspent clay goes back to the store.
- **Tier-3 work.** The builder faces each packed-clay cell the colony built with nest-cut stone, using one intact stone item (`minecraft:stone`) per cell; cobblestone stays bulk stock and is not used. The clay core stays in place.
- **Counting.** Clay in the colony's own walls of a confirmed chamber, and in the builder's mandibles, keeps counting toward Mature's 16 clay. Stone in tier-3 walls, or carried for them, keeps counting toward Great's 32 stone.
- **Builder.** An upgrade needs a recipe unit in the confirmed store, no other builder at work, finished room digging, and a free worker that leaves two caregivers.
- **Ledger.** Each upgrade job keeps exact accounts: taken = carried + built + released, where released counts the units that dead builders handed to transfer custody. Each transfer is recorded by its ID, so the units still waiting in custody are a separate figure.
- **Effects.** At tier 2 the nursery gains 3 brood slots and raises brood 1.5 times as fast, the food cache grows from 6 to 12 slots, the material store from 32 to 64 units, and the queen's hall halves the laying interval from 1,200 to 600 loaded ticks. At tier 3 the nursery has 8 extra slots instead of 3 and raises brood twice as fast, the cache holds 18 and the store 96, and the hall's interval is 300 ticks. Tiers change nothing else: food rules, costs, stage caps and thresholds, the founding reserve and adult lifespan stay the same.

### Brood capacity and laying speed

At default timing (adults live 144,000 loaded ticks, a brood passes three stages of 12,000 ticks each, and the queen lays one egg per 1,200 ticks), a nursery sustains slots × speed × 4 workers, but never more workers than the queen can lay eggs for in one lifespan (120 at the base laying interval). Slots: 3 for the founding chamber, 4 more with the queen's hall, and 3 more with tier-2 nursery walls or 8 more with tier-3 walls.

| Nursery | Slots | Speed | Workers sustained | Enough for |
|---|---|---|---|---|
| Founding chamber, earth walls | 3 | ×1 | 12 | Young (5) |
| With the queen's hall | 7 | ×1 | 28 | Mature (25) |
| With tier-2 nursery walls | 10 | ×1.5 | 60 | Great (50) |
| With tier-3 nursery walls | 15 | ×2 | 120 | Great's cap (120) |

- Speed applies to running and new brood stages alike.
- The hall's live confirmed tier sets the laying interval (1,200, 600 or 300 loaded ticks for tiers 1, 2 and 3), scaled by the brood time multiplier `prime_ants.broodMultiplier`.
- A check at class load makes sure that what each stage can build sustains its next threshold with workers alone.

### Store and cache display

- The material store block shows a clay heap and a heap of other stock, one level per four units (clay up to 8 levels at tier 1, 16 at tier 2 and 24 at tier 3; other stock up to 4, 8 and 12), and one lump for each other material present (stone, gravel, sand, ore). Two stores whose totals differ by eight or more units, or whose materials differ, always look different.
- The food cache block shows how many units it holds and which of the six foods are present (apple, chicken, sweet berries, nectar, rotten flesh, prey).
- The block state is only a display. The inventory is the list of one-unit stacks saved in the block entity.
- **Inventory formats.** Stores and caches save a format number. A save without one (format 1) is checked against tier-1 limits, format 2 against tier-2 limits, and format 3, the current one, against tier-3 limits. Unknown terrain or a lost tier never truncates contents.

### Stage mound and plug soil

- Soil dug out of nest-plan rooms and mining connectors is laid on the mound plan of the colony's current stage. The plan uses the old blueprint's tiers primitive (stacked elliptical truncated cones) and lies behind the entrance in two lobes, one on each side of the approach lane. The lane, the exterior standing spot and the stairs stay clear.
- **Young:** two lobes three layers high in a footprint 15 columns wide (side -7 to 7) and 8 deep (forward -8 to -1): 98 columns, 214 cells.
- **Mature:** two lobes five layers high, 23 columns wide (side -11 to 11) and 13 deep (forward -13 to -1): 242 columns, 798 cells, containing the Young mound.
- **Great** keeps the Mature mound; its additions are the surface structures below.
- **Founding** has no mound plan. Its 0.1.0 founding deposits and the 0.1.0 widening's deposits keep their own lists, inside the Young lobes.
- **Cells.** Each column sits on its own ground: the highest witnessed natural soil or stone within two blocks of the entrance level. A cell takes soil only if it is air or a witnessed short plant, which is buried without drops, and only if it rests on natural ground or the colony's own mound soil. Fluids, blocks a player placed, block entities, logs, leaves, crops and any other block stop the column beneath them and are never covered or removed.
- A builder with nowhere to lay its soil keeps it and waits.
- Mound soil is the colony's own nest soil, a counted stock. Nothing removes it when the colony regresses.
- **Plug soil.** When the queen's founding deposit list is full, the forager that opens the nest lays the two plug units on the colony's mound plan; a Founding colony, which has no plan of its own, uses Young's.

### Mining

- **When.** Only a colony held at Mature or Great mines, and only after widening, room digging and upgrade work. A drop to a lower stage stops new removals, but a miner can still deliver a unit it already carries.
- **Limits.** Within 16 blocks of the entrance horizontally, 1 to 6 blocks below the entrance level, and at most 64 new mining cells per colony, connector soil included. Founding, widening and chamber bounds are unchanged.
- **Gallery.** The current planner digs one fixed, connected, supported gallery two blocks high beyond the store passage: forward 8 to 15, straight ahead, at most 16 cells. It is planned only if it contains at least one natural resource block, and it ends after the last one. It does not look for deposits elsewhere and does not replan a partly dug gallery that has stopped.
- **Units.** One removed block gives one carried item, never vanilla loot: stone → cobblestone, clay → clay ball, gravel → gravel, sand → sand, coal ore → coal, copper ore → raw copper, iron ore → raw iron. Ore is stored without processing. Connector soil becomes dirt and goes to the mound like other dug soil.
- **Natural materials.** Resource blocks are recorded as natural when a newly generated chunk is converted, from the surface down to 16 blocks below it. This record is separate from the soil rules and does not widen them. Loading old terrain, or a block merely looking natural, never grants it; any later write, even of the same block, cancels it.
- **Work.** A claimed miner walks up, works for 20 loaded ticks, carries one unit and puts it into the confirmed store within reach. At each removal the game rechecks loaded state, the expected block and its origin, bounds, budget, line of sight and reach, exposure, support, and room in the store (or a free mound cell for soil). Gravity stays on: a block under a falling block such as sand or gravel is never removed (this is checked before a miner is even picked), and neither is a block whose removal would break support. Unknown or changed terrain holds the work and the cargo.
- **Saving.** Plans, claims, successful removals, deliveries and the units dead miners released are saved; there is no abstract mining income. Only cells the miner actually dug join the habitat checks. Mining never force-loads terrain, and planning inspects a small fixed queue on a slow cadence.
- **Known gap.** Mining does not help a colony reach Mature. Until then its clay must come from clay balls lying on the ground near the nest, for example dropped by a player.

### Surface structures

- Surface plans are bundled data (`data/prime_ants/surface_plans/`), compiled into an ordered queue of cells and kept separate from the mound's spoil capacity. Young has no structures beyond its mound.
- **Mature (19 cells):** an entrance arch three blocks high just behind the entrance (two pillars, a lintel over the open lane and a one-block step beside each pillar), and one mound crest four blocks high with one-block steps up to it.
- **Great (57 cells, including Mature's 19):** two-high ramparts on both sides of the arch, a five-high watch post at each end with one-block steps behind it, and the base of each arch pillar converted into a mound gate.
- **Bounds.** Up to 14 blocks behind the entrance and 12 to either side, six layers high, each column on ground found within two blocks of the entrance level; at most 64 structure cells. The approach lane stays open two blocks high and the exterior standing spot stays clear. A plan's sizes, costs and order are fixed before any block is built.
- **Cost.** Each structure block uses one soil unit taken from the colony's own mound. A gate is compacted earth around the open two-high passage: converting a pillar base keeps the soil already paid there and costs no extra unit, stone or clay. Mound soil that already sits in a structure cell is kept as a paid foundation instead of being replaced, and cells finished under the Mature plan carry over into the Great plan.
- **When.** Work starts at Mature (the Great part at Great), only when no widening, room digging, upgrade or mining work is waiting. It uses the one builder slot and keeps two caregivers.
- **Work.** The builder takes a unit only from the colony's own removable mound soil (open air above, solid support below, outside the approach lane and the Great structure's columns, nothing standing on it), carries it in its mandibles, walks to a supported stand and works 20 loaded ticks for each removal and each placement. Loaded state, the colony's claim on the cells, stage, support, collisions, line of sight and reach are rechecked at each action. The builder keeps its chosen stand through normal stair jumps and reloads, rechecking it every tick. If its own body is in the target cell, it keeps its claim and walks on until the cell is clear; nothing is placed into an occupied cell.
- **Stops and waits.** Unloaded terrain pauses the work. Any later write to a target cell, even one that leaves the same block, revokes permission to build there, and because cells are built in order, a blocked cell keeps the plan from completing. A drop below the plan's stage stops new work; blocks already paid stay. An empty builder that cannot go on (no stand, a route stuck for 1,200 ticks, other work taking priority) is released and the job waits. A builder that has to give up while carrying a unit leaves it in transfer custody as a named item, and the job stops.
- **Saving.** The plan description, each column's ground level, one receipt per paid cell (naming that cell), the claim, the carried unit and named releases are saved. A save whose receipts do not match the plan's cells is rejected, so a receipt can never move to another cell. Moving mound soil into structures does not change excavation history; the colony's current soil count includes surface blocks and gates.

### Founder access

A queen-founded colony on flat ground can get stuck on its Mature plan: after four paid cells, the next cell, the base of the mound crest (forward -4, side 3, layer 0), is mound soil already in place but buried on all sides, so no worker can reach it. By design, the surface job then gets one finite access step, run by the same single builder. Here layer 0 is the first block above the ground at the entrance level.

- It applies only to the bundled Mature plan, with exactly four paid cells (two kept as foundations, two placed), the buried crest base next, every column on ground at the entrance level, and exactly 72 blocks of the colony's soil on the surface.
- Two fixed moves: the soil unit at forward -4, side 2, layer 1 goes to forward -9, side 8, layer 0; then the unit at forward -4, side 2, layer 0 goes to forward -10, side 7, layer 0. This opens a stand beside the crest base. Both destinations must be free cells of the Mature mound plan.
- Each removal and each redeposit takes 20 consecutive loaded action ticks. The lower unit must still pass the ordinary removability check after the first redeposit. There is no exception for structure columns and no third move.
- Both destinations hold saved reservations for the colony and the unit; any write there, even air to air, cancels them and stops the access.
- The moved unit is a tagged dirt item and is always in exactly one place: the builder's mandibles, transfer custody, or a named item on the ground. If the carrier dies, one other worker may pick up that named item and carry on; a second carrier death ends the access, with the unit left in named custody.
- A cancelled or completed access is saved and is never started again for the same job.
- The access keeps its own history (removed + named pickups = redeposited + carried + released) and earns no excavation or structure credit. Only the ordinary surface work that follows adds the usual receipt for the crest base.
- If the next target is still buried after the access, the job stops; no wider clearing follows.
- The access stops if the queen dies, the colony's home changes, paid surface blocks change, the stage falls below Mature, the excavation history changes, or a source or destination is revoked. If the crest base becomes unloaded, the access pauses and resumes only after two fresh evaluations confirm Mature.
- Normal saving and reloading keep the access state; recovery from an abrupt crash is not guaranteed.

### Colony defence

- **What alarms a colony.** Damage a player in survival mode deals to one of its ants, or a player's successful break of a block the colony currently owns (for example its nest soil, plugs, brood pile, food cache or material store). The game reads which colony owns the block before the block changes, and raises the alarm only after the break succeeds. Creative and spectator players never alarm a colony.
- **Response.** Adult workers of that colony (not callows) within 12 blocks of the harm point respond. They pursue only the player who caused the harm, and only while that player is within 16 blocks of the harm point; a worker already pursuing keeps going up to 16 blocks from it. They bite at physical reach with a clear line of sight, for one health point per bite, with a 20-tick cooldown.
- **Duration.** The alarm lasts 600 loaded ticks; new harm renews it.
- Defence interrupts work: the worker keeps its task, cargo and claims and resumes afterwards, and crop sharing in progress is cancelled. There is no reputation, automatic repair or resource credit.

### Transfer custody and saves

- Items that leave a worker or a block without being delivered (a dead worker's cargo, units shed by an older cache, the contents of a broken store or cache, a surface unit given up) go into transfer custody: the level's saved data keeps each stack under an ID until it can be set down in the world as an item entity with that ID. A unit is therefore always in exactly one place: a store, a worker's mandibles, custody or the ground.
- Jobs record each release by its transfer ID, so their accounts stay exact across deaths and reloads.
- State is consistent across normal saves and reloads; recovery from a crash in the middle of a handoff is not guaranteed.
