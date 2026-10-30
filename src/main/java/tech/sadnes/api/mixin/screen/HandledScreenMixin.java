package tech.sadnes.api.mixin.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.api.mixin.accessor.KeyBindingAccessor;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.SlotTooltipEvent;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.impl.misc.ItemScroller;
import tech.sadnes.client.feature.impl.movement.Spider;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.server.auction.SafeBuyLimitWidget;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Unique
    private static final AuctionPriceParser sadnes$auctionPriceParser = new AuctionPriceParser();
    @Unique
    private static final Pattern sadnes$auctionPriceLine = Pattern.compile("Цен[аa](?!\\s+за).*?([0-9][0-9, ]*)", 2);
    @Unique
    private static final Pattern sadnes$auctionPriceLabel = Pattern.compile("Цен[аa]", 2);
    @Unique
    private static final Pattern sadnes$auctionPriceValue = Pattern.compile("[0-9][0-9, ]*");
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected Slot focusedSlot;
    @Shadow
    @Final
    protected ScreenHandler handler;

    @Inject(
        method = "renderMain",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V")
    )
    private void sadnes$handledScreenEvent(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        new HandledScreenRenderEvent(drawContext, this.focusedSlot, this.backgroundWidth, this.backgroundHeight).post();
    }

    @Inject(
        method = "renderMain",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V")
    )
    private void sadnes$spiderItemSelectionRender(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        Spider spider = Spider.getInstance();
        if (spider != null) {
            spider.renderSlotHighlights(drawContext);
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void sadnes$screenTooltipEvent(DrawContext drawContext, int i, int i2, CallbackInfo callbackInfo) {
        SlotTooltipEvent slotTooltipEvent = new SlotTooltipEvent(drawContext, this.focusedSlot, i, i2);
        slotTooltipEvent.post();
        if (slotTooltipEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private void sadnes$appendAuctionUnitPrice(ItemStack itemStack, CallbackInfoReturnable<List<Text>> callbackInfoReturnable) {
        if (AuctionHelper.getInstance().isEnabled()) {
            if (ServerManager.isSpookyTime() || ServerManager.isFunTime()) {
                if ((Object)this instanceof GenericContainerScreen && AuctionHelper.isAuctionScreen((GenericContainerScreen)(Object)this) && itemStack != null && !itemStack.isEmpty()) {
                    if (itemStack.getCount() <= 2) {
                        return;
                    }

                    int i = sadnes$auctionPriceParser.getPrice(itemStack);
                    if (i < 0) {
                        return;
                    }

                    List list = (List)callbackInfoReturnable.getReturnValue();
                    if (list != null) {
                        if (list.isEmpty()) {
                            return;
                        }

                        int i2 = list.size();

                        for (int i3 = 0; i3 < i2; i3++) {
                            String string = ((Text)list.get(i3)).getString();
                            if (string.contains("Цена за 1")) {
                                return;
                            }
                        }

                        for (int i4 = 0; i4 < i2; i4++) {
                            Text text = (Text)list.get(i4);
                            if (sadnes$auctionPriceLine.matcher(text.getString()).find()) {
                                ArrayList arrayList = new ArrayList(list);
                                arrayList.add(i4 + 1, this.sadnes$unitPriceLine(text, i / itemStack.getCount()));
                                callbackInfoReturnable.setReturnValue(arrayList);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Unique
    private Text sadnes$unitPriceLine(Text text2, int i) {
        String string = AuctionLoreUtil.formatNumber(i);
        String string2 = Matcher.quoteReplacement(Language.translate("auctionhelper.unitPrice", "Цена за 1 шт"));
        MutableText mutableText = Text.empty().setStyle(text2.getStyle());
        boolean[] booleans = new boolean[]{false};
        boolean[] booleans2 = new boolean[]{false};
        text2.visit((style, string3) -> {
            String string4 = string3;
            Matcher matcher2 = sadnes$auctionPriceLabel.matcher(string3);
            boolean flag;
            if (!booleans[0]) {
                flag = matcher2.find();
            } else {
                flag = false;
            }

            if (!booleans2[0] && (booleans[0] || flag)) {
                Matcher matcher3 = sadnes$auctionPriceValue.matcher(string3);
                if (matcher3.find(flag ? matcher2.end() : 0)) {
                    string4 = string3.substring(0, matcher3.start()) + string + string3.substring(matcher3.end());
                    booleans2[0] = true;
                }
            }

            if (flag) {
                string4 = sadnes$auctionPriceLabel.matcher(string4).replaceFirst(string2);
                booleans[0] = true;
            }

            mutableText.append(Text.literal(string4).setStyle(style));
            return Optional.empty();
        }, text2.getStyle());
        return mutableText;
    }

    @Inject(method = "renderMain", at = @At("TAIL"))
    private void sadnes$renderSafeBuy(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        SafeBuyLimitWidget safeBuyLimitWidget = this.sadnes$safeBuy();
        if (safeBuyLimitWidget != null) {
            safeBuyLimitWidget.render(drawContext, this.handler.slots, this.x, this.y, i, i2);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sadnes$safeBuyClicked(Click click, boolean flag, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        SafeBuyLimitWidget safeBuyLimitWidget = this.sadnes$safeBuy();
        if (safeBuyLimitWidget != null && safeBuyLimitWidget.mouseClicked(click.x(), click.y(), click.button(), this.x, this.y)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void sadnes$safeBuyDragged(Click click, double d, double d2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        SafeBuyLimitWidget safeBuyLimitWidget = this.sadnes$safeBuy();
        if (safeBuyLimitWidget != null && safeBuyLimitWidget.mouseDragged(click.x(), this.x, this.y)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void sadnes$safeBuyReleased(Click click, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        SafeBuyLimitWidget safeBuyLimitWidget = this.sadnes$safeBuy();
        if (safeBuyLimitWidget != null && safeBuyLimitWidget.mouseReleased()) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void sadnes$safeBuyScrolled(double d, double d2, double d3, double d4, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        SafeBuyLimitWidget safeBuyLimitWidget = this.sadnes$safeBuy();
        if (safeBuyLimitWidget != null && safeBuyLimitWidget.mouseScrolled(d, d2, d4, this.x, this.y)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }

    @Unique
    private SafeBuyLimitWidget sadnes$safeBuy() {
        return AuctionHelper.getInstance().getSafeBuyOverlay((HandledScreen<?>)(Object)this);
    }

    @Inject(method = "renderMain", at = @At("TAIL"))
    private void sadnes$itemScrollerHandledScreen(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ItemScroller itemScroller = ItemScroller.getInstance();
        Slot slot = this.focusedSlot;
        if (itemScroller != null && itemScroller.isEnabled() && minecraftClient.player != null && minecraftClient.interactionManager != null && slot != null && slot.hasStack()) {
            long l = minecraftClient.getWindow().getHandle();
            boolean flag = InputUtil.isKeyPressed(minecraftClient.getWindow(), minecraftClient.options.sneakKey.getDefaultKey().getCode());
            boolean flag2 = InputUtil.isKeyPressed(minecraftClient.getWindow(), minecraftClient.options.sprintKey.getDefaultKey().getCode());
            boolean flag3 = InputUtil.isKeyPressed(minecraftClient.getWindow(), ((KeyBindingAccessor)minecraftClient.options.dropKey).sadnes$getBoundKey().getCode());
            boolean flag4 = GLFW.glfwGetMouseButton(l, 0) == 1;
            SlotActionType slotActionType = flag3 ? SlotActionType.THROW : (flag4 ? SlotActionType.QUICK_MOVE : null);
            ScreenHandler screenHandler = minecraftClient.player.currentScreenHandler;
            boolean flag5;
            if (slot.id < screenHandler.slots.size()) {
                flag5 = screenHandler.getSlot(slot.id).hasStack();
            } else {
                flag5 = false;
            }

            boolean flag6;
            if (slotActionType == SlotActionType.QUICK_MOVE) {
                flag6 = (Object)this instanceof InventoryScreen;
            } else {
                flag6 = false;
            }

            boolean flag7;
            if (flag6) {
                flag7 = itemScroller.canQuickMove(screenHandler, screenHandler.getSlot(slot.id));
            } else {
                flag7 = true;
            }

            if (flag5
                && flag7
                && !GuiMoveComponent.isActive()
                && flag
                && !flag2
                && slotActionType != null
                && itemScroller.stopWatch.hasPassedAndReset(itemScroller.delay.getValue().floatValue())) {
                if (slotActionType == SlotActionType.THROW) {
                    minecraftClient.interactionManager.clickSlot(minecraftClient.player.currentScreenHandler.syncId, slot.id, 1, slotActionType, minecraftClient.player);
                    return;
                }

                minecraftClient.interactionManager.clickSlot(minecraftClient.player.currentScreenHandler.syncId, slot.id, 0, slotActionType, minecraftClient.player);
            }
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void sadnes$itemScrollerClickSlot(Slot slot, int i, int i2, SlotActionType slotActionType, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ItemScroller itemScroller = ItemScroller.getInstance();
        if (itemScroller != null
            && itemScroller.isEnabled()
            && minecraftClient.player != null
            && minecraftClient.interactionManager != null
            && i >= 0
            && i < minecraftClient.player.currentScreenHandler.slots.size()) {
            Slot slot2 = minecraftClient.player.currentScreenHandler.getSlot(i);
            if (!slot2.hasStack()) {
                return;
            }

            Item item = slot2.getStack().getItem();
            boolean flag = InputUtil.isKeyPressed(minecraftClient.getWindow(), minecraftClient.options.sneakKey.getDefaultKey().getCode());
            boolean flag2 = InputUtil.isKeyPressed(minecraftClient.getWindow(), minecraftClient.options.sprintKey.getDefaultKey().getCode());
            boolean flag3;
            if (slotActionType == SlotActionType.QUICK_MOVE) {
                flag3 = (Object)this instanceof InventoryScreen;
            } else {
                flag3 = false;
            }

            if (item != null && flag && flag2 && itemScroller.stopWatch.hasPassedAndReset(50.0)) {
                for (Slot slot3 : minecraftClient.player.currentScreenHandler.slots) {
                    if (slot3.hasStack()
                        && slot3.getStack().getItem().equals(item)
                        && slot3.inventory.equals(slot2.inventory)
                        && (!flag3 || itemScroller.canQuickMove(minecraftClient.player.currentScreenHandler, slot3))) {
                        minecraftClient.interactionManager.clickSlot(minecraftClient.player.currentScreenHandler.syncId, slot3.id, 1, slotActionType, minecraftClient.player);
                    }
                }
            }
        }
    }
}
