// @name {name}
// @author {author}
// @version 1.0
// @description ESP Lab: свой элемент «Угроза», вспышка попадания, сегменты нейм-тега, плашка цели ауры, свои чамсы и глов на шейдерах

// Всё, что объявлено через esp.*, появляется в превью ESP (Visuals → Esp, ПКМ по превью — список
// элементов): свои элементы включаются и таскаются как встроенные, сегменты нейм-тега — тоглы
// в «Элементах» настроек Nametag.

var GREEN = Color.rgb(90, 220, 130);
var YELLOW = Color.rgb(250, 205, 70);
var RED = Color.rgb(240, 70, 80);

var previewTarget = settings.checkbox("Target plate in preview", true);

// ── Оценка угрозы: броня, оружие, здоровье и близость → 0..1 ─────────────
// Колбэки ESP зовутся каждый кадр, а перебирать предметы так часто незачем: кэш на 250 мс по id.
var threatCache = {};
setInterval(function () { threatCache = {}; }, 30000);

function weaponScore(item) {
    if (!item || item.empty) return 0;
    var id = item.id;
    if (id.indexOf("mace") >= 0) return 1.0;
    if (id.indexOf("netherite_sword") >= 0 || id.indexOf("netherite_axe") >= 0) return 0.9;
    if (id.indexOf("diamond_sword") >= 0 || id.indexOf("diamond_axe") >= 0) return 0.75;
    if (id.indexOf("end_crystal") >= 0 || id.indexOf("totem") >= 0) return 0.6;
    if (id.indexOf("sword") >= 0 || id.indexOf("axe") >= 0 || id.indexOf("trident") >= 0) return 0.5;
    if (id.indexOf("bow") >= 0) return 0.45;
    return 0.1;
}

function threatOf(entity) {
    var now = Date.now();
    var cached = threatCache[entity.id()];
    if (cached && now - cached.at < 250) return cached.value;
    var armor = Math.min(entity.armor() / 20, 1);
    var hp = entity.maxHealth() > 0 ? entity.health() / entity.maxHealth() : 0;
    var near = 1 - Math.min(entity.distance() / 24, 1);
    var value = armor * 0.35 + weaponScore(entity.mainHand()) * 0.35 + hp * 0.15 + near * 0.15;
    threatCache[entity.id()] = {value: value, at: now};
    return value;
}

// Зелёный → жёлтый → красный.
function heat(t) {
    t = Math.max(0, Math.min(1, t));
    return t < 0.5 ? Color.mix(GREEN, YELLOW, t * 2) : Color.mix(YELLOW, RED, (t - 0.5) * 2);
}

// ── Элемент «Угроза»: таблетка справа от бокса (снизу уже висят предметы в руках) ──
var threat = esp.element("Threat", {
    categories: ["players"],
    anchor: "right",
    icon: "skull",
    size: {w: 46, h: 10},
    render: function (e) {
        var t = threatOf(e.entity);
        var color = heat(t);
        render.rect(e.x, e.y, e.w, e.h, Color.alpha(Color.background(), 0.8), 3);
        if (pulse.get() && t > 0.7) {
            var k = 0.5 + 0.5 * Math.sin(Date.now() / 140);
            render.outline(e.x, e.y, e.w, e.h, 0.6, Color.alpha(color, 0.3 + 0.5 * k), 3);
        }
        render.icon("skull", e.x + 2.5, e.y + 1.8, 5, color);
        var label = showPercent.get() ? Math.round(t * 100) + "%" : (t > 0.7 ? "HIGH" : t > 0.4 ? "MID" : "LOW");
        render.text(label, e.x + 9.5, e.y + 1.6, Color.text(), 5.5);
        // Полоска снизу: градиент от зелёного к цвету текущей угрозы (gradient4: право, право, лево, лево).
        var barW = Math.max((e.w - 5) * t, 1.5);
        render.gradient4(e.x + 2.5, e.y + e.h - 2.5, barW, 1.2, color, color, GREEN, GREEN, 0.6);
    }
});
var showPercent = threat.checkbox("Show percent", true);
var pulse = threat.checkbox("Pulse when dangerous", true);

// ── Элемент «Вспышка»: кольцо у головы, пока сущность в hurt-анимации ─────
esp.element("Hit flash", {
    categories: ["players", "mob", "animal"],
    anchor: "top_right",
    icon: "bolt",
    // null — для этой сущности элемента нет (не бьют — нечего рисовать).
    size: function (e) {
        return e.preview || e.entity.hurtTime() > 0 ? {w: 12, h: 12} : null;
    },
    render: function (e) {
        var k = e.preview ? 0.5 + 0.5 * Math.sin(Date.now() / 220) : e.entity.hurtTime() / 10;
        var cx = e.x + e.w / 2, cy = e.y + e.h / 2;
        render.ring(cx, cy, 3 + (1 - k) * 2.5, 1.2, Color.alpha(RED, k));
        render.icon("bolt", cx - 2.5, cy - 2.5, 5, Color.alpha(YELLOW, k));
    }
});

// ── Свои шейдеры: чамсы и глов из assets/*.fsh ───────────────────────────
// Появляются в том же списке элементов превью (выключены по умолчанию); рисуются только в мире.
// color/params читаются раз в кадр: так настройки из шестерёнки сразу попадают в шейдер.
// Правка .fsh перезагружает скрипт сама, ошибки GLSL — в консоли скрипта.

var hologram = esp.chams("Hologram", {
    shader: "hologram.fsh",
    categories: ["players"],
    enabled: false,
    color: function () { return holoColor.get(); },
    params: function () { return [holoDensity.get(), holoSpeed.get(), 0, 0]; }
});
var holoColor = hologram.color("Color", Color.rgb(90, 200, 255));
var holoDensity = hologram.slider("Line density", 6, 1, 20, 1);
var holoSpeed = hologram.slider("Line speed", 0.6, 0, 3, 0.1);

var pulseGlow = esp.glow("Pulse glow", {
    shader: "pulse_glow.fsh",
    categories: ["players", "mob"],
    enabled: false,
    radius: function () { return pulseRadius.get(); },
    params: function () { return [pulseSpeed.get(), pulseFill.get(), 0, 0]; }
});
var pulseRadius = pulseGlow.slider("Radius", 6, 2, 16, 0.5);
var pulseSpeed = pulseGlow.slider("Wave speed", 1, 0.1, 4, 0.1);
var pulseFill = pulseGlow.slider("Body fill", 0.15, 0, 1, 0.05);

// ── Сегменты встроенного нейм-тега ───────────────────────────────────────

// Дистанция: иконка краснеет, чем ближе игрок.
esp.nametag.segment("distance", {
    icon: "location_crosshairs",
    iconColor: function (e) { return heat(1 - Math.min(e.entity.distance() / 30, 1)); },
    text: function (e) { return e.preview ? "12m" : Math.round(e.entity.distance()) + "m"; },
    color: function (e) { return Color.textSecondary(); }
});

// Своя отрисовка: мини-полоска здоровья прямо в плашке.
esp.nametag.segment("hpbar", {
    width: function (e) { return e.entity.maxHealth() > 0 ? 18 : 0; },
    draw: function (e) {
        var hp = Math.max(0, Math.min(1, e.entity.health() / e.entity.maxHealth()));
        var y = e.y + e.h / 2 - 1.25;
        render.rect(e.x, y, e.w, 2.5, Color.alpha(Color.text(), 0.15), 1.25);
        render.rect(e.x, y, Math.max(e.w * hp, 2.5), 2.5, heat(1 - hp), 1.25);
    }
});

// Только иконка и только когда в руке серьёзное оружие: null прячет сегмент, "" — оставляет иконку.
esp.nametag.segment("armed", {
    categories: ["players"],
    icon: "swords",
    iconColor: RED,
    text: function (e) { return weaponScore(e.entity.mainHand()) >= 0.5 ? "" : null; }
});

// ── Фон плашки: цель ауры — пульсирующая красная, остальные — стандартная ─
esp.nametag.background(function (e) {
    var target = aura.target();
    var isTarget = e.preview ? previewTarget.get() : target && e.entity && target.id() === e.entity.id();
    if (!isTarget) return false;
    var k = 0.5 + 0.5 * Math.sin(Date.now() / 180);
    var base = Color.alpha(Color.background(), 0.85);
    render.shadow(e.x, e.y, e.w, e.h, 6, Color.alpha(RED, 0.25 + 0.25 * k), 3);
    render.gradient4(e.x, e.y, e.w, e.h, base, base, Color.alpha(RED, 0.55), Color.alpha(RED, 0.55), 3);
    render.outline(e.x, e.y, e.w, e.h, 0.6, Color.alpha(RED, 0.45 + 0.4 * k), 3);
});
