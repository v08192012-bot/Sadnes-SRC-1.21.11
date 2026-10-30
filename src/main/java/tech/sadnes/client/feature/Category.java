package tech.sadnes.client.feature;

import java.util.Locale;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;

public enum Category implements CategoryDescriptor {
    COMBAT(Icon.SHIELD, "Бой: атака, оружие, криты, хитбоксы и расходники в бою."),
    MOVEMENT(Icon.RUN, "Передвижение: скорость, полёт, элитры, проходимость и помощь в движении."),
    PLAYER(Icon.USER, "Игрок: взаимодействие с миром, предметы, инвентарь и мелкие удобства."),
    VISUALS(Icon.EYE, "Визуал: ESP сущностей, прозрачность игроков, настройки камеры и оформление мира."),
    MISC(Icon.PACKAGE, "Разное: сервер, общение, чат, безопасность и настройки клиента."),
    INTERFACE(Icon.OBJECT_GROUP, "Интерфейс: HUD-элементы, водяной знак, список модулей и менеджер конфигов.");

    private final Icon icon;
    private final String fallbackDescription;

    public String getDisplayName() {
        String string = this.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }

    @Override
    public String getName() {
        return this.getDisplayName();
    }

    @Override
    public String getDescription() {
        return Language.translate(this.getDescriptionKey(), this.fallbackDescription);
    }

    public String getDescriptionKey() {
        return "category." + this.name().toLowerCase(Locale.ROOT) + ".desc";
    }

    Category(Icon icon2, String string) {
        this.icon = icon2;
        this.fallbackDescription = string;
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }
}
