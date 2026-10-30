package tech.sadnes.client.server.autobuy;

import net.minecraft.item.ItemStack;

public abstract class EnchantRequirement {
    private final int maxLevel;
    private EnchantRequirement.Mode mode = EnchantRequirement.Mode.ANY;
    private EnchantRequirement.Mode defaultMode = EnchantRequirement.Mode.ANY;
    private int minLevel;

    protected EnchantRequirement(int i) {
        this.maxLevel = i;
        this.minLevel = i;
    }

    public EnchantRequirement excludeByDefault() {
        this.defaultMode = EnchantRequirement.Mode.EXCLUDE;
        this.mode = EnchantRequirement.Mode.EXCLUDE;
        return this;
    }

    public void resetMode() {
        this.mode = this.defaultMode;
    }

    public void setMinLevel(int i) {
        this.minLevel = Math.max(1, Math.min(this.maxLevel, i));
    }

    public boolean isRequired() {
        return this.mode == EnchantRequirement.Mode.REQUIRE;
    }

    public boolean isExcluded() {
        return this.mode == EnchantRequirement.Mode.EXCLUDE;
    }

    public abstract String getKey();

    public abstract String getDisplayName();

    protected abstract int getLevel(ItemStack itemStack);

    public boolean test(ItemStack itemStack) {
        switch (this.mode) {
            case ANY:
                return true;
            case REQUIRE:
                if (this.getLevel(itemStack) >= this.minLevel) {
                    return true;
                }
                break;
            case EXCLUDE:
                if (this.getLevel(itemStack) == 0) {
                    return true;
                }
                break;
            default:
                throw new MatchException(null, null);
        }

        return false;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public EnchantRequirement.Mode getMode() {
        return this.mode;
    }

    public EnchantRequirement.Mode getDefaultMode() {
        return this.defaultMode;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public void setMode(EnchantRequirement.Mode mode2) {
        this.mode = mode2;
    }

    public enum Mode {
        ANY,
        REQUIRE,
        EXCLUDE;
    }
}
