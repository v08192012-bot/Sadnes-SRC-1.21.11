// @name {name}
// @author {author}
// @version 1.0
// @description Своя ротация ауры: выбери режим "{name}" в настройках AttackAura

include("rotation.js"); // утилита: сглаживание, джиттер, выбор точки

// Настройки скрипта (шестерёнка на карточке).
var smooth = settings.slider("Smooth", 0.55, 0.1, 1.0, 0.05);
var jitter = settings.slider("Jitter", 0.25, 0.0, 1.0, 0.05);

// Регистрирует режим в списке ротаций ауры. ctx: target, yaw, pitch (серверные),
// distance, range, cooldown (0..1). Вернуть {yaw, pitch, yawSpeed, pitchSpeed, returnSpeed};
// ничего не вернуть — тик пропущен.
aura.rotation("{name}", function (ctx) {
    // 1. Точка прицеливания: видимая точка бокса, иначе ближайшая к глазам.
    var point = RotationUtil.aimPoint(ctx.target);
    if (!point) return; // цель целиком за стеной — не крутим

    // 2. Углы на точку + человеческий шум.
    var angle = aura.angleToPoint(point.x, point.y, point.z);
    angle = RotationUtil.addJitter(angle, jitter.get());

    // 3. Плавное доведение от текущего серверного угла (заряд удара ускоряет доводку).
    var eased = RotationUtil.easeTo(ctx.yaw, ctx.pitch, angle, smooth.get(), ctx.cooldown);

    // 4. Скорости: если луч уже попадает по yaw — не дёргаем его лишний раз.
    var yawSpeed = aura.rayTraceYaw(ctx.target, ctx.yaw) ? 25 : 45;

    return {
        yaw: eased.yaw,
        pitch: eased.pitch,
        yawSpeed: yawSpeed,
        pitchSpeed: 30,
        returnSpeed: 12
    };
});
