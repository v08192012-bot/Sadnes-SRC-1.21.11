# Пакет для переноса: `Removals` + `ElytraSwap`

Извлечено из **Sadnes SRC**, маппинги **Minecraft 1.21.11 (Yarn)**, **Java 21**.
Все файлы лежат **verbatim** (как есть, с исходными `package tech.sadnes...`) — при переносе меняй пакеты под свою структуру.

```
porting/
├── README.md                     ← этот файл
├── removals/
│   ├── Removals.java             ← модуль (категория VISUALS → WORLD)
│   ├── mixins/                   ← 19 миксин-хуков (см. таблицы ниже)
│   └── mixins-json-entries.txt   ← записи для своего mixins-конфига
└── elytraswap/
    ├── ElytraSwap.java           ← модуль (категория MOVEMENT → ELYTRA)
    └── utils/                    ← самодостаточные утилиты, которые реально тащит модуль
        ├── ActionSequence.java   ← тайминг-очередь действий (269 строк)
        ├── StopWatch.java        ← таймер (63)
        ├── Action.java           ← @FunctionalInterface (6)
        └── ScreenUtil.java       ← allowsMovement() — работают ли бинды при открытом GUI (31)
```

---

# 1. Removals (VISUALS → WORLD)

Модуль «снятия» визуальных элементов. Статические хуки-запросы (`Removals.isRemoved(...)`)
вызываются из миксинов — поэтому сначала заводи класс модуля, потом миксины.

## 1.1 Поверхность API (то, что дёргают хуки)

| Метод | Кто зовёт | Назначение |
|---|---|---|
| `static boolean isRemoved(String key)` | все миксины | элемент включён в MultiSetting `removals.elements` и модуль включён |
| `static boolean isEntityHidden(Entity)` | EntityRenderManagerMixin | скрытие сущности (items/players + условие дистанции/стены) |
| `static boolean isBlockHidden(Block)` | block-миксины, ClientWorld, WorldRenderer | рендер-скип блоков (sculk/vegetation) |
| `static boolean shouldCullFace(a, b)` | ClientWorldMixin | не перестраивать меш при скрытии |
| `static boolean isParticleRemoved(ParticleEffect)` | ClientWorldMixin | sculk-/взрыв-частицы |
| геттеры настроек (`getElements()` и т.д.) | вспомогательные экраны клиента | можно не переносить |

Внутри также: `updateHiddenBlocks()` (пересобирает `Set<Block>` и делает `mc.worldRenderer.reload()`)
и модульный `@EventHandler onPacket(...)` — отмена входящих пакетов (звуки, mace-частицы).

## 1.2 Настройки (`removals.*`)

- **elements** (Multi): `fire, fog, lava, water, shake, bossbar, player_glow, enchant_glint,
  bad_effects, black_hearts, scoreboard, totem_anim, hand_anim, block_particles, mace_particles,
  explosion_particles, vegetation, sculk, sculk_sensors, sculk_particles, eat_bob, remove_sounds`
- **soundCheck** (Multi, виден при `remove_sounds`): `event_spawn, wither_hit, trident, exp_bottle, thunder, totem`
- **hideEntities** (Bool) + **hideTypes** (Multi: `items, players`) +
  **hideCondition** (Mode: `always, closer, farther, wall`) + **hideDistance** (Number 1–128)

Lang-ключи: `feature.impl.render.removals` + все `removals.*` (переименуй под свой lang;
`Language.translate(...)`-фоллбэков у настроек нет — в худшем случае покажутся сырые ключи).

## 1.3 Миксины — ЧИСТЫЕ (переносятся пакетом, единственная правка = import Removals)

| Файл | Инжект | Что делает |
|---|---|---|
| `RemovalsBlockStateMixin` | `AbstractBlockState.getRenderType` → `INVISIBLE` | скрытые блоки не рендерятся |
| `RemovalsBlockCullingMixin` | `Block.shouldDrawSide` → `VoxelShapes.empty()` | кул-фейсы скрытых блоков |
| `SodiumBlockCullingMixin` | `@Pseudo` sodium `AbstractBlockRenderContext.shouldDrawSide` (`require = 0`) | то же для Sodium; без Sodium просто не применяется |
| `RemovalsClientPlayNetworkHandlerMixin` | `onEntityStatus` статус **35** → cancel | **totem_anim** |
| `BossBarHudMixin` | `BossBarHud.render` HEAD → cancel | **bossbar** |
| `StatusEffectFogMixin` | `StatusEffectFogModifier.shouldApply` → `false` | **bad_effects** (туман от эффектов) |
| `WorldRendererRemovalsMixin` | `fillEntityOutlineRenderStates` RETURN → outline = null | блок-аутлайн скрытого блока |

## 1.4 Миксины — СМЕШАННЫЕ (переносить **только** инжекты с `Removals.` — остальное чужое)

| Файл | Переносимые инжекты (Removals) | Чужое в этом же файле (НЕ тащить) |
|---|---|---|
| `ItemRendererMixin` | `renderItem` Glint → `Glint.NONE` (**enchant_glint**) | quadAlpha (TransparentPlayers) |
| `EquipmentRendererMixin` | armor-glint boolean → false (**enchant_glint**) | 2 хука TransparentPlayers |
| `FogRendererMixin` | `getCameraSubmersionType`: LAVA/WATER → ATMOSPHERIC (**lava, water**); `applyFog` args 3–8 = `Float.MAX_VALUE` (**fog**) | `FogDistanceEvent` (если в целевом клиенте нет своего fog-оверрайда — выкинь, оставь только блок с `isRemoved("fog")`) |
| `InGameOverlayRendererMixin` | `renderFireOverlay` cancel (**fire**); `renderUnderwaterOverlay` cancel (**water**) | `renderInWallOverlay` (это NoPush, не Removals) |
| `EntityRenderManagerMixin` | `shouldRender`: `isEntityHidden` → false (**hideEntities**) | Esp chams, EntityStateCache |
| `EntityMixin` | `isGlowing` HEAD: игрок + **player_glow** → false | bubbleColumns, boundingBox/targetingMargin ивенты, запрет yaw/pitch в контейнере |
| `LivingEntityMixin` | `hasStatusEffect`: WITHER → false (**black_hearts**) | JumpEvent, Speed, UsingItemChange, HandOffset, duck-interface |
| `ClientWorldMixin` | `updateListeners` WrapOperation + `scheduleBlockRerenderIfNeeded` cancel (**shouldCullFace**); `addBlockBreakParticles` + `spawnBlockBreakingParticle` cancel (**block_particles**); `addBlockParticleEffects` cancel (**explosion_particles**); `addParticle(...)` cancel (**isParticleRemoved**) | BlockUpdateEvent, EntityStateCache preview, EntityRemoveEvent |
| `GameRendererMixin` | `tiltViewWhenHurt` cancel (**shake**); 2× `getEffectFadeFactor` → 0 (**bad_effects**) | CameraTweaks, FullBright, Ambience, MotionBlur, RenderHooks |
| `HeldItemRendererMixin` | 2× `renderItem` pitch/yaw sway → lerp (**hand_anim**); `applyEatOrDrinkTransformation` translate skip (**eat_bob**) | ~10 хуков анимаций/косметики руки |
| `InGameHudMixin` | `renderScoreboardSidebar` cancel (**scoreboard**); `renderNauseaOverlay` cancel (**bad_effects**) | Hotbar, Crosshair, FreeCam, RadialMenu… (и `renderStatusEffectOverlay` — там cancel **без условия**, это другая фича) |
| `LightmapTextureManagerMixin` | `getDarkness` → 0; `update` `getEffectFadeFactor` → 0 (**bad_effects**) | FullBright gamma, Ambience lightmap |

## 1.5 Зависимости модуля (Removals.java)

| Символ Sadnes | Замена в целевом клиенте |
|---|---|
| `Feature` + `@FeatureInfo(name/description/category=VISUALS/subcategory=WORLD)` | своя базовая фича/модуль + аннотация |
| `Sadnes.getInstance().getFeatureManager().getFeature(...)` (`getInstance()`) | свой singleton/менеджер |
| `@EventHandler`, `PacketEvent` (`isReceive/getPacket/cancel`) | свой ивент-бас; нужен **receive**-хук пакетов |
| `MultiSetting/BooleanSetting/ModeSetting/NumberSetting`, `setOnChange`, `setVisibleWhen`, `has/getOption/is/getValue` | свой API настроек (эти методы — прямые аналоги) |
| `RaycastUtil.canSee(from, to)` | `mc.world.raycast(new RaycastContext(from, to, COLLIDER, NONE, player)).getType() != BLOCK` |
| `mc.execute(mc.worldRenderer::reload)` | без изменений |

**Требования к миксинам:** MixinExtras (`@WrapOperation`, `@ModifyReturnValue`) —
в Fabric Loader ≥ 0.15 встроен; в standalone-клиентах добавь зависимость `io.github.llamalad7:mixinextras`.

## 1.6 Регистрация

```java
// в своём менеджере модулей:
new Removals(),
```
```json
// в mixins-конфиг (плюс свой package-префикс):
// см. porting/removals/mixins-json-entries.txt
```

⚠ Обратная зависимость (НЕ входит в пакет): `Ambience.java:356` вызывает
`Removals.isRemoved("fog")` — если переносишь и Ambience, захвати и эту строку.

---

# 2. ElytraSwap (MOVEMENT → ELYTRA)

Свап «нагрудник ↔ элитры» + использование фейерверка + автокоманды после взлёта.

## 2.1 Поведение

- **Бинд `swap`** — свап:
  - на груди **элита** → вернуть нагрудник: если целевой слот в хотбаре — `useHotbarSlot`,
    иначе ритуал `SWAP` со слотом `selected` + анимационный `PlayerInteractItemC2SPacket` + обратный `SWAP`;
  - на груди **кираса** (или пусто) → надеть элитру: `findElytraSlot()` (не сломанная!) либо
    «через свободный слот» (PICKUP-трюк со слотом индекса 6 = броня торса);
  - каждое действие обёрнуто в `ActionSequence` с таймингами (`setBlockMovement(true/false)` —
    заморозка движения клиента на время свапа).
- **Бинд `use`** — во время полёта использовать фейерверк из инвентаря
  (нет — уведомление `elytraswap.noFirework`; фоллбэк-строка захардкожена в коде ✓).
- **`autoFly`** — через ≤3 с после экипировки элитр выполнить команды из `flyCommands`
  (разделители `;` или перевод строки, ведущий `/` срезается → `sendChatCommand`).
- **`oldSwap`** — legacy-вариант через `swapLegacy` (Holy-серверы) / `swapDirect` (PICKUP-тройник).
- **`chestSwap`** — авто-свап в PvP-режиме сервера, **каждые 15 тиков**
  (обфусцированное условие в `onTick`: `Integer.compareUnsigned(age * -286331153 + 143165576, 286331153) < 0`
  ≡ `age % 15 == 0`) — при желании замени на простой счётчик.

### Тайминги (`SwapTiming(first, armor, back)` в тиках)
| Профиль | first / armor / back | Когда |
|---|---|---|
| `DEFAULT_TIMING` | 2 / 2 / 2 | по умолчанию |
| `FUNTIME_TIMING` | 50 / 50 / 50 | funtime (+local proxy) |
| `SPOOKYTIME_TIMING` | 50 / 50 / 150 | spookytime |
| `HOLY_TIMING` | 50 / 50 / 50 | holy (+dildo) |

Определение сервера: `ServerManager.isServer("funtime"|"holy"|"spookytime"|"dildo"|"...")`,
`isLocalProxy()`, `isHolyWorld()`, `isInPvp()`. **Проще всего при переносе:**
выкинуть всю серверную ветку и оставить `DEFAULT_TIMING`, либо завести свой enum-профиль в настройках.

## 2.2 Настройки (`elytraswap.*`)

`KeybindSetting swap`, `KeybindSetting use`, `BooleanSetting autoFly`,
`TextSetting flyCommands` (подсказка `elytraswap.flyCommands.hint`, дефолт `"fly; speed 10"`),
`BooleanSetting oldSwap` (false), `BooleanSetting chestSwap` (false).
Lang-ключи: `feature.impl.misc.elytraswap` + `elytraswap.*`;
нотификации `elytraswap.noElytra / noFirework / noSpace` — **фоллбэки захардкожены в коде** ✓.

## 2.3 Хуки событий

| Хук | Что делает |
|---|---|
| `TickEvent` → `onTick` | тик `ActionSequence`, выбор тайминга, авто-флай-команды, chestSwap |
| `ScreenKeyEvent` (PRESS) → `onScreenKey` | бинды работают и при открытом GUI |
| `ScreenMouseClickEvent` (PRESS) → `onMouseClick` | то же для мыши |

Оба экран-хука проверяют `ScreenUtil.allowsMovement(screen)` (утилита в `utils/`):
`null` (игровой мир) или `HandledScreen` без фокуса на текстовом поле → бинды активны.

## 2.4 Зависимости — карта переноса

| Символ Sadnes | Что это | Что делать |
|---|---|---|
| `ActionSequence`, `StopWatch`, `Action`, `ScreenUtil` | утилиты тайминга/экрана | **уже в `elytraswap/utils/`** (самодостаточные, только java+MC) |
| **`ThrowComponent`** (2308 строк) | ⚠ **ЯДРО**: `queueActions(...)`, `useItem(item)`, `setBlockMovement(boolean)`, `useHotbarSlot(i)`, `swapSlotWithHotbar(slot, hotbar)`, `isIdle()` | не переносится в пакете. Либо тащи его целиком, либо **перепиши свап под свой инвентарь-менеджер**: последовательность кликов описана в `swapChestSlot/swapDirect/swapLegacy/swapViaFreeSlot` |
| **`GuiWalk.isBypassActive()`** | свап «не закрывая» GUI, когда управление заморочено | замени на `false` (будет всегда обычная ActionSequence-ветка) или свой аналог |
| `InventoryUtil.*` (`findMainInventorySlot`, `clickSlot`, `getPlayerSlots`, `toHotbarIndex`, `getSelectedSlot`) | клики слотов | свои; `clickSlot(slot, button, SlotActionType)` = `mc.interactionManager.clickSlot(handler, slot, button, type, player)` |
| `PacketSender.sendSequenced(creator)` | пакет use с sequence-id | `mc.interactionManager.sendSequencedPacket(mc.world, creator)` (в Sadnes — через `ClientPlayerInteractionManagerAccessor`) |
| `ChatUtil.sendChat(cmd)` | авто-команды флай | `mc.getNetworkHandler().sendChatCommand(cmd)` |
| `NotificationUtil.notify(type, title, icon, msg)` | уведомления | свой уведомлятор или `mc.inGameHud.getChatHud().addMessage(...)` |
| `ServerManager.*` | серверные тайминги/PvP | см. §2.1 — оставить `DEFAULT_TIMING` |
| `KeybindSetting.matchesKey(int)` | бинды | свой Keybind |
| `Language.translate(key, fallback)` | строки | свой lang; фоллбэки в коде есть |

Внешние потребители Sadnes (не нужны при переносе): `ServerBinds` дёргает
`ElytraSwap.getInstance()` и `ElytraSwap.findElytraSlot()`.

## 2.5 Регистрация

```java
// в своём менеджере модулей:
new ElytraSwap(),
```
Категория: `MOVEMENT` / сабкат: `MOVEMENT_ELYTRA` (в `FeatureInfo`).

---

# 3. Общие заметки

1. **Версия Minecraft**: всё под Yarn **1.21.11**. На другой версии имена методов
   (`tiltViewWhenHurt`, `fillEntityOutlineRenderStates`, `getCameraSubmersionType`,
   `StatusEffectFogModifier`, `EquipmentRenderer.render(...)`, нумерация `args` и т.д.)
   надо перепроверять по маппингам целевой версии.
2. **Порядок переноса Removals**: сначала класс модуля (чтобы статические методы отвечали),
   потом чистые миксины, потом вырезанные инжекты из смешанных файлов (см. §1.4),
   потом регистрация в mixins-json и менеджере модулей.
3. **Порядок переноса ElytraSwap**: решить судьбу `ThrowComponent`/`GuiWalk` (самая тяжёлая часть),
   подключить `utils/`, заменить `ServerManager`-тайминги, подключить бинды и событийные хуки.
4. В коде обнаружены следы декомпилятора (битые касты в других модулях клиента — например,
   классические `(Long)(Object)...`); в **двух извлечённых модулях** их нет,
   `Removals` и `ElytraSwap` компилируются как есть (сборка CI проходила с ними в составе клиента).
