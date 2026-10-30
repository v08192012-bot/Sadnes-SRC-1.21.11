package tech.sadnes.client.ui.clickgui.component;

import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.ThemePrefs;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.shader.ShapeBlur;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.CheckboxSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.SubscriptionUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class ProfilePopup extends Component {
    private static final float WIDTH = 110.0F;
    private static final float PADDING = 8.0F;
    private static final float OFFSET = 8.0F;
    private static final float TOP_PADDING = 6.0F;
    private static final float AVATAR_SIZE = 22.0F;
    private static final float HEADER_HEIGHT = 28.0F;
    private static final float INFO_ROW_HEIGHT = 12.0F;
    private static final float BUTTON_HEIGHT = 16.0F;
    private static final float SETTING_HEIGHT = 15.0F;
    private static final float SLIDER_HEIGHT = 24.0F;
    private static final float SEPARATOR_HEIGHT = 8.0F;
    private static final float CORNER_RADIUS = 8.0F;
    private static final float SCREEN_MARGIN = 4.0F;
    private final Animation openAnimation = new Animation();
    private final Vector2f popupPosition = new Vector2f();
    private final ColorSetting accent = new ColorSetting("settings.accent", Icon.DROPLET, false, Theme.getPrimary());
    private final ColorSetting background;
    private final ColorSetting text;
    private final CheckboxSetting tooltips;
    private final CheckboxSetting shadows;
    private final ModeSetting language;
    private final NumberSetting scale;
    private final ModeSetting blurQuality;
    private boolean open;
    private boolean visible;
    private float popupWidth;
    private float popupHeight;
    private float chatButtonX;
    private float chatButtonY;
    private float chatButtonWidth;

    public ProfilePopup() {
        this.accent.setOnChange(() -> Theme.setAccent(this.accent.getValue()));
        this.background = new ColorSetting("settings.background", Icon.PALETTE, false, Theme.getBackground());
        this.background.setOnChange(() -> Theme.setBackground(this.background.getValue()));
        this.text = new ColorSetting("settings.text", Icon.FONT, false, Theme.getTextColor());
        this.text.setOnChange(() -> Theme.setTextColor(this.text.getValue()));
        this.tooltips = new CheckboxSetting("settings.tooltips", HoverTooltip.isEnabled());
        this.tooltips.setOnChange(() -> HoverTooltip.setEnabled(Boolean.TRUE.equals(this.tooltips.getValue())));
        this.shadows = new CheckboxSetting("settings.shadows", Theme.isShadowEnabled());
        this.shadows.setOnChange(() -> Theme.setShadowEnabled(Boolean.TRUE.equals(this.shadows.getValue())));
        String[] strings = Language.LOCALES.stream().map(Language.LocaleEntry::nativeName).toArray(String[]::new);
        this.language = new ModeSetting("settings.language", strings);
        this.language.setOnChange(() -> {
            String string = Language.getLocale();
            Language.setLocale(Language.getCodeByNativeName(this.language.getValue()));
            if (!string.equals(Language.getLocale())) {
                Sadnes.getInstance().getFeatureManager().refreshSearchIndex();
            }
        });
        this.language.setValue(Language.getNativeName(Language.getLocale()));
        this.scale = new NumberSetting("settings.scale", GuiScale.getScale() * 100.0F, 75.0F, 150.0F, 5.0F, Unit.PERCENT);
        this.blurQuality = new ModeSetting("settings.blur_quality", new String[]{"High", "Medium", "Low"});
        this.blurQuality.setValue(this.levelToBlurQuality(ShapeBlur.downscale));
        this.blurQuality.setOnChange(() -> ShapeBlur.downscale = this.blurQualityToLevel(this.blurQuality.getValue()));
        this.language.setIcon(Icon.LANGUAGE);
        this.tooltips.setIcon(Icon.LIGHTBULB);
        this.shadows.setIcon(Icon.CLONE);
        this.scale.setIcon(Icon.MAXIMIZE);
        this.blurQuality.setIcon(Icon.WAND_SPARKLES);
    }

    @Override
    public void resize(int i, int i2) {
        this.open = false;
        this.visible = false;
        this.openAnimation.forceValue(0.0);
        this.syncSettings();
        this.accent.close();
        this.background.close();
        this.text.close();
        this.language.close();
        this.scale.close();
        this.blurQuality.close();
    }

    @Override
    public void init(int i, int i2) {
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isAnyOverlayOpen() {
        return this.accent.isOverlayOpen()
            || this.background.isOverlayOpen()
            || this.text.isOverlayOpen()
            || this.language.isOverlayOpen()
            || this.blurQuality.isOverlayOpen();
    }

    public float getOpenProgress() {
        return this.openAnimation.get();
    }

    public void toggle() {
        if (this.open) {
            this.close();
        } else {
            this.openPopup();
        }
    }

    public void openPopup() {
        this.open = true;
        this.syncSettings();
        this.accent.close();
        this.background.close();
        this.text.close();
        this.language.close();
        this.scale.close();
        this.blurQuality.close();
        this.openAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
    }

    @Override
    public void close() {
        this.open = false;
        this.accent.close();
        this.background.close();
        this.text.close();
        this.language.close();
        this.scale.close();
        this.blurQuality.close();
        this.openAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
    }

    private void syncSettings() {
        this.accent.setValue(Theme.getPrimary());
        this.accent.alphaAnimation.forceValue(1.0);
        this.accent.alphaMultiplier = 1.0F;
        if (Theme.hasCustomBackground()) {
            this.background.setValue(Theme.getBackground());
        }

        this.background.alphaAnimation.forceValue(1.0);
        this.background.alphaMultiplier = 1.0F;
        if (Theme.hasCustomText()) {
            this.text.setValue(Theme.getTextColor());
        }

        this.text.alphaAnimation.forceValue(1.0);
        this.text.alphaMultiplier = 1.0F;
        this.tooltips.setValue(HoverTooltip.isEnabled());
        this.tooltips.alphaAnimation.forceValue(1.0);
        this.tooltips.alphaMultiplier = 1.0F;
        this.shadows.setValue(Theme.isShadowEnabled());
        this.shadows.alphaAnimation.forceValue(1.0);
        this.shadows.alphaMultiplier = 1.0F;
        this.language.setValue(Language.getNativeName(Language.getLocale()));
        this.language.alphaAnimation.forceValue(1.0);
        this.language.alphaMultiplier = 1.0F;
        this.scale.setValue(Float.valueOf(GuiScale.getScale() * 100.0F));
        this.scale.alphaAnimation.forceValue(1.0);
        this.scale.alphaMultiplier = 1.0F;
        this.blurQuality.setValue(this.levelToBlurQuality(ShapeBlur.downscale));
        this.blurQuality.alphaAnimation.forceValue(1.0);
        this.blurQuality.alphaMultiplier = 1.0F;
    }

    public void renderPopup(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, int i3, int i4) {
        this.openAnimation.update();
        this.openAnimation.animate(this.open ? 1.0 : 0.0, this.open ? 0.3 : 0.2, this.open ? Easings.BACK_OUT : Easings.QUAD_IN);
        float f5 = this.openAnimation.get();
        float f6 = this.getAlpha();
        if (!(f5 <= 0.001F) && !(f6 <= 0.001F)) {
            this.visible = true;
            float f7 = this.getPopupHeight();
            this.popupWidth = 110.0F;
            this.popupHeight = f7;
            float f8 = f2 + 8.0F;
            float f9 = f3 - 110.0F - 8.0F;
            float f10 = f8 + 110.0F <= i3 - 4.0F ? f8 : f9;
            float f11 = this.clamp(f10, 4.0F, Math.max(4.0F, i3 - 4.0F - 110.0F));
            float f12 = this.clamp(f4 - f7 * 0.5F, 4.0F, Math.max(4.0F, i4 - 4.0F - f7));
            this.popupPosition.set(f11, f12);
            float f13 = Math.min(f5, 1.0F) * f6;
            float f14 = Math.min(f5, 1.0F);
            float f15 = 0.9F + 0.1F * f5;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f11 + 110.0F / 2.0F, f12 + f7 / 2.0F, f15);
            GuiRenderUtil.drawPopup(drawContext, f11, f12, 110.0F, f7, f13, 8.0F);
            this.renderContent(drawContext, f11, f12, 110.0F, f13, f14, i, i2, f);
            if (!this.scale.isDragging()) {
                GuiScale.setScale(this.scale.getValue() / 100.0F);
            }

            MatrixUtil.pop(drawContext);
            this.language.renderOverlay(drawContext, i, i2, f);
            this.blurQuality.renderOverlay(drawContext, i, i2, f);
            this.accent.renderOverlay(drawContext, i, i2, f);
            this.background.renderOverlay(drawContext, i, i2, f);
            this.text.renderOverlay(drawContext, i, i2, f);
        } else {
            this.visible = false;
            this.popupWidth = this.popupHeight = 0.0F;
        }
    }

    private void renderContent(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i, int i2, float f6) {
        float f7 = f + 8.0F;
        float f8 = f3 - 16.0F;
        float f9 = f2 + 6.0F;
        this.renderProfileHeader(drawContext, f7, f9, f8, f4);
        float f10 = f9 + 28.0F;
        this.renderSeparator(drawContext, f7, f10 + 4.0F, f8, f4);
        float f11 = f10 + 8.0F;
        this.renderInfoRow(drawContext, f7, f11, f8, Icon.ID_CARD, Language.translate("profile.uid", "UID"), String.valueOf(Profile.getUid()), f4);
        float f12 = f11 + 12.0F;
        this.renderInfoRow(
            drawContext, f7, f12, f8, Icon.CROWN, Language.translate("profile.subscription", "Subscription"), SubscriptionUtil.getSubscriptionText(), f4
        );
        float f13 = f12 + 12.0F;
        this.renderSeparator(drawContext, f7, f13 + 4.0F, f8, f4);
        float f14 = f13 + 8.0F;
        this.language.alphaMultiplier = f5;
        this.language.position.set(f7, f14);
        this.language.size.set(f8, 15.0F);
        this.language.render(drawContext, i, i2, f6);
        float f15 = f14 + 15.0F;
        this.tooltips.alphaMultiplier = f5;
        this.tooltips.position.set(f7, f15);
        this.tooltips.size.set(f8, 15.0F);
        this.tooltips.render(drawContext, i, i2, f6);
        float f16 = f15 + 15.0F;
        this.shadows.alphaMultiplier = f5;
        this.shadows.position.set(f7, f16);
        this.shadows.size.set(f8, 15.0F);
        this.shadows.render(drawContext, i, i2, f6);
        float f17 = f16 + 15.0F;
        this.scale.alphaMultiplier = f5;
        this.scale.position.set(f7, f17);
        this.scale.size.set(f8, 24.0F);
        this.scale.render(drawContext, i, i2, f6);
        float f18 = f17 + 24.0F;
        this.blurQuality.alphaMultiplier = f5;
        this.blurQuality.position.set(f7, f18);
        this.blurQuality.size.set(f8, 15.0F);
        this.blurQuality.render(drawContext, i, i2, f6);
        float f19 = f18 + 15.0F;
        this.accent.alphaMultiplier = f5;
        this.accent.position.set(f7, f19);
        this.accent.size.set(f8, 15.0F);
        this.accent.render(drawContext, i, i2, f6);
        float f20 = f19 + 15.0F;
        this.background.alphaMultiplier = f5;
        this.background.position.set(f7, f20);
        this.background.size.set(f8, 15.0F);
        this.background.render(drawContext, i, i2, f6);
        float f21 = f20 + 15.0F;
        this.text.alphaMultiplier = f5;
        this.text.position.set(f7, f21);
        this.text.size.set(f8, 15.0F);
        this.text.render(drawContext, i, i2, f6);
    }

    private void renderProfileHeader(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = f2 + 3.0F;
        Identifier identifier = AvatarTextureLoader.getTexture(Profile.getAvatarUrl());
        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
        } else {
            GuiRenderUtil.bindTexture(Texture.AVATAR_DEFAULT);
        }

        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f, f5, 22.0F, 22.0F).frostedGlass(f4, Radius.all(11.0F));
        }

        float f6 = f + 22.0F + 6.0F;
        float f7 = f3 - 22.0F - 6.0F;
        int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        int i2 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f4);
        float f8 = f5 + (22.0F - (7.0F + 2.0F + 6.0F)) / 2.0F;
        new RenderUtil(drawContext, f6, f2, f7, 28.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, Profile.getUsername(), 0.0F, f8 - f2, f7, i, 7.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, this.getRoleName(), 0.0F, f8 - f2 + 7.0F + 2.0F, f7, i2, 6.0F);
    }

    private void renderInfoRow(DrawContext drawContext, float f, float f2, float f3, Icon icon2, String string, String string2, float f4) {
        int i = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4);
        int i2 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f4);
        new RenderUtil(drawContext, f, f2, 7.0F, 12.0F).icon(icon2, 0.0F, 6.0F - 7.0F / 2.0F, 7.0F, i3);
        float f5 = 7.0F + 3.0F;
        float f6 = Fonts.INTER_MEDIUM.getWidth(string2, 5.5F);
        new RenderUtil(drawContext, f, f2, f3, 12.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, f5, 0.0F, i, 5.5F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string2, f3 - f6, 0.0F, i2, 5.5F);
    }

    private void renderChatButton(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        this.chatButtonX = f;
        this.chatButtonY = f2;
        this.chatButtonWidth = f3;
        if (this.isHovered(i, i2, f, f2, f3, 16.0)) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 16.0F, f4, Radius.all(5.0F));
        }

        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        new RenderUtil(drawContext, f, f2, 7.0F, 16.0F).icon(Icon.USERS, 0.0F, 8.0F - 7.0F / 2.0F, 7.0F, i3);
        new RenderUtil(drawContext, f, f2, f3, 16.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("chat.profile_button", "Chat"), 7.0F + 3.0F, 0.0F, i3, 5.5F);
        new RenderUtil(drawContext, f, f2, f3, 16.0F)
            .icon(Icon.CHEVRON_RIGHT, f3 - 5.0F, 8.0F - 5.0F / 2.0F, 5.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4));
    }

    private void renderSeparator(DrawContext drawContext, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawOverlay(drawContext, f, f2, f3, 0.5F, f4);
    }

    private float getPopupHeight() {
        return 209.0F;
    }

    private String getRoleName() {
        Role role = Profile.getRole();
        if (role == null) {
            return "";
        }

        String string = role.name();
        return string.charAt(0) + string.substring(1).toLowerCase(Locale.ROOT);
    }

    public boolean isHoveringPopup(double d, double d2) {
        return this.visible && this.isHovered(d, d2, this.popupPosition.x, this.popupPosition.y, this.popupWidth, this.popupHeight);
    }

    public boolean isHoveringAny(double d, double d2) {
        return !this.visible
            ? false
            : this.isHoveringPopup(d, d2)
                || this.accent.isMouseOverPicker(d, d2)
                || this.background.isMouseOverPicker(d, d2)
                || this.text.isMouseOverPicker(d, d2)
                || this.language.isOverDropdown(d, d2)
                || this.blurQuality.isOverDropdown(d, d2);
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.visible) {
            return false;
        }

        if (this.language.mouseClicked(click, flag)) {
            if (this.language.isOverlayOpen()) {
                this.closeOtherOverlays(this.language);
            }
        } else if (this.blurQuality.mouseClicked(click, flag)) {
            if (this.blurQuality.isOverlayOpen()) {
                this.closeOtherOverlays(this.blurQuality);
                return true;
            }
        } else if (this.accent.mouseClicked(click, flag)) {
            if (this.accent.isOverlayOpen()) {
                this.closeOtherOverlays(this.accent);
                return true;
            }
        } else if (this.background.mouseClicked(click, flag)) {
            if (this.background.isOverlayOpen()) {
                this.closeOtherOverlays(this.background);
                return true;
            }
        } else {
            if (!this.text.mouseClicked(click, flag)) {
                if (this.tooltips.mouseClicked(click, flag)) {
                    return true;
                }

                if (this.shadows.mouseClicked(click, flag)) {
                    return true;
                }

                if (this.scale.mouseClicked(click, flag)) {
                    return true;
                }

                return this.isHoveringPopup(click.x(), click.y());
            }

            if (this.text.isOverlayOpen()) {
                this.closeOtherOverlays(this.text);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        boolean flag = this.accent.onMouseRelease(click);
        boolean flag2 = flag | this.background.onMouseRelease(click);
        boolean flag3 = flag2 | this.text.onMouseRelease(click);
        boolean flag4 = flag3 | this.language.onMouseRelease(click);
        this.tooltips.onMouseRelease(click);
        this.shadows.onMouseRelease(click);
        this.scale.onMouseRelease(click);
        GuiScale.setScale(this.scale.getValue() / 100.0F);
        ThemePrefs.save();
        return flag4 || this.isHoveringPopup(click.x(), click.y());
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return !this.accent.mouseDragged(click, d, d2)
                && !this.background.mouseDragged(click, d, d2)
                && !this.text.mouseDragged(click, d, d2)
                && !this.scale.mouseDragged(click, d, d2)
            ? this.isHoveringPopup(click.x(), click.y())
            : true;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return this.isHoveringPopup(d, d2);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (!this.visible) {
            return false;
        } else if (this.accent.onKeyPress(keyInput)) {
            return true;
        } else if (this.background.onKeyPress(keyInput)) {
            return true;
        } else if (this.text.onKeyPress(keyInput)) {
            return true;
        } else if (this.language.onKeyPress(keyInput)) {
            return true;
        } else if (this.scale.onKeyPress(keyInput)) {
            return true;
        } else if (this.blurQuality.onKeyPress(keyInput)) {
            return true;
        } else if (keyInput.key() == 256) {
            this.close();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        return this.visible && this.scale.onCharType(charInput);
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
    }

    private float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private void closeOtherOverlays(Setting<?> setting) {
        if (setting != this.language) {
            this.language.close();
        }

        if (setting != this.blurQuality) {
            this.blurQuality.close();
        }

        if (setting != this.accent) {
            this.accent.close();
        }

        if (setting != this.background) {
            this.background.close();
        }

        if (setting != this.text) {
            this.text.close();
        }
    }

    private int blurQualityToLevel(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -1994163307:
                if (string.equals("Medium")) {
                    b = 1;
                }
                break;
            case 2249154:
                if (string.equals("High")) {
                    return 1;
                }
        }

        switch (b) {
            case 0:
                return 1;
            case 1:
                return 4;
            default:
                return 8;
        }
    }

    private String levelToBlurQuality(int i) {
        if (i <= 1) {
            return "High";
        } else {
            return i <= 4 ? "Medium" : "Low";
        }
    }
}
