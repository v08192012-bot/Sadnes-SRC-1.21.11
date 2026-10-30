package tech.sadnes.client.ui.hud;

import java.nio.file.Path;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.TexturedShapeRenderer;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.ScriptIconCache;
import tech.sadnes.client.render.texture.SvgIconCache;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptWatermarkChip extends WatermarkChip {
    private final Script script;
    private Icon icon;
    private Path iconPath;
    private Object label;
    private Object valueText;
    private Function warningFunction;

    public ScriptWatermarkChip(String string, BooleanSetting booleanSetting, Script script2, Icon icon2) {
        super(string, booleanSetting);
        this.script = script2;
        this.icon = icon2;
    }

    public Script getScript() {
        return this.script;
    }

    public void setIcon(Icon icon2) {
        if (icon2 != null) {
            this.icon = icon2;
            this.iconPath = null;
        }
    }

    public void setIconPath(Path path) {
        if (path != null) {
            this.iconPath = path;
        }
    }

    public void setLabel(Object object) {
        this.label = object;
    }

    public void setValueText(Object object) {
        this.valueText = object;
    }

    public void setWarningFunction(Function function) {
        this.warningFunction = function;
    }

    @Override
    public boolean isVisible() {
        return this.script.isActive() && super.isVisible();
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }

    @Override
    public void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
        if (this.iconPath == null) {
            super.renderIcon(drawContext, f, f2, f3, f4);
        } else {
            TextureSetup textureSetup = ScriptIconCache.getTextureSetup(this.iconPath);
            if (textureSetup != null) {
                TexturedShapeRenderer.drawTexture(
                    drawContext,
                    textureSetup,
                    drawContext.getMatrices(),
                    f,
                    f2,
                    f3,
                    f3,
                    Radius.zero(),
                    SvgIconCache.getSdfSpread(),
                    -0.4F,
                    0.4F,
                    1.0F,
                    this.resolveIconColor(f4),
                    6
                );
            }
        }
    }

    @Override
    protected String getLabel() {
        return this.resolveText(this.label);
    }

    @Override
    protected String getValueText() {
        return this.resolveText(this.valueText);
    }

    @Override
    public float getWarningLevel() {
        return this.warningFunction != null && this.script.isActive() && this.script.call(this.warningFunction) instanceof Number number
            ? Math.max(0.0F, Math.min(1.0F, number.floatValue()))
            : 0.0F;
    }

    private String resolveText(Object object) {
        if (object == null || Undefined.isUndefined(object)) {
            return "";
        }

        if (object instanceof Function function) {
            if (!this.script.isActive()) {
                return "";
            }

            Object object2 = this.script.call(function);
            return object2 != null && !Undefined.isUndefined(object2) ? Context.toString(object2) : "";
        } else {
            return Context.toString(object);
        }
    }
}
