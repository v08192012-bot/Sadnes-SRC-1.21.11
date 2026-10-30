#version 330

// sadnes: ванильный вершинник террейна плюс колыхание растительности от ветра Ambience (sadnes:foliage_sway.glsl).
// UBO SadnesFoliage объявлен во всех конвейерах с этим вершинником (RenderPipelineBuilderMixin) и привязывается
// в RenderSystem.bindDefaultUniforms (FoliageSway); без ветра сила нулевая и вершины не сдвигаются.
// FoliageWind: xy — направление ветра, z — сила, w — время. Комментарии держим снаружи блока (конвертер VulkanMod).

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:chunksection.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <sadnes:foliage_sway.glsl>

layout(std140) uniform SadnesFoliage {
    vec4 FoliageWind;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((uv / 256.0) + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main() {
    // Мировая позиция для фаз ветра: позиция секции свёрнута в 4096 блоков — в float она остаётся точной.
    vec3 world = vec3(ChunkPosition & ivec3(4095)) + Position;
    vec3 pos = Position + (ChunkPosition - CameraBlockPos) + CameraOffset + sadnes_foliageSway(world, Color.a, FoliageWind);
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
