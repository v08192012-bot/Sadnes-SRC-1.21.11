// @name Uppercuts
// @author {author}
// @version 1.0
// @description Комбо на серверах с ванильным отбрасыванием: удар на взлёте и отход назад от цели

var pack = Module("Uppercuts", true);
var feature = pack.feature("Uppercuts", "Combat", "COMBAT_TWEAKS")
    .description("feature.impl.combat.uppercuts");
var sTapTicks = feature.slider("uppercuts.sTapTicks", 2, 1, 10, 1);

// Счёт — в тиках ввода: move_input приходит раз в тик, поэтому S-Tap Ticks = ровно столько тиков отхода.
var ticksAfterHit = 0;
var target = null;

feature.onDisabled(function () { ticksAfterHit = 0; });

events.attack(function (e) {
    if (!feature.enabled() || !e.post) return;
    ticksAfterHit = sTapTicks.get();
    target = e.target;
});

// После удара пятимся строго от цели: оси ввода поворачиваются под направление на неё.
events.move_input(function (e) {
    if (!feature.enabled() || ticksAfterHit <= 0 || !target || !player.exists()) return;
    ticksAfterHit--;
    var yawToTarget = Math.atan2(target.z() - player.z(), target.x() - player.x()) * 180 / Math.PI - 90;
    var diff = wrapDegrees(player.yaw() - yawToTarget) * Math.PI / 180;
    e.setForward(Math.round(-Math.cos(diff)));
    e.setStrafe(Math.round(-Math.sin(diff)));
});

// Дальше бьём: аура — с 4 блоков, триггербот — с 5.
aura.rangeHook(function (ctx) {
    if (feature.enabled()) return ctx.source === "trigger" ? 5.0 : 4.0;
});

// Удар на взлёте вместо ожидания крита на падении.
aura.hitMomentHook(function () {
    if (feature.enabled()) return !player.onGround() && player.velocity().y > 0;
});

function wrapDegrees(degrees) {
    var d = degrees % 360;
    if (d >= 180) d -= 360;
    if (d < -180) d += 360;
    return d;
}
