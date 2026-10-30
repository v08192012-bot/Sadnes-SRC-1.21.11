package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;

public record ScriptShader(RenderPipeline pipeline, boolean needsScene) {
}
