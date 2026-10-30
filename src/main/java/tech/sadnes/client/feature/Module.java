package tech.sadnes.client.feature;

import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.setting.SettingHolder;

public interface Module extends SettingHolder {
    boolean isEnabled();

    void toggle();

    CategoryDescriptor getCategory();

    Subcategory getSubcategory();

    default String getSubcategoryName() {
        Subcategory subcategory = this.getSubcategory();
        return subcategory != null && subcategory != Subcategory.NONE ? subcategory.getDisplayName() : "";
    }

    String getDescription();

    default String getLocalizedDescription() {
        return this.getDescription();
    }

    default boolean isHidden() {
        return false;
    }

    default boolean hasCustomPanel() {
        return false;
    }

    default boolean isAccessible() {
        return true;
    }

    default boolean isBeta() {
        return false;
    }

    default Danger getDanger() {
        return null;
    }

    default MarkerLevel getDangerLevel() {
        return null;
    }

    default boolean isDangerActive() {
        return false;
    }
}
