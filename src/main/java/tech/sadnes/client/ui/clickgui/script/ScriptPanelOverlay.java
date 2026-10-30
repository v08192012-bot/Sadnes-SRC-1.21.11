package tech.sadnes.client.ui.clickgui.script;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptPanel;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.util.color.Theme;

public final class ScriptPanelOverlay extends Component {
    private static final float CLOSE_ICON_SIZE = 10.0F;
    private final Animation openAnimation = new Animation();
    private ScriptFeature panel;
    private boolean closing;
    private float closeX;
    private float closeY;

    public boolean isOpen() {
        return this.panel != null && !this.closing;
    }

    public boolean isShowing(ScriptFeature scriptFeature) {
        return this.isOpen() && this.panel == scriptFeature;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001F;
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public void open(ScriptFeature scriptFeature) {
        this.panel = scriptFeature;
        this.closing = false;
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
        scriptFeature.getPanel().call("open");
    }

    @Override
    public void close() {
        if (this.panel != null && !this.closing) {
            this.closing = true;
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
            this.panel.getPanel().call("close");
        }
    }

    public void clear() {
        this.panel = null;
        this.closing = false;
        this.openAnimation.forceValue(0.0);
    }

    private ScriptPanel getScriptPanel() {
        return this.panel.getPanel();
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001F) {
            this.panel = null;
            this.closing = false;
        }

        if (this.panel != null && !(this.openAnimation.get() <= 0.001F)) {
            if (!this.panel.getModule().getScript().isActive()) {
                this.clear();
            } else {
                this.position.set(f2, f3);
                this.size.set(f4, f5);
                float f6 = Math.min(this.openAnimation.get(), 1.0F);
                float f7 = f6 * this.getAlpha();
                RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
                renderUtil.blur(f7, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true);
                renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.all(8.0F));
                this.closeX = f2 + f4 - 5.0F - 22.0F + 6.0F;
                this.closeY = f3 + 4.0F + 6.0F;
                new RenderUtil(drawContext, this.closeX, this.closeY, 10.0F, 10.0F)
                    .icon(Icon.CLOSE, 0.0F, 0.0F, 10.0F, Theme.withAlpha(Theme.getSecondaryText(), f7));
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f2 + f4 / 2.0F, f3 + f5 / 2.0F, 0.96F + 0.04F * f6);
                this.getScriptPanel().render(drawContext, f7, ScriptPanel.renderArgs(f2, f3, f4, f5, i, i2, f7, f));
                MatrixUtil.pop(drawContext);
            }
        }
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.panel == null) {
            return false;
        } else {
            boolean flag2 = this.isHovered(click.x(), click.y(), this.closeX - 2.0F, this.closeY - 2.0F, 14.0, 14.0);
            if (this.isLeftButton(click.button()) && flag2) {
                this.close();
                return true;
            } else {
                return this.getScriptPanel().callBoolean("click", ScriptPanel.mouseArgs(click.x(), click.y(), click.button(), 0.0, 0.0))
                    ? true
                    : this.isMouseOver(click.x(), click.y());
            }
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        return this.panel == null ? false : this.getScriptPanel().callBoolean("release", ScriptPanel.mouseArgs(click.x(), click.y(), click.button(), 0.0, 0.0));
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return this.panel == null ? false : this.getScriptPanel().callBoolean("drag", ScriptPanel.mouseArgs(click.x(), click.y(), click.button(), d, d2));
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.panel == null) {
            return false;
        } else {
            return this.getScriptPanel().callBoolean("scroll", ScriptPanel.scrollArgs(d, d2, d4, d3)) ? true : this.isMouseOver(d, d2);
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.panel == null) {
            return false;
        } else if (this.getScriptPanel().callBoolean("key", ScriptPanel.keyArgs(keyInput.key(), keyInput.scancode(), keyInput.modifiers(), true))) {
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
        return this.panel == null ? false : this.getScriptPanel().callBoolean("char", ScriptPanel.charArgs(charInput.codepoint(), charInput.modifiers()));
    }

    @Override
    public void resize(int i, int i2) {
        this.clear();
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void removed() {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
