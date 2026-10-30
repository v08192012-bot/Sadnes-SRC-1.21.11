package tech.sadnes.client.feature.impl.misc.autobuild;

import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import tech.sadnes.client.feature.impl.misc.autobuy.PriceGuard;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.donut.DonutPriceUtil;
import tech.sadnes.client.server.donut.QuickAction;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.ChatMessageUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;

final class DonutShopStrategy implements ShopStrategy, IMinecraft {
    private final ShopDialogHelper dialogHelper;
    private final StopWatch timer = new StopWatch();
    private DonutShopStrategy.State state = DonutShopStrategy.State.IDLE;
    private Item item;
    private PriceGuard priceTracker;
    private int targetCount;
    private int countBeforeBuy;
    private int lotSize;
    private int openAttempts;
    private int retries;
    private int pickAttempts;
    private int searchCount;
    private int dialogClicks;
    private int purchases;
    private long pauseDuration;
    private ScreenHandler shopMenu;
    private Dialog pendingDialog;
    private boolean bought;
    private boolean abortAll;
    private boolean itemPicked;
    private boolean waitRequested;
    private boolean refused;
    private boolean alreadyBought;
    private boolean notEnoughMoney;

    @Override
    public boolean isReady() {
        return this.state == DonutShopStrategy.State.IDLE;
    }

    @Override
    public void start(Item item2, int i, PriceGuard priceGuard) {
        this.item = item2;
        this.priceTracker = priceGuard;
        this.targetCount = MaterialShopper.countItem(item2) + i;
        this.bought = false;
        this.openAttempts = this.retries = this.pickAttempts = this.purchases = 0;
        this.pauseFor(0L);
    }

    @Override
    public boolean isBusy() {
        return this.state != DonutShopStrategy.State.IDLE;
    }

    @Override
    public boolean wasSuccessful() {
        return this.bought;
    }

    @Override
    public boolean shouldAbortAll() {
        return this.abortAll;
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            this.state = DonutShopStrategy.State.IDLE;
        } else {
            switch (this.state) {
                case IDLE:
                default:
                    break;
                case PAUSE:
                    if (this.timer.isElapsed(this.pauseDuration)) {
                        this.openShop();
                        return;
                    }
                    break;
                case OPENING:
                    this.waitForShop();
                    return;
                case SHOWCASE:
                    this.scanShowcase();
                    return;
                case PICKING:
                    this.waitForPick();
                    return;
                case BUYING:
                    this.waitForPurchase();
            }
        }
    }

    @Override
    public void onChatMessage(String string) {
        if (this.state != DonutShopStrategy.State.IDLE && !ChatMessageUtil.isPlayerMessage(string)) {
            String string2 = string.toLowerCase(Locale.ROOT);
            if (string2.contains("недостаточно") || string2.contains("not enough") || string2.contains("afford")) {
                this.notEnoughMoney = true;
            } else if (string2.contains("уже купили") || string2.contains("already")) {
                this.alreadyBought = true;
            } else if (!string2.contains("подождать") && !string2.contains("wait")) {
                if (ChatMessageUtil.isErrorMessage(string)) {
                    this.refused = true;
                }
            } else {
                this.waitRequested = true;
            }
        }
    }

    @Override
    public void onDialog(Dialog dialog) {
        if (this.state == DonutShopStrategy.State.PICKING || this.state == DonutShopStrategy.State.BUYING) {
            this.pendingDialog = dialog;
        }
    }

    @Override
    public void stop() {
        this.state = DonutShopStrategy.State.IDLE;
        this.dialogHelper.closeDialog();
        if (mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
        }

        this.abortAll = false;
    }

    private void openShop() {
        this.waitRequested = this.refused = this.alreadyBought = this.notEnoughMoney = false;
        this.pendingDialog = null;
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (this.isShopMenu(screenHandler) && mc.currentScreen instanceof HandledScreen) {
            this.shopMenu = screenHandler;
            this.setState(DonutShopStrategy.State.SHOWCASE);
        } else if (screenHandler != mc.player.playerScreenHandler) {
            mc.player.closeHandledScreen();
            this.pauseFor(600L);
        } else {
            this.openAttempts++;
            if (this.openAttempts % 2 == 0 && QuickAction.SHOP.run()) {
                this.setState(DonutShopStrategy.State.OPENING);
            } else {
                mc.player.networkHandler.sendChatCommand("shop");
                this.setState(DonutShopStrategy.State.OPENING);
            }
        }
    }

    private void waitForShop() {
        if (this.isShopMenu(mc.player.currentScreenHandler)) {
            this.shopMenu = mc.player.currentScreenHandler;
            this.openAttempts = 0;
            this.setState(DonutShopStrategy.State.SHOWCASE);
        } else if (this.refused) {
            this.abortAll = true;
            this.fail("autobuild.shop.refused");
        } else if (this.waitRequested || this.timer.isElapsed(3000L)) {
            if (this.openAttempts >= 6) {
                this.fail("autobuild.shop.noWindow");
            } else if (this.waitRequested) {
                this.pauseFor(1000L);
            } else {
                this.pauseFor(5000L);
            }
        }
    }

    private void scanShowcase() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (screenHandler != this.shopMenu) {
            if (this.isShopMenu(screenHandler)) {
                this.shopMenu = screenHandler;
                this.timer.reset();
            } else if (screenHandler != mc.player.playerScreenHandler || this.timer.isElapsed(600L + ServerManager.getPing())) {
                if (++this.retries > 3) {
                    this.fail("autobuild.shop.noWindow");
                } else {
                    this.pauseFor(0L);
                }
            }
        } else if (this.timer.isElapsed(400 + ServerManager.getPing())) {
            if (!this.dialogHelper.canInteract()) {
                if (this.timer.isElapsed(8000L)) {
                    this.fail("autobuild.shop.noAnswer");
                }
            } else {
                this.dialogHelper.dumpScreen("first /shop window", this.shopMenu);
                int i = -1;
                int i2 = -1;
                boolean flag = false;

                for (int i3 = 0; i3 < 45 && i < 0; i3++) {
                    ItemStack itemStack = this.shopMenu.getSlot(i3).getStack();
                    long l = itemStack.isEmpty() ? -1L : DonutPriceUtil.getItemPrice(itemStack);
                    flag |= itemStack.isOf(this.item);
                    if (itemStack.isOf(this.item) && l > 0L) {
                        i = i3;
                    }

                    LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
                    boolean flag2;
                    if (itemStack.isOf(Items.GRAY_STAINED_GLASS_PANE) && l < 0L && loreComponent != null) {
                        flag2 = !loreComponent.lines().isEmpty();
                    } else {
                        flag2 = false;
                    }

                    if (flag2 && i2 < 0) {
                        i2 = i3;
                    }
                }

                if (i >= 0) {
                    this.buyLot(i);
                } else if (flag) {
                    this.fail("autobuild.shop.noLots");
                } else if (i2 >= 0 && this.pickAttempts < 2) {
                    this.pickAttempts++;
                    this.searchCount = this.dialogClicks = 0;
                    this.itemPicked = false;
                    this.pendingDialog = null;
                    AuctionUtil.leftClickSlot(i2);
                    this.setState(DonutShopStrategy.State.PICKING);
                } else {
                    this.dialogHelper.dumpScreen(i2 < 0 ? "no free slot in the /shop window" : "item did not appear in the /shop window", this.shopMenu);
                    if (i2 < 0) {
                        this.fail("autobuild.shop.noSlot");
                    } else {
                        this.fail("autobuild.shop.noPick");
                    }
                }
            }
        }
    }

    private void buyLot(int i) {
        ItemStack itemStack = this.shopMenu.getSlot(i).getStack();
        long l = DonutPriceUtil.getItemPrice(itemStack);
        double d = (double)l / itemStack.getCount();
        this.priceTracker.recordPrice(this.item, d);
        if (!this.priceTracker.isAcceptable(this.item, d)) {
            this.fail("autobuild.shop.overprice");
        } else if (!this.dialogHelper.canAfford(l)) {
            this.fail("autobuild.shop.noMoney");
        } else {
            this.countBeforeBuy = MaterialShopper.countItem(this.item);
            this.lotSize = itemStack.getCount();
            this.alreadyBought = this.notEnoughMoney = false;
            this.pendingDialog = null;
            AuctionUtil.leftClickSlot(i);
            this.setState(DonutShopStrategy.State.BUYING);
        }
    }

    private void waitForPurchase() {
        boolean flag = MaterialShopper.countItem(this.item) > this.countBeforeBuy;
        if (!flag) {
            if (this.pendingDialog != null) {
                this.answerPurchaseDialog(this.pendingDialog);
            } else if (this.notEnoughMoney) {
                this.abortAll = true;
                this.fail("autobuild.shop.noMoney");
            } else if (this.alreadyBought) {
                if (++this.retries > 3) {
                    this.fail("autobuild.shop.taken");
                } else {
                    this.setState(DonutShopStrategy.State.SHOWCASE);
                }
            } else if (this.timer.isElapsed(3000L + ServerManager.getPing() * 2L)) {
                this.dialogHelper.dumpScreen("/shop click got no answer", mc.player.currentScreenHandler);
                this.fail("autobuild.shop.noAnswer");
            }
        } else {
            this.bought = true;
            this.purchases++;
            this.retries = 0;
            boolean flag2 = MaterialShopper.countItem(this.item) >= this.targetCount || this.purchases >= 30;
            if (flag2) {
                this.state = DonutShopStrategy.State.IDLE;
            } else {
                this.setState(DonutShopStrategy.State.SHOWCASE);
            }
        }
    }

    private void answerPurchaseDialog(Dialog dialog) {
        this.pendingDialog = null;
        this.dialogHelper.dumpDialog("purchase question", dialog);
        DialogActionButtonData dialogActionButtonData = this.dialogHelper.findConfirmButton(dialog);
        DialogActionButtonData dialogActionButtonData2 = this.dialogHelper.findCancelButton(dialog);
        long l = this.dialogHelper.getDialogPrice(dialog);
        int i = this.dialogHelper.getDialogItemCount(dialog) > 0 ? this.dialogHelper.getDialogItemCount(dialog) : this.lotSize;
        if ((l <= 0L || this.priceTracker.isAcceptable(this.item, (double)l / i) && this.dialogHelper.canAfford(l)) && dialogActionButtonData != null) {
            this.dialogHelper.clickButton(dialog, dialogActionButtonData, Map.of());
            this.timer.reset();
        } else {
            if (dialogActionButtonData2 != null) {
                this.dialogHelper.clickButton(dialog, dialogActionButtonData2, Map.of());
            } else {
                this.dialogHelper.closeDialog();
            }

            if (dialogActionButtonData == null) {
                this.fail("autobuild.shop.noAnswer");
            } else {
                this.fail("autobuild.shop.overprice");
            }
        }
    }

    private void waitForPick() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (screenHandler != this.shopMenu && this.isShopMenu(screenHandler)) {
            this.shopMenu = screenHandler;
            this.setState(DonutShopStrategy.State.SHOWCASE);
        } else if (this.pendingDialog != null) {
            this.answerPickDialog(this.pendingDialog);
        } else if (this.timer.isElapsed(3000 + ServerManager.getPing())) {
            this.dialogHelper.closeDialog();
            if (this.itemPicked) {
                this.pauseFor(0L);
            } else {
                this.dialogHelper.dumpScreen("empty /shop slot opened no dialog", screenHandler);
                this.fail("autobuild.shop.noPick");
            }
        }
    }

    private void answerPickDialog(Dialog dialog) {
        this.pendingDialog = null;
        String string = this.dialogHelper.getEnglishName(this.item);
        String string2 = this.item.getName().getString();
        DialogActionButtonData dialogActionButtonData = null;
        DialogActionButtonData dialogActionButtonData2 = null;

        for (DialogActionButtonData dialogActionButtonData3 : this.dialogHelper.getButtons(dialog)) {
            String string3 = this.dialogHelper.getButtonLabel(dialogActionButtonData3);
            boolean flag;
            if (!string3.equalsIgnoreCase(string)) {
                flag = string3.equalsIgnoreCase(string2);
            } else {
                flag = true;
            }

            if (flag && dialogActionButtonData == null) {
                dialogActionButtonData = dialogActionButtonData3;
            }

            if (string3.equalsIgnoreCase("search") || string3.equalsIgnoreCase("поиск")) {
                dialogActionButtonData2 = dialogActionButtonData3;
            }
        }

        DialogActionButtonData dialogActionButtonData4 = dialogActionButtonData == null ? this.dialogHelper.findConfirmButton(dialog) : null;
        boolean flag2;
        DialogActionButtonData dialogActionButtonData5;
        if (dialogActionButtonData == null && dialogActionButtonData4 == null && dialogActionButtonData2 != null && this.searchCount == 0) {
            flag2 = true;
            dialogActionButtonData5 = dialogActionButtonData2;
        } else {
            flag2 = false;
            dialogActionButtonData5 = dialogActionButtonData != null ? dialogActionButtonData : (dialogActionButtonData4 != null ? dialogActionButtonData4 : null);
        }

        DialogActionButtonData dialogActionButtonData6 = dialogActionButtonData5;
        this.dialogHelper
            .dumpDialog(dialogActionButtonData != null ? "item button pressed" : (dialogActionButtonData4 != null ? "item question" : (flag2 ? "item search" : "item not in the dialog")), dialog);
        Map map = flag2 ? this.dialogHelper.fillTextInputs(dialog, string) : Map.of();
        if (dialogActionButtonData6 != null && ++this.dialogClicks <= 6 && this.dialogHelper.clickButton(dialog, dialogActionButtonData6, map)) {
            if (flag2) {
                this.searchCount++;
            }

            if (dialogActionButtonData != null) {
                this.itemPicked = true;
                this.timer.reset();
            } else {
                this.itemPicked = this.itemPicked;
                this.timer.reset();
            }
        } else {
            this.dialogHelper.closeDialog();
            this.fail("autobuild.shop.noPick");
        }
    }

    private boolean isShopMenu(ScreenHandler screenHandler) {
        return screenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler && genericContainerScreenHandler.getRows() == 6 && genericContainerScreenHandler.getSlot(53).getStack().isOf(Items.DEBUG_STICK);
    }

    private void pauseFor(long l) {
        this.pauseDuration = l;
        this.setState(DonutShopStrategy.State.PAUSE);
    }

    private void setState(DonutShopStrategy.State state2) {
        this.state = state2;
        this.timer.reset();
    }

    private void fail(String string) {
        if (!this.bought) {
            NotificationUtil.warning(this.item.getName().getString(), Language.translate(string));
        }

        this.state = DonutShopStrategy.State.IDLE;
    }

    public DonutShopStrategy(ShopDialogHelper shopDialogHelper) {
        this.dialogHelper = shopDialogHelper;
    }

    enum State {
        IDLE,
        PAUSE,
        OPENING,
        SHOWCASE,
        PICKING,
        BUYING;
    }
}
