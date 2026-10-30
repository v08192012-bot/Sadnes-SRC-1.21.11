// @name SneakTap
// @author {author}
// @version 1.0
// @description Приседает в момент удара по противнику

var pack = Module("SneakTap", true);
var feature = pack.feature("SneakTap", "Combat", "AURA")
    .description("feature.impl.combat.sneaktap");
var sitDelay = feature.slider("sneaktap.sitDelay", 100, 50, 300, 5, "ms");

// Отсчёт в тиках ввода, а не в мс: move_input приходит раз в тик (~50 мс), и миллисекундное окно
// могло бы проскочить между тиками — тогда присед на удар не сработал бы. Минимум — 1 тик.
var sneakTicks = 0;

events.attack(function (e) {
    if (feature.enabled() && e.stage === "pre" && e.target && e.target.isLiving()) {
        sneakTicks = Math.max(1, Math.round(sitDelay.get() / 50));
    }
});

events.move_input(function (e) {
    if (!feature.enabled() || sneakTicks <= 0) return;
    e.setSneaking(true);
    sneakTicks--;
});
