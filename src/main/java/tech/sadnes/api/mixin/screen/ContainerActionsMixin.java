package tech.sadnes.api.mixin.screen;

import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.server.FunTimeOnlineApi;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.autobuy.AutoBuyController;
import tech.sadnes.client.server.autobuy.AutoBuyPriceScanner;
import tech.sadnes.client.server.autobuy.KitCollector;

@Mixin(HandledScreen.class)
public abstract class ContainerActionsMixin extends Screen {
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int titleY;
    @Shadow
    @Final
    protected ScreenHandler handler;
    @Unique
    private final MinecraftClient mc = MinecraftClient.getInstance();
    @Unique
    private ButtonWidget sadnes$autoBuyButton;
    @Unique
    private ButtonWidget sadnes$collectorButton;
    @Unique
    private ButtonWidget sadnes$scanButton;
    @Unique
    private ButtonWidget sadnes$takeAllButton;
    @Unique
    private ButtonWidget sadnes$putAllButton;
    @Unique
    private ButtonWidget sadnes$dropAllButton;
    @Unique
    private ButtonWidget sadnes$inventoryDropAllButton;

    protected ContainerActionsMixin(Text text) {
        super(text);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sadnes$addContainerActions(CallbackInfo callbackInfo) {
        if ((Object)this instanceof GenericContainerScreen
            && ServerManager.isFunTime()
            && this.getTitle().getString().toLowerCase(Locale.ROOT).contains("выберите режим")) {
            this.addDrawableChild(
                ButtonWidget.builder(Text.literal("F"), buttonWidget -> FunTimeOnlineApi.joinLeastPopulatedAnarchy())
                    .tooltip(Tooltip.of(Text.literal("Зайти на анархию с меньшим онлайном")))
                    .dimensions(this.x + this.backgroundWidth - 10 - 4, this.y + 4, 10, 10)
                    .build()
            );
        }

        AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
        AutoBuyController autoBuyController = AutoBuyController.getInstance();
        boolean flag;
        if (autoBuyController != null) {
            flag = autoBuyController.isAuctionOpen();
        } else {
            flag = false;
        }

        if (autoBuyPriceScanner != null && autoBuyPriceScanner.isRunning() && !flag) {
            this.sadnes$scanButton = this.addDrawableChild(
                ButtonWidget.builder(this.sadnes$scanLabel(autoBuyPriceScanner), buttonWidget2 -> autoBuyPriceScanner.toggle())
                    .dimensions(this.x + this.backgroundWidth - 80, this.y - 22, 80, 20)
                    .build()
            );
        }

        if ((Object)this instanceof InventoryScreen) {
            int i = this.y - 20 - 2;
            this.sadnes$inventoryDropAllButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Выбросить всё"), buttonWidget3 -> this.dropAllInventory())
                    .dimensions(this.x, i, this.backgroundWidth, 20)
                    .build()
            );
            this.sadnes$collectorButton = this.addDrawableChild(
                ButtonWidget.builder(KitCollector.getRunButtonText(), buttonWidget4 -> this.sadnes$toggleCollector())
                    .dimensions(this.x, i - 20 - 2, this.backgroundWidth, 20)
                    .build()
            );
            this.sadnes$updateButtonsState();
        } else if ((Object)this instanceof GenericContainerScreen || (Object)this instanceof ShulkerBoxScreen) {
            int i2 = this.x + this.backgroundWidth + 4;
            int i3 = this.y + 10;
            if (flag) {
                this.sadnes$collectorButton = this.addDrawableChild(
                    ButtonWidget.builder(KitCollector.getRunButtonText(), buttonWidget5 -> this.sadnes$toggleCollector()).dimensions(i2, i3, 80, 20).build()
                );
                int i4 = i3 + 22;
                this.sadnes$autoBuyButton = this.addDrawableChild(
                    ButtonWidget.builder(this.sadnes$autoBuyLabel(autoBuyController), buttonWidget6 -> autoBuyController.toggle()).dimensions(i2, i4, 80, 20).build()
                );
                if (autoBuyPriceScanner != null) {
                    this.sadnes$scanButton = this.addDrawableChild(
                        ButtonWidget.builder(this.sadnes$scanLabel(autoBuyPriceScanner), buttonWidget7 -> autoBuyPriceScanner.toggle())
                            .tooltip(Tooltip.of(Text.literal("Собрать рыночные цены включённых предметов")))
                            .dimensions(i2, i4 + 20 + 2, 80, 20)
                            .build()
                    );
                }
            } else {
                this.sadnes$takeAllButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Взять всё"), buttonWidget8 -> this.takeAll()).dimensions(i2, i3, 80, 20).build()
                );
                this.sadnes$putAllButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Положить всё"), buttonWidget9 -> this.putAll()).dimensions(i2, i3 + 20 + 2, 80, 20).build()
                );
                this.sadnes$dropAllButton = this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("Выбросить всё"), buttonWidget10 -> this.dropAll()).dimensions(i2, i3 + 44, 80, 20).build()
                );
                this.sadnes$updateButtonsState();
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$updateActionsState(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        this.sadnes$updateButtonsState();
    }

    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void sadnes$updateActionsTick(CallbackInfo callbackInfo) {
        this.sadnes$updateButtonsState();
    }

    private void takeAll() {
        if (this.mc.player != null && this.mc.interactionManager != null) {
            int i = this.handler.slots.size();
            int i2 = i - 36;
            if (i2 <= 0) {
                return;
            }

            for (int i3 = 0; i3 < i2; i3++) {
                Slot slot = this.handler.slots.get(i3);
                if (slot.hasStack()) {
                    this.mc.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                }
            }
        }
    }

    private void putAll() {
        if (this.mc.player != null && this.mc.interactionManager != null) {
            int i = this.handler.slots.size();
            int i2 = i - 36;
            if (i2 <= 0) {
                return;
            }

            PlayerInventory playerInventory = this.mc.player.getInventory();

            for (int i3 = i2; i3 < i; i3++) {
                Slot slot = this.handler.slots.get(i3);
                if (slot.hasStack() && (slot.inventory != playerInventory || slot.getIndex() < 36)) {
                    this.mc.interactionManager.clickSlot(this.handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                }
            }
        }
    }

    private void dropAll() {
        if (this.mc.player != null && this.mc.interactionManager != null) {
            int i = this.handler.slots.size();
            int i2 = i - 36;
            if (i2 <= 0) {
                return;
            }

            for (int i3 = 0; i3 < i2; i3++) {
                Slot slot = this.handler.slots.get(i3);
                if (slot.hasStack()) {
                    this.mc.interactionManager.clickSlot(this.handler.syncId, slot.id, 1, SlotActionType.THROW, this.mc.player);
                }
            }
        }
    }

    @Unique
    private void dropAllInventory() {
        if (this.mc.player != null && this.mc.interactionManager != null) {
            int i = this.handler.slots.size();

            for (int i2 = 0; i2 < i; i2++) {
                Slot slot = this.handler.slots.get(i2);
                if (slot != null && slot.hasStack()) {
                    this.mc.interactionManager.clickSlot(this.handler.syncId, slot.id, 1, SlotActionType.THROW, this.mc.player);
                }
            }
        }
    }

    @Unique
    private Text sadnes$autoBuyLabel(AutoBuyController autoBuyController) {
        if (!autoBuyController.isRunning()) {
            return Text.literal("Автобай");
        }

        long l = autoBuyController.getLastResponseTime();
        return l > 0L ? Text.literal("Стоп · " + l + " мс") : Text.literal("Стоп");
    }

    @Unique
    private Text sadnes$scanLabel(AutoBuyPriceScanner autoBuyPriceScanner) {
        return !autoBuyPriceScanner.isRunning() ? Text.literal("Цены") : Text.literal("Стоп " + autoBuyPriceScanner.getProcessedItems() + "/" + autoBuyPriceScanner.getTotalItems());
    }

    @Unique
    private void sadnes$toggleCollector() {
        KitCollector kitCollector = KitCollector.getInstance();
        if (kitCollector != null) {
            kitCollector.toggle();
        }
    }

    @Unique
    private void sadnes$updateButtonsState() {
        if (this.sadnes$collectorButton != null) {
            this.sadnes$collectorButton.setMessage(KitCollector.getRunButtonText());
        }

        if (this.sadnes$autoBuyButton != null) {
            AutoBuyController autoBuyController = AutoBuyController.getInstance();
            if (autoBuyController != null) {
                this.sadnes$autoBuyButton.setMessage(this.sadnes$autoBuyLabel(autoBuyController));
            }
        }

        if (this.sadnes$scanButton != null) {
            AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
            if (autoBuyPriceScanner != null) {
                this.sadnes$scanButton.setMessage(this.sadnes$scanLabel(autoBuyPriceScanner));
            }
        }

        if (this.sadnes$takeAllButton != null) {
            this.sadnes$takeAllButton.active = this.sadnes$hasContainerItems();
        }

        if (this.sadnes$dropAllButton != null) {
            this.sadnes$dropAllButton.active = this.sadnes$hasContainerItems();
        }

        if (this.sadnes$putAllButton != null) {
            this.sadnes$putAllButton.active = this.sadnes$hasTransferItems();
        }

        if (this.sadnes$inventoryDropAllButton != null) {
            this.sadnes$inventoryDropAllButton.active = this.handler.slots.stream().filter(Objects::nonNull).anyMatch(Slot::hasStack);
        }
    }

    @Unique
    private boolean sadnes$hasContainerItems() {
        int i = this.handler.slots.size();
        int i2 = i - 36;
        if (i2 <= 0) {
            return false;
        }

        for (int i3 = 0; i3 < i2; i3++) {
            Slot slot = this.handler.slots.get(i3);
            if (slot != null && slot.hasStack()) {
                return true;
            }
        }

        return false;
    }

    @Unique
    private boolean sadnes$hasTransferItems() {
        if (this.mc.player == null) {
            return false;
        }

        int i = this.handler.slots.size();
        int i2 = i - 36;
        if (i2 <= 0) {
            return false;
        }

        PlayerInventory playerInventory = this.mc.player.getInventory();

        for (int i3 = i2; i3 < i; i3++) {
            Slot slot = this.handler.slots.get(i3);
            if (slot != null && slot.hasStack() && (slot.inventory != playerInventory || slot.getIndex() < 36)) {
                return true;
            }
        }

        return false;
    }
}
