package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.chat.ChatUtil;

public final class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle");
    }

    @Override
    public List<String> getAliases() {
        return List.of("t");
    }

    @Override
    public void execute(String[] strings) {
        if (strings.length < 1) {
            ChatUtil.print(Language.translate("command.toggle.usage"));
        } else {
            Feature feature = this.findFeature(strings[0]);
            if (feature == null) {
                ChatUtil.print(Language.translate("command.toggle.notfound"), strings[0]);
            } else {
                feature.toggle();
                ChatUtil.print(Language.translate(feature.isEnabled() ? "command.toggle.enabled" : "command.toggle.disabled"), feature.getDisplayName());
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        return strings.length != 1 ? List.of() : CommandManager.filterByPrefix(this.getToggleableNames(), strings[0]);
    }

    private Feature findFeature(String string) {
        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            if (feature.getDisplayName().equalsIgnoreCase(string)) {
                return feature;
            }
        }

        return null;
    }

    private List<String> getToggleableNames() {
        ArrayList arrayList = new ArrayList();

        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            if (!feature.isHidden()) {
                arrayList.add(feature.getDisplayName());
            }
        }

        return arrayList;
    }
}
