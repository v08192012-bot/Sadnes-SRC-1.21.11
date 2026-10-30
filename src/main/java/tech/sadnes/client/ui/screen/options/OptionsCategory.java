package tech.sadnes.client.ui.screen.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.sound.SoundCategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.screen.settings.CycleOptionWidget;
import tech.sadnes.client.ui.screen.settings.KeybindOptionWidget;
import tech.sadnes.client.ui.screen.settings.SettingEntry;
import tech.sadnes.client.ui.screen.settings.SliderOptionWidget;
import tech.sadnes.client.ui.screen.settings.ToggleOptionWidget;
import tech.sadnes.client.util.OptionsUtil;

public enum OptionsCategory {
    ESSENTIALS(Icon.GAUGE, "Быстрая настройка", true),
    FAVORITES(Icon.STAR, "Избранное", true),
    VIDEO(Icon.DISPLAY, "Графика", true),
    SOUND(Icon.VOLUME_HIGH, "Звуки", true),
    GENERAL(Icon.SETTINGS, "Главные", true),
    MOUSE(Icon.LOCATION_CROSSHAIRS, "Мышь", true),
    KEYBOARD(Icon.KEYBOARD, "Клавиатура", true),
    LANGUAGE(Icon.GLOBE, "Язык", true),
    RESOURCE_PACKS(Icon.PACKAGE, "Ресурспаки", true),
    CHAT(Icon.LAYOUT_DASHBOARD, "Чат", true),
    ACCESSIBILITY(Icon.FACE_SMILE, "Спец. возможности", true),
    FEATURES(Icon.BOLT, "Возможности", true);

    public final Icon icon;
    public final String title;
    public final boolean enabled;

    OptionsCategory(Icon icon2, String string, boolean flag) {
        this.icon = icon2;
        this.title = string;
        this.enabled = flag;
    }

    public List<SettingEntry> getOptions(MinecraftClient minecraftClient) {
        switch (this) {
            case ESSENTIALS:
                return buildEssentials(minecraftClient);
            case FAVORITES:
            case LANGUAGE:
            case RESOURCE_PACKS:
            default:
                return Collections.emptyList();
            case VIDEO:
                return buildVideo(minecraftClient);
            case SOUND:
                return buildSound(minecraftClient);
            case GENERAL:
                return buildGeneral(minecraftClient);
            case MOUSE:
                return buildMouse(minecraftClient);
            case KEYBOARD:
                return buildKeyboard(minecraftClient);
            case CHAT:
                return buildChat(minecraftClient);
            case ACCESSIBILITY:
                return buildAccessibility(minecraftClient);
        }
    }

    private static List<SettingEntry> buildEssentials(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        int[] ints = OptionsUtil.getIntRange(gameOptions.getViewDistance(), 2, 16);
        int[] ints2 = OptionsUtil.getIntRange(gameOptions.getMaxFps(), 10, 260);
        int i = ints2[1];
        int[] ints3 = OptionsUtil.getIntRange(gameOptions.getFov(), 30, 110);
        arrayList.add(SettingEntry.createHeader("Графика"));
        arrayList.add(
            new SettingEntry(
                Icon.DISPLAY,
                "Графика",
                new CycleOptionWidget<>(
                    gameOptions.getPreset(),
                    new GraphicsMode[]{GraphicsMode.FAST, GraphicsMode.FANCY, GraphicsMode.FABULOUS},
                    OptionsCategory::graphicsModeLabel
                )
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.GAUGE,
                "Прорисовка",
                new SliderOptionWidget(OptionsUtil.intSlider(gameOptions.getViewDistance()), ints[0], ints[1], 1.0F, f -> Math.round(f) + " ч.")
            )
        );
        arrayList.add(
            new SettingEntry(
                    Icon.BOLT,
                    "Макс. FPS",
                    new SliderOptionWidget(
                        OptionsUtil.intSlider(gameOptions.getMaxFps()),
                        ints2[0],
                        ints2[1],
                        1.0F,
                        f2 -> Math.round(f2) >= i ? "∞" : Integer.toString(Math.round(f2))
                    )
                )
                .withKeywords("fps", "кадры")
        );
        arrayList.add(
            new SettingEntry(
                    Icon.EYE,
                    "Поле зрения",
                    new SliderOptionWidget(OptionsUtil.intSlider(gameOptions.getFov()), ints3[0], ints3[1], 1.0F, f3 -> Math.round(f3) + "°")
                )
                .withKeywords("fov", "обзор")
        );
        arrayList.add(
            new SettingEntry(
                    Icon.FLAME,
                    "Яркость",
                    new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getGamma()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
                )
                .withKeywords("gamma", "гамма")
        );
        arrayList.add(guiScaleOption(gameOptions));
        arrayList.add(SettingEntry.createHeader("Звук"));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.MASTER));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.MUSIC));
        arrayList.add(SettingEntry.createHeader("Игра"));
        arrayList.add(new SettingEntry(Icon.RUN, "Авто-прыжок", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getAutoJump()))));
        arrayList.add(
            new SettingEntry(
                Icon.EYE,
                "Видимость чата",
                new CycleOptionWidget<>(
                    gameOptions.getChatVisibility(),
                    new ChatVisibility[]{ChatVisibility.FULL, ChatVisibility.SYSTEM, ChatVisibility.HIDDEN},
                    OptionsCategory::chatVisibilityLabel
                )
            )
        );
        return arrayList;
    }

    public List<OptionsCategory.Preset> getPresets(MinecraftClient minecraftClient) {
        switch (this) {
            case VIDEO:
                return buildVideoPresets(minecraftClient.options);
            case SOUND:
                return buildSoundPresets(minecraftClient);
            default:
                return Collections.emptyList();
        }
    }

    private static List<OptionsCategory.Preset> buildVideoPresets(GameOptions gameOptions) {
        int[] ints = OptionsUtil.getIntRange(gameOptions.getViewDistance(), 2, 16);
        int i = Math.max(ints[0], 5);
        int i2 = Math.min(ints[1], 12);
        int i3 = ints[1];
        ArrayList arrayList = new ArrayList();
        arrayList.add(
            new OptionsCategory.Preset("Производительность")
                .set(gameOptions.getPreset(), GraphicsMode.FAST)
                .set(gameOptions.getViewDistance(), i)
                .set(gameOptions.getCloudRenderMode(), CloudRenderMode.OFF)
                .set(gameOptions.getEnableVsync(), false)
                .set(gameOptions.getEntityShadows(), false)
                .set(gameOptions.getAo(), false)
                .set(gameOptions.getVignette(), false)
        );
        arrayList.add(
            new OptionsCategory.Preset("Баланс")
                .set(gameOptions.getPreset(), GraphicsMode.FANCY)
                .set(gameOptions.getViewDistance(), i2)
                .set(gameOptions.getCloudRenderMode(), CloudRenderMode.FAST)
                .set(gameOptions.getEnableVsync(), true)
                .set(gameOptions.getEntityShadows(), true)
                .set(gameOptions.getAo(), true)
                .set(gameOptions.getVignette(), true)
        );
        arrayList.add(
            new OptionsCategory.Preset("Качество")
                .set(gameOptions.getPreset(), GraphicsMode.FABULOUS)
                .set(gameOptions.getViewDistance(), i3)
                .set(gameOptions.getCloudRenderMode(), CloudRenderMode.FANCY)
                .set(gameOptions.getEnableVsync(), true)
                .set(gameOptions.getEntityShadows(), true)
                .set(gameOptions.getAo(), true)
                .set(gameOptions.getVignette(), true)
        );
        return arrayList;
    }

    private static List<OptionsCategory.Preset> buildSoundPresets(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        OptionsCategory.Preset preset = new OptionsCategory.Preset("По умолчанию");
        OptionsCategory.Preset preset2 = new OptionsCategory.Preset("Без звука");
        OptionsCategory.Preset preset3 = new OptionsCategory.Preset("Только музыка");

        for (SoundCategory soundCategory : SoundCategory.values()) {
            preset.set(gameOptions.getSoundVolumeOption(soundCategory), 1.0);
            preset2.set(gameOptions.getSoundVolumeOption(soundCategory), 0.0);
            double d = soundCategory != SoundCategory.MASTER && soundCategory != SoundCategory.MUSIC ? 0.0 : 1.0;
            preset3.set(gameOptions.getSoundVolumeOption(soundCategory), d);
        }

        ArrayList arrayList = new ArrayList();
        arrayList.add(preset);
        arrayList.add(preset2);
        arrayList.add(preset3);
        return arrayList;
    }

    private static List<SettingEntry> buildSound(MinecraftClient minecraftClient) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(SettingEntry.createHeader("Основное"));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.MASTER));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.MUSIC));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.RECORDS));
        arrayList.add(SettingEntry.createHeader("Окружение"));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.WEATHER));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.BLOCKS));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.AMBIENT));
        arrayList.add(SettingEntry.createHeader("Существа"));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.HOSTILE));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.NEUTRAL));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.PLAYERS));
        arrayList.add(SettingEntry.createHeader("Прочее"));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.VOICE));
        arrayList.add(soundVolumeOption(minecraftClient, SoundCategory.UI));
        return arrayList;
    }

    private static SettingEntry soundVolumeOption(MinecraftClient minecraftClient, SoundCategory soundCategory) {
        return new SettingEntry(
            getSoundCategoryIcon(soundCategory),
            getSoundCategoryName(soundCategory),
            new SliderOptionWidget(OptionsUtil.soundVolume(minecraftClient, soundCategory), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
        );
    }

    private static List<SettingEntry> buildGeneral(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        arrayList.add(SettingEntry.createHeader("Геймплей"));
        arrayList.add(new SettingEntry(Icon.RUN, "Авто-прыжок", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getAutoJump()))));
        arrayList.add(new SettingEntry(Icon.EYE, "Покачивание камеры", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getBobView()))));
        arrayList.add(new SettingEntry(Icon.TOGGLE_LEFT, "Переключать присед", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getSneakToggled()))));
        arrayList.add(new SettingEntry(Icon.TOGGLE_RIGHT, "Переключать бег", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getSprintToggled()))));
        arrayList.add(SettingEntry.createHeader("Интерфейс"));
        arrayList.add(new SettingEntry(Icon.INFO_CIRCLE, "Субтитры", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getShowSubtitles()))));
        arrayList.add(new SettingEntry(Icon.DEVICE_FLOPPY, "Индикатор автосохранения", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getShowAutosaveIndicator()))));
        arrayList.add(new SettingEntry(Icon.SIGNAL, "Уведомления Realms", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getRealmsNotifications()))));
        return arrayList;
    }

    private static List<SettingEntry> buildVideo(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        int[] ints = OptionsUtil.getIntRange(gameOptions.getViewDistance(), 2, 32);
        int[] ints2 = OptionsUtil.getIntRange(gameOptions.getSimulationDistance(), 5, 32);
        int[] ints3 = OptionsUtil.getIntRange(gameOptions.getMaxFps(), 10, 260);
        int i = ints3[1];
        int[] ints4 = OptionsUtil.getIntRange(gameOptions.getFov(), 30, 110);
        arrayList.add(SettingEntry.createHeader("Производительность"));
        arrayList.add(
            new SettingEntry(
                Icon.DISPLAY,
                "Графика",
                new CycleOptionWidget<>(
                    gameOptions.getPreset(),
                    new GraphicsMode[]{GraphicsMode.FAST, GraphicsMode.FANCY, GraphicsMode.FABULOUS},
                    OptionsCategory::graphicsModeLabel
                )
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.GAUGE,
                "Прорисовка",
                new SliderOptionWidget(OptionsUtil.intSlider(gameOptions.getViewDistance()), ints[0], ints[1], 1.0F, f -> Math.round(f) + " ч.")
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.SIGNAL,
                "Симуляция",
                new SliderOptionWidget(OptionsUtil.intSlider(gameOptions.getSimulationDistance()), ints2[0], ints2[1], 1.0F, f2 -> Math.round(f2) + " ч.")
            )
        );
        arrayList.add(
            new SettingEntry(
                    Icon.BOLT,
                    "Макс. FPS",
                    new SliderOptionWidget(
                        OptionsUtil.intSlider(gameOptions.getMaxFps()),
                        ints3[0],
                        ints3[1],
                        1.0F,
                        f3 -> Math.round(f3) >= i ? "∞" : Integer.toString(Math.round(f3))
                    )
                )
                .withKeywords("fps", "кадры", "framerate")
        );
        arrayList.add(
            new SettingEntry(Icon.REFRESH, "Верт. синхронизация", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getEnableVsync())))
                .withKeywords("vsync", "вертикальная синхронизация")
        );
        arrayList.add(SettingEntry.createHeader("Внешний вид"));
        arrayList.add(guiScaleOption(gameOptions));
        arrayList.add(
            new SettingEntry(
                Icon.DROPLET,
                "Облака",
                new CycleOptionWidget<>(
                    gameOptions.getCloudRenderMode(),
                    new CloudRenderMode[]{CloudRenderMode.OFF, CloudRenderMode.FAST, CloudRenderMode.FANCY},
                    OptionsCategory::cloudModeLabel
                )
            )
        );
        arrayList.add(new SettingEntry(Icon.USER, "Тени сущностей", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getEntityShadows()))));
        arrayList.add(new SettingEntry(Icon.PALETTE, "Плавное освещение", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getAo()))));
        arrayList.add(new SettingEntry(Icon.FACE_SMILE, "Виньетка", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getVignette()))));
        arrayList.add(
            new SettingEntry(
                Icon.CIRCLE_NODES,
                "Дальность сущностей",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getEntityDistanceScaling()), 0.5F, 5.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(SettingEntry.createHeader("Камера"));
        arrayList.add(
            new SettingEntry(
                    Icon.EYE,
                    "Поле зрения",
                    new SliderOptionWidget(OptionsUtil.intSlider(gameOptions.getFov()), ints4[0], ints4[1], 1.0F, f4 -> Math.round(f4) + "°")
                )
                .withKeywords("fov", "обзор")
        );
        arrayList.add(
            new SettingEntry(
                    Icon.FLAME,
                    "Яркость",
                    new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getGamma()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
                )
                .withKeywords("gamma", "гамма", "brightness")
        );
        return arrayList;
    }

    private static SettingEntry guiScaleOption(GameOptions gameOptions) {
        int[] ints = OptionsUtil.getIntRange(gameOptions.getGuiScale(), 0, 4);
        String string = Language.translate("settings.gui_scale.auto", "Авто");
        return new SettingEntry(
                Icon.LAYOUT_DASHBOARD,
                Language.translate("settings.gui_scale", "Размер интерфейса"),
                new SliderOptionWidget(
                    OptionsUtil.deferredIntSlider(gameOptions.getGuiScale()),
                    ints[0],
                    ints[1],
                    1.0F,
                    f -> Math.round(f) == 0 ? string : Integer.toString(Math.round(f))
                )
            )
            .withKeywords("gui scale", "масштаб", "интерфейс", "размер");
    }

    private static List<SettingEntry> buildChat(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        arrayList.add(SettingEntry.createHeader("Поведение"));
        arrayList.add(
            new SettingEntry(
                Icon.EYE,
                "Видимость чата",
                new CycleOptionWidget<>(
                    gameOptions.getChatVisibility(),
                    new ChatVisibility[]{ChatVisibility.FULL, ChatVisibility.SYSTEM, ChatVisibility.HIDDEN},
                    OptionsCategory::chatVisibilityLabel
                )
            )
        );
        arrayList.add(new SettingEntry(Icon.PALETTE, "Цвета чата", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getChatColors()))));
        arrayList.add(new SettingEntry(Icon.LINK, "Ссылки в чате", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getChatLinks()))));
        arrayList.add(new SettingEntry(Icon.INFO_CIRCLE, "Подтверждать ссылки", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getChatLinksPrompt()))));
        arrayList.add(new SettingEntry(Icon.SEARCH, "Автодополнение команд", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getAutoSuggestions()))));
        arrayList.add(SettingEntry.createHeader("Размер"));
        arrayList.add(
            new SettingEntry(
                Icon.GAUGE,
                "Масштаб текста",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatScale()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.LAYOUT_DASHBOARD,
                "Ширина чата",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatWidth()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.SORT_ASC,
                "Высота (фокус)",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatHeightFocused()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.SORT_DESC,
                "Высота (свёрнут)",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatHeightUnfocused()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(SettingEntry.createHeader("Вид"));
        arrayList.add(
            new SettingEntry(
                Icon.DROPLET,
                "Непрозрачность",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatOpacity()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.PALETTE,
                "Фон текста",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getTextBackgroundOpacity()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        arrayList.add(
            new SettingEntry(
                Icon.SORT,
                "Межстрочный интервал",
                new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getChatLineSpacing()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
            )
        );
        return arrayList;
    }

    private static String chatVisibilityLabel(ChatVisibility chatVisibility) {
        switch (chatVisibility) {
            case FULL:
                return "Показывать";
            case SYSTEM:
                return "Команды";
            case HIDDEN:
                return "Скрыт";
            default:
                throw new MatchException(null, null);
        }
    }

    private static List<SettingEntry> buildMouse(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        if (OptionsUtil.isUnitRange(gameOptions.getMouseSensitivity())) {
            arrayList.add(
                new SettingEntry(
                        Icon.GAUGE,
                        "Чувствительность",
                        new SliderOptionWidget(
                            OptionsUtil.doubleSlider(gameOptions.getMouseSensitivity()), 0.0F, 1.0F, 0.0F, f -> Math.round(f * 200.0F) + "%"
                        )
                    )
                    .withKeywords("sens", "sensitivity", "мышь")
            );
        }

        if (OptionsUtil.isUnitRange(gameOptions.getMouseWheelSensitivity())) {
            arrayList.add(
                new SettingEntry(
                    Icon.SORT,
                    "Скорость прокрутки",
                    new SliderOptionWidget(OptionsUtil.doubleSlider(gameOptions.getMouseWheelSensitivity()), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)
                )
            );
        }

        arrayList.add(new SettingEntry(Icon.SORT_ALPHA_DOWN, "Инверсия оси X", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getInvertMouseX()))));
        arrayList.add(new SettingEntry(Icon.SORT_ALPHA_UP, "Инверсия оси Y", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getInvertMouseY()))));
        arrayList.add(new SettingEntry(Icon.MICROCHIP, "Необработанный ввод", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getRawMouseInput()))));
        arrayList.add(new SettingEntry(Icon.SORT, "Дискретная прокрутка", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getDiscreteMouseScroll()))));
        arrayList.add(new SettingEntry(Icon.FACE_SMILE, "Сенсорный режим", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getTouchscreen()))));
        arrayList.add(new SettingEntry(Icon.CIRCLE_NODES, "Изменение курсора", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getAllowCursorChanges()))));
        return arrayList;
    }

    private static List<SettingEntry> buildKeyboard(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();

        for (KeyBinding keyBinding : gameOptions.allKeys) {
            String string = KeyBinding.getLocalizedName(keyBinding.getId()).get().getString();
            arrayList.add(
                new SettingEntry(Icon.KEYBOARD, string, new KeybindOptionWidget(keyBinding)).withKeywords(keyBinding.getBoundKeyLocalizedText().getString(), keyBinding.getId())
            );
        }

        return arrayList;
    }

    private static List<SettingEntry> buildAccessibility(MinecraftClient minecraftClient) {
        GameOptions gameOptions = minecraftClient.options;
        ArrayList arrayList = new ArrayList();
        int[] ints = OptionsUtil.getIntRange(gameOptions.getMenuBackgroundBlurriness(), 0, 10);
        arrayList.add(SettingEntry.createHeader("Текст и диктор"));
        arrayList.add(
            new SettingEntry(
                Icon.VOLUME_HIGH, "Диктор", new CycleOptionWidget<>(gameOptions.getNarrator(), NarratorMode.values(), OptionsCategory::narratorModeLabel)
            )
        );
        arrayList.add(new SettingEntry(Icon.INFO_CIRCLE, "Субтитры", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getShowSubtitles()))));
        arrayList.add(new SettingEntry(Icon.VOLUME_HIGH, "Клавиша диктора", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getNarratorHotkey()))));
        arrayList.add(SettingEntry.createHeader("Фон и эффекты"));
        arrayList.add(
            new SettingEntry(
                Icon.DROPLET,
                "Размытие фона",
                new SliderOptionWidget(
                    OptionsUtil.intSlider(gameOptions.getMenuBackgroundBlurriness()), ints[0], ints[1], 1.0F, f -> Integer.toString(Math.round(f))
                )
            )
        );
        addUnitSlider(arrayList, Icon.PALETTE, "Фон текста", gameOptions.getTextBackgroundOpacity());
        arrayList.add(new SettingEntry(Icon.LAYOUT_DASHBOARD, "Фон только для чата", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getBackgroundForChatOnly()))));
        addUnitSlider(arrayList, Icon.EYE, "Эффект искажения", gameOptions.getDistortionEffectScale());
        addUnitSlider(arrayList, Icon.GAUGE, "Эффект поля зрения", gameOptions.getFovEffectScale());
        addUnitSlider(arrayList, Icon.HEART, "Наклон при уроне", gameOptions.getDamageTiltStrength());
        addUnitSlider(arrayList, Icon.BOLT, "Скорость мерцания", gameOptions.getGlintSpeed());
        addUnitSlider(arrayList, Icon.BOLT, "Сила мерцания", gameOptions.getGlintStrength());
        addUnitSlider(arrayList, Icon.CLOCK_HOUR_4, "Время уведомления", gameOptions.getNotificationDisplayTime());
        arrayList.add(SettingEntry.createHeader("Контраст и прочее"));
        arrayList.add(new SettingEntry(Icon.SIGNAL, "Высокий контраст", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getHighContrast()))));
        arrayList.add(new SettingEntry(Icon.PACKAGE, "Контур блоков (контраст)", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getHighContrastBlockOutline()))));
        arrayList.add(new SettingEntry(Icon.PALETTE, "Ч/б логотип", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getMonochromeLogo()))));
        arrayList.add(new SettingEntry(Icon.FACE_SMILE, "Скрывать сплэши", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getHideSplashTexts()))));
        arrayList.add(new SettingEntry(Icon.BOLT, "Скрывать зарницы", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getHideLightningFlashes()))));
        arrayList.add(new SettingEntry(Icon.RUN, "Поворот с вагонеткой", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getRotateWithMinecart()))));
        arrayList.add(new SettingEntry(Icon.INFO_CIRCLE, "Сокращённая отладка", new ToggleOptionWidget(OptionsUtil.toggle(gameOptions.getReducedDebugInfo()))));
        return arrayList;
    }

    private static void addUnitSlider(List<SettingEntry> settingEntries, Icon icon2, String string, SimpleOption<Double> simpleOption) {
        if (OptionsUtil.isUnitRange(simpleOption)) {
            settingEntries.add(new SettingEntry(icon2, string, new SliderOptionWidget(OptionsUtil.doubleSlider(simpleOption), 0.0F, 1.0F, 0.0F, SliderOptionWidget.PERCENT_FORMAT)));
        }
    }

    private static String narratorModeLabel(NarratorMode narratorMode) {
        switch (narratorMode) {
            case OFF:
                return "Выкл";
            case ALL:
                return "Всё";
            case CHAT:
                return "Чат";
            case SYSTEM:
                return "Система";
            default:
                throw new MatchException(null, null);
        }
    }

    private static String graphicsModeLabel(GraphicsMode graphicsMode) {
        switch (graphicsMode) {
            case FAST:
                return "Быстрая";
            case FANCY:
                return "Красивая";
            case FABULOUS:
                return "Превосходная";
            case CUSTOM:
                return "Своя";
            default:
                throw new MatchException(null, null);
        }
    }

    private static String cloudModeLabel(CloudRenderMode cloudRenderMode) {
        switch (cloudRenderMode) {
            case OFF:
                return "Выкл";
            case FAST:
                return "Быстрые";
            case FANCY:
                return "Красивые";
            default:
                throw new MatchException(null, null);
        }
    }

    private static String getSoundCategoryName(SoundCategory soundCategory) {
        switch (soundCategory) {
            case MASTER:
                return "Общая";
            case MUSIC:
                return "Музыка";
            case RECORDS:
                return "Пластинки";
            case WEATHER:
                return "Погода";
            case BLOCKS:
                return "Блоки";
            case HOSTILE:
                return "Враждебные";
            case NEUTRAL:
                return "Дружелюбные";
            case PLAYERS:
                return "Игроки";
            case AMBIENT:
                return "Окружение";
            case VOICE:
                return "Голос";
            case UI:
                return "Интерфейс";
            default:
                throw new MatchException(null, null);
        }
    }

    private static Icon getSoundCategoryIcon(SoundCategory soundCategory) {
        switch (soundCategory) {
            case MASTER:
                return Icon.VOLUME_HIGH;
            case MUSIC:
            case RECORDS:
                return Icon.MUSIC;
            case WEATHER:
                return Icon.DROPLET;
            case BLOCKS:
                return Icon.PACKAGE;
            case HOSTILE:
                return Icon.SKULL;
            case NEUTRAL:
                return Icon.DRUMSTICK;
            case PLAYERS:
                return Icon.USER;
            case AMBIENT:
                return Icon.GLOBE;
            case VOICE:
                return Icon.MICROCHIP;
            case UI:
                return Icon.LAYOUT_DASHBOARD;
            default:
                throw new MatchException(null, null);
        }
    }

    public static final class Preset {
        public final String name;
        private final List<Runnable> applyActions = new ArrayList<>();
        private final List<BooleanSupplier> activeChecks = new ArrayList<>();

        public Preset(String string) {
            this.name = string;
        }

        public <T> OptionsCategory.Preset set(SimpleOption<T> simpleOption, T value) {
            this.applyActions.add(() -> simpleOption.setValue(value));
            this.activeChecks.add(() -> Objects.equals(simpleOption.getValue(), value));
            return this;
        }

        public void apply() {
            Iterator iterator2 = this.applyActions.iterator();

            while (iterator2.hasNext()) {
                ((Runnable)iterator2.next()).run();
            }
        }

        public boolean isActive() {
            if (this.activeChecks.isEmpty()) {
                return false;
            }

            Iterator iterator2 = this.activeChecks.iterator();

            while (iterator2.hasNext()) {
                if (!((BooleanSupplier)iterator2.next()).getAsBoolean()) {
                    return false;
                }
            }

            return true;
        }
    }
}
