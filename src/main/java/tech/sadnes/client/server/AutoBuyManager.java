package tech.sadnes.client.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.item.equipment.trim.ArmorTrimPatterns;
import net.minecraft.registry.RegistryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.config.cloud.CloudConfigSync;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.ConfigKind;
import tech.sadnes.client.server.autobuy.CustomColorPotionItem;
import tech.sadnes.client.server.autobuy.CustomEnchant;
import tech.sadnes.client.server.autobuy.CustomEnchantRequirement;
import tech.sadnes.client.server.autobuy.DonateAutoBuyItem;
import tech.sadnes.client.server.autobuy.EnchantRequirement;
import tech.sadnes.client.server.autobuy.EnchantedAutoBuyItem;
import tech.sadnes.client.server.autobuy.FunTimeItem;
import tech.sadnes.client.server.autobuy.HolyWorldItem;
import tech.sadnes.client.server.autobuy.PotionEffectItem;
import tech.sadnes.client.server.autobuy.SpookyTimeItem;
import tech.sadnes.client.server.autobuy.VanillaEnchantRequirement;

public final class AutoBuyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBuyManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ORIGINAL_ITEM_LORE = "Оригинальный предмет";
    private static final String SPOOKY_STASH_ID_KEY = "spookystash:stash_id";
    private static final String SPOOKY_STASH_LEVELS_KEY = "spookystash:levels";
    private static final RegistryKey<ArmorTrimMaterial> CRUSHER_TRIM_PATTERN = ArmorTrimMaterials.REDSTONE;
    private static final RegistryKey<ArmorTrimPattern> CRUSHER_TRIM_MATERIAL = ArmorTrimPatterns.RAISER;
    private final List<AutoBuyItem> items = new CopyOnWriteArrayList<>();
    private final Path configPath;
    private AutoBuyManager.Mode mode = AutoBuyManager.Mode.BUY;
    private int stashAnarchy;
    private AutoBuyItem.Source currentSource = AutoBuyItem.Source.VANILLA;
    private CloudConfigSync cloudSync;

    public AutoBuyManager() {
        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("autobuy");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
            LOGGER.error("Failed to create autobuy directory", ioException);
        }

        this.configPath = path.resolve("autobuy.json");
        this.registerItems();
    }

    public List<AutoBuyItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public void initCloudSync(SadnesSocketClient sadnesSocketClient) {
        this.cloudSync = new CloudConfigSync(ConfigKind.AUTOBUY, sadnesSocketClient, this::serialize, this::applyCloudConfig);
    }

    public boolean hasCustomMatch(ItemStack itemStack, AutoBuyItem autoBuyItem) {
        if (itemStack != null && !itemStack.isEmpty() && autoBuyItem != null && !autoBuyItem.isCustom()) {
            ServerType serverType = this.getCurrentServer();
            int i = this.items.size();

            for (int i2 = 0; i2 < i; i2++) {
                AutoBuyItem autoBuyItem2 = this.items.get(i2);
                if (autoBuyItem2.isCustom() && autoBuyItem2.getItem() == autoBuyItem.getItem() && autoBuyItem2.isAvailableOn(serverType) && autoBuyItem2.matches(itemStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public AutoBuyItem findEnabledMatch(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            ServerType serverType = this.getCurrentServer();
            if (serverType == null) {
                return null;
            }

            int i = this.items.size();

            for (int i2 = 0; i2 < i; i2++) {
                AutoBuyItem autoBuyItem = this.items.get(i2);
                if (autoBuyItem.isEnabled() && autoBuyItem.isAvailableOn(serverType) && autoBuyItem.matches(itemStack)) {
                    return autoBuyItem;
                }
            }
        }

        return null;
    }

    public AutoBuyItem findMatch(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            ServerType serverType = this.getCurrentServer();
            if (serverType == null) {
                return null;
            }

            int i = this.items.size();

            for (int i2 = 0; i2 < i; i2++) {
                AutoBuyItem autoBuyItem = this.items.get(i2);
                if (autoBuyItem.isAvailableOn(serverType) && autoBuyItem.matches(itemStack)) {
                    return autoBuyItem;
                }
            }
        }

        return null;
    }

    private ServerType getCurrentServer() {
        for (ServerType serverType : ServerType.values()) {
            if (serverType.isCurrent()) {
                return serverType;
            }
        }

        return null;
    }

    public List<AutoBuyItem> getItemsForCurrentServer() {
        ServerType serverType = this.getCurrentServer();
        ServerType serverType2 = serverType != ServerType.SPOOKYTIME && serverType != ServerType.HOLYWORLD ? ServerType.FUNTIME : serverType;
        ArrayList arrayList = new ArrayList();

        for (AutoBuyItem autoBuyItem : this.items) {
            if (autoBuyItem.isAvailableOn(serverType2)) {
                arrayList.add(autoBuyItem);
            }
        }

        return arrayList;
    }

    private void registerItems() {
        this.registerSpookyTimeItems();
        this.registerHolyWorldItems();
        this.currentSource = AutoBuyItem.Source.CRUSHER;
        this.addCrusherArmor(Items.NETHERITE_HELMET, 4500000, "Шлем Крушителя");
        this.addCrusherArmor(Items.NETHERITE_CHESTPLATE, 2800000, "Нагрудник Крушителя");
        this.addCrusherArmor(Items.NETHERITE_LEGGINGS, 2600000, "Штаны Крушителя");
        this.addCrusherArmor(Items.NETHERITE_BOOTS, 8000000, "Ботинки Крушителя");
        this.addOriginalItem(Items.NETHERITE_SWORD, 10000000, "Меч Крушителя")
            .withFixedRequirements(this.concat(this.crusherSwordRequirements(), this.excluded(Enchantments.KNOCKBACK)));
        this.addOriginalItem(Items.NETHERITE_SWORD, 10000000, "Меч Крушителя (Отдача)", this.required(Enchantments.KNOCKBACK, 2))
            .withFixedRequirements(this.crusherSwordRequirements());
        this.addOriginalItem(Items.MACE, 15900000, "Булава Крушителя")
            .withFixedRequirements(this.concat(this.crusherSwordRequirements(), this.required(Enchantments.DENSITY, 5), this.required(Enchantments.BREACH, 3)));
        this.addOriginalItem(Items.NETHERITE_PICKAXE, 10900000, "Кирка Крушителя")
            .withFixedRequirements(
                this.required(Enchantments.EFFICIENCY, 10),
                this.required(Enchantments.FORTUNE, 5),
                this.required(Enchantments.UNBREAKING, 5),
                this.required(Enchantments.MENDING, 1)
            );
        this.addOriginalItem(Items.CROSSBOW, 2600000, "Арбалет Крушителя")
            .withFixedRequirements(
                this.required(Enchantments.PIERCING, 5),
                this.required(Enchantments.QUICK_CHARGE, 3),
                this.required(Enchantments.MULTISHOT, 1),
                this.required(Enchantments.MENDING, 1)
            );
        this.addOriginalItem(Items.BOW, 700000, "Лук Крушителя")
            .withFixedRequirements(
                this.required(Enchantments.POWER, 5),
                this.required(Enchantments.INFINITY, 1),
                this.required(Enchantments.FLAME, 1),
                this.required(Enchantments.UNBREAKING, 5),
                this.required(Enchantments.MENDING, 1)
            );
        this.currentSource = AutoBuyItem.Source.SPHERE;
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 70000000, "Талисман Крушителя", "Несокрушимая мощь");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 54000000, "Талисман Карателя", "Несёт строгий приговор");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 5000000, "Талисман Ярости", "Чистая, дикая агрессия");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 1200000, "Талисман Вихря", "Вихрь не знает покоя");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 700000, "Талисман Раздора", "Раздор жаждет хаоса");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 500000, "Талисман Тирана", "Тиран подавляет слабых");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 300000, "Талисман Мрака", "Мрак сгущается рядом");
        this.addLoreItem(Items.TOTEM_OF_UNDYING, 270000, "Талисман Демона", "Печать разжигает ярость");
        this.addLoreItem(Items.PLAYER_HEAD, 17000000, "Сфера Титана", "Мощь Титанов крепка")
            .withSkullTexture("81e9698458b7841c96ae4f24ec84ae01724100641c564e2a7b185f406e8ed23");
        this.addLoreItem(Items.PLAYER_HEAD, 3800000, "Сфера Гидры", "Живучесть темных глубин")
            .withSkullTexture("3e3c118d696d910e54de02ca4d807543f9b18c008c9838d2ff69377622fb1d32");
        this.addLoreItem(Items.PLAYER_HEAD, 1450000, "Сфера Хаоса", "Хаос искажает реальность")
            .withSkullTexture("e7a7ae7cdcf616e8b7a4221a621b2435753c60ed6a258ea060dae3002ffe9e28");
        this.addLoreItem(Items.PLAYER_HEAD, 1200000, "Сфера Ареса", "Дух Ареса пылает внутри")
            .withSkullTexture("c16adc6bafcb57fd707dee7dd6a736fe126711d53a1fd6ce789da41b3be13f2a");
        this.addLoreItem(Items.PLAYER_HEAD, 880000, "Сфера Бестии", "Звериная дикая мощь")
            .withSkullTexture("5411ac17381b9fce9bab3c72afdb7f198570daf4732bd811d31c227d80fa39b1");
        this.addLoreItem(Items.PLAYER_HEAD, 90000, "Сфера Икара", "Хранит волю Икара")
            .withSkullTexture("c6803e6d5667a2d610628bc3b32f863cda495c465616de655cb329933b61af77");
        this.addLoreItem(Items.PLAYER_HEAD, 89000, "Сфера Сатира", "Шёпот Сатира звучит")
            .withSkullTexture("771a9a498b4fa5ec49362f9bc88eda4f52b04de49d75aa3ca332a1fea1aa0e57");
        this.addLoreItem(Items.PLAYER_HEAD, 30000, "Сфера Эрида", "Холод Эриды вечен")
            .withSkullTexture("6e4e2f1047f3ec6e9e459184739e33b7c1fc63ad8202bdab9f024508add23e5b");
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.addLoreItem(Items.TRIPWIRE_HOOK, 1400000, "Отмычка к Сферам", "Открыть хранилище", "С Сферами");
        this.addLoreItem(Items.TRIPWIRE_HOOK, 190000, "Отмычка к Ресурсам", "Открыть хранилище", "С Ресурсами");
        this.addLoreItem(Items.TRIPWIRE_HOOK, 150000, "Отмычка к Оружию", "Открыть хранилище", "С Оружием");
        this.addLoreItem(Items.TRIPWIRE_HOOK, 100000, "Отмычка к Инструментам", "Открыть хранилище", "С Инструментами");
        this.addLoreItem(Items.TRIPWIRE_HOOK, 100000, "Отмычка к Броне", "Открыть хранилище", "С Броней");
        this.addLoreItem(Items.IRON_NUGGET, 5000, "Серебро", "отмычек к тайникам");
        this.addLoreItem(Items.GOLDEN_PICKAXE, 19000000, "Божье касание", "добыть спавнер");
        this.addLoreItem(Items.GOLDEN_PICKAXE, 1300000, "Мощный удар", "разрушить бедрок");
        this.addLoreItem(Items.NETHERITE_PICKAXE, 12500000, "Молот Тора", "Мега-бульдозер");
        this.currentSource = AutoBuyItem.Source.MODIFIER;
        this.addLoreItem(Items.FEATHER, 5666000, "Модификатор полёта", "Доступ к /fly");
        this.addLoreItem(Items.AMETHYST_SHARD, 800000, "Модификатор починки", "Доступ к /fix");
        this.addLoreItem(Items.ENDER_EYE, 799999, "Модификатор эндер-сундука", "Доступ к /ec");
        this.addLoreItem(Items.COMPASS, 669900, "Модификатор возврата", "Доступ к /back");
        this.addLoreItem(Items.RAW_IRON, 599000, "Модификатор наковальни", "Доступ к /anvil");
        this.addLoreItem(Items.LADDER, 499999, "Модификатор подъёма", "Доступ к /top");
        this.addLoreItem(Items.NAME_TAG, 399999, "Модификатор переименования", "Доступ к /name");
        this.addLoreItem(Items.POPPED_CHORUS_FRUIT, 299999, "Модификатор варпов", "Доступ к /setwarp");
        this.addLoreItem(Items.COOKED_BEEF, 190000, "Модификатор насыщения", "Доступ к /feed");
        this.addLoreItem(Items.GOLDEN_APPLE, 100000, "Модификатор исцеления", "Доступ к /heal");
        this.addLoreItem(Items.BUCKET, 100000, "Модификатор очистки", "Доступ к /clear");
        this.addLoreItem(Items.GOAT_HORN, 99999, "Модификатор вещания", "/bc, /me, /dchat");
        this.addLoreItem(Items.SLIME_BALL, 50000, "Модификатор прыжка", "Доступ к /jump");
        this.currentSource = AutoBuyItem.Source.SKIN;
        this.addLoreItem(Items.PAPER, 499999, "Драконий скин", "получаете Драконий скин");
        this.addLoreItem(Items.PAPER, 200000, "Неизбежный скин", "получаете Неизбежный скин");
        this.addLoreItem(Items.PAPER, 99999, "Ледяной скин", "получаете Ледяной скин");
        this.addLoreItem(Items.PAPER, 79365, "Смертельный скин", "получаете Смертельный скин");
        this.addLoreItem(Items.PAPER, 40000, "Коварный скин", "получаете Коварный скин");
        this.addLoreItem(Items.PAPER, 33333, "Жестокий скин", "получаете Жестокий скин");
        this.addLoreItem(Items.PAPER, 33333, "Мучительный скин", "получаете Мучительный скин");
        this.addLoreItem(Items.PAPER, 25000, "Призрачный скин", "получаете Призрачный скин");
        this.addLoreItem(Items.PAPER, 23437, "Сингулярный скин", "получаете Сингулярный скин");
        this.addLoreItem(Items.PAPER, 23437, "Царский скин", "получаете Царский скин");
        this.addLoreItem(Items.PAPER, 20166, "Зловещий скин", "получаете Зловещий скин");
        this.addLoreItem(Items.PAPER, 19999, "Проклятый скин", "получаете Проклятый скин");
        this.addLoreItem(Items.PAPER, 15625, "Мистический скин", "получаете Мистический скин");
        this.addLoreItem(Items.PAPER, 8333, "Жемчужный скин", "получаете Жемчужный скин");
        this.addLoreItem(Items.PAPER, 4444, "Кошмарный скин", "получаете Кошмарный скин");
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.addLoreItem(Items.SUGAR, 50000, "Явная пыль", "Каст: Световая вспышка");
        this.addLoreItem(Items.ENDER_EYE, 32000, "Дезориентация", "Каст: Звуковая волна");
        this.addLoreItem(Items.DRIED_KELP, 38000, "Пласт", "Каст: Нерушимая стена");
        this.addLoreItem(Items.NETHERITE_SCRAP, 200000, "Трапка", "Каст: Нерушимая клетка");
        this.addLoreItem(Items.FIRE_CHARGE, 13000, "Огненный шар", "Каст: Огненный шар");
        this.currentSource = AutoBuyItem.Source.POTION;
        this.register(new DonateAutoBuyItem(DonateItem.SLEEP, 1800000));
        this.register(new DonateAutoBuyItem(DonateItem.PALADIN, 700000));
        this.register(new DonateAutoBuyItem(DonateItem.ASSASSIN, 600000));
        this.register(new DonateAutoBuyItem(DonateItem.RAGE, 600000));
        this.register(new DonateAutoBuyItem(DonateItem.HOLY_WATER, 200000));
        this.register(new DonateAutoBuyItem(DonateItem.RADIATION, 70000));
        this.register(new DonateAutoBuyItem(DonateItem.POPPER, 67000));
        this.register(new DonateAutoBuyItem(DonateItem.AURA, 800000));
        this.register(new DonateAutoBuyItem(DonateItem.SNOWBALL, 30000));
        this.addArrow(11000, "Стрела обледенения", 65535);
        this.addArrow(9000, "Стрела Зевса", 16776960);
        this.addArrow(5600, "Кровавая стрела", 16711680);
        this.addArrow(3200, "Стрела терапии", 65280);
        this.addArrow(3200, "Мучительная стрела", 0);
        this.addArrow(1000, "Световая стрела", 16777215);
        this.register(new PotionEffectItem(5000, "Исцеление", List.of(StatusEffects.INSTANT_HEALTH)));
        this.register(new PotionEffectItem(20000, "Исцеление + Регенерация", List.of(StatusEffects.INSTANT_HEALTH, StatusEffects.REGENERATION)));
        this.register(new PotionEffectItem(15000, "Сила", List.of(StatusEffects.STRENGTH)));
        this.register(new PotionEffectItem(10000, "Скорость", List.of(StatusEffects.SPEED)));
        this.register(new PotionEffectItem(30000, "Сила + Скорость", List.of(StatusEffects.STRENGTH, StatusEffects.SPEED)));
        this.currentSource = AutoBuyItem.Source.GEAR;
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_HELMET, 5900000, this.helmetEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_CHESTPLATE, 1500000, this.armorEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_LEGGINGS, 960000, this.armorEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_BOOTS, 8000000, this.bootsEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.ELYTRA, 300000, this.enchant(Enchantments.UNBREAKING, 5), this.enchant(Enchantments.MENDING, 1)));
        this.register(new EnchantedAutoBuyItem(Items.MACE, 15900000, this.maceEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_SWORD, 4400000, this.swordEnchants()));
        this.register(
            new EnchantedAutoBuyItem(
                Items.TRIDENT,
                2970000,
                this.concat(
                    new EnchantRequirement[]{
                        this.enchant(Enchantments.IMPALING, 5),
                        this.enchant(Enchantments.LOYALTY, 4),
                        this.enchant(Enchantments.CHANNELING, 2),
                        this.enchant(Enchantments.SHARPNESS, 7),
                        this.enchant(Enchantments.FIRE_ASPECT, 2),
                        this.enchant(Enchantments.UNBREAKING, 5),
                        this.enchant(Enchantments.MENDING, 1),
                        this.customEnchant(CustomEnchant.SCOUT),
                        this.customEnchant(CustomEnchant.STUPOR),
                        this.customEnchant(CustomEnchant.ATTRACTION),
                        this.customEnchant(CustomEnchant.RETURN),
                        this.customEnchant(CustomEnchant.DEMOLITIONIST)
                    },
                    this.swordCustomEnchants()
                )
            )
        );
        this.register(new EnchantedAutoBuyItem(Items.CROSSBOW, 2600000, this.crossbowEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.BOW, 700000, this.bowEnchants()));
        this.register(new EnchantedAutoBuyItem(Items.NETHERITE_PICKAXE, 10900000, this.pickaxeEnchants()));
        this.register(
            new EnchantedAutoBuyItem(
                Items.ENCHANTED_BOOK,
                150000,
                this.enchant(Enchantments.PROTECTION, 5),
                this.enchant(Enchantments.SHARPNESS, 7),
                this.enchant(Enchantments.DENSITY, 5),
                this.enchant(Enchantments.UNBREAKING, 5),
                this.enchant(Enchantments.MENDING, 1)
            )
        );
        this.currentSource = AutoBuyItem.Source.VANILLA;
        this.register(new AutoBuyItem(Items.PIGLIN_HEAD, 1000000));
        this.register(new AutoBuyItem(Items.DRAGON_HEAD, 240000));
        this.register(new AutoBuyItem(Items.WITHER_SKELETON_SKULL, 90000));
        this.register(new AutoBuyItem(Items.SKELETON_SKULL, 25000));
        this.register(new AutoBuyItem(Items.CREEPER_HEAD, 5000));
        this.register(new AutoBuyItem(Items.ZOMBIE_HEAD, 3000));
        this.register(new AutoBuyItem(Items.DIAMOND_SWORD, 10000));
        this.register(new AutoBuyItem(Items.DIAMOND_HELMET, 8000));
        this.register(new AutoBuyItem(Items.DIAMOND_CHESTPLATE, 15000));
        this.register(new AutoBuyItem(Items.DIAMOND_LEGGINGS, 12000));
        this.register(new AutoBuyItem(Items.DIAMOND_BOOTS, 8000));
        this.register(new AutoBuyItem(Items.NETHERITE_INGOT, 1000000));
        this.register(new AutoBuyItem(Items.NETHERITE_SCRAP, 250000));
        this.register(new AutoBuyItem(Items.ANCIENT_DEBRIS, 250000));
        this.register(new AutoBuyItem(Items.TOTEM_OF_UNDYING, 1200000));
        this.register(new AutoBuyItem(Items.SHULKER_BOX, 10000));
        this.register(new AutoBuyItem(Items.BUNDLE, 30000));
        this.register(new AutoBuyItem(Items.WIND_CHARGE, 12000));
        this.register(new AutoBuyItem(Items.ENCHANTED_GOLDEN_APPLE, 100000));
        this.register(new AutoBuyItem(Items.GOLDEN_APPLE, 10000));
        this.register(new AutoBuyItem(Items.GOLDEN_CARROT, 2000));
        this.register(new AutoBuyItem(Items.CHORUS_FRUIT, 3000));
        this.register(new AutoBuyItem(Items.ENDER_PEARL, 3000));
        this.register(new AutoBuyItem(Items.EXPERIENCE_BOTTLE, 1500).markCustom());
        this.register(new AutoBuyItem(Items.FIREWORK_ROCKET, 1500));
        this.register(new AutoBuyItem(Items.DIAMOND, 5000));
        this.register(new AutoBuyItem(Items.VILLAGER_SPAWN_EGG, 400000));
        this.register(new AutoBuyItem(Items.ZOMBIE_VILLAGER_SPAWN_EGG, 180000));
        Iterator iterator2 = this.items.iterator();

        while (iterator2.hasNext()) {
            ((AutoBuyItem)iterator2.next()).setEnabled(false);
        }
    }

    private void registerSpookyTimeItems() {
        this.currentSource = AutoBuyItem.Source.SPHERE;
        this.addSpookySphere(Items.PLAYER_HEAD, 11000000, "Сфера Хаоса", "attribute-item-shaosa")
            .withSkullTexture("e7a7ae7cdcf616e8b7a4221a621b2435753c60ed6a258ea060dae3002ffe9e28");
        this.addSpookySphere(Items.PLAYER_HEAD, 10600000, "Сфера Гидры", "attribute-item-shydra")
            .withSkullTexture("3e3c118d696d910e54de02ca4d807543f9b18c008c9838d2ff69377622fb1d32");
        this.addSpookySphere(Items.PLAYER_HEAD, 10000000, "Сфера Ареса", "attribute-item-saresa")
            .withSkullTexture("c16adc6bafcb57fd707dee7dd6a736fe126711d53a1fd6ce789da41b3be13f2a");
        this.addSpookySphere(Items.PLAYER_HEAD, 5300000, "Сфера Бестии", "attribute-item-sbestii")
            .withSkullTexture("5411ac17381b9fce9bab3c72afdb7f198570daf4732bd811d31c227d80fa39b1");
        this.addSpookySphere(Items.PLAYER_HEAD, 4550000, "Сфера Титана", "attribute-item-stitana")
            .withSkullTexture("81e9698458b7841c96ae4f24ec84ae01724100641c564e2a7b185f406e8ed23");
        this.addSpookySphere(Items.PLAYER_HEAD, 3300000, "Сфера Эрида", "attribute-item-serida")
            .withSkullTexture("6e4e2f1047f3ec6e9e459184739e33b7c1fc63ad8202bdab9f024508add23e5b");
        this.addSpookySphere(Items.PLAYER_HEAD, 2400000, "Сфера Сатира", "attribute-item-ssatira")
            .withSkullTexture("771a9a498b4fa5ec49362f9bc88eda4f52b04de49d75aa3ca332a1fea1aa0e57");
        this.addSpookySphere(Items.PLAYER_HEAD, 2100000, "Сфера Икара", "attribute-item-sikara")
            .withSkullTexture("c6803e6d5667a2d610628bc3b32f863cda495c465616de655cb329933b61af77");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 79000000, "Талисман Карателя", "attribute-item-tkaratela");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 60000000, "Талисман Крушителя", "attribute-item-tkryshitela");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 19999999, "Талисман Ярости", "attribute-item-tyarosti");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 13500000, "Талисман Раздора", "attribute-item-trazdora");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 4750000, "Талисман Демона", "attribute-item-tdemona");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 4700000, "Талисман Вихря", "attribute-item-tvihra");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 4000000, "Талисман Тирана", "attribute-item-ttirana");
        this.addSpookySphere(Items.TOTEM_OF_UNDYING, 2850000, "Талисман Мрака", "attribute-item-tmraka");
        this.currentSource = AutoBuyItem.Source.CRUSHER;
        this.register(new SpookyTimeItem(Items.NETHERITE_HELMET, 7000000, "Шлем Крушителя", "helmet-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_CHESTPLATE, 1700000, "Нагрудник Крушителя", "chestplate-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_LEGGINGS, 900000, "Поножи Крушителя", "leggings-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_BOOTS, 4350000, "Ботинки Крушителя", "boots-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_SWORD, 6199999, "Меч Крушителя", "sword-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_PICKAXE, 4000000, "Кирка Крушителя", "pickaxe-kryshitel"));
        this.register(new SpookyTimeItem(Items.CROSSBOW, 1000000, "Арбалет Крушителя", "crossbow-kryshitel"));
        this.register(new SpookyTimeItem(Items.BOW, 300000, "Лук Крушителя", "bow-kryshitel"));
        this.register(new SpookyTimeItem(Items.TRIDENT, 425000, "Трезубец Крушителя", "trident-kryshitel"));
        this.register(new SpookyTimeItem(Items.ELYTRA, 10000000, "Элитры Крушителя", "elytra-kryshitel"));
        this.register(new SpookyTimeItem(Items.NETHERITE_SWORD, 10000000, "Катана", "sword-katana"));
        this.register(new SpookyTimeItem(Items.ELYTRA, 10000000, "Нерушимые элитры", "elytra-unbreakable"));
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.register(new SpookyTimeItem(Items.NETHERITE_PICKAXE, 7500000, "Молот Тора", "radius-item-mega-buldozer"));
        this.register(new SpookyTimeItem(Items.NETHERITE_PICKAXE, 5000000, "Топ-Кирка", "pickaxe-top-silktouch"));
        this.register(new SpookyTimeItem(Items.GOLDEN_PICKAXE, 25000000, "Божье касание", "spawner-item-spawner-break"));
        this.register(new SpookyTimeItem(Items.GOLDEN_PICKAXE, 2000000, "Мощный удар", "bedrock-item-bedrock-break"));
        this.currentSource = AutoBuyItem.Source.MODIFIER;
        this.register(new SpookyTimeItem(Items.BUCKET, 5000000, "Модификатор очистки", "modifier-item-clear-day"));
        this.register(new SpookyTimeItem(Items.RAW_IRON, 3000000, "Модификатор наковальни", "modifier-item-anvil-day"));
        this.register(new SpookyTimeItem(Items.FEATHER, 2999999, "Модификатор полёта", "modifier-item-fly-day"));
        this.register(new SpookyTimeItem(Items.AMETHYST_SHARD, 1111111, "Модификатор починки", "modifier-item-fix-day"));
        this.register(new SpookyTimeItem(Items.COOKED_BEEF, 1000000, "Модификатор насыщения", "modifier-item-feed-day"));
        this.register(new SpookyTimeItem(Items.SLIME_BALL, 1000000, "Модификатор прыжка", "modifier-item-jump-day"));
        this.register(new SpookyTimeItem(Items.NAME_TAG, 500000, "Модификатор переименования", "modifier-item-name-day"));
        this.register(new SpookyTimeItem(Items.COMPASS, 400000, "Модификатор возврата", "modifier-item-back-day"));
        this.register(new SpookyTimeItem(Items.LADDER, 400000, "Модификатор подъёма", "modifier-item-top-day"));
        this.register(new SpookyTimeItem(Items.GOAT_HORN, 300000, "Модификатор вещания", "modifier-item-adv-day"));
        this.register(new SpookyTimeItem(Items.ENDER_EYE, 270000, "Модификатор эндер-сундука", "modifier-item-ec-day"));
        this.register(new SpookyTimeItem(Items.GOLDEN_APPLE, 199990, "Модификатор исцеления", "modifier-item-heal-day"));
        this.register(new SpookyTimeItem(Items.POPPED_CHORUS_FRUIT, 100000, "Модификатор варпов", "modifier-item-setwarp-day"));
        this.currentSource = AutoBuyItem.Source.SKIN;
        this.register(new SpookyTimeItem(Items.PAPER, 9000000, "Неизбежный скин", "trap-skin-item-inevitable"));
        this.register(new SpookyTimeItem(Items.PAPER, 4500000, "Драконий скин", "trap-skin-item-dragon"));
        this.register(new SpookyTimeItem(Items.PAPER, 1250000, "Мучительный скин", "trap-skin-item-excruciating"));
        this.register(new SpookyTimeItem(Items.PAPER, 500000, "Кошмарный скин", "trap-skin-item-nightmarish"));
        this.register(new SpookyTimeItem(Items.PAPER, 500000, "Сингулярный скин", "trap-skin-item-singular"));
        this.register(new SpookyTimeItem(Items.PAPER, 350000, "Коварный скин", "trap-skin-item-insidious"));
        this.register(new SpookyTimeItem(Items.PAPER, 222222, "Призрачный скин", "trap-skin-item-phantom"));
        this.register(new SpookyTimeItem(Items.PAPER, 166666, "Ледяной скин", "trap-skin-item-icy"));
        this.register(new SpookyTimeItem(Items.PAPER, 166666, "Мистический скин", "trap-skin-item-mystical"));
        this.register(new SpookyTimeItem(Items.PAPER, 130000, "Смертельный скин", "trap-skin-item-deadly"));
        this.register(new SpookyTimeItem(Items.PAPER, 120000, "Проклятый скин", "trap-skin-item-damned"));
        this.register(new SpookyTimeItem(Items.PAPER, 75000, "Жемчужный скин", "trap-skin-item-pearl"));
        this.register(new SpookyTimeItem(Items.PAPER, 50000, "Жестокий скин", "trap-skin-item-cruel"));
        this.register(new SpookyTimeItem(Items.PAPER, 37500, "Зловещий скин", "trap-skin-item-sinister"));
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.register(new SpookyTimeItem(Items.NETHERITE_SCRAP, 974999, "Трапка", "schematic-item-trap"));
        this.register(new SpookyTimeItem(Items.SNOWBALL, 325000, "Снежок заморозка", "effect-item-snowball"));
        this.register(new SpookyTimeItem(Items.DRIED_KELP, 200000, "Пласт", "schematic-item-plast"));
        this.register(new SpookyTimeItem(Items.ENDER_EYE, 105000, "Дезориентация", "effect-item-diz"));
        this.register(new SpookyTimeItem(Items.SUGAR, 106250, "Явная пыль", "effect-item-dust"));
        this.register(new SpookyTimeItem(Items.FIRE_CHARGE, 92632, "Огненный смерч", "effect-item-smerch"));
        this.register(SpookyTimeItem.ofCustomKey(Items.TRIPWIRE_HOOK, 6800000, "Отмычка к Сферам", "spookystash:stash_id", "spheres"));
        this.register(SpookyTimeItem.ofCustomKey(Items.TRIPWIRE_HOOK, 700000, "Отмычка к Инструментам", "spookystash:stash_id", "tools"));
        this.register(SpookyTimeItem.ofCustomKey(Items.TRIPWIRE_HOOK, 125000, "Отмычка к Оружию", "spookystash:stash_id", "weapons"));
        this.register(SpookyTimeItem.ofCustomKey(Items.TRIPWIRE_HOOK, 125000, "Отмычка к Броне", "spookystash:stash_id", "armors"));
        this.register(SpookyTimeItem.ofCustomKey(Items.IRON_NUGGET, 32812, "Серебро", "spookystash:currency", "silver"));
        this.register(SpookyTimeItem.ofLevel(Items.EXPERIENCE_BOTTLE, 500000, "Пузырёк опыта [50]", "exp-bottle", "spookystash:levels", 50));
        this.register(SpookyTimeItem.ofLevel(Items.EXPERIENCE_BOTTLE, 400000, "Пузырёк опыта [30]", "exp-bottle", "spookystash:levels", 30));
        this.register(SpookyTimeItem.ofLevel(Items.EXPERIENCE_BOTTLE, 100000, "Пузырёк опыта [15]", "exp-bottle", "spookystash:levels", 15));
    }

    private SpookyTimeItem addSpookySphere(Item item, int i, String string, String string2) {
        SpookyTimeItem spookyTimeItem = new SpookyTimeItem(item, i, string, string2).withLevel("spookyitems:sphere-item-level", 1);
        this.register(spookyTimeItem);
        return spookyTimeItem;
    }

    private void registerHolyWorldItems() {
        this.currentSource = AutoBuyItem.Source.SPHERE;
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Обычная сфера", true).withSkullTexture("9e95293acbcd4f55faf5947bfc5135038b275a7ab81087341b9ec6e453e839");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Эпическая сфера", true).withSkullTexture("163bcaf6d2679d8d7d9bf6a474a48a77a8e91747a1084c09256ebc86cb74811");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Легендарная сфера", true).withSkullTexture("dc9365642c6eddcfedf5b5e14e2bc71257d9e4a3363d123c6f33c55cafbf6d");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Мифическая сфера", true).withSkullTexture("faff2eb498e5c6a04484f0c9f785b448479ab213df95ec91176a308a12add70");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера ᴇᴛᴇʀɴɪᴛʏ", true).withSkullTexture("dc9365642c6eddcfedf5b5e14e2bc71257d9e4a3363d123c6f33c55cafbf6d");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ", true)
            .withSkullTexture("ea62b9de6a26b86869ca22ea40f1bde80a0430a54547becce8fda8707777258f");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ", true)
            .withSkullTexture("83ed4ce23933e66e04df16070644f7599eeb55307f7eafe8d92f40fb3520863c");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера sᴛɪɴɢᴇʀ", true).withSkullTexture("dc9365642c6eddcfedf5b5e14e2bc71257d9e4a3363d123c6f33c55cafbf6d");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера Флеша", true).withSkullTexture("77400ea19dbd84f75c39ad6823ac4ef786f39f48fc6f84602366ac29b837422");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Сфера Цербера", true).withSkullTexture("b095a7fd90daa1bbe7069089740e05d0bfc66296ee3c40ee71a4e0a6616b2bbc");
        this.addHolyWorldItem(Items.PLAYER_HEAD, "Осколок сферы", true).withSkullTexture("ff7bbce23e8162e42d207055b0ce90f0ed57b015e5212139ac8fc7e6d45ddfca");
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Обычный талисман", true);
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Эпический талисман", true);
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Легендарный талисман", true);
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Талисман ɪɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Талисман ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.TOTEM_OF_UNDYING, "Талисман sᴛɪɴɢᴇʀ", true);
        this.currentSource = AutoBuyItem.Source.CRUSHER;
        this.addHolyWorldItem(Items.NETHERITE_HELMET, "Шлем Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_CHESTPLATE, "Нагрудник Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_LEGGINGS, "Поножи Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_BOOTS, "Ботинки Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_SWORD, "Меч Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.BOW, "Лук Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.CROSSBOW, "Арбалет Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.TRIDENT, "Трезубец Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_PICKAXE, "Кирка Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_SHOVEL, "Лопата Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_AXE, "Топор Iɴғɪɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_HELMET, "Шлем ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_CHESTPLATE, "Нагрудник ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_LEGGINGS, "Штаны ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_BOOTS, "Ботинки ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_SWORD, "Меч ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.BOW, "Лук ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.CROSSBOW, "Арбалет ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.TRIDENT, "Трезубец ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_PICKAXE, "Кирка ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_SHOVEL, "Лопата ᴇᴛᴇʀɴɪᴛʏ", true);
        this.addHolyWorldItem(Items.NETHERITE_AXE, "Топор ᴇᴛᴇʀɴɪᴛʏ", true);
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.addHolyWorldItem(Items.CLAY, "Взрывчатое вещество", true);
        this.addHolyWorldItem(Items.TNT, "Динамит", true);
        this.addHolyWorldItem(Items.TNT, "Динамит A", true);
        this.addHolyWorldItem(Items.TNT, "Динамит B", true);
        this.addHolyWorldItem(Items.TNT, "Динамит Б2", true);
        this.addHolyWorldItem(Items.TNT, "С4 ВзРыВчАтКа", true);
        this.addHolyWorldItem(Items.TNT, "Разрывная волна", true);
        this.addHolyWorldItem(Items.TNT, "Ледяная волна", true);
        this.addHolyWorldItem(Items.TNT, "Стиллер", true);
        this.addHolyWorldItem(Items.TNT, "Надёжный стиллер", true);
        this.addHolyWorldItem(Items.DISPENSER, "Тнт-Пушка", true);
        this.addHolyWorldItem(Items.POPPED_CHORUS_FRUIT, "Трапка", true);
        this.addHolyWorldItem(Items.PRISMARINE_SHARD, "Взрывная трапка", true);
        this.addHolyWorldItem(Items.FIRE_CHARGE, "Взрывная штучка", true);
        this.addHolyWorldItem(Items.SNOWBALL, "Ком снега", true);
        this.addHolyWorldItem(Items.NETHER_STAR, "Стан", true);
        this.addHolyWorldItem(Items.RED_DYE, "«Восстановление»", true);
        this.addHolyWorldItem(Items.ORANGE_DYE, "Руна «Бессмертие»", true);
        this.addHolyWorldItem(Items.PINK_SHULKER_BOX, "Рюкзак (I уровень)", true);
        this.addHolyWorldItem(Items.LIGHT_BLUE_SHULKER_BOX, "Рюкзак (II уровень)", true);
        this.addHolyWorldItem(Items.RED_SHULKER_BOX, "Рюкзак (III уровень)", true);
        this.addHolyWorldItem(Items.MAGENTA_SHULKER_BOX, "Рюкзак (IV уровень)", true);
        this.addHolyWorldItem(Items.LIME_SHULKER_BOX, "Рюкзак Iɴғɪɴɪᴛʏ", true);
        this.currentSource = AutoBuyItem.Source.POTION;
        this.addHolyWorldItem(Items.POTION, "Зелье победителя", true);
        this.addHolyWorldItem(Items.POTION, "Зелье силы", true);
        this.addHolyWorldItem(Items.POTION, "Улучшенное зелье силы", true);
        this.addHolyWorldItem(Items.POTION, "Зелье черепашьей мощи", true);
        this.currentSource = AutoBuyItem.Source.DONATE;
        this.addHolyWorldItem(Items.CONDUIT, "Артефакт", true);
        this.addHolyWorldItem(Items.COMPASS, "Особый компас", true);
        this.addHolyWorldItem(Items.GOLDEN_PICKAXE, "Золотая кирка Джейка", true);
        this.addHolyWorldItem(Items.SPAWNER, "Золотой Спавнер", true);
        this.addHolyWorldItem(Items.ANCIENT_DEBRIS, "Уникальный приват", true);
        this.addHolyWorldItem(Items.EXPERIENCE_BOTTLE, "Бутылек с 15 ур. опыта", false);
        this.addHolyWorldItem(Items.EXPERIENCE_BOTTLE, "Бутылек с 30 ур. опыта", false);
        this.addHolyWorldItem(Items.EXPERIENCE_BOTTLE, "Бутылек с 50 ур. опыта", false);
        this.addHolyWorldItem(Items.EXPERIENCE_BOTTLE, "Бутылек с 100 ур. опыта", false);
    }

    private HolyWorldItem addHolyWorldItem(Item item, String string, boolean flag) {
        HolyWorldItem holyWorldItem = new HolyWorldItem(item, 0, string, flag);
        this.register(holyWorldItem);
        return holyWorldItem;
    }

    private EnchantRequirement enchant(RegistryKey<Enchantment> registryKey, int i) {
        return new VanillaEnchantRequirement(registryKey, i);
    }

    private EnchantRequirement customEnchant(CustomEnchant customEnchant2) {
        return new CustomEnchantRequirement(customEnchant2);
    }

    private EnchantRequirement[] concat(EnchantRequirement[] enchantRequirements, EnchantRequirement... enchantRequirements2) {
        EnchantRequirement[] enchantRequirements3 = new EnchantRequirement[enchantRequirements.length + enchantRequirements2.length];
        System.arraycopy(enchantRequirements, 0, enchantRequirements3, 0, enchantRequirements.length);
        System.arraycopy(enchantRequirements2, 0, enchantRequirements3, enchantRequirements.length, enchantRequirements2.length);
        return enchantRequirements3;
    }

    private EnchantRequirement[] swordCustomEnchants() {
        return new EnchantRequirement[]{
            this.customEnchant(CustomEnchant.EXPERIENCED),
            this.customEnchant(CustomEnchant.VAMPIRISM),
            this.customEnchant(CustomEnchant.OXIDATION),
            this.customEnchant(CustomEnchant.POISON),
            this.customEnchant(CustomEnchant.DETECTION)
        };
    }

    private EnchantRequirement[] armorEnchants() {
        return new EnchantRequirement[]{
            this.enchant(Enchantments.PROTECTION, 5),
            this.enchant(Enchantments.PROJECTILE_PROTECTION, 5),
            this.enchant(Enchantments.BLAST_PROTECTION, 5),
            this.enchant(Enchantments.FIRE_PROTECTION, 5),
            this.enchant(Enchantments.THORNS, 3).excludeByDefault(),
            this.enchant(Enchantments.UNBREAKING, 5),
            this.enchant(Enchantments.MENDING, 1)
        };
    }

    private EnchantRequirement[] helmetEnchants() {
        return this.concat(this.armorEnchants(), this.enchant(Enchantments.RESPIRATION, 3), this.enchant(Enchantments.AQUA_AFFINITY, 1));
    }

    private EnchantRequirement[] bootsEnchants() {
        return this.concat(
            this.armorEnchants(),
            this.enchant(Enchantments.FEATHER_FALLING, 4),
            this.enchant(Enchantments.DEPTH_STRIDER, 3),
            this.enchant(Enchantments.SOUL_SPEED, 3)
        );
    }

    private EnchantRequirement[] swordEnchants() {
        return this.concat(
            new EnchantRequirement[]{
                this.enchant(Enchantments.SHARPNESS, 7),
                this.enchant(Enchantments.SMITE, 7),
                this.enchant(Enchantments.BANE_OF_ARTHROPODS, 7),
                this.enchant(Enchantments.FIRE_ASPECT, 2),
                this.enchant(Enchantments.LOOTING, 5),
                this.enchant(Enchantments.KNOCKBACK, 2),
                this.enchant(Enchantments.SWEEPING_EDGE, 3),
                this.enchant(Enchantments.UNBREAKING, 5),
                this.enchant(Enchantments.MENDING, 1)
            },
            this.swordCustomEnchants()
        );
    }

    private EnchantRequirement[] maceEnchants() {
        return this.concat(this.swordEnchants(), this.enchant(Enchantments.DENSITY, 5), this.enchant(Enchantments.BREACH, 3));
    }

    private EnchantRequirement[] bowEnchants() {
        return new EnchantRequirement[]{
            this.enchant(Enchantments.POWER, 5),
            this.enchant(Enchantments.FLAME, 1),
            this.enchant(Enchantments.INFINITY, 1),
            this.enchant(Enchantments.PUNCH, 2),
            this.enchant(Enchantments.UNBREAKING, 5),
            this.enchant(Enchantments.MENDING, 1),
            this.customEnchant(CustomEnchant.SNIPER),
            this.customEnchant(CustomEnchant.DEMOLITIONIST)
        };
    }

    private EnchantRequirement[] crossbowEnchants() {
        return new EnchantRequirement[]{
            this.enchant(Enchantments.PIERCING, 5),
            this.enchant(Enchantments.QUICK_CHARGE, 3),
            this.enchant(Enchantments.MULTISHOT, 1),
            this.enchant(Enchantments.UNBREAKING, 5),
            this.enchant(Enchantments.MENDING, 1)
        };
    }

    private EnchantRequirement[] pickaxeEnchants() {
        return new EnchantRequirement[]{
            this.enchant(Enchantments.EFFICIENCY, 10),
            this.enchant(Enchantments.FORTUNE, 5),
            this.enchant(Enchantments.UNBREAKING, 5),
            this.enchant(Enchantments.MENDING, 1),
            this.customEnchant(CustomEnchant.BULLDOZER),
            this.customEnchant(CustomEnchant.EXPERIENCED),
            this.customEnchant(CustomEnchant.MAGNET),
            this.customEnchant(CustomEnchant.AUTO_SMELT),
            this.customEnchant(CustomEnchant.COBWEB),
            this.customEnchant(CustomEnchant.PINGER)
        };
    }

    private CustomColorPotionItem addArrow(int i, String string, int i2) {
        CustomColorPotionItem customColorPotionItem = new CustomColorPotionItem(i, string, i2);
        customColorPotionItem.setServer(ServerType.FUNTIME);
        this.register(customColorPotionItem);
        return customColorPotionItem;
    }

    private FunTimeItem addLoreItem(Item item, int i, String string, String... strings) {
        FunTimeItem funTimeItem = new FunTimeItem(item, i, string, strings);
        funTimeItem.setServer(ServerType.FUNTIME);
        this.register(funTimeItem);
        return funTimeItem;
    }

    private FunTimeItem addOriginalItem(Item item, int i, String string, EnchantRequirement... enchantRequirements) {
        FunTimeItem funTimeItem = new FunTimeItem(item, i, string, new String[]{"Оригинальный предмет"}, enchantRequirements);
        funTimeItem.setServer(ServerType.FUNTIME);
        this.register(funTimeItem);
        return funTimeItem;
    }

    private void addCrusherArmor(Item item, int i, String string) {
        this.addOriginalItem(item, i, string)
            .withTrim(CRUSHER_TRIM_PATTERN, CRUSHER_TRIM_MATERIAL)
            .withFixedRequirements(this.concat(this.crusherArmorRequirements(), this.excluded(Enchantments.THORNS)));
        this.addOriginalItem(item, i, string + " (Шипы)")
            .withTrim(CRUSHER_TRIM_PATTERN, CRUSHER_TRIM_MATERIAL)
            .withFixedRequirements(this.concat(this.crusherArmorRequirements(), this.required(Enchantments.THORNS, 3)));
    }

    private EnchantRequirement[] crusherArmorRequirements() {
        return new EnchantRequirement[]{
            this.required(Enchantments.PROTECTION, 5),
            this.required(Enchantments.BLAST_PROTECTION, 5),
            this.required(Enchantments.PROJECTILE_PROTECTION, 5),
            this.required(Enchantments.FIRE_PROTECTION, 5),
            this.required(Enchantments.UNBREAKING, 5),
            this.required(Enchantments.MENDING, 1)
        };
    }

    private EnchantRequirement[] crusherSwordRequirements() {
        return new EnchantRequirement[]{
            this.required(Enchantments.SHARPNESS, 7),
            this.required(Enchantments.SMITE, 7),
            this.required(Enchantments.BANE_OF_ARTHROPODS, 7),
            this.required(Enchantments.SWEEPING_EDGE, 3),
            this.required(Enchantments.FIRE_ASPECT, 2),
            this.required(Enchantments.LOOTING, 5),
            this.required(Enchantments.UNBREAKING, 5),
            this.required(Enchantments.MENDING, 1)
        };
    }

    private EnchantRequirement required(RegistryKey<Enchantment> registryKey, int i) {
        EnchantRequirement enchantRequirement = this.enchant(registryKey, i);
        enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
        return enchantRequirement;
    }

    private EnchantRequirement excluded(RegistryKey<Enchantment> registryKey) {
        EnchantRequirement enchantRequirement = this.enchant(registryKey, 1);
        enchantRequirement.setMode(EnchantRequirement.Mode.EXCLUDE);
        return enchantRequirement;
    }

    private void register(AutoBuyItem autoBuyItem) {
        if (this.getByName(autoBuyItem.getName()) != null) {
            LOGGER.error("Duplicate autobuy entry id: {}", autoBuyItem.getName());
        } else {
            autoBuyItem.setSource(this.currentSource);
            this.items.add(autoBuyItem);
        }
    }

    public void save() {
        try {
            Files.writeString(this.configPath, GSON.toJson(this.toJson()), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.error("Failed to save autobuy items", exception);
        }
    }

    public void load() {
        if (Files.exists(this.configPath)) {
            try {
                this.fromJson(GSON.fromJson(Files.readString(this.configPath, StandardCharsets.UTF_8), JsonObject.class));
            } catch (Exception exception) {
                LOGGER.error("Failed to load autobuy items", exception);
            }
        }
    }

    public String serialize() {
        return GSON.toJson(this.toJson());
    }

    public boolean applyCloudConfig(String string) {
        if (string == null) {
            return false;
        }

        try {
            JsonObject jsonObject = GSON.fromJson(string, JsonObject.class);
            if (jsonObject == null) {
                return false;
            }

            this.fromJson(jsonObject);
            this.save();
            return true;
        } catch (Exception exception) {
            LOGGER.error("Failed to apply cloud autobuy config", exception);
            return false;
        }
    }

    private JsonObject toJson() {
        JsonArray jsonArray = new JsonArray();

        for (AutoBuyItem autoBuyItem : this.items) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", autoBuyItem.getName());
            jsonObject.addProperty("price", autoBuyItem.getPrice());
            jsonObject.addProperty("enabled", autoBuyItem.isEnabled());
            autoBuyItem.save(jsonObject);
            jsonArray.add(jsonObject);
        }

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("stashAnarchy", this.stashAnarchy);
        jsonObject2.add("items", jsonArray);
        return jsonObject2;
    }

    private void fromJson(JsonObject jsonObject) {
        if (jsonObject != null) {
            if (jsonObject.has("stashAnarchy")) {
                this.stashAnarchy = jsonObject.get("stashAnarchy").getAsInt();
            }

            if (jsonObject.has("items")) {
                for (JsonElement jsonElement : jsonObject.getAsJsonArray("items")) {
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    if (jsonObject2.has("id")) {
                        AutoBuyItem autoBuyItem = this.getByName(jsonObject2.get("id").getAsString());
                        if (autoBuyItem != null) {
                            if (jsonObject2.has("price")) {
                                autoBuyItem.setPrice(jsonObject2.get("price").getAsInt());
                            }

                            if (jsonObject2.has("enabled")) {
                                autoBuyItem.setEnabled(jsonObject2.get("enabled").getAsBoolean());
                            }

                            autoBuyItem.load(jsonObject2);
                        }
                    }
                }
            }
        }
    }

    public AutoBuyItem getByName(String string) {
        for (AutoBuyItem autoBuyItem : this.items) {
            if (autoBuyItem.getName().equals(string)) {
                return autoBuyItem;
            }
        }

        return null;
    }

    public AutoBuyManager.Mode getMode() {
        return this.mode;
    }

    public void setMode(AutoBuyManager.Mode mode2) {
        this.mode = mode2;
    }

    public int getStashAnarchy() {
        return this.stashAnarchy;
    }

    public void setStashAnarchy(int i) {
        this.stashAnarchy = i;
    }

    public CloudConfigSync getCloudSync() {
        return this.cloudSync;
    }

    public enum Mode {
        BUY,
        CHECK,
        OFF;
    }
}
