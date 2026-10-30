package tech.sadnes.client.ui.clickgui.search;

import tech.sadnes.client.feature.Module;
import tech.sadnes.client.setting.Setting;

public record SearchResult(String chain, String module, String values, Module feature, Setting<?> setting, boolean openSetting) {
    public boolean isModule() {
        return this.setting == null;
    }
}
