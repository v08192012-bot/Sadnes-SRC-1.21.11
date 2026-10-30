package tech.sadnes.client.script.api;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.shader.ScriptFxRenderer;
import tech.sadnes.client.render.shader.ScriptShader;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptPathValidator;
import tech.sadnes.client.script.ScriptShaders;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptRender3DApi {
    private ScriptRender3DApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(
            nativeObject,
            "box",
            objects -> {
                Box box = new Box(
                    ScriptValues.argDouble(objects, 0, 0.0),
                    ScriptValues.argDouble(objects, 1, 0.0),
                    ScriptValues.argDouble(objects, 2, 0.0),
                    ScriptValues.argDouble(objects, 3, 0.0),
                    ScriptValues.argDouble(objects, 4, 0.0),
                    ScriptValues.argDouble(objects, 5, 0.0)
                );
                Render3DUtil.drawBox(getMatrices(), box, ScriptValues.argIntOrDefault(objects, 7, 0), ScriptValues.argIntOrDefault(objects, 6, 1090519039));
                return null;
            }
        );
        ScriptValues.putFunction(nativeObject, "blockBox", objects2 -> {
            double d = Math.floor(ScriptValues.argDouble(objects2, 0, 0.0));
            double d2 = Math.floor(ScriptValues.argDouble(objects2, 1, 0.0));
            double d3 = Math.floor(ScriptValues.argDouble(objects2, 2, 0.0));
            Box box2 = new Box(d, d2, d3, d + 1.0, d2 + 1.0, d3 + 1.0);
            Render3DUtil.drawBox(getMatrices(), box2, ScriptValues.argIntOrDefault(objects2, 4, 0), ScriptValues.argIntOrDefault(objects2, 3, 1090519039));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "entityBox", objects3 -> {
            Entity entity = ScriptPlayerApi.unwrapEntity(ScriptValues.hasArg(objects3, 0) ? objects3[0] : null);
            if (entity == null) {
                return null;
            }

            ScriptManager scriptManager = getScriptManager();
            Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, scriptManager.getTickDelta());
            Box box3 = entity.getBoundingBox().offset(vec3d.subtract(entity.getEntityPos()));
            Render3DUtil.drawBox(scriptManager.getMatrices(), box3, ScriptValues.argIntOrDefault(objects3, 2, 0), ScriptValues.argIntOrDefault(objects3, 1, 1090519039));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "sphere", objects4 -> {
            double d4 = ScriptValues.argDouble(objects4, 0, 0.0);
            double d5 = ScriptValues.argDouble(objects4, 1, 0.0);
            double d6 = ScriptValues.argDouble(objects4, 2, 0.0);
            double d7 = ScriptValues.argDouble(objects4, 3, 1.0);
            int i = ScriptValues.argIntOrDefault(objects4, 4, -1);
            int i2 = Math.clamp(ScriptValues.argInt(objects4, 5, 5), 1, 24);
            boolean flag = ScriptValues.argBoolean(objects4, 6, true);
            float f = ScriptValues.argFloat(objects4, 7, 2.0F);
            MatrixStack matrixStack = getMatrices();

            for (int i3 = 1; i3 <= i2; i3++) {
                double d8 = Math.PI * i3 / (i2 + 1.0);
                double d9 = d7 * Math.sin(d8);
                double d10 = d5 + d7 * Math.cos(d8);
                drawCircle(matrixStack, i, f, flag, 28, d4, d10, d6, d9, true, 0.0);
            }

            drawCircle(matrixStack, i, f, flag, 28, d4, d5, d6, d7, false, 0.0);
            drawCircle(matrixStack, i, f, flag, 28, d4, d5, d6, d7, false, Math.PI / 2);
            return null;
        });
        ScriptValues.putFunction(
            nativeObject,
            "shader",
            objects5 -> {
                ScriptShader scriptShader = ScriptShaders.getShader(ScriptShaders.ShaderKind.FX, script, resolvePath(script, ScriptValues.argString(objects5, 0)));
                if (scriptShader == null) {
                    return null;
                }

                RenderPipeline renderPipeline = scriptShader.pipeline();
                NativeObject nativeObject2 = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject2, "name", ScriptValues.argString(objects5, 0));
                ScriptValues.putFunction(
                    nativeObject2,
                    "draw",
                    objects6 -> {
                        getMatrices();
                        ScriptFxRenderer.renderBillboard(
                            getScriptManager().getMatrices(),
                            renderPipeline,
                            ScriptValues.argDouble(objects6, 0, 0.0),
                            ScriptValues.argDouble(objects6, 1, 0.0),
                            ScriptValues.argDouble(objects6, 2, 0.0),
                            ScriptValues.argFloat(objects6, 3, 1.0F),
                            ScriptValues.argIntOrDefault(objects6, 4, -1),
                            ScriptFxRenderer.getTime(),
                            ScriptValues.argFloat(objects6, 5, 0.0F),
                            ScriptValues.argFloat(objects6, 6, 0.0F),
                            ScriptValues.argFloat(objects6, 7, 0.0F),
                            ScriptValues.argFloat(objects6, 8, 0.0F)
                        );
                        return null;
                    }
                );
                return nativeObject2;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "line",
            objects7 -> {
                List list = List.of(
                    new Vec3d(ScriptValues.argDouble(objects7, 0, 0.0), ScriptValues.argDouble(objects7, 1, 0.0), ScriptValues.argDouble(objects7, 2, 0.0)),
                    new Vec3d(ScriptValues.argDouble(objects7, 3, 0.0), ScriptValues.argDouble(objects7, 4, 0.0), ScriptValues.argDouble(objects7, 5, 0.0))
                );
                Render3DUtil.drawLines(getMatrices(), list, ScriptValues.argIntOrDefault(objects7, 6, -1), ScriptValues.argFloat(objects7, 7, 2.0F));
                return null;
            }
        );
        scriptableObject.put("render3d", scriptableObject, nativeObject);
    }

    static Path resolvePath(Script script, String string) {
        return ScriptPathValidator.resolveSafe(script.getAssetsDir(), string);
    }

    private static void drawCircle(
        MatrixStack matrixStack, int i, float f, boolean flag, int i2, double d, double d2, double d3, double d4, boolean flag2, double d5
    ) {
        if (!(d4 <= 1.0E-4)) {
            ArrayList arrayList = new ArrayList(i2 + 1);

            for (int i3 = 0; i3 <= i2; i3++) {
                double d6 = (double)i3 / i2 * Math.PI * 2.0;
                double d7 = Math.cos(d6) * d4;
                double d8 = Math.sin(d6) * d4;
                if (flag2) {
                    arrayList.add(new Vec3d(d + d7, d2, d3 + d8));
                } else {
                    arrayList.add(new Vec3d(d + d7 * Math.cos(d5), d2 + d8, d3 + d7 * Math.sin(d5)));
                }
            }

            if (flag) {
                Render3DUtil.drawGlowLines(matrixStack, arrayList, i, f);
            } else {
                Render3DUtil.drawLines(matrixStack, arrayList, i, f);
            }
        }
    }

    private static ScriptManager getScriptManager() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null && scriptManager.getMatrices() != null) {
            return scriptManager;
        } else {
            throw Context.reportRuntimeError("render3d.* доступен только внутри события render3d");
        }
    }

    private static MatrixStack getMatrices() {
        return getScriptManager().getMatrices();
    }
}
