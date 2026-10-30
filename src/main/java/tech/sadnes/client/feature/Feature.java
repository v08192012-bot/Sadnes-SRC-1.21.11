package tech.sadnes.client.feature;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.feature.impl.hud.Notifications;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.server.MarkerServer;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import ua.mintantileak.profile.Profile;

public abstract class Feature implements Module, IMinecraft {
    private static volatile boolean initialized = false;
    private final FeatureInfo featureInfo;
    private final String name;
    private final Category category;
    private final Subcategory subcategory;
    private final String description;
    private final boolean autoEnabled;
    private final boolean allowDisable;
    private final boolean hidden;
    private final Beta beta;
    private final Danger danger;
    private final List<FeatureBind> binds;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final Map<FeatureBind, Long> activeBinds = new IdentityHashMap<>();
    private long bindOrderCounter;
    private boolean toggledEnabled;
    private boolean enabled;
    private transient FeatureBind triggeringBind;

    public static boolean isInitialized() {
        return initialized;
    }

    public static void setInitialized(boolean flag) {
        initialized = flag;
    }

    public Feature() {
        Class<? extends Feature> clazz = this.getClass();
        FeatureInfo featureInfo2 = clazz.getAnnotation(FeatureInfo.class);
        if (featureInfo2 == null) {
            throw new IllegalStateException("@FeatureInfo annotation is missing for " + clazz.getName());
        }

        this.featureInfo = featureInfo2;
        this.name = featureInfo2.name();
        this.category = featureInfo2.category();
        Subcategory subcategory2 = featureInfo2.subcategory();
        if (subcategory2 != Subcategory.NONE && subcategory2.getCategory() != this.category) {
            throw new IllegalStateException(
                "@FeatureInfo "
                    + clazz.getName()
                    + " declares subcategory "
                    + subcategory2
                    + " whose parent "
                    + subcategory2.getCategory()
                    + " doesn't match category "
                    + this.category
            );
        }

        this.subcategory = subcategory2;
        this.description = featureInfo2.description();
        this.autoEnabled = featureInfo2.autoEnabled();
        this.allowDisable = featureInfo2.allowDisable();
        this.hidden = featureInfo2.hidden();
        this.beta = clazz.getAnnotation(Beta.class);
        this.danger = clazz.getAnnotation(Danger.class);
        this.binds = this.createBinds(featureInfo2, clazz);
    }

    private List<FeatureBind> createBinds(FeatureInfo featureInfo2, Class<? extends Feature> clazz) {
        int[] ints = featureInfo2.keys();
        BindMode[] bindModes = featureInfo2.bindTypes();
        if (bindModes.length != 0 && bindModes.length != ints.length) {
            throw new IllegalStateException("bindTypes length must match keys length in @FeatureInfo for " + clazz.getName());
        }

        ArrayList arrayList = new ArrayList(ints.length);

        for (int i = 0; i < ints.length; i++) {
            BindMode bindMode = bindModes.length == 0 ? BindMode.TOGGLE : bindModes[i];
            FeatureBind featureBind = new FeatureBind(ints[i], bindMode);
            if (!this.matchesBinds(arrayList, featureBind.getKey(), featureBind.getMode(), featureBind.getEnableState(), featureBind.getModifiers())) {
                arrayList.add(featureBind);
            }
        }

        return arrayList;
    }

    public void initialize() {
        this.settings.clear();
        List list = this.settings;
        List list2 = this.collectSettings();
        list.addAll(list2);
    }

    public void destroy() {
        this.settings.clear();
    }

    public void onKey(int i, boolean flag) {
        boolean flag2 = false;
        FeatureBind featureBind = null;

        for (FeatureBind featureBind2 : this.binds) {
            if (featureBind2.matches(i)) {
                if (featureBind2.getMode() == BindMode.TOGGLE) {
                    if (flag && this.activateBind(featureBind2)) {
                        flag2 = true;
                        featureBind = featureBind2;
                    }
                } else if (flag) {
                    if (this.deactivateBind(featureBind2)) {
                        flag2 = true;
                        featureBind = featureBind2;
                    }
                } else if (this.isBindActive(featureBind2)) {
                    flag2 = true;
                    featureBind = featureBind2;
                }
            }
        }

        if (!this.activeBinds.isEmpty()) {
            for (FeatureBind featureBind3 : this.binds) {
                if (featureBind3.getMode() == BindMode.HOLD && this.activeBinds.containsKey(featureBind3) && !featureBind3.isHeld() && this.isBindActive(featureBind3)) {
                    flag2 = true;
                    featureBind = featureBind3;
                }
            }
        }

        if (flag2) {
            this.triggeringBind = featureBind;
            this.applyState(false);
            this.triggeringBind = null;
        }

        for (Setting setting : this.settings) {
            setting.handleKey(i, flag);
        }
    }

    public void onKeyRelease(int i) {
        this.onKey(i, true);
    }

    public boolean isBoundTo(int i) {
        for (FeatureBind featureBind : this.binds) {
            if (featureBind.getKey() == i) {
                return true;
            }
        }

        return false;
    }

    public boolean handleBindPress(FeatureBind featureBind) {
        if (featureBind != null && this.binds.contains(featureBind)) {
            return featureBind.getMode() == BindMode.TOGGLE ? this.enabled == featureBind.getEnableState() && this.activeBinds.isEmpty() : this.activeBinds.containsKey(featureBind);
        } else {
            return false;
        }
    }

    public boolean handleBindRelease(FeatureBind featureBind) {
        return featureBind == null || featureBind.getMode() != BindMode.TOGGLE || !this.binds.contains(featureBind) ? false : this.toggledEnabled == featureBind.getEnableState();
    }

    public boolean handleBindHold(FeatureBind featureBind) {
        if (featureBind == null) {
            return false;
        }

        int[] ints = this.featureInfo.keys();
        BindMode[] bindModes = this.featureInfo.bindTypes();
        int i = ints.length;

        for (int i2 = 0; i2 < i; i2++) {
            BindMode bindMode = bindModes.length == 0 ? BindMode.TOGGLE : bindModes[i2];
            if (featureBind.getKey() == ints[i2] && featureBind.getMode() == bindMode && featureBind.getEnableState()) {
                return true;
            }
        }

        return false;
    }

    public Feature addBind(int i, BindMode bindMode) {
        return this.addBind(new FeatureBind(i, bindMode));
    }

    public Feature addBind(int i, BindMode bindMode, int[] ints) {
        FeatureBind featureBind = new FeatureBind(i, bindMode);
        if (ints != null) {
            featureBind.setModifiers(ints);
            return this.addBind(featureBind);
        } else {
            featureBind.setModifiers(FeatureBind.NO_MODIFIERS);
            return this.addBind(featureBind);
        }
    }

    public Feature addBind(FeatureBind featureBind) {
        if (featureBind != null && !this.matchesBinds(this.binds, featureBind.getKey(), featureBind.getMode(), featureBind.getEnableState(), featureBind.getModifiers())) {
            this.binds.add(featureBind);
            this.applyState(false);
            return this;
        } else {
            return this;
        }
    }

    public Feature removeBind(FeatureBind featureBind) {
        if (featureBind == null) {
            return this;
        }

        this.binds.remove(featureBind);
        this.activeBinds.remove(featureBind);
        this.applyState(false);
        return this;
    }

    public Feature clearBinds() {
        this.binds.clear();
        this.activeBinds.clear();
        this.applyState(false);
        return this;
    }

    public boolean hasBinds() {
        int i = this.settings.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (this.settings.get(i2).hasBindsRecursive()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void toggle() {
        if (!this.toggledEnabled) {
            this.toggledEnabled = true;
            this.applyState(false);
        } else {
            this.toggledEnabled = false;
            this.applyState(false);
        }
    }

    public void setEnabled(boolean flag) {
        this.setEnabled(flag, false);
    }

    public void setEnabled(boolean flag, boolean flag2) {
        if (flag2 && !flag) {
            this.activeBinds.clear();
        }

        this.toggledEnabled = flag;
        this.applyState(flag2);
    }

    private void applyState(boolean flag) {
        boolean flag2 = this.toggledEnabled;
        long l = Long.MIN_VALUE;

        for (Entry entry : this.activeBinds.entrySet()) {
            if ((Long)entry.getValue() > l) {
                l = (Long)entry.getValue();
                flag2 = ((FeatureBind)entry.getKey()).getEnableState();
            }
        }

        if (this.enabled != flag2 && (this.allowDisable || flag2 || flag)) {
            this.enabled = flag2;
            if (flag2) {
                this.onDisable();
            } else {
                this.onEnable();
            }

            this.notifyToggle(flag2);
            ConfigManager configManager = Sadnes.getInstance().getConfigManager();
            if (configManager != null) {
                configManager.markDirty();
            }
        }
    }

    private void notifyToggle(boolean flag) {
        if (initialized && !this.isHidden() && !(this instanceof Notifications)) {
            if (this.triggeringBind == null || this.triggeringBind.isVisible()) {
                NotificationType notificationType = flag ? NotificationType.SUCCESS : NotificationType.ERROR;
                String string = this.getDisplayName();
                if (flag) {
                    NotificationUtil.notifyKeyed(notificationType, string, Language.translate("notification.module.enabled"));
                } else {
                    NotificationUtil.notifyKeyed(notificationType, string, Language.translate("notification.module.disabled"));
                }
            }
        }
    }

    public void onDisable() {
        Sadnes.getEventBus().subscribe(this);
    }

    public void onEnable() {
        Sadnes.getEventBus().unsubscribe(this);
    }

    private List<Setting<?>> collectSettings() {
        ArrayList arrayList = new ArrayList();
        Set set = Collections.newSetFromMap(new IdentityHashMap());

        for (Class clazz = this.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);

                    try {
                        Object object = field.get(this);
                        if (object instanceof Setting setting) {
                            this.registerSetting(field, setting);
                            if (!setting.isChild() && set.add(setting)) {
                                arrayList.add(setting);
                            }
                        } else if (object instanceof Map map) {
                            for (Object object2 : map.values()) {
                                if (object2 instanceof Setting setting2 && !setting2.isChild() && set.add(setting2)) {
                                    arrayList.add(setting2);
                                }
                            }
                        } else if (object instanceof Collection) {
                            for (Object object3 : (Collection)object) {
                                if (object3 instanceof Setting setting3 && !setting3.isChild() && set.add(setting3)) {
                                    arrayList.add(setting3);
                                }
                            }
                        }
                    } catch (IllegalAccessException illegalAccessException) {
                        throw new IllegalStateException("Failed to load setting field '" + field.getName() + "' for " + clazz.getName(), illegalAccessException);
                    }
                }
            }
        }

        return arrayList;
    }

    private void registerSetting(Field field, Setting<?> setting) {
        Beta beta2 = field.getAnnotation(Beta.class);
        if (beta2 != null) {
            setting.setSettingInfo(beta2);
        }

        Danger danger2 = field.getAnnotation(Danger.class);
        if (danger2 != null) {
            setting.setDependency(danger2);
        }
    }

    @Override
    public boolean isAccessible() {
        return this.beta == null || this.beta.scope().isAllowed(Profile.getRole());
    }

    @Override
    public boolean isBeta() {
        return this.beta != null;
    }

    @Override
    public MarkerLevel getDangerLevel() {
        return this.danger == null ? null : this.danger.level();
    }

    @Override
    public boolean isDangerActive() {
        return this.danger != null && MarkerServer.anyActive(this.danger.reasons());
    }

    public Beta getBetaInfo() {
        return this.beta;
    }

    @Override
    public Danger getDanger() {
        return this.danger;
    }

    public FeatureInfo getInfo() {
        return this.featureInfo;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    public Category getFeatureCategory() {
        return this.category;
    }

    @Override
    public Subcategory getSubcategory() {
        return this.subcategory;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getLocalizedDescription() {
        return Language.translate(this.getTranslationKey(), this.getDescription());
    }

    public String getTranslationKey() {
        String string = this.featureInfo.displayName();
        if (!string.isEmpty()) {
            return string;
        }

        String string2 = this.getClass().getPackageName();
        int i = string2.indexOf("feature.impl.");
        String string3 = i >= 0 ? string2.substring(i) : string2;
        return string3 + "." + this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    public boolean shouldAutoEnable() {
        return this.autoEnabled;
    }

    public boolean canDisable() {
        return this.allowDisable;
    }

    @Override
    public boolean isHidden() {
        return this.hidden;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(this.settings);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    private boolean matchesBinds(List<FeatureBind> featureBinds, int i, BindMode bindMode, boolean flag, int[] ints) {
        if (i == -1) {
            return false;
        }

        for (FeatureBind featureBind : featureBinds) {
            if (featureBind.getKey() == i && featureBind.getMode() == bindMode && featureBind.getEnableState() == flag && Arrays.equals(featureBind.getModifiers(), ints)) {
                return true;
            }
        }

        return false;
    }

    private boolean activateBind(FeatureBind featureBind) {
        this.activeBinds.remove(featureBind);
        if (!this.enabled) {
            this.toggledEnabled = true;
            return true;
        } else {
            this.toggledEnabled = false;
            return true;
        }
    }

    private boolean deactivateBind(FeatureBind featureBind) {
        if (this.activeBinds.containsKey(featureBind)) {
            return false;
        }

        this.activeBinds.put(featureBind, ++this.bindOrderCounter);
        return true;
    }

    private boolean isBindActive(FeatureBind featureBind) {
        return this.activeBinds.remove(featureBind) != null;
    }

    public FeatureInfo getFeatureInfo() {
        return this.featureInfo;
    }

    public String getName() {
        return this.name;
    }

    public Category getCategory() {
        return this.category;
    }

    public Subcategory subcategory() {
        return this.subcategory;
    }

    public String description() {
        return this.description;
    }

    public boolean isAutoEnabled() {
        return this.autoEnabled;
    }

    public boolean isAllowDisable() {
        return this.allowDisable;
    }

    public boolean hidden() {
        return this.hidden;
    }

    public Beta getBeta() {
        return this.beta;
    }

    public Danger danger() {
        return this.danger;
    }

    public List<FeatureBind> getBinds() {
        return this.binds;
    }

    public List<Setting<?>> rawSettings() {
        return this.settings;
    }

    public Map<FeatureBind, Long> getActiveBinds() {
        return this.activeBinds;
    }

    public long getBindOrderCounter() {
        return this.bindOrderCounter;
    }

    public boolean isToggledEnabled() {
        return this.toggledEnabled;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public FeatureBind getTriggeringBind() {
        return this.triggeringBind;
    }

    public void setBindOrderCounter(long l) {
        this.bindOrderCounter = l;
    }

    public void setToggledEnabled(boolean flag) {
        this.toggledEnabled = flag;
    }

    public void setTriggeringBind(FeatureBind featureBind) {
        this.triggeringBind = featureBind;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof Feature feature)) {
            return false;
        } else {
            if (!feature.canEqual(this)) {
                return false;
            }

            if (this.isAutoEnabled() != feature.isAutoEnabled()) {
                return false;
            }

            if (this.isAllowDisable() != feature.isAllowDisable()) {
                return false;
            }

            if (this.hidden() != feature.hidden()) {
                return false;
            }

            if (this.getBindOrderCounter() != feature.getBindOrderCounter()) {
                return false;
            }

            if (this.isToggledEnabled() != feature.isToggledEnabled()) {
                return false;
            }

            if (this.enabled() != feature.enabled()) {
                return false;
            }

            FeatureInfo featureInfo2 = this.getFeatureInfo();
            FeatureInfo featureInfo3 = feature.getFeatureInfo();
            if (featureInfo2 == null) {
                if (featureInfo3 != null) {
                    return false;
                }
            } else if (!featureInfo2.equals(featureInfo3)) {
                return false;
            }

            String string = this.getName();
            String string2 = feature.getName();
            if (string == null) {
                if (string2 != null) {
                    return false;
                }
            } else if (!string.equals(string2)) {
                return false;
            }

            Category category2 = this.getCategory();
            Category category3 = feature.getCategory();
            if (category2 == null) {
                if (category3 != null) {
                    return false;
                }
            } else if (!category2.equals(category3)) {
                return false;
            }

            Subcategory subcategory2 = this.subcategory();
            Subcategory subcategory3 = feature.subcategory();
            if (subcategory2 == null) {
                if (subcategory3 != null) {
                    return false;
                }
            } else if (!subcategory2.equals(subcategory3)) {
                return false;
            }

            String string3 = this.description();
            String string4 = feature.description();
            if (string3 == null) {
                if (string4 != null) {
                    return false;
                }
            } else if (!string3.equals(string4)) {
                return false;
            }

            Beta beta2 = this.getBeta();
            Beta beta3 = feature.getBeta();
            if (beta2 == null) {
                if (beta3 != null) {
                    return false;
                }
            } else if (!beta2.equals(beta3)) {
                return false;
            }

            Danger danger2 = this.danger();
            Danger danger3 = feature.danger();
            if (danger2 == null) {
                if (danger3 != null) {
                    return false;
                }
            } else if (!danger2.equals(danger3)) {
                return false;
            }

            List list = this.getBinds();
            List list2 = feature.getBinds();
            if (list == null) {
                if (list2 != null) {
                    return false;
                }
            } else if (!list.equals(list2)) {
                return false;
            }

            List list3 = this.rawSettings();
            List list4 = feature.rawSettings();
            if (list3 == null) {
                if (list4 != null) {
                    return false;
                }
            } else if (!list3.equals(list4)) {
                return false;
            }

            Map map = this.getActiveBinds();
            Map map2 = feature.getActiveBinds();
            if (map == null) {
                if (map2 != null) {
                    return false;
                }
            } else if (!map.equals(map2)) {
                return false;
            }

            return true;
        }
    }

    protected boolean canEqual(Object object) {
        return object instanceof Feature;
    }

    @Override
    public int hashCode() {
        int i = ((59 + (this.isAutoEnabled() ? 79 : 97)) * 59 + (this.isAllowDisable() ? 79 : 97)) * 59 + (this.hidden() ? 79 : 97);
        long l = this.getBindOrderCounter();
        int i2 = ((i * 59 + (int)(l >>> 32 ^ l)) * 59 + (this.isToggledEnabled() ? 79 : 97)) * 59 + (this.enabled() ? 79 : 97);
        FeatureInfo featureInfo2 = this.getFeatureInfo();
        int i3 = i2 * 59 + (featureInfo2 == null ? 43 : featureInfo2.hashCode());
        String string = this.getName();
        int i4 = i3 * 59 + (string == null ? 43 : string.hashCode());
        Category category2 = this.getCategory();
        int i5 = i4 * 59 + (category2 == null ? 43 : category2.hashCode());
        Subcategory subcategory2 = this.subcategory();
        int i6 = i5 * 59 + (subcategory2 == null ? 43 : subcategory2.hashCode());
        String string2 = this.description();
        int i7 = i6 * 59 + (string2 == null ? 43 : string2.hashCode());
        Beta beta2 = this.getBeta();
        int i8 = i7 * 59 + (beta2 == null ? 43 : beta2.hashCode());
        Danger danger2 = this.danger();
        int i9 = i8 * 59 + (danger2 == null ? 43 : danger2.hashCode());
        List list = this.getBinds();
        int i10 = i9 * 59 + (list == null ? 43 : list.hashCode());
        List list2 = this.rawSettings();
        int i11 = i10 * 59 + (list2 == null ? 43 : list2.hashCode());
        Map map = this.getActiveBinds();
        return map == null ? i11 * 59 + 43 : i11 * 59 + map.hashCode();
    }

    @Override
    public String toString() {
        return "Feature(featureInfo="
            + this.getFeatureInfo()
            + ", name="
            + this.getName()
            + ", category="
            + this.getCategory()
            + ", subcategory="
            + this.subcategory()
            + ", description="
            + this.description()
            + ", autoEnabled="
            + this.isAutoEnabled()
            + ", allowDisable="
            + this.isAllowDisable()
            + ", hidden="
            + this.hidden()
            + ", beta="
            + this.getBeta()
            + ", danger="
            + this.danger()
            + ", binds="
            + this.getBinds()
            + ", settings="
            + this.rawSettings()
            + ", activeBinds="
            + this.getActiveBinds()
            + ", bindOrderCounter="
            + this.getBindOrderCounter()
            + ", toggledEnabled="
            + this.isToggledEnabled()
            + ", enabled="
            + this.enabled()
            + ", triggeringBind="
            + this.getTriggeringBind()
            + ")";
    }
}
