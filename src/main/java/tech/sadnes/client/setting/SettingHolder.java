package tech.sadnes.client.setting;

import java.util.List;

public interface SettingHolder {
    String getDisplayName();

    List<Setting<?>> getSettings();

    default boolean isSettingVisible(Setting<?> setting) {
        return !setting.isHidden();
    }
}
