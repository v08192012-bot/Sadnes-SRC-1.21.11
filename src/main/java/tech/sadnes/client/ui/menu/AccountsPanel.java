package tech.sadnes.client.ui.menu;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.account.Account;
import tech.sadnes.client.account.AccountManager;
import tech.sadnes.client.account.MicrosoftAuthenticator;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.PlayerHeadCache;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.NickGeneratorUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class AccountsPanel implements IRenderable, IMinecraft {
    private static final float ROW_HEIGHT = 34.0F;
    private static final float ROW_SPACING = 6.0F;
    private static final float PADDING = 16.0F;
    private static final float HEADER_HEIGHT = 14.0F;
    private static final float ACTION_ICON_SIZE = 16.0F;
    private static final float AVATAR_SIZE = 12.0F;
    private static final float HEADER_RESERVED_HEIGHT = 70.0F;
    private static final float FOOTER_RESERVED_HEIGHT = 48.0F;
    private static final int FAVORITE_COLOR = -14020;
    private static final int DANGER_COLOR = -38037;
    private static final float SHELF_ICON_SIZE = 14.0F;
    private static final float BUTTON_GAP = 4.0F;
    private static final float ICON_RADIUS = 6.0F;
    private static final float BADGE_AVATAR_SIZE = 10.5F;
    private final MainMenuScreen menuScreen;
    private final MicrosoftAuthenticator microsoftAuth = new MicrosoftAuthenticator();
    private final MenuButton copyCodeButton;
    private final MenuButton openPageButton;
    private final MenuButton cancelButton;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float usernameFieldX;
    private float usernameFieldY;
    private float usernameFieldWidth;
    private float usernameFieldHeight;
    private float addButtonX;
    private float randomButtonX;
    private float fileLoginButtonX;
    private float microsoftButtonX;
    private float actionButtonsY;
    private float actionButtonWidth;
    private float actionButtonHeight;
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;
    private float rowWidth;
    private String usernameInput = "";
    private boolean usernameFocused;
    private boolean usernameSelectedAll;
    private String searchQuery = "";
    private boolean searchFocused;
    private boolean searchSelectedAll;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float searchHeight;
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private long appearTime;
    private long badgeLastNanos;
    private float badgeSquash;
    private float badgeSquashVelocity;
    private String badgeName = "";
    private float badgeX;
    private float badgeY;
    private float badgeWidth;
    private float badgeHeight;
    private float shelfX;
    private float shelfY;
    private float shelfWidth;
    private int visibleShelfCount;
    private float closeButtonX;
    private float closeButtonY;
    private float closeButtonSize;
    private float clearButtonX;
    private float clearButtonY;
    private float clearButtonSize;
    private boolean overflowPopupOpen;
    private final Animation overflowPopupAnimation = new Animation();
    private float overflowPopupX;
    private float overflowPopupY;
    private float overflowPopupWidth;
    private float overflowPopupHeight;
    private float overflowScroll;
    private float overflowMaxScroll;
    private float overflowButtonX;
    private float overflowButtonY;
    private float overflowButtonSize;
    private final List<float[]> overflowRowBounds = new ArrayList<>();
    private final List<Account> overflowRowAccounts = new ArrayList<>();
    private boolean clearConfirmOpen;
    private final Animation clearConfirmAnimation = new Animation();
    private float confirmDialogX;
    private float confirmDialogY;
    private float confirmDialogWidth;
    private float confirmDialogHeight;
    private float confirmButtonX;
    private float confirmButtonY;
    private float confirmButtonWidth;
    private float confirmButtonHeight;
    private float dismissButtonX;
    private float dismissButtonY;
    private float dismissButtonWidth;
    private float dismissButtonHeight;
    private final ShimmerText confirmTextEffect = new ShimmerText();
    private final ShimmerText dismissTextEffect = new ShimmerText();
    private final Animation searchIconAnimation = new Animation();
    private final Animation addHoverAnimation = new Animation();
    private final Animation randomHoverAnimation = new Animation();
    private final Animation fileHoverAnimation = new Animation();
    private final Animation microsoftHoverAnimation = new Animation();
    private float diceRotation;
    private long diceLastTime;
    private final IconHoverAnimator starIcon = new IconHoverAnimator();
    private final IconHoverAnimator trashIcon = new IconHoverAnimator();
    private final IconHoverAnimator clearIcon = new IconHoverAnimator();
    private final IconHoverAnimator closeIcon = new IconHoverAnimator();
    private final HashMap<String, Float> shelfHoverProgress = new HashMap<>();
    private String tooltipText;
    private float tooltipX;
    private float tooltipY;
    private volatile Account pendingLoginAccount;
    private volatile boolean loginInProgress;
    private Account highlightedAccount;
    private final Animation highlightAnimation = new Animation();
    private final ShimmerText activeNameEffect = new ShimmerText();
    private final List<AccountsPanel.Particle> particles = new ArrayList<>();
    private long particlesLastNanos;
    private Account removingAccount;
    private long removeStartTime;
    private static final long REMOVE_ANIMATION_MS = 380L;

    public AccountsPanel(MainMenuScreen mainMenuScreen) {
        this.menuScreen = mainMenuScreen;
        this.copyCodeButton = new MenuButton(MenuButton.Style.GHOST, Icon.COPY, "Copy", this::copyUserCode).withChevron(false);
        this.openPageButton = new MenuButton(MenuButton.Style.PRIMARY, Icon.LINK, "Open", this::openVerificationPage).withChevron(false);
        this.cancelButton = new MenuButton(MenuButton.Style.GHOST, Icon.CLOSE, "Cancel", this::cancelMicrosoftLogin).withChevron(false);
    }

    public void onOpen() {
        this.usernameFocused = false;
        this.searchFocused = false;
        this.searchQuery = "";
        this.scroll.reset();
        this.badgeName = "";
        this.badgeSquash = 0.0F;
        this.badgeSquashVelocity = 0.0F;
        this.badgeLastNanos = 0L;
        this.clearConfirmOpen = false;
        this.overflowPopupOpen = false;
        this.overflowPopupAnimation.forceValue(0.0);
        this.appearTime = System.currentTimeMillis();
    }

    public void resetAppearTime() {
        this.appearTime = System.currentTimeMillis();
    }

    public float computePanelHeight(float f) {
        float f2 = f - 70.0F - 48.0F - 24.0F;
        int i = Math.max(1, (int)Math.floor((f2 + 6.0F) / 40.0F));
        return 94.0F + (i * 40.0F - 6.0F) + 48.0F;
    }

    public void setBounds(float f, float f2, float f3, float f4) {
        this.panelX = f;
        this.panelY = f2;
        this.panelWidth = f3;
        this.panelHeight = f4;
    }

    public float[] getUsernameFieldBounds() {
        return new float[]{
            this.usernameFieldX, this.usernameFieldY, this.microsoftButtonX + this.actionButtonWidth - this.usernameFieldX, this.usernameFieldHeight
        };
    }

    public float[] getFavoriteButtonBounds() {
        float[] floats = this.findFirstVisibleRowY();
        if (floats == null) {
            return null;
        }

        float f = this.listX + this.rowWidth - 16.0F - 8.0F;
        return new float[]{f - 14.0F - 6.0F, floats[0] + 10.0F, 14.0F, 14.0F};
    }

    public float[] getDeleteButtonBounds() {
        float[] floats = this.findFirstVisibleRowY();
        return floats == null ? null : new float[]{this.listX + this.rowWidth - 16.0F - 8.0F, floats[0] + 9.0F, 16.0F, 16.0F};
    }

    public float[] getCloseButtonBounds() {
        return new float[]{this.closeButtonX, this.closeButtonY, this.closeButtonSize, this.closeButtonSize};
    }

    private float[] findFirstVisibleRowY() {
        int i = this.getFilteredAccounts().size();

        for (int i2 = 0; i2 < i; i2++) {
            float f = this.getRowY(i2);
            if (f >= this.listY && f + 34.0F <= this.listY + this.listHeight) {
                return new float[]{f};
            }
        }

        return null;
    }

    private AccountManager getAccountManager() {
        return Sadnes.getInstance().getAccountManager();
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2) {
        this.pollLoginResults();
        if (this.removingAccount != null && System.currentTimeMillis() - this.removeStartTime >= 380L) {
            this.getAccountManager().remove(this.removingAccount);
            this.removingAccount = null;
        }

        float f3 = f * f2;
        this.tooltipText = null;
        int i3;
        int i4;
        if (!this.isMicrosoftDialogOpen() && !this.clearConfirmOpen) {
            i3 = i;
            i4 = i2;
        } else {
            i3 = -99999;
            i4 = -99999;
        }

        int i5 = i4;
        int i6 = this.overflowPopupOpen ? -99999 : i3;
        int i7 = this.overflowPopupOpen ? -99999 : i5;
        float f4 = this.panelY;
        float f5 = this.panelHeight;
        new RenderUtil(drawContext, this.panelX, f4, this.panelWidth, f5).border(74.0F, ColorUtil.BLACK, 0.16F * f, Radius.all(14.0F));
        new RenderUtil(drawContext, this.panelX, f4, this.panelWidth, f5).border(42.0F, ColorUtil.BLACK, 0.3F * f, Radius.all(14.0F));
        GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.panelX, f4, this.panelWidth, f5, f * 0.4F, 14.0F);
        new RenderUtil(drawContext, this.panelX, f4, this.panelWidth, f5).rect(Theme.withAlpha(ColorUtil.BLACK, 0.45F * f), Radius.all(14.0F));
        new RenderUtil(drawContext, this.panelX, f4, this.panelWidth, f5).shadow(0.7F, Theme.withAlpha(ColorUtil.WHITE, 0.05F * f), Radius.all(14.0F));
        float f6 = this.panelX + 16.0F;
        float f7 = this.panelWidth - 16.0F * 2.0F;
        float f8 = f4 + 16.0F;
        this.badgeHeight = 14.0F;
        this.badgeY = f8;
        this.badgeX = f6;
        this.badgeWidth = this.renderCurrentAccountBadge(drawContext, i3, i5, f3);
        this.closeButtonSize = 16.0F;
        this.closeButtonX = this.panelX + this.panelWidth - 16.0F - this.closeButtonSize;
        this.closeButtonY = f8 + (14.0F - this.closeButtonSize) / 2.0F;
        this.clearButtonSize = this.closeButtonSize;
        this.clearButtonY = this.closeButtonY;
        this.clearButtonX = this.closeButtonX - 6.0F - this.clearButtonSize;
        this.renderClearButton(drawContext, i3, i5, f3);
        this.renderIconButton(drawContext, this.closeButtonX, this.closeButtonY, this.closeButtonSize, Icon.CLOSE, i3, i5, f3);
        float f9 = f6 + (this.badgeWidth > 0.0F ? this.badgeWidth + 8.0F : 0.0F);
        if (this.badgeWidth > 0.0F) {
            new RenderUtil(drawContext, f9 - 5.0F, f8 + 2.0F, 1.0F, 10.0F).rect(Theme.withAlpha(ColorUtil.WHITE, 0.1F * f3), Radius.zero());
        }

        this.shelfY = f8;
        this.shelfX = f9;
        this.shelfWidth = this.clearButtonX - 8.0F - this.shelfX;
        this.renderFavoritesShelf(drawContext, i3, i5, f3);
        this.searchX = f6;
        this.searchY = f8 + 14.0F + 10.0F;
        this.searchWidth = f7;
        this.searchHeight = 22.0F;
        this.renderSearchField(drawContext, f3);
        float f10 = f4 + f5 - 16.0F - 22.0F;
        this.actionButtonWidth = 22.0F;
        this.actionButtonHeight = 22.0F;
        this.microsoftButtonX = f6 + f7 - 22.0F;
        this.fileLoginButtonX = this.microsoftButtonX - 4.0F - 22.0F;
        this.randomButtonX = this.fileLoginButtonX - 4.0F - 22.0F;
        this.addButtonX = this.randomButtonX - 4.0F - 22.0F;
        this.usernameFieldX = f6;
        this.usernameFieldY = f10;
        this.usernameFieldHeight = 22.0F;
        this.usernameFieldWidth = this.addButtonX - 8.0F - f6;
        this.actionButtonsY = f10;
        this.renderUsernameField(drawContext, f3);
        this.renderAnimatedIconButton(
            drawContext, this.addButtonX, f10, 22.0F, Icon.PLUS, this.addHoverAnimation, i3, i5, f3, Language.translate("menu.tip.add", "Добавить аккаунт")
        );
        this.renderAnimatedIconButton(
            drawContext,
            this.randomButtonX,
            f10,
            22.0F,
            Icon.DICE,
            this.randomHoverAnimation,
            i3,
            i5,
            f3,
            Language.translate("menu.tip.random", "Случайный ник")
        );
        this.renderAnimatedIconButton(
            drawContext,
            this.fileLoginButtonX,
            f10,
            22.0F,
            Icon.FOLDER,
            this.fileHoverAnimation,
            i3,
            i5,
            f3,
            Language.translate("menu.tip.auth_file", "Войти из файла cookie или refresh-token")
        );
        this.renderMicrosoftButton(drawContext, i3, i5, f3);
        this.listX = f6;
        this.listY = this.searchY + this.searchHeight + 8.0F;
        this.listWidth = f7;
        this.rowWidth = this.listWidth - 16.0F;
        this.listHeight = f10 - 10.0F - this.listY;
        int i8 = this.getFilteredAccounts().size();
        float f11 = i8 == 0 ? 0.0F : 24.0F + i8 * 40.0F - 6.0F;
        this.scroll.update(f11, this.listHeight);
        this.scroll.updateHover(i6, i7);
        this.renderAccountList(drawContext, i6, i7, f3);
        this.scroll.render(drawContext, this.listX + this.listWidth - 3.0F, this.listY, this.listHeight, f11, f3);
        this.renderParticles(drawContext, f);
        this.renderOverflowPopup(drawContext, i3, i5, f3);
        this.renderTooltip(drawContext, f3);
        if (this.clearConfirmOpen) {
            this.renderClearConfirmDialog(drawContext, i, i2, f);
        }

        if (this.isMicrosoftDialogOpen()) {
            this.renderMicrosoftLoginDialog(drawContext, i, i2, f);
        }
    }

    private float renderCurrentAccountBadge(DrawContext drawContext, int i, int i2, float f) {
        Account account = this.findActiveAccount();
        String string = account != null ? account.getUsername() : this.getSessionUsername();
        if (string == null) {
            string = "";
        }

        if (!string.equals(this.badgeName)) {
            if (!this.badgeName.isEmpty()) {
                this.badgeSquash = 0.16F;
            }

            this.badgeName = string;
        }

        this.updateBadgeSpring();
        if (this.badgeName.isEmpty()) {
            return 0.0F;
        }

        float f2 = (this.panelWidth - 32.0F) * 0.45F;
        String string2 = this.truncateBadgeName(string, f2);
        float f3 = this.getBadgeWidth(string2);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, this.badgeX + f3 / 2.0F, this.badgeY + 7.0F, 1.0F + this.badgeSquash * 0.4F, 1.0F - this.badgeSquash);
        this.renderAccountBadge(drawContext, string2, this.badgeX, this.badgeY, f3, ColorUtil.WHITE, 0.12F, 0.0F, f, 1.0F);
        MatrixUtil.pop(drawContext);
        return f3;
    }

    private void updateBadgeSpring() {
        long l = System.nanoTime();
        if (this.badgeLastNanos == 0L) {
            this.badgeLastNanos = l;
        }

        float f = Math.min((float)(l - this.badgeLastNanos) / 1.0E9F, 0.05F);
        this.badgeLastNanos = l;
        if (!(f <= 0.0F)) {
            this.badgeSquashVelocity = this.badgeSquashVelocity + (-42.0F * this.badgeSquash - 7.0F * this.badgeSquashVelocity) * f;
            this.badgeSquash = this.badgeSquash + this.badgeSquashVelocity * f;
            if (Math.abs(this.badgeSquash) < 5.0E-4F && Math.abs(this.badgeSquashVelocity) < 0.002F) {
                this.badgeSquash = 0.0F;
                this.badgeSquashVelocity = 0.0F;
            }
        }
    }

    private void renderFavoritesShelf(DrawContext drawContext, int i, int i2, float f) {
        List list = this.getFavoriteAccounts();
        if (list.isEmpty()) {
            new RenderUtil(drawContext, this.shelfX, this.shelfY, this.shelfWidth, 14.0F)
                .textMiddleLeft(
                    Fonts.INTER_REGULAR,
                    Language.translate("menu.shelf.empty", "Отметьте звёздочкой — аккаунт попадёт сюда"),
                    0.0F,
                    0.0F,
                    Theme.withAlpha(Theme.getSecondaryText(), 0.45F * f),
                    6.0F
                );
        } else {
            int i3 = list.size();
            long l = System.currentTimeMillis();
            this.visibleShelfCount = Math.max(0, (int)Math.floor((this.shelfWidth + 4.0F) / 18.0F));
            if (i3 > this.visibleShelfCount && this.visibleShelfCount > 0) {
                this.visibleShelfCount--;
            }

            int i4 = Math.min(i3, this.visibleShelfCount);

            for (int i5 = 0; i5 < i4; i5++) {
                Account account = (Account)list.get(i5);
                String string = account.getUsername() == null ? "" : account.getUsername();
                float f2 = this.shelfX + i5 * 18.0F;
                boolean flag = this.isHovered(i, i2, f2, this.shelfY, 14.0, 14.0);
                float f3 = this.shelfHoverProgress.getOrDefault(string, 0.0F);
                float f4 = f3 + ((flag ? 1.0F : 0.0F) - f3) * 0.25F;
                this.shelfHoverProgress.put(string, f4);
                if (flag) {
                    this.setTooltip(string, f2 + 7.0F, this.shelfY);
                }

                float f5 = 1.5F * f4;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f2 + 7.0F, this.shelfY + 7.0F - f5, 1.0F + 0.08F * f4);
                float f6 = f2 - 1.2F;
                float f7 = this.shelfY - f5 - 1.2F;
                float f8 = 14.0F + 1.2F * 2.0F;
                Radius radius = Radius.all(7.0F + 1.2F);
                float f9 = 0.5F + 0.5F * (float)Math.sin(l / 260.0 - i5 * 0.55);
                float f10 = (0.07F + 0.07F * f9 + 0.14F * f4) * f;
                new RenderUtil(drawContext, f6, f7, f8, f8).border(4.0F + 1.2F * f9, -14020, f10, radius);
                new RenderUtil(drawContext, f6, f7, f8, f8).border(2.0F, -14020, f10, radius);
                new RenderUtil(drawContext, f6, f7, f8, f8).rect(Theme.withAlpha(-14020, (0.7F + 0.3F * f4) * f), radius);
                this.renderAvatar(drawContext, string, f2, this.shelfY - f5, 14.0F, f, false);
                MatrixUtil.pop(drawContext);
            }

            this.overflowButtonSize = 0.0F;
            if (i3 > i4) {
                float f11 = this.shelfX + i4 * 18.0F;
                this.overflowButtonX = f11;
                this.overflowButtonY = this.shelfY;
                this.overflowButtonSize = 14.0F;
                boolean flag2;
                if (!this.isHovered(i, i2, f11, this.shelfY, 14.0, 14.0)) {
                    flag2 = this.overflowPopupOpen;
                } else {
                    flag2 = true;
                }

                new RenderUtil(drawContext, f11 - 1.2F, this.shelfY - 1.2F, 14.0F + 1.2F * 2.0F, 14.0F + 1.2F * 2.0F)
                    .rect(Theme.withAlpha(-14020, (flag2 ? 0.7F : 0.45F) * f), Radius.all(7.0F + 1.2F));
                new RenderUtil(drawContext, f11, this.shelfY, 14.0F, 14.0F)
                    .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, flag2 ? 0.14F : 0.09F), f), Radius.all(7.0F));
                new RenderUtil(drawContext, f11, this.shelfY, 14.0F, 14.0F)
                    .textCenter(Fonts.INTER_MEDIUM, "+" + (i3 - i4), Theme.withAlpha(flag2 ? Theme.getTextColor() : Theme.getSecondaryText(), f), 6.0F);
                if (flag2 && !this.overflowPopupOpen) {
                    this.setTooltip(Language.translate("menu.shelf.more", "ещё в избранном") + ": " + (i3 - i4), f11 + 7.0F, this.shelfY);
                    return;
                }
            } else if (this.overflowPopupOpen) {
                this.overflowPopupOpen = false;
            }
        }
    }

    private void renderOverflowPopup(DrawContext drawContext, int i, int i2, float f) {
        this.overflowRowBounds.clear();
        this.overflowRowAccounts.clear();
        this.overflowPopupAnimation.update();
        float f2 = this.overflowPopupAnimation.get();
        if (this.overflowPopupOpen || !(f2 <= 0.01F)) {
            List list = this.getFavoriteAccounts();
            int i3 = Math.max(0, list.size() - this.visibleShelfCount);
            if (i3 != 0) {
                int i4 = Math.min(i3, 8);
                float f3 = 90.0F;

                for (int i5 = 0; i5 < i3; i5++) {
                    String string = ((Account)list.get(this.visibleShelfCount + i5)).getUsername();
                    f3 = Math.max(f3, 6.0F + 12.0F + 6.0F + Fonts.INTER_MEDIUM.getWidth(string == null ? "" : string, 6.5F) + 10.0F);
                }

                this.overflowPopupWidth = Math.min(f3, this.panelWidth - 24.0F);
                this.overflowPopupHeight = i4 * 18.0F + 4.0F * 2.0F;
                this.overflowMaxScroll = Math.max(0.0F, i3 * 18.0F - i4 * 18.0F);
                this.overflowScroll = Math.max(0.0F, Math.min(this.overflowScroll, this.overflowMaxScroll));
                this.overflowPopupX = Math.max(
                    this.panelX + 8.0F,
                    Math.min(
                        this.overflowButtonX + this.overflowButtonSize / 2.0F - this.overflowPopupWidth / 2.0F,
                        this.panelX + this.panelWidth - 8.0F - this.overflowPopupWidth
                    )
                );
                this.overflowPopupY = this.overflowButtonY + this.overflowButtonSize + 5.0F;
                float f4 = f * f2;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, this.overflowPopupX + this.overflowPopupWidth / 2.0F, this.overflowPopupY, 0.94F + 0.06F * f2);
                Radius radius = Radius.all(9.0F);
                new RenderUtil(drawContext, this.overflowPopupX, this.overflowPopupY, this.overflowPopupWidth, this.overflowPopupHeight)
                    .border(18.0F, ColorUtil.BLACK, 0.3F * f4, radius);
                GuiRenderUtil.window(
                    drawContext,
                    RenderUtil.BlurQuality.MIDDLE,
                    true,
                    this.overflowPopupX,
                    this.overflowPopupY,
                    this.overflowPopupWidth,
                    this.overflowPopupHeight,
                    f4 * 0.95F,
                    9.0F
                );
                new RenderUtil(drawContext, this.overflowPopupX, this.overflowPopupY, this.overflowPopupWidth, this.overflowPopupHeight)
                    .rect(Theme.withAlpha(ColorUtil.BLACK, 0.3F * f4), radius);
                boolean flag = this.overflowPopupOpen;
                float f5 = this.overflowPopupY + 4.0F;
                float f6 = i4 * 18.0F;
                float f7 = 18.0F;
                float f8 = 4.0F;
                float f9 = 12.0F;
                float f10 = 6.5F;
                new RenderUtil(drawContext, this.overflowPopupX, f5, this.overflowPopupWidth, f6)
                    .scissor(
                        () -> {
                            int i6 = Math.max(0, (int)(this.overflowScroll / f7));
                            int i7 = Math.min(i3, i6 + i4 + 1);

                            for (int i8 = i6; i8 < i7; i8++) {
                                Account account = (Account)list.get(this.visibleShelfCount + i8);
                                String string2 = account.getUsername() == null ? "" : account.getUsername();
                                float f11 = f5 + i8 * f7 - this.overflowScroll;
                                boolean flag2;
                                if (flag && this.isHovered(i, i2, this.overflowPopupX, f11, this.overflowPopupWidth, f7)) {
                                    flag2 = this.isHovered(i, i2, this.overflowPopupX, f5, this.overflowPopupWidth, f6);
                                } else {
                                    flag2 = false;
                                }

                                if (flag2) {
                                    new RenderUtil(drawContext, this.overflowPopupX + f8, f11, this.overflowPopupWidth - f8 * 2.0F, f7)
                                        .rect(Theme.withAlpha(ColorUtil.WHITE, 0.08F * f4), Radius.all(7.0F));
                                }

                                float f12 = f11 + (f7 - f9) / 2.0F;
                                new RenderUtil(drawContext, this.overflowPopupX + 6.0F - 1.0F, f12 - 1.0F, f9 + 2.0F, f9 + 2.0F)
                                    .rect(Theme.withAlpha(-14020, 0.7F * f4), Radius.all(f9 / 2.0F + 1.0F));
                                this.renderAvatar(drawContext, string2, this.overflowPopupX + 6.0F, f12, f9, f4, false);
                                new RenderUtil(drawContext, this.overflowPopupX + 6.0F + f9 + 6.0F, f11, this.overflowPopupWidth, f7)
                                    .textMiddleLeft(
                                        Fonts.INTER_MEDIUM,
                                        string2,
                                        0.0F,
                                        0.0F,
                                        Theme.withAlpha(flag2 ? Theme.getTextColor() : Theme.getSecondaryText(), f4),
                                        f10
                                    );
                                if (flag) {
                                    this.overflowRowBounds.add(new float[]{this.overflowPopupX, f11, this.overflowPopupWidth, f7});
                                    this.overflowRowAccounts.add(account);
                                }
                            }
                        }
                    );
                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void renderClearButton(DrawContext drawContext, int i, int i2, float f) {
        boolean flag = !this.getAccountManager().hasNonFavorites();
        boolean flag2;
        if (!flag) {
            flag2 = this.isHovered(i, i2, this.clearButtonX, this.clearButtonY, this.clearButtonSize, this.clearButtonSize);
        } else {
            flag2 = false;
        }

        if (flag2) {
            this.setTooltip(Language.translate("menu.tip.clear", "Очистить неизбранные"), this.clearButtonX + this.clearButtonSize / 2.0F, this.clearButtonY);
        }

        if (flag2) {
            new RenderUtil(drawContext, this.clearButtonX, this.clearButtonY, this.clearButtonSize, this.clearButtonSize)
                .rect(Theme.withAlpha(-38037, 0.18F * f), Radius.all(6.0F));
        }

        float f2 = this.clearButtonSize * 0.46F;
        int i3 = flag ? Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f) : Theme.withAlpha(flag2 ? -38037 : Theme.getSecondaryText(), f);
        float f3 = this.clearButtonX + (this.clearButtonSize - f2) / 2.0F;
        float f4 = this.clearButtonY + (this.clearButtonSize - f2) / 2.0F;
        if (flag2) {
            this.clearIcon.render(drawContext, Icon.TRASH, f3, f4, f2, i3, 1.0F);
        } else {
            this.clearIcon.render(drawContext, Icon.TRASH, f3, f4, f2, i3, 0.0F);
        }
    }

    private void renderClearConfirmDialog(DrawContext drawContext, int i, int i2, float f) {
        this.clearConfirmAnimation.update();
        float f2 = this.clearConfirmAnimation.get();
        float f3 = f * f2;
        new RenderUtil(drawContext, this.panelX, this.panelY, this.panelWidth, this.panelHeight)
            .rect(Theme.withAlpha(ColorUtil.BLACK, 0.45F * f3), Radius.all(14.0F));
        this.confirmDialogWidth = Math.min(this.panelWidth - 44.0F, 232.0F);
        this.confirmDialogHeight = 104.0F;
        this.confirmDialogX = this.panelX + (this.panelWidth - this.confirmDialogWidth) / 2.0F;
        this.confirmDialogY = this.panelY + (this.panelHeight - this.confirmDialogHeight) / 2.0F;
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(
            drawContext, this.confirmDialogX + this.confirmDialogWidth / 2.0F, this.confirmDialogY + this.confirmDialogHeight / 2.0F, 0.94F + 0.06F * f2
        );
        Radius radius = Radius.all(12.0F);
        new RenderUtil(drawContext, this.confirmDialogX, this.confirmDialogY, this.confirmDialogWidth, this.confirmDialogHeight)
            .border(26.0F, ColorUtil.BLACK, 0.26F * f3, radius);
        GuiRenderUtil.window(
            drawContext,
            RenderUtil.BlurQuality.MAX,
            true,
            this.confirmDialogX,
            this.confirmDialogY,
            this.confirmDialogWidth,
            this.confirmDialogHeight,
            f3 * 0.9F,
            12.0F
        );
        new RenderUtil(drawContext, this.confirmDialogX, this.confirmDialogY, this.confirmDialogWidth, this.confirmDialogHeight)
            .rect(Theme.withAlpha(ColorUtil.BLACK, 0.22F * f3), radius);
        new RenderUtil(drawContext, this.confirmDialogX, this.confirmDialogY + 14.0F, this.confirmDialogWidth, 12.0F)
            .textTopCenter(
                Fonts.INTER_SEMIBOLD, Language.translate("menu.clear.title", "Очистить все аккаунты?"), Theme.withAlpha(Theme.getTextColor(), f3), 8.5F
            );
        new RenderUtil(drawContext, this.confirmDialogX + 12.0F, this.confirmDialogY + 30.0F, this.confirmDialogWidth - 24.0F, 10.0F)
            .textTopCenter(
                Fonts.INTER_REGULAR,
                Language.translate("menu.clear.subtitle", "Избранные аккаунты останутся — удалятся только остальные"),
                Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f3),
                6.0F
            );
        float f4 = (this.confirmDialogWidth - 28.0F - 8.0F) / 2.0F;
        this.dismissButtonX = this.confirmDialogX + 14.0F;
        this.dismissButtonY = this.confirmDialogY + this.confirmDialogHeight - 16.0F - 22.0F;
        this.dismissButtonWidth = f4;
        this.dismissButtonHeight = 22.0F;
        this.confirmButtonX = this.dismissButtonX + f4 + 8.0F;
        this.confirmButtonY = this.dismissButtonY;
        this.confirmButtonWidth = f4;
        this.confirmButtonHeight = 22.0F;
        boolean flag = this.isHovered(i, i2, this.dismissButtonX, this.dismissButtonY, this.dismissButtonWidth, this.dismissButtonHeight);
        boolean flag2 = this.isHovered(i, i2, this.confirmButtonX, this.confirmButtonY, this.confirmButtonWidth, this.confirmButtonHeight);
        this.renderDialogButton(
            drawContext,
            this.dismissButtonX,
            this.dismissButtonY,
            this.dismissButtonWidth,
            this.dismissButtonHeight,
            Language.translate("menu.cancel", "Отмена"),
            Theme.getTextColor(),
            flag,
            this.dismissTextEffect,
            f3
        );
        this.renderDialogButton(
            drawContext,
            this.confirmButtonX,
            this.confirmButtonY,
            this.confirmButtonWidth,
            this.confirmButtonHeight,
            Language.translate("menu.clear.confirm", "Очистить"),
            -38037,
            flag2,
            this.confirmTextEffect,
            f3
        );
        MatrixUtil.pop(drawContext);
    }

    private void renderDialogButton(
        DrawContext drawContext, float f, float f2, float f3, float f4, String string, int i, boolean flag, ShimmerText shimmerText, float f5
    ) {
        new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(ColorUtil.WHITE, (flag ? 0.11F : 0.06F) * f5), Radius.all(8.0F));
        int i2 = Theme.withAlpha(Theme.getSecondaryText(), f5);
        String string2 = flag ? shimmerText.applyThemed(Fonts.INTER_MEDIUM, string, 6.5F, i, i2, 1.0F) : string;
        new RenderUtil(drawContext, f, f2, f3, f4).textCenter(Fonts.INTER_MEDIUM, string2, i2, 6.5F);
    }

    private void closeOverflowPopup() {
        if (this.overflowPopupOpen) {
            this.overflowPopupOpen = false;
            this.overflowPopupAnimation.animate(0.0, 0.15, Easings.QUAD_OUT);
        }
    }

    private void setTooltip(String string, float f, float f2) {
        if (string != null && !string.isEmpty()) {
            this.tooltipText = string;
            this.tooltipX = f;
            this.tooltipY = f2;
        }
    }

    private void renderTooltip(DrawContext drawContext, float f) {
        if (this.tooltipText != null && !this.tooltipText.isEmpty()) {
            float f2 = Fonts.INTER_MEDIUM.getWidth(this.tooltipText, 6.0F) + 7.0F * 2.0F;
            float f3 = Math.max(this.panelX + 6.0F, Math.min(this.tooltipX - f2 / 2.0F, this.panelX + this.panelWidth - 6.0F - f2));
            float f4 = this.tooltipY - 4.0F - 14.0F;
            float f5 = f4 < 2.0F ? this.tooltipY + 4.0F : f4;
            new RenderUtil(drawContext, f3, f5, f2, 14.0F)
                .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.04F), 0.97F * f), Radius.all(6.0F));
            if (GraphicsQuality.getCurrent().isHigh()) {
                new RenderUtil(drawContext, f3, f5, f2, 14.0F)
                    .water(
                        Theme.getPrimary(),
                        f,
                        0.0F,
                        0.5F,
                        0.5F,
                        (float)(System.currentTimeMillis() % 1000000L) / 1000.0F + (f3 + f5) * 0.37F,
                        Radius.all(6.0F)
                    );
            }

            new RenderUtil(drawContext, f3, f5, f2, 14.0F).textCenter(Fonts.INTER_MEDIUM, this.tooltipText, Theme.withAlpha(Theme.getTextColor(), f), 6.0F);
        }
    }

    private String truncateBadgeName(String string, float f) {
        if (string != null && !string.isEmpty() && !(this.getBadgeWidth(string) <= f)) {
            String string2 = string;

            while (string2.length() > 1 && this.getBadgeWidth(string2 + "…") > f) {
                string2 = string2.substring(0, string2.length() - 1);
            }

            return string2 + "…";
        } else {
            return string;
        }
    }

    private float getBadgeWidth(String string) {
        String string2 = string == null ? "" : string;
        return 16.5F + Fonts.INTER_SEMIBOLD.getWidth(string2, 6.0F) + 7.0F;
    }

    private void renderAccountBadge(
        DrawContext drawContext, String string, float f, float f2, float f3, int i, float f4, float f5, float f6, float f7
    ) {
        float f8 = 1.5F * f5;
        float f9 = (0.7F + 0.3F * f7) * (1.0F + 0.06F * f5);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f + f3 / 2.0F, f2 + 7.0F - f8, f9);
        Radius radius = Radius.all(7.0F);
        new RenderUtil(drawContext, f, f2 - f8, f3, 14.0F).rect(Theme.withAlpha(ColorUtil.BLACK, (0.5F + 0.15F * f5) * f6), radius);
        if (GraphicsQuality.getCurrent().isHigh()) {
            new RenderUtil(drawContext, f, f2 - f8, f3, 14.0F)
                .water(Theme.getPrimary(), f6, f5, 0.5F, 0.5F, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F + (f + f2) * 0.37F, radius);
        }

        new RenderUtil(drawContext, f, f2 - f8, f3, 14.0F).shadow(0.8F, Theme.withAlpha(i, f4 * f6), radius);
        this.renderAvatarWithShadow(drawContext, string, f + 2.0F, f2 - f8 + 1.75F, 10.5F, f6);
        new RenderUtil(drawContext, f + 2.0F + 10.5F + 4.0F, f2 - f8, f3, 14.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f6), 6.0F);
        MatrixUtil.pop(drawContext);
    }

    private void renderAvatarWithShadow(DrawContext drawContext, String string, float f, float f2, float f3, float f4) {
        this.renderAvatar(drawContext, string, f, f2, f3, f4, true);
    }

    private void renderAvatar(DrawContext drawContext, String string, float f, float f2, float f3, float f4, boolean flag) {
        Radius radius = Radius.all(f3 / 2.0F);
        Identifier identifier = PlayerHeadCache.getHead(string);
        if (identifier == null) {
            identifier = PlayerHeadCache.getSteveHead();
        }

        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
            if (GuiRenderUtil.getBoundTexture() != null) {
                new RenderUtil(drawContext, f, f2, f3, f3).frostedGlass(f4, radius);
                if (flag) {
                    new RenderUtil(drawContext, f, f2, f3, f3).shadow(0.75F, Theme.withAlpha(ColorUtil.WHITE, 0.12F * f4), radius);
                }

                return;
            }
        }

        int i = ColorUtil.fromHsb(((string == null ? 0 : string.hashCode()) & 65535) / 65535.0F, 0.55F, 0.75F);
        new RenderUtil(drawContext, f, f2, f3, f3).rect(Theme.withAlpha(i, 0.9F * f4), radius);
        String string2 = string != null && !string.isEmpty() ? string.substring(0, 1).toUpperCase(Locale.ROOT) : "?";
        new RenderUtil(drawContext, f, f2, f3, f3).textCenter(Fonts.INTER_SEMIBOLD, string2, Theme.withAlpha(ColorUtil.WHITE, f4), f3 * 0.42F);
    }

    private Account findActiveAccount() {
        for (Account account : this.getAccountManager().getAccounts()) {
            if (this.isActiveAccount(account)) {
                return account;
            }
        }

        return null;
    }

    private void renderHint(DrawContext drawContext, float f, float f2, String string, float f3) {
        new RenderUtil(drawContext, f, f2, 240.0F, 8.0F)
            .textTopLeft(Fonts.INTER_REGULAR, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f3), 6.0F);
    }

    private void renderAnimatedIconButton(
        DrawContext drawContext, float f, float f2, float f3, Icon icon2, Animation animation, int i, int i2, float f4, String string
    ) {
        animation.update();
        boolean flag = this.isHovered(i, i2, f, f2, f3, f3);
        animation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f5 = animation.get();
        if (flag && string != null) {
            this.setTooltip(string, f + f3 / 2.0F, f2);
        }

        long l = System.currentTimeMillis();
        if (this.diceLastTime == 0L) {
            this.diceLastTime = l;
        }

        float f6 = (float)Math.min(l - this.diceLastTime, 100L);
        boolean flag2 = icon2 == Icon.DICE;
        if (flag2) {
            this.diceLastTime = l;
            if (f5 > 0.01F) {
                this.diceRotation = (this.diceRotation + f6 * 0.22F * f5) % 360.0F;
            }
        }

        float f7 = f3 * 0.46F;
        float f8 = f + f3 / 2.0F;
        float f9 = f2 + f3 / 2.0F;
        boolean flag3 = f5 > 0.001F || flag2 && this.diceRotation > 0.001F;
        if (flag3) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f8, f9, flag2 ? 90.0F * f5 + this.diceRotation : 90.0F * f5);
            MatrixUtil.scale(drawContext, f8, f9, 1.0F + 0.24F * f5);
        }

        int i3 = Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), ColorUtil.WHITE, f5), f4);
        new RenderUtil(drawContext, f, f2, f3, f3).icon(icon2, (f3 - f7) / 2.0F, (f3 - f7) / 2.0F, f7, i3);
        if (flag3) {
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderIconButtonWithTooltip(DrawContext drawContext, float f, float f2, float f3, Icon icon, int i, int i2, float f4, String string) {
        if (string != null && this.isHovered(i, i2, f, f2, f3, f3)) {
            this.setTooltip(string, f + f3 / 2.0F, f2);
        }

        this.renderIconButton(drawContext, f, f2, f3, icon, i, i2, f4);
    }

    private void renderIconButton(DrawContext drawContext, float f, float f2, float f3, Icon icon2, int i, int i2, float f4) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, f3);
        if (icon2 == Icon.CLOSE) {
            float f5 = f3 * 0.46F;
            IconHoverAnimator iconHoverAnimator = this.closeIcon;
            float f6 = f + (f3 - f5) / 2.0F;
            float f7 = f2 + (f3 - f5) / 2.0F;
            int i3 = Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f4);
            if (flag) {
                iconHoverAnimator.render(drawContext, icon2, f6, f7, f5, i3, 1.0F);
            } else {
                iconHoverAnimator.render(drawContext, icon2, f6, f7, f5, i3, 0.0F);
            }
        } else {
            if (flag) {
                new RenderUtil(drawContext, f, f2, f3, f3)
                    .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.12F), 0.7F * f4), Radius.all(6.0F));
            }

            float f8 = f3 * 0.46F;
            int i4 = Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f4);
            new RenderUtil(drawContext, f, f2, f3, f3).icon(icon2, (f3 - f8) / 2.0F, (f3 - f8) / 2.0F, f8, i4);
        }
    }

    private void renderMicrosoftButton(DrawContext drawContext, int i, int i2, float f) {
        this.microsoftHoverAnimation.update();
        boolean flag = this.isHovered(i, i2, this.microsoftButtonX, this.actionButtonsY, this.actionButtonWidth, this.actionButtonHeight);
        this.microsoftHoverAnimation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = this.microsoftHoverAnimation.get();
        if (flag) {
            this.setTooltip(
                Language.translate("menu.ms.button", "Войти через Microsoft"), this.microsoftButtonX + this.actionButtonWidth / 2.0F, this.actionButtonsY
            );
        }

        float f3 = this.actionButtonWidth * 0.55F;
        float f4 = this.microsoftButtonX + (this.actionButtonWidth - f3) / 2.0F;
        float f5 = this.actionButtonsY + (this.actionButtonHeight - f3) / 2.0F;
        float f6 = 2.0F + 1.6F * f2;
        float f7 = (f3 - 2.0F) / 2.0F;
        boolean flag2 = f2 > 0.001F;
        if (flag2) {
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(
                drawContext, this.microsoftButtonX + this.actionButtonWidth / 2.0F, this.actionButtonsY + this.actionButtonHeight / 2.0F, 1.0F + 0.14F * f2
            );
        }

        float f8 = f4 - (f6 - 2.0F) / 2.0F;
        float f9 = f5 - (f6 - 2.0F) / 2.0F;
        new RenderUtil(drawContext, f8, f9, f7, f7).rect(Theme.withAlpha(-896990, f), Radius.zero());
        new RenderUtil(drawContext, f8 + f7 + f6, f9, f7, f7).rect(Theme.withAlpha(-8406528, f), Radius.zero());
        new RenderUtil(drawContext, f8, f9 + f7 + f6, f7, f7).rect(Theme.withAlpha(-16734993, f), Radius.zero());
        new RenderUtil(drawContext, f8 + f7 + f6, f9 + f7 + f6, f7, f7).rect(Theme.withAlpha(-18176, f), Radius.zero());
        if (flag2) {
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderSearchField(DrawContext drawContext, float f) {
        new RenderUtil(drawContext, this.searchX, this.searchY, this.searchWidth, this.searchHeight)
            .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.85F * f), Radius.all(9.0F));
        this.searchIconAnimation.update();
        float f2 = this.searchIconAnimation.get();
        boolean flag = f2 > 0.001F && f2 < 0.999F;
        float f3 = this.searchX + 12.0F;
        float f4 = this.searchY + this.searchHeight / 2.0F;
        if (flag) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f3, f4, (float)Math.sin(f2 * Math.PI * 2.0) * 14.0F);
            MatrixUtil.scale(drawContext, f3, f4, 1.0F + (float)Math.sin(f2 * Math.PI) * 0.22F);
        }

        new RenderUtil(drawContext, this.searchX + 6.0F, this.searchY, 12.0F, this.searchHeight)
            .icon(
                Icon.SEARCH,
                0.0F,
                (this.searchHeight - 10.0F) / 2.0F,
                10.0F,
                Theme.withAlpha(this.searchFocused ? Theme.getTextColor() : Theme.getSecondaryText(), f)
            );
        if (flag) {
            MatrixUtil.pop(drawContext);
        }

        if (this.searchFocused && this.searchSelectedAll && !this.searchQuery.isEmpty()) {
            float f5 = Math.min(Fonts.INTER_MEDIUM.getWidth(this.searchQuery, 6.5F) + 2.0F, this.searchWidth - 24.0F);
            new RenderUtil(drawContext, this.searchX + 20.0F, this.searchY + 3.0F, f5, this.searchHeight - 6.0F)
                .rect(Theme.withAlpha(Theme.getPrimary(), 0.3F * f), Radius.all(3.0F));
        }

        boolean flag2;
        if (this.searchQuery.isEmpty()) {
            flag2 = !this.searchFocused;
        } else {
            flag2 = false;
        }

        String string = flag2 ? Language.translate("menu.search", "Поиск") : this.searchQuery + (this.searchFocused && isCaretVisible() ? "_" : "");
        int i = flag2 ? Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f) : Theme.withAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, this.searchX + 21.0F, this.searchY, this.searchWidth - 25.0F, this.searchHeight)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 6.5F);
    }

    private List<Account> getFavoriteAccounts() {
        ArrayList arrayList = new ArrayList();

        for (Account account : this.getAccountManager().getAccounts()) {
            if (account.isFavorite()) {
                arrayList.add(account);
            }
        }

        return arrayList;
    }

    private float getRowAppearProgress(int i, long l) {
        float f = Math.min(i * 45.0F, 360.0F);
        float f2 = ((float)(l - this.appearTime) - f) / 300.0F;
        if (f2 <= 0.0F) {
            return 0.0F;
        } else {
            return f2 >= 1.0F ? 1.0F : (float)Easings.EXPO_OUT.ease(f2);
        }
    }

    private List<Account> getFilteredAccounts() {
        String string = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        ArrayList arrayList = new ArrayList();

        for (Account account : this.getAccountManager().getAccounts()) {
            String string2 = account.getUsername();
            if (string.isEmpty() || string2 != null && string2.toLowerCase(Locale.ROOT).contains(string)) {
                arrayList.add(account);
            }
        }

        return arrayList;
    }

    private void renderUsernameField(DrawContext drawContext, float f) {
        new RenderUtil(drawContext, this.usernameFieldX, this.usernameFieldY, this.usernameFieldWidth, this.usernameFieldHeight)
            .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.85F * f), Radius.all(6.0F));
        if (this.usernameFocused && this.usernameSelectedAll && !this.usernameInput.isEmpty()) {
            float f2 = Math.min(Fonts.INTER_MEDIUM.getWidth(this.usernameInput, 7.5F) + 2.0F, this.usernameFieldWidth - 12.0F);
            new RenderUtil(drawContext, this.usernameFieldX + 7.0F, this.usernameFieldY + 4.0F, f2, this.usernameFieldHeight - 8.0F)
                .rect(Theme.withAlpha(Theme.getPrimary(), 0.3F * f), Radius.all(3.0F));
        }

        boolean flag;
        if (this.usernameInput.isEmpty()) {
            flag = !this.usernameFocused;
        } else {
            flag = false;
        }

        String string = flag
            ? Language.translate("menu.offline_placeholder", "Offline username")
            : this.usernameInput + (this.usernameFocused && isCaretVisible() ? "_" : "");
        int i = flag ? Theme.withAlpha(Theme.getSecondaryText(), f * 0.8F) : Theme.withAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, this.usernameFieldX + 8.0F, this.usernameFieldY, this.usernameFieldWidth - 16.0F, this.usernameFieldHeight)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 7.5F);
    }

    private void renderAccountList(DrawContext drawContext, int i, int i2, float f) {
        List<Account> accounts = this.getFilteredAccounts();
        long l = System.currentTimeMillis();
        Account account = null;

        for (Account account2 : accounts) {
            if (this.isActiveAccount(account2)) {
                account = account2;
                break;
            }
        }

        this.highlightAnimation.update();
        if (this.highlightedAccount != account && this.highlightAnimation.get() < 0.04F) {
            this.highlightedAccount = account;
        }

        this.highlightAnimation.animate(this.highlightedAccount != null && this.highlightedAccount == account ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        float f2 = this.highlightAnimation.get();
        if (this.highlightedAccount != null && f2 > 0.001F) {
            int i3 = accounts.indexOf(this.highlightedAccount);
            float f3 = i3 >= 0 ? this.getRowY(i3) : 0.0F;
            if (i3 >= 0 && f3 + 34.0F >= this.listY && f3 <= this.listY + this.listHeight) {
                float f4 = this.getRowAppearProgress(i3, l);
                float f5 = f3 + (1.0F - f4) * 8.0F;
                float f6 = f * f4 * f2;
                new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
                    .scissor(
                        () -> new RenderUtil(drawContext, this.listX, f5, this.rowWidth, 34.0F)
                            .rect(Theme.withAlpha(ColorUtil.WHITE, 0.05F * f6), Radius.all(8.0F))
                    );
            }
        }

        new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
            .scissor(
                () -> {
                    for (int i4 = 0; i4 < accounts.size(); i4++) {
                        float f7 = this.getRowY(i4);
                        if (!(f7 + 34.0F < this.listY) && !(f7 > this.listY + this.listHeight)) {
                            float f8 = accounts.get(i4) == this.highlightedAccount ? f2 : 0.0F;
                            this.renderAccountRow(
                                drawContext,
                                (Account)accounts.get(i4),
                                this.listX,
                                f7,
                                this.rowWidth,
                                i,
                                i2,
                                f,
                                this.getRowAppearProgress(i4, l),
                                f8
                            );
                        }
                    }
                }
            );
        if (accounts.isEmpty()) {
            String string = this.getAccountManager().getAccounts().isEmpty()
                ? Language.translate("menu.no_accounts", "Пока нет аккаунтов")
                : Language.translate("menu.no_found", "Ничего не найдено");
            new RenderUtil(drawContext, this.listX, this.listY + 6.0F, this.listWidth, 40.0F)
                .textTopCenter(Fonts.INTER_MEDIUM, string, Theme.withAlpha(Theme.getSecondaryText(), f), 7.5F);
        }
    }

    private void renderAccountRow(DrawContext drawContext, Account account, float f, float f2, float f3, int i, int i2, float f4, float f5, float f6) {
        float f7 = f2 + (1.0F - f5) * 8.0F;
        float f8 = f4 * f5;
        boolean flag = false;
        if (account == this.removingAccount) {
            float f9 = Math.min((float)(System.currentTimeMillis() - this.removeStartTime) / 380.0F, 1.0F);
            f8 *= 1.0F - f9;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f + f3 / 2.0F, f7 + 17.0F, 1.0F - 0.25F * f9);
            flag = true;
        }

        boolean flag2 = this.isActiveAccount(account);
        boolean flag3;
        if (this.isHovered(i, i2, f, f7, f3, 34.0)) {
            flag3 = this.isHovered(i, i2, this.listX, this.listY, this.listWidth, this.listHeight);
        } else {
            flag3 = false;
        }

        Radius radius = Radius.all(8.0F);
        if (flag3 && !flag2) {
            GuiRenderUtil.drawHighlight(drawContext, f, f7, f3, 34.0F, f8, radius);
        }

        String string = account.getUsername() == null ? "" : account.getUsername();
        float f10 = f + 7.0F;
        float f11 = f7 + (34.0F - 22.0F) / 2.0F;
        this.renderAvatarWithShadow(drawContext, string, f10, f11, 22.0F, f8);
        float f12 = f10 + 22.0F + 8.0F;
        int i3 = Theme.withAlpha(Theme.getTextColor(), f8);
        String string2 = f6 > 0.001F ? this.activeNameEffect.applyThemed(Fonts.INTER_MEDIUM, string, 7.5F, Theme.getPrimary(), i3, f6) : string;
        new RenderUtil(drawContext, f12, f7, f3, 34.0F).textTopLeft(Fonts.INTER_MEDIUM, string2, 0.0F, 8.0F, i3, 7.5F);
        String string3 = account.isRefreshTokenAccount()
            ? Language.translate("menu.type.microsoft_refresh_token", "Microsoft refresh token")
            : (
                account.isCookieAccount()
                    ? Language.translate("menu.type.microsoft_cookie", "Microsoft cookie")
                    : (account.isMicrosoft() ? Language.translate("menu.type.microsoft", "Microsoft") : Language.translate("menu.type.offline", "Offline"))
            );
        new RenderUtil(drawContext, f12, f7, f3, 34.0F)
            .textTopLeft(Fonts.INTER_REGULAR, string3, 0.0F, 19.0F, Theme.withAlpha(Theme.getSecondaryText(), f8), 6.5F);
        float f13 = f + f3 - 16.0F - 8.0F;
        float f14 = f7 + 9.0F;
        boolean flag4 = this.isHovered(i, i2, f13, f14, 16.0, 16.0);
        this.trashIcon.render(drawContext, Icon.TRASH, f13, f14, 16.0F, Theme.withAlpha(flag4 ? -38037 : Theme.getSecondaryText(), f8), flag4 ? 1.0F : 0.0F);
        float f15 = f13 - 14.0F - 6.0F;
        float f16 = f7 + 10.0F;
        boolean flag5 = this.isHovered(i, i2, f15, f16, 14.0, 14.0);
        int i4 = account.isFavorite()
            ? Theme.withAlpha(-14020, f8)
            : Theme.withAlpha(flag5 ? Theme.getTextColor() : Theme.getSecondaryText(), (flag5 ? 0.9F : 0.55F) * f8);
        this.starIcon.render(drawContext, Icon.STAR, f15, f16, 14.0F, i4, flag5 ? 1.0F : 0.0F);
        if (flag) {
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderMicrosoftLoginDialog(DrawContext drawContext, int i, int i2, float f) {
        new RenderUtil(drawContext, this.panelX, this.panelY, this.panelWidth, this.panelHeight).rect(Theme.withAlpha(-16777216, 0.45F * f), Radius.all(14.0F));
        float f2 = Math.min(280.0F, this.panelWidth - 40.0F);
        float f3 = this.panelX + (this.panelWidth - f2) / 2.0F;
        float f4 = this.panelY + (this.panelHeight - 158.0F) / 2.0F;
        GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f3, f4, f2, 158.0F, f, 12.0F);
        new RenderUtil(drawContext, f3, f4 + 14.0F, f2, 12.0F)
            .textTopCenter(
                Fonts.INTER_SEMIBOLD, Language.translate("menu.ms.title", "Sign in with Microsoft"), Theme.withAlpha(Theme.getTextColor(), f), 9.5F
            );
        MicrosoftAuthenticator.State state = this.microsoftAuth.getState();
        if (state == MicrosoftAuthenticator.State.SELECTING_FILE) {
            new RenderUtil(drawContext, f3, f4 + 60.0F, f2, 12.0F)
                .textTopCenter(
                    Fonts.INTER_MEDIUM,
                    Language.translate("menu.ms.file.selecting", "Выберите файл Microsoft auth..."),
                    Theme.withAlpha(Theme.getSecondaryText(), f),
                    7.5F
                );
            this.cancelButton.setLabel(Language.translate("menu.cancel", "Cancel"));
            this.cancelButton.setBounds(f3 + f2 / 2.0F - 40.0F, f4 + 158.0F - 30.0F, 80.0F, 20.0F).render(drawContext, i, i2, f);
        } else if (state == MicrosoftAuthenticator.State.AUTHENTICATING_COOKIE) {
            new RenderUtil(drawContext, f3, f4 + 60.0F, f2, 12.0F)
                .textTopCenter(
                    Fonts.INTER_MEDIUM,
                    Language.translate("menu.ms.cookie.requesting", "Выполняется вход через cookie..."),
                    Theme.withAlpha(Theme.getSecondaryText(), f),
                    7.5F
                );
            this.cancelButton.setLabel(Language.translate("menu.cancel", "Cancel"));
            this.cancelButton.setBounds(f3 + f2 / 2.0F - 40.0F, f4 + 158.0F - 30.0F, 80.0F, 20.0F).render(drawContext, i, i2, f);
        } else if (state == MicrosoftAuthenticator.State.AUTHENTICATING_REFRESH_TOKEN) {
            new RenderUtil(drawContext, f3, f4 + 60.0F, f2, 12.0F)
                .textTopCenter(
                    Fonts.INTER_MEDIUM,
                    Language.translate("menu.ms.refresh_token.requesting", "Выполняется вход по refresh-token..."),
                    Theme.withAlpha(Theme.getSecondaryText(), f),
                    7.5F
                );
            this.cancelButton.setLabel(Language.translate("menu.cancel", "Cancel"));
            this.cancelButton.setBounds(f3 + f2 / 2.0F - 40.0F, f4 + 158.0F - 30.0F, 80.0F, 20.0F).render(drawContext, i, i2, f);
        } else if (state == MicrosoftAuthenticator.State.REQUESTING) {
            new RenderUtil(drawContext, f3, f4 + 60.0F, f2, 12.0F)
                .textTopCenter(
                    Fonts.INTER_MEDIUM, Language.translate("menu.ms.requesting", "Requesting code..."), Theme.withAlpha(Theme.getSecondaryText(), f), 7.5F
                );
            this.cancelButton.setLabel(Language.translate("menu.cancel", "Cancel"));
            this.cancelButton.setBounds(f3 + f2 / 2.0F - 40.0F, f4 + 158.0F - 30.0F, 80.0F, 20.0F).render(drawContext, i, i2, f);
        } else if (state == MicrosoftAuthenticator.State.ERROR) {
            new RenderUtil(drawContext, f3 + 12.0F, f4 + 56.0F, f2 - 24.0F, 12.0F)
                .textTopCenter(Fonts.INTER_MEDIUM, Language.translate("menu.ms.error", "Login failed"), Theme.withAlpha(-38037, f), 7.5F);
            this.cancelButton.setLabel(Language.translate("menu.close", "Close"));
            this.cancelButton.setBounds(f3 + f2 / 2.0F - 40.0F, f4 + 158.0F - 30.0F, 80.0F, 20.0F).render(drawContext, i, i2, f);
        } else {
            new RenderUtil(drawContext, f3 + 16.0F, f4 + 36.0F, f2 - 32.0F, 10.0F)
                .textTopCenter(
                    Fonts.INTER_REGULAR,
                    Language.translate("menu.ms.instruction", "Open the page and enter the code:"),
                    Theme.withAlpha(Theme.getSecondaryText(), f),
                    6.5F
                );
            new RenderUtil(drawContext, f3, f4 + 54.0F, f2, 20.0F)
                .textTopCenter(Fonts.INTER_BOLD, this.microsoftAuth.getUserCode(), Theme.withAlpha(Theme.getPrimary(), f), 16.0F);
            new RenderUtil(drawContext, f3 + 16.0F, f4 + 80.0F, f2 - 32.0F, 10.0F)
                .textTopCenter(Fonts.INTER_REGULAR, this.microsoftAuth.getVerificationUri(), Theme.withAlpha(Theme.getSecondaryText(), f), 6.0F);
            float f5 = f4 + 158.0F - 32.0F;
            float f6 = (f2 - 32.0F - 16.0F) / 3.0F;
            this.copyCodeButton.setLabel(Language.translate("menu.copy", "Copy code"));
            this.openPageButton.setLabel(Language.translate("menu.open", "Open page"));
            this.cancelButton.setLabel(Language.translate("menu.cancel", "Cancel"));
            this.copyCodeButton.setBounds(f3 + 16.0F, f5, f6, 22.0F).render(drawContext, i, i2, f);
            this.openPageButton.withAccentColor(Theme.getPrimary()).setBounds(f3 + 16.0F + f6 + 8.0F, f5, f6, 22.0F).render(drawContext, i, i2, f);
            this.cancelButton.setBounds(f3 + 16.0F + (f6 + 8.0F) * 2.0F, f5, f6, 22.0F).render(drawContext, i, i2, f);
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (this.isMicrosoftDialogOpen()) {
            if (this.microsoftAuth.getState() == MicrosoftAuthenticator.State.AWAITING_USER) {
                if (this.copyCodeButton.mouseClicked(d, d2)) {
                    return true;
                }

                if (this.openPageButton.mouseClicked(d, d2)) {
                    return true;
                }
            }

            this.cancelButton.mouseClicked(d, d2);
            return true;
        } else {
            this.usernameFocused = this.isHovered(d, d2, this.usernameFieldX, this.usernameFieldY, this.usernameFieldWidth, this.usernameFieldHeight);
            this.searchFocused = this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, this.searchHeight);
            this.usernameSelectedAll = false;
            this.searchSelectedAll = false;
            if (this.searchFocused) {
                float f = this.searchIconAnimation.get();
                if (!this.searchIconAnimation.isAnimating() || f <= 0.001F || f >= 0.999F) {
                    this.searchIconAnimation.forceValue(0.0);
                    this.searchIconAnimation.animate(1.0, 0.42, Easings.QUAD_OUT);
                }
            }

            if (this.clearConfirmOpen) {
                if (this.isHovered(d, d2, this.confirmButtonX, this.confirmButtonY, this.confirmButtonWidth, this.confirmButtonHeight)) {
                    this.getAccountManager().removeNonFavorites();
                    this.clearConfirmOpen = false;
                    return true;
                }

                if (this.isHovered(d, d2, this.dismissButtonX, this.dismissButtonY, this.dismissButtonWidth, this.dismissButtonHeight)
                    || !this.isHovered(d, d2, this.confirmDialogX, this.confirmDialogY, this.confirmDialogWidth, this.confirmDialogHeight)) {
                    this.clearConfirmOpen = false;
                }
            } else {
                if (this.isHovered(d, d2, this.closeButtonX, this.closeButtonY, this.closeButtonSize, this.closeButtonSize)) {
                    this.menuScreen.closeAccounts();
                    return true;
                }

                if (this.isHovered(d, d2, this.clearButtonX, this.clearButtonY, this.clearButtonSize, this.clearButtonSize)) {
                    if (this.getAccountManager().hasNonFavorites()) {
                        this.clearConfirmOpen = true;
                        this.clearConfirmAnimation.forceValue(0.0);
                        this.clearConfirmAnimation.animate(1.0, 0.2, Easings.QUAD_OUT);
                        return true;
                    }
                } else {
                    if (this.isHovered(d, d2, this.addButtonX, this.usernameFieldY, this.actionButtonWidth, this.actionButtonHeight)) {
                        this.addOfflineAccount();
                        return true;
                    }

                    if (this.isHovered(d, d2, this.randomButtonX, this.actionButtonsY, this.actionButtonWidth, this.actionButtonHeight)) {
                        this.addRandomAccount();
                        return true;
                    }

                    if (this.isHovered(d, d2, this.fileLoginButtonX, this.actionButtonsY, this.actionButtonWidth, this.actionButtonHeight)) {
                        this.startFileLogin();
                        return true;
                    }

                    if (this.isHovered(d, d2, this.microsoftButtonX, this.actionButtonsY, this.actionButtonWidth, this.actionButtonHeight)) {
                        this.startMicrosoftLogin();
                        return true;
                    }

                    if (!this.badgeName.isEmpty() && this.isHovered(d, d2, this.badgeX, this.badgeY, this.badgeWidth, this.badgeHeight)) {
                        this.badgeSquash = 0.05F;
                        this.badgeSquashVelocity = 0.0F;
                        return true;
                    }

                    if (this.overflowPopupOpen) {
                        int i = 0;

                        for (int i2 = Math.min(this.overflowRowBounds.size(), this.overflowRowAccounts.size()); i < i2; i++) {
                            float[] floats = this.overflowRowBounds.get(i);
                            if (this.isHovered(d, d2, floats[0], floats[1], floats[2], floats[3])) {
                                Account account = this.overflowRowAccounts.get(i);
                                if (click.button() == 1) {
                                    account.setFavorite(false);
                                    this.getAccountManager().save();
                                    this.spawnFavoriteParticles(floats[0] + 12.0F, floats[1] + floats[3] / 2.0F);
                                    return true;
                                }

                                this.loginAccount(account);
                                this.closeOverflowPopup();
                                return true;
                            }
                        }

                        this.closeOverflowPopup();
                        return true;
                    }

                    if (this.overflowButtonSize > 0.0F
                        && this.isHovered(d, d2, this.overflowButtonX, this.overflowButtonY, this.overflowButtonSize, this.overflowButtonSize)) {
                        this.overflowPopupOpen = true;
                        this.overflowScroll = 0.0F;
                        this.overflowPopupAnimation.forceValue(0.0);
                        this.overflowPopupAnimation.animate(1.0, 0.2, Easings.QUAD_OUT);
                        return true;
                    }

                    if (this.isHovered(d, d2, this.shelfX, this.shelfY, this.shelfWidth, 14.0)) {
                        List list = this.getFavoriteAccounts();
                        int i3 = 0;

                        for (int i4 = Math.min(list.size(), this.visibleShelfCount); i3 < i4; i3++) {
                            float f2 = this.shelfX + i3 * 18.0F;
                            if (this.isHovered(d, d2, f2, this.shelfY, 14.0, 14.0)) {
                                Account account2 = (Account)list.get(i3);
                                if (click.button() == 1) {
                                    account2.setFavorite(false);
                                    this.getAccountManager().save();
                                    this.spawnFavoriteParticles(f2 + 7.0F, this.shelfY + 7.0F);
                                    return true;
                                }

                                this.loginAccount(account2);
                                return true;
                            }
                        }
                    } else {
                        if (this.scroll.mouseClicked(d, d2)) {
                            return true;
                        }

                        if (!this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
                            return false;
                        }

                        if (this.scroll.isDragging()) {
                            return true;
                        }

                        List list2 = this.getFilteredAccounts();

                        for (int i5 = 0; i5 < list2.size(); i5++) {
                            float f3 = this.getRowY(i5);
                            if (!(f3 + 34.0F < this.listY) && !(f3 > this.listY + this.listHeight)) {
                                float f4 = this.listX + this.rowWidth - 16.0F - 8.0F;
                                float f5 = f3 + 9.0F;
                                if (this.isHovered(d, d2, f4, f5, 16.0, 16.0)) {
                                    this.getAccountManager().remove((Account)list2.get(i5));
                                    return true;
                                }

                                float f6 = f4 - 14.0F - 6.0F;
                                float f7 = f3 + 10.0F;
                                if (this.isHovered(d, d2, f6, f7, 14.0, 14.0)) {
                                    Account account3 = (Account)list2.get(i5);
                                    boolean flag2 = !account3.isFavorite();
                                    account3.setFavorite(flag2);
                                    this.getAccountManager().save();
                                    if (flag2) {
                                        this.spawnFavoriteParticles(f6 + 7.0F, f7 + 7.0F);
                                        return true;
                                    }
                                    break;
                                }

                                if (this.isHovered(d, d2, this.listX, f3, this.rowWidth, 34.0)) {
                                    this.loginAccount((Account)list2.get(i5));
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            return true;
        }
    }

    public void mouseReleased(Click click) {
        this.scroll.mouseReleased();
    }

    public boolean mouseDragged(Click click) {
        return this.scroll.mouseDragged(this.getMouseX(), this.getMouseY());
    }

    public boolean mouseScrolled(double d, double d2, double d3) {
        if (this.isMicrosoftDialogOpen()) {
            return true;
        }

        if (this.overflowPopupOpen) {
            if (this.isHovered(d, d2, this.overflowPopupX, this.overflowPopupY, this.overflowPopupWidth, this.overflowPopupHeight)) {
                this.overflowScroll -= (float)d3 * 18.0F;
            }

            return true;
        } else {
            if (!this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
                return false;
            }

            this.scroll.onScroll(d3);
            return true;
        }
    }

    public boolean keyPressed(KeyInput keyInput) {
        label148: {
            label159: {
                if (this.clearConfirmOpen) {
                    int i = keyInput.key();
                    switch (i) {
                        case 256:
                            this.clearConfirmOpen = false;
                            return true;
                        case 257:
                        case 335:
                            this.getAccountManager().removeNonFavorites();
                            this.clearConfirmOpen = false;
                    }
                } else if (this.isMicrosoftDialogOpen()) {
                    if (keyInput.key() == 256) {
                        this.cancelMicrosoftLogin();
                        return true;
                    }
                } else {
                    if (this.overflowPopupOpen && keyInput.key() == 256) {
                        this.closeOverflowPopup();
                        return true;
                    }

                    int i2 = keyInput.key();
                    boolean flag = (keyInput.modifiers() & 2) != 0;
                    if (this.usernameFocused) {
                        if (flag && i2 == 65) {
                            if (!this.usernameInput.isEmpty()) {
                                this.usernameSelectedAll = true;
                                return true;
                            }

                            this.usernameSelectedAll = false;
                            return true;
                        }

                        if (flag && i2 == 67) {
                            if (!this.usernameInput.isEmpty()) {
                                mc.keyboard.setClipboard(this.usernameInput);
                                return true;
                            }

                            return true;
                        }

                        if (flag) {
                            switch (i2) {
                                case 86:
                                    this.pasteUsername();
                                    return true;
                                case 256:
                                    break label159;
                                case 257:
                                case 335:
                                    break label148;
                                case 259:
                                    break;
                                default:
                                    return false;
                            }
                        } else {
                            switch (i2) {
                                case 256:
                                    break label159;
                                case 257:
                                case 335:
                                    break label148;
                                case 259:
                                    break;
                                default:
                                    return false;
                            }
                        }

                        if (this.usernameSelectedAll) {
                            this.usernameInput = "";
                            this.usernameSelectedAll = false;
                            return true;
                        }

                        if (!this.usernameInput.isEmpty()) {
                            this.usernameInput = this.usernameInput.substring(0, this.usernameInput.length() - 1);
                        }

                        this.usernameSelectedAll = false;
                        return true;
                    } else {
                        label134:
                        if (this.searchFocused) {
                            if (flag && i2 == 65) {
                                if (!this.searchQuery.isEmpty()) {
                                    this.searchSelectedAll = true;
                                    return true;
                                }

                                this.searchSelectedAll = false;
                                return true;
                            }

                            if (flag && i2 == 67) {
                                if (!this.searchQuery.isEmpty()) {
                                    mc.keyboard.setClipboard(this.searchQuery);
                                    return true;
                                }

                                return true;
                            }

                            if (flag) {
                                switch (i2) {
                                    case 86:
                                        this.pasteSearchQuery();
                                        return true;
                                    case 256:
                                    case 257:
                                    case 335:
                                        break label134;
                                    case 259:
                                        break;
                                    default:
                                        return false;
                                }
                            } else {
                                switch (i2) {
                                    case 256:
                                    case 257:
                                    case 335:
                                        break label134;
                                    case 259:
                                        break;
                                    default:
                                        return false;
                                }
                            }

                            if (this.searchSelectedAll) {
                                this.searchQuery = "";
                                this.searchSelectedAll = false;
                                return true;
                            }

                            if (!this.searchQuery.isEmpty()) {
                                this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                            }

                            this.searchSelectedAll = false;
                            return true;
                        } else {
                            if (flag && i2 == 86) {
                                this.pasteAndLoginAccount();
                                return true;
                            }

                            return false;
                        }
                    }

                    this.searchFocused = false;
                    this.searchSelectedAll = false;
                    return true;
                }

                return true;
            }

            this.usernameFocused = false;
            this.usernameSelectedAll = false;
            return true;
        }

        this.addOfflineAccount();
        return true;
    }

    public boolean charTyped(CharInput charInput) {
        boolean flag;
        if (!InputUtil.isKeyPressed(mc.getWindow(), 341)) {
            flag = InputUtil.isKeyPressed(mc.getWindow(), 345);
        } else {
            flag = true;
        }

        if (!this.isMicrosoftDialogOpen() && !flag) {
            char c = (char)charInput.codepoint();
            if (this.usernameFocused) {
                if (this.usernameSelectedAll) {
                    this.usernameInput = "";
                    this.usernameSelectedAll = false;
                }

                if (this.usernameInput.length() < 16 && (Character.isLetterOrDigit(c) || c == '_')) {
                    this.usernameInput = this.usernameInput + c;
                    return true;
                }
            } else if (this.searchFocused) {
                if (this.searchSelectedAll) {
                    this.searchQuery = "";
                    this.searchSelectedAll = false;
                }

                if (this.searchQuery.length() < 24 && c >= ' ') {
                    this.searchQuery = this.searchQuery + c;
                    return true;
                }
            }
        }

        return false;
    }

    private void pasteUsername() {
        String string = mc.keyboard.getClipboard();
        if (string != null && !string.isEmpty()) {
            if (this.usernameSelectedAll) {
                this.usernameInput = appendValidUsernameChars("", string);
                this.usernameSelectedAll = false;
            } else {
                this.usernameInput = appendValidUsernameChars(this.usernameInput, string);
                this.usernameSelectedAll = false;
            }
        }
    }

    private void pasteAndLoginAccount() {
        String string = mc.keyboard.getClipboard();
        if (string != null) {
            Account account = this.getAccountManager().addOffline(appendValidUsernameChars("", string));
            if (account != null) {
                this.getAccountManager().apply(account);
            }
        }
    }

    private static String appendValidUsernameChars(String string, String string2) {
        StringBuilder builder = new StringBuilder(string);
        int i = string2.length();

        for (int i2 = 0; i2 < i && builder.length() < 16; i2++) {
            char c = string2.charAt(i2);
            if (Character.isLetterOrDigit(c) || c == '_') {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private void pasteSearchQuery() {
        String string = mc.keyboard.getClipboard();
        if (string != null && !string.isEmpty()) {
            StringBuilder builder = new StringBuilder(this.searchSelectedAll ? "" : this.searchQuery);
            this.searchSelectedAll = false;

            for (int i = 0; i < string.length() && builder.length() < 24; i++) {
                char c = string.charAt(i);
                if (c >= ' ') {
                    builder.append(c);
                }
            }

            this.searchQuery = builder.toString();
        }
    }

    private void addOfflineAccount() {
        String string = this.usernameInput.trim();
        if (!string.isEmpty()) {
            this.getAccountManager().addOffline(string);
            this.usernameInput = "";
            this.usernameFocused = false;
        }
    }

    private void addRandomAccount() {
        Account account = this.getAccountManager().addOffline(NickGeneratorUtil.generate());
        if (account != null) {
            this.getAccountManager().apply(account);
        }
    }

    private void startMicrosoftLogin() {
        this.microsoftAuth.startDeviceCodeLogin();
    }

    private void startFileLogin() {
        this.microsoftAuth
            .startFileLogin(
                Language.translate("menu.ms.file_dialog", "Choose Microsoft auth file"),
                Language.translate("menu.ms.file_filter", "Microsoft authentication (*.txt)")
            );
    }

    private void cancelMicrosoftLogin() {
        this.microsoftAuth.cancel();
        this.microsoftAuth.reset();
    }

    private void copyUserCode() {
        if (!this.microsoftAuth.getUserCode().isEmpty()) {
            mc.keyboard.setClipboard(this.microsoftAuth.getUserCode());
        }
    }

    private void openVerificationPage() {
        String string = this.microsoftAuth.getDirectVerificationUri();
        if (string == null || string.isEmpty()) {
            string = this.microsoftAuth.getVerificationUri();
        }

        if (string != null && !string.isEmpty()) {
            Util.getOperatingSystem().open(URI.create(string));
        }
    }

    private void loginAccount(Account account) {
        if (account != null) {
            if (account.isMicrosoft()) {
                if (!this.loginInProgress) {
                    this.loginInProgress = true;
                    Thread thread = new Thread(() -> {
                        boolean flag = MicrosoftAuthenticator.refresh(account);
                        if (flag) {
                            this.pendingLoginAccount = account;
                        }

                        this.loginInProgress = false;
                    }, "Sadnes-MS-Login");
                    thread.setDaemon(true);
                    thread.start();
                }
            } else {
                this.getAccountManager().apply(account);
            }
        }
    }

    private void pollLoginResults() {
        if (this.microsoftAuth.getState() == MicrosoftAuthenticator.State.SUCCESS) {
            Account account = this.microsoftAuth.takeResult();
            if (account != null) {
                this.getAccountManager().add(account);
                this.getAccountManager().apply(account);
            }
        }

        if (this.pendingLoginAccount != null) {
            this.getAccountManager().apply(this.pendingLoginAccount);
            this.pendingLoginAccount = null;
        }
    }

    private boolean isMicrosoftDialogOpen() {
        return this.microsoftAuth.isBusy() || this.microsoftAuth.getState() == MicrosoftAuthenticator.State.ERROR;
    }

    private float getRowY(int i) {
        return this.listY + 12.0F + this.scroll.getScroll() + i * 40.0F;
    }

    private void spawnFavoriteParticles(float f, float f2) {
        for (int i = 0; i < 14; i++) {
            AccountsPanel.Particle particle = new AccountsPanel.Particle();
            double d = Math.random() * Math.PI * 2.0;
            float f3 = 18.0F + (float)Math.random() * 34.0F;
            particle.x = f;
            particle.y = f2;
            particle.velocityX = (float)Math.cos(d) * f3;
            particle.velocityY = (float)Math.sin(d) * f3 - 10.0F;
            particle.lifetime = 0.45F + (float)Math.random() * 0.35F;
            particle.size = 1.4F + (float)Math.random() * 1.8F;
            particle.color = -14020;
            this.particles.add(particle);
        }
    }

    private void spawnDeleteParticles(float f, float f2) {
        for (int i = 0; i < 18; i++) {
            AccountsPanel.Particle particle = new AccountsPanel.Particle();
            double d = Math.random() * Math.PI * 2.0;
            float f3 = 14.0F + (float)Math.random() * 40.0F;
            particle.x = f + (float)(Math.random() * 2.0 - 1.0) * 8.0F;
            particle.y = f2 + (float)(Math.random() * 2.0 - 1.0) * 6.0F;
            particle.velocityX = (float)Math.cos(d) * f3;
            particle.velocityY = (float)Math.sin(d) * f3 - 6.0F;
            particle.lifetime = 0.4F + (float)Math.random() * 0.4F;
            particle.size = 1.2F + (float)Math.random() * 1.6F;
            particle.color = Math.random() < 0.5 ? -38037 : -4209719;
            this.particles.add(particle);
        }
    }

    private void renderParticles(DrawContext drawContext, float f) {
        if (this.particles.isEmpty()) {
            this.particlesLastNanos = 0L;
        } else {
            long l = System.nanoTime();
            if (this.particlesLastNanos == 0L) {
                this.particlesLastNanos = l;
            }

            float f2 = Math.min((float)(l - this.particlesLastNanos) / 1.0E9F, 0.05F);
            this.particlesLastNanos = l;

            for (int i = this.particles.size() - 1; i >= 0; i--) {
                AccountsPanel.Particle particle = this.particles.get(i);
                particle.age += f2;
                if (particle.age >= particle.lifetime) {
                    this.particles.remove(i);
                } else {
                    particle.velocityY += 80.0F * f2;
                    particle.velocityX *= 1.0F - 1.6F * f2;
                    particle.x = particle.x + particle.velocityX * f2;
                    particle.y = particle.y + particle.velocityY * f2;
                    float f3 = 1.0F - particle.age / particle.lifetime;
                    float f4 = particle.size * (0.45F + 0.55F * f3);
                    new RenderUtil(drawContext, particle.x - f4, particle.y - f4, f4 * 2.0F, f4 * 2.0F)
                        .rect(Theme.withAlpha(particle.color, f3 * f), Radius.all(f4));
                }
            }
        }
    }

    private boolean isActiveAccount(Account account) {
        AccountManager accountManager = this.getAccountManager();
        if (accountManager.isActive(account)) {
            return true;
        }

        String string = this.getSessionUsername();
        return string != null && !string.isEmpty() && string.equals(account.getUsername());
    }

    private String getSessionUsername() {
        try {
            return mc.getSession() != null ? mc.getSession().getUsername() : "";
        } catch (Exception exception) {
            return "";
        }
    }

    private double getMouseX() {
        Window window = mc.getWindow();
        return window.getWidth() == 0 ? 0.0 : mc.mouse.getX() * window.getScaledWidth() / window.getWidth();
    }

    private double getMouseY() {
        Window window = mc.getWindow();
        return window.getHeight() == 0 ? 0.0 : mc.mouse.getY() * window.getScaledHeight() / window.getHeight();
    }

    private static boolean isCaretVisible() {
        return System.currentTimeMillis() / 500L % 2L == 0L;
    }

    static final class Particle {
        float x;
        float y;
        float velocityX;
        float velocityY;
        float age;
        float lifetime;
        float size;
        int color;
    }
}
