package tech.sadnes.client.feature.impl.render.esp;

import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import tech.sadnes.client.render.shader.ScriptFxRenderer;
import tech.sadnes.client.render.shader.ScriptFxUniforms;
import tech.sadnes.client.render.shader.ScriptShader;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptEspRegistry;
import tech.sadnes.client.script.ScriptShaders;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.libs.rhino.NativeObject;

public final class ScriptShaderElement extends ScriptEspElement {
    private final ScriptShaderElement.Definition definition;
    private final NativeObject context;
    private final float[] paramValues = new float[4];
    private ScriptShader shader;
    private boolean shaderLoaded;

    public ScriptShaderElement(ScriptShaderElement.Definition definition2, EspTargetType espTargetType) {
        super(definition2.script(), definition2.name(), definition2.enabledByDefault(), espTargetType, definition2.replaces(), definition2.icon());
        this.definition = definition2;
        this.context = ScriptValues.newObject();
        ScriptValues.putValue(this.context, "category", espTargetType.name().toLowerCase(Locale.ROOT));
    }

    public boolean isChams() {
        return this.definition.kind() == ScriptShaders.ShaderKind.CHAMS;
    }

    public ScriptShader getShader() {
        if (!this.shaderLoaded) {
            this.shaderLoaded = true;
            this.shader = ScriptShaders.getShader(this.definition.kind(), this.getScript(), this.definition.file());
        }

        return this.shader;
    }

    public boolean shouldRenderShader() {
        return this.isChams() && !this.definition.model() && this.isActive() && this.getShader() != null;
    }

    public boolean isModel() {
        return this.definition.model();
    }

    public int getColor() {
        int i = Theme.getPrimary();
        return ScriptEspRegistry.resolveInt(this.getScript(), this.definition.color(), this.context, ColorUtil.fade(10, 0, i, ColorUtil.darken(i, 0.25F)));
    }

    public float getRadius() {
        return ScriptEspRegistry.resolveFloat(this.getScript(), this.definition.radius(), this.context, 4.0F);
    }

    public ScriptFxUniforms createUniforms(float f) {
        ScriptEspRegistry.resolveFloats(this.getScript(), this.definition.params(), this.context, this.paramValues);
        return new ScriptFxUniforms(
            ScriptFxRenderer.getTime(), f, this.paramValues[0], this.paramValues[1], this.paramValues[2], this.paramValues[3], 0.0F, 0.0F
        );
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        this.size2.set(0.0F, 0.0F);
    }

    public record Definition(
        Script script,
        String name,
        ScriptShaders.ShaderKind kind,
        Path file,
        boolean enabledByDefault,
        Icon icon,
        String replaces,
        Object color,
        Object params,
        Object radius,
        boolean model
    ) {
    }
}
