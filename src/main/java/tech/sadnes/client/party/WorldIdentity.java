package tech.sadnes.client.party;

import tech.sadnes.api.mixin.accessor.BiomeAccessAccessor;
import tech.sadnes.client.util.IMinecraft;

public final class WorldIdentity implements IMinecraft {
    private static final char SEPARATOR = '|';

    private WorldIdentity() {
    }

    public static String currentWorldId() {
        if (mc.world == null) {
            return "";
        }

        String string = mc.world.getRegistryKey().getValue().toString();
        long l = ((BiomeAccessAccessor)mc.world.getBiomeAccess()).sadnes$getSeed();
        return l == 0L ? string : string + "|" + l;
    }

    public static boolean sameWorld(String string, String string2) {
        if (string2 == null || string2.isEmpty() || string == null || string.isEmpty()) {
            return false;
        } else {
            return string.equals(string2) ? true : stripSeed(string).equals(stripSeed(string2)) && (!hasSeed(string) || !hasSeed(string2));
        }
    }

    private static String stripSeed(String string) {
        int i = string.indexOf(124);
        return i < 0 ? string : string.substring(0, i);
    }

    private static boolean hasSeed(String string) {
        return string.indexOf(124) >= 0;
    }
}
