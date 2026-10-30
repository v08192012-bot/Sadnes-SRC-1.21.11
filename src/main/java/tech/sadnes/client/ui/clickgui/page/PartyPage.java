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
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.StaggeredAppearAnimation;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.party.PartyPrefs;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class PartyPage implements IInteractable, IRenderable, IMinecraft {
    private static final float HEADER_HEIGHT = 40.0F;
    private static final float SECTION_TITLE_HEIGHT = 13.0F;
    private static final float BUTTON_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 21.0F;
    private static final float ROW_GAP = 3.0F;
    private static final float PADDING = 8.0F;
    private static final float SEARCH_HEIGHT = 15.0F;
    private static final float AVATAR_SIZE = 14.0F;
    private static final float ROW_TEXT_OFFSET = 22.0F;
    private static final float FADE_HEIGHT = 12.0F;
    private static final float MENU_ITEM_HEIGHT = 16.0F;
    private static final float MENU_PADDING = 4.5F;
    private static final float MENU_MIN_WIDTH = 84.0F;
    private final ClickGuiScreen clickGui;
    private final StaggeredAppearAnimation appearAnimation = new StaggeredAppearAnimation();
    private final SmoothScrollbar membersScroll = new SmoothScrollbar();
    private final SmoothScrollbar onlineScroll = new SmoothScrollbar();
    private final TextFieldComponent searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "party.page.search", false, false);
    private final Map<String, Animation> hoverAnimations = new HashMap<>();
    private final List<PartyPage.ClickArea> clickAreas = new ArrayList<>();
    private final List<PartyPage.OnlineRow> onlineRows = new ArrayList<>();
    private float membersX;
    private float onlineX;
    private float listsY;
    private float columnWidth;
    private float listsHeight;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float refreshX;
    private String menuTarget;
    private boolean menuOpen;
    private float menuAnchorX;
    private float menuAnchorY;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private final Animation menuAnimation = new Animation();

    public PartyPage(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;
    }

    public void onOpen() {
        this.appearAnimation.reset();
        this.membersScroll.reset();
        this.onlineScroll.reset();
        this.searchField.setFocused(false);
        this.closeMenu();
        this.hoverAnimations.clear();
    }

    public void renderPage(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.appearAnimation.update();
        this.clickAreas.clear();
        this.onlineRows.clear();
        float f6 = this.getGuiAlpha();
        BackendService backendService = Sadnes.getInstance().getBackend();
        if (backendService != null && backendService.isConnected()) {
            PartyManager partyManager = backendService.getPartyManager();
            this.renderHeader(drawContext, partyManager, i, i2, f2, f3 + this.appearAnimation.getOffset(0), f4, f6 * this.appearAnimation.getProgress(0));
            this.columnWidth = (f4 - 8.0F) / 2.0F;
            this.membersX = f2;
            this.onlineX = f2 + this.columnWidth + 8.0F;
            this.listsY = f3 + 40.0F + 8.0F;
            this.listsHeight = f3 + f5 - this.listsY;
            if (!(this.listsHeight <= 13.0F)) {
                this.renderMembers(drawContext, partyManager, i, i2, f6);
                this.renderOnline(drawContext, partyManager, i, i2, f6);
                this.renderMenu(drawContext, i, i2, f6);
            }
        } else {
            this.renderOfflineState(drawContext, f2, f3, f4, f5, Icon.LINK_SLASH, Language.translate("party.page.offline", "Нет соединения с сервером"), f6);
        }
    }

    private void renderHeader(DrawContext drawContext, PartyManager partyManager, int i, int i2, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 40.0F, f4 * 0.7F, 8.0F);
        boolean flag = partyManager.inParty();
        String string = flag ? Language.translate("party.page.title", "Пати") : Language.translate("party.page.none", "Вы не в пати");
        String string2 = flag
            ? String.format(Language.translate("party.page.count", "Участников: %s"), partyManager.getMembers().size())
            : Language.translate("party.page.none.hint", "Создайте свою или примите приглашение");
        new RenderUtil(drawContext, f, f2, f3, 40.0F)
            .icon(Icon.USER_GROUP, 12.0F, 14.0F, 12.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f4))
            .textTopLeft(Fonts.INTER_MEDIUM, string, 30.0F, 12.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), 7.5F)
            .textTopLeft(Fonts.INTER_REGULAR, string2, 30.0F, 21.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f4), 6.0F);
        float f5 = f + f3 - 8.0F;
        if (flag) {
            if (partyManager.isLeader()) {
                f5 -= this.renderHeaderButton(
                        drawContext,
                        "disband",
                        Language.translate("party.page.disband", "Распустить"),
                        Icon.CIRCLE_XMARK,
                        f5,
                        f2,
                        i,
                        i2,
                        f4,
                        false,
                        partyManager::disband
                    )
                    + 5.0F;
            }

            float f6 = f5
                - (
                    this.renderHeaderButton(
                            drawContext, "leave", Language.translate("party.page.leave", "Выйти"), Icon.LINK_SLASH, f5, f2, i, i2, f4, false, partyManager::leave
                        )
                        + 5.0F
                );
            this.renderHeaderButton(drawContext, "code", partyManager.getPartyName(), Icon.COPY, f6, f2, i, i2, f4, true, () -> {
                mc.keyboard.setClipboard(partyManager.getPartyName());
                NotificationUtil.success(NotificationSources.party(), Language.translate("party.page.code.copied", "Код скопирован"));
            });
        } else {
            String string3 = partyManager.getPendingInviter();
            if (string3 != null) {
                f5 -= this.renderHeaderButton(
                        drawContext, "accept", Language.translate("party.page.accept", "Принять"), Icon.CHECK, f5, f2, i, i2, f4, true, partyManager::acceptInvite
                    )
                    + 5.0F;
                new RenderUtil(drawContext, f, f2, f3, 40.0F)
                    .textTopLeft(
                        Fonts.INTER_REGULAR,
                        String.format(Language.translate("party.page.invite.from", "%s зовёт вас"), string3),
                        30.0F,
                        29.0F,
                        ColorUtil.multiplyAlpha(Theme.getPrimary(), f4),
                        6.0F
                    );
            }

            this.renderHeaderButton(
                drawContext, "create", Language.translate("party.page.create", "Создать"), Icon.PLUS, f5, f2, i, i2, f4, string3 == null, partyManager::createParty
            );
        }
    }

    private float renderHeaderButton(
        DrawContext drawContext, String string, String string2, Icon icon2, float f, float f2, int i, int i2, float f3, boolean flag, Runnable runnable
    ) {
        float f4 = Fonts.INTER_MEDIUM.getWidth(string2, 6.5F) + 24.0F;
        float f5 = f - f4;
        float f6 = f2 + 20.0F - 7.5F;
        Animation animation = this.getHoverAnimation(string);
        animation.update();
        animation.animate(this.isHovered(i, i2, f5, f6, f4, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f7 = animation.get();
        if (flag) {
            new RenderUtil(drawContext, f5, f6, f4, 15.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.75F + 0.25F * f7), f3), Radius.all(5.0F));
        } else {
            GuiRenderUtil.drawOutlined(drawContext, f5, f6, f4, 15.0F, f3 * (0.7F + 0.25F * f7), 5.0F);
        }

        int i3 = flag
            ? ColorUtil.multiplyAlpha(Theme.getTextColor(), f3)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.65F + 0.35F * f7), f3);
        new RenderUtil(drawContext, f5, f6, f4, 15.0F).icon(icon2, 7.0F, 4.0F, 7.0F, i3).textMiddleLeft(Fonts.INTER_MEDIUM, string2, 17.0F, 0.0F, i3, 6.5F);
        this.clickAreas.add(new PartyPage.ClickArea(f5, f6, f4, 15.0F, runnable));
        return f4;
    }

    private void renderMembers(DrawContext drawContext, PartyManager partyManager, int i, int i2, float f) {
        this.renderSectionTitle(drawContext, this.membersX, this.listsY, Language.translate("party.page.members", "Состав"), f);
        float f2 = this.listsY + 13.0F;
        float f3 = this.listsHeight - 13.0F;
        List<PartyMemberView> partyMemberViews = partyManager.getMemberViews();
        if (partyMemberViews.isEmpty()) {
            this.renderEmptyState(
                drawContext, this.membersX, f2, this.columnWidth, f3, Icon.USER_GROUP, Language.translate("party.page.empty.members", "Пати пустое"), f
            );
        } else {
            partyMemberViews.sort((partyMemberView, partyMemberView2) -> {
                int i3 = partyMemberView2.uid() == partyManager.getLeaderUid() ? 1 : 0;
                return partyMemberView.uid() == partyManager.getLeaderUid() ? i3 - 1 : i3;
            });
            float f4 = partyMemberViews.size() * 24.0F;
            this.membersScroll.update(f4, f3);
            if (ScissorStack.push(drawContext, this.membersX, f2, this.columnWidth, f3)) {
                try {
                    this.beginFadeMask(f2, f3, f4, this.membersScroll.getScroll());
                    float f5 = f2 + this.membersScroll.getScroll();
                    boolean flag = partyManager.isLeader();
                    int i4 = 0;

                    for (int i5 = partyMemberViews.size(); i4 < i5; i4++) {
                        float f6 = f5 + this.appearAnimation.getOffset(1 + i4);
                        if (f6 + 21.0F >= f2 && f6 <= f2 + f3) {
                            this.renderMember(
                                drawContext, partyManager, (PartyMemberView)partyMemberViews.get(i4), flag, i, i2, f6, f * this.appearAnimation.getProgress(1 + i4)
                            );
                        }

                        f5 += 24.0F;
                    }
                } finally {
                    VerticalFadeMask.end();
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderMember(DrawContext drawContext, PartyManager partyManager, PartyMemberView partyMemberView, boolean flag, int i, int i2, float f, float f2) {
        boolean flag2 = partyMemberView.uid() == partyManager.getLeaderUid();
        boolean flag3;
        if (flag) {
            flag3 = !flag2;
        } else {
            flag3 = false;
        }

        Animation animation = this.getHoverAnimation("m:" + partyMemberView.uid());
        animation.update();
        animation.animate(this.isHovered(i, i2, this.membersX, f, this.columnWidth, 21.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f3 = animation.get();
        GuiRenderUtil.drawOutlined(drawContext, this.membersX, f, this.columnWidth, 21.0F, f2 * (0.6F + 0.2F * f3), 6.0F);
        float f4 = partyMemberView.online() ? 1.0F : 0.55F;
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), f4), f2);
        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2);
        String string = partyMemberView.hasState()
            ? partyMemberView.state().getUsername() + " · " + partyMemberView.state().getServer()
            : Language.translate(partyMemberView.online() ? "party.page.state.idle" : "party.page.state.offline", "");
        this.renderAvatar(drawContext, partyManager, partyMemberView.username(), this.membersX + 4.0F, f + 10.5F - 7.0F, f2 * f4);
        float f5 = this.columnWidth - 22.0F - (flag3 ? 22.0F : 8.0F);
        String string2 = this.truncate(partyMemberView.username(), f5 - (flag2 ? 9.0F : 0.0F), 6.5F);
        new RenderUtil(drawContext, this.membersX, f, this.columnWidth, 21.0F)
            .textTopLeft(Fonts.INTER_MEDIUM, string2, 22.0F, 4.0F, i3, 6.5F)
            .textTopLeft(Fonts.INTER_REGULAR, this.truncate(string, f5, 5.5F), 22.0F, 12.0F, i4, 5.5F);
        if (flag2) {
            new RenderUtil(drawContext, this.membersX, f, this.columnWidth, 21.0F)
                .icon(Icon.CROWN, 22.0F + Fonts.INTER_MEDIUM.getWidth(string2, 6.5F) + 3.0F, 4.0F, 6.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f2));
        }

        if (flag3) {
            float f6 = this.membersX + this.columnWidth - 18.0F;
            float f7 = f + 10.5F - 5.0F;
            new RenderUtil(drawContext, f6, f7, 10.0F, 10.0F)
                .icon(Icon.USER_MINUS, 1.0F, 1.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F + 0.5F * f3), f2));
            this.clickAreas.add(new PartyPage.ClickArea(f6 - 2.0F, f7 - 2.0F, 14.0F, 14.0F, () -> partyManager.kick(partyMemberView.uid())));
        }
    }

    private void renderOnline(DrawContext drawContext, PartyManager partyManager, int i, int i2, float f) {
        this.renderSectionTitle(drawContext, this.onlineX, this.listsY, Language.translate("party.page.online", "В сети"), f);
        this.renderSearchBar(drawContext, partyManager, i, i2, f);
        float f2 = this.listsY + 13.0F + 15.0F + 4.0F;
        float f3 = this.listsY + this.listsHeight - f2;
        if (!(f3 <= 0.0F)) {
            ArrayList arrayList = new ArrayList<>(partyManager.suggestInvitees(this.searchField.getText().trim().toLowerCase(Locale.ROOT)));
            arrayList.sort(Comparator.comparingInt(PartyPage::onlineSortOrder));
            if (arrayList.isEmpty()) {
                this.renderEmptyState(
                    drawContext, this.onlineX, f2, this.columnWidth, f3, Icon.USERS, Language.translate("party.page.empty.online", "Никого нет в сети"), f
                );
            } else {
                float f4 = arrayList.size() * 24.0F;
                this.onlineScroll.update(f4, f3);
                if (ScissorStack.push(drawContext, this.onlineX, f2, this.columnWidth, f3)) {
                    try {
                        this.beginFadeMask(f2, f3, f4, this.onlineScroll.getScroll());
                        float f5 = f2 + this.onlineScroll.getScroll();
                        int i3 = 0;

                        for (int i4 = arrayList.size(); i3 < i4; i3++) {
                            if (f5 + 21.0F >= f2 && f5 <= f2 + f3) {
                                this.renderOnlineUser(drawContext, partyManager, (String)arrayList.get(i3), i, i2, f5, f);
                            }

                            f5 += 24.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private void renderOnlineUser(DrawContext drawContext, PartyManager partyManager, String string, int i, int i2, float f, float f2) {
        Animation animation = this.getHoverAnimation("o:" + string);
        animation.update();
        animation.animate(this.isHovered(i, i2, this.onlineX, f, this.columnWidth, 21.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f3 = animation.get();
        boolean flag = PartyPrefs.isIgnored(string);
        boolean flag2 = PartyPrefs.isPinned(string);
        float f4 = flag ? f2 * 0.45F : f2;
        this.onlineRows.add(new PartyPage.OnlineRow(string, f));
        GuiRenderUtil.drawOutlined(drawContext, this.onlineX, f, this.columnWidth, 21.0F, f4 * (0.6F + 0.2F * f3), 6.0F);
        this.renderAvatar(drawContext, partyManager, string, this.onlineX + 4.0F, f + 10.5F - 7.0F, f4);
        String string2 = this.truncate(string, this.columnWidth - 22.0F - (flag2 ? 30.0F : 20.0F), 6.5F);
        RenderUtil renderUtil = new RenderUtil(drawContext, this.onlineX, f, this.columnWidth, 21.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string2, 22.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), 6.5F)
            .icon(
                flag ? Icon.USER_SLASH : Icon.USER_PLUS,
                this.columnWidth - 18.0F,
                6.5F,
                8.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.4F + 0.6F * f3), f4)
            );
        if (flag2) {
            renderUtil.icon(
                Icon.THUMBTACK,
                22.0F + Fonts.INTER_MEDIUM.getWidth(string2, 6.5F) + 3.0F,
                7.5F,
                6.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.8F), f4)
            );
        }

        if (!flag) {
            this.clickAreas.add(new PartyPage.ClickArea(this.onlineX, f, this.columnWidth, 21.0F, () -> {
                partyManager.invite(string);
                NotificationUtil.info(NotificationSources.party(), String.format(Language.translate("party.page.invited", "Приглашение отправлено: %s"), string));
            }));
        }
    }

    private static int onlineSortOrder(String string) {
        if (PartyPrefs.isPinned(string)) {
            return 0;
        } else {
            return PartyPrefs.isIgnored(string) ? 2 : 1;
        }
    }

    private void renderSearchBar(DrawContext drawContext, PartyManager partyManager, int i, int i2, float f) {
        this.searchX = this.onlineX;
        this.searchY = this.listsY + 13.0F;
        this.refreshX = this.onlineX + this.columnWidth - 15.0F;
        this.searchWidth = this.columnWidth - 15.0F - 4.0F;
        Animation animation = this.getHoverAnimation("search");
        animation.update();
        animation.animate(
            !this.isHovered(i, i2, this.searchX, this.searchY, this.searchWidth, 15.0) && !this.searchField.isFocused() ? 0.0 : 1.0,
            0.25,
            Easings.QUAD_OUT
        );
        GuiRenderUtil.drawOutlined(drawContext, this.searchX, this.searchY, this.searchWidth, 15.0F, f * (0.7F + 0.2F * animation.get()), 5.0F)
            .icon(Icon.SEARCH, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f));
        this.searchField
            .render(
                drawContext,
                new Vector2f(this.searchX + 14.0F, this.searchY + 7.5F - 3.0F),
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f),
                this.searchWidth - 17.0F
            );
        Animation animation2 = this.getHoverAnimation("refresh");
        animation2.update();
        animation2.animate(this.isHovered(i, i2, this.refreshX, this.searchY, 15.0, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f2 = animation2.get();
        GuiRenderUtil.drawOutlined(drawContext, this.refreshX, this.searchY, 15.0F, 15.0F, f * (0.7F + 0.25F * f2), 5.0F)
            .icon(Icon.REFRESH, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F + 0.4F * f2), f));
        this.clickAreas.add(new PartyPage.ClickArea(this.refreshX, this.searchY, 15.0F, 15.0F, partyManager::refreshSearch));
    }

    private void renderAvatar(DrawContext drawContext, PartyManager partyManager, String string, float f, float f2, float f3) {
        Radius radius = Radius.all(7.0F);
        String string2 = partyManager.getAvatarUrl(string);
        if (!string2.isEmpty()) {
            Identifier identifier = AvatarTextureLoader.getTexture(string2);
            if (identifier != null) {
                GuiRenderUtil.bindTexture(identifier);
                if (GuiRenderUtil.getBoundTexture() != null) {
                    new RenderUtil(drawContext, f, f2, 14.0F, 14.0F).frostedGlass(f3, radius);
                    return;
                }
            }
        }

        int i = ColorUtil.fromHsb((string.hashCode() & 65535) / 65535.0F, 0.5F, 0.7F);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, 14.0F, 14.0F).rect(Theme.withAlpha(i, 0.9F * f3), radius);
        MsdfFont msdfFont = Fonts.INTER_SEMIBOLD;
        if (string.isEmpty()) {
            renderUtil.textCenter(msdfFont, "?", Theme.withAlpha(ColorUtil.WHITE, f3), 5.8799996F);
        } else {
            renderUtil.textCenter(msdfFont, string.substring(0, 1).toUpperCase(Locale.ROOT), Theme.withAlpha(ColorUtil.WHITE, f3), 5.8799996F);
        }
    }

    private List<PartyPage.MenuItem> buildMenuItems(String string) {
        boolean flag = Sadnes.getInstance().getFriendManager().isFriend(string);
        boolean flag2 = PartyPrefs.isIgnored(string);
        boolean flag3 = PartyPrefs.isPinned(string);
        ArrayList arrayList = new ArrayList(3);
        arrayList.add(
            new PartyPage.MenuItem(
                flag ? Icon.USER_MINUS : Icon.USER_PLUS,
                Language.translate(flag ? "party.page.menu.friend.remove" : "party.page.menu.friend.add", ""),
                false,
                () -> {
                    if (flag) {
                        Sadnes.getInstance().getFriendManager().removeFriend(string);
                    } else {
                        Sadnes.getInstance().getFriendManager().addFriend(string);
                    }
                }
            )
        );
        arrayList.add(
            new PartyPage.MenuItem(
                Icon.THUMBTACK, Language.translate(flag3 ? "party.page.menu.unpin" : "party.page.menu.pin", ""), false, () -> PartyPrefs.togglePinned(string)
            )
        );
        arrayList.add(
            new PartyPage.MenuItem(
                Icon.USER_SLASH,
                Language.translate(flag2 ? "party.page.menu.unignore" : "party.page.menu.ignore", ""),
                !flag2,
                () -> PartyPrefs.toggleIgnored(string)
            )
        );
        return arrayList;
    }

    private void openMenu(String string, double d, double d2) {
        this.menuTarget = string;
        this.menuOpen = true;
        this.menuAnchorX = (float)d;
        this.menuAnchorY = (float)d2;
        this.menuAnimation.forceValue(0.0);
    }

    private void closeMenu() {
        this.menuOpen = false;
    }

    private void renderMenu(DrawContext drawContext, int i, int i2, float f) {
        this.menuAnimation.update();
        if (this.menuOpen) {
            this.menuAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.menuAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }

        if (this.menuTarget != null) {
            if (!this.menuOpen && this.menuAnimation.get() <= 0.001F) {
                this.menuTarget = null;
            } else {
                List<PartyPage.MenuItem> menuItems = this.buildMenuItems(this.menuTarget);
                float f2 = f * Math.min(this.menuAnimation.get(), 1.0F);
                this.menuWidth = 84.0F;

                for (PartyPage.MenuItem menuItem : menuItems) {
                    this.menuWidth = Math.max(this.menuWidth, Fonts.INTER_MEDIUM.getWidth(menuItem.label(), 5.5F) + 16.0F + 9.0F);
                }

                this.menuHeight = menuItems.size() * 16.0F + 9.0F;
                this.menuX = GuiRenderUtil.clampToScreenX(this.menuAnchorX, this.menuWidth);
                this.menuY = GuiRenderUtil.clampToScreenY(this.menuAnchorY, this.menuHeight);
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, this.menuX + this.menuWidth / 2.0F, this.menuY + this.menuHeight / 2.0F, 0.85F + 0.15F * this.menuAnimation.get());
                GuiRenderUtil.drawPopup(drawContext, this.menuX, this.menuY, this.menuWidth, this.menuHeight, f2, 10.0F);
                float f3 = this.menuWidth - 9.0F;
                int i3 = 0;

                for (int i4 = menuItems.size(); i3 < i4; i3++) {
                    PartyPage.MenuItem menuItem2 = (PartyPage.MenuItem)menuItems.get(i3);
                    float f4 = this.menuX + 4.5F;
                    float f5 = this.menuY + 4.5F + i3 * 16.0F;
                    Animation animation = this.getHoverAnimation("menu:" + i3);
                    animation.update();
                    animation.animate(this.menuOpen && this.isHovered(i, i2, f4, f5, f3, 16.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
                    GuiRenderUtil.drawHighlight(drawContext, f4, f5, f3, 16.0F, f2 * animation.get(), Radius.all(4.0F));
                    int i5 = menuItem2.danger() ? Theme.withAlpha(ColorUtil.rgb(255, 86, 86), f2) : ColorUtil.multiplyAlpha(Theme.getTextColor(), f2);
                    int i6 = menuItem2.danger() ? Theme.withAlpha(ColorUtil.rgb(255, 120, 120), f2) : ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f2);
                    new RenderUtil(drawContext, f4, f5, f3, 16.0F)
                        .icon(menuItem2.icon(), 3.0F, 3.75F, 8.5F, i6)
                        .textMiddleLeft(Fonts.INTER_MEDIUM, menuItem2.label(), 16.0F, 0.0F, i5, 5.5F);
                }

                MatrixUtil.pop(drawContext);
            }
        }
    }

    private boolean clickMenu(double d, double d2) {
        List<PartyPage.MenuItem> menuItems = this.buildMenuItems(this.menuTarget);
        if (this.isHovered(d, d2, this.menuX, this.menuY, this.menuWidth, this.menuHeight)) {
            int i = (int)((d2 - this.menuY - 4.5) / 16.0);
            if (i >= 0 && i < menuItems.size()) {
                ((PartyPage.MenuItem)menuItems.get(i)).action().run();
            }
        }

        this.closeMenu();
        return true;
    }

    private String findOnlineUserAt(double d, double d2) {
        for (PartyPage.OnlineRow onlineRow : this.onlineRows) {
            if (this.isHovered(d, d2, this.onlineX, onlineRow.y(), this.columnWidth, 21.0)) {
                return onlineRow.login();
            }
        }

        return null;
    }

    private void renderSectionTitle(DrawContext drawContext, float f, float f2, String string, float f3) {
        new RenderUtil(drawContext, f, f2, this.columnWidth, 13.0F)
            .textTopLeft(Fonts.INTER_MEDIUM, string, 2.0F, 1.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f3), 6.0F);
    }

    private void renderEmptyState(DrawContext drawContext, float f, float f2, float f3, float f4, Icon icon2, String string, float f5) {
        new RenderUtil(drawContext, f, f2, f3, f4)
            .icon(icon2, f3 / 2.0F - 9.0F, f4 / 2.0F - 19.0F, 18.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.22F), f5))
            .textCenter(Fonts.INTER_REGULAR, string, 0.0F, 6.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.45F), f5), 6.0F);
    }

    private void renderOfflineState(DrawContext drawContext, float f, float f2, float f3, float f4, Icon icon2, String string, float f5) {
        new RenderUtil(drawContext, f, f2, f3, f4)
            .icon(icon2, f3 / 2.0F - 9.0F, f4 / 2.0F - 22.0F, 18.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.3F), f5))
            .textCenter(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.7F), f5), 7.0F);
    }

    private void beginFadeMask(float f, float f2, float f3, float f4) {
        float f5 = f3 - f2;
        if (!(f5 <= 0.5F) && !(f2 <= 24.0F)) {
            float f6 = Math.clamp(-f4 / 12.0F, 0.0F, 1.0F);
            float f7 = Math.clamp((f5 + f4) / 12.0F, 0.0F, 1.0F);
            if (!(f6 <= 0.001F) || !(f7 <= 0.001F)) {
                VerticalFadeMask.begin(f, f + 12.0F, f6, f + f2 - 12.0F, f + f2, f7);
            }
        }
    }

    private Animation getHoverAnimation(String string) {
        return this.hoverAnimations.computeIfAbsent(string, string2 -> new Animation());
    }

    private String truncate(String string, float f, float f2) {
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

    private float getGuiAlpha() {
        return (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
    }

    public boolean handleMouseClick(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (this.menuTarget != null && this.menuOpen) {
            return this.clickMenu(d, d2);
        }

        if (this.isRightButton(click.button())) {
            String string = this.findOnlineUserAt(d, d2);
            if (string == null) {
                return false;
            }

            this.openMenu(string, d, d2);
            return true;
        } else {
            if (!this.isLeftButton(click.button())) {
                return false;
            }

            if (this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, 15.0)) {
                this.searchField.setFocused(true);
                this.searchField.mouseClicked(d, d2, click.button());
                return true;
            }

            this.searchField.setFocused(false);

            for (PartyPage.ClickArea clickArea : this.clickAreas) {
                if (this.isHovered(d, d2, clickArea.x(), clickArea.y(), clickArea.w(), clickArea.h())) {
                    clickArea.action().run();
                    return true;
                }
            }

            return false;
        }
    }

    public boolean handleMouseScroll(double d, double d2, double d3, double d4) {
        if (this.menuOpen) {
            this.closeMenu();
        }

        if (this.isHovered(d, d2, this.membersX, this.listsY, this.columnWidth, this.listsHeight)) {
            this.membersScroll.onScroll(d4);
            return true;
        } else if (this.isHovered(d, d2, this.onlineX, this.listsY, this.columnWidth, this.listsHeight)) {
            this.onlineScroll.onScroll(d4);
            return true;
        } else {
            return false;
        }
    }

    public boolean handleMouseRelease(Click click) {
        this.membersScroll.mouseReleased();
        this.onlineScroll.mouseReleased();
        return false;
    }

    public boolean handleKeyPress(KeyInput keyInput) {
        if (this.menuOpen && keyInput.key() == 256) {
            this.closeMenu();
            return true;
        } else if (!this.searchField.isFocused()) {
            return false;
        } else if (keyInput.key() != 256 && keyInput.key() != 257) {
            this.searchField.keyPressed(keyInput.key());
            return true;
        } else {
            this.searchField.setFocused(false);
            return true;
        }
    }

    public boolean handleCharTyped(CharInput charInput) {
        if (!this.searchField.isFocused()) {
            return false;
        }

        this.searchField.charTyped(charInput.codepoint());
        return true;
    }

    record ClickArea(float x, float y, float w, float h, Runnable action) {
    }

    record MenuItem(Icon icon, String label, boolean danger, Runnable action) {
    }

    record OnlineRow(String login, float y) {
    }
}
