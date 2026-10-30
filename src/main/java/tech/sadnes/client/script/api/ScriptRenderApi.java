package tech.sadnes.client.script.api;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.hud.TargetHud;
import tech.sadnes.client.render.GuiParticle;
import tech.sadnes.client.render.GuiParticleSystem;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.PixelRenderUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.TexturedShapeRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.font.RollingNumberRenderer;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.ScriptIconCache;
import tech.sadnes.client.render.texture.SvgIconCache;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptPathValidator;
import tech.sadnes.client.script.ScriptTextureLoader;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptRenderApi {
    private static final float DEFAULT_FONT_SIZE = 7.0F;
    private static final long START_TIME = System.nanoTime();
    public static float alpha = 1.0F;
    private static final Object FONT_KEY = new Object();
    private static final Map<String, MsdfFont> FONT_CACHE = new ConcurrentHashMap<>();

    private ScriptRenderApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "width", objects -> GuiScale.getScaledWidth());
        ScriptValues.putFunction(nativeObject, "height", objects2 -> GuiScale.getScaledHeight());
        ScriptValues.putFunction(
            nativeObject, "textWidth", objects3 -> Fonts.getDefault().getWidth(ScriptValues.argString(objects3, 0), ScriptValues.argFloat(objects3, 1, 7.0F))
        );
        ScriptValues.putFunction(nativeObject, "textHeight", objects4 -> Fonts.getDefault().getHeight(ScriptValues.argFloat(objects4, 0, 7.0F)));
        ScriptValues.putFunction(
            nativeObject,
            "toScreen",
            objects5 -> {
                Vector2f vector2f = ProjectionUtil.worldToScreen(
                    ScriptValues.argDouble(objects5, 0, 0.0), ScriptValues.argDouble(objects5, 1, 0.0), ScriptValues.argDouble(objects5, 2, 0.0)
                );
                if (vector2f == null) {
                    return null;
                }

                NativeObject nativeObject2 = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject2, "x", vector2f.x);
                ScriptValues.putValue(nativeObject2, "y", vector2f.y);
                return nativeObject2;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "rect",
            objects6 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects6, 0),
                        ScriptValues.argFloat(objects6, 1),
                        ScriptValues.argFloat(objects6, 2),
                        ScriptValues.argFloat(objects6, 3)
                    )
                    .rect(getColor(objects6, 4), getRadius(objects6, 5, 6));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "outline",
            objects7 -> {
                float f = ScriptValues.argFloat(objects7, 4, 1.0F);
                float f2 = ScriptValues.argFloat(objects7, 6, 0.0F);
                Radius radius = f2 <= 0.0F
                    ? Radius.zero()
                    : (ScriptValues.argBoolean(objects7, 7, false) ? Radius.smoothAll(f2 + f) : Radius.all(f2 + f));
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects7, 0),
                        ScriptValues.argFloat(objects7, 1),
                        ScriptValues.argFloat(objects7, 2),
                        ScriptValues.argFloat(objects7, 3)
                    )
                    .shadow(f, getColor(objects7, 5), radius);
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "gradientV",
            objects8 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects8, 0),
                        ScriptValues.argFloat(objects8, 1),
                        ScriptValues.argFloat(objects8, 2),
                        ScriptValues.argFloat(objects8, 3)
                    )
                    .rectGradientX(getColor(objects8, 4), getColor(objects8, 5));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "gradientH",
            objects9 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects9, 0),
                        ScriptValues.argFloat(objects9, 1),
                        ScriptValues.argFloat(objects9, 2),
                        ScriptValues.argFloat(objects9, 3)
                    )
                    .rectGradientY(getColor(objects9, 4), getColor(objects9, 5));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "gradient4",
            objects10 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects10, 0),
                        ScriptValues.argFloat(objects10, 1),
                        ScriptValues.argFloat(objects10, 2),
                        ScriptValues.argFloat(objects10, 3)
                    )
                    .rect(getColor(objects10, 4), getColor(objects10, 5), getColor(objects10, 6), getColor(objects10, 7), getRadius(objects10, 8, 9));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "shadow",
            objects11 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects11, 0),
                        ScriptValues.argFloat(objects11, 1),
                        ScriptValues.argFloat(objects11, 2),
                        ScriptValues.argFloat(objects11, 3)
                    )
                    .outline(ScriptValues.argFloat(objects11, 4, 8.0F), getColor(objects11, 5), getRadius(objects11, 6, 7));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "shadowHollow",
            objects12 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects12, 0),
                        ScriptValues.argFloat(objects12, 1),
                        ScriptValues.argFloat(objects12, 2),
                        ScriptValues.argFloat(objects12, 3)
                    )
                    .border(ScriptValues.argFloat(objects12, 4, 8.0F), getColor(objects12, 5), getRadius(objects12, 6, 7));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "blur",
            objects13 -> {
                new RenderUtil(
                        getContext(),
                        ScriptValues.argFloat(objects13, 0),
                        ScriptValues.argFloat(objects13, 1),
                        ScriptValues.argFloat(objects13, 2),
                        ScriptValues.argFloat(objects13, 3)
                    )
                    .blur(alpha * ScriptValues.argFloat(objects13, 5, 1.0F), getRadius(objects13, 4, 6), RenderUtil.BlurQuality.MIDDLE, true);
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "header",
            objects14 -> {
                HudPanelUtil.drawPanel(
                    getContext(),
                    ScriptValues.argFloat(objects14, 0),
                    ScriptValues.argFloat(objects14, 1),
                    ScriptValues.argFloat(objects14, 2),
                    ScriptValues.argFloat(objects14, 3),
                    alpha * ScriptValues.argFloat(objects14, 4, 1.0F),
                    ScriptValues.argFloat(objects14, 5, 8.0F)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "panel",
            objects15 -> {
                GuiRenderUtil.drawPopup(
                    getContext(),
                    ScriptValues.argFloat(objects15, 0),
                    ScriptValues.argFloat(objects15, 1),
                    ScriptValues.argFloat(objects15, 2),
                    ScriptValues.argFloat(objects15, 3),
                    alpha * ScriptValues.argFloat(objects15, 4, 1.0F),
                    ScriptValues.argFloat(objects15, 5, 6.0F)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "glass",
            objects16 -> {
                GuiRenderUtil.window(
                    getContext(),
                    ScriptValues.argFloat(objects16, 0),
                    ScriptValues.argFloat(objects16, 1),
                    ScriptValues.argFloat(objects16, 2),
                    ScriptValues.argFloat(objects16, 3),
                    alpha * ScriptValues.argFloat(objects16, 4, 1.0F),
                    ScriptValues.argFloat(objects16, 5, 4.0F)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "container",
            objects17 -> {
                GuiRenderUtil.drawOutlined(
                    getContext(),
                    ScriptValues.argFloat(objects17, 0),
                    ScriptValues.argFloat(objects17, 1),
                    ScriptValues.argFloat(objects17, 2),
                    ScriptValues.argFloat(objects17, 3),
                    alpha * ScriptValues.argFloat(objects17, 4, 1.0F),
                    ScriptValues.argFloat(objects17, 5, 10.0F)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "hover",
            objects18 -> {
                GuiRenderUtil.drawHighlight(
                    getContext(),
                    ScriptValues.argFloat(objects18, 0),
                    ScriptValues.argFloat(objects18, 1),
                    ScriptValues.argFloat(objects18, 2),
                    ScriptValues.argFloat(objects18, 3),
                    alpha * ScriptValues.argFloat(objects18, 4, 1.0F),
                    Radius.all(ScriptValues.argFloat(objects18, 5, 4.0F))
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "toggle",
            objects19 -> {
                SettingWidgets.drawSwitch(
                    getContext(),
                    ScriptValues.argFloat(objects19, 0),
                    ScriptValues.argFloat(objects19, 1),
                    ScriptValues.argFloat(objects19, 2, 14.0F),
                    ScriptValues.argFloat(objects19, 3, 9.0F),
                    ScriptValues.argFloat(objects19, 4, 0.0F),
                    alpha * ScriptValues.argFloat(objects19, 5, 1.0F)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "text",
            objects20 -> {
                Fonts.getDefault()
                    .draw(
                        getContext(),
                        ScriptValues.argString(objects20, 0),
                        ScriptValues.argFloat(objects20, 1),
                        ScriptValues.argFloat(objects20, 2),
                        getColor(objects20, 3),
                        ScriptValues.argFloat(objects20, 4, 7.0F)
                    );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textCenter",
            objects21 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects21, 1), ScriptValues.argFloat(objects21, 2), 0.0F, 0.0F)
                    .textTopCenter(Fonts.getDefault(), ScriptValues.argString(objects21, 0), getColor(objects21, 3), ScriptValues.argFloat(objects21, 4, 7.0F));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textRight",
            objects22 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects22, 1), ScriptValues.argFloat(objects22, 2), 0.0F, 0.0F)
                    .textTopRight(Fonts.getDefault(), ScriptValues.argString(objects22, 0), getColor(objects22, 3), ScriptValues.argFloat(objects22, 4, 7.0F));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textWrap",
            objects23 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects23, 1), ScriptValues.argFloat(objects23, 2), ScriptValues.argFloat(objects23, 3), 0.0F)
                    .scrollingTextTop(
                        Fonts.getDefault(),
                        ScriptValues.argString(objects23, 0),
                        0.0F,
                        0.0F,
                        ScriptValues.argFloat(objects23, 3),
                        getColor(objects23, 4),
                        ScriptValues.argFloat(objects23, 5, 7.0F)
                    );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "line",
            objects24 -> {
                drawLine(
                    getContext(),
                    ScriptValues.argFloat(objects24, 0),
                    ScriptValues.argFloat(objects24, 1),
                    ScriptValues.argFloat(objects24, 2),
                    ScriptValues.argFloat(objects24, 3),
                    ScriptValues.argFloat(objects24, 4, 1.0F),
                    getColor(objects24, 5)
                );
                return null;
            }
        );
        ScriptValues.putFunction(nativeObject, "circle", objects25 -> {
            float f3 = ScriptValues.argFloat(objects25, 2);
            circleRenderer(objects25, f3).circle(ScriptValues.argFloat(objects25, 4, 1.0F), getColor(objects25, 3));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "ring", objects26 -> {
            float f4 = ScriptValues.argFloat(objects26, 2);
            circleRenderer(objects26, f4).circle(ScriptValues.argFloat(objects26, 3, 1.5F), getColor(objects26, 4));
            return null;
        });
        ScriptValues.putFunction(
            nativeObject,
            "arc",
            objects27 -> {
                float f5 = ScriptValues.argFloat(objects27, 2);
                float f6 = ScriptValues.argFloat(objects27, 3);
                float f7 = ScriptValues.argFloat(objects27, 4) - f6;
                circleRenderer(objects27, f5)
                    .arc(
                        f6,
                        f7,
                        ScriptValues.argFloat(objects27, 5, 1.5F),
                        getColor(objects27, 6),
                        1.0F,
                        ScriptValues.argFloat(objects27, 7, 0.0F),
                        ScriptValues.argFloat(objects27, 8, 0.0F)
                    );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "icon",
            objects28 -> {
                Icon icon2 = findIcon(ScriptValues.argString(objects28, 0));
                if (icon2 != null) {
                    float f8 = ScriptValues.argFloat(objects28, 3, 8.0F);
                    new RenderUtil(getContext(), ScriptValues.argFloat(objects28, 1), ScriptValues.argFloat(objects28, 2), f8, f8)
                        .icon(icon2, 0.0F, 0.0F, f8, getColor(objects28, 4));
                }

                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "svg",
            objects29 -> {
                TextureSetup textureSetup = ScriptIconCache.getTextureSetup(resolvePath(script, ScriptValues.argString(objects29, 0)));
                if (textureSetup == null) {
                    return null;
                }

                DrawContext drawContext = getContext();
                float f9 = ScriptValues.argFloat(objects29, 3, 8.0F);
                TexturedShapeRenderer.drawTexture(
                    drawContext,
                    textureSetup,
                    drawContext.getMatrices(),
                    ScriptValues.argFloat(objects29, 1),
                    ScriptValues.argFloat(objects29, 2),
                    f9,
                    f9,
                    Radius.zero(),
                    SvgIconCache.getSdfSpread(),
                    -0.4F,
                    0.4F,
                    1.0F,
                    getColor(objects29, 4),
                    6
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "face",
            objects30 -> {
                TextureSetup textureSetup2 = ScriptPlayerApi.unwrapEntity(ScriptValues.hasArg(objects30, 0) ? objects30[0] : null) instanceof LivingEntity livingEntity
                    ? TargetHud.getSkinTextureSetup(livingEntity)
                    : null;
                if (textureSetup2 == null) {
                    return false;
                }

                TargetHud.drawPlayerHead(
                    getContext(),
                    textureSetup2,
                    ScriptValues.argFloat(objects30, 1),
                    ScriptValues.argFloat(objects30, 2),
                    ScriptValues.argFloat(objects30, 3),
                    getRadius(objects30, 4, 5),
                    alpha * ScriptValues.argFloat(objects30, 6, 1.0F)
                );
                return true;
            }
        );
        ScriptValues.putFunction(nativeObject, "font", objects31 -> {
            String string = ScriptValues.argString(objects31, 0);
            Supplier supplier = getBuiltinFont(string);
            if (supplier != null) {
                return wrapFont(supplier, string);
            }

            MsdfFont msdfFont = loadFont(script, string);
            return wrapFont(() -> msdfFont, string);
        });
        ScriptValues.putFunction(
            nativeObject, "counter", objects32 -> ScriptValues.hasArg(objects32, 0) ? createCounter(unwrapFont(objects32[0])) : createCounter(unwrapFont(null))
        );
        ScriptValues.putFunction(nativeObject, "particles", objects33 -> createParticles());
        ScriptValues.putFunction(
            nativeObject,
            "item",
            objects34 -> {
                ItemStack itemStack = ScriptConvertUtil.unwrapStack(ScriptValues.hasArg(objects34, 0) ? objects34[0] : null);
                ItemStack itemStack2 = itemStack != null ? itemStack : createItemStack(ScriptValues.argString(objects34, 0));
                float f10 = ScriptValues.argFloat(objects34, 3, 16.0F);
                if (!itemStack2.isEmpty() && !(f10 <= 0.0F)) {
                    TargetHud.drawItemStack(
                        getContext(),
                        itemStack2,
                        ScriptValues.argFloat(objects34, 1),
                        ScriptValues.argFloat(objects34, 2),
                        f10,
                        alpha * ScriptValues.argFloat(objects34, 4, 1.0F)
                    );
                    return null;
                } else {
                    return null;
                }
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "clip",
            objects35 -> {
                Function function = ScriptValues.argFunction(objects35, 4);
                if (function == null) {
                    return null;
                }

                DrawContext drawContext2 = getContext();
                PixelRenderUtil.scissor(
                    drawContext2,
                    ScriptValues.argFloat(objects35, 0),
                    ScriptValues.argFloat(objects35, 1),
                    ScriptValues.argFloat(objects35, 2),
                    ScriptValues.argFloat(objects35, 3),
                    () -> script.call(function)
                );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "image",
            objects36 -> {
                DrawContext drawContext3 = getContext();
                String string2 = ScriptValues.argString(objects36, 0);
                if (ScriptTextureLoader.bind(string2)) {
                    float f11 = alpha * ScriptValues.argFloat(objects36, 6, 1.0F);
                    new RenderUtil(
                            drawContext3,
                            ScriptValues.argFloat(objects36, 1),
                            ScriptValues.argFloat(objects36, 2),
                            ScriptValues.argFloat(objects36, 3),
                            ScriptValues.argFloat(objects36, 4)
                        )
                        .tintedGlass(ColorUtil.multiplyAlpha(-1, f11), f11, getRadius(objects36, 5, 7));
                }

                return null;
            }
        );
        ScriptValues.putFunction(nativeObject, "imageSize", objects37 -> {
            ScriptTextureLoader.ScriptTexture scriptTexture = ScriptTextureLoader.load(ScriptValues.argString(objects37, 0));
            if (scriptTexture != null && scriptTexture.width() > 0) {
                NativeObject nativeObject3 = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject3, "width", scriptTexture.width());
                ScriptValues.putValue(nativeObject3, "height", scriptTexture.height());
                return nativeObject3;
            } else {
                return null;
            }
        });
        ScriptValues.putFunction(
            nativeObject,
            "animatedImage",
            objects38 -> {
                drawAnimatedImage(
                    ScriptValues.argString(objects38, 0),
                    ScriptValues.argFloat(objects38, 1),
                    ScriptValues.argFloat(objects38, 2),
                    ScriptValues.argFloat(objects38, 3),
                    ScriptValues.argFloat(objects38, 4),
                    getRadius(objects38, 5, 6)
                );
                return null;
            }
        );
        scriptableObject.put("render", scriptableObject, nativeObject);
    }

    private static Supplier<MsdfFont> getBuiltinFont(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        byte b = -1;
        switch (string2.hashCode()) {
            case -1078030475:
                if (string2.equals("medium")) {
                    return () -> Fonts.INTER_MEDIUM;
                }
                break;
            case 3029637:
                if (string2.equals("bold")) {
                    return () -> Fonts.INTER_BOLD;
                }
                break;
            case 1086463900:
                if (string2.equals("regular")) {
                    b = 3;
                }
                break;
            case 1223860979:
                if (string2.equals("semibold")) {
                    return () -> Fonts.INTER_SEMIBOLD;
                }
        }

        switch (b) {
            case 0:
                return () -> Fonts.INTER_BOLD;
            case 1:
                return () -> Fonts.INTER_SEMIBOLD;
            case 2:
                return () -> Fonts.INTER_MEDIUM;
            case 3:
                return () -> Fonts.INTER_REGULAR;
            default:
                return null;
        }
    }

    private static MsdfFont loadFont(Script script, String string) {
        Path path = resolvePath(script, string + ".png");
        Path path2 = resolvePath(script, string + ".json");
        return FONT_CACHE.computeIfAbsent(path.toString(), string2 -> {
            try {
                return new MsdfFont(path, path2);
            } catch (Exception exception) {
                throw Context.reportRuntimeError("render.font: " + exception.getMessage());
            }
        });
    }

    private static NativeObject wrapFont(Supplier<MsdfFont> supplier, String string) {
        NativeObject nativeObject = ScriptValues.newObject();
        nativeObject.associateValue(FONT_KEY, supplier);
        ScriptValues.putValue(nativeObject, "name", string);
        ScriptValues.putFunction(
            nativeObject,
            "text",
            objects -> {
                ((MsdfFont)supplier.get())
                    .draw(
                        getContext(),
                        ScriptValues.argString(objects, 0),
                        ScriptValues.argFloat(objects, 1),
                        ScriptValues.argFloat(objects, 2),
                        getColor(objects, 3),
                        ScriptValues.argFloat(objects, 4, 7.0F)
                    );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textCenter",
            objects2 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects2, 1), ScriptValues.argFloat(objects2, 2), 0.0F, 0.0F)
                    .textTopCenter((MsdfFont)supplier.get(), ScriptValues.argString(objects2, 0), getColor(objects2, 3), ScriptValues.argFloat(objects2, 4, 7.0F));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textRight",
            objects3 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects3, 1), ScriptValues.argFloat(objects3, 2), 0.0F, 0.0F)
                    .textTopRight((MsdfFont)supplier.get(), ScriptValues.argString(objects3, 0), getColor(objects3, 3), ScriptValues.argFloat(objects3, 4, 7.0F));
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "textWrap",
            objects4 -> {
                new RenderUtil(getContext(), ScriptValues.argFloat(objects4, 1), ScriptValues.argFloat(objects4, 2), ScriptValues.argFloat(objects4, 3), 0.0F)
                    .scrollingTextTop(
                        (MsdfFont)supplier.get(),
                        ScriptValues.argString(objects4, 0),
                        0.0F,
                        0.0F,
                        ScriptValues.argFloat(objects4, 3),
                        getColor(objects4, 4),
                        ScriptValues.argFloat(objects4, 5, 7.0F)
                    );
                return null;
            }
        );
        ScriptValues.putFunction(
            nativeObject, "width", objects5 -> ((MsdfFont)supplier.get()).getWidth(ScriptValues.argString(objects5, 0), ScriptValues.argFloat(objects5, 1, 7.0F))
        );
        ScriptValues.putFunction(nativeObject, "height", objects6 -> ((MsdfFont)supplier.get()).getHeight(ScriptValues.argFloat(objects6, 0, 7.0F)));
        return nativeObject;
    }

    private static Supplier<MsdfFont> unwrapFont(Object object) {
        Object object2 = object instanceof NativeObject ? ((NativeObject)object).getAssociatedValue(FONT_KEY) : null;
        return object2 instanceof Supplier ? (Supplier)object2 : Fonts::getDefault;
    }

    private static NativeObject createCounter(Supplier<MsdfFont> supplier) {
        RollingNumberRenderer rollingNumberRenderer = new RollingNumberRenderer();
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "reset", objects -> {
            rollingNumberRenderer.reset();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "update", objects2 -> {
            rollingNumberRenderer.update(Math.round(ScriptValues.argDouble(objects2, 0, 0.0)));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "width", objects3 -> rollingNumberRenderer.getWidth((MsdfFont)supplier.get(), ScriptValues.argFloat(objects3, 0, 7.0F)));
        ScriptValues.putFunction(
            nativeObject,
            "draw",
            objects4 -> {
                rollingNumberRenderer.render(
                    getContext(),
                    (MsdfFont)supplier.get(),
                    ScriptValues.argFloat(objects4, 0),
                    ScriptValues.argFloat(objects4, 1),
                    ScriptValues.argFloat(objects4, 2, 7.0F),
                    getColor(objects4, 3)
                );
                return null;
            }
        );
        return nativeObject;
    }

    private static NativeObject createParticles() {
        GuiParticleSystem guiParticleSystem = new GuiParticleSystem();
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "spawn", objects -> {
            GuiParticle guiParticle = parseParticle(ScriptValues.hasArg(objects, 0) ? objects[0] : null);
            guiParticleSystem.spawn(1, () -> guiParticle);
            return null;
        });
        ScriptValues.putFunction(nativeObject, "rebase", objects2 -> {
            guiParticleSystem.applyForce(ScriptValues.argFloat(objects2, 0), ScriptValues.argFloat(objects2, 1));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "update", objects3 -> {
            guiParticleSystem.update();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "render", objects4 -> {
            guiParticleSystem.render(getContext(), alpha * ScriptValues.argFloat(objects4, 0, 1.0F));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "clear", objects5 -> {
            guiParticleSystem.clear();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "empty", objects6 -> guiParticleSystem.isEmpty());
        return nativeObject;
    }

    private static GuiParticle parseParticle(Object object) {
        return GuiParticle.builder()
            .texture(parseParticleTexture(ScriptValues.getProperty(object, "texture")))
            .color((int)((long)ScriptValues.getNumber(object, "color", 4.294967295E9)))
            .position((float)ScriptValues.getNumber(object, "x", 0.0), (float)ScriptValues.getNumber(object, "y", 0.0))
            .offsetX((float)ScriptValues.getNumber(object, "offsetX", 0.0))
            .offsetY((float)ScriptValues.getNumber(object, "offsetY", 0.0))
            .motionX((float)ScriptValues.getNumber(object, "motionX", 0.0))
            .motionY((float)ScriptValues.getNumber(object, "motionY", 0.0))
            .size((float)ScriptValues.getNumber(object, "size", 3.0))
            .lifetime((long)ScriptValues.getNumber(object, "life", 1500.0))
            .fadeIn((long)ScriptValues.getNumber(object, "fadeIn", 150.0))
            .fadeOut((long)ScriptValues.getNumber(object, "fadeOut", 600.0))
            .gravity((float)ScriptValues.getNumber(object, "gravity", 0.0))
            .friction((float)ScriptValues.getNumber(object, "friction", 0.0))
            .spin((float)ScriptValues.getNumber(object, "spin", 0.0))
            .build();
    }

    private static Texture parseParticleTexture(Object object) {
        if (object == null) {
            return Texture.BLOOM;
        }

        String string = "particle/" + Context.toString(object).toLowerCase(Locale.ROOT) + ".png";

        for (Texture texture : Texture.values()) {
            if (texture.getPath().equals(string)) {
                return texture;
            }
        }

        return Texture.BLOOM;
    }

    private static RenderUtil circleRenderer(Object[] objects, float f) {
        return new RenderUtil(getContext(), ScriptValues.argFloat(objects, 0) - f, ScriptValues.argFloat(objects, 1) - f, f * 2.0F, f * 2.0F);
    }

    static Path resolvePath(Script script, String string) {
        return ScriptPathValidator.resolveSafe(script.getAssetsDir(), string);
    }

    private static DrawContext getContext() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        DrawContext drawContext = scriptManager == null ? null : scriptManager.getDrawContext();
        if (drawContext == null) {
            throw Context.reportRuntimeError("render.* доступен только внутри рендер-событий (render2d, clickgui_bg, ...)");
        } else {
            return drawContext;
        }
    }

    private static int getColor(Object[] objects, int i) {
        int i2 = ScriptValues.argIntOrDefault(objects, i, -1);
        return alpha < 0.999F ? ColorUtil.multiplyAlpha(i2, Math.max(0.0F, Math.min(1.0F, alpha))) : i2;
    }

    private static Radius getRadius(Object[] objects, int i, int i2) {
        float f = ScriptValues.argFloat(objects, i, 0.0F);
        if (f <= 0.0F) {
            return Radius.zero();
        } else {
            return ScriptValues.argBoolean(objects, i2, false) ? Radius.smoothAll(f) : Radius.all(f);
        }
    }

    static Icon findIcon(String string) {
        if (string == null) {
            return null;
        }

        for (Icon icon : Icon.values()) {
            if (icon.name().equalsIgnoreCase(string)) {
                return icon;
            }
        }

        return null;
    }

    private static ItemStack createItemStack(String string) {
        try {
            return Registries.ITEM.get(Identifier.of(string)).getDefaultStack();
        } catch (Exception exception) {
            return ItemStack.EMPTY;
        }
    }

    private static void drawLine(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i) {
        float f6 = f3 - f;
        float f7 = f4 - f2;
        float f8 = (float)Math.sqrt(f6 * f6 + f7 * f7);
        if (!(f8 < 0.001F) && !(f5 <= 0.0F)) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f, f2, (float)Math.toDegrees(Math.atan2(f7, f6)));
            new RenderUtil(drawContext, f, f2 - f5 * 0.5F, f8, f5).rect(i);
            MatrixUtil.pop(drawContext);
        }
    }

    private static void drawArc(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        if (!(f3 <= 0.0F)) {
            int i2 = Math.max(8, (int)Math.ceil(f3 * Math.PI / 2.0));
            float f7 = f5 - f4;

            while (f7 <= 0.0F) {
                f7 += 360.0F;
            }

            if (f7 > 360.0F) {
                f7 = 360.0F;
            }

            float f8 = f7 / i2;
            float f9 = f + f3 * (float)Math.cos(Math.toRadians(f4));
            float f10 = f2 + f3 * (float)Math.sin(Math.toRadians(f4));

            for (int i3 = 1; i3 <= i2; i3++) {
                float f11 = f4 + i3 * f8;
                float f12 = f + f3 * (float)Math.cos(Math.toRadians(f11));
                float f13 = f2 + f3 * (float)Math.sin(Math.toRadians(f11));
                drawLine(drawContext, f9, f10, f12, f13, Math.max(1.0F, f6), i);
                f9 = f12;
                f10 = f13;
            }
        }
    }

    private static void drawAnimatedImage(String string, float f, float f2, float f3, float f4, Radius radius) {
        DrawContext drawContext = getContext();
        ScriptTextureLoader.ScriptTexture scriptTexture = ScriptTextureLoader.load(string);
        if (scriptTexture != null && ScriptTextureLoader.bind(string)) {
            int i = scriptTexture.cols() > 0 ? scriptTexture.cols() : 8;
            int i2 = scriptTexture.rows() > 0 ? scriptTexture.rows() : 8;
            int i3 = scriptTexture.frames() > 0 ? scriptTexture.frames() : i * i2;
            float f5 = scriptTexture.fps() > 0.0F ? scriptTexture.fps() : 30.0F;
            double d = (System.nanoTime() - START_TIME) / 1.0E9;
            int i4 = Math.floorMod((long)Math.floor(d * f5), i3);
            float f6 = 1.0F / i;
            float f7 = 1.0F / i2;
            float f8 = i4 % i * f6;
            float f9 = i4 / i * f7;
            TextureSetup textureSetup = GuiRenderUtil.getBoundTexture();
            if (textureSetup != null) {
                int i5 = ColorUtil.multiplyAlpha(-1, alpha);
                if (radius.getMax() > 0.001F) {
                    TexturedShapeRenderer.drawOutline(
                        drawContext, textureSetup, drawContext.getMatrices(), f, f2, f3, f4, f8, f9, f8 + f6, f9 + f7, radius, alpha, i5
                    );
                } else {
                    TexturedShapeRenderer.drawTextureRegion(
                        drawContext,
                        textureSetup,
                        drawContext.getMatrices(),
                        f,
                        f2,
                        f3,
                        f4,
                        f8,
                        f9,
                        f8 + f6,
                        f9 + f7,
                        Radius.zero(),
                        0.0F,
                        -0.5F,
                        0.5F,
                        alpha,
                        i5,
                        i5,
                        2,
                        false
                    );
                }
            }
        }
    }
}
