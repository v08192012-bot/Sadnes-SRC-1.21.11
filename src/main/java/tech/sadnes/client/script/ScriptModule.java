package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingHolder;
import tech.sadnes.libs.rhino.Function;

public final class ScriptModule implements SettingHolder {
    private final Script script;
    private final String name;
    private final BooleanSetting enabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<ScriptFeature> hudElements = new ArrayList<>();
    private String description = "";
    private Function onEnable;
    private Function onDisable;

    ScriptModule(Script script2, String string, boolean flag) {
        this.script = script2;
        this.name = string;
        this.enabled = new BooleanSetting("scripts.module.enable", flag);
        this.enabled.setDisplayName("$on");
        this.enabled.setOnChange(this::onEnabledChanged);
        this.settings.add(this.enabled);
    }

    public boolean isEnabled() {
        return this.script.isActive() && Boolean.TRUE.equals(this.enabled.getValue());
    }

    public void setEnabled(boolean flag) {
        this.enabled.setValue(flag);
    }

    public void toggle() {
        if (!Boolean.TRUE.equals(this.enabled.getValue())) {
            this.setEnabled(true);
        } else {
            this.setEnabled(false);
        }
    }

    public void setDescription(String string) {
        if (string == null) {
            this.description = "";
        } else {
            this.description = string;
        }
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

    public ScriptFeature getOrCreateHudElement(String string, String string2, String string3) {
        for (ScriptFeature scriptFeature : this.hudElements) {
            if (scriptFeature.getDisplayName().equals(string)) {
                return scriptFeature;
            }
        }

        CategoryDescriptor categoryDescriptor = ScriptCategory.resolve(string2);
        Subcategory subcategory = ScriptCategory.resolveSubcategory(categoryDescriptor, string3);
        boolean flag;
        if (string3 != null) {
            flag = string3.isBlank();
        } else {
            flag = true;
        }

        String string4 = subcategory != null ? subcategory.getDisplayName() : (flag ? this.name : string3.trim());
        ScriptFeature scriptFeature2 = new ScriptFeature(this, string, categoryDescriptor, subcategory, string4);
        this.hudElements.add(scriptFeature2);
        return scriptFeature2;
    }

    private void onEnabledChanged() {
        Function function = Boolean.TRUE.equals(this.enabled.getValue()) ? this.onEnable : this.onDisable;
        if (function != null && this.script.isActive()) {
            this.script.call(function);
        }

        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        if (configManager != null) {
            configManager.markDirty();
        }

        if (!this.hudElements.isEmpty()) {
            ScriptManager.refreshClickGui();
        }
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return setting != this.enabled && !setting.isHidden();
    }

    public Script getScript() {
        return this.script;
    }

    public BooleanSetting getEnabledSetting() {
        return this.enabled;
    }

    public List<ScriptFeature> getHudElements() {
        return this.hudElements;
    }

    public String getDescription() {
        return this.description;
    }

    public Function getOnEnable() {
        return this.onEnable;
    }

    public Function getOnDisable() {
        return this.onDisable;
    }
}
