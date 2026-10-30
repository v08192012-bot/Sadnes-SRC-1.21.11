package tech.sadnes.client.feature.impl.hud;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Bind;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureBind;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "Hotkeys",
    description = "feature.impl.hud.keybinds",
    displayName = "feature.impl.hud.keybinds",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_BINDS
)
public class Hotkeys extends HudFeature<Hotkeys.HotkeyEntry> {
    private static final float ICON_SIZE = 8.0F;
    private final Map<Object, Hotkeys.HotkeyEntry> entryCache = new IdentityHashMap<>();
    private final List<Hotkeys.HotkeyEntry> hotkeyEntries = new ArrayList<>();
    private final StopWatch refreshTimer = new StopWatch();
    private boolean initialized;

    public Hotkeys() {
        super(10.0F, 30.0F);
    }

    @Override
    public String getTitle() {
        return "Hotkeys";
    }

    @Override
    public Icon getIcon() {
        return Icon.RECTANGLE_LIST;
    }

    @Override
    public void collectEntries(List<Hotkeys.HotkeyEntry> hotkeyEntries2) {
        if (!this.initialized || this.refreshTimer.hasPassed(500.0)) {
            this.rebuildEntries();
            this.refreshTimer.reset();
            this.initialized = true;
        }

        int i = this.hotkeyEntries.size();

        for (int i2 = 0; i2 < i; i2++) {
            Hotkeys.HotkeyEntry hotkeyEntry = this.hotkeyEntries.get(i2);
            if (hotkeyEntry.keybindSetting != null) {
                String string = getPressedKeyName(hotkeyEntry.keybindSetting);
                hotkeyEntry.active = hotkeyEntry.keybindFeature.enabled() && string != null;
                if (string != null) {
                    hotkeyEntry.keyName = string;
                }

                hotkeyEntry.enabled = true;
            } else {
                hotkeyEntry.active = hotkeyEntry.feature != null
                    ? isFeatureBindActive(hotkeyEntry.feature, (FeatureBind)hotkeyEntry.bind)
                    : isSettingBindActive(hotkeyEntry.setting, (ValueBind<?>)hotkeyEntry.bind);
                hotkeyEntry.enabled = hotkeyEntry.feature != null
                    ? hotkeyEntry.feature.enabled()
                    : hotkeyEntry.setting instanceof BooleanSetting booleanSetting && Boolean.TRUE.equals(booleanSetting.getValue());
            }

            if (hotkeyEntry.active || hotkeyEntry.animation.get() > 0.001) {
                hotkeyEntries2.add(hotkeyEntry);
            }
        }
    }

    private void rebuildEntries() {
        this.hotkeyEntries.clear();

        for (Feature feature2 : Sadnes.getInstance().getFeatureManager().getHudFeatures()) {
            for (FeatureBind featureBind : feature2.getBinds()) {
                if (featureBind.isVisible() && !feature2.handleBindHold(featureBind)) {
                    Hotkeys.HotkeyEntry hotkeyEntry = this.entryCache.get(featureBind);
                    if (hotkeyEntry == null) {
                        hotkeyEntry = new Hotkeys.HotkeyEntry(feature2.getInfo().name(), null, featureBind, feature2, null, null);
                        this.entryCache.put(featureBind, hotkeyEntry);
                    } else {
                        hotkeyEntry.update(feature2.getInfo().name(), null);
                    }

                    this.hotkeyEntries.add(hotkeyEntry);
                }
            }

            this.addSettingBinds(feature2.getSettings(), null);
        }

        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            for (ScriptFeature scriptFeature : scriptManager.getActiveHudElements()) {
                this.addSettingBinds(scriptFeature.getSettings(), scriptFeature);
            }
        }

        for (Feature feature3 : Sadnes.getInstance().getFeatureManager().values()) {
            for (Setting<?> setting : feature3.getSettings()) {
                this.addKeybindSetting(feature3, setting);

                for (Setting<?> setting2 : setting.getAllChildren()) {
                    this.addKeybindSetting(feature3, setting2);
                }
            }
        }

        for (Hotkeys.HotkeyEntry hotkeyEntry2 : this.hotkeyEntries) {
            hotkeyEntry2.displayName = getDisplayName(hotkeyEntry2);
            if (hotkeyEntry2.bind != null) {
                hotkeyEntry2.keyName = hotkeyEntry2.bind.getDisplayString();
            }
        }

        this.entryCache.values().removeIf(hotkeyEntry3 -> !this.hotkeyEntries.contains(hotkeyEntry3));
    }

    private void addKeybindSetting(Feature feature, Setting<?> setting) {
        if (setting instanceof KeybindSetting keybindSetting && keybindSetting.getCategory() != null && keybindSetting.isBound()) {
            Hotkeys.HotkeyEntry hotkeyEntry = this.entryCache.get(keybindSetting);
            if (hotkeyEntry == null) {
                Hotkeys.HotkeyEntry hotkeyEntry2 = new Hotkeys.HotkeyEntry(keybindSetting.getCategory(), feature, keybindSetting);
                this.entryCache.put(keybindSetting, hotkeyEntry2);
                this.hotkeyEntries.add(hotkeyEntry2);
            } else {
                hotkeyEntry.update(keybindSetting.getCategory(), null);
                this.hotkeyEntries.add(hotkeyEntry);
            }
        }
    }

    private static String getPressedKeyName(KeybindSetting keybindSetting) {
        int i = keybindSetting.getDefaultValue();
        if (mc.currentScreen == null && Key.isPressed(i)) {
            return Key.getDisplayName(i);
        }

        for (ValueBind valueBind : keybindSetting.getBinds()) {
            if (valueBind.isVisible() && keybindSetting.isBindPressed(valueBind)) {
                return valueBind.getDisplayString();
            }
        }

        return null;
    }

    private void addSettingBinds(List<Setting<?>> settings, ScriptFeature scriptFeature) {
        List<Setting<?>> settings2 = new ArrayList<>();

        for (Setting<?> setting : settings) {
            settings2.add(setting);
            settings2.addAll(setting.getAllChildren());
        }

        Map<Object, String> map = new IdentityHashMap<>();

        for (Setting<?> setting2 : settings2) {
            if (setting2 instanceof MultiSetting multiSetting) {
                for (MultiSetting.Entry entry : multiSetting.getEntries()) {
                    map.put(entry.value(), multiSetting.getName());
                }
            } else if (setting2 instanceof ModeSetting modeSetting) {
                for (String string : modeSetting.getModes()) {
                    BooleanSetting booleanSetting = modeSetting.getModeToggle(string);
                    if (booleanSetting != null) {
                        map.put(booleanSetting, modeSetting.getName());
                    }
                }
            }
        }

        for (Setting<?> setting3 : settings2) {
            if (setting3 instanceof ValueSetting<?> valueSetting && !(setting3 instanceof KeybindSetting) && !valueSetting.getBinds().isEmpty()) {
                String string2 = map.get(setting3);
                String string3 = scriptFeature != null && setting3 == scriptFeature.getEnabledSetting() ? scriptFeature.getDisplayName() : setting3.getName();

                for (ValueBind<?> valueBind : valueSetting.getBinds()) {
                    if (valueBind.isVisible()) {
                        Hotkeys.HotkeyEntry hotkeyEntry = this.entryCache.get(valueBind);
                        if (hotkeyEntry == null) {
                            hotkeyEntry = new Hotkeys.HotkeyEntry(string3, getBindOptionText(valueSetting, valueBind), valueBind, null, valueSetting, string2);
                            this.entryCache.put(valueBind, hotkeyEntry);
                        } else {
                            hotkeyEntry.update(string3, getBindOptionText(valueSetting, valueBind));
                        }

                        this.hotkeyEntries.add(hotkeyEntry);
                    }
                }
            }
        }
    }

    private static boolean isFeatureBindActive(Feature feature, FeatureBind featureBind) {
        if (feature.handleBindRelease(featureBind)) {
            return true;
        } else {
            return hasActiveToggleBind(feature) ? false : feature.handleBindPress(featureBind);
        }
    }

    private static boolean isSettingBindActive(ValueSetting<?> valueSetting, ValueBind<?> valueBind) {
        if (valueSetting.isToggleBindActive(valueBind)) {
            return true;
        } else {
            return hasActiveSettingToggleBind(valueSetting) ? false : valueSetting.isBindActive(valueBind);
        }
    }

    private static boolean hasActiveToggleBind(Feature feature) {
        for (FeatureBind featureBind : feature.getBinds()) {
            if (featureBind.isVisible() && !feature.handleBindHold(featureBind) && feature.handleBindRelease(featureBind)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasActiveSettingToggleBind(ValueSetting<?> valueSetting) {
        List list = valueSetting.getBinds();
        int i = list.size();

        for (int i2 = 0; i2 < i; i2++) {
            ValueBind valueBind = (ValueBind)list.get(i2);
            if (valueBind.isVisible() && valueSetting.isToggleBindActive(valueBind)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected float getEntryWidth(Hotkeys.HotkeyEntry hotkeyEntry) {
        if (hasToggleIcon(hotkeyEntry)) {
            return 8.0F;
        } else {
            return hasOptionText(hotkeyEntry) ? BODY_FONT.getWidth(hotkeyEntry.option, 6.0F) : 0.0F;
        }
    }

    @Override
    protected float getEntryValueWidth(Hotkeys.HotkeyEntry hotkeyEntry) {
        return BODY_FONT.getWidth(hotkeyEntry.keyName, 6.0F);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, Hotkeys.HotkeyEntry hotkeyEntry) {
        if (hasToggleIcon(hotkeyEntry)) {
            int i = ColorUtil.setAlphaf(Theme.getTextColor(), f4);
            Icon icon2 = hotkeyEntry.enabled ? Icon.TOGGLE_RIGHT : Icon.TOGGLE_LEFT;
            new RenderUtil(drawContext, f, f2 + f3 / 2.0F - 4.0F, 8.0F, 8.0F).icon(icon2, 0.0F, 0.0F, 8.0F, 8.0F, i);
        } else {
            if (hasOptionText(hotkeyEntry)) {
                float f5 = BODY_FONT.getWidth(hotkeyEntry.option, 6.0F);
                new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, hotkeyEntry.option, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
            }
        }
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, Hotkeys.HotkeyEntry hotkeyEntry) {
        float f5 = BODY_FONT.getWidth(hotkeyEntry.keyName, 6.0F);
        new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, hotkeyEntry.keyName, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
    }

    @Override
    protected String getEntryText(Hotkeys.HotkeyEntry hotkeyEntry) {
        return hotkeyEntry.displayName;
    }

    private static String getDisplayName(Hotkeys.HotkeyEntry hotkeyEntry) {
        if (hotkeyEntry.parentName != null) {
            return Language.translateOption(hotkeyEntry.parentName, hotkeyEntry.name);
        } else {
            return hotkeyEntry.setting == null && hotkeyEntry.keybindSetting == null ? hotkeyEntry.name : Language.translate(hotkeyEntry.name);
        }
    }

    private static boolean hasToggleIcon(Hotkeys.HotkeyEntry hotkeyEntry) {
        return hotkeyEntry.feature != null || hotkeyEntry.keybindSetting != null || hotkeyEntry.setting instanceof BooleanSetting;
    }

    private static boolean hasOptionText(Hotkeys.HotkeyEntry hotkeyEntry) {
        return !hasToggleIcon(hotkeyEntry) && hotkeyEntry.option != null && !hotkeyEntry.option.isEmpty();
    }

    private static <T> String getBindOptionText(ValueSetting<T> valueSetting, ValueBind<?> valueBind) {
        @SuppressWarnings("unchecked")
        ValueBind<T> valueBind2 = (ValueBind<T>)valueBind;
        return valueSetting.formatBindValue(valueBind2);
    }

    public static Hotkeys getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Hotkeys.class);
    }

    public static class HotkeyEntry extends HudEntry {
        public String name;
        public String option;
        public String displayName;
        public String keyName;
        public boolean enabled;
        public final Bind bind;
        public final Feature feature;
        public final ValueSetting<?> setting;
        public final String parentName;
        public final Feature keybindFeature;
        public final KeybindSetting keybindSetting;

        public HotkeyEntry(String string, String string2, Bind bind2, Feature feature2, ValueSetting<?> valueSetting, String string3) {
            this.name = string;
            this.option = string2;
            this.bind = bind2;
            this.feature = feature2;
            this.setting = valueSetting;
            this.parentName = string3;
            this.keybindFeature = null;
            this.keybindSetting = null;
        }

        public HotkeyEntry(String string, Feature feature2, KeybindSetting keybindSetting2) {
            this.name = string;
            this.bind = null;
            this.feature = null;
            this.setting = null;
            this.parentName = null;
            this.keybindFeature = feature2;
            this.keybindSetting = keybindSetting2;
        }

        void update(String string, String string2) {
            this.name = string;
            this.option = string2;
        }
    }
}
