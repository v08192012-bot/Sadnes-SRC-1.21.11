package tech.sadnes.client.server;

import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum DonateItem {
    DESORIENTATION(ServerType.FUNTIME, "Дезориентация", Items.ENDER_EYE),
    DUST(ServerType.FUNTIME, "Явная пыль", Items.SUGAR),
    BALL(ServerType.FUNTIME, "Огненный шар", Items.FIRE_CHARGE),
    AURA(ServerType.FUNTIME, "Божья аура", Items.PHANTOM_MEMBRANE),
    SNOWBALL(ServerType.FUNTIME, "Снежок заморозка", Items.SNOWBALL),
    PLAST(ServerType.FUNTIME, "Пласт", Items.DRIED_KELP),
    TRAP(ServerType.FUNTIME, "Трапка", Items.NETHERITE_SCRAP),
    POPPER(ServerType.FUNTIME, "Хлопушка", Items.SPLASH_POTION),
    HOLY_WATER(ServerType.FUNTIME, "Святая вода", Items.SPLASH_POTION),
    RAGE(ServerType.FUNTIME, "Зелье Гнева", Items.SPLASH_POTION),
    PALADIN(ServerType.FUNTIME, "Зелье Палладина", Items.SPLASH_POTION),
    ASSASSIN(ServerType.FUNTIME, "Зелье Ассасина", Items.SPLASH_POTION),
    RADIATION(ServerType.FUNTIME, "Зелье Радиации", Items.SPLASH_POTION),
    SLEEP(ServerType.FUNTIME, "Снотворное", Items.SPLASH_POTION),
    BUFF(ServerType.FUNTIME, "Перебаф донок", Items.POTATO),
    ALTERNATIVE_TRAP(ServerType.HOLYWORLD, "Обычная трапка", Items.POPPED_CHORUS_FRUIT),
    EXPLOSIVE_TRAP(ServerType.HOLYWORLD, "Взрывная трапка", Items.PRISMARINE_SHARD),
    EXPLOSIVE_STUFF(ServerType.HOLYWORLD, "Взрывная штучка", Items.FIRE_CHARGE),
    STUN_STAR(ServerType.HOLYWORLD, "Стан", Items.NETHER_STAR),
    SNOW_LUMP(ServerType.HOLYWORLD, "Ком снега", Items.SNOWBALL),
    JAKE_LUMINAIRE(ServerType.HOLYWORLD, "Светильник джейка", Items.CARVED_PUMPKIN),
    ANTIFLY(ServerType.REALLYWORLD, "Анти-полёт", Items.FIREWORK_STAR);

    private final ServerType server;
    private final String displayName;
    private final Item item;

    public boolean isPotion() {
        switch (this) {
            case POPPER:
            case HOLY_WATER:
            case RAGE:
            case PALADIN:
            case ASSASSIN:
            case RADIATION:
            case SLEEP:
            case BUFF:
                return true;
            default:
                return false;
        }
    }

    public SpecialPotion getPotionType() {
        switch (this) {
            case POPPER:
                return SpecialPotion.BOOMKA;
            case HOLY_WATER:
                return SpecialPotion.HOLY;
            case RAGE:
                return SpecialPotion.RAGE;
            case PALADIN:
                return SpecialPotion.PALADIN;
            case ASSASSIN:
                return SpecialPotion.ASSASSIN;
            case RADIATION:
                return SpecialPotion.RADIATION;
            case SLEEP:
                return SpecialPotion.SLEEPS;
            default:
                return null;
        }
    }

    public ItemStack createStack() {
        ItemStack itemStack = new ItemStack(this.item);
        SpecialPotion specialPotion = this.getPotionType();
        if (specialPotion == null) {
            return itemStack;
        }

        itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(specialPotion.getColor()), List.of(), Optional.empty()));
        return itemStack;
    }

    public boolean isPositivePotion() {
        switch (this) {
            case POPPER:
            case HOLY_WATER:
            case RAGE:
            case PALADIN:
            case ASSASSIN:
                return true;
            default:
                return false;
        }
    }

    public ServerType getServer() {
        return this.server;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Item getItem() {
        return this.item;
    }

    DonateItem(ServerType serverType, String string, Item item2) {
        this.server = serverType;
        this.displayName = string;
        this.item = item2;
    }
}
