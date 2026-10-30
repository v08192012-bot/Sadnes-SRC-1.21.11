package tech.sadnes.client.script.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tech.sadnes.client.script.ScriptEvents;

public final class ScriptApiDocs {
    private static final Map<String, List<ScriptApiDocs.ApiEntry>> MEMBERS_BY_OWNER = new HashMap<>();
    private static final List<ScriptApiDocs.ApiEntry> GLOBALS = new ArrayList<>();
    private static final Map<String, String> EVENT_FIELDS = new LinkedHashMap<>();
    private static final Map<String, String> SCRIPT_CALLBACKS = new LinkedHashMap<>();
    private static final Set<String> CANCELLABLE_EVENTS = Set.of(
        "move",
        "move_input",
        "player_input",
        "keep_sprint",
        "bounding_box",
        "slime_bounce",
        "look",
        "attack",
        "entity_trace",
        "click_slot",
        "use_item",
        "stop_use",
        "packet",
        "block_collision",
        "screen_close",
        "set_screen",
        "screen_tooltip",
        "scroll",
        "player_tick",
        "ambience_cloud",
        "custom_sky"
    );

    private ScriptApiDocs() {
    }

    private static void addGlobal(String string, String string2, String string3, boolean flag, String string4) {
        GLOBALS.add(new ScriptApiDocs.ApiEntry(string, string2, string3, flag, string4));
    }

    private static void addMember(String string, String string2, String string3, String string4, String string5) {
        MEMBERS_BY_OWNER.computeIfAbsent(string, string6 -> new ArrayList<>()).add(new ScriptApiDocs.ApiEntry(string2, string3, string4, true, string5));
    }

    private static void addSettingMembers(String string) {
        addMember(string, "name", "name", "на языке клиента", "");
        addMember(string, "id", "id", "стабильный ключ, как в конфиге", "");
        addMember(string, "type", "type", "checkbox/slider/mode/...", "");
        addMember(string, "min", "min", "для slider", "");
        addMember(string, "max", "max", "для slider", "");
        addMember(string, "step", "step", "для slider", "");
        addMember(string, "options", "options()", "варианты mode", "");
        addMember(string, "children", "children()", "настройки группы (клиентские)", "");
        addMember(string, "visible", "visible()", "", "");
        addMember(string, "render", "render(x, y, w[, alpha, deferOverlay])", "строка настройки, → высота", "");
        addMember(string, "overlay", "overlay()", "отложенный дропдаун/пикер", "");
        addMember(string, "overlayOpen", "overlayOpen()", "", "");
        addMember(string, "height", "height()", "", "");
        addMember(string, "click", "click(x, y, button)", "", "");
        addMember(string, "release", "release(x, y, button)", "", "");
        addMember(string, "drag", "drag()", "каждый кадр при зажатой кнопке", "");
        addMember(string, "key", "key(key, scanCode, mods)", "", "");
        addMember(string, "char", "char(code, mods)", "", "");
    }

    private static void registerEvents() {
        addEvent("update", "");
        addEvent("pre_update", "");
        addEvent("post_motion", "");
        addEvent("player_tick", "");
        addEvent("game_update", "");
        addEvent("slow", "slowCrawling, slowSneaking, setSlowCrawling(v), setSlowSneaking(v)");
        addEvent("move", "x, y, z, onGround, setX(v), setY(v), setZ(v), setOnGround(v)");
        addEvent(
            "move_input",
            "forward, strafe, jump, sneaking, sneakSlow, moving, setForward(v), setStrafe(v), setJump(v), setSneaking(v), setSneakSlow(v), steer(x, z)"
        );
        addEvent("move_sync", "");
        addEvent("player_input", "jump(bool), sneak(bool), sprint(bool)");
        addEvent("jump", "");
        addEvent("forced_sprint", "stage, forced, setForced(v)");
        addEvent("keep_sprint", "stage");
        addEvent("slime_bounce", "");
        addEvent("firework_boost", "shooter, multiplierXZ, multiplierY, setMultipliers(xz, y)");
        addEvent("bounding_box", "entity, box");
        addEvent("look", "yaw, pitch, setYaw(v), setPitch(v)");
        addEvent("rotation", "yaw, pitch, partial, setYaw(v), setPitch(v)");
        addEvent("attack", "target, post, stage");
        addEvent("entity_trace", "entity");
        addEvent("entity_hitbox", "entity, size, setSize(v)");
        addEvent("death", "");
        addEvent("click_slot", "windowId, slot, button, action");
        addEvent("use_item", "type, hand");
        addEvent("stop_use", "item");
        addEvent("entity_use_item", "entity, using");
        addEvent("item_pickup", "collector, item, count");
        addEvent("packet", "name, send, receive, ghost, data()");
        addEvent("world_change", "");
        addEvent("block_update", "block, type");
        addEvent("block_collision", "block");
        addEvent("entity_remove", "entity, id, reason");
        addEvent("client_options", "language, viewDistance, modelParts, setLanguage(v), setViewDistance(v)");
        addEvent("screen", "width, height, slot");
        addEvent("screen_close", "screen");
        addEvent("set_screen", "screen");
        addEvent("screen_tooltip", "x, y, slot");
        addEvent("key", "key, scanCode, modifiers, press, release, screenOpen");
        addEvent("mouse", "button, modifiers, press, release, x, y, screenOpen");
        addEvent("scroll", "vertical, horizontal, x, y, screenOpen");
        addEvent("render", "stage, partial");
        addEvent("text_factory", "text, setText(v), replace(from, to)");
        addEvent("custom_sky", "color, brightness, rotation, partial");
        addEvent("ambience_cloud", "color, height, speed, setColor(v), setHeight(v), setSpeed(v)");
        addEvent("ambience_fog_color", "color, viewDistance, skyDarkness, setColor(v)");
        addEvent("ambience_fog_distance", "start, end, renderStart, renderEnd, setRange(start, end)");
        addEvent("ambience_sky_color", "color, partial, setColor(v)");
        addEvent("ambience_time", "timeOfDay, setTimeOfDay(v)");
        addEvent("ambience_weather", "type, gradient, setGradient(v)");
        addEvent("chat", "text");
        addEvent("block_interact", "x, y, z, hand, side");
        addEvent("health_change", "from, to");
        addEvent("clickgui_bg", "x, y, w, h, radius, sidebar, alpha");
        addEvent("clickgui_screen_bg", "w, h, alpha");
        addEvent("editor_bg", "x, y, w, h, radius, alpha");
        addEvent("hud_header_bg", "x, y, w, h, alpha, corner");
        addEvent("hud_cell_bg", "x, y, w, h, alpha, corner");
        SCRIPT_CALLBACKS.put("tick", "no argument; every game tick");
        SCRIPT_CALLBACKS.put("render2d", "argument: partial (number) — draw with render.*");
        SCRIPT_CALLBACKS.put("render3d", "argument: partial (number) — draw with render3d.*");
        SCRIPT_CALLBACKS.put("tab_update", "argument: array of {name, ping}");
        SCRIPT_CALLBACKS.put("enable", "no argument; the script was turned on");
        SCRIPT_CALLBACKS.put("disable", "no argument; the script was turned off");
    }

    private static void addEvent(String string, String string2) {
        boolean flag = CANCELLABLE_EVENTS.contains(string);
        String string3 = string2;
        if (flag) {
            string3 = string2.isEmpty() ? "cancel(), cancelled()" : "cancel(), cancelled(), " + string2;
        }

        EVENT_FIELDS.put(string, string3);
        if (!string3.isEmpty()) {
            for (String string4 : string3.split(",")) {
                String string5 = string4.trim();
                int i = string5.indexOf(40);
                addMember("event:" + string, i < 0 ? string5 : string5.substring(0, i), string5, "", "");
            }
        }
    }

    public static String buildEventsDoc() {
        StringBuilder builder = new StringBuilder(4096);

        for (Entry entry : SCRIPT_CALLBACKS.entrySet()) {
            builder.append("- `").append((String)entry.getKey()).append("` — ").append((String)entry.getValue()).append('\n');
        }

        for (Entry entry2 : EVENT_FIELDS.entrySet()) {
            builder.append("- `").append((String)entry2.getKey()).append('`');
            if (CANCELLABLE_EVENTS.contains(entry2.getKey())) {
                builder.append(" *(cancellable)*");
            }

            builder.append(" — ").append(((String)entry2.getValue()).isEmpty() ? "no fields" : (String)entry2.getValue()).append('\n');
        }

        return builder.toString();
    }

    private static String inferEventParamType(String string, String string2) {
        String string3 = "\\(\\s*" + Pattern.quote(string) + "\\b";
        String string4 = null;

        for (Pattern pattern : new Pattern[]{
            Pattern.compile("events\\.(\\w+)\\(\\s*function\\s*" + string3), Pattern.compile("events\\.(?:on|once)\\(\\s*\"(\\w+)\"\\s*,\\s*function\\s*" + string3)
        }) {
            Matcher matcher2 = pattern.matcher(string2);

            while (matcher2.find()) {
                String string5 = matcher2.group(1);
                if (!string5.equals("on") && !string5.equals("once")) {
                    if (string4 != null && !string4.equals(string5)) {
                        return "";
                    }

                    string4 = string5;
                }
            }
        }

        return string4 != null && EVENT_FIELDS.containsKey(string4) ? "event:" + string4 : "";
    }

    public static String getEventOwner(String string) {
        return EVENT_FIELDS.containsKey(string) ? "event:" + string : "";
    }

    private static void addSettingFactories(String string) {
        addMember(string, "checkbox", "checkbox(name, default)", "тогл", "setting");
        addMember(string, "slider", "slider(name, def, min, max, step[, unit])", "ползунок; unit — \"ms\", \"%\", \"s\"…", "setting");
        addMember(string, "mode", "mode(name, [опции], def)", "выбор одного", "setting");
        addMember(string, "multi", "multi(name, [опции][, [включённые]])", "выбор нескольких", "setting");
        addMember(string, "color", "color(name, 0xFFFFFFFF)", "цвет", "setting");
        addMember(string, "text", "text(name, \"\")", "строка", "setting");
        addMember(string, "button", "button(name, fn)", "кнопка", "setting");
        addMember(string, "bind", "bind(name, keyCode)", "клавиша", "setting");
        if (!string.equals("group")) {
            addMember(string, "group", "group(name, default)", "группа настроек", "group");
        }
    }

    public static boolean isGlobal(String string) {
        for (ScriptApiDocs.ApiEntry apiEntry : GLOBALS) {
            if (apiEntry.name().equals(string)) {
                return true;
            }
        }

        return false;
    }

    public static String getGlobalType(String string) {
        for (ScriptApiDocs.ApiEntry apiEntry : GLOBALS) {
            if (apiEntry.name().equals(string)) {
                if (apiEntry.function()) {
                    return "";
                }

                return apiEntry.returns();
            }
        }

        return "";
    }

    public static boolean hasOwner(String string) {
        return MEMBERS_BY_OWNER.containsKey(string);
    }

    public static boolean hasMember(String string, String string2) {
        for (ScriptApiDocs.ApiEntry apiEntry : MEMBERS_BY_OWNER.getOrDefault(string, List.of())) {
            if (apiEntry.name().equals(string2)) {
                return true;
            }
        }

        return false;
    }

    public static List<ScriptApiDocs.ApiEntry> completeGlobals(String string) {
        return filterByPrefix(GLOBALS, string);
    }

    public static String buildApiDoc() {
        StringBuilder builder = new StringBuilder(8192);
        builder.append("### Globals\n");

        for (ScriptApiDocs.ApiEntry apiEntry : GLOBALS) {
            if (!apiEntry.detail().isEmpty() || !apiEntry.signature().equals(apiEntry.name())) {
                appendEntry(builder, apiEntry);
            }
        }

        for (Entry entry : new TreeMap<>(MEMBERS_BY_OWNER).entrySet()) {
            if (!((String)entry.getKey()).startsWith("event:")) {
                builder.append("\n### ").append((String)entry.getKey()).append(".*\n");

                for (ScriptApiDocs.ApiEntry apiEntry2 : (List<ScriptApiDocs.ApiEntry>)entry.getValue()) {
                    appendEntry(builder, apiEntry2);
                }
            }
        }

        return builder.toString();
    }

    private static void appendEntry(StringBuilder builder, ScriptApiDocs.ApiEntry apiEntry) {
        builder.append("- `").append(apiEntry.signature()).append('`');
        if (!apiEntry.detail().isEmpty()) {
            builder.append(" — ").append(apiEntry.detail());
        }

        if (!apiEntry.returns().isEmpty()) {
            builder.append(" → ").append(apiEntry.returns());
        }

        builder.append('\n');
    }

    public static List<ScriptApiDocs.ApiEntry> completeMembers(String string, String string2) {
        List list = MEMBERS_BY_OWNER.get(string);
        return list == null ? List.of() : filterByPrefix(list, string2);
    }

    private static List<ScriptApiDocs.ApiEntry> filterByPrefix(List<ScriptApiDocs.ApiEntry> apiEntries, String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();

        for (ScriptApiDocs.ApiEntry apiEntry : apiEntries) {
            String string3 = apiEntry.name().toLowerCase(Locale.ROOT);
            if (string3.startsWith(string2)) {
                arrayList.add(apiEntry);
            } else if (!string2.isEmpty() && string3.contains(string2)) {
                arrayList2.add(apiEntry);
            }
        }

        arrayList.addAll(arrayList2);
        return arrayList;
    }

    public static String inferType(String string, String string2) {
        return inferTypeRecursive(string, string2, 0);
    }

    private static String inferTypeRecursive(String string, String string2, int i) {
        if (i > 4) {
            return "";
        }

        for (ScriptApiDocs.ApiEntry apiEntry : GLOBALS) {
            if (apiEntry.name().equals(string)) {
                if (apiEntry.returns().isEmpty()) {
                    return string;
                }

                return apiEntry.returns();
            }
        }

        Matcher matcher2 = Pattern.compile("(?<![\\w$.])" + Pattern.quote(string) + "\\s*=(?!=)").matcher(string2);
        if (!matcher2.find()) {
            return inferEventParamType(string, string2);
        }

        int i2 = matcher2.start();
        int i3 = string2.indexOf(10, i2);
        String string3 = string2.substring(i2, i3 < 0 ? string2.length() : i3);
        int i4 = string3.indexOf(61);
        if (i4 < 0) {
            return "";
        }

        String string4 = string3.substring(i4 + 1).trim();
        if (string4.startsWith("Module(") || string4.startsWith("new Module(")) {
            return "module";
        }

        if (string4.startsWith("Timer(")) {
            return "timer";
        }

        if (string4.startsWith("Animation(") || string4.startsWith("new Animation(")) {
            return "animation";
        }

        if (string4.startsWith("Screen(") || string4.startsWith("new Screen(")) {
            return "screen";
        }

        if (string4.startsWith("category(")) {
            return "category";
        }

        int i5 = string4.indexOf(46);
        int i6 = string4.indexOf(40);
        if (i5 > 0 && i6 > i5) {
            String string5 = inferTypeRecursive(string4.substring(0, i5).trim(), string2, i + 1);
            String string6 = string4.substring(i5 + 1, i6).trim();

            for (ScriptApiDocs.ApiEntry apiEntry2 : MEMBERS_BY_OWNER.getOrDefault(string5, List.of())) {
                if (apiEntry2.name().equals(string6)) {
                    return apiEntry2.returns();
                }
            }
        }

        return "";
    }

    static {
        addGlobal("Module", "Module(name[, enabledByDefault])", "модуль скрипта; второй аргумент — включён по умолчанию", true, "module");
        addGlobal("events", "events", "подписка на события", false, "events");
        addGlobal("settings", "settings", "настройки скрипта", false, "settings");
        addGlobal("client", "client", "чат, тосты, модули клиента", false, "client");
        addGlobal("player", "player", "свой игрок", false, "player");
        addGlobal("world", "world", "мир и сущности", false, "world");
        addGlobal("render", "render", "2D-рендер", false, "render");
        addGlobal("render3d", "render3d", "рендер в мире", false, "render3d");
        addGlobal("assets", "assets", "файлы скрипта: json, png, glsl", false, "assets");
        addGlobal("storage", "storage", "долгая память скрипта", false, "storage");
        addGlobal("Color", "Color", "цвета", false, "color");
        addGlobal("mc", "mc", "состояние игры", false, "mc");
        addGlobal("script", "script", "инфо о скрипте", false, "script");
        addGlobal("aura", "aura", "свои режимы боевой ауры", false, "aura");
        addGlobal("watermark", "watermark", "свои чипы ватермарки", false, "watermark");
        addGlobal("esp", "esp", "свои ESP-элементы и нейм-теги", false, "esp");
        addGlobal("targethud", "targethud", "свой стиль TargetHud", false, "targethud");
        addGlobal("move", "move", "управление движением", false, "move");
        addGlobal("rotation", "rotation", "серверная ротация: стройка, наводка на блок", false, "rotation");
        addGlobal("input", "input", "нажатия игровых клавиш за игрока", false, "input");
        addGlobal("container", "container", "открытый контейнер: слоты, заголовок", false, "container");
        addGlobal("Timer", "Timer()", "секундомер", true, "timer");
        addGlobal("Animation", "Animation([start])", "плавная анимация значения", true, "animation");
        addGlobal("Screen", "Screen({render, click, ...})", "свой экран", true, "screen");
        addGlobal("category", "category(name)", "своя категория ClickGui", true, "category");
        addGlobal("chat", "chat(text)", "сообщение в чат", true, "");
        addGlobal("print", "print(...)", "в консоль скриптов и лог игры", true, "");
        addGlobal("console", "console", "консоль скриптов: log/warn/error", false, "console");
        addGlobal("include", "include(\"utils.js\")", "подключить файл из src/", true, "");
        addGlobal("setTimeout", "setTimeout(fn, ms)", "разовый таймер", true, "");
        addGlobal("setInterval", "setInterval(fn, ms)", "повторяющийся таймер", true, "");
        addGlobal("clearTimeout", "clearTimeout(id)", "снять таймер", true, "");
        addGlobal("clearInterval", "clearInterval(id)", "снять таймер", true, "");
        addGlobal("function", "function", "", false, "");
        addGlobal("return", "return", "", false, "");

        for (String string : new String[]{
            "true",
            "false",
            "null",
            "undefined",
            "var",
            "let",
            "const",
            "if",
            "else",
            "for",
            "while",
            "do",
            "new",
            "typeof",
            "instanceof",
            "break",
            "continue",
            "switch",
            "case",
            "default",
            "try",
            "catch",
            "finally",
            "throw",
            "this",
            "in",
            "of",
            "delete"
        }) {
            addGlobal(string, string, "", false, "");
        }

        for (String string2 : ScriptEvents.getEventClasses().keySet()) {
            addMember("events", string2, string2 + "(fn[, priority])", "событие клиента; priority — high/low… относительно модулей", "");
        }

        for (String string3 : ScriptEvents.CUSTOM_EVENTS) {
            addMember("events", string3, string3 + "(fn)", "событие", "");
        }

        addMember("events", "names", "names()", "все имена событий", "");
        addMember("events", "on", "on(name, fn[, priority])", "подписка по имени; priority: highest/high/normal/low/lowest", "");
        addMember("events", "once", "once(name, fn[, priority])", "разовая подписка", "");
        addMember("events", "off", "off(id)", "отписка", "");
        addMember("module", "enabled", "enabled()", "включён ли модуль", "");
        addMember("module", "isEnabled", "isEnabled()", "включён ли модуль", "");
        addMember("module", "setEnabled", "setEnabled(bool)", "", "");
        addMember("module", "toggle", "toggle()", "", "");
        addMember("module", "description", "description(text)", "описание модуля", "module");
        addMember("module", "onEnabled", "onEnabled(fn)", "колбэк включения", "module");
        addMember("module", "onDisabled", "onDisabled(fn)", "колбэк выключения", "module");
        addSettingFactories("module");
        addSettingFactories("settings");
        addSettingFactories("group");
        addMember("module", "name", "name", "имя модуля", "");
        addMember(
            "module",
            "feature",
            "feature(name[, category, subcategory])",
            "модуль в категории ClickGui; subcategory — встроенная группа по имени (Attack, Supplies, Tweaks… — как module.subcategory у модулей клиента) или своя, без неё группа зовётся как скрипт",
            "feature"
        );
        addMember("feature", "name", "name", "", "");
        addMember("feature", "category", "category", "", "");
        addMember("feature", "subcategory", "subcategory", "", "");
        addMember("feature", "enabled", "enabled()", "включён ли модуль", "");
        addMember("feature", "isEnabled", "isEnabled()", "включён ли модуль", "");
        addMember("feature", "setEnabled", "setEnabled(bool)", "", "");
        addMember("feature", "toggle", "toggle()", "", "");
        addMember("feature", "description", "description(text)", "описание модуля", "feature");
        addMember("feature", "onEnabled", "onEnabled(fn)", "колбэк включения", "feature");
        addMember("feature", "onDisabled", "onDisabled(fn)", "колбэк выключения", "feature");
        addMember("feature", "panel", "panel({render, click, ...})", "своя панель вместо настроек", "panel");
        addSettingFactories("feature");
        addMember("panel", "open", "open()", "открыть панель (и ClickGui)", "");
        addMember("panel", "close", "close()", "", "");
        addMember("panel", "isOpen", "isOpen()", "", "");
        addMember("screen", "open", "open()", "показать экран", "");
        addMember("screen", "close", "close()", "", "");
        addMember("screen", "toggle", "toggle()", "", "");
        addMember("screen", "isOpen", "isOpen()", "", "");
        addMember("category", "name", "name", "", "");
        addMember("category", "custom", "custom", "своя ли категория", "");
        addMember("category", "icon", "icon(name)", "иконка своей категории", "category");
        addMember("category", "description", "description(text)", "", "category");
        addMember("setting", "get", "get()", "значение", "");
        addMember("setting", "set", "set(value)", "задать значение; у multi — массив включённых вариантов", "");
        addMember("setting", "toggle", "toggle()", "", "");
        addMember("setting", "is", "is(value)", "для mode", "");
        addMember("setting", "enabled", "enabled(name)", "для multi", "");
        addMember("setting", "isKey", "isKey(code)", "для bind", "");
        addMember("setting", "visibleIf", "visibleIf(fn)", "условная видимость", "setting");
        addSettingMembers("setting");
        addMember("group", "get", "get()", "", "");
        addMember("group", "set", "set(bool)", "", "");
        addMember("group", "visibleIf", "visibleIf(fn)", "", "group");
        addMember("render", "rect", "rect(x, y, w, h, color[, radius, smooth])", "", "");
        addMember("render", "outline", "outline(x, y, w, h, thickness, color[, radius, smooth])", "", "");
        addMember("render", "gradientV", "gradientV(x, y, w, h, top, bottom)", "", "");
        addMember("render", "gradientH", "gradientH(x, y, w, h, left, right)", "", "");
        addMember("render", "gradient4", "gradient4(x, y, w, h, lt, rt, rb, lb[, radius])", "", "");
        addMember("render", "shadow", "shadow(x, y, w, h, size, color[, radius])", "", "");
        addMember("render", "shadowHollow", "shadowHollow(x, y, w, h, size, color[, radius])", "свечение только снаружи формы", "");
        addMember("render", "blur", "blur(x, y, w, h[, radius, alpha])", "", "");
        addMember("render", "header", "header(x, y, w, h[, alpha, corner])", "плашка HUD клиента", "");
        addMember("render", "panel", "panel(x, y, w, h[, alpha, corner])", "", "");
        addMember("render", "glass", "glass(x, y, w, h[, alpha, corner])", "", "");
        addMember("render", "hover", "hover(x, y, w, h[, alpha, corner])", "", "");
        addMember("render", "toggle", "toggle(x, y, w, h, t[, alpha])", "тогл клиента, t 0..1", "");
        addMember("render", "container", "container(x, y, w, h[, alpha, radius])", "блок контента", "");
        addMember("render", "text", "text(text, x, y, color[, size])", "", "");
        addMember("render", "textCenter", "textCenter(text, x, y, color[, size])", "", "");
        addMember("render", "textRight", "textRight(text, x, y, color[, size])", "", "");
        addMember("render", "textWrap", "textWrap(text, x, y, width, color[, size])", "", "");
        addMember("render", "textWidth", "textWidth(text[, size])", "", "");
        addMember("render", "textHeight", "textHeight([size])", "", "");
        addMember("render", "line", "line(x1, y1, x2, y2, thickness, color)", "", "");
        addMember("render", "circle", "circle(cx, cy, radius, color[, thickness])", "", "");
        addMember("render", "ring", "ring(cx, cy, radius, thickness, color)", "кольцо", "");
        addMember("render", "arc", "arc(cx, cy, radius, from, to, thickness, color)", "", "");
        addMember("render", "icon", "icon(name, x, y, size, color)", "", "");
        addMember(
            "render",
            "item",
            "item(item, x, y[, size, alpha])",
            "id-строка или хэндл предмета (mainHand(), equipment().armor[i]...) — настоящий стак с зачарованиями и прочностью",
            ""
        );
        addMember("render", "face", "face(entity, x, y, size[, radius, smooth, alpha])", "лицо скина сущности; false — скина нет (не игрок)", "");
        addMember("render", "image", "image(path, x, y, w, h[, radius, alpha])", "из assets", "");
        addMember("render", "imageSize", "imageSize(path)", "{width, height}", "");
        addMember("render", "animatedImage", "animatedImage(path, x, y, w, h[, radius])", "спрайтшит", "");
        addMember("render", "clip", "clip(x, y, w, h, fn)", "обрезка", "");
        addMember("render", "svg", "svg(path, x, y, size, color)", "своя SVG-иконка", "");
        addMember("render", "font", "font(name)", "свой msdf-шрифт из assets или встроенный клиента: bold/semibold/medium/regular", "font");
        addMember("font", "text", "text(text, x, y, color[, size])", "", "");
        addMember("font", "textCenter", "textCenter(text, x, y, color[, size])", "", "");
        addMember("font", "textRight", "textRight(text, x, y, color[, size])", "", "");
        addMember("font", "textWrap", "textWrap(text, x, y, width, color[, size])", "перенос по ширине", "");
        addMember("font", "width", "width(text[, size])", "", "");
        addMember("font", "height", "height([size])", "", "");
        addMember("render", "counter", "counter([font])", "счётчик-одометр: цифры докручиваются барабанами", "counter");
        addMember("counter", "reset", "reset()", "следующий update() без прокрутки", "");
        addMember("counter", "update", "update(value)", "звать раз в кадр, до width()/draw()", "");
        addMember("counter", "width", "width([size])", "", "");
        addMember("counter", "draw", "draw(x, y[, size, color])", "", "");
        addMember("render", "particles", "particles()", "свой рой 2D-частиц", "particles");
        addMember(
            "particles",
            "spawn",
            "spawn({x, y, offsetX, offsetY, motionX, motionY, color, size, life, fadeIn, fadeOut, gravity, friction, spin, texture})",
            "texture — имя файла из particle/: bloom, star, heart, snow, dollar, pumpkin, sadnes, star_five...",
            ""
        );
        addMember("particles", "rebase", "rebase(x, y)", "сдвинуть якорь всех частиц", "");
        addMember("particles", "update", "update()", "звать раз в кадр, до render()", "");
        addMember("particles", "render", "render([alpha])", "", "");
        addMember("particles", "clear", "clear()", "убрать все частицы", "");
        addMember("particles", "empty", "empty()", "", "");
        addMember("render", "width", "width()", "ширина экрана", "");
        addMember("render", "height", "height()", "высота экрана", "");
        addMember("render", "toScreen", "toScreen(x, y, z)", "мир → экран", "");
        addMember("render3d", "box", "box(x1, y1, z1, x2, y2, z2, fill[, outline])", "", "");
        addMember("render3d", "blockBox", "blockBox(x, y, z, fill[, outline])", "", "");
        addMember("render3d", "entityBox", "entityBox(entity, fill[, outline])", "", "");
        addMember("render3d", "line", "line(x1, y1, z1, x2, y2, z2, color[, width])", "", "");
        addMember("render3d", "sphere", "sphere(x, y, z, radius, color[, rings, glow, width])", "светящаяся сфера", "");
        addMember("render3d", "shader", "shader(name)", "свой шейдер из assets", "shader");
        addMember("shader", "draw", "draw(x, y, z, size[, color, p0, p1, p2, p3])", "квад к камере", "");

        for (String[] strings : new String[][]{
            {"exists", ""},
            {"x", ""},
            {"y", ""},
            {"z", ""},
            {"yaw", ""},
            {"pitch", ""},
            {"health", ""},
            {"maxHealth", ""},
            {"hunger", ""},
            {"onGround", ""},
            {"sprinting", ""},
            {"sneaking", ""},
            {"speed", "блок/тик"},
            {"velocity", "{x,y,z}"},
            {"heldItem", "id предмета"},
            {"offhandItem", ""}
        }) {
            addMember("player", strings[0], strings[0] + "()", strings[1], "");
        }

        addMember("player", "wrap", "wrap()", "обёртка-сущность", "entity");
        addMember("player", "renderPos", "renderPos()", "позиция кадра (интерполяция)", "");
        addMember("player", "pos", "pos()", "{x, y, z}", "vec");
        addMember("player", "eyeY", "eyeY()", "", "");
        addMember("player", "box", "box()", "хитбокс", "box");
        addMember("player", "fallDistance", "fallDistance()", "", "");
        addMember("player", "hurtTime", "hurtTime()", "", "");
        addMember("player", "age", "age()", "тиков с появления в мире", "");
        addMember("player", "setOnGround", "setOnGround(bool)", "флаг «на земле» на клиенте", "");
        addMember("player", "startGliding", "startGliding()", "раскрыть элитры в воздухе", "");
        addMember("player", "movementInput", "movementInput()", "{forward, strafe, jump, sneak, sprint} после всех модулей", "");
        addMember("player", "air", "air()", "воздух", "");
        addMember("player", "maxAir", "maxAir()", "", "");
        addMember("player", "armor", "armor()", "очки брони", "");
        addMember("player", "absorption", "absorption()", "", "");
        addMember("player", "saturation", "saturation()", "", "");
        addMember("player", "experience", "experience()", "уровни", "");
        addMember("player", "experienceProgress", "experienceProgress()", "0..1", "");
        addMember("player", "horizontalCollision", "horizontalCollision()", "", "");
        addMember("player", "verticalCollision", "verticalCollision()", "", "");
        addMember("player", "gliding", "gliding()", "элитра", "");
        addMember("player", "swimming", "swimming()", "", "");
        addMember("player", "inWater", "inWater()", "", "");
        addMember("player", "inLava", "inLava()", "", "");
        addMember("player", "onFire", "onFire()", "", "");
        addMember("player", "invisible", "invisible()", "", "");
        addMember("player", "usingItem", "usingItem()", "", "");
        addMember("player", "activeHand", "activeHand()", "main/off", "");
        addMember("player", "itemUseTime", "itemUseTime()", "тиков осталось", "");
        addMember("player", "handSwing", "handSwing()", "", "");
        addMember("player", "attackCooldown", "attackCooldown()", "0..1", "");
        addMember("player", "riding", "riding()", "", "");
        addMember("player", "vehicle", "vehicle()", "сущность", "entity");
        addMember("player", "creative", "creative()", "", "");
        addMember("player", "flying", "flying()", "", "");
        addMember("player", "setFlying", "setFlying(bool)", "", "");
        addMember("player", "mainHand", "mainHand()", "предмет", "item");
        addMember("player", "offHand", "offHand()", "предмет", "item");
        addMember("player", "equipment", "equipment()", "рука + броня", "equipment");
        addMember("player", "effects", "effects()", "массив эффектов", "");
        addMember("player", "effect", "effect(id)", "эффект или null", "effect");
        addMember("player", "lookingAt", "lookingAt()", "на что смотрим", "hit");
        addMember("player", "inventory", "inventory", "инвентарь", "inventory");
        addMember("player", "place", "place(hit[, hand])", "клик по блоку: hit из world.raycast / world.placeTarget / lookingAt; false — далеко", "");
        addMember("player", "breakBlock", "breakBlock(x, y, z[, side])", "ломать блок — звать каждый тик, как зажатую ЛКМ", "");
        addMember("player", "stopBreaking", "stopBreaking()", "бросить ломание", "");
        addMember("player", "breaking", "breaking()", "ломаем ли блок", "");
        addMember("player", "breakProgress", "breakProgress()", "0..1", "");
        addMember("player", "attack", "attack(entity)", "удар по сущности; false — далеко", "");
        addMember("player", "interact", "interact(entity[, hand])", "ПКМ по сущности", "");
        addMember("player", "predict", "predict(ticks)", "{x, y, z, onGround, box} через n тиков при текущем вводе", "");
        addMember("player", "interactItem", "interactItem([hand])", "использовать предмет из руки как ПКМ в воздух: клиент сразу ест/пьёт/кидает", "");
        addMember("player", "rightClick", "rightClick()", "ванильный ПКМ: сущность/блок под прицелом, иначе предмет; работает и с открытым экраном", "");
        addMember("player", "dropHeld", "dropHeld([whole])", "выбросить предмет из руки, как Q (whole — весь стак)", "");
        addMember("console", "log", "log(...)", "вывод; объекты — JSON", "");
        addMember("console", "info", "info(...)", "как log", "");
        addMember("console", "warn", "warn(...)", "предупреждение (жёлтым)", "");
        addMember("console", "error", "error(...)", "ошибка (красным), скрипт не падает", "");
        addMember("console", "clear", "clear()", "очистить консоль", "");
        addMember(
            "rotation",
            "set",
            "set(yaw, pitch[, {speed, pitchSpeed, returnSpeed, pitchReturnSpeed, timeout, priority}])",
            "повернуть голову для сервера; звать в events.pre_update каждый тик, пока нужно",
            ""
        );
        addMember("rotation", "lookAt", "lookAt(x, y, z[, opts])", "повернуть на точку, → {yaw, pitch}", "");
        addMember("rotation", "angleTo", "angleTo(x, y, z)", "{yaw, pitch} на точку", "");
        addMember("rotation", "server", "server()", "{yaw, pitch}, которые видит сервер", "");
        addMember("rotation", "camera", "camera()", "{yaw, pitch} камеры — куда смотрит игрок", "");
        addMember("rotation", "delta", "delta(yaw, pitch)", "градусов от серверной ротации до угла", "");
        addMember("rotation", "active", "active()", "крутит ли сейчас клиент", "");
        addMember("rotation", "stop", "stop()", "плавно вернуть к камере (только свою ротацию)", "");
        addMember(
            "input",
            "press",
            "press(name[, ticks])",
            "клик + удержание: forward, back, left, right, jump, sneak, sprint, attack, use, drop, swap, inventory, pick",
            ""
        );
        addMember("input", "hold", "hold(name[, ticks])", "держать без клика; продлевать каждый тик", "");
        addMember("input", "physical", "physical(name)", "физически ли зажата клавиша игрового действия", "");
        addMember("input", "release", "release(name)", "отпустить раньше срока", "");
        addMember("input", "isDown", "isDown(name)", "зажата ли игровая клавиша", "");
        addMember("input", "keyDown", "keyDown(code)", "физическая клавиша по коду GLFW", "");
        addMember("input", "mouseDown", "mouseDown(button)", "физическая кнопка мыши", "");
        addMember("container", "open", "open()", "открыт ли контейнер (не свой инвентарь)", "");
        addMember("container", "title", "title()", "заголовок экрана", "");
        addMember("container", "syncId", "syncId()", "", "");
        addMember("container", "size", "size()", "слотов у контейнера без инвентаря игрока", "");
        addMember("container", "slots", "slots()", "все слоты экрана: предмет + slot (для inventory.click) + playerInventory", "");
        addMember("container", "slot", "slot(index)", "предмет в слоте экрана", "item");
        addMember("inventory", "size", "size()", "", "");
        addMember("inventory", "selectedSlot", "selectedSlot()", "", "");
        addMember("inventory", "slot", "slot(index)", "предмет в слоте", "item");
        addMember("inventory", "all", "all()", "все слоты", "");
        addMember("inventory", "hotbar", "hotbar()", "9 слотов", "");
        addMember("inventory", "armor", "armor()", "4 слота", "");
        addMember("inventory", "find", "find(id)", "индекс или -1", "");
        addMember("inventory", "count", "count(id)", "сколько всего", "");
        addMember("inventory", "click", "click(slot, button, action)", "PICKUP/QUICK_MOVE/SWAP", "");
        addMember("inventory", "swap", "swap(slot, hotbarIndex)", "", "");
        addMember("inventory", "drop", "drop(slot[, whole])", "", "");
        addMember("inventory", "close", "close()", "", "");
        addMember("inventory", "swapHand", "swapHand(slot[, hand])", "слот экрана ↔ рука, как у модулей клиента (на ходу ждёт остановки)", "");
        addMember("inventory", "busy", "busy()", "клиент ещё перекладывает предметы — новые клики отложить", "");
        addMember("inventory", "findServerItem", "findServerItem(type)", "предмет сервера (plast, trap…): слот экрана или -1", "");
        addMember("inventory", "throwItem", "throwItem(slot[, yaw, pitch])", "бросить/использовать предмет из слота, как модули клиента", "");
        addMember("inventory", "throwing", "throwing()", "клиент ещё бросает предмет", "");
        addMember("item", "id", "id", "minecraft:diamond", "");
        addMember("item", "name", "name", "отображаемое имя", "");
        addMember("item", "count", "count", "", "");
        addMember("item", "maxCount", "maxCount", "", "");
        addMember("item", "empty", "empty", "", "");
        addMember("item", "damage", "damage", "", "");
        addMember("item", "maxDamage", "maxDamage", "", "");
        addMember("item", "durability", "durability", "0..1", "");
        addMember("item", "damageable", "damageable", "", "");
        addMember("item", "stackable", "stackable", "", "");
        addMember("item", "food", "food", "", "");
        addMember("item", "nutrition", "nutrition", "сытость (у еды)", "");
        addMember("item", "saturation", "saturation", "насыщение (у еды)", "");
        addMember("item", "effects", "effects", "эффекты зелья (у зелий и стрел)", "");
        addMember("item", "lore", "lore", "строки", "");
        addMember("item", "enchants", "enchants", "[{id, level}]", "");
        addMember("item", "enchant", "enchant(id)", "уровень или 0", "");
        addMember("item", "hasEnchant", "hasEnchant(id)", "", "");
        addMember("item", "is", "is(id)", "", "");
        addMember("item", "block", "block", "предмет — блок, его можно поставить", "");
        addMember("item", "slot", "slot", "индекс слота (в container.slots)", "");
        addMember("item", "playerInventory", "playerInventory", "слот инвентаря игрока (в container.slots)", "");
        addMember("block", "id", "id", "", "");
        addMember("block", "x", "x", "", "");
        addMember("block", "y", "y", "", "");
        addMember("block", "z", "z", "", "");
        addMember("block", "air", "air", "", "");
        addMember("block", "liquid", "liquid", "", "");
        addMember("block", "solid", "solid", "", "");
        addMember("block", "replaceable", "replaceable", "", "");
        addMember("block", "luminance", "luminance", "", "");
        addMember("block", "properties", "properties", "состояние блока", "");
        addMember("block", "is", "is(id)", "", "");
        addMember("block", "box", "box", "рамка блока в мире (плита, ступенька)", "box");
        addMember("block", "full", "full", "полный куб", "");
        addMember("effect", "id", "id", "", "");
        addMember("effect", "level", "level", "1 = I", "");
        addMember("effect", "duration", "duration", "тики", "");
        addMember("equipment", "mainHand", "mainHand", "", "item");
        addMember("equipment", "offHand", "offHand", "", "item");
        addMember("equipment", "armor", "armor", "4 предмета", "");
        addMember("hit", "type", "type", "block/entity/miss", "");
        addMember("hit", "pos", "pos", "", "vec");
        addMember("hit", "side", "side", "сторона блока", "");
        addMember("hit", "block", "block", "", "block");
        addMember("hit", "entity", "entity", "", "entity");
        addMember("hit", "target", "target", "куда встанет блок (у world.placeTarget)", "vec");
        addMember("vec", "x", "x", "", "");
        addMember("vec", "y", "y", "", "");
        addMember("vec", "z", "z", "", "");
        addMember("box", "x1", "x1", "", "");
        addMember("box", "y1", "y1", "", "");
        addMember("box", "z1", "z1", "", "");
        addMember("box", "x2", "x2", "", "");
        addMember("box", "y2", "y2", "", "");
        addMember("box", "z2", "z2", "", "");
        addMember("box", "width", "width", "", "");
        addMember("box", "height", "height", "", "");
        addMember("box", "depth", "depth", "", "");
        addMember("world", "exists", "exists()", "", "");
        addMember("world", "time", "time()", "время суток", "");
        addMember("world", "dimension", "dimension()", "", "");
        addMember("world", "players", "players()", "массив сущностей", "");
        addMember("world", "entities", "entities()", "массив сущностей", "");
        addMember("world", "blockAt", "blockAt(x, y, z)", "id блока", "");
        addMember("world", "findBlocks", "findBlocks(id, radius)", "массив {x,y,z}", "");
        addMember("world", "holes", "holes([radius])", "холки рядом: клетки пола ям ниже земли вокруг; массив {x,y,z}", "");
        addMember("world", "block", "block(x, y, z)", "обёртка блока", "block");
        addMember("world", "raining", "raining()", "", "");
        addMember("world", "thundering", "thundering()", "", "");
        addMember("world", "rainGradient", "rainGradient()", "0..1", "");
        addMember("world", "day", "day()", "номер дня", "");
        addMember("world", "difficulty", "difficulty()", "", "");
        addMember("world", "entityById", "entityById(id)", "", "entity");
        addMember("world", "raycast", "raycast([yaw, pitch, range, fluids])", "луч по ротации; без углов — по серверной; fluids: true / \"source\"", "hit");
        addMember("world", "bottomY", "bottomY()", "нижняя граница мира", "");
        addMember(
            "world", "placeTarget", "placeTarget(x, y, z)", "опора под блок в точке: грань и точка клика для player.place; null — не к чему приставить", "hit"
        );

        for (String[] strings2 : new String[][]{
            {"name", ""},
            {"type", ""},
            {"id", ""},
            {"x", ""},
            {"y", ""},
            {"z", ""},
            {"yaw", ""},
            {"pitch", ""},
            {"health", ""},
            {"maxHealth", ""},
            {"hurtTime", ""},
            {"maxHurtTime", ""},
            {"alive", ""},
            {"isPlayer", ""},
            {"isLiving", "живая сущность (игрок, моб, стойка)"},
            {"distance", "до себя"},
            {"box", "{x1..z2}"},
            {"renderPos", "позиция кадра"},
            {"prevPos", "позиция прошлого тика"}
        }) {
            addMember("entity", strings2[0], strings2[0] + "()", strings2[1], "");
        }

        addMember("entity", "pos", "pos()", "", "vec");
        addMember("entity", "velocity", "velocity()", "", "vec");
        addMember("entity", "eyeY", "eyeY()", "", "");
        addMember("entity", "width", "width()", "", "");
        addMember("entity", "height", "height()", "", "");
        addMember("entity", "fallDistance", "fallDistance()", "", "");
        addMember("entity", "onGround", "onGround()", "", "");
        addMember("entity", "sprinting", "sprinting()", "", "");
        addMember("entity", "sneaking", "sneaking()", "", "");
        addMember("entity", "invisible", "invisible()", "", "");
        addMember("entity", "onFire", "onFire()", "", "");
        addMember("entity", "inWater", "inWater()", "", "");
        addMember("entity", "gliding", "gliding()", "", "");
        addMember("entity", "usingItem", "usingItem()", "", "");
        addMember("entity", "armor", "armor()", "очки брони", "");
        addMember("entity", "absorption", "absorption()", "", "");
        addMember("entity", "mainHand", "mainHand()", "", "item");
        addMember("entity", "offHand", "offHand()", "", "item");
        addMember("entity", "equipment", "equipment()", "", "equipment");
        addMember("entity", "effects", "effects()", "", "");
        addMember("entity", "canSee", "canSee()", "видим ли мы её", "");
        addMember("client", "msg", "msg(text)", "в чат", "");
        addMember("client", "info", "info(text[, {effect | item}])", "тост; effect/item — id иконки", "");
        addMember("client", "success", "success(text[, {effect | item}])", "тост; effect/item — id иконки", "");
        addMember("client", "warn", "warn(text[, {effect | item}])", "тост; effect/item — id иконки", "");
        addMember("client", "error", "error(text[, {effect | item}])", "тост; effect/item — id иконки", "");
        addMember("client", "username", "username()", "", "");
        addMember("client", "lang", "lang(key[, fallback])", "строка из lang-файлов клиента на текущем языке", "");
        addMember("client", "server", "server()", "", "");
        addMember("client", "clipboard", "clipboard([text])", "буфер обмена", "");
        addMember("client", "module", "module(name)", "модуль клиента", "clientModule");
        addMember("client", "modules", "modules()", "имена модулей", "");
        addMember("client", "say", "say(text)", "сообщение на сервер; \"/...\" — команда", "");
        addMember("client", "command", "command(cmd)", "команда на сервер (без /)", "");
        addMember("client", "isFriend", "isFriend(name)", "друг или участник пати", "");
        addMember("client", "friends", "friends()", "ники друзей", "");
        addMember("clientModule", "setting", "setting(id)", "настройка по стабильному id (setting.id), не зависит от языка", "setting");
        addMember("clientModule", "enabled", "enabled()", "", "");
        addMember("clientModule", "toggle", "toggle()", "", "");
        addMember("clientModule", "setEnabled", "setEnabled(bool)", "", "");
        addMember("clientModule", "isEnabled", "isEnabled()", "", "");
        addMember("clientModule", "name", "name", "", "");
        addMember("clientModule", "category", "category", "вкладка ClickGui", "");
        addMember("clientModule", "subcategory", "subcategory", "группа в категории", "");
        addMember("clientModule", "description", "description()", "локализованное описание", "");
        addMember("clientModule", "hasSettings", "hasSettings()", "", "");
        addMember("clientModule", "settings", "settings()", "хэндлы настроек модуля", "");
        addMember("client", "categories", "categories()", "вкладки ClickGui с модулями", "");
        addMember("clientCategory", "name", "name", "", "");
        addMember("clientCategory", "icon", "icon", "имя иконки для render.icon", "");
        addMember("clientCategory", "description", "description", "", "");
        addMember("clientCategory", "modules", "modules()", "модули категории", "");
        addMember("assets", "path", "path(name)", "абсолютный путь", "");
        addMember("assets", "dir", "dir()", "папка assets", "");
        addMember("assets", "exists", "exists(name)", "", "");
        addMember("assets", "list", "list([subdir])", "имена файлов", "");
        addMember("assets", "read", "read(name)", "текст файла", "");
        addMember("assets", "write", "write(name, text)", "", "");
        addMember("assets", "delete", "delete(name)", "", "");
        addMember("assets", "json", "json(name[, def])", "распарсить JSON", "");
        addMember("assets", "writeJson", "writeJson(name, value)", "", "");
        addMember("storage", "get", "get(key[, def])", "", "");
        addMember("storage", "set", "set(key, value)", "", "");
        addMember("storage", "remove", "remove(key)", "", "");
        addMember("storage", "keys", "keys()", "", "");
        addMember("color", "rgb", "rgb(r, g, b[, a])", "", "");
        addMember("color", "hex", "hex(\"#82AAFF\")", "", "");
        addMember("color", "alpha", "alpha(color, 0..1)", "заменить альфу", "");
        addMember("color", "multAlpha", "multAlpha(color, 0..1)", "умножить текущую альфу", "");
        addMember("color", "darker", "darker(color[, f])", "", "");
        addMember("color", "brighter", "brighter(color[, f])", "", "");
        addMember("color", "mix", "mix(c1, c2, t)", "", "");
        addMember("color", "rainbow", "rainbow([speed, index])", "", "");
        addMember("color", "fade", "fade([speed, index])", "", "");
        addMember("color", "accent", "accent()", "цвет темы", "");
        addMember("color", "background", "background()", "", "");
        addMember("color", "foreground", "foreground()", "цвет поверх акцента/подсветки", "");
        addMember("color", "text", "text()", "", "");
        addMember("color", "textSecondary", "textSecondary()", "", "");
        addMember("color", "red", "red(c)", "", "");
        addMember("color", "green", "green(c)", "", "");
        addMember("color", "blue", "blue(c)", "", "");
        addMember("color", "alphaOf", "alphaOf(c)", "", "");
        addMember("client", "fps", "fps()", "", "");
        addMember("client", "ping", "ping()", "", "");
        addMember("client", "deltaTime", "deltaTime()", "длительность кадра в секундах", "");
        addMember("client", "funtime", "funtime()", "подключены ли к FunTime", "");
        addMember("client", "inGame", "inGame()", "", "");
        addMember("client", "screenOpen", "screenOpen()", "", "");
        addMember("client", "width", "width()", "ширина экрана", "");
        addMember("client", "height", "height()", "высота экрана", "");
        addMember("mc", "fps", "fps()", "", "");
        addMember("mc", "ping", "ping()", "", "");
        addMember("mc", "inGame", "inGame()", "", "");
        addMember("mc", "screenOpen", "screenOpen()", "", "");
        addMember(
            "mc",
            "screen",
            "screen()",
            "имя экрана: InventoryScreen, CreativeInventoryScreen, ChatScreen, GameMenuScreen, DeathScreen, HandledScreen (прочие контейнеры), \"\" — закрыт",
            ""
        );
        addMember("mc", "paused", "paused()", "", "");
        addMember("mc", "width", "width()", "ширина в scale", "");
        addMember("mc", "height", "height()", "высота в scale", "");
        addMember("mc", "windowWidth", "windowWidth()", "пиксели окна", "");
        addMember("mc", "windowHeight", "windowHeight()", "пиксели окна", "");
        addMember("mc", "fov", "fov()", "", "");
        addMember("mc", "sensitivity", "sensitivity()", "", "");
        addMember("mc", "renderDistance", "renderDistance()", "", "");
        addMember(
            "watermark",
            "chip",
            "chip(name, {icon, value, unit, badness, enabled})",
            "свой чип: icon — имя клиента или \"file.svg\" из assets, value/unit — текст или функция",
            "chip"
        );
        addMember("watermark", "icons", "icons()", "имена всех иконок клиента", "");
        addMember("chip", "name", "name", "", "");
        addMember("chip", "icon", "icon(name | \"file.svg\")", "иконка клиента или свой SVG из assets", "chip");
        addMember("chip", "value", "value(text|fn)", "значение чипа", "chip");
        addMember("chip", "unit", "unit(text|fn)", "единица измерения", "chip");
        addMember("chip", "badness", "badness(fn)", "0..1 — насколько плохое значение, красит чип", "chip");
        addMember("chip", "remove", "remove()", "убрать чип", "");
        addMember(
            "esp",
            "element",
            "element(name, {size, render, replaces, categories, anchor, icon, enabled})",
            "свой ESP-элемент: в списке элементов превью ESP, таскается и включается как встроенный. size — {w, h} или функция (e), render(e) рисует render.* в e.x, e.y, e.w, e.h; replaces: \"nametag\" — вместо встроенного, а не поверх",
            "espElement"
        );
        addMember(
            "esp",
            "chams",
            "chams(name, {shader, color, params, model, replaces, categories, icon, enabled})",
            "свои чамсы: .fsh из assets красит геометрию модели. Входы как у встроенного: vertexColor (color), texCoord0, viewPos, viewNormal, localPos; Sampler0 — скин, Sampler1 — мир позади, Sampler3 — шум; UBO SadnesScriptFx (uTime, uP0..uP3 из params). Выход premultiplied: vec4(rgb * a, a); model: true — не прятать модель",
            "espElement"
        );
        addMember(
            "esp",
            "glow",
            "glow(name, {shader, radius, color, params, replaces, categories, icon, enabled})",
            "свой экранный шейдер по силуэтам сущностей: texCoord0, tint (color); Sampler0.r — размытое на radius покрытие, Sampler1 — резкий силуэт (rgb модели, a — покрытие), Sampler2 — кадр; UBO SadnesScriptFx (uTime, uSize = radius, uP0..uP3). Выход premultiplied: vec4(rgb, 0) — свечение, vec4(rgb * a, a) — наложение",
            "espElement"
        );
        addMember("esp", "nametag", "nametag", "встроенный нейм-тег: свои сегменты и фон", "espNametag");
        addMember(
            "esp", "categories", "categories()", "категории: friends, naked, invisible, others, self, animal, mob, item; в categories можно \"players\"", ""
        );
        addMember("espElement", "name", "name", "", "");
        addSettingFactories("espElement");
        addMember("espElement", "remove", "remove()", "убрать элемент", "");
        addMember(
            "espNametag",
            "segment",
            "segment(id, {text, icon, color, iconColor, iconAfter, categories, enabled} | {width, draw})",
            "свой сегмент нейм-тега (тогл в «Элементах»): text/color — строка или функция (e); width(e) + draw(e) — рисуешь сам",
            "espSegment"
        );
        addMember(
            "espNametag",
            "background",
            "background(fn)",
            "свой фон плашки: fn(e) рисует render.rect/gradient в e.x, e.y, e.w, e.h; return false — стандартный; null — снять",
            ""
        );
        addMember("espSegment", "id", "id", "", "");
        addMember("espSegment", "remove", "remove()", "убрать сегмент", "");
        addMember(
            "targethud",
            "style",
            "style(name, {width, height, render(hud), hit(target)})",
            "свой стиль карточки TargetHud (рядом с classic/ring): render(hud) рисует каждый кадр, hit(target) — на наш удар по цели (необязательно). hud: target (или нет значения — карточка гаснет), name, x, y, width, height, health, maxHealth, absorption, alpha, showArmor, particles, eatingBadge(cx, cy[, alpha])",
            ""
        );
        addMember("aura", "rotation", "rotation(name, fn)", "свой режим наводки", "");
        addMember("aura", "esp", "esp(name, fn)", "свой стиль таргет-ESP", "");
        addMember("aura", "rangeHook", "rangeHook(fn)", "fn({source, range}) → число: своя дистанция удара ауры/триггербота", "");
        addMember("aura", "hitMomentHook", "hitMomentHook(fn)", "fn({canCrit, noRoomToJump}) → true бить / false не ждать крит", "");
        addMember("aura", "track", "track(entity)", "своя цель скрипта для таргет-ESP и TargetHud; звать каждый тик, пока цель активна", "");
        addMember("aura", "target", "target()", "цель ауры", "entity");
        addMember("aura", "range", "range()", "дистанция удара", "");
        addMember("aura", "enabled", "enabled()", "", "");
        addMember("aura", "shouldAttack", "shouldAttack()", "", "");
        addMember("aura", "cooldown", "cooldown()", "0..1", "");
        addMember("aura", "angleTo", "angleTo(entity)", "{yaw, pitch}", "");
        addMember("aura", "visibleAngleTo", "visibleAngleTo(entity)", "видимая точка", "");
        addMember("aura", "lookVec", "lookVec()", "взгляд серверной ротации", "vec");
        addMember("aura", "aimDistance", "aimDistance(entity)", "дистанция прицеливания", "");
        addMember("aura", "closestPoint", "closestPoint(entity[, x, y, z])", "ближайшая точка бокса", "vec");
        addMember("aura", "bestVisiblePoint", "bestVisiblePoint(entity)", "видимая точка бокса или null", "vec");
        addMember("aura", "angleToPoint", "angleToPoint(x, y, z)", "{yaw, pitch}", "");
        addMember("aura", "canSeePoint", "canSeePoint(x, y, z)", "рейтрейс до точки", "");
        addMember("aura", "rayTraceYaw", "rayTraceYaw(entity|null, yaw)", "попадёт ли луч по yaw", "");
        addMember("aura", "rayTracePitch", "rayTracePitch(entity|null, pitch)", "попадёт ли луч по pitch", "");
        addMember("script", "name", "name", "имя папки", "");
        addMember("script", "dir", "dir", "путь к скрипту", "");
        addMember("script", "platform", "platform", "windows/macos/linux", "");
        addMember("move", "speed", "speed()", "блок/тик", "");
        addMember("move", "isMoving", "isMoving()", "", "");
        addMember("move", "setSpeed", "setSpeed(speed[, yaw])", "разгон по вводу", "");
        addMember("move", "velocity", "velocity(x, y, z)", "задать вектор", "");
        addMember("move", "add", "add(x, y, z)", "добавить к вектору", "");
        addMember("move", "stop", "stop()", "", "");
        addMember("move", "jump", "jump([force])", "прыжок; force — и не с земли", "");
        addMember("move", "holdJump", "holdJump(ms)", "держать прыжок; броски клиента ждут", "");
        addMember("move", "escapeFrom", "escapeFrom(entity)", "{x, z} куда бежать от сущности или null", "vec");
        addMember("move", "forward", "forward(speed[, yaw])", "{x, y, z} по направлению", "");
        addMember("player", "useItem", "useItem([hand])", "пакет использования", "");
        addMember("player", "swing", "swing([hand])", "взмах рукой", "");
        addMember("player", "slot", "slot([n])", "активный слот", "");
        addMember("timer", "passed", "passed(ms)", "", "");
        addMember("timer", "reset", "reset()", "", "");
        addMember("timer", "elapsed", "elapsed()", "", "");
        addMember("animation", "run", "run(to, seconds[, easing, safe])", "повтор с той же целью не перезапускает", "");
        addMember("animation", "update", "update()", "звать раз в кадр, до get()", "");
        addMember("animation", "get", "get()", "", "");
        addMember("animation", "set", "set(value)", "мгновенно, без анимации", "");
        addMember("animation", "target", "target()", "последний to из run", "");
        addMember("animation", "finished", "finished()", "", "");
        addMember("shader", "name", "name", "", "");
        addMember("font", "name", "name", "", "");
        addMember("item", "customName", "customName", "своё имя предмета или \"\"", "");
        addMember("item", "onCooldown", "onCooldown", "предмет на перезарядке у своего игрока", "");
        addMember("effect", "ambient", "ambient", "от маяка", "");
        addSettingMembers("group");
        registerEvents();
    }

    public record ApiEntry(String name, String signature, String detail, boolean function, String returns) {
    }
}
