package tech.sadnes.client.ui.clickgui.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.StaggeredAppearAnimation;
import tech.sadnes.client.feature.component.AutoReconnectComponent;
import tech.sadnes.client.feature.impl.misc.Events;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.FunTimeEventType;
import tech.sadnes.client.server.FunTimeEventsApi;
import tech.sadnes.client.server.HolyWorldEventsApi;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.hud.WaypointRenderer;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class EventsPage implements IRenderable, IMinecraft {
    private static final float TAB_HEIGHT = 15.0F;
    private static final float CARD_HEIGHT = 34.0F;
    private static final float CARD_GAP = 4.0F;
    private static final float CARD_ICON_SIZE = 22.0F;
    private static final int COLUMNS = 2;
    private static final float HEADER_ICON_SIZE = 7.0F;
    private static final float FADE_HEIGHT = 12.0F;
    private static final float GROUP_HEADER_HEIGHT = 14.0F;
    private final ClickGuiScreen clickGui;
    private final StaggeredAppearAnimation staggerAnimator = new StaggeredAppearAnimation();
    private final SmoothScrollbar scrollbar = new SmoothScrollbar();
    private final Animation tabAnimation = new Animation();
    private final Map<String, Animation> cardHoverAnimations = new HashMap<>();
    private EventsPage.EventSource source = EventsPage.EventSource.HOLY;
    private final List<EventsPage.EventCard> cards = new ArrayList<>();
    private final List<EventsPage.CardBounds> cardBounds = new ArrayList<>();
    private float tabsX;
    private float tabsY;
    private float tabsWidth;
    private float filterButtonX;
    private final Animation filterHoverAnimation = new Animation();
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;
    private float cardWidth;

    public EventsPage(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;
        this.source = ServerManager.isHolyWorld() ? EventsPage.EventSource.HOLY : EventsPage.EventSource.FUNTIME;
    }

    public void onOpen() {
        this.staggerAnimator.reset();
        this.scrollbar.reset();
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.staggerAnimator.update();
        float f6 = this.getOpenAlpha();
        this.tabsX = f2;
        this.tabsY = f3 + this.staggerAnimator.getOffset(0);
        this.filterButtonX = f2 + f4 - 15.0F;
        this.tabsWidth = f4 - 15.0F - 6.0F;
        this.listX = f2;
        this.listY = f3 + 15.0F + 6.0F;
        this.listWidth = f4;
        this.listHeight = f3 + f5 - this.listY;
        this.cardWidth = (this.listWidth - 7.0F - 4.0F) / 2.0F;
        Events events = Events.getInstance();
        if (events != null) {
            events.setFunTimeMode(this.source == EventsPage.EventSource.FUNTIME);
        }

        this.refreshCards();
        this.renderSourceTabs(drawContext, i, i2, f6);
        this.renderFilterButton(drawContext, i, i2, f6);
        this.renderEventList(drawContext, i, i2, f6);
    }

    private void renderSourceTabs(DrawContext drawContext, int i, int i2, float f) {
        EventsPage.EventSource[] eventSources = EventsPage.EventSource.values();
        this.tabAnimation.update();
        this.tabAnimation.animate(this.source.ordinal(), 0.3, Easings.QUAD_OUT);
        float f2 = this.tabsWidth / eventSources.length;
        float f3 = Math.clamp(this.tabAnimation.get(), 0.0F, eventSources.length - 1.0F);
        GuiRenderUtil.drawOutlined(drawContext, this.tabsX, this.tabsY, this.tabsWidth, 15.0F, f * 0.7F, 5.0F);
        GuiRenderUtil.drawHighlight(drawContext, this.tabsX + 1.5F + f2 * f3, this.tabsY + 1.5F, f2 - 3.0F, 12.0F, f * 2.0F, Radius.all(4.0F));
        int i3 = 0;

        for (int i4 = eventSources.length; i3 < i4; i3++) {
            float f4 = Math.max(0.0F, 1.0F - Math.abs(f3 - i3));
            int i5 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.45F + 0.55F * f4), f);
            new RenderUtil(drawContext, this.tabsX + f2 * i3, this.tabsY, f2, 15.0F).textCenter(Fonts.INTER_MEDIUM, eventSources[i3].getDisplayName(), i5, 6.0F);
        }
    }

    private void renderFilterButton(DrawContext drawContext, int i, int i2, float f) {
        this.filterHoverAnimation.update();
        this.filterHoverAnimation.animate(this.isHovered(i, i2, this.filterButtonX, this.tabsY, 15.0, 15.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = this.filterHoverAnimation.get();
        GuiRenderUtil.drawOutlined(drawContext, this.filterButtonX, this.tabsY, 15.0F, 15.0F, f * (0.7F + 0.2F * f2), 5.0F);
        new RenderUtil(drawContext, this.filterButtonX, this.tabsY, 15.0F, 15.0F)
            .icon(Icon.SETTINGS, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F + 0.3F * f2), f));
    }

    private void renderEventList(DrawContext drawContext, int i, int i2, float f) {
        this.cardBounds.clear();
        if (!(this.listHeight <= 0.0F)) {
            List list = this.buildListEntries();
            float f2 = this.computeContentHeight(list);
            this.scrollbar.update(f2, this.listHeight);
            this.scrollbar.updateHover(i, i2);
            if (f2 > this.listHeight) {
                this.scrollbar.render(drawContext, this.listX + this.listWidth - 2.5F, this.listY, this.listHeight, f2, f);
            }

            if (this.cards.isEmpty()) {
                this.renderEmptyState(drawContext, f);
            } else if (ScissorStack.push(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)) {
                try {
                    this.applyEdgeFade(f2);
                    float f3 = this.listY + this.scrollbar.getScroll();
                    int i3 = 0;
                    int i4 = 0;

                    for (Object object : list) {
                        if (object instanceof EventsPage.EventGroup eventGroup) {
                            if (i3 != 0) {
                                f3 += 38.0F;
                                i3 = 0;
                                i4++;
                            }

                            if (f3 + 14.0F >= this.listY && f3 <= this.listY + this.listHeight) {
                                this.renderGroupHeader(
                                    drawContext, eventGroup, f3 + this.staggerAnimator.getOffset(2 + i4), f * this.staggerAnimator.getProgress(2 + i4)
                                );
                            }

                            f3 += 14.0F;
                        } else {
                            EventsPage.EventCard eventCard = (EventsPage.EventCard)object;
                            float f4 = this.listX + i3 * (this.cardWidth + 4.0F);
                            float f5 = f3 + this.staggerAnimator.getOffset(2 + i4);
                            if (f5 + 34.0F >= this.listY && f5 <= this.listY + this.listHeight) {
                                this.renderCard(
                                    drawContext, eventCard, this.cardBounds.size(), f4, f5, i, i2, f * this.staggerAnimator.getProgress(2 + i4)
                                );
                            }

                            this.cardBounds.add(new EventsPage.CardBounds(eventCard, f4, f5, this.cardWidth));
                            if (++i3 >= 2) {
                                i3 = 0;
                                i4++;
                                f3 += 38.0F;
                            }
                        }
                    }
                } finally {
                    VerticalFadeMask.end();
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderGroupHeader(DrawContext drawContext, EventsPage.EventGroup eventGroup, float f, float f2) {
        String string = eventGroup.getDisplayName();
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f2);
        new RenderUtil(drawContext, this.listX, f, this.listWidth, 14.0F)
            .icon(eventGroup.icon, 0.0F, 3.5F, 7.0F, i)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 11.0F, 0.0F, i, 5.5F);
        float f3 = this.listX + 11.0F + Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 6.0F;
        new RenderUtil(drawContext, f3, f + 7.0F - 0.25F, this.listX + this.listWidth - 7.0F - f3, 0.5F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.22F), f2), Radius.all(0.0F));
    }

    private List<Object> buildListEntries() {
        ArrayList arrayList = new ArrayList();
        if (this.source == EventsPage.EventSource.HOLY) {
            arrayList.addAll(this.cards);
            return arrayList;
        }

        for (EventsPage.EventGroup eventGroup : EventsPage.EventGroup.values()) {
            if (eventGroup != EventsPage.EventGroup.ALL) {
                ArrayList arrayList2 = new ArrayList();

                for (EventsPage.EventCard eventCard : this.cards) {
                    if (eventCard.group() == eventGroup) {
                        arrayList2.add(eventCard);
                    }
                }

                if (!arrayList2.isEmpty()) {
                    arrayList.add(eventGroup);
                    arrayList.addAll(arrayList2);
                }
            }
        }

        return arrayList;
    }

    private float computeContentHeight(List<Object> objects) {
        float f = 0.0F;
        int i = 0;

        for (Object object : objects) {
            if (object instanceof EventsPage.EventGroup) {
                if (i != 0) {
                    f += 38.0F;
                    i = 0;
                }

                f += 14.0F;
            } else if (++i >= 2) {
                i = 0;
                f += 38.0F;
            }
        }

        return i == 0 ? f : f + 34.0F + 4.0F;
    }

    private void renderEmptyState(DrawContext drawContext, float f) {
        boolean flag;
        if (this.source == EventsPage.EventSource.HOLY) {
            flag = !HolyWorldEventsApi.isLoaded();
        } else {
            flag = !FunTimeEventsApi.isLoaded();
        }

        String string = flag ? Language.translate("events.page.offline", "Сервер недоступен") : Language.translate("events.page.empty", "Нет событий");
        String string2 = flag
            ? Language.translate("events.page.offline.desc", "Проверь подключение к серверу")
            : Language.translate("events.page.waiting", "Ждём данные сервера");
        new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
            .icon(
                flag ? Icon.LINK_SLASH : Icon.CLOCK_HOUR_4,
                this.listWidth / 2.0F - 9.0F,
                this.listHeight / 2.0F - 24.0F,
                18.0F,
                Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f)
            )
            .textCenter(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), 0.7F * f), 8.0F)
            .textCenter(Fonts.INTER_REGULAR, string2, 0.0F, 11.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f), 6.5F);
    }

    private void applyEdgeFade(float f) {
        float f2 = f - this.listHeight;
        if (!(f2 <= 0.5F) && !(this.listHeight <= 24.0F)) {
            float f3 = this.scrollbar.getScroll();
            float f4 = Math.clamp(-f3 / 12.0F, 0.0F, 1.0F);
            float f5 = Math.clamp((f2 + f3) / 12.0F, 0.0F, 1.0F);
            if (!(f4 <= 0.001F) || !(f5 <= 0.001F)) {
                VerticalFadeMask.begin(this.listY, this.listY + 12.0F, f4, this.listY + this.listHeight - 12.0F, this.listY + this.listHeight, f5);
            }
        }
    }

    private void renderCard(DrawContext drawContext, EventsPage.EventCard eventCard, int i, float f, float f2, int i2, int i3, float f3) {
        Animation animation = this.cardHoverAnimations.computeIfAbsent(cardKey(eventCard) + "#" + i, string -> new Animation());
        animation.update();
        animation.animate(this.isHovered(i2, i3, f, f2, this.cardWidth, 34.0) ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        float f4 = animation.get();
        GuiRenderUtil.drawOutlined(drawContext, f, f2, this.cardWidth, 34.0F, f3 * (0.7F + 0.15F * f4), 8.0F);
        if (f4 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, this.cardWidth, 34.0F, f3 * f4 * 0.8F, Radius.all(8.0F));
        }

        float f5 = f + 5.0F;
        float f6 = f2 + 17.0F - 11.0F;
        new RenderUtil(drawContext, f5, f6, 22.0F, 22.0F)
            .rect(Theme.withAlpha(Theme.getPrimary(), (0.12F + 0.06F * f4) * f3), Radius.all(7.0F))
            .icon(eventCard.icon(), 6.0F, 6.0F, 10.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f3));
        float f7 = f5 + 22.0F + 6.0F;
        float f8 = eventCard.time().isEmpty() ? 0.0F : Fonts.INTER_MEDIUM.getWidth(eventCard.time(), 6.5F) + 6.0F;
        float f9 = f + this.cardWidth - 6.0F - f7 - f8;
        String string2 = eventCard.count() > 1 ? eventCard.name() + " ×" + eventCard.count() : eventCard.name();
        new RenderUtil(drawContext, f7, f2 + 7.0F, f9, 8.0F)
            .textTopLeft(Fonts.INTER_SEMIBOLD, this.ellipsize(string2, f9, 6.0F), 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), 6.0F);
        String string3 = eventCard.server();
        if (!eventCard.loot().isEmpty()) {
            string3 = string3 + " · " + eventCard.loot();
        }

        if (eventCard.located()) {
            string3 = string3 + " · " + eventCard.x() + ", " + eventCard.z();
        }

        int i4 = eventCard.loot().isEmpty()
            ? ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.75F), f3)
            : ColorUtil.multiplyAlpha(lootColor(eventCard.loot()), f3);
        new RenderUtil(drawContext, f7, f2 + 34.0F - 13.0F, f9, 8.0F)
            .textTopLeft(Fonts.INTER_MEDIUM, this.ellipsize(string3, f9, 5.5F), 0.0F, 0.0F, i4, 5.5F);
        if (!eventCard.time().isEmpty()) {
            new RenderUtil(drawContext, f + this.cardWidth - f8, f2, f8 - 4.0F, 34.0F)
                .textCenter(Fonts.INTER_MEDIUM, eventCard.time(), ColorUtil.multiplyAlpha(Theme.getPrimary(), f3), 6.5F);
        }
    }

    private void refreshCards() {
        this.cards.clear();
        if (this.source == EventsPage.EventSource.HOLY) {
            this.loadHolyWorldEvents();
        } else {
            this.loadFunTimeEvents();
        }
    }

    private void loadHolyWorldEvents() {
        HolyWorldEventsApi.refresh();
        Events events = Events.getInstance();
        ArrayList arrayList = new ArrayList();

        for (HolyWorldEventsApi.EventInfo eventInfo : HolyWorldEventsApi.getEvents()) {
            if (this.isAnarchyAllowed(eventInfo.anarchy()) && (events == null || events.isEventAllowed(eventInfo.type()) && events.isModeAllowed(eventInfo.server()))) {
                arrayList.add(
                    new EventsPage.EventCard(
                        eventInfo.name(),
                        eventInfo.server(),
                        eventInfo.anarchy(),
                        "",
                        Integer.MAX_VALUE,
                        localizeRarity(eventInfo.rare()),
                        iconForType(eventInfo.type()),
                        false,
                        0,
                        0,
                        0,
                        1
                    )
                );
            }
        }

        arrayList.sort(Comparator.comparingInt(EventsPage.EventCard::anarchy).thenComparing(EventsPage.EventCard::name));
        this.cards.addAll(arrayList);
    }

    private boolean isAnarchyAllowed(int i) {
        Events events = Events.getInstance();
        return events == null || events.isAnarchyAllowed(i);
    }

    private void loadFunTimeEvents() {
        FunTimeEventsApi.refresh();
        long l = System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        Events events = Events.getInstance();

        for (FunTimeEventsApi.EventInfo eventInfo : FunTimeEventsApi.getEvents()) {
            if (eventInfo.system() && this.isAnarchyAllowed(eventInfo.anarchy())) {
                if (events != null) {
                    boolean flag = !eventInfo.loot().isEmpty();
                    if (flag && !events.showActive() || !events.isFunTimeEventAllowed(eventInfo.id()) || !events.isLootAllowed(eventInfo.loot())) {
                        continue;
                    }
                }

                String string = eventInfo.id();
                String string2 = string.isEmpty()
                    ? Language.translate("eventtimer.ftupcoming", "Предстоящее")
                    : Language.translate("eventtimer.ftevents." + string, string);
                int i = eventInfo.secondsLeft() - (int)((l - FunTimeEventsApi.getLastUpdateTime()) / 1000L);
                EventsPage.EventCard eventCard = new EventsPage.EventCard(
                    string2,
                    Language.translate("events.page.anarchy", "Анархия") + " " + eventInfo.anarchy(),
                    eventInfo.anarchy(),
                    i > 0 ? formatSeconds(i) : "",
                    i > 0 ? i : Integer.MAX_VALUE,
                    localizeRarity(eventInfo.loot()),
                    FunTimeEventType.getIconById(string, Icon.CLOCK_HOUR_4),
                    eventInfo.located(),
                    eventInfo.x(),
                    eventInfo.y(),
                    eventInfo.z(),
                    1
                );
                int i2 = indexOfAnarchy(arrayList, eventInfo.anarchy());
                if (i2 < 0) {
                    arrayList.add(eventCard);
                } else if (isBetterCard(eventCard, (EventsPage.EventCard)arrayList.get(i2))) {
                    arrayList.set(i2, eventCard);
                }
            }
        }

        Events events2 = Events.getInstance();
        Comparator comparator = events2 != null && events2.isSortByRarity()
            ? Comparator.<EventsPage.EventCard>comparingInt(eventCard2 -> lootPriority(eventCard2.loot())).reversed().thenComparingInt(EventsPage.EventCard::seconds)
            : Comparator.comparingInt(EventsPage.EventCard::seconds);
        arrayList.sort(Comparator.comparingInt(EventsPage.EventCard::anarchy).thenComparing(comparator));
        this.cards.addAll(arrayList);
    }

    private static int indexOfAnarchy(List<EventsPage.EventCard> eventCards, int i) {
        int i2 = 0;

        for (int i3 = eventCards.size(); i2 < i3; i2++) {
            if (((EventsPage.EventCard)eventCards.get(i2)).anarchy() == i) {
                return i2;
            }
        }

        return -1;
    }

    private static boolean isBetterCard(EventsPage.EventCard eventCard, EventsPage.EventCard eventCard2) {
        boolean flag = eventCard.located();
        boolean flag2 = eventCard2.located();
        return flag != flag2 ? flag : eventCard.seconds() < eventCard2.seconds();
    }

    private static String formatSeconds(int i) {
        int i2 = i / 60;
        int i3 = i % 60;
        return i2 + ":" + (i3 < 10 ? "0" + i3 : String.valueOf(i3));
    }

    private static String localizeRarity(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.trim();
            if (string2.indexOf(95) < 0 && !containsLatinLetter(string2)) {
                return string2;
            }

            String string3 = string2.contains("_") ? string2.substring(0, string2.indexOf(95)) : string2;
            String string4 = string3.toLowerCase(Locale.US);
            byte b = -1;
            switch (string4.hashCode()) {
                case -2081562821:
                    if (string4.equals("legendary")) {
                        return Language.translate("events.page.rarity.legendary", "Легендарный");
                    }
                    break;
                case -1354814997:
                    if (string4.equals("common")) {
                        return Language.translate("events.page.rarity.common", "Обычный");
                    }
                    break;
                case -1059084742:
                    if (string4.equals("mythic")) {
                        return Language.translate("events.page.rarity.mythic", "Мифический");
                    }
                    break;
                case 3119877:
                    if (string4.equals("epic")) {
                        return Language.translate("events.page.rarity.epic", "Эпический");
                    }
                    break;
                case 3493026:
                    if (string4.equals("rare")) {
                        return Language.translate("events.page.rarity.rare", "Редкий");
                    }
                    break;
                case 96597651:
                    if (string4.equals("elite")) {
                        b = 5;
                    }
            }

            switch (b) {
                case 0:
                    return Language.translate("events.page.rarity.common", "Обычный");
                case 1:
                    return Language.translate("events.page.rarity.rare", "Редкий");
                case 2:
                    return Language.translate("events.page.rarity.epic", "Эпический");
                case 3:
                    return Language.translate("events.page.rarity.mythic", "Мифический");
                case 4:
                    return Language.translate("events.page.rarity.legendary", "Легендарный");
                case 5:
                    return Language.translate("events.page.rarity.elite", "Элитный");
                default:
                    return "";
            }
        } else {
            return "";
        }
    }

    private static boolean containsLatinLetter(String string) {
        int i = 0;

        for (int i2 = string.length(); i < i2; i++) {
            char c = string.charAt(i);
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                return true;
            }
        }

        return false;
    }

    private static int lootPriority(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -1769313434:
                if (string.equals("Богатый")) {
                    return 3;
                }
                break;
            case -957954831:
                if (string.equals("Солидный")) {
                    return 2;
                }
                break;
            case 828965756:
                if (string.equals("Обычный")) {
                    b = 4;
                }
                break;
            case 1048297000:
                if (string.equals("Легендарный")) {
                    return 5;
                }
                break;
            case 1525214739:
                if (string.equals("Элитный")) {
                    return 4;
                }
        }

        switch (b) {
            case 0:
                return 5;
            case 1:
                return 4;
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
                return 1;
            default:
                return 0;
        }
    }

    private static int lootColor(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -1769313434:
                if (string.equals("Богатый")) {
                    return ColorUtil.rgb(90, 190, 255);
                }
                break;
            case -957954831:
                if (string.equals("Солидный")) {
                    b = 3;
                }
                break;
            case 1048297000:
                if (string.equals("Легендарный")) {
                    return ColorUtil.rgb(255, 170, 60);
                }
                break;
            case 1525214739:
                if (string.equals("Элитный")) {
                    return ColorUtil.rgb(190, 120, 255);
                }
        }

        switch (b) {
            case 0:
                return ColorUtil.rgb(255, 170, 60);
            case 1:
                return ColorUtil.rgb(190, 120, 255);
            case 2:
                return ColorUtil.rgb(90, 190, 255);
            case 3:
                return ColorUtil.rgb(110, 220, 140);
            default:
                return Theme.getSecondaryText();
        }
    }

    private static Icon iconForType(String string) {
        String string2 = string == null ? "" : string.toUpperCase(Locale.US);
        byte b = -1;
        switch (string2.hashCode()) {
            case -2113662956:
                if (string2.equals("JAYCOB")) {
                    return Icon.GHOST;
                }
                break;
            case -1929121589:
                if (string2.equals("POSTAL")) {
                    return Icon.ENVELOPE;
                }
                break;
            case -1919329183:
                if (string2.equals("CONTAINER")) {
                    return Icon.BOXES;
                }
                break;
            case -127296097:
                if (string2.equals("SNOWQUARRY")) {
                    return Icon.FLOWER;
                }
                break;
            case -75461270:
                if (string2.equals("PARCELS")) {
                    return Icon.BOX_OPEN;
                }
                break;
            case 2044781:
                if (string2.equals("BOSS")) {
                    return Icon.SKULL_CROSSBONES;
                }
                break;
            case 2079797:
                if (string2.equals("CUBE")) {
                    b = 10;
                }
                break;
            case 2366547:
                if (string2.equals("MINE")) {
                    return Icon.DIGGING;
                }
                break;
            case 2544188:
                if (string2.equals("SHIP")) {
                    return Icon.SHIP;
                }
                break;
            case 63893404:
                if (string2.equals("CARGO")) {
                    return Icon.TRUCK;
                }
                break;
            case 1511873940:
                if (string2.equals("GOLDEN_FORTRESS")) {
                    return Icon.CROWN;
                }
        }

        switch (b) {
            case 0:
                return Icon.SKULL_CROSSBONES;
            case 1:
                return Icon.TRUCK;
            case 2:
                return Icon.BOXES;
            case 3:
                return Icon.BOX_OPEN;
            case 4:
                return Icon.ENVELOPE;
            case 5:
                return Icon.CROWN;
            case 6:
                return Icon.GHOST;
            case 7:
                return Icon.DIGGING;
            case 8:
                return Icon.FLOWER;
            case 9:
                return Icon.SHIP;
            case 10:
                return Icon.CUBE;
            default:
                return Icon.STAR;
        }
    }

    private static String cardKey(EventsPage.EventCard eventCard) {
        return eventCard.anarchy() + "|" + eventCard.name();
    }

    private String ellipsize(String string, float f, float f2) {
        if (!(f <= 0.0F) && !(Fonts.INTER_MEDIUM.getWidth(string, f2) <= f)) {
            StringBuilder builder = new StringBuilder();
            int i = 0;

            for (int i2 = string.length(); i < i2 && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", f2) > f); i++) {
                builder.append(string.charAt(i));
            }

            return builder + "…";
        } else {
            return string;
        }
    }

    private float getOpenAlpha() {
        return (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
    }

    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (this.isRightButton(click.button())) {
            for (EventsPage.CardBounds cardBounds2 : this.cardBounds) {
                if (this.isHovered(d, d2, cardBounds2.x(), cardBounds2.y(), cardBounds2.w(), 34.0) && cardBounds2.card().located()) {
                    this.copyCoordinates(cardBounds2.card());
                    return true;
                }
            }
        } else {
            if (!this.isLeftButton(click.button())) {
                return false;
            }

            if (this.scrollbar.mouseClicked(d, d2)) {
                return true;
            }

            if (this.isHovered(d, d2, this.filterButtonX, this.tabsY, 15.0, 15.0)) {
                Events events = Events.getInstance();
                if (events != null) {
                    this.clickGui.toggleSettingsPopup(events, this.filterButtonX + 15.0F - 150.0F, this.tabsY + 15.0F + 4.0F);
                }

                return true;
            }

            if (this.isHovered(d, d2, this.tabsX, this.tabsY, this.tabsWidth, 15.0)) {
                EventsPage.EventSource[] eventSources = EventsPage.EventSource.values();
                int i = (int)((d - this.tabsX) / (this.tabsWidth / eventSources.length));
                this.source = eventSources[Math.clamp(i, 0, eventSources.length - 1)];
                this.scrollbar.reset();
                return true;
            }

            for (EventsPage.CardBounds cardBounds3 : this.cardBounds) {
                if (this.isHovered(d, d2, cardBounds3.x(), cardBounds3.y(), cardBounds3.w(), 34.0)) {
                    this.joinEvent(cardBounds3.card());
                    return true;
                }
            }
        }

        return false;
    }

    private void joinEvent(EventsPage.EventCard eventCard) {
        if (eventCard.anarchy() > 0) {
            if (eventCard.located()) {
                WaypointRenderer.addTimed(eventCard.name(), eventCard.icon(), eventCard.anarchy(), eventCard.x(), eventCard.y(), eventCard.z());
                NotificationUtil.info(
                    NotificationSources.events(), Language.translate("events.page.marked", "Метка поставлена") + ": " + eventCard.x() + ", " + eventCard.z()
                );
            }

            if (this.source == EventsPage.EventSource.HOLY) {
                if (AutoReconnectComponent.getInstance().start(eventCard.anarchy(), stripServerSuffix(eventCard.server()))) {
                    mc.setScreen(null);
                }
            } else if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendChatCommand("an" + eventCard.anarchy());
                mc.setScreen(null);
            }
        }
    }

    private void copyCoordinates(EventsPage.EventCard eventCard) {
        String string = eventCard.x() + " " + eventCard.y() + " " + eventCard.z();
        mc.keyboard.setClipboard(string);
        NotificationUtil.success(NotificationSources.events(), Language.translate("events.page.copied", "Координаты скопированы") + ": " + string);
    }

    private static String stripServerSuffix(String string) {
        int i = string.indexOf(35);
        return i > 0 ? string.substring(0, i).trim() : string.trim();
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
            return false;
        }

        this.scrollbar.onScroll(d4);
        return true;
    }

    public boolean mouseReleased(Click click) {
        this.scrollbar.mouseReleased();
        return false;
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        return this.scrollbar.isDragging() && this.scrollbar.mouseDragged(click.x(), click.y());
    }

    public void mouseMoved(double d, double d2) {
    }

    public boolean keyPressed(KeyInput keyInput) {
        return false;
    }

    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    public boolean charTyped(CharInput charInput) {
        return false;
    }

    record CardBounds(EventsPage.EventCard card, float x, float y, float w) {
    }

    record EventCard(String name, String server, int anarchy, String time, int seconds, String loot, Icon icon, boolean located, int x, int y, int z, int count) {
        EventsPage.EventGroup group() {
            return EventsPage.EventGroup.fromAnarchy(this.anarchy);
        }
    }

    enum EventGroup {
        ALL(Icon.USERS, "events.page.group.all", "Все"),
        SOLO(Icon.USER, "events.page.group.solo", "Соло"),
        DUO(Icon.USER_GROUP, "events.page.group.duo", "Дуо"),
        TRIO(Icon.USERS, "events.page.group.trio", "Трио"),
        CLAN5(Icon.PEOPLE_GROUP, "events.page.group.clan5", "Клан 5"),
        CLAN10(Icon.PEOPLE_GROUP, "events.page.group.clan10", "Клан 10");

        final Icon icon;
        private final String translationKey;
        private final String fallback;

        EventGroup(Icon icon2, String string, String string2) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallback = string2;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.fallback);
        }

        private EventsPage.EventGroup next() {
            EventsPage.EventGroup[] eventGroups = values();
            return eventGroups[(this.ordinal() + 1) % eventGroups.length];
        }

        static EventsPage.EventGroup fromAnarchy(int i) {
            switch (String.valueOf(i).charAt(0)) {
                case '2':
                    return DUO;
                case '3':
                    return TRIO;
                case '4':
                case '6':
                case '7':
                case '8':
                default:
                    return SOLO;
                case '5':
                    return CLAN5;
                case '9':
                    return CLAN10;
            }
        }
    }

    enum EventSource {
        HOLY("events.page.holy", "HolyWorld"),
        FUNTIME("events.page.funtime", "FunTime");

        private final String translationKey;
        private final String fallback;

        EventSource(String string, String string2) {
            this.translationKey = string;
            this.fallback = string2;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.fallback);
        }
    }
}
