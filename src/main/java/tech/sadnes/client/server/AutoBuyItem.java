package tech.sadnes.client.server;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import tech.sadnes.client.server.autobuy.EnchantRequirement;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.Unit;

public class AutoBuyItem {
    private final Item item;
    private int price;
    private boolean enabled = true;
    private AutoBuyItem.Source source = AutoBuyItem.Source.VANILLA;
    private ServerType server;
    private boolean custom;
    private final BooleanSetting autoParse = new BooleanSetting("autobuy.setting.autoparse", true);
    private final NumberSetting discount = new NumberSetting("autobuy.setting.discount", 25.0F, 0.0F, 100.0F, 1.0F, Unit.PERCENT)
        .setVisibleWhen(this.autoParse::getValue);
    private ItemStack displayStack;
    private ItemStack matchedStack;
    private String skullTexture;
    private static final Set<String> IGNORED_TAGS = Set.of("minecraft:s", "auctions:if-uuid", "openshulker:shulker");
    private static final String BUKKIT_VALUES_KEY = "PublicBukkitValues";

    public boolean isAvailableOn(ServerType serverType) {
        return this.server == null || this.server == serverType;
    }

    public AutoBuyItem(Item item2, int i) {
        this.item = item2;
        this.price = i;
    }

    public String getName() {
        return Registries.ITEM.getId(this.item).toString();
    }

    public String getId() {
        return this.item.getName().getString();
    }

    public int getMaxCount() {
        return this.source == AutoBuyItem.Source.SPHERE ? 1 : this.item.getMaxCount();
    }

    public String getCategoryName() {
        return this.getId();
    }

    public ItemStack getDisplayStack() {
        if (this.matchedStack != null) {
            return this.matchedStack;
        }

        if (this.displayStack == null || this.isStrictMatch()) {
            this.displayStack = this.createStack();
        }

        return this.displayStack;
    }

    public AutoBuyItem withSkullTexture(String string) {
        this.skullTexture = string;
        return this;
    }

    public AutoBuyItem markCustom() {
        this.custom = true;
        return this;
    }

    public ItemStack createStack() {
        ItemStack itemStack = new ItemStack(this.item);
        if (this.skullTexture != null) {
            itemStack.set(DataComponentTypes.PROFILE, createSkullProfile(this.skullTexture));
        }

        return itemStack;
    }

    private static ProfileComponent createSkullProfile(String string) {
        String string2 = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + string + "\"}}}";
        Property property = new Property("textures", Base64.getEncoder().encodeToString(string2.getBytes(StandardCharsets.UTF_8)));
        PropertyMap propertyMap = new PropertyMap(ImmutableMultimap.of(property.name(), property));
        UUID uuid = UUID.nameUUIDFromBytes(string.getBytes(StandardCharsets.UTF_8));
        return ProfileComponent.ofStatic(new GameProfile(uuid, "", propertyMap));
    }

    protected boolean isStrictMatch() {
        return false;
    }

    public boolean tryMatch(ItemStack itemStack) {
        if (this.matchedStack == null && itemStack != null && !itemStack.isEmpty()) {
            ItemStack itemStack2 = itemStack.copy();
            itemStack2.setCount(1);
            itemStack2.remove(DataComponentTypes.LORE);
            itemStack2.remove(DataComponentTypes.CUSTOM_DATA);
            this.matchedStack = itemStack2;
            return true;
        } else {
            return false;
        }
    }

    public final boolean isCustom() {
        return this.getClass() != AutoBuyItem.class;
    }

    public boolean matches(ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty() || !this.matchesItem(itemStack) ? false : !this.custom || hasBukkitValues(itemStack);
    }

    public boolean matchesItem(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.isOf(this.item)) {
                return true;
            }

            if (this.item == Items.SHULKER_BOX) {
                return itemStack.isIn(ItemTags.SHULKER_BOXES);
            }

            if (this.item == Items.BUNDLE) {
                return itemStack.isIn(ItemTags.BUNDLES);
            }
        }

        return false;
    }

    private static boolean hasBukkitValues(ItemStack itemStack) {
        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            return true;
        }

        NbtCompound nbtCompound = nbtComponent.copyNbt();

        for (String string : nbtCompound.getKeys()) {
            if (!"PublicBukkitValues".equals(string)) {
                return false;
            }

            for (String string2 : nbtCompound.getCompoundOrEmpty(string).getKeys()) {
                if (!IGNORED_TAGS.contains(string2)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean matchesComponents(ItemStack itemStack) {
        if (this.matches(itemStack)) {
            return true;
        }

        ContainerComponent containerComponent = itemStack == null ? null : itemStack.get(DataComponentTypes.CONTAINER);
        if (containerComponent == null) {
            return false;
        }

        for (ItemStack itemStack2 : containerComponent.iterateNonEmpty()) {
            if (this.matches(itemStack2)) {
                return true;
            }
        }

        return false;
    }

    public List<EnchantRequirement> getVariants() {
        return List.of();
    }

    public NumberSetting getDiscountSetting() {
        return null;
    }

    public List<Setting<?>> getSettings() {
        return List.of(this.autoParse, this.discount);
    }

    public float getDiscountedPrice() {
        return Math.max(0.0F, 1.0F - this.discount.getValue() / 100.0F);
    }

    public void save(JsonObject jsonObject) {
        jsonObject.addProperty("autoParse", this.autoParse.getValue());
        jsonObject.addProperty("discount", this.discount.getValue());
    }

    public void load(JsonObject jsonObject) {
        if (jsonObject.has("autoParse")) {
            this.autoParse.setValue(jsonObject.get("autoParse").getAsBoolean());
        }

        if (jsonObject.has("discount")) {
            this.discount.setValue(Float.valueOf(jsonObject.get("discount").getAsFloat()));
        }
    }

    public Item getItem() {
        return this.item;
    }

    public int getPrice() {
        return this.price;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public AutoBuyItem.Source getSource() {
        return this.source;
    }

    public ServerType getServer() {
        return this.server;
    }

    public BooleanSetting getAutoParse() {
        return this.autoParse;
    }

    public NumberSetting getDiscount() {
        return this.discount;
    }

    public void setPrice(int i) {
        this.price = i;
    }

    public void setEnabled(boolean flag) {
        this.enabled = flag;
    }

    public void setSource(AutoBuyItem.Source source2) {
        this.source = source2;
    }

    public void setServer(ServerType serverType) {
        this.server = serverType;
    }

    public void setDisplayStack(ItemStack itemStack) {
        this.displayStack = itemStack;
    }

    public void setMatchedStack(ItemStack itemStack) {
        this.matchedStack = itemStack;
    }

    public enum Source {
        CRUSHER,
        DONATE,
        SPHERE,
        POTION,
        MODIFIER,
        SKIN,
        GEAR,
        VANILLA;
    }
}
