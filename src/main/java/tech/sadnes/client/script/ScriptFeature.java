package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.libs.rhino.Function;

public final class ScriptFeature implements Module {
    private final ScriptModule module;
    private final String name;
    private final CategoryDescriptor category;
    private final Subcategory subcategory;
    private final String subcategoryName;
    private final BooleanSetting enabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    private String descriptionKey = "";
    private Function onEnable;
    private Function onDisable;
    private ScriptPanel panel;

    ScriptFeature(ScriptModule scriptModule, String string, CategoryDescriptor categoryDescriptor, Subcategory subcategory2, String string2) {
        this.module = scriptModule;
        this.name = string;
        this.category = categoryDescriptor;
        this.subcategory = subcategory2;
        this.subcategoryName = string2;
        this.enabled = new BooleanSetting("scripts.module.enable", false);
        this.enabled.setDisplayName("$on");
        this.enabled.setOnChange(this::onEnabledChanged);
        this.settings.add(this.enabled);
    }

    @Override
    public boolean isEnabled() {
        return this.module.isEnabled() && Boolean.TRUE.equals(this.enabled.getValue());
    }

    public void setEnabled(boolean flag) {
        this.enabled.setValue(flag);
    }

    @Override
    public void toggle() {
        if (!Boolean.TRUE.equals(this.enabled.getValue())) {
            this.setEnabled(true);
        } else {
            this.setEnabled(false);
        }
    }

    public void setDescriptionKey(String string) {
        if (string == null) {
            this.descriptionKey = "";
        } else {
            this.descriptionKey = string;
        }
    }

    @Override
    public String getLocalizedDescription() {
        return Language.translate(this.descriptionKey);
    }

    public void setOnEnable(Function function) {
        this.onEnable = function;
    }

    public void setOnDisable(Function function) {
        this.onDisable = function;
    }

    public void addSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    public void setPanel(ScriptPanel scriptPanel) {
        this.panel = scriptPanel;
    }

    @Override
    public boolean hasCustomPanel() {
        return this.panel != null;
    }

    private void onEnabledChanged() {
        Function function = Boolean.TRUE.equals(this.enabled.getValue()) ? this.onEnable : this.onDisable;
        if (function != null && this.module.isEnabled()) {
            this.module.getScript().call(function);
        }

        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        if (configManager != null) {
            configManager.markDirty();
        }
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return setting != this.enabled && !setting.isHidden();
    }

    public ScriptModule getModule() {
        return this.module;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public CategoryDescriptor getCategory() {
        return this.category;
    }

    @Override
    public Subcategory getSubcategory() {
        return this.subcategory;
    }

    @Override
    public String getSubcategoryName() {
        return this.subcategoryName;
    }

    public BooleanSetting getEnabledSetting() {
        return this.enabled;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    @Override
    public String getDescription() {
        return this.descriptionKey;
    }

    public Function getOnEnable() {
        return this.onEnable;
    }

    public Function getOnDisable() {
        return this.onDisable;
    }

    public ScriptPanel getPanel() {
        return this.panel;
    }
}
