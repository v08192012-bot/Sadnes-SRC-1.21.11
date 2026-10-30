package tech.sadnes.client.server.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.ServerType;

public class SpookyTimeItem extends AutoBuyItem {
    private static final String BUKKIT_VALUES_KEY = "PublicBukkitValues";
    private static final String SPOOKY_ITEM_KEY = "spookyitems:spooky-item";
    private static final String LEGACY_SPOOKY_ITEM_KEY = "spooky-item";
    private final String displayId;
    private final NbtCompound bukkitValuesTag = new NbtCompound();
    private final NbtCompound legacyTag = new NbtCompound();
    private String levelKey;
    private int level;
    private String entryName;

    public SpookyTimeItem(Item item, int i, String string, String string2) {
        super(item, i);
        this.displayId = string;
        this.entryName = "spooky:" + string2;
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString("spookyitems:spooky-item", string2);
        this.bukkitValuesTag.put("PublicBukkitValues", nbtCompound);
        this.legacyTag.putString("spooky-item", string2);
        this.setServer(ServerType.SPOOKYTIME);
    }

    public static SpookyTimeItem ofCustomKey(Item item, int i, String string, String string2, String string3) {
        SpookyTimeItem spookyTimeItem = new SpookyTimeItem(item, i, string, string3);
        spookyTimeItem.legacyTag.remove("spooky-item");
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString(string2, string3);
        spookyTimeItem.bukkitValuesTag.put("PublicBukkitValues", nbtCompound);
        return spookyTimeItem;
    }

    public static SpookyTimeItem ofLevel(Item item, int i, String string, String string2, String string3, int i2) {
        SpookyTimeItem spookyTimeItem = new SpookyTimeItem(item, i, string, string2);
        spookyTimeItem.legacyTag.remove("spooky-item");
        spookyTimeItem.bukkitValuesTag.put("PublicBukkitValues", new NbtCompound());
        return spookyTimeItem.withLevel(string3, i2);
    }

    public SpookyTimeItem withLevel(String string, int i) {
        this.levelKey = string;
        this.level = i;
        this.entryName = this.entryName + "#" + i;
        return this;
    }

    @Override
    public String getName() {
        return this.entryName;
    }

    @Override
    public String getId() {
        return this.displayId;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (!super.matches(itemStack)) {
            return false;
        } else {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent == null) {
                return false;
            } else {
                return nbtComponent.matches(this.bukkitValuesTag) || !this.legacyTag.isEmpty() && nbtComponent.matches(this.legacyTag)
                    ? this.levelKey == null || nbtComponent.copyNbt().getCompoundOrEmpty("PublicBukkitValues").getInt(this.levelKey, 0) == this.level
                    : false;
            }
        }
    }
}
