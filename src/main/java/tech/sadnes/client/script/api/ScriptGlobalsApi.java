package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptCategory;
import tech.sadnes.client.script.ScriptConsole;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptStorage;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptGlobalsApi {
    private static final Map<String, Easing> EASINGS = Map.ofEntries(
        Map.entry("LINEAR", Easings.LINEAR),
        Map.entry("QUAD_IN", Easings.QUAD_IN),
        Map.entry("QUAD_OUT", Easings.QUAD_OUT),
        Map.entry("QUAD_IN_OUT", Easings.QUAD_IN_OUT),
        Map.entry("CUBIC_IN", Easings.CUBIC_IN),
        Map.entry("CUBIC_OUT", Easings.CUBIC_OUT),
        Map.entry("CUBIC_IN_OUT", Easings.CUBIC_IN_OUT),
        Map.entry("QUART_IN", Easings.QUART_IN),
        Map.entry("QUART_OUT", Easings.QUART_OUT),
        Map.entry("QUART_IN_OUT", Easings.QUART_IN_OUT),
        Map.entry("QUINT_IN", Easings.QUINT_IN),
        Map.entry("QUINT_OUT", Easings.QUINT_OUT),
        Map.entry("QUINT_IN_OUT", Easings.QUINT_IN_OUT),
        Map.entry("SINE_IN", Easings.SINE_IN),
        Map.entry("SINE_OUT", Easings.SINE_OUT),
        Map.entry("SINE_IN_OUT", Easings.SINE_IN_OUT),
        Map.entry("CIRC_IN", Easings.CIRC_IN),
        Map.entry("CIRC_OUT", Easings.CIRC_OUT),
        Map.entry("CIRC_IN_OUT", Easings.CIRC_IN_OUT),
        Map.entry("ELASTIC_IN", Easings.ELASTIC_IN),
        Map.entry("ELASTIC_OUT", Easings.ELASTIC_OUT),
        Map.entry("ELASTIC_IN_OUT", Easings.ELASTIC_IN_OUT),
        Map.entry("EXPO_IN", Easings.EXPO_IN),
        Map.entry("EXPO_OUT", Easings.EXPO_OUT),
        Map.entry("EXPO_IN_OUT", Easings.EXPO_IN_OUT),
        Map.entry("BACK_IN", Easings.BACK_IN),
        Map.entry("BACK_OUT", Easings.BACK_OUT),
        Map.entry("BACK_IN_OUT", Easings.BACK_IN_OUT),
        Map.entry("BOUNCE_IN", Easings.BOUNCE_IN),
        Map.entry("BOUNCE_OUT", Easings.BOUNCE_OUT),
        Map.entry("BOUNCE_IN_OUT", Easings.BOUNCE_IN_OUT)
    );

    private ScriptGlobalsApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        scriptableObject.put("chat", scriptableObject, ScriptValues.function("chat", objects -> {
            ChatUtil.print(ScriptValues.argString(objects, 0));
            return null;
        }));
        scriptableObject.put("print", scriptableObject, ScriptValues.function("print", objects2 -> consoleLog(script, "print", objects2)));
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "log", objects3 -> consoleLog(script, "print", objects3));
        ScriptValues.putFunction(nativeObject, "info", objects4 -> consoleLog(script, "print", objects4));
        ScriptValues.putFunction(nativeObject, "warn", objects5 -> consoleLog(script, "warn", objects5));
        ScriptValues.putFunction(nativeObject, "error", objects6 -> consoleLog(script, "error", objects6));
        ScriptValues.putFunction(nativeObject, "clear", objects7 -> {
            ScriptConsole.clear();
            return null;
        });
        scriptableObject.put("console", scriptableObject, nativeObject);
        installClient(scriptableObject, script);
        installMc(scriptableObject);
        installColor(scriptableObject);
        installStorage(scriptableObject, script);
        installTimers(scriptableObject, script);
        installAnimation(scriptableObject);
    }

    private static Object consoleLog(Script script, String string, Object[] objects) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < objects.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }

            builder.append(ScriptValues.stringify(objects[i]));
        }

        String string2 = builder.toString();
        if ("print".equals(string)) {
            Sadnes.getLogger().info("[JS:{}] {}", script.getDisplayName(), string2);
            ScriptConsole.log(script.getDisplayName(), string, string2);
            return null;
        } else {
            Sadnes.getLogger().warn("[JS:{}] {}", script.getDisplayName(), string2);
            ScriptConsole.log(script.getDisplayName(), string, string2);
            return null;
        }
    }

    private static Object showNotification(NotificationType notificationType, String string, Object[] objects) {
        String string2 = ScriptValues.argString(objects, 0);
        Object object = ScriptValues.hasArg(objects, 1) ? objects[1] : null;
        Optional optional = getIdentifierProperty(object, "effect").flatMap(Registries.STATUS_EFFECT::getEntry);
        Optional optional2 = getIdentifierProperty(object, "item").flatMap(Registries.ITEM::getOptionalValue);
        if (optional.isPresent()) {
            NotificationUtil.notify(notificationType, string, (RegistryEntry<StatusEffect>)optional.get(), string2);
            return null;
        } else if (optional2.isPresent()) {
            NotificationUtil.notify(notificationType, string, ((Item)optional2.get()).getDefaultStack(), string2);
            return null;
        } else {
            NotificationUtil.notify(notificationType, string, string2);
            return null;
        }
    }

    private static Optional<Identifier> getIdentifierProperty(Object object, String string) {
        Object object2 = ScriptValues.getProperty(object, string);
        return object2 == null ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(Context.toString(object2)));
    }

    private static void installClient(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        String string = script.getTitle();
        String string2 = string != null && !string.isEmpty() ? string : NotificationSources.scripts();
        ScriptValues.putFunction(nativeObject, "msg", objects -> {
            ChatUtil.print(ScriptValues.argString(objects, 0));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "info", objects2 -> showNotification(NotificationType.INFO, string2, objects2));
        ScriptValues.putFunction(nativeObject, "success", objects3 -> showNotification(NotificationType.SUCCESS, string2, objects3));
        ScriptValues.putFunction(nativeObject, "warn", objects4 -> showNotification(NotificationType.WARNING, string2, objects4));
        ScriptValues.putFunction(nativeObject, "error", objects5 -> showNotification(NotificationType.ERROR, string2, objects5));
        ScriptValues.putFunction(
            nativeObject, "lang", objects6 -> Language.translate(ScriptValues.argString(objects6, 0), ScriptValues.argString(objects6, 1, ScriptValues.argString(objects6, 0)))
        );
        ScriptValues.putFunction(nativeObject, "username", objects7 -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            return minecraftClient.getSession() != null ? minecraftClient.getSession().getUsername() : "";
        });
        ScriptValues.putFunction(nativeObject, "server", objects8 -> {
            MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
            return minecraftClient2.getCurrentServerEntry() != null ? minecraftClient2.getCurrentServerEntry().address : "";
        });
        ScriptValues.putFunction(nativeObject, "clipboard", objects9 -> {
            MinecraftClient minecraftClient3 = MinecraftClient.getInstance();
            if (ScriptValues.hasArg(objects9, 0)) {
                minecraftClient3.keyboard.setClipboard(ScriptValues.argString(objects9, 0));
                return null;
            } else {
                return minecraftClient3.keyboard.getClipboard();
            }
        });
        ScriptValues.putFunction(nativeObject, "fps", objects10 -> MinecraftClient.getInstance().getCurrentFps());
        ScriptValues.putFunction(nativeObject, "deltaTime", objects11 -> MathUtil.getFrameTime());
        ScriptValues.putFunction(nativeObject, "funtime", objects12 -> ServerManager.isFunTime());
        ScriptValues.putFunction(nativeObject, "ping", objects13 -> {
            MinecraftClient minecraftClient4 = MinecraftClient.getInstance();
            if (minecraftClient4.player != null && minecraftClient4.getNetworkHandler() != null) {
                PlayerListEntry playerListEntry = minecraftClient4.getNetworkHandler().getPlayerListEntry(minecraftClient4.player.getUuid());
                return playerListEntry == null ? 0 : playerListEntry.getLatency();
            } else {
                return 0;
            }
        });
        ScriptValues.putFunction(nativeObject, "inGame", objects14 -> MinecraftClient.getInstance().world != null ? true : false);
        ScriptValues.putFunction(nativeObject, "screenOpen", objects15 -> MinecraftClient.getInstance().currentScreen != null ? true : false);
        ScriptValues.putFunction(nativeObject, "width", objects16 -> GuiScale.getScaledWidth());
        ScriptValues.putFunction(nativeObject, "height", objects17 -> GuiScale.getScaledHeight());
        ScriptValues.putFunction(nativeObject, "say", objects18 -> {
            ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
            String string3 = ScriptValues.argString(objects18, 0).trim();
            if (clientPlayNetworkHandler == null || string3.isEmpty()) {
                return false;
            } else if (string3.startsWith("/")) {
                clientPlayNetworkHandler.sendChatCommand(string3.substring(1));
                return true;
            } else {
                clientPlayNetworkHandler.sendChatMessage(string3);
                return true;
            }
        });
        ScriptValues.putFunction(nativeObject, "command", objects19 -> {
            ClientPlayNetworkHandler clientPlayNetworkHandler2 = MinecraftClient.getInstance().getNetworkHandler();
            String string4 = ScriptValues.argString(objects19, 0).trim();
            if (string4.startsWith("/")) {
                string4 = string4.substring(1);
            }

            if (clientPlayNetworkHandler2 != null && !string4.isEmpty()) {
                clientPlayNetworkHandler2.sendChatCommand(string4);
                return true;
            } else {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject, "isFriend", objects20 -> Sadnes.getInstance().getFriendManager().isFriend(ScriptValues.argString(objects20, 0)));
        ScriptValues.putFunction(nativeObject, "friends", objects21 -> ScriptValues.toArray(new ArrayList<>(Sadnes.getInstance().getFriendManager())));
        ScriptValues.putFunction(nativeObject, "module", objects22 -> wrapModule(findModule(ScriptValues.argString(objects22, 0))));
        ScriptValues.putFunction(nativeObject, "modules", objects23 -> {
            ArrayList arrayList = new ArrayList();

            for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
                if (!feature.isHidden()) {
                    arrayList.add(feature.getDisplayName());
                }
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject, "categories", objects24 -> {
            ArrayList arrayList2 = new ArrayList();

            for (Category category : Category.values()) {
                arrayList2.add(wrapCategory(category));
            }

            ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
            if (scriptManager != null) {
                for (ScriptCategory scriptCategory : scriptManager.getActiveHudStyles()) {
                    arrayList2.add(wrapCategory(scriptCategory));
                }
            }

            return ScriptValues.toArray(arrayList2);
        });
        scriptableObject.put("client", scriptableObject, nativeObject);
    }

    private static Module findModule(String string) {
        if (string != null && !string.isEmpty()) {
            for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
                if (!feature.isHidden() && feature.getDisplayName().equalsIgnoreCase(string)) {
                    return feature;
                }
            }

            ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
            if (scriptManager == null) {
                return null;
            }

            for (ScriptFeature scriptFeature : scriptManager.getActiveHudElements()) {
                if (scriptFeature.getDisplayName().equalsIgnoreCase(string)) {
                    return scriptFeature;
                }
            }
        }

        return null;
    }

    private static List<Module> getModulesInCategory(CategoryDescriptor categoryDescriptor) {
        ArrayList arrayList = new ArrayList();
        if (categoryDescriptor instanceof Category category) {
            for (Feature feature : Sadnes.getInstance().getFeatureManager().getByCategory(category)) {
                if (!feature.isHidden()) {
                    arrayList.add(feature);
                }
            }
        }

        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            for (ScriptFeature scriptFeature : scriptManager.getActiveHudElements()) {
                if (scriptFeature.getCategory() == categoryDescriptor) {
                    arrayList.add(scriptFeature);
                }
            }
        }

        return arrayList;
    }

    private static NativeObject wrapCategory(CategoryDescriptor categoryDescriptor) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", categoryDescriptor.getName());
        ScriptValues.putValue(nativeObject, "icon", categoryDescriptor.getIcon().name().toLowerCase(Locale.ROOT));
        ScriptValues.putValue(nativeObject, "description", categoryDescriptor.getDescription());
        ScriptValues.putFunction(nativeObject, "modules", objects -> {
            ArrayList arrayList = new ArrayList();

            for (Module module : getModulesInCategory(categoryDescriptor)) {
                arrayList.add(wrapModule(module));
            }

            return ScriptValues.toArray(arrayList);
        });
        return nativeObject;
    }

    private static Object wrapModule(Module module) {
        if (module == null) {
            return null;
        }

        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", module.getDisplayName());
        ScriptValues.putValue(nativeObject, "category", module.getCategory().getName());
        ScriptValues.putValue(nativeObject, "subcategory", module.getSubcategoryName());
        ScriptValues.putFunction(nativeObject, "description", objects -> module.getLocalizedDescription());
        ScriptValues.putFunction(nativeObject, "enabled", objects2 -> module.isEnabled());
        ScriptValues.putFunction(nativeObject, "isEnabled", objects3 -> module.isEnabled());
        ScriptValues.putFunction(nativeObject, "toggle", objects4 -> {
            module.toggle();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "setEnabled", objects5 -> {
            if (module.isEnabled() != ScriptValues.argBoolean(objects5, 0, false)) {
                module.toggle();
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "hasSettings", objects6 -> !getVisibleSettings(module).isEmpty() ? true : false);
        ScriptValues.putFunction(nativeObject, "settings", objects7 -> {
            ArrayList arrayList = new ArrayList();

            for (Setting setting : getVisibleSettings(module)) {
                arrayList.add(ScriptSettingsApi.wrapSetting(setting));
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject, "setting", objects8 -> {
            String string = ScriptValues.argString(objects8, 0);

            for (Setting setting2 : getVisibleSettings(module)) {
                for (Setting setting3 : withChildren(setting2)) {
                    if (setting3.getDisplayName().equalsIgnoreCase(string)) {
                        return ScriptSettingsApi.wrapSetting(setting3);
                    }
                }
            }

            return null;
        });
        return nativeObject;
    }

    private static List<Setting<?>> withChildren(Setting<?> setting) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(setting);
        arrayList.addAll(setting.getAllChildren());
        return arrayList;
    }

    private static List<Setting<?>> getVisibleSettings(Module module) {
        ArrayList arrayList = new ArrayList();

        for (Setting setting : module.getSettings()) {
            if (!(setting instanceof DragSetting) && module.isSettingVisible(setting)) {
                arrayList.add(setting);
            }
        }

        return arrayList;
    }

    private static void installMc(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "fps", objects -> MinecraftClient.getInstance().getCurrentFps());
        ScriptValues.putFunction(nativeObject, "ping", objects2 -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient.player != null && minecraftClient.getNetworkHandler() != null) {
                PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid());
                return playerListEntry == null ? 0 : playerListEntry.getLatency();
            } else {
                return 0;
            }
        });
        ScriptValues.putFunction(nativeObject, "inGame", objects3 -> MinecraftClient.getInstance().world != null ? true : false);
        ScriptValues.putFunction(nativeObject, "screenOpen", objects4 -> MinecraftClient.getInstance().currentScreen != null ? true : false);
        ScriptValues.putFunction(nativeObject, "screen", objects5 -> ScriptConvertUtil.getScreenName(MinecraftClient.getInstance().currentScreen));
        ScriptValues.putFunction(nativeObject, "paused", objects6 -> MinecraftClient.getInstance().isPaused());
        ScriptValues.putFunction(nativeObject, "width", objects7 -> GuiScale.getScaledWidth());
        ScriptValues.putFunction(nativeObject, "height", objects8 -> GuiScale.getScaledHeight());
        ScriptValues.putFunction(nativeObject, "windowWidth", objects9 -> MinecraftClient.getInstance().getWindow().getWidth());
        ScriptValues.putFunction(nativeObject, "windowHeight", objects10 -> MinecraftClient.getInstance().getWindow().getHeight());
        ScriptValues.putFunction(nativeObject, "fov", objects11 -> MinecraftClient.getInstance().options.getFov().getValue());
        ScriptValues.putFunction(nativeObject, "sensitivity", objects12 -> MinecraftClient.getInstance().options.getMouseSensitivity().getValue());
        ScriptValues.putFunction(nativeObject, "renderDistance", objects13 -> MinecraftClient.getInstance().options.getViewDistance().getValue());
        scriptableObject.put("mc", scriptableObject, nativeObject);
    }

    private static void installColor(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(
            nativeObject,
            "rgb",
            objects -> ColorUtil.rgba(
                ScriptValues.argInt(objects, 0, 255), ScriptValues.argInt(objects, 1, 255), ScriptValues.argInt(objects, 2, 255), ScriptValues.argInt(objects, 3, 255)
            )
        );
        ScriptValues.putFunction(nativeObject, "hex", objects2 -> {
            String string = ScriptValues.argString(objects2, 0).replace("#", "").trim();

            try {
                long l = Long.parseLong(string, 16);
                if (string.length() <= 6) {
                    l |= 4278190080L;
                }

                return (int)l;
            } catch (NumberFormatException numberFormatException) {
                return -1;
            }
        });
        ScriptValues.putFunction(nativeObject, "alpha", objects3 -> ColorUtil.setAlphaf(ScriptValues.argIntOrDefault(objects3, 0, -1), ScriptValues.argFloat(objects3, 1, 1.0F)));
        ScriptValues.putFunction(
            nativeObject, "multAlpha", objects4 -> ColorUtil.multiplyAlpha(ScriptValues.argIntOrDefault(objects4, 0, -1), ScriptValues.argFloat(objects4, 1, 1.0F))
        );
        ScriptValues.putFunction(nativeObject, "darker", objects5 -> ColorUtil.darken(ScriptValues.argIntOrDefault(objects5, 0, -1), ScriptValues.argFloat(objects5, 1, 0.85F)));
        ScriptValues.putFunction(
            nativeObject, "brighter", objects6 -> ColorUtil.brighten(ScriptValues.argIntOrDefault(objects6, 0, -1), ScriptValues.argFloat(objects6, 1, 0.85F))
        );
        ScriptValues.putFunction(
            nativeObject,
            "mix",
            objects7 -> ColorUtil.interpolate(
                ScriptValues.argIntOrDefault(objects7, 0, -1), ScriptValues.argIntOrDefault(objects7, 1, -1), ScriptValues.argFloat(objects7, 2, 0.5F)
            )
        );
        ScriptValues.putFunction(
            nativeObject,
            "rainbow",
            objects8 -> ColorUtil.rainbow(
                ScriptValues.argInt(objects8, 0, 10),
                ScriptValues.argInt(objects8, 1, 0),
                ScriptValues.argFloat(objects8, 2, 1.0F),
                ScriptValues.argFloat(objects8, 3, 1.0F),
                ScriptValues.argFloat(objects8, 4, 1.0F)
            )
        );
        ScriptValues.putFunction(
            nativeObject,
            "fade",
            objects9 -> ColorUtil.fade(
                ScriptValues.argInt(objects9, 0, 10), ScriptValues.argInt(objects9, 1, 0), Theme.getPrimary(), ColorUtil.darken(Theme.getPrimary(), 0.5F)
            )
        );
        ScriptValues.putFunction(nativeObject, "accent", objects10 -> Theme.getPrimary());
        ScriptValues.putFunction(nativeObject, "background", objects11 -> Theme.getBackground());
        ScriptValues.putFunction(nativeObject, "text", objects12 -> Theme.getTextColor());
        ScriptValues.putFunction(nativeObject, "textSecondary", objects13 -> Theme.getSecondaryText());
        ScriptValues.putFunction(nativeObject, "foreground", objects14 -> Theme.getWhite());
        ScriptValues.putFunction(nativeObject, "red", objects15 -> ColorUtil.red(ScriptValues.argIntOrDefault(objects15, 0, 0)));
        ScriptValues.putFunction(nativeObject, "green", objects16 -> ColorUtil.green(ScriptValues.argIntOrDefault(objects16, 0, 0)));
        ScriptValues.putFunction(nativeObject, "blue", objects17 -> ColorUtil.blue(ScriptValues.argIntOrDefault(objects17, 0, 0)));
        ScriptValues.putFunction(nativeObject, "alphaOf", objects18 -> ColorUtil.alpha(ScriptValues.argIntOrDefault(objects18, 0, 0)));
        scriptableObject.put("Color", scriptableObject, nativeObject);
    }

    private static void installStorage(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "get", objects -> {
            ScriptStorage scriptStorage = script.getStorage();
            String string = ScriptValues.argString(objects, 0);
            return ScriptValues.hasArg(objects, 1) ? scriptStorage.get(string, objects[1]) : scriptStorage.get(string, null);
        });
        ScriptValues.putFunction(nativeObject, "set", objects2 -> {
            ScriptStorage scriptStorage2 = script.getStorage();
            String string2 = ScriptValues.argString(objects2, 0);
            if (ScriptValues.hasArg(objects2, 1)) {
                scriptStorage2.set(string2, objects2[1]);
                return null;
            } else {
                scriptStorage2.set(string2, null);
                return null;
            }
        });
        ScriptValues.putFunction(nativeObject, "remove", objects3 -> {
            script.getStorage().remove(ScriptValues.argString(objects3, 0));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "keys", objects4 -> ScriptValues.toArray(script.getStorage().keys()));
        scriptableObject.put("storage", scriptableObject, nativeObject);
    }

    private static void installAnimation(ScriptableObject scriptableObject) {
        scriptableObject.put("Animation", scriptableObject, ScriptValues.function("Animation", objects -> {
            Animation animation = new Animation();
            if (ScriptValues.hasArg(objects, 0)) {
                animation.forceValue(ScriptValues.argDouble(objects, 0, 0.0));
            }

            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putFunction(nativeObject, "run", objects2 -> {
                Easing easing = EASINGS.getOrDefault(ScriptValues.argString(objects2, 2, "LINEAR").toUpperCase(Locale.ROOT), Easings.LINEAR);
                animation.animate(ScriptValues.argDouble(objects2, 0, 0.0), ScriptValues.argDouble(objects2, 1, 0.0), easing, ScriptValues.argBoolean(objects2, 3, false));
                return null;
            });
            ScriptValues.putFunction(nativeObject, "update", objects3 -> animation.update());
            ScriptValues.putFunction(nativeObject, "get", objects4 -> animation.get());
            ScriptValues.putFunction(nativeObject, "set", objects5 -> {
                animation.forceValue(ScriptValues.argDouble(objects5, 0, 0.0));
                return null;
            });
            ScriptValues.putFunction(nativeObject, "target", objects6 -> animation.getToValue());
            ScriptValues.putFunction(nativeObject, "finished", objects7 -> animation.isFinished());
            return nativeObject;
        }));
    }

    private static void installTimers(ScriptableObject scriptableObject, Script script) {
        scriptableObject.put("Timer", scriptableObject, ScriptValues.function("Timer", objects -> {
            StopWatch stopWatch = new StopWatch();
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putFunction(nativeObject, "passed", objects2 -> stopWatch.isElapsed((long)ScriptValues.argDouble(objects2, 0, 0.0)));
            ScriptValues.putFunction(nativeObject, "reset", objects3 -> {
                stopWatch.reset();
                return null;
            });
            ScriptValues.putFunction(nativeObject, "elapsed", objects4 -> stopWatch.getElapsed());
            return nativeObject;
        }));
        scriptableObject.put("setTimeout", scriptableObject, ScriptValues.function("setTimeout", objects5 -> {
            Function function2 = ScriptValues.argFunction(objects5, 0);
            if (function2 == null) {
                throw Context.reportRuntimeError("setTimeout: function expected");
            } else {
                return script.addTimer((long)ScriptValues.argDouble(objects5, 1, 0.0), false, function2);
            }
        }));
        scriptableObject.put("setInterval", scriptableObject, ScriptValues.function("setInterval", objects6 -> {
            Function function3 = ScriptValues.argFunction(objects6, 0);
            if (function3 == null) {
                throw Context.reportRuntimeError("setInterval: function expected");
            } else {
                return script.addTimer((long)ScriptValues.argDouble(objects6, 1, 50.0), true, function3);
            }
        }));
        scriptableObject.put("clearTimeout", scriptableObject, ScriptValues.function("clearTimeout", objects7 -> script.removeTimer(ScriptValues.argInt(objects7, 0, -1))));
        scriptableObject.put("clearInterval", scriptableObject, ScriptValues.function("clearInterval", objects8 -> script.removeTimer(ScriptValues.argInt(objects8, 0, -1))));
    }
}
