package tech.sadnes.client.ui.clickgui.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.feature.Subcategory;

public final class SearchIndex {
    private static final Comparator<FeatureManager.SearchEntry> ENTRY_ORDER = Comparator.<FeatureManager.SearchEntry>comparingInt(
            searchEntry -> getCategoryOrder(searchEntry.feature().getCategory())
        )
        .thenComparing(searchEntry2 -> searchEntry2.feature().getCategory().getName(), String.CASE_INSENSITIVE_ORDER)
        .thenComparingInt(searchEntry3 -> getSubCategoryOrder(searchEntry3.feature().getSubcategory()))
        .thenComparing(searchEntry4 -> searchEntry4.feature().getSubcategoryName(), String.CASE_INSENSITIVE_ORDER)
        .thenComparing(searchEntry5 -> searchEntry5.feature().getDisplayName(), String.CASE_INSENSITIVE_ORDER);

    public List<SearchResult> search(String string) {
        String string2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
        String[] strings = string2.isEmpty() ? new String[0] : string2.split("\\s+");
        boolean flag = strings.length == 0;
        ArrayList<FeatureManager.SearchEntry> searchEntries = new ArrayList<>(Sadnes.getInstance().getFeatureManager().getSearchEntries());
        searchEntries.sort(ENTRY_ORDER);
        ArrayList arrayList = new ArrayList();

        for (FeatureManager.SearchEntry searchEntry : searchEntries) {
            Module module = searchEntry.feature();
            if (!module.isHidden() && module.isAccessible()) {
                String string3 = module.getDisplayName();
                if (flag || this.containsAllTokens(string3.toLowerCase(Locale.ROOT), strings)) {
                    arrayList.add(new SearchResult(string3, string3, null, module, null, false));
                }

                if (!flag) {
                    for (FeatureManager.SettingEntry settingEntry : searchEntry.settings()) {
                        if (settingEntry.setting().hasAccess() && this.containsAllTokens(settingEntry.searchLine(), strings)) {
                            arrayList.add(this.createSettingResult(module, string3, settingEntry, this.findMatchingValues(settingEntry, strings)));
                        }
                    }
                }
            }
        }

        return arrayList;
    }

    private SearchResult createSettingResult(Module module, String string, FeatureManager.SettingEntry settingEntry, String string2) {
        return new SearchResult(settingEntry.displayName(), string, string2, module, settingEntry.setting(), string2 != null);
    }

    private String findMatchingValues(FeatureManager.SettingEntry settingEntry, String[] strings) {
        ArrayList arrayList = new ArrayList();

        for (String string : settingEntry.values()) {
            if (this.containsAnyToken(string.toLowerCase(Locale.ROOT), strings)) {
                arrayList.add(string);
            }
        }

        return arrayList.isEmpty() ? null : String.join(", ", arrayList);
    }

    private static int getCategoryOrder(CategoryDescriptor categoryDescriptor) {
        return categoryDescriptor instanceof Category ? ((Category)categoryDescriptor).ordinal() : Integer.MAX_VALUE;
    }

    private static int getSubCategoryOrder(Subcategory subcategory) {
        return subcategory != null && subcategory != Subcategory.NONE ? subcategory.ordinal() : Integer.MAX_VALUE;
    }

    private boolean containsAllTokens(String string, String[] strings) {
        for (String string2 : strings) {
            if (!string2.isEmpty() && !string.contains(string2)) {
                return false;
            }
        }

        return true;
    }

    private boolean containsAnyToken(String string, String[] strings) {
        for (String string2 : strings) {
            if (!string2.isEmpty() && string.contains(string2)) {
                return true;
            }
        }

        return false;
    }
}
