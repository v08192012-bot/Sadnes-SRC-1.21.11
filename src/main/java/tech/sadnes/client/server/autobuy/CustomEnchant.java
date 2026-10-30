package tech.sadnes.client.server.autobuy;


public enum CustomEnchant {
    EXPERIENCED("Опытный", 3),
    DETECTION("Детекция", 3),
    SCOUT("Скаут", 3),
    STUPOR("Ступор", 3),
    POISON("Яд", 3),
    VAMPIRISM("Вампиризм", 2),
    OXIDATION("Окисление", 2),
    ATTRACTION("Притяжение", 2),
    SNIPER("Снайпер", 2),
    BULLDOZER("Бульдозер", 2),
    RETURN("Возвращение", 1),
    DEMOLITIONIST("Подрывник", 1),
    MAGNET("Магнит", 1),
    AUTO_SMELT("Авто-Плавка", 1),
    COBWEB("Паутина", 1),
    PINGER("Пингер", 1);

    private final String displayName;
    private final int maxLevel;

    public String getDisplayName() {
        return this.displayName;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    CustomEnchant(String string, int i) {
        this.displayName = string;
        this.maxLevel = i;
    }
}
