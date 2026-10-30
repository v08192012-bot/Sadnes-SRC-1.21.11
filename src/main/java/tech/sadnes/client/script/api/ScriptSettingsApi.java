package tech.sadnes.client.script.api;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.Window;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.setting.WaveCurve;
import tech.sadnes.client.setting.curve.BezierCurve;
import tech.sadnes.client.setting.curve.CurveSetting;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptSettingsApi {
    private ScriptSettingsApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        installFactories(nativeObject, script, script::addSetting);
        scriptableObject.put("settings", scriptableObject, nativeObject);
    }

    public static void installFactories(NativeObject nativeObject, Script script, Consumer<Setting<?>> consumer) {
        ScriptValues.putFunction(nativeObject, "checkbox", objects -> {
            BooleanSetting booleanSetting = new BooleanSetting(ScriptValues.argString(objects, 0, "Option"), ScriptValues.argBoolean(objects, 1, false));
            consumer.accept(booleanSetting);
            NativeObject nativeObject2 = wrapScriptSetting(booleanSetting, script);
            ScriptValues.putFunction(nativeObject2, "get", objects2 -> booleanSetting.getValue());
            ScriptValues.putFunction(nativeObject2, "set", objects3 -> {
                booleanSetting.setValue(ScriptValues.argBoolean(objects3, 0, false));
                return null;
            });
            ScriptValues.putFunction(nativeObject2, "toggle", objects4 -> {
                if (!Boolean.TRUE.equals(booleanSetting.getValue())) {
                    booleanSetting.setValue(true);
                    return null;
                } else {
                    booleanSetting.setValue(false);
                    return null;
                }
            });
            return nativeObject2;
        });
        ScriptValues.putFunction(
            nativeObject,
            "slider",
            objects5 -> {
                NumberSetting numberSetting = new NumberSetting(
                    ScriptValues.argString(objects5, 0, "Value"),
                    ScriptValues.argFloat(objects5, 1, 0.0F),
                    ScriptValues.argFloat(objects5, 2, 0.0F),
                    ScriptValues.argFloat(objects5, 3, 1.0F),
                    ScriptValues.argFloat(objects5, 4, 0.1F),
                    parseSuffix(ScriptValues.argString(objects5, 5))
                );
                consumer.accept(numberSetting);
                NativeObject nativeObject3 = wrapScriptSetting(numberSetting, script);
                ScriptValues.putFunction(nativeObject3, "get", objects6 -> numberSetting.getValue().doubleValue());
                ScriptValues.putFunction(nativeObject3, "set", objects7 -> {
                    numberSetting.setValue(Float.valueOf(ScriptValues.argFloat(objects7, 0)));
                    return null;
                });
                return nativeObject3;
            }
        );
        ScriptValues.putFunction(nativeObject, "mode", objects8 -> {
            String[] strings = ScriptValues.argStringArray(objects8, 1);
            if (strings.length == 0) {
                strings = new String[]{"Default"};
            }

            ModeSetting modeSetting = new ModeSetting(ScriptValues.argString(objects8, 0, "Mode"), ScriptValues.argString(objects8, 2, strings[0]), strings);
            consumer.accept(modeSetting);
            NativeObject nativeObject4 = wrapScriptSetting(modeSetting, script);
            ScriptValues.putFunction(nativeObject4, "get", objects9 -> modeSetting.getValue());
            ScriptValues.putFunction(nativeObject4, "set", objects10 -> {
                modeSetting.setValue(ScriptValues.argString(objects10, 0));
                return null;
            });
            ScriptValues.putFunction(nativeObject4, "is", objects11 -> modeSetting.is(ScriptValues.argString(objects11, 0)));
            return nativeObject4;
        });
        ScriptValues.putFunction(nativeObject, "multi", objects12 -> {
            String[] strings2 = ScriptValues.argStringArray(objects12, 1);
            List list = List.of(ScriptValues.argStringArray(objects12, 2));
            BooleanSetting[] booleanSettings = new BooleanSetting[strings2.length];

            for (int i = 0; i < strings2.length; i++) {
                booleanSettings[i] = BooleanSetting.of(strings2[i], list.contains(strings2[i]));
            }

            MultiSetting multiSetting = new MultiSetting(ScriptValues.argString(objects12, 0, "Options"), booleanSettings);
            consumer.accept(multiSetting);
            NativeObject nativeObject5 = wrapScriptSetting(multiSetting, script);
            ScriptValues.putFunction(nativeObject5, "get", objects13 -> ScriptValues.toArray(new ArrayList<>(multiSetting.getValue())));
            ScriptValues.putFunction(nativeObject5, "set", objects14 -> {
                multiSetting.setValue(new ArrayList<>(List.of(ScriptValues.argStringArray(objects14, 0))));
                return null;
            });
            ScriptValues.putFunction(nativeObject5, "enabled", objects15 -> {
                String string = ScriptValues.argString(objects15, 0);

                for (String string2 : multiSetting.getValue()) {
                    if (string2.equalsIgnoreCase(string)) {
                        return true;
                    }
                }

                return false;
            });
            return nativeObject5;
        });
        ScriptValues.putFunction(nativeObject, "color", objects16 -> {
            ColorSetting colorSetting = new ColorSetting(ScriptValues.argString(objects16, 0, "Color"), ScriptValues.argIntOrDefault(objects16, 1, -1));
            consumer.accept(colorSetting);
            NativeObject nativeObject6 = wrapScriptSetting(colorSetting, script);
            ScriptValues.putFunction(nativeObject6, "get", objects17 -> colorSetting.getValue());
            ScriptValues.putFunction(nativeObject6, "set", objects18 -> {
                colorSetting.setValue(ScriptValues.argIntOrDefault(objects18, 0, -1));
                return null;
            });
            return nativeObject6;
        });
        ScriptValues.putFunction(nativeObject, "text", objects19 -> {
            TextSetting textSetting = new TextSetting(ScriptValues.argString(objects19, 0, "Text"), ScriptValues.argString(objects19, 1, ""));
            consumer.accept(textSetting);
            NativeObject nativeObject7 = wrapScriptSetting(textSetting, script);
            ScriptValues.putFunction(nativeObject7, "get", objects20 -> textSetting.getValue());
            ScriptValues.putFunction(nativeObject7, "set", objects21 -> {
                textSetting.setValue(ScriptValues.argString(objects21, 0));
                return null;
            });
            return nativeObject7;
        });
        ScriptValues.putFunction(nativeObject, "bind", objects22 -> {
            KeybindSetting keybindSetting = new KeybindSetting(ScriptValues.argString(objects22, 0, "Key"), ScriptValues.argInt(objects22, 1, -1));
            consumer.accept(keybindSetting);
            NativeObject nativeObject8 = wrapScriptSetting(keybindSetting, script);
            ScriptValues.putFunction(nativeObject8, "get", objects23 -> keybindSetting.getValue());
            ScriptValues.putFunction(nativeObject8, "set", objects24 -> {
                keybindSetting.setValue(Integer.valueOf(ScriptValues.argInt(objects24, 0, -1)));
                return null;
            });
            ScriptValues.putFunction(nativeObject8, "isKey", objects25 -> keybindSetting.matchesKey(ScriptValues.argInt(objects25, 0, -1)));
            return nativeObject8;
        });
        ScriptValues.putFunction(nativeObject, "button", objects26 -> {
            Function function = ScriptValues.argFunction(objects26, 1);
            ButtonSetting buttonSetting = new ButtonSetting(ScriptValues.argString(objects26, 0, "Action"), () -> {
                if (function != null) {
                    script.call(function);
                }
            });
            consumer.accept(buttonSetting);
            return wrapScriptSetting(buttonSetting, script);
        });
        ScriptValues.putFunction(nativeObject, "group", objects27 -> {
            GroupSetting groupSetting = new GroupSetting(ScriptValues.argString(objects27, 0, "Group"), ScriptValues.argBoolean(objects27, 1, false));
            consumer.accept(groupSetting);
            NativeObject nativeObject9 = wrapScriptSetting(groupSetting, script);
            ScriptValues.putFunction(nativeObject9, "get", objects28 -> groupSetting.getValue());
            ScriptValues.putFunction(nativeObject9, "set", objects29 -> {
                groupSetting.setValue(ScriptValues.argBoolean(objects29, 0, false));
                return null;
            });
            installFactories(nativeObject9, script, setting2 -> groupSetting.onInput(setting2));
            return nativeObject9;
        });
    }

    private static NativeObject wrapScriptSetting(Setting<?> setting, Script script) {
        NativeObject nativeObject = wrapBase(setting, setting.getName());
        ScriptValues.putFunction(nativeObject, "visibleIf", objects -> {
            Function function = ScriptValues.argFunction(objects, 0);
            if (function != null) {
                setting.setVisibleWhen(() -> {
                    if (script.isActive() && !script.hasError()) {
                        Object object = script.call(function);
                        return object != null && Context.toBoolean(object) ? true : false;
                    } else {
                        return true;
                    }
                });
            }

            return nativeObject;
        });
        return nativeObject;
    }

    public static NativeObject wrapSetting(Setting<?> setting) {
        NativeObject object = wrapBase(setting, Language.translate(setting.getName()));
        ScriptValues.putFunction(object, "get", args -> getJsValue(setting));
        switch (setting) {
            case GroupSetting group -> {
                ScriptValues.putFunction(object, "set", args -> {
                    group.setValue(ScriptValues.argBoolean(args, 0, false));
                    return null;
                });
                ScriptValues.putFunction(object, "toggle", args -> {
                    group.toggle();
                    return null;
                });
                ScriptValues.putFunction(object, "children", args -> {
                    List<Object> children = new ArrayList<>();
                    for (Setting<?> child : group.getChildList()) {
                        children.add(wrapSetting(child));
                    }
                    return ScriptValues.toArray(children);
                });
            }
            case BooleanSetting bool -> {
                ScriptValues.putFunction(object, "set", args -> {
                    bool.setValue(ScriptValues.argBoolean(args, 0, false));
                    return null;
                });
                ScriptValues.putFunction(object, "toggle", args -> {
                    if (!Boolean.TRUE.equals(bool.getValue())) {
                        bool.setValue(true);
                        return null;
                    }
                    bool.setValue(false);
                    return null;
                });
            }
            case NumberSetting number -> {
                ScriptValues.putValue(object, "min", number.getMin());
                ScriptValues.putValue(object, "max", number.getMax());
                ScriptValues.putValue(object, "step", number.getStep());
                ScriptValues.putFunction(object, "set", args -> {
                    number.setValue(Float.valueOf(ScriptValues.argFloat(args, 0)));
                    return null;
                });
            }
            case ModeSetting mode -> {
                ScriptValues.putFunction(object, "set", args -> {
                    mode.setValue(ScriptValues.argString(args, 0));
                    return null;
                });
                ScriptValues.putFunction(object, "is", args -> mode.is(ScriptValues.argString(args, 0)));
                ScriptValues.putFunction(object, "options", args -> ScriptValues.toArray(mode.getModes()));
            }
            case MultiSetting multi -> {
                ScriptValues.putFunction(object, "enabled", args -> {
                    String name = ScriptValues.argString(args, 0);
                    for (String enabled : multi.getValue()) {
                        if (enabled.equalsIgnoreCase(name)) {
                            return true;
                        }
                    }
                    return false;
                });
                ScriptValues.putFunction(object, "set", args -> {
                    multi.setValue(new ArrayList<>(List.of(ScriptValues.argStringArray(args, 0))));
                    return null;
                });
            }
            case ColorSetting color -> ScriptValues.putFunction(object, "set", args -> {
                color.setValue(ScriptValues.argIntOrDefault(args, 0, -1));
                return null;
            });
            case TextSetting text -> ScriptValues.putFunction(object, "set", args -> {
                text.setValue(ScriptValues.argString(args, 0));
                return null;
            });
            case KeybindSetting keybind -> {
                ScriptValues.putFunction(object, "set", args -> {
                    keybind.setValue(Integer.valueOf(ScriptValues.argInt(args, 0, -1)));
                    return null;
                });
                ScriptValues.putFunction(object, "isKey", args -> keybind.matchesKey(ScriptValues.argInt(args, 0, -1)));
            }
            default -> {
            }
        }
        return object;
    }

    private static NativeObject wrapBase(Setting<?> setting, String string) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", string);
        ScriptValues.putValue(nativeObject, "id", setting.getDisplayName());
        ScriptValues.putValue(nativeObject, "type", getTypeName(setting));
        ScriptValues.putFunction(nativeObject, "visible", objects -> setting.isVisible());
        ScriptValues.putFunction(nativeObject, "render", objects2 -> renderSetting(setting, objects2));
        ScriptValues.putFunction(nativeObject, "overlay", objects3 -> {
            renderOverlay(setting);
            return null;
        });
        ScriptValues.putFunction(nativeObject, "overlayOpen", objects4 -> setting.isOverlayOpen());
        ScriptValues.putFunction(nativeObject, "height", objects5 -> setting.resolveHeight(20.0F));
        ScriptValues.putFunction(nativeObject, "click", objects6 -> setting.mouseClicked(parseClick(objects6), false));
        ScriptValues.putFunction(nativeObject, "release", objects7 -> setting.onMouseRelease(parseClick(objects7)));
        ScriptValues.putFunction(nativeObject, "drag", objects8 -> {
            Vector2f vector2f = getMousePos();
            return setting.mouseDragged(new Click(vector2f.x, vector2f.y, new MouseInput(0, 0)), 0.0, 0.0);
        });
        ScriptValues.putFunction(
            nativeObject,
            "key",
            objects9 -> setting.onKeyPress(new KeyInput(ScriptValues.argInt(objects9, 0, -1), ScriptValues.argInt(objects9, 1, 0), ScriptValues.argInt(objects9, 2, 0)))
        );
        ScriptValues.putFunction(
            nativeObject,
            "char",
            objects10 -> {
                int i = ScriptValues.hasArg(objects10, 0) && objects10[0] instanceof CharSequence charSequence && !charSequence.isEmpty()
                    ? charSequence.toString().codePointAt(0)
                    : ScriptValues.argInt(objects10, 0, 0);
                return setting.onCharType(new CharInput(i, ScriptValues.argInt(objects10, 1, 0)));
            }
        );
        return nativeObject;
    }

    private static String getTypeName(Setting<?> setting) {
        return switch (setting) {
            case GroupSetting group -> "group";
            case BooleanSetting bool -> "checkbox";
            case NumberSetting number -> "slider";
            case ModeSetting mode -> "mode";
            case MultiSetting multi -> "multi";
            case ColorSetting color -> "color";
            case TextSetting text -> "text";
            case KeybindSetting keybind -> "bind";
            case ButtonSetting button -> "button";
            case CurveSetting curve -> "curve";
            default -> "other";
        };
    }

    private static Object getJsValue(Setting<?> setting) {
        Object object = setting.getValue();
        if (object instanceof BezierCurve) {
            object = ((BezierCurve)object).toArray();
        }

        if (object instanceof WaveCurve) {
            object = ((WaveCurve)object).toArray();
        }

        if (object != null && object.getClass().isArray()) {
            ArrayList arrayList = new ArrayList();

            for (int i = 0; i < Array.getLength(object); i++) {
                arrayList.add(Array.get(object, i));
            }

            return ScriptValues.toArray(arrayList);
        } else {
            return object instanceof List list ? ScriptValues.toArray(new ArrayList(list)) : object;
        }
    }

    private static double renderSetting(Setting<?> setting, Object[] objects) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        DrawContext drawContext = scriptManager == null ? null : scriptManager.getDrawContext();
        if (drawContext == null) {
            return 0.0;
        }

        float f = setting.resolveHeight(20.0F);
        setting.alphaMultiplier = ScriptValues.argFloat(objects, 3, 1.0F);
        setting.alphaAnimation.update();
        setting.alphaAnimation.animate(setting.isVisible() ? 1.0 : 0.0, 0.2, Easings.EXPO_OUT);
        setting.position.set(ScriptValues.argFloat(objects, 0), ScriptValues.argFloat(objects, 1));
        setting.size.set(ScriptValues.argFloat(objects, 2, 150.0F), f);
        Vector2f vector2f = getMousePos();
        setting.ignoreGuiAlpha = true;

        try {
            setting.render(drawContext, (int)vector2f.x, (int)vector2f.y, scriptManager.getTickDelta());
            if (!ScriptValues.argBoolean(objects, 4, false)) {
                setting.renderOverlay(drawContext, (int)vector2f.x, (int)vector2f.y, scriptManager.getTickDelta());
            }
        } finally {
            setting.ignoreGuiAlpha = false;
        }

        return f * setting.alphaAnimation.get();
    }

    private static void renderOverlay(Setting<?> setting) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        DrawContext drawContext = scriptManager == null ? null : scriptManager.getDrawContext();
        if (drawContext != null) {
            Vector2f vector2f = getMousePos();
            setting.ignoreGuiAlpha = true;

            try {
                setting.renderOverlay(drawContext, (int)vector2f.x, (int)vector2f.y, scriptManager.getTickDelta());
            } finally {
                setting.ignoreGuiAlpha = false;
            }
        }
    }

    private static Unit parseSuffix(String string) {
        for (Unit unit : Unit.values()) {
            if (!string.isEmpty() && (unit.getSuffix().equalsIgnoreCase(string) || unit.name().equalsIgnoreCase(string))) {
                return unit;
            }
        }

        return Unit.EMPTY;
    }

    private static Vector2f getMousePos() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        Mouse mouse2 = minecraftClient.mouse;
        double d = window.getWidth() == 0 ? 0.0 : mouse2.getX() * window.getScaledWidth() / window.getWidth();
        double d2 = window.getHeight() == 0 ? 0.0 : mouse2.getY() * window.getScaledHeight() / window.getHeight();
        return GuiScale.isActive() ? new Vector2f((float)d, (float)d2) : GuiScale.toScaledPos(d, d2);
    }

    private static Click parseClick(Object[] objects) {
        return new Click(ScriptValues.argDouble(objects, 0, 0.0), ScriptValues.argDouble(objects, 1, 0.0), new MouseInput(ScriptValues.argInt(objects, 2, 0), 0));
    }
}
