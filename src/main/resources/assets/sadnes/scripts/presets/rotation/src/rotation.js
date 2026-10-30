// Утилита ротации: подключается из main.js через include("rotation.js").
// Все расчёты собраны здесь, чтобы main.js читался как сценарий, а не как математика.

var RotationUtil = {

    // Точка прицеливания: лучшая видимая точка бокса (рейтрейс), иначе ближайшая к глазам.
    aimPoint: function (target) {
        var visible = aura.bestVisiblePoint(target);
        if (visible) return visible;
        return aura.closestPoint(target);
    },

    // Кратчайшая разница углов (-180..180) — крутим в ближнюю сторону, не через спину.
    wrap: function (angle) {
        angle = angle % 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    },

    // Человеческий шум: маленький случайный сдвиг обоих углов.
    addJitter: function (angle, amount) {
        return {
            yaw: angle.yaw + (Math.random() - 0.5) * amount,
            pitch: angle.pitch + (Math.random() - 0.5) * amount
        };
    },

    // Плавное доведение: чем ближе заряд удара к 1, тем агрессивнее доворот.
    easeTo: function (fromYaw, fromPitch, angle, smooth, cooldown) {
        var t = smooth * (0.5 + 0.5 * cooldown);
        var yaw = fromYaw + this.wrap(angle.yaw - fromYaw) * t;
        var pitch = fromPitch + this.wrap(angle.pitch - fromPitch) * t;
        return { yaw: yaw, pitch: Math.max(-89, Math.min(89, pitch)) };
    }
};
