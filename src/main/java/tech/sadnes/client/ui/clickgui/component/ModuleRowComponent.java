package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Danger;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.StatusIconUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.MarkerServer;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

public final class ModuleRowComponent implements IInteractable, IRenderable {
    public static final float ROW_HEIGHT = 22.0F;
    private static final float PADDING = 8.0F;
    private static final float FONT_SIZE = 6.5F;
    private static final float RIGHT_PADDING = 8.0F;
    private static final float BUTTON_SIZE = 10.0F;
    private static final float ICON_SIZE = 7.5F;
    private final Module module;
    private final ModuleRowComponent.RowActionListener listener;
    private final Animation enabledAnimation = new Animation();
    private final Animation hoverAnimation = new Animation();
    private final Animation settingsHoverAnimation = new Animation();
    private final Animation bindHoverAnimation = new Animation();
    private final Animation flashAnimation = new Animation();
    private final StopWatch flashTimer = new StopWatch();
    private boolean flashing;
    private boolean forceSettingsButton;
    private final HoverTooltip tooltip = new HoverTooltip();
    private float x;
    private float y;
    private float width;

    public ModuleRowComponent(Module module2, ModuleRowComponent.RowActionListener rowActionListener) {
        this.module = module2;
        this.listener = rowActionListener;
    }

    public void enableSettingsButton() {
        this.forceSettingsButton = true;
    }

    public void reset() {
        this.hoverAnimation.forceValue(0.0);
        this.enabledAnimation.forceValue(this.module.isEnabled() ? 1.0 : 0.0);
        this.settingsHoverAnimation.forceValue(0.0);
        this.bindHoverAnimation.forceValue(0.0);
        this.flashAnimation.forceValue(0.0);
        this.flashing = false;
        this.flashTimer.reset();
        this.tooltip.reset();
    }

    public void update() {
        this.enabledAnimation.update();
        this.enabledAnimation.animate(this.module.isEnabled() ? 1.0 : 0.0, 0.5, Easings.QUART_OUT);
        this.updateFlash();
        this.tooltip.update(this.module.getDisplayName(), this.module.getLocalizedDescription(), this.buildTooltipNotes());
    }

    private List<HoverTooltip.TooltipEntry> buildTooltipNotes() {
        if (!this.module.isBeta() && this.module.getDanger() == null) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList(2);
        if (this.module.getDanger() != null) {
            arrayList.add(new HoverTooltip.TooltipEntry(Icon.WARNING, this.module.getDangerLevel().getColor(), this.buildDangerNote(this.module.getDanger())));
        }

        if (this.module.isBeta()) {
            arrayList.add(
                new HoverTooltip.TooltipEntry(
                    Icon.BUG,
                    StatusIconUtil.BUG_COLOR,
                    Language.translate("marker.beta.note", "Модуль в бете и ещё дорабатывается — возможны баги и изменения.")
                )
            );
        }

        return arrayList;
    }

    private String buildDangerNote(Danger danger) {
        StringBuilder builder = new StringBuilder();
        MarkerServer[] markerServers = danger.reasons();

        for (int i = 0; i < markerServers.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append(Language.translate(markerServers[i].getTranslationKey(), markerServers[i].name()));
        }

        String string = Language.translate(danger.level().getTranslationKey(), danger.level().name());
        return Language.translate("marker.danger.note", "Рискованный модуль — может привести к бану.")
            + " "
            + Language.translate("marker.danger.servers", "Серверы")
            + ": "
            + builder
            + ". "
            + Language.translate("marker.danger.level", "Уровень")
            + ": "
            + string
            + ".";
    }

    public boolean hasVisibleSettings() {
        if (this.module.hasCustomPanel()) {
            return true;
        }

        for (Setting setting : this.module.getSettings()) {
            if (!(setting instanceof DragSetting) && this.module.isSettingVisible(setting)) {
                return true;
            }
        }

        return false;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4, Radius radius) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        float f5 = this.getGuiAlpha();
        boolean flag = this.isHovered(i, i2, f, f2, f3, 22.0);
        if (flag && !this.isSettingsPopupOpen()) {
            this.tooltip.markHovered();
        }

        boolean flag2;
        if (this.showsSettingsButton()) {
            flag2 = this.isHovered(i, i2, this.getSettingsButtonX(), f2, 10.0, 22.0);
        } else {
            flag2 = false;
        }

        boolean flag3 = this.isOverBindButton(i, i2);
        this.hoverAnimation.update();
        this.settingsHoverAnimation.update();
        this.bindHoverAnimation.update();
        this.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        this.settingsHoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.bindHoverAnimation.animate(flag3 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = this.enabledAnimation.get();
        float f7 = this.hoverAnimation.get();
        float f8 = this.flashAnimation.get();
        if (f8 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 22.0F, f5 * f8 * 0.67F, radius);
        }

        GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, 22.0F, f5 * f7 * 0.67F, radius);
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getTextColor(), f6), f5);
        boolean flag4 = this.module.isBeta();
        boolean flag5 = this.module.isDangerActive();
        float f9 = StatusIconUtil.drawIcons(drawContext, f + 8.0F, f2, 22.0F, 7.5F, f5, flag4, flag5, this.module.getDangerLevel());
        int i4 = StatusIconUtil.getNameColor(i3, f5, flag4, flag5, this.module.getDangerLevel());
        new RenderUtil(drawContext, f, f2, f3, 22.0F).textMiddleLeft(Fonts.INTER_MEDIUM, this.module.getDisplayName(), 8.0F + f9, -0.5F, i4, 6.5F);
        if (this.showsSettingsButton()) {
            this.renderIconButton(
                drawContext, this.getSettingsButtonX(), this.forceSettingsButton ? Icon.CHEVRON_RIGHT : Icon.SETTINGS, f5, this.settingsHoverAnimation.get()
            );
        }

        this.renderIconButton(drawContext, this.getBindButtonX(), Icon.KEYBOARD, f5 * f7, this.bindHoverAnimation.get());
        this.renderSwitch(drawContext, f5, f6);
    }

    private void renderIconButton(DrawContext drawContext, float f, Icon icon2, float f2, float f3) {
        GuiRenderUtil.drawHighlight(drawContext, f, this.y + 11.0F - 5.0F, 10.0F, 10.0F, f2 * f3, Radius.all(3.3333333F));
        float f4 = 0.65F + 0.35F * f3;
        int i = Theme.withAlpha(Theme.getPrimary(), f4 * f2);
        float f5 = f + 1.25F;
        float f6 = this.y + 11.0F - 3.75F;
        new RenderUtil(drawContext, f5, f6, 7.5F, 7.5F).icon(icon2, 0.0F, 0.0F, 7.5F, i);
    }

    private void renderSwitch(DrawContext drawContext, float f, float f2) {
        float f3 = this.getSwitchX();
        float f4 = this.y + 11.0F - 4.5F;
        SettingWidgets.drawDefaultSwitch(drawContext, f3, f4, f2, f);
    }

    public void renderTooltip(DrawContext drawContext) {
        this.tooltip.render(drawContext, this.x, this.y, this.width, 22.0F, HoverTooltip.Side.RIGHT, this.getGuiAlpha());
    }

    private void updateFlash() {
        this.flashAnimation.update();
        if (!this.flashing) {
            this.flashAnimation.animate(0.0, 0.24, Easings.QUAD_IN);
        } else {
            long l = this.flashTimer.getElapsed();
            if (l >= 1650L) {
                this.flashing = false;
            } else {
                float f = (float)(l % 550L) / (float)550L;
                float f2 = 0.5F + 0.5F * (float)Math.cos(f * (float) (Math.PI * 2));
                this.flashAnimation.animate(f2, 0.1, Easings.SINE_OUT);
            }
        }
    }

    public void flash() {
        this.flashing = true;
        this.flashTimer.reset();
    }

    private float getSettingsButtonX() {
        return this.getSwitchX() - 8.0F - 10.0F + 6.0F;
    }

    private float getBindButtonX() {
        return this.showsSettingsButton() ? this.getSettingsButtonX() - 10.0F - 2.0F : this.getSettingsButtonX();
    }

    private float getSwitchX() {
        return this.x + this.width - 8.0F - 14.0F;
    }

    public boolean isOverSettingsButton(double d, double d2) {
        return this.showsSettingsButton() && this.isHovered(d, d2, this.getSettingsButtonX(), this.y + 11.0F - 5.0F, 10.0, 10.0);
    }

    private boolean isOverBindButton(double d, double d2) {
        return this.isHovered(d, d2, this.getBindButtonX(), this.y + 11.0F - 5.0F, 10.0, 10.0);
    }

    private boolean showsSettingsButton() {
        return this.forceSettingsButton || this.hasVisibleSettings();
    }

    private boolean isOverRow(double d, double d2) {
        return this.isHovered(d, d2, this.x, this.y, this.width, 22.0);
    }

    public float getTop() {
        return this.y;
    }

    public float getLeft() {
        return this.x;
    }

    public float getRowWidth() {
        return this.width;
    }

    public boolean handleMouseClick(Click click, boolean flag) {
        if (!this.isOverRow(click.x(), click.y())) {
            return false;
        }

        if (this.isLeftButton(click.button())) {
            if (this.isOverBindButton(click.x(), click.y())) {
                this.listener.onBind(this.module, click.x(), click.y());
                return true;
            } else if (this.isOverSettingsButton(click.x(), click.y())) {
                this.listener.onOpenSettings(this.module, this);
                return true;
            } else {
                this.listener.onToggle(this.module);
                return true;
            }
        } else if (this.isRightButton(click.button()) && this.hasVisibleSettings()) {
            this.listener.onOpenSettings(this.module, this);
            return true;
        } else if (this.isMiddleButton(click.button())) {
            this.listener.onBind(this.module, click.x(), click.y());
            return true;
        } else {
            return false;
        }
    }

    public boolean handleMouseRelease(Click click) {
        return false;
    }

    public boolean handleMouseDrag(Click click, double d, double d2) {
        return false;
    }

    public boolean handleMouseScroll(double d, double d2, double d3, double d4) {
        return false;
    }

    private float getGuiAlpha() {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        return clickGuiScreen == null ? 1.0F : (float)Math.pow(clickGuiScreen.getOpenAnimation().get(), 3.0) * clickGuiScreen.getCategorySwitchAnimation().get();
    }

    private boolean isSettingsPopupOpen() {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        return clickGuiScreen != null && clickGuiScreen.getSettingsPopup().isOpen();
    }

    public Module getModule() {
        return this.module;
    }

    public ModuleRowComponent.RowActionListener getListener() {
        return this.listener;
    }

    public Animation getEnabledAnimation() {
        return this.enabledAnimation;
    }

    public Animation getHoverAnimation() {
        return this.hoverAnimation;
    }

    public Animation getSettingsHoverAnimation() {
        return this.settingsHoverAnimation;
    }

    public Animation getBindHoverAnimation() {
        return this.bindHoverAnimation;
    }

    public Animation getFlashAnimation() {
        return this.flashAnimation;
    }

    public StopWatch getFlashTimer() {
        return this.flashTimer;
    }

    public boolean isFlashing() {
        return this.flashing;
    }

    public boolean isForceSettingsButton() {
        return this.forceSettingsButton;
    }

    public HoverTooltip getTooltip() {
        return this.tooltip;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public interface RowActionListener {
        void onToggle(Module module2);

        void onOpenSettings(Module module2, ModuleRowComponent moduleRowComponent);

        void onBind(Module module2, double d, double d2);
    }
}
