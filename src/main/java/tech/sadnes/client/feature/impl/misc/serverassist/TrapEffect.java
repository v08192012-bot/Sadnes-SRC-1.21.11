package tech.sadnes.client.feature.impl.misc.serverassist;

import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.math.StopWatch;

public enum TrapEffect {
    NONE(null, null, -1.0F),
    REGENERATION("serverassist.effect.regeneration", "Регенерация", -1.0F),
    WITHERING("serverassist.effect.withering", "Иссушение", -1.0F),
    RUSH("serverassist.effect.rush", "Спешка", -1.0F),
    GLOW("serverassist.effect.glow", "Свечение", -1.0F),
    BLINDNESS("serverassist.effect.blindness", "Слепота", 3000.0F),
    HORUS("serverassist.effect.horus", "Нельзя есть хорус ещё", 5000.0F),
    PEARL("serverassist.effect.pearl", "Нельзя кидать пёрл ещё", 10000.0F),
    HORUS_DRAGON("serverassist.effect.horus_dragon", "Нельзя есть хорус и кидать пёрл ещё", 5000.0F);

    private final String nameKey;
    private final String nameFallback;
    private final float cooldown;
    private final StopWatch timer = new StopWatch();

    TrapEffect(String string, String string2, float f) {
        this.nameKey = string;
        this.nameFallback = string2;
        this.cooldown = f;
    }

    public String getDisplayName() {
        return this.nameKey == null ? "" : Language.translate(this.nameKey, this.nameFallback);
    }

    public String getNameKey() {
        return this.nameKey;
    }

    public String getNameFallback() {
        return this.nameFallback;
    }

    public float getCooldown() {
        return this.cooldown;
    }

    public StopWatch getTimer() {
        return this.timer;
    }
}
