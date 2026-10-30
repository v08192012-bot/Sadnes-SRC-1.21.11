package tech.sadnes.client.server.donut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogListDialog;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DialogTags;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Custom;
import net.minecraft.util.Formatting;
import tech.sadnes.client.util.IMinecraft;

public final class QuickActionsUtil implements IMinecraft {
    private QuickActionsUtil() {
    }

    public static boolean trigger(String string, String... strings) {
        Custom custom = findCustomAction(collectButtons(), string, strings);
        if (custom == null) {
            return false;
        }

        mc.player.networkHandler.sendPacket(new CustomClickActionC2SPacket(custom.id(), custom.payload()));
        return true;
    }

    public static List<String> describeActions() {
        List<String> strings = new ArrayList<>();

        for (DialogActionButtonData dialogActionButtonData : collectButtons()) {
            ClickEvent clickEvent = getClickEvent(dialogActionButtonData);
            String string = switch (clickEvent) {
                case null -> "nothing";
                case Custom custom -> "custom " + custom.id() + custom.payload().map(nbtElement -> " " + nbtElement).orElse("");
                case ClickEvent.RunCommand runCommand -> "command " + runCommand.command();
                case ClickEvent.ShowDialog showDialog -> "submenu \"" + showDialog.dialog().value().common().title().getString() + "\"";
                default -> clickEvent.getAction().asString();
            };
            strings.add("\"" + getButtonLabel(dialogActionButtonData) + "\" -> " + string);
        }

        return strings;
    }

    private static Custom findCustomAction(List<DialogActionButtonData> dialogActionButtonDatas, String string, String[] strings) {
        for (DialogActionButtonData dialogActionButtonData : dialogActionButtonDatas) {
            String string2 = getButtonLabel(dialogActionButtonData);
            boolean flag;
            if (!string2.equalsIgnoreCase(string)) {
                flag = Arrays.stream(strings).anyMatch(string2::equalsIgnoreCase);
            } else {
                flag = true;
            }

            if (flag) {
                ClickEvent clickEvent = getClickEvent(dialogActionButtonData);
                if (clickEvent instanceof Custom) {
                    return (Custom)clickEvent;
                }
            }
        }

        for (DialogActionButtonData dialogActionButtonData2 : dialogActionButtonDatas) {
            if (getClickEvent(dialogActionButtonData2) instanceof Custom custom && matchesActionId(custom, string)) {
                return custom;
            }
        }

        return null;
    }

    private static boolean matchesActionId(Custom custom, String string) {
        String string2 = custom.id().getPath().toLowerCase(Locale.ROOT);
        boolean flag;
        if (!string2.equals(string)) {
            flag = string2.endsWith("/" + string);
        } else {
            flag = true;
        }

        return custom.payload().orElse(null) instanceof NbtCompound nbtCompound
                && nbtCompound.getKeys().stream().anyMatch(string3 -> nbtCompound.getString(string3, "").equalsIgnoreCase(string))
            || flag;
    }

    private static ClickEvent getClickEvent(DialogActionButtonData dialogActionButtonData) {
        return dialogActionButtonData.action().flatMap(dialogAction -> dialogAction.createClickEvent(Map.of())).orElse(null);
    }

    private static String getButtonLabel(DialogActionButtonData dialogActionButtonData) {
        return Formatting.strip(dialogActionButtonData.data().label().getString()).strip();
    }

    private static List<DialogActionButtonData> collectButtons() {
        ArrayList arrayList = new ArrayList();
        if (mc.player == null) {
            return arrayList;
        }

        Registry registry = mc.player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.DIALOG);
        Iterator iterator2 = registry.iterateEntries(DialogTags.PAUSE_SCREEN_ADDITIONS).iterator();

        while (iterator2.hasNext()) {
            collectFromDialog((Dialog)((RegistryEntry)iterator2.next()).value(), arrayList, 0);
        }

        return arrayList;
    }

    private static void collectFromDialog(Dialog dialog, List<DialogActionButtonData> dialogActionButtonDatas, int i) {
        if (dialog instanceof MultiActionDialog multiActionDialog && multiActionDialog.common().inputs().isEmpty()) {
            dialogActionButtonDatas.addAll(multiActionDialog.actions());
        }

        if (dialog instanceof DialogListDialog dialogListDialog && i < 3) {
            Iterator iterator2 = dialogListDialog.dialogs().iterator();

            while (iterator2.hasNext()) {
                collectFromDialog((Dialog)((RegistryEntry)iterator2.next()).value(), dialogActionButtonDatas, i + 1);
            }
        }
    }
}
