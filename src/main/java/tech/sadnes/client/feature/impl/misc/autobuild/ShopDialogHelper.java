package tech.sadnes.client.feature.impl.misc.autobuild;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.action.DialogAction.ValueGetter;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.body.ItemDialogBody;
import net.minecraft.dialog.body.PlainMessageDialogBody;
import net.minecraft.dialog.input.BooleanInputControl;
import net.minecraft.dialog.input.InputControl;
import net.minecraft.dialog.input.NumberRangeInputControl;
import net.minecraft.dialog.input.SingleOptionInputControl;
import net.minecraft.dialog.input.TextInputControl;
import net.minecraft.dialog.type.ConfirmationDialog;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.dialog.type.DialogInput;
import net.minecraft.dialog.type.MultiActionDialog;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Custom;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.api.mixin.accessor.PlayerListHudAccessor;
import tech.sadnes.client.server.donut.DonutPriceUtil;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.player.InventoryUtil;

final class ShopDialogHelper implements IMinecraft {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopDialogHelper.class);
    private static final Set<String> CANCEL_LABELS = Set.of("no", "нет", "cancel", "отмена");
    private final Set<String> dumpedKeys = new HashSet<>();
    private Map<String, String> englishNames;

    long getBalance() {
        Text text = ((PlayerListHudAccessor)mc.inGameHud.getPlayerListHud()).sadnes$getFooter();
        return text == null ? -1L : DonutPriceUtil.parsePrice(Formatting.strip(text.getString()));
    }

    boolean canAfford(long l) {
        long l2 = this.getBalance();
        return l2 < 0L || l <= l2 + DonutPriceUtil.getPriceStep(l2);
    }

    boolean canInteract() {
        return !mc.player.isSneaking() && !InventoryUtil.isMovementActive();
    }

    String getEnglishName(Item item) {
        if (this.englishNames == null) {
            this.englishNames = this.loadEnglishNames();
        }

        String string = this.englishNames.get(item.getTranslationKey());
        return string != null ? string : Registries.ITEM.getId(item).getPath().replace('_', ' ');
    }

    private Map<String, String> loadEnglishNames() {
        HashMap hashMap = new HashMap();
        Optional optional = mc.getResourceManager().getResource(Identifier.ofVanilla("lang/en_us.json"));
        if (optional.isEmpty()) {
            return hashMap;
        }

        try (BufferedReader bufferedReader = ((Resource)optional.get()).getReader()) {
            JsonObject jsonObject = JsonParser.parseReader(bufferedReader).getAsJsonObject();

            for (Entry entry : jsonObject.entrySet()) {
                boolean flag = ((String)entry.getKey()).startsWith("block.minecraft.") || ((String)entry.getKey()).startsWith("item.minecraft.");
                if (flag) {
                    hashMap.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
                }
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("AutoBuild: english item names are unavailable", exception);
        }

        return hashMap;
    }

    List<DialogActionButtonData> getButtons(Dialog dialog) {
        if (dialog instanceof MultiActionDialog) {
            return ((MultiActionDialog)dialog).actions();
        } else {
            return dialog instanceof ConfirmationDialog confirmationDialog ? List.of(confirmationDialog.yesButton(), confirmationDialog.noButton()) : List.of();
        }
    }

    String getButtonLabel(DialogActionButtonData dialogActionButtonData) {
        String string = Formatting.strip(dialogActionButtonData.data().label().getString());
        return string.substring(string.lastIndexOf(93) + 1).strip();
    }

    DialogActionButtonData findConfirmButton(Dialog dialog2) {
        List<DialogActionButtonData> dialogActionButtonDatas = this.getButtons(dialog2);
        boolean flag;
        if (dialogActionButtonDatas.size() <= 3) {
            flag = dialogActionButtonDatas.stream().anyMatch(this::isCancelButton);
        } else {
            flag = false;
        }

        return flag ? dialogActionButtonDatas.stream().filter(dialogActionButtonData -> !this.isCancelButton(dialogActionButtonData)).findFirst().orElse(null) : null;
    }

    DialogActionButtonData findCancelButton(Dialog dialog) {
        List<DialogActionButtonData> dialogActionButtonDatas = this.getButtons(dialog);
        return dialogActionButtonDatas.size() <= 3 ? dialogActionButtonDatas.stream().filter(this::isCancelButton).findFirst().orElse(null) : null;
    }

    private boolean isCancelButton(DialogActionButtonData dialogActionButtonData) {
        ClickEvent clickEvent = dialogActionButtonData.action().flatMap(dialogAction -> dialogAction.createClickEvent(Map.of())).orElse(null);
        String string = clickEvent instanceof Custom && ((Custom)clickEvent).payload().orElse(null) instanceof NbtCompound nbtCompound
            ? nbtCompound.getString("args", "").toLowerCase(Locale.ROOT)
            : "";
        return CANCEL_LABELS.contains(this.getButtonLabel(dialogActionButtonData).toLowerCase(Locale.ROOT)) || string.equals("cancel") || string.endsWith(" no");
    }

    long getDialogPrice(Dialog dialog) {
        for (DialogBody dialogBody : dialog.common().body()) {
            Text text = dialogBody instanceof PlainMessageDialogBody
                ? ((PlainMessageDialogBody)dialogBody).contents()
                : (dialogBody instanceof ItemDialogBody ? ((ItemDialogBody)dialogBody).description().map(PlainMessageDialogBody::contents).orElse(null) : null);
            long l = text == null ? -1L : DonutPriceUtil.parsePrice(Formatting.strip(text.getString()));
            if (l >= 0L) {
                return l;
            }
        }

        return -1L;
    }

    int getDialogItemCount(Dialog dialog) {
        for (DialogBody dialogBody : dialog.common().body()) {
            if (dialogBody instanceof ItemDialogBody) {
                return ((ItemDialogBody)dialogBody).item().getCount();
            }
        }

        return 0;
    }

    Map<String, String> fillTextInputs(Dialog dialog, String string) {
        HashMap hashMap = new HashMap();

        for (DialogInput dialogInput : dialog.common().inputs()) {
            if (dialogInput.control() instanceof TextInputControl) {
                hashMap.put(dialogInput.key(), string);
            }
        }

        return hashMap;
    }

    boolean clickButton(Dialog dialog2, DialogActionButtonData dialogActionButtonData, Map<String, String> map) {
        Map<String, ValueGetter> map2 = new HashMap<>();

        for (DialogInput dialogInput : dialog2.common().inputs()) {
            String string = (String)map.get(dialogInput.key());
            map2.put(dialogInput.key(), string != null ? ValueGetter.of(string) : defaultInputValue(dialogInput.control()));
        }

        ClickEvent clickEvent = dialogActionButtonData.action().flatMap(dialogAction -> dialogAction.createClickEvent(map2)).orElse(null);
        if (clickEvent instanceof Custom custom) {
            mc.player.networkHandler.sendPacket(new CustomClickActionC2SPacket(custom.id(), custom.payload()));
        } else if (clickEvent instanceof RunCommand runCommand) {
            mc.player.networkHandler.sendChatCommand(runCommand.command().startsWith("/") ? runCommand.command().substring(1) : runCommand.command());
        }

        this.closeDialog();
        return clickEvent instanceof Custom || clickEvent instanceof RunCommand;
    }

    private static ValueGetter defaultInputValue(InputControl control) {
        switch (control) {
            case TextInputControl text:
                return ValueGetter.of(text.initial());
            case BooleanInputControl bool:
                if (bool.initial()) {
                    return inputValue(bool.onTrue(), NbtByte.of(bool.initial()));
                }
                return inputValue(bool.onFalse(), NbtByte.of(bool.initial()));
            case NumberRangeInputControl range:
                float value = range.rangeInfo().sliderProgressToValue(range.rangeInfo().getInitialSliderProgress());
                if ((float)(int)value == value) {
                    return inputValue(Integer.toString((int)value), NbtFloat.of(value));
                }
                return inputValue(Float.toString(value), NbtFloat.of(value));
            case SingleOptionInputControl option:
                return ValueGetter.of(option.getInitialEntry().orElse(option.entries().getFirst()).id());
            default:
                return ValueGetter.of("");
        }
    }

    private static ValueGetter inputValue(final String string, final NbtElement nbtElement) {
        return new ValueGetter() {
            @Override
            public String get() {
                return string;
            }

            @Override
            public NbtElement getAsNbt() {
                return nbtElement;
            }
        };
    }

    void closeDialog() {
        if (mc.currentScreen instanceof DialogScreen dialogScreen) {
            mc.setScreen(dialogScreen.getParentScreen());
        }
    }

    void dumpScreen(String string, ScreenHandler screenHandler) {
        if (this.dumpedKeys.add(string)) {
            StringBuilder builder = new StringBuilder();
            int i = screenHandler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler)screenHandler).getRows() * 9 : 0;

            for (int i2 = 0; i2 < i; i2++) {
                ItemStack itemStack = screenHandler.getSlot(i2).getStack();
                if (!itemStack.isEmpty()) {
                    builder.append("\n  ")
                        .append(i2)
                        .append(": ")
                        .append(itemStack.getCount())
                        .append("x ")
                        .append(Registries.ITEM.getId(itemStack.getItem()))
                        .append(" \"")
                        .append(itemStack.getName().getString())
                        .append('"');
                    LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
                    if (loreComponent != null) {
                        for (Text text : loreComponent.lines()) {
                            builder.append(" | ").append(text.getString());
                        }
                    }
                }
            }

            String string2 = mc.currentScreen == null ? "" : mc.currentScreen.getTitle().getString();
            LOGGER.info("AutoBuild: {}, window \"{}\":{}", string, string2, builder);
        }
    }

    void dumpDialog(String string, Dialog dialog2) {
        if (this.dumpedKeys.add(string)) {
            List list = this.getButtons(dialog2);
            StringBuilder builder = new StringBuilder();
            int i = Math.min(list.size(), 6);

            for (int i2 = 0; i2 < i; i2++) {
                DialogActionButtonData dialogActionButtonData = (DialogActionButtonData)list.get(i2);
                builder.append("\n  \"")
                    .append(this.getButtonLabel(dialogActionButtonData))
                    .append("\" -> ")
                    .append(dialogActionButtonData.action().flatMap(dialogAction -> dialogAction.createClickEvent(Map.of())).orElse(null));
            }

            LOGGER.info(
                "AutoBuild: {}, dialog \"{}\", price {}, {} buttons:{}", string, dialog2.common().title().getString(), this.getDialogPrice(dialog2), list.size(), builder
            );
        }
    }
}
