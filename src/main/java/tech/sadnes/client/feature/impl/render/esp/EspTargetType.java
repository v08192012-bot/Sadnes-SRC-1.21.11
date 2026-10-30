package tech.sadnes.client.feature.impl.render.esp;


public enum EspTargetType {
    FRIENDS("Друзья"),
    NAKED("Голые"),
    INVISIBLE("Невидимые"),
    OTHERS("Обычные"),
    SELF("На себе"),
    ANIMAL("Животные"),
    MOB("Мобы"),
    ITEM("Предметы");

    private final String displayName;

    EspTargetType(String string) {
        this.displayName = string;
    }

    public boolean isPlayerType() {
        return this == FRIENDS || this == NAKED || this == INVISIBLE || this == OTHERS || this == SELF;
    }

    public EspTargetType next() {
        EspTargetType[] espTargetTypes = values();
        return espTargetTypes[(this.ordinal() + 1) % espTargetTypes.length];
    }

    public EspTargetType previous() {
        EspTargetType[] espTargetTypes = values();
        return espTargetTypes[(this.ordinal() - 1 + espTargetTypes.length) % espTargetTypes.length];
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
