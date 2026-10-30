package tech.sadnes.client.feature.impl.misc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.CommandTyper;
import tech.sadnes.client.util.math.TimerUtil;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

@FeatureInfo(
    name = "AutoSell",
    description = "feature.impl.misc.autosell",
    displayName = "feature.impl.misc.autosell",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoSell extends Feature {
    private final KeybindSetting sellKey = new KeybindSetting("autosell.sellKey");
    private final ModeSetting priceMode = new ModeSetting("autosell.priceMode", new String[]{"popular", "average", "minimal"}).setValue("popular");
    private final NumberSetting modifier = new NumberSetting("autosell.modifier", 0.0F, -99.0F, 99.0F, 1.0F, Unit.PERCENT);
    private final BooleanSetting automaticSell = new BooleanSetting("autosell.automaticSell", false);
    private final BooleanSetting humanTyping = new BooleanSetting("autosell.humanTyping", true);
    TimerUtil stepTimer = new TimerUtil();
    private final CommandTyper commandTyper = new CommandTyper();
    private final TimerUtil priceCacheTimer = new TimerUtil();
    private String cachedPriceKey;
    private long cachedPrice;
    private boolean pageRefreshed;
    private AutoSell.State state = AutoSell.State.Unknown;
    long calculatedPrice = -1L;
    int openAttempts = 0;
    private boolean forceSell;
    private long minimumPrice;
    private boolean priceBelowMinimum;
    private final String[][] itemNameMappings = new String[][]{
        {"modificator-ec", "Модификатор эндер-сундука"},
        {"potion-popper", "Хлопушка"},
        {"desorientation", "Дезориентация"},
        {"weaponskin-pearl", "Скин Жемчужный"},
        {"creeper-egg", "Яйцо крипера"},
        {"sword-kryshitel", "Меч Крушителя"},
        {"helmet-kryshitel", "Шлем Крушителя"},
        {"chestplate-kryshitel", "Нагрудник Крушителя"},
        {"leggings-kryshitel", "Поножи Крушителя"},
        {"boots-kryshitel", "Ботинки Крушителя"},
        {"customColor=254", "Снотворное"},
        {"customColor=16777215", "Святая вода"},
        {"customColor=3329330", "Зелье Радиации"},
        {"customColor=3355443", "Зелье Ассасина"},
        {"effect-item-smerch", "Огненный смерч"},
        {"effect-item-diz", "Дезориентация"},
        {"effect-item-snowball", "Снежок заморозка"},
        {"schematic-item-trap", "Трапка"},
        {"crossbow-kryshitel", "Арбалет Крушителя"},
        {"attribute-item-tkryshitela", "Талисман Крушителя"},
        {"mace-kryshitel", "Булава Крушителя"},
        {"attribute-item-tyarosti", "Талисман Ярости"},
        {"\"spookystash:levels\":15", "Бутылочка с уровнем 15"},
        {"\"spookystash:levels\":30", "Бутылочка с уровнем 30"},
        {"\"spookystash:levels\":45", "Бутылочка с уровнем 45"},
        {"\"spookystash:levels\":50", "Бутылочка с уровнем 50"}
    };

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (!ServerManager.isNullWorld()) {
            Packet packet = packetEvent.getPacket();
            boolean flag = packet instanceof GameMessageS2CPacket;
            AutoSell.State state2 = this.state;
            AutoSell.State state3;
            if (flag) {
                boolean flag2 = ((GameMessageS2CPacket)packet)
                    .content()
                    .getString()
                    .toLowerCase()
                    .contains("РІРІРµРґРёС‚Рµ РєРѕРјР°РЅРґСѓ РїСЂРѕРґР°Р¶Рё РµС‰С‘ СЂР°Р·, С‡С‚РѕР±С‹ РїРѕРґС‚РІРµСЂРґРёС‚СЊ");
                state3 = this.state;
                if (flag2) {
                    state3 = AutoSell.State.OpeningAuction;
                }
            } else {
                state3 = state2;
            }

            this.state = state3;
            if (AutoSell.State.RefreshingPage != state3) {
                return;
            }

            Packet packet2 = packetEvent.getPacket();
            if (!(packet2 instanceof InventoryS2CPacket)) {
                return;
            }

            if (((InventoryS2CPacket)packet2).syncId() != 0) {
                this.pageRefreshed = true;
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            if (this.state != AutoSell.State.Unknown) {
                if (this.state == AutoSell.State.OpeningAuction) {
                    if (mc.player.getMainHandStack().isEmpty()) {
                        ChatUtil.print(this.getName() + " - Возьми в руку предмет для продажи.");
                        this.commandTyper.cancel();
                        this.forceSell = false;
                        this.state = AutoSell.State.Unknown;
                    } else if (this.openAttempts >= 6) {
                        ChatUtil.print(this.getName() + " - Не удалось открыть аукцион.");
                        this.commandTyper.cancel();
                        this.forceSell = false;
                        this.state = AutoSell.State.Unknown;
                    } else if (this.hasCachedPrice()) {
                        this.calculatedPrice = this.calculateTotalPrice(this.cachedPrice);
                        this.state = AutoSell.State.SellingItem;
                    } else if (!this.commandTyper.isTyping()) {
                        if (mc.player.age % 10 == 0) {
                            if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
                                if (AuctionHelper.isAuctionScreen(genericContainerScreen)) {
                                    this.openAttempts = 0;
                                    this.pageRefreshed = false;
                                    boolean flag = mc.player.currentScreenHandler.slots.size() > 49;
                                    if (flag) {
                                        AuctionUtil.leftClickSlot(49);
                                    }

                                    this.stepTimer.reset();
                                    if (flag) {
                                        this.state = AutoSell.State.RefreshingPage;
                                    } else {
                                        this.state = AutoSell.State.ParsingPrices;
                                    }
                                } else {
                                    mc.player.closeHandledScreen();
                                }
                            } else if (this.stepTimer.isReached()) {
                                String string = this.getSearchQuery();
                                if (Profile.getRole() == Role.OWNER) {
                                    mc.keyboard.setClipboard("ah search " + string);
                                }

                                this.sendCommand("ah search " + string);
                                this.openAttempts++;
                                this.stepTimer.setDelay(3000L);
                            }
                        }
                    }
                } else if (this.state == AutoSell.State.RefreshingPage) {
                    if (this.pageRefreshed || this.stepTimer.hasPassed(1500L)) {
                        if (this.pageRefreshed) {
                            this.stepTimer.setDelay(200L);
                            this.state = AutoSell.State.ParsingPrices;
                        } else {
                            this.stepTimer.setDelay(0L);
                            this.state = AutoSell.State.ParsingPrices;
                        }
                    }
                } else if (this.state == AutoSell.State.ParsingPrices && this.stepTimer.isReached()) {
                    ScreenHandler screenHandler = mc.player.currentScreenHandler;
                    ItemStack itemStack = mc.player.getMainHandStack();
                    String string2 = AuctionLoreUtil.buildItemSignature(itemStack);
                    ArrayList<Integer> integers = new ArrayList<>();
                    AuctionPriceParser auctionPriceParser = new AuctionPriceParser();
                    int i = 0;

                    for (Slot slot : screenHandler.slots) {
                        ItemStack itemStack2 = slot.getStack();
                        if (!itemStack2.isEmpty() && itemStack2.getItem() == itemStack.getItem() && !itemStack2.getName().getString().contains("Товар не актуален")) {
                            if (!string2.equals(AuctionLoreUtil.buildItemSignature(itemStack2))) {
                                i++;
                            } else {
                                int i2 = auctionPriceParser.getPrice(itemStack2);
                                if (i2 > 0) {
                                    int i3 = itemStack2.getCount();
                                    int i4 = i3 > 2 ? i2 / i3 : i2;
                                    integers.add(i4);
                                }
                            }
                        }
                    }

                    if (integers.isEmpty()) {
                        ChatUtil.print(
                            this.getName()
                                + " - "
                                + (
                                    i > 0
                                        ? Language.translate("autosell.error.noSameEnchants", "Лоты с такими же чарами не найдены.")
                                        : "Не найдены цены для парсинга."
                                )
                        );
                        this.forceSell = false;
                        this.state = AutoSell.State.Unknown;
                    } else {
                        integers.sort(Integer::compareTo);
                        long l;
                        if (this.priceMode.is("minimal")) {
                            l = ((Integer)integers.getFirst()).intValue();
                        } else {
                            l = this.priceMode.is("popular") ? this.getMostFrequentPrice(integers) : 0L;
                            if (l <= 0L) {
                                int i5 = Math.min(3, integers.size());
                                long l2 = 0L;

                                for (int i6 = 0; i6 < i5; i6++) {
                                    l2 += ((Integer)integers.get(i6)).intValue();
                                }

                                l = l2 / i5;
                            }
                        }

                        this.cachePrice(l);
                        this.calculatedPrice = this.calculateTotalPrice(l);
                        this.state = AutoSell.State.SellingItem;
                    }
                } else {
                    if (this.state == AutoSell.State.SellingItem) {
                        if (this.calculatedPrice != -1L && this.minimumPrice > 0L && this.calculatedPrice <= this.minimumPrice) {
                            if (mc.currentScreen != null) {
                                mc.player.closeHandledScreen();
                            }

                            ChatUtil.print(
                                this.getName() + " - Цена ниже закупки ($" + String.format(Locale.US, "%,d", this.calculatedPrice) + "), не выставляю."
                            );
                            this.priceBelowMinimum = true;
                            this.calculatedPrice = -1L;
                            this.forceSell = false;
                            this.minimumPrice = 0L;
                            this.state = AutoSell.State.Unknown;
                            return;
                        }

                        if (this.calculatedPrice != -1L) {
                            if (mc.currentScreen != null) {
                                mc.player.closeHandledScreen();
                            }

                            if (!this.automaticSell.getValue() && !this.forceSell) {
                                ChatUtil.print(this.getName() + " - Советуемая цена для предмета: $" + String.format(Locale.US, "%,d", this.calculatedPrice));
                            } else {
                                this.sendCommand("ah sell " + this.calculatedPrice);
                            }
                        } else {
                            ChatUtil.print("Цена для предмета не была определена!");
                        }

                        this.calculatedPrice = -1L;
                        this.forceSell = false;
                        this.minimumPrice = 0L;
                        this.state = AutoSell.State.Unknown;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDisconnect(HudPostRenderEvent hudPostRenderEvent) {
        this.commandTyper.tick();
    }

    private long getMostFrequentPrice(List<Integer> integers) {
        int i = 0;
        byte b = 1;
        byte b2 = 0;
        int i2 = integers.size();

        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = (Integer)integers.get(i3);
            b2 = (byte)(i3 > 0 && i4 == integers.get(i3 - 1) ? b2 + 1 : 1);
            if (b2 > b) {
                b = b2;
                i = i4;
            }
        }

        return i;
    }

    private long calculateTotalPrice(long l) {
        long l2 = l * mc.player.getMainHandStack().getCount();
        int i = this.modifier.getValue().intValue();
        if (i != 0) {
            l2 += l2 * i / 100L;
        }

        long l3 = Math.round(l2 / 1000.0) * 1000L;
        return l3 <= 0L ? 1000L : l3;
    }

    private String getPriceCacheKey() {
        return this.getSearchQuery() + "|" + AuctionLoreUtil.buildItemSignature(mc.player.getMainHandStack());
    }

    private boolean hasCachedPrice() {
        return ServerManager.isFunTime()
            && this.cachedPriceKey != null
            && !this.priceCacheTimer.hasPassed(300000L)
            && this.cachedPriceKey.equals(this.getPriceCacheKey());
    }

    private void cachePrice(long l) {
        this.cachedPriceKey = this.getPriceCacheKey();
        this.cachedPrice = l;
        this.priceCacheTimer.reset();
    }

    private void sendCommand(String string) {
        if (this.humanTyping.getValue() && ServerManager.isFunTime()) {
            this.commandTyper.start(string);
        } else {
            mc.player.networkHandler.sendChatCommand(string);
        }
    }

    private String getSearchQuery() {
        ItemStack itemStack = mc.player.getMainHandStack();
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            AutoBuyItem autoBuyItem = autoBuyManager.findMatch(itemStack);
            if (autoBuyItem != null) {
                return autoBuyItem.getCategoryName();
            }
        }

        String string = itemStack.getComponents().toString();
        String[][] stringses = this.itemNameMappings;
        int i = Array.getLength(stringses);
        if (i > 0) {
            int i2 = i & 2147483647;
            long l = 0L;

            do {
                int i3 = (int)l;
                Object object = Array.get(stringses, i3);
                Object object2 = Array.get(object, 0);
                if (string.contains((CharSequence)object2)) {
                    return (String)Array.get(object, 1);
                }
            } while (Integer.toUnsignedLong(i2) != ++l);

            return AuctionLoreUtil.cleanName(itemStack.getName().getString());
        } else {
            return AuctionLoreUtil.cleanName(itemStack.getName().getString());
        }
    }

    public static AutoSell getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoSell.class);
    }

    public void startSell() {
        this.startSell(0L);
    }

    public void startSell(long l) {
        this.forceSell = true;
        this.priceBelowMinimum = false;
        this.minimumPrice = l;
        this.startPriceCheck();
    }

    private void startPriceCheck() {
        this.openAttempts = 0;
        this.stepTimer.setTime(0L);
        this.state = AutoSell.State.OpeningAuction;
    }

    public boolean isPriceBelowMinimum() {
        return this.priceBelowMinimum;
    }

    public boolean isBusy() {
        return this.state != AutoSell.State.Unknown;
    }

    @EventHandler
    public void onKeyPress(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress()) {
            Screen screen = screenMouseClickEvent.getScreen();
            KeybindSetting keybindSetting = this.sellKey;
            if (screen == null) {
                int i = screenMouseClickEvent.getButton();
                if (keybindSetting.matchesKey(i)) {
                    this.startPriceCheck();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMousePress(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress()) {
            Screen screen = screenKeyEvent.getScreen();
            KeybindSetting keybindSetting = this.sellKey;
            if (screen == null) {
                int i = screenKeyEvent.getKey();
                if (keybindSetting.matchesKey(i)) {
                    this.startPriceCheck();
                    return;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.commandTyper.cancel();
        this.cachedPriceKey = null;
        this.calculatedPrice = -1L;
        this.openAttempts = 0;
        AutoSell.State state2 = AutoSell.State.Unknown;
        this.state = state2;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.commandTyper.cancel();
        this.cachedPriceKey = null;
        this.calculatedPrice = -1L;
        this.openAttempts = 0;
        AutoSell.State state2 = AutoSell.State.Unknown;
        this.state = state2;
    }

    enum State {
        Unknown,
        OpeningAuction,
        RefreshingPage,
        ParsingPrices,
        SellingItem;
    }
}
