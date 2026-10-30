package tech.sadnes.client.server.donut;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import tech.sadnes.client.util.IMinecraft;

public enum DonutRegion implements IMinecraft {
    EU_CENTRAL("eu central"),
    EU_WEST("eu west"),
    EAST("east"),
    WEST("west"),
    ASIA("asia"),
    OCEANIA("oceania");

    private static final DonutRegion[] VALUES = values();
    private static final String[] REGION_GRID = new String[]{
        "533322222", "533322222", "533322222", "555322222", "444422222", "411000002", "411000002", "010000000", "011111100"
    };
    private final String displayName;

    public static DonutRegion getCurrent() {
        return getAt(mc.player.getX(), mc.player.getZ());
    }

    public static DonutRegion getAt(double d, double d2) {
        double d3 = mc.world.getRegistryKey() == World.NETHER ? 8.0 : 1.0;
        int i = toGridIndex(d2 * d3);
        int i2 = toGridIndex(d * d3);
        return VALUES[REGION_GRID[i].charAt(i2) - '0'];
    }

    private static int toGridIndex(double d) {
        return MathHelper.clamp(MathHelper.floor((d + 225000.0) / 50000.0), 0, 8);
    }

    public String getDisplayName() {
        return this.displayName;
    }

    DonutRegion(String string) {
        this.displayName = string;
    }
}
