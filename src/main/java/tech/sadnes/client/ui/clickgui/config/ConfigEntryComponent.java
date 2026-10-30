package tech.sadnes.client.ui.clickgui.config;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.cloud.CloudConfigInfo;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class ConfigEntryComponent extends Component {
    private static final float BUTTON_ICON_SIZE = 7.5F;
    private static final float BUTTON_SIZE = 10.0F;
    private static final float BUTTON_GAP = 2.0F;
    private static final float RIGHT_PADDING = 4.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float ICON_OFFSET = 4.0F;
    private static final float TEXT_OFFSET = 15.0F;
    private static final float BADGE_SIZE = 6.0F;
    private CloudConfigInfo config;
    private boolean selected;
    public float alphaMultiplier = 1.0F;
    private final Animation hoverAnimation2 = new Animation();
    private final Animation settingsHoverAnimation = new Animation();
    private final Animation actionHoverAnimation = new Animation();
    private final Animation favoriteHoverAnimation = new Animation();

    public ConfigEntryComponent(CloudConfigInfo cloudConfigInfo, boolean flag) {
        this.config = cloudConfigInfo;
        this.selected = flag;
    }

    public void update(CloudConfigInfo cloudConfigInfo, boolean flag) {
        this.config = cloudConfigInfo;
        this.selected = flag;
    }

    @Override
    public float getAlpha() {
        return super.getAlpha() * this.alphaMultiplier;
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void resize(int i, int i2) {
    }

    public boolean isActive() {
        return !this.config.isShared() && this.selected;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        boolean flag = this.isActive();
        float f2 = this.getAlpha();
        Radius radius = Radius.all(this.size.y / 3.0F);
        this.hoverAnimation2.update();
        this.hoverAnimation2.animate(this.isHovered(i, i2, this.position, this.size) ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        float f3 = this.hoverAnimation2.get();
        float f4 = flag ? 0.72F + 0.18F * f3 : 0.67F * f3;
        GuiRenderUtil.drawHighlight(drawContext, this.position.x, this.position.y, this.size.x, this.size.y, f2 * f4, radius);
        float f5 = this.getActionButtonX();
        float f6 = this.getSettingsButtonX();
        float f7 = this.getFavoriteButtonX();
        float f8 = f7 - this.position.x - 15.0F - 2.0F;
        Icon icon2 = this.config.getIcon() != null ? this.config.getIcon() : (this.config.isShared() ? Icon.USERS : Icon.DEVICE_FLOPPY);
        int i3 = this.config.getColor() != 0
            ? Theme.withAlpha(this.config.getColor(), f2)
            : (flag ? this.getPrimaryColor() : Theme.withAlpha(Theme.getSecondaryText(), 0.7F * f2));
        new RenderUtil(drawContext, this.position.x, this.position.y, this.size.x, this.size.y).icon(icon2, 4.0F, this.size.y / 2.0F - 4.0F, 8.0F, i3);
        if (this.config.isShared()) {
            float f9 = this.size.y / 2.0F + 4.0F - 6.0F + 2.0F;
            new RenderUtil(drawContext, this.position.x + 8.0F - 1.0F, this.position.y + f9 - 1.0F, 8.0F, 8.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.getBackground(), f2), Radius.all(4.0F));
            new RenderUtil(drawContext, this.position.x, this.position.y, this.size.x, this.size.y)
                .icon(Icon.LINK, 8.0F, f9, 6.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f2));
        }

        int i4 = flag ? this.getPrimaryColor() : ColorUtil.multiplyAlpha(Theme.getTextColor(), 0.82F * this.getAlpha());
        new RenderUtil(drawContext, this.position.x, this.position.y, this.size.x, this.size.y)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, this.config.getName(), 15.0F, 0.0F, f8, i4, 5.5F);
        this.favoriteHoverAnimation.update();
        this.favoriteHoverAnimation.animate(this.isFavoriteHovered(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        boolean flag2 = this.isFavorite();
        if (flag2 || f3 > 0.01F) {
            this.renderButton(
                drawContext,
                f7,
                Icon.BOOKMARK,
                flag2 ? Theme.getPrimary() : Theme.getSecondaryText(),
                this.favoriteHoverAnimation.get(),
                flag2 ? f2 : f2 * f3
            );
        }

        this.settingsHoverAnimation.update();
        this.settingsHoverAnimation.animate(this.isSettingsHovered(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.renderButton(drawContext, f6, Icon.SETTINGS, Theme.getSecondaryText(), this.settingsHoverAnimation.get(), f2);
        this.actionHoverAnimation.update();
        this.actionHoverAnimation.animate(this.isActionHovered(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        Icon icon3 = flag ? Icon.DEVICE_FLOPPY : Icon.CLOUD_DOWNLOAD;
        if (flag) {
            this.renderButton(drawContext, f5, icon3, Theme.getTextColor(), this.actionHoverAnimation.get(), f2);
        } else {
            this.renderButton(drawContext, f5, icon3, Theme.getSecondaryText(), this.actionHoverAnimation.get(), f2);
        }
    }

    private void renderButton(DrawContext drawContext, float f, Icon icon2, int i, float f2, float f3) {
        if (f2 > 0.001F) {
            GuiRenderUtil.drawHighlight(
                drawContext, f + 5.0F - 10.0F / 2.0F, this.position.y + this.size.y / 2.0F - 10.0F / 2.0F, 10.0F, 10.0F, f3 * f2, Radius.all(10.0F / 3.0F)
            );
        }

        int i2 = Theme.withAlpha(i, (0.55F + 0.4F * f2) * f3);
        new RenderUtil(drawContext, f, this.position.y, 10.0F, this.size.y).icon(icon2, 1.25F, this.size.y / 2.0F - 3.75F, 7.5F, i2);
    }

    public boolean isSettingsHovered(double d, double d2) {
        return this.isHovered(d, d2, this.getSettingsButtonX(), this.position.y, 10.0, this.size.y);
    }

    public boolean isActionHovered(double d, double d2) {
        return this.isHovered(d, d2, this.getActionButtonX(), this.position.y, 10.0, this.size.y);
    }

    public boolean isFavoriteHovered(double d, double d2) {
        return this.isHovered(d, d2, this.getFavoriteButtonX(), this.position.y, 10.0, this.size.y);
    }

    public boolean isFavorite() {
        return Sadnes.getInstance().getConfigManager().isFavorite(this.config.getId());
    }

    private float getFavoriteButtonX() {
        return this.getSettingsButtonX() - 10.0F;
    }

    private float getActionButtonX() {
        return this.position.x + this.size.x - 10.0F - 4.0F;
    }

    private float getSettingsButtonX() {
        return this.getActionButtonX() - 10.0F - 2.0F;
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        return false;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        return false;
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
    public void close() {
    }

    @Override
    public void removed() {
    }

    public CloudConfigInfo getConfig() {
        return this.config;
    }

    public float getAlphaMultiplier() {
        return this.alphaMultiplier;
    }

    public Animation getHoverAnimation() {
        return this.hoverAnimation2;
    }

    public Animation getSettingsHoverAnimation() {
        return this.settingsHoverAnimation;
    }

    public Animation getActionHoverAnimation() {
        return this.actionHoverAnimation;
    }

    public Animation getFavoriteHoverAnimation() {
        return this.favoriteHoverAnimation;
    }
}
