package tech.sadnes.client.ui.menu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.account.Account;
import tech.sadnes.client.account.AccountManager;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.WelcomePrefs;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.RopeRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.screen.settings.SettingsScreen;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class MainMenuScreen extends Screen implements IRenderable, IMinecraft {
    private static final float GRAB_RADIUS_X_RATIO = 0.164F;
    private static final float GRAB_RADIUS_Y_RATIO = 0.138F;
    private static final String TITLE_TEXT = "Sadnes 1.21.11";
    private static final float MIN_PANEL_WIDTH = 280.0F;
    private static final float MIN_PANEL_HEIGHT = 240.0F;
    private static final float ACCOUNTS_MIN_WIDTH = 240.0F;
    private static final float ACCOUNTS_MIN_HEIGHT = 165.0F;
    private static final int ROPE_SEGMENTS = 16;
    private final Animation openAnimation = new Animation();
    private final Animation contentAnimation = new Animation();
    private final Animation accountsAnimation = new Animation();
    private final Animation welcomeAnimation = new Animation();
    private final ShimmerText titleGradientText = new ShimmerText();
    private boolean welcomeShown;
    private boolean welcomeDismissed;
    private long lastPhysicsNanos;
    private float physicsAccumulator;
    private static final float PHYSICS_STEP_MS = 16.666666F;
    private float releaseStartX;
    private float releaseStartY;
    private float releaseOffsetX;
    private float releaseOffsetY;
    private final LogoLiquidEffect logoRenderer = new LogoLiquidEffect();
    private boolean accountsOpen;
    private final List<MenuButton> primaryButtons = new ArrayList<>();
    private final List<MainMenuScreen.FooterLink> footerLinks = new ArrayList<>();
    private static final MainMenuScreen.MenuAction[] PRIMARY_ACTIONS = new MainMenuScreen.MenuAction[]{
        MainMenuScreen.MenuAction.SINGLEPLAYER, MainMenuScreen.MenuAction.MULTIPLAYER
    };
    private static final MainMenuScreen.MenuAction[] FOOTER_ACTIONS = new MainMenuScreen.MenuAction[]{
        MainMenuScreen.MenuAction.ACCOUNTS, MainMenuScreen.MenuAction.SETTINGS, MainMenuScreen.MenuAction.QUIT
    };
    private static final float BUTTON_RADIUS = 6.0F;
    private static final float BUTTON_GAP = 4.0F;
    private static final float FOOTER_TEXT_SIZE = 6.5F;
    private static final float BUTTON_HEIGHT = 19.0F;
    private static final float FOOTER_HEIGHT = 11.0F;
    private static final float FOOTER_GAP = 16.0F;
    private static final float FOOTER_MARGIN = 8.0F;
    private static final float FOOTER_HOVER_RANGE = 42.0F;
    private static final int QUIT_COLOR = -2075297;
    private final float[] ropeX = new float[17];
    private final float[] ropeY = new float[17];
    private final float[] ropePrevX = new float[17];
    private final float[] ropePrevY = new float[17];
    private boolean ropeInitialized;
    private final float[] splineX = new float[161];
    private final float[] splineY = new float[161];
    private final float[] smoothedRopeX = new float[17];
    private final float[] smoothedRopeY = new float[17];
    private MainMenuScreen.LogoState logoState = MainMenuScreen.LogoState.HIDDEN;
    private long stateStartTime;
    private float logoX;
    private float logoY;
    private float logoVelocityX;
    private float logoVelocityY;
    private float logoRotation;
    private long shakeStartTime = -100000L;
    private float homeX;
    private float homeY;
    private float grabOffsetX;
    private float grabOffsetY;
    private final float[] thinkOffsetsX = new float[3];
    private final float[] thinkOffsetsY = new float[3];
    private float idleOffsetX;
    private float idleOffsetY;
    private float ropeTargetX;
    private float ropeTargetY;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private float dragStartX;
    private float dragStartY;
    private float squashX;
    private float squashVelocityX;
    private float squashY;
    private float squashVelocityY;
    private static final float SPRING_STIFFNESS = 0.017F;
    private static final float VELOCITY_DAMPING = 0.976F;
    private static final float BOUNCE_RESTITUTION = 0.5F;
    private static final float DRAG_FOLLOW_FACTOR = 0.55F;
    private static final float MAX_DRAG_SPEED = 15.0F;
    private static final float CLICK_DRAG_THRESHOLD_SQ = 25.0F;
    private static final int MAX_PARTICLES = 64;
    private final float[] particleX = new float[64];
    private final float[] particleY = new float[64];
    private final float[] particleVelocityX = new float[64];
    private final float[] particleVelocityY = new float[64];
    private final float[] particleLife = new float[64];
    private final float[] particleMaxLife = new float[64];
    private int nextParticleIndex;
    private float ropeAlpha;
    private float grabAmount;
    private float ropeLength;
    private float reachLength;
    private float ropeTipVelocityX;
    private float ropeTipVelocityY;
    private float thinkProgress;
    private int thinkFromIndex;
    private int thinkToIndex;
    private float wobblePhase;
    private static final int ROPE_ITERATIONS = 2;
    private static final int CONSTRAINT_ITERATIONS = 6;
    private static final float MIN_SQUASH = 0.35F;
    private static final float SQUASH_SPRING = 0.3F;
    private static final float HIT_SCALE = 1.45F;
    private static final long SHAKE_DURATION_MS = 1500L;
    private static final float REACH_DURATION_MS = 240.0F;
    private static final long REACH_TIMEOUT_MS = 300L;
    private static final float ROPE_MIN_LENGTH = 4.0F;
    private static final float PARTICLE_MIN_COUNT = 3.0F;
    private static final float REACH_LINEAR_WEIGHT = 0.07F;
    private static final float REACH_WOBBLE_AMPLITUDE = 0.55F;
    private static final float REACH_WOBBLE_FREQUENCY = 0.04F;
    private static final int SPLINE_SUBDIVISIONS = 10;
    private static final int SMOOTH_PASSES = 3;
    private static final float SMOOTH_FACTOR = 0.45F;
    private static final float ROPE_WIDTH = 4.0F;
    private static final float ROPE_DASH_LENGTH = 1.8F;
    private static final float ROPE_DASH_GAP = 4.0F;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float logoBaseX;
    private float logoBaseY;
    private float logoSize;
    private float buttonsX;
    private float buttonsWidth;
    private float buttonsY;
    private float footerY;
    private float footerCenterX;
    private final AccountsPanel accountsPanel = new AccountsPanel(this);
    private final ChangelogPanel changelogPanel = new ChangelogPanel();
    private final MenuTour tour = new MenuTour();
    private final GraphicsQualitySlider sidePanel = new GraphicsQualitySlider();
    private float lastScreenWidth;
    private float lastScreenHeight;

    static int accentColor() {
        return Theme.getPrimary();
    }

    private static int lightAccentColor() {
        return ColorUtil.interpolate(Theme.getPrimary(), ColorUtil.WHITE, 0.45F);
    }

    private static int darkAccentColor() {
        return ColorUtil.darken(Theme.getPrimary(), 0.7F);
    }

    private static int paleAccentColor() {
        return ColorUtil.interpolate(Theme.getPrimary(), ColorUtil.WHITE, 0.85F);
    }

    public MainMenuScreen() {
        super(Text.literal("Sadnes"));
    }

    public float getPanelX() {
        return this.panelX;
    }

    public float getPanelY() {
        return this.panelY;
    }

    public float getPanelWidth() {
        return this.panelWidth;
    }

    public float getPanelHeight() {
        return this.panelHeight;
    }

    @Override
    public void init() {
        this.accountsOpen = false;
        this.ropeInitialized = false;
        this.logoState = MainMenuScreen.LogoState.HIDDEN;
        this.idleOffsetX = 0.0F;
        this.idleOffsetY = 0.0F;
        this.shakeStartTime = -100000L;
        this.ropeLength = 0.0F;
        this.reachLength = 0.0F;
        this.ropeTipVelocityX = 0.0F;
        this.ropeTipVelocityY = 0.0F;
        this.thinkProgress = 0.0F;
        this.thinkFromIndex = 0;
        this.thinkToIndex = 1;
        this.wobblePhase = 0.0F;
        this.dragging = false;
        this.ropeAlpha = 0.0F;
        this.grabAmount = 0.0F;
        this.squashX = this.squashVelocityX = this.squashY = this.squashVelocityY = 0.0F;

        for (int i = 0; i < 64; i++) {
            this.particleLife[i] = 0.0F;
        }

        this.logoRenderer.reset();
        this.openAnimation.forceValue(0.0);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
        this.contentAnimation.forceValue(1.0);
        this.welcomeShown = !this.welcomeDismissed && !WelcomePrefs.isShown();
        if (this.welcomeShown) {
            this.welcomeAnimation.forceValue(1.0);
            this.contentAnimation.forceValue(0.0);
        }

        this.updateLayout();
        this.accountsPanel.onOpen();
    }

    private void dismissWelcome() {
        this.welcomeDismissed = true;
        this.welcomeShown = false;
        WelcomePrefs.markShown();
        this.welcomeAnimation.animate(0.0, 0.55, Easings.QUART_OUT);
        this.contentAnimation.forceValue(0.0);
        this.contentAnimation.animate(1.0, 0.6, Easings.QUAD_OUT);
        this.tour.start();
    }

    private void updateLayout() {
        float f = GuiScale.getScaledWidth();
        float f2 = GuiScale.getScaledHeight();
        this.lastScreenWidth = f;
        this.lastScreenHeight = f2;
        this.panelWidth = Math.min(Math.max(f * 0.4F, 280.0F), Math.min(340.0F, f - 24.0F));
        float f3 = Math.min(Math.max(f2 * 0.62F, 240.0F), Math.min(372.0F, f2 - 24.0F));
        this.panelHeight = this.accountsPanel.computePanelHeight(f3);
        this.panelX = (f - this.panelWidth) / 2.0F;
        this.panelY = (f2 - this.panelHeight) / 2.0F;
        this.logoSize = 135.0F;
        float f4 = PRIMARY_ACTIONS.length * 19.0F + (PRIMARY_ACTIONS.length - 1) * 4.0F + 8.0F + 11.0F;
        float f5 = f / 2.0F;
        float f6 = f2 * 0.07F;
        this.buttonsY = (f2 - f4) / 2.0F + f6;
        this.logoBaseX = f5 - this.logoSize / 2.0F;
        this.logoBaseY = this.buttonsY - 45.0F - 0.638F * this.logoSize;
        this.buttonsWidth = Math.min(f * 0.32F, 165.0F);
        this.buttonsX = f5 - this.buttonsWidth / 2.0F;
        if (this.primaryButtons.isEmpty()) {
            for (MainMenuScreen.MenuAction menuAction : PRIMARY_ACTIONS) {
                MenuButton menuButton = new MenuButton(MenuButton.Style.GHOST, menuAction.icon, menuAction.getDisplayName(), () -> this.performAction(menuAction));
                menuButton.withAccentColor(accentColor());
                this.primaryButtons.add(menuButton);
            }
        }

        if (this.footerLinks.isEmpty()) {
            for (MainMenuScreen.MenuAction menuAction2 : FOOTER_ACTIONS) {
                this.footerLinks.add(new MainMenuScreen.FooterLink(menuAction2));
            }
        }

        float f7 = this.buttonsY;

        for (int i = 0; i < this.primaryButtons.size(); i++) {
            this.primaryButtons.get(i).setLabel(PRIMARY_ACTIONS[i].getDisplayName());
            this.primaryButtons.get(i).setBounds(this.buttonsX, f7, this.buttonsWidth, 19.0F);
            f7 += 19.0F + 4.0F;
        }

        float f8 = this.buttonsY + this.primaryButtons.size() * 19.0F + (this.primaryButtons.size() - 1) * 4.0F;
        this.footerY = f8 + 8.0F;
        this.footerCenterX = f5;
        this.layoutFooter();
        this.accountsPanel.setBounds(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
    }

    private void layoutFooter() {
        if (!this.footerLinks.isEmpty()) {
            float f = 0.0F;

            for (MainMenuScreen.FooterLink footerLink : this.footerLinks) {
                f += footerLink.getPreferredWidth();
            }

            float f2 = f + Math.max(0, this.footerLinks.size() - 1) * 16.0F;
            float f3 = this.footerCenterX - f2 / 2.0F;

            for (MainMenuScreen.FooterLink footerLink2 : this.footerLinks) {
                float f4 = footerLink2.getPreferredWidth();
                footerLink2.setBounds(f3, this.footerY, f4, 11.0F);
                f3 += f4 + 16.0F;
            }
        }
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        Sadnes.getInstance().saveAll();
        if (Fonts.INTER_MEDIUM != null) {
            int i3 = (int)GuiScale.toScaled(i);
            int i4 = (int)GuiScale.toScaled(i2);
            if (GuiScale.getScaledWidth() != this.lastScreenWidth || GuiScale.getScaledHeight() != this.lastScreenHeight) {
                this.updateLayout();
            }

            this.openAnimation.update();
            this.contentAnimation.update();
            float f2 = this.openAnimation.get();
            this.renderBackground(drawContext, 1.0F);
            float f3 = GuiScale.getScaledWidth();
            float f4 = GuiScale.getScaledHeight();
            if (GuiScale.isTooSmall()) {
                this.sidePanel.cancelDrag();
                this.changelogPanel.close();
            } else {
                this.welcomeAnimation.update();
                float f5 = this.welcomeAnimation.get();
                this.accountsAnimation.update();
                this.accountsAnimation.animate(this.accountsOpen ? 1.0 : 0.0, 0.4, Easings.QUART_OUT);
                MenuBackgroundRenderer.renderBlurOverlay(drawContext, f3, f4, Math.max(this.accountsAnimation.get(), f5), f2);
                float f6 = f2 * (1.0F - f5);
                float f7 = this.contentAnimation.get();
                float f8 = 0.95F + 0.05F * f7;
                boolean flag = this.panelWidth >= 240.0F && this.panelHeight >= 165.0F;
                if (f5 < 0.999F) {
                    MatrixUtil.push(drawContext);
                    MatrixUtil.scale(drawContext, f3 / 2.0F, f4 / 2.0F, f8, f8);
                    if (this.accountsOpen) {
                        if (flag) {
                            this.accountsPanel.render(drawContext, i3, i4, f6, f7);
                        }
                    } else {
                        boolean flag2 = this.changelogPanel.isVisible();
                        this.renderMainMenu(drawContext, flag2 ? -100000 : i3, flag2 ? -100000 : i4, f6);
                    }

                    MatrixUtil.pop(drawContext);
                }

                float f9 = f6 * (1.0F - this.accountsAnimation.get());
                this.sidePanel.render(drawContext, i3, i4, this.lastScreenWidth, this.lastScreenHeight, f9 * (1.0F - this.changelogPanel.getOpenProgress()));
                if (f9 <= 0.004F || this.changelogPanel.isOpen()) {
                    this.sidePanel.cancelDrag();
                }

                this.changelogPanel.render(drawContext, i3, i4, f9, f2);
                this.tour.update(this.buildTourState());
                float f10 = this.accountsOpen ? this.panelY - 8.0F : this.logoBaseY + 0.362F * this.logoSize - 12.0F;
                if (this.tour.isActive() && f2 > 0.004F) {
                    this.tour
                        .render(
                            drawContext, i3, i4, f2, this.getTourHighlightBounds(), f10, this.getTourPlacement(), this.changelogPanel.getToggleIconBounds()
                        );
                }

                if (f5 > 0.005F) {
                    this.renderWelcome(drawContext, f3, f4, f2 * f5, f5);
                }
            }
        }
    }

    private MenuTour.State buildTourState() {
        AccountManager accountManager = Sadnes.getInstance().getAccountManager();
        int i = accountManager == null ? 0 : accountManager.getAccounts().size();
        int i2 = 0;
        if (accountManager != null) {
            Iterator iterator2 = accountManager.getAccounts().iterator();

            while (iterator2.hasNext()) {
                if (((Account)iterator2.next()).isFavorite()) {
                    i2++;
                }
            }
        }

        return new MenuTour.State(this.changelogPanel.isOpen(), this.accountsOpen, i, i2);
    }

    private float[] getTourHighlightBounds() {
        switch (this.tour.getBlockReason()) {
            case CLOSE_CHANGELOG:
                return this.changelogPanel.getToggleBounds();
            case CLOSE_ACCOUNTS:
                if (this.accountsOpen) {
                    return this.accountsPanel.getCloseButtonBounds();
                }

                return null;
            case OPEN_ACCOUNTS:
                return this.getFooterBounds(MainMenuScreen.MenuAction.ACCOUNTS);
            default:
                switch (this.tour.getStep()) {
                    case ASK:
                        break;
                    case CHANGELOG:
                        return this.changelogPanel.getToggleBounds();
                    case ACCOUNTS_OPEN:
                        if (this.accountsOpen) {
                            return null;
                        }

                        return this.getFooterBounds(MainMenuScreen.MenuAction.ACCOUNTS);
                    case ACCOUNTS_ADD:
                        if (this.accountsOpen) {
                            return this.accountsPanel.getUsernameFieldBounds();
                        }
                        break;
                    case ACCOUNTS_FAV:
                        if (this.accountsOpen) {
                            return this.accountsPanel.getFavoriteButtonBounds();
                        }
                        break;
                    case ACCOUNTS_DEL:
                        if (this.accountsOpen) {
                            return this.accountsPanel.getDeleteButtonBounds();
                        }
                        break;
                    case ACCOUNTS_CLOSE:
                        if (this.accountsOpen) {
                            return this.accountsPanel.getCloseButtonBounds();
                        }
                        break;
                    case SETTINGS:
                        if (this.accountsOpen) {
                            return null;
                        }

                        return this.getFooterBounds(MainMenuScreen.MenuAction.SETTINGS);
                    default:
                        throw new MatchException(null, null);
                }

                return null;
        }
    }

    private MenuTour.Placement getTourPlacement() {
        boolean flag = this.tour.getBlockReason() == MenuTour.BlockReason.CLOSE_CHANGELOG
            || this.tour.getBlockReason() == MenuTour.BlockReason.NONE && this.tour.getStep() == MenuTour.Step.CHANGELOG;
        return flag ? MenuTour.Placement.RIGHT : MenuTour.Placement.BELOW;
    }

    private float[] getFooterBounds(MainMenuScreen.MenuAction menuAction) {
        for (int i = 0; i < FOOTER_ACTIONS.length && i < this.footerLinks.size(); i++) {
            if (FOOTER_ACTIONS[i] == menuAction) {
                return this.footerLinks.get(i).getBounds();
            }
        }

        return null;
    }

    private void renderWelcome(DrawContext drawContext, float f, float f2, float f3, float f4) {
        new RenderUtil(drawContext, 0.0F, 0.0F, f, f2).rect(Theme.withAlpha(ColorUtil.BLACK, 0.35F * f3), Radius.zero());
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f / 2.0F, f2 / 2.0F, 1.0F + 0.12F * (1.0F - f4));
        float f5 = f2 * 0.4F;
        new RenderUtil(drawContext, 0.0F, f5 - 26.0F, f, 9.0F)
            .textTopCenter(
                Fonts.INTER_MEDIUM, Language.translate("menu.welcome.title", "Добро пожаловать в"), Theme.withAlpha(Theme.getSecondaryText(), f3), 9.0F
            );
        String string = this.titleGradientText.apply(Fonts.NUNITO, "Sadnes 1.21.11", 22.0F, lightAccentColor(), darkAccentColor(), paleAccentColor());
        new RenderUtil(drawContext, 0.0F, f5 - 12.0F, f, 22.0F).textTopCenter(Fonts.NUNITO, string, Theme.withAlpha(ColorUtil.WHITE, f3), 22.0F);
        new RenderUtil(drawContext, 0.0F, f5 + 22.0F, f, 7.0F)
            .textTopCenter(
                Fonts.INTER_MEDIUM,
                Language.translate("menu.welcome.thanks", "Приятной игры и спасибо, что выбираете нас!"),
                Theme.withAlpha(Theme.getSecondaryText(), 0.9F * f3),
                7.0F
            );
        new RenderUtil(drawContext, 0.0F, f5 + 34.0F, f, 6.5F)
            .textTopCenter(
                Fonts.INTER_REGULAR,
                Language.translate("menu.welcome.bugs", "Если нашли недоработку или баг — напишите об этом в нашем Discord-канале!"),
                Theme.withAlpha(Theme.getSecondaryText(), 0.65F * f3),
                6.5F
            );
        float f6 = 0.55F + 0.2F * (float)Math.sin((float)(System.currentTimeMillis() % 2000L) / 2000.0F * Math.PI * 2.0);
        new RenderUtil(drawContext, 0.0F, f5 + 58.0F, f, 6.0F)
            .textTopCenter(
                Fonts.INTER_MEDIUM,
                Language.translate("menu.welcome.continue", "Нажмите, чтобы продолжить"),
                Theme.withAlpha(Theme.getTextColor(), f6 * f3),
                6.0F
            );
        MatrixUtil.pop(drawContext);
    }

    private void renderHitSparkles(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f3 / 1500.0F;
        float f6 = f5 < 0.8F ? 1.0F : Math.max(0.0F, 1.0F - (f5 - 0.8F) / 0.2F);
        float f7 = this.logoSize * 0.55F;
        float f8 = f2 - this.logoSize * 0.7F;
        float f9 = f3 / 1000.0F * 3.2F;

        for (int i = 0; i < 4; i++) {
            float f10 = f9 + i * (float)((Math.PI * 2) / 4);
            float f11 = f + (float)Math.cos(f10) * f7;
            float f12 = f8 + (float)Math.sin(f10) * f7 * 0.42F;
            float f13 = 0.55F + 0.45F * (float)Math.sin(f3 / 1000.0F * 8.0 + i);
            int i2 = Theme.withAlpha(-8070, f13 * f6 * f4);
            this.drawSparkle(drawContext, f11, f12, this.logoSize * 0.11F, i2);
        }
    }

    private void drawSparkle(DrawContext drawContext, float f, float f2, float f3, int i) {
        Radius radius = Radius.all(f3 * 0.2F);
        new RenderUtil(drawContext, f - f3, f2 - f3 * 0.2F, f3 * 2.0F, f3 * 0.4F).rect(i, radius);
        new RenderUtil(drawContext, f - f3 * 0.2F, f2 - f3, f3 * 0.4F, f3 * 2.0F).rect(i, radius);
    }

    private void renderBackground(DrawContext drawContext, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        float f4;
        float f5;
        if (f2 / f3 > 1.7777778F) {
            f4 = f2;
            f5 = f2 / 1.7777778F;
        } else {
            f5 = f3;
            f4 = f3 * 1.7777778F;
        }

        float f6 = (f2 - f4) / 2.0F;
        float f7 = (f3 - f5) / 2.0F;
        GuiRenderUtil.bindTexture(Texture.MENU_BACKGROUND);
        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f6, f7, f4, f5).frostedGlass(f, Radius.zero());
        }

        new RenderUtil(drawContext, f6, f7, f4, f5).spriteBlended(AnimatedIcon.BG, 0.0F, 0.0F, f4, f5, Theme.withAlpha(ColorUtil.WHITE, f));
    }

    private void renderMainMenu(DrawContext drawContext, int i, int i2, float f) {
        long l = System.nanoTime();
        if (this.lastPhysicsNanos == 0L) {
            this.lastPhysicsNanos = l;
            this.tickPhysics(i, i2);
        } else {
            if (l - this.lastPhysicsNanos > 250000000L) {
                this.logoRenderer.reset();
            }

            this.physicsAccumulator = this.physicsAccumulator + Math.min((float)(l - this.lastPhysicsNanos) / 1000000.0F, 100.0F);
            this.lastPhysicsNanos = l;

            for (int i3 = 0; this.physicsAccumulator >= 16.666666F && i3 < 5; i3++) {
                this.tickPhysics(i, i2);
                this.physicsAccumulator -= 16.666666F;
            }
        }

        if (this.ropeAlpha > 0.01F) {
            this.renderRope(drawContext, f);
        }

        float f2 = this.logoX;
        float f3 = this.logoY;
        long l2 = System.currentTimeMillis() - this.shakeStartTime;
        boolean flag = l2 >= 0L && l2 < 1500L;
        if (flag) {
            float f4 = (1.0F - (float)l2 / 1500.0F) * 2.2F;
            f2 += (float)(Math.random() * 2.0 - 1.0) * f4;
            f3 += (float)(Math.random() * 2.0 - 1.0) * f4;
        }

        float f5 = f2 - this.logoSize / 2.0F;
        float f6 = f3 - this.logoSize / 2.0F;
        boolean flag2 = this.logoState == MainMenuScreen.LogoState.GRAB
            || this.logoState == MainMenuScreen.LogoState.FREE
            || this.logoState == MainMenuScreen.LogoState.RELEASE;
        float f7 = 0.0F;
        if (flag2) {
            float f8 = this.logoSize * 0.164F;
            float f9 = -(this.grabOffsetX / f8) * 13.0F * this.grabAmount + this.logoVelocityX * 0.8F;
            f7 = Math.max(-18.0F, Math.min(18.0F, f9));
        }

        float f10 = Math.max(0.35F, 1.0F + this.squashX);
        float f11 = Math.max(0.35F, 1.0F + this.squashY);
        float f12 = f7 + (float)Math.toDegrees(this.logoRotation);
        if (GraphicsQuality.getCurrent().isHigh()) {
            this.logoRenderer.render(drawContext, f2, f3, f10, f11, f12, this.logoSize, i, i2, accentColor(), f);
        } else {
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2, f3, f10, f11);
            MatrixUtil.rotate(drawContext, f2, f3, f12);
            new RenderUtil(drawContext, f5, f6, this.logoSize, this.logoSize)
                .spinner(0.0F, 0.0F, this.logoSize, Theme.withAlpha(lightAccentColor(), f), Theme.withAlpha(darkAccentColor(), f));
            MatrixUtil.pop(drawContext);
        }

        if (flag) {
            this.renderHitSparkles(drawContext, this.logoX, this.logoY, (float)l2, f);
        }

        this.renderParticles(drawContext, f);
        float f13 = f * this.contentAnimation.get();
        float f14 = GraphicsQuality.getCurrent().isEffectsEnabled() ? 0.55F : 0.0F;
        Iterator iterator2 = this.primaryButtons.iterator();

        while (iterator2.hasNext()) {
            ((MenuButton)iterator2.next()).withAccentColor(accentColor()).withSlime(f14).render(drawContext, i, i2, f13);
        }

        this.layoutFooter();
        Iterator iterator3 = this.footerLinks.iterator();

        while (iterator3.hasNext()) {
            ((MainMenuScreen.FooterLink)iterator3.next()).render(drawContext, i, i2, f13);
        }
    }

    private void tickPhysics(int i, int i2) {
        this.homeX = this.logoBaseX + this.logoSize / 2.0F;
        this.homeY = this.logoBaseY + this.logoSize / 2.0F;
        long l = System.currentTimeMillis();
        switch (this.logoState) {
            case HIDDEN:
                this.logoX = this.homeX;
                this.logoY = this.homeY;
                this.logoVelocityX = 0.0F;
                this.logoVelocityY = 0.0F;
                this.grabOffsetX = 0.0F;
                this.grabOffsetY = 0.0F;
                this.logoRotation = 0.0F;
                break;
            case REACH:
                float f = this.logoX - this.homeX;
                float f2 = (float)Math.sqrt(f * f + this.logoY * this.logoY);
                float f3 = Math.min((float)(l - this.stateStartTime) / 240.0F, 1.0F);
                float f4 = 0.07F * f3 + 0.93F * (float)Math.pow(f3, 3.0);
                this.reachLength = 4.0F + (f2 - 4.0F) * f4;
                float f5 = this.logoSize * 0.55F * (1.0F - f3);
                float f6 = (float)Math.sin((float)(l - this.stateStartTime) * 0.04F) * f5;
                float f7 = this.logoX + f6 - this.homeX;
                float f8 = this.logoY;
                float f9 = (float)Math.sqrt(f7 * f7 + f8 * f8);
                if (f9 < 1.0F) {
                    f9 = 1.0F;
                }

                this.ropeTargetX = this.homeX + f7 / f9 * this.reachLength;
                this.ropeTargetY = f8 / f9 * this.reachLength;
                float f10 = this.ropeX[16] - this.logoX;
                float f11 = this.ropeY[16] - this.logoY;
                float f12 = (float)Math.sqrt(f10 * f10 + f11 * f11);
                if (f3 >= 1.0F && f12 < this.logoSize * 0.5F || l - this.stateStartTime > 300L) {
                    this.thinkFromIndex = 0;
                    this.thinkToIndex = 1;
                    this.thinkProgress = 0.0F;
                    this.wobblePhase = 0.0F;
                    this.logoState = MainMenuScreen.LogoState.THINK;
                    this.stateStartTime = l;
                }
                break;
            case THINK:
                this.thinkProgress += 0.045F;
                if (this.thinkProgress >= 1.0F) {
                    this.thinkProgress = 0.0F;
                    this.thinkFromIndex = this.thinkToIndex;
                    this.thinkToIndex = (this.thinkToIndex + 1) % 3;
                }

                float f13 = this.thinkProgress * this.thinkProgress * (3.0F - 2.0F * this.thinkProgress);
                float f14 = this.thinkOffsetsX[this.thinkFromIndex]
                    + (this.thinkOffsetsX[this.thinkToIndex] - this.thinkOffsetsX[this.thinkFromIndex]) * f13;
                float f15 = this.thinkOffsetsY[this.thinkFromIndex]
                    + (this.thinkOffsetsY[this.thinkToIndex] - this.thinkOffsetsY[this.thinkFromIndex]) * f13;
                this.wobblePhase += 0.05F;
                float f16 = (float)(Math.sin(this.wobblePhase) * 2.2 + Math.sin(this.wobblePhase * 1.7 + 1.3) * 1.1);
                float f17 = (float)(Math.cos(this.wobblePhase * 0.9) * 2.0 + Math.sin(this.wobblePhase * 2.3) * 0.9);
                this.ropeTargetX = this.logoX + f14 + f16;
                this.ropeTargetY = this.logoY + f15 + f17;
                if (l - this.stateStartTime > 1100L) {
                    double d = Math.random() * Math.PI * 2.0;
                    float f18 = 0.55F + (float)Math.random() * 0.25F;
                    this.grabOffsetX = (float)Math.cos(d) * this.logoSize * 0.164F * f18;
                    this.grabOffsetY = (float)Math.sin(d) * this.logoSize * 0.138F * f18;
                    this.logoState = MainMenuScreen.LogoState.GRAB;
                    this.stateStartTime = l;
                }
                break;
            case GRAB:
                this.ropeTargetX = this.logoX + this.grabOffsetX;
                this.ropeTargetY = this.logoY + this.grabOffsetY;
                if (l - this.stateStartTime > 120L) {
                    this.logoState = MainMenuScreen.LogoState.FREE;
                    this.logoVelocityX = 0.0F;
                    this.logoVelocityY = 0.0F;
                }
                break;
            case FREE:
                float f19 = this.logoSize * 0.164F;
                float f20 = this.logoSize * 0.138F;
                float f21 = GuiScale.getScaledWidth();
                float f22 = GuiScale.getScaledHeight();
                if (this.dragging && (i <= 1 || i >= f21 - 1.0F || i2 <= 1 || i2 >= f22 - 1.0F)) {
                    this.dragging = false;
                }

                if (this.dragging) {
                    float f23 = i + this.dragOffsetX;
                    float f24 = i2 + this.dragOffsetY;
                    float f25 = this.logoX + (f23 - this.logoX) * 0.55F;
                    float f26 = this.logoY + (f24 - this.logoY) * 0.55F;
                    this.logoVelocityX = f25 - this.logoX;
                    this.logoVelocityY = f26 - this.logoY;
                    float f27 = (float)Math.sqrt(this.logoVelocityX * this.logoVelocityX + this.logoVelocityY * this.logoVelocityY);
                    if (f27 > 15.0F) {
                        float f28 = 15.0F / f27;
                        this.logoVelocityX *= f28;
                        this.logoVelocityY *= f28;
                    }

                    this.logoX = f25;
                    this.logoY = f26;
                } else {
                    this.logoVelocityX = this.logoVelocityX + (this.homeX - this.logoX) * 0.017F;
                    this.logoVelocityY = this.logoVelocityY + (this.homeY - this.logoY) * 0.017F;
                    this.logoVelocityX *= 0.976F;
                    this.logoVelocityY *= 0.976F;
                    this.logoX = this.logoX + this.logoVelocityX;
                    this.logoY = this.logoY + this.logoVelocityY;
                    if (this.logoX < f19) {
                        this.logoX = f19;
                        this.onWallHit(1.0F, 0.0F, Math.abs(this.logoVelocityX));
                        this.logoVelocityX = -this.logoVelocityX * 0.5F;
                    } else if (this.logoX > f21 - f19) {
                        this.logoX = f21 - f19;
                        this.onWallHit(-1.0F, 0.0F, Math.abs(this.logoVelocityX));
                        this.logoVelocityX = -this.logoVelocityX * 0.5F;
                    }

                    if (this.logoY < f20) {
                        this.logoY = f20;
                        this.onWallHit(0.0F, 1.0F, Math.abs(this.logoVelocityY));
                        this.logoVelocityY = -this.logoVelocityY * 0.5F;
                    } else if (this.logoY > f22 - f20) {
                        this.logoY = f22 - f20;
                        this.onWallHit(0.0F, -1.0F, Math.abs(this.logoVelocityY));
                        this.logoVelocityY = -this.logoVelocityY * 0.5F;
                    }
                }

                this.ropeTargetX = this.logoX + this.grabOffsetX;
                this.ropeTargetY = this.logoY + this.grabOffsetY;
                break;
            case RELEASE:
                float f29 = Math.min((float)(l - this.stateStartTime) / 650.0F, 1.0F);
                float f30 = f29 * f29 * f29 * (f29 * (f29 * 6.0F - 15.0F) + 10.0F);
                this.logoX = this.releaseStartX + (this.homeX - this.releaseStartX) * f30;
                this.logoY = this.releaseStartY + (this.homeY - this.releaseStartY) * f30;
                this.logoVelocityX = 0.0F;
                this.logoVelocityY = 0.0F;
                this.logoRotation *= 0.85F;
                this.grabOffsetX = this.releaseOffsetX * (1.0F - f30);
                this.grabOffsetY = this.releaseOffsetY * (1.0F - f30);
                this.ropeTargetX = this.logoX + this.grabOffsetX;
                this.ropeTargetY = this.logoY + this.grabOffsetY;
                if (f29 >= 1.0F) {
                    this.logoX = this.homeX;
                    this.logoY = this.homeY;
                    this.logoState = MainMenuScreen.LogoState.HIDDEN;
                    this.ropeInitialized = false;
                }
        }

        this.squashVelocityX = this.squashVelocityX + (-this.squashX * 0.3F - this.squashVelocityX * 0.32F);
        this.squashX = this.squashX + this.squashVelocityX;
        this.squashVelocityY = this.squashVelocityY + (-this.squashY * 0.3F - this.squashVelocityY * 0.32F);
        this.squashY = this.squashY + this.squashVelocityY;
        this.tickParticles();
        this.logoRenderer.update(i, i2, this.logoX, this.logoY, this.logoSize, this.logoState == MainMenuScreen.LogoState.HIDDEN);
        this.ropeAlpha = this.ropeAlpha + ((this.logoState == MainMenuScreen.LogoState.HIDDEN ? 0.0F : 1.0F) - this.ropeAlpha) * 0.15F;
        this.grabAmount = this.grabAmount
            + ((this.logoState != MainMenuScreen.LogoState.GRAB && this.logoState != MainMenuScreen.LogoState.FREE ? 0.0F : 1.0F) - this.grabAmount) * 0.12F;
        if (this.logoState != MainMenuScreen.LogoState.HIDDEN) {
            this.simulateRope();
        }
    }

    private void onWallHit(float f, float f2, float f3) {
        this.logoRenderer.addEnergy(f3);
        if (!(f3 < 0.8F)) {
            float f4 = Math.min(f3 / 16.0F, 1.0F);
            if (f != 0.0F) {
                this.squashX -= 0.33F * f4;
                this.squashY += 0.33F * f4;
            } else {
                this.squashY -= 0.33F * f4;
                this.squashX += 0.33F * f4;
            }

            float f5 = this.logoSize * 0.164F;
            float f6 = this.logoSize * 0.138F;
            this.spawnParticles(this.logoX - f * f5, this.logoY - f2 * f6, f, f2, f4);
        }
    }

    private void spawnParticles(float f, float f2, float f3, float f4, float f5) {
        int i = 3 + (int)(f5 * 5.0F);
        float f6 = -f4;

        for (int i2 = 0; i2 < i; i2++) {
            int i3 = this.nextParticleIndex;
            this.nextParticleIndex = (this.nextParticleIndex + 1) % 64;
            float f7 = (1.5F + (float)Math.random() * 3.0F) * (0.5F + f5);
            float f8 = (float)(Math.random() * 2.0 - 1.0);
            this.particleX[i3] = f;
            this.particleY[i3] = f2;
            this.particleVelocityX[i3] = f3 * f7 + f6 * f8 * f7 * 0.85F;
            this.particleVelocityY[i3] = f4 * f7 + f3 * f8 * f7 * 0.85F;
            float f9 = 15.0F + (float)Math.random() * 13.0F;
            this.particleLife[i3] = f9;
            this.particleMaxLife[i3] = f9;
        }
    }

    private void tickParticles() {
        for (int i = 0; i < 64; i++) {
            if (!(this.particleLife[i] <= 0.0F)) {
                this.particleLife[i]--;
                this.particleVelocityX[i] = this.particleVelocityX[i] * 0.9F;
                this.particleVelocityY[i] = this.particleVelocityY[i] * 0.9F + 0.18F;
                this.particleX[i] = this.particleX[i] + this.particleVelocityX[i];
                this.particleY[i] = this.particleY[i] + this.particleVelocityY[i];
            }
        }
    }

    private void renderParticles(DrawContext drawContext, float f) {
        for (int i = 0; i < 64; i++) {
            if (!(this.particleLife[i] <= 0.0F)) {
                float f2 = this.particleLife[i] / this.particleMaxLife[i];
                float f3 = 1.1F + 1.8F * f2;
                int i2 = Theme.withAlpha(ColorUtil.interpolate(accentColor(), -1, 0.3F), f * f2);
                new RenderUtil(drawContext, this.particleX[i] - f3, this.particleY[i] - f3, f3 * 2.0F, f3 * 2.0F).rect(i2, Radius.all(f3));
            }
        }
    }

    private void simulateRope() {
        float f = this.homeX;
        float f2 = this.ropeTargetX - f;
        float f3 = this.ropeTargetY - 0.0F;
        float f4 = (float)Math.sqrt(f2 * f2 + f3 * f3);
        if (f4 < 1.0F) {
            f4 = 1.0F;
        }

        float f5 = f4;
        if (this.logoState == MainMenuScreen.LogoState.REACH) {
            f5 = Math.min(f4, this.reachLength);
        }

        float f6 = f5 * 1.06F;
        if (this.ropeLength <= 0.0F) {
            this.ropeLength = f6;
        }

        float f7 = this.logoState == MainMenuScreen.LogoState.REACH ? 0.3F : 0.6F;
        float f8 = this.logoState == MainMenuScreen.LogoState.REACH ? 0.3F : 0.18F;
        this.ropeLength = this.ropeLength + (f6 - this.ropeLength) * (f6 > this.ropeLength ? f7 : f8);
        float f9 = this.ropeLength / 16;
        if (!this.ropeInitialized) {
            float f10 = (float)Math.sqrt(f2 * f2 + f3 * f3);
            if (f10 < 1.0F) {
                f10 = 1.0F;
            }

            float f11 = f2 / f10;
            float f12 = f3 / f10;
            float f13 = this.logoState == MainMenuScreen.LogoState.REACH ? Math.min(f10, this.reachLength) : f10;

            for (int i = 0; i <= 16; i++) {
                float f14 = f13 * ((float)i / 16);
                this.ropeX[i] = f + f11 * f14;
                this.ropeY[i] = 0.0F + f12 * f14;
                this.ropePrevX[i] = this.ropeX[i];
                this.ropePrevY[i] = this.ropeY[i];
            }

            this.ropeTipVelocityX = 0.0F;
            this.ropeTipVelocityY = 0.0F;
            this.ropeInitialized = true;
        }

        float f15 = Math.min(f4 / Math.max(this.ropeLength, 1.0F), 1.0F);
        float f16 = 0.1F * (1.0F - 0.4F * f15);

        for (int i2 = 0; i2 < 2; i2++) {
            for (int i3 = 1; i3 <= 16; i3++) {
                float f17 = (this.ropeX[i3] - this.ropePrevX[i3]) * 0.9F;
                float f18 = (this.ropeY[i3] - this.ropePrevY[i3]) * 0.9F;
                this.ropePrevX[i3] = this.ropeX[i3];
                this.ropePrevY[i3] = this.ropeY[i3];
                this.ropeX[i3] = this.ropeX[i3] + f17;
                this.ropeY[i3] = this.ropeY[i3] + (f18 + f16 / 2.0F);
            }

            float f19 = (this.ropeTargetX - this.ropeX[16]) * 0.17F;
            float f20 = (this.ropeTargetY - this.ropeY[16]) * 0.17F;
            this.ropeTipVelocityX = (this.ropeTipVelocityX + f19) * 0.78F;
            this.ropeTipVelocityY = (this.ropeTipVelocityY + f20) * 0.78F;
            this.ropeX[16] = this.ropeX[16] + this.ropeTipVelocityX;
            this.ropeY[16] = this.ropeY[16] + this.ropeTipVelocityY;
            this.ropePrevX[16] = this.ropeX[16] - this.ropeTipVelocityX;
            this.ropePrevY[16] = this.ropeY[16] - this.ropeTipVelocityY;

            for (int i4 = 0; i4 < 6; i4++) {
                this.ropeX[0] = f;
                this.ropeY[0] = 0.0F;

                for (int i5 = 0; i5 < 16; i5++) {
                    float f21 = this.ropeX[i5 + 1] - this.ropeX[i5];
                    float f22 = this.ropeY[i5 + 1] - this.ropeY[i5];
                    float f23 = (float)Math.sqrt(f21 * f21 + f22 * f22);
                    if (f23 < 0.001F) {
                        f23 = 0.001F;
                    }

                    float f24 = (f9 - f23) / f23 * 0.5F * 0.55F;
                    float f25 = f21 * f24;
                    float f26 = f22 * f24;
                    if (i5 != 0) {
                        this.ropeX[i5] = this.ropeX[i5] - f25;
                        this.ropeY[i5] = this.ropeY[i5] - f26;
                    }

                    float f27 = i5 + 1 == 16 ? 0.6F : 1.0F;
                    this.ropeX[i5 + 1] = this.ropeX[i5 + 1] + f25 * f27;
                    this.ropeY[i5 + 1] = this.ropeY[i5 + 1] + f26 * f27;
                }
            }
        }

        this.ropeX[0] = f;
        this.ropeY[0] = 0.0F;
    }

    private void renderRope(DrawContext drawContext, float f) {
        int i = this.buildRopeSpline();
        if (i >= 2) {
            float f2 = (float)(System.currentTimeMillis() % 2000L) / 2000.0F;
            RopeRenderer.draw(drawContext, this.splineX, this.splineY, i, 4.0F, 1.8F, 4.0F, 5.0F, accentColor(), f * this.ropeAlpha, f2);
        }
    }

    private int buildRopeSpline() {
        for (int i = 0; i <= 16; i++) {
            this.smoothedRopeX[i] = this.ropeX[i];
            this.smoothedRopeY[i] = this.ropeY[i];
        }

        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = 1; i3 < 16; i3++) {
                this.smoothedRopeX[i3] = this.smoothedRopeX[i3]
                    + ((this.smoothedRopeX[i3 - 1] + this.smoothedRopeX[i3 + 1]) * 0.5F - this.smoothedRopeX[i3]) * 0.45F;
                this.smoothedRopeY[i3] = this.smoothedRopeY[i3]
                    + ((this.smoothedRopeY[i3 - 1] + this.smoothedRopeY[i3 + 1]) * 0.5F - this.smoothedRopeY[i3]) * 0.45F;
            }
        }

        int i4 = 0;

        for (int i5 = 0; i5 < 16; i5++) {
            float f = this.smoothedRopeX[Math.max(0, i5 - 1)];
            float f2 = this.smoothedRopeY[Math.max(0, i5 - 1)];
            float f3 = this.smoothedRopeX[i5];
            float f4 = this.smoothedRopeY[i5];
            float f5 = this.smoothedRopeX[i5 + 1];
            float f6 = this.smoothedRopeY[i5 + 1];
            float f7 = this.smoothedRopeX[Math.min(16, i5 + 2)];
            float f8 = this.smoothedRopeY[Math.min(16, i5 + 2)];

            for (int i6 = 0; i6 < 10; i6++) {
                float f9 = i6 / 10.0F;
                this.splineX[i4] = catmullRom(f, f3, f5, f7, f9);
                this.splineY[i4] = catmullRom(f2, f4, f6, f8, f9);
                i4++;
            }
        }

        this.splineX[i4] = this.smoothedRopeX[16];
        this.splineY[i4] = this.smoothedRopeY[16];
        return i4 + 1;
    }

    private static float catmullRom(float f, float f2, float f3, float f4, float f5) {
        float f6 = f5 * f5;
        float f7 = f6 * f5;
        return 0.5F
            * (
                2.0F * f2
                    + (-f + f3) * f5
                    + (2.0F * f - 5.0F * f2 + 4.0F * f3 - f4) * f6
                    + (-f + 3.0F * f2 - 3.0F * f3 + f4) * f7
            );
    }

    void performAction(MainMenuScreen.MenuAction menuAction) {
        switch (menuAction) {
            case SINGLEPLAYER:
                this.openSingleplayer();
                return;
            case MULTIPLAYER:
                this.openMultiplayer();
                return;
            case ACCOUNTS:
                this.openAccounts();
                return;
            case SETTINGS:
                mc.setScreen(new SettingsScreen(this));
                return;
            case QUIT:
                mc.scheduleStop();
        }
    }

    public void closeAccounts() {
        if (this.accountsOpen) {
            this.accountsOpen = false;
            this.contentAnimation.forceValue(0.0);
            this.contentAnimation.animate(1.0, 0.34, Easings.QUAD_OUT);
        }
    }

    private void openAccounts() {
        this.sidePanel.cancelDrag();
        this.accountsOpen = true;
        this.contentAnimation.forceValue(0.0);
        this.contentAnimation.animate(1.0, 0.34, Easings.QUAD_OUT);
        this.accountsPanel.resetAppearTime();
    }

    private void openSingleplayer() {
        mc.setScreen(new SelectWorldScreen(this));
    }

    private void openMultiplayer() {
        mc.setScreen(new MultiplayerScreen(this));
    }

    private Click scaleMouseInput(Click click) {
        return new Click(GuiScale.toScaled(click.x()), GuiScale.toScaled(click.y()), click.buttonInfo());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        Click click2 = this.scaleMouseInput(click);
        double d = click2.x();
        double d2 = click2.y();
        if (!this.welcomeShown && !(this.welcomeAnimation.get() > 0.05)) {
            if (GuiScale.isTooSmall()) {
                return false;
            }

            if (this.tour.isActive() && this.tour.mouseClicked(d, d2, this.buildTourState())) {
                return true;
            }

            boolean flag2 = click2.button() == 0;
            if (!this.accountsOpen) {
                if (flag2) {
                    if (this.changelogPanel.mouseClicked(d, d2)) {
                        return true;
                    }
                } else if (this.changelogPanel.isOpen()) {
                    return true;
                }
            }

            if (flag2 && !this.accountsOpen && this.sidePanel.mouseClicked(d, d2)) {
                return true;
            }

            if (this.accountsOpen) {
                return this.accountsPanel.mouseClicked(click2, flag);
            }

            float f = this.logoState == MainMenuScreen.LogoState.FREE ? this.logoX : this.logoBaseX + this.logoSize * 0.5F;
            float f2 = this.logoState == MainMenuScreen.LogoState.FREE ? this.logoY : this.logoBaseY + this.logoSize * 0.5F;
            if ((this.logoState == MainMenuScreen.LogoState.HIDDEN || this.logoState == MainMenuScreen.LogoState.FREE)
                && Math.abs(d - f) <= this.logoSize * 0.164F * 1.35F
                && Math.abs(d2 - f2) <= this.logoSize * 0.138F * 1.35F) {
                if (this.logoState != MainMenuScreen.LogoState.HIDDEN) {
                    this.dragging = true;
                    this.dragOffsetX = this.logoX - (float)d;
                    this.dragOffsetY = this.logoY - (float)d2;
                    this.dragStartX = (float)d;
                    this.dragStartY = (float)d2;
                    return true;
                }

                this.logoX = this.logoBaseX + this.logoSize / 2.0F;
                this.logoY = this.logoBaseY + this.logoSize / 2.0F;
                this.logoVelocityX = 0.0F;
                this.logoVelocityY = 0.0F;
                this.logoRotation = 0.0F;
                this.grabOffsetX = 0.0F;
                this.grabOffsetY = 0.0F;

                for (int i = 0; i < 3; i++) {
                    double d3 = Math.random() * Math.PI * 2.0;
                    float f3 = 0.55F + (float)Math.random() * 0.25F;
                    this.thinkOffsetsX[i] = (float)Math.cos(d3) * this.logoSize * 0.164F * f3;
                    this.thinkOffsetsY[i] = (float)Math.sin(d3) * this.logoSize * 0.138F * f3;
                }

                this.thinkFromIndex = 0;
                this.thinkToIndex = 1;
                this.thinkProgress = 0.0F;
                this.wobblePhase = 0.0F;
                this.ropeTargetX = this.logoX;
                this.ropeTargetY = 4.0F;
                this.reachLength = 4.0F;
                this.ropeLength = 0.0F;
                this.ropeTipVelocityX = 0.0F;
                this.ropeTipVelocityY = 0.0F;
                this.ropeInitialized = false;
                this.logoState = MainMenuScreen.LogoState.REACH;
                this.stateStartTime = System.currentTimeMillis();
                return true;
            } else {
                for (MenuButton menuButton : this.primaryButtons) {
                    if (menuButton.mouseClicked(d, d2)) {
                        return true;
                    }
                }

                for (MainMenuScreen.FooterLink footerLink : this.footerLinks) {
                    if (footerLink.mouseClicked(d, d2)) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            if (this.welcomeShown) {
                this.dismissWelcome();
            }

            return true;
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && this.sidePanel.mouseReleased()) {
            return true;
        }

        Click click2 = this.scaleMouseInput(click);
        this.changelogPanel.mouseReleased();
        if (this.accountsOpen) {
            this.accountsPanel.mouseReleased(click2);
        }

        if (this.dragging) {
            this.dragging = false;
            float f = (float)click2.x() - this.dragStartX;
            float f2 = (float)click2.y() - this.dragStartY;
            if (f * f + f2 * f2 < 25.0F) {
                this.logoState = MainMenuScreen.LogoState.RELEASE;
                this.stateStartTime = System.currentTimeMillis();
                this.releaseStartX = this.logoX;
                this.releaseStartY = this.logoY;
                this.releaseOffsetX = this.grabOffsetX;
                this.releaseOffsetY = this.grabOffsetY;
            }

            return true;
        } else {
            return super.mouseReleased(click);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        Click click2 = this.scaleMouseInput(click);
        if (this.sidePanel.mouseDragged(click2.x())) {
            return true;
        } else if (this.changelogPanel.isOpen() && this.changelogPanel.mouseDragged(click2.x(), click2.y())) {
            return true;
        } else if (this.accountsOpen && this.accountsPanel.mouseDragged(click2)) {
            return true;
        } else {
            return this.dragging ? true : super.mouseDragged(click, d, d2);
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (GuiScale.isTooSmall()) {
            return false;
        } else if (this.changelogPanel.isOpen() && this.changelogPanel.mouseScrolled(d4)) {
            return true;
        } else if (!this.changelogPanel.isOpen() && !this.accountsOpen && this.sidePanel.mouseScrolled(GuiScale.toScaled(d), GuiScale.toScaled(d2), d4)) {
            return true;
        } else {
            return this.accountsOpen && this.accountsPanel.mouseScrolled(GuiScale.toScaled(d), GuiScale.toScaled(d2), d4)
                ? true
                : super.mouseScrolled(d, d2, d3, d4);
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (this.welcomeShown) {
            this.dismissWelcome();
            return true;
        }

        if (this.changelogPanel.isOpen()) {
            if (keyInput.key() == 256) {
                this.changelogPanel.close();
            }

            return true;
        } else if (this.accountsOpen && this.accountsPanel.keyPressed(keyInput)) {
            return true;
        } else if (keyInput.key() == 256 && this.accountsOpen) {
            this.closeAccounts();
            return true;
        } else {
            return super.keyPressed(keyInput);
        }
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        return this.accountsOpen && this.accountsPanel.charTyped(charInput) ? true : super.charTyped(charInput);
    }

    @Override
    public void removed() {
        this.sidePanel.cancelDrag();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    final class FooterLink {
        private final MainMenuScreen.MenuAction action;
        private final Animation hoverAnimation = new Animation();
        private final Animation clickAnimation = new Animation();
        private final ShimmerText gradientText = new ShimmerText();
        private final IconHoverAnimator iconRenderer = new IconHoverAnimator();
        private float x;
        private float y;
        private float width;
        private float height;

        FooterLink(MainMenuScreen.MenuAction menuAction) {
            this.action = menuAction;
        }

        float getPreferredWidth() {
            if (Fonts.INTER_MEDIUM != null) {
                float f = Fonts.INTER_MEDIUM.getWidth(this.action.getDisplayName(), 6.5F);
                return 10.0F + f;
            } else {
                float f2 = this.action.getDisplayName().length() * 6.5F * 0.6F;
                return 10.0F + f2;
            }
        }

        void setBounds(float f, float f2, float f3, float f4) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
        }

        void render(DrawContext drawContext, int i, int i2, float f) {
            this.hoverAnimation.update();
            this.clickAnimation.update();
            this.clickAnimation.animate(0.0, 0.22, Easings.BACK_OUT);
            boolean flag = MainMenuScreen.this.isHovered(i, i2, this.x, this.y, this.width, this.height);
            this.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.18, Easings.BACK_OUT);
            float f2 = this.hoverAnimation.get();
            float f3 = this.clickAnimation.get();
            float f4 = this.x + this.width / 2.0F;
            float f5 = (f4 - i) / 42.0F;
            float f6 = Math.max(0.0F, 1.0F - Math.abs(f5)) * (1.0F - f2);
            float f7 = Math.max(-1.0F, Math.min(1.0F, f5)) * f6 * 3.0F;
            int i3 = this.action == MainMenuScreen.MenuAction.QUIT ? -2075297 : ColorUtil.WHITE;
            int i4 = Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), i3, Math.max(f2, f6 * 0.35F)), f);
            boolean flag2 = f2 > 0.001F || f6 > 0.001F || f3 > 0.001F;
            if (flag2) {
                MatrixUtil.push(drawContext);
                MatrixUtil.translate(drawContext, f7, 0.0F);
                MatrixUtil.scale(drawContext, f4, this.y + this.height / 2.0F, 1.0F + 0.14F * f2 + 0.05F * f6 - 0.09F * f3);
            }

            RenderUtil renderUtil = new RenderUtil(drawContext, this.x, this.y, this.width, this.height);
            this.iconRenderer.render(drawContext, this.action.icon, this.x, this.y + (this.height - 6.0F) / 2.0F, 6.0F, i4, f2);
            String string = this.action.getDisplayName();
            if (f2 > 0.001F) {
                string = this.gradientText
                    .applyThemed(
                        Fonts.INTER_MEDIUM, string, 6.5F, this.action == MainMenuScreen.MenuAction.QUIT ? -2075297 : MainMenuScreen.accentColor(), i4, f2
                    );
            }

            renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string, 10.0F, 0.0F, i4, 6.5F);
            if (flag2) {
                MatrixUtil.pop(drawContext);
            }
        }

        float[] getBounds() {
            return new float[]{this.x, this.y, this.width, this.height};
        }

        boolean mouseClicked(double d, double d2) {
            if (MainMenuScreen.this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
                this.clickAnimation.forceValue(1.0);
                MainMenuScreen.this.performAction(this.action);
                return true;
            } else {
                return false;
            }
        }
    }

    enum LogoState {
        HIDDEN,
        REACH,
        THINK,
        GRAB,
        FREE,
        RELEASE;
    }

    public enum MenuAction {
        SINGLEPLAYER(Icon.MOUNTAIN_SUN, "menu.singleplayer", "Singleplayer"),
        MULTIPLAYER(Icon.SERVER, "menu.multiplayer", "Multiplayer"),
        ACCOUNTS(Icon.USERS, "menu.accounts", "Accounts"),
        SETTINGS(Icon.SETTINGS, "menu.settings", "Settings"),
        QUIT(Icon.CLOSE, "menu.quit", "Quit");

        final Icon icon;
        final String translationKey;
        final String fallbackName;

        MenuAction(Icon icon2, String string, String string2) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallbackName = string2;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.fallbackName);
        }
    }
}
