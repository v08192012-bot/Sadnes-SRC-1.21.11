package tech.sadnes.client.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class FunTimeAnarchies {
    private static final int[][] ANARCHY_RANGES = new int[][]{{101, 114}, {201, 236}, {301, 325}, {501, 516}, {901, 904}};
    private static final long FULL_COOLDOWN_MS = 300000L;
    private static final Map<Integer, Long> fullUntil = new ConcurrentHashMap<>();
    private static final Pattern SERVER_FULL_PATTERN = Pattern.compile("кикнут.*подключени.*заполнен", 34);

    private FunTimeAnarchies() {
    }

    public static List<int[]> getRanges() {
        ArrayList arrayList = new ArrayList(ANARCHY_RANGES.length);

        for (int[] ints : ANARCHY_RANGES) {
            arrayList.add(new int[]{ints[0], ints[1]});
        }

        return arrayList;
    }

    public static List<Integer> getAllAnarchies() {
        ArrayList arrayList = new ArrayList();

        for (int[] ints : ANARCHY_RANGES) {
            for (int i = ints[0]; i <= ints[1]; i++) {
                arrayList.add(i);
            }
        }

        return arrayList;
    }

    public static boolean isServerFullMessage(String string) {
        return string != null && SERVER_FULL_PATTERN.matcher(string).find();
    }

    public static void markFull(int i) {
        if (i > 0) {
            fullUntil.put(i, System.currentTimeMillis() + 300000L);
        }
    }

    public static boolean isOnCooldown(int i) {
        Long longValue = fullUntil.get(i);
        return longValue != null && longValue > System.currentTimeMillis();
    }

    public static int pickRandomAnarchy(int i) {
        long l = System.currentTimeMillis();
        fullUntil.entrySet().removeIf(entry -> entry.getValue() <= l);
        ArrayList arrayList = new ArrayList();

        for (int[] ints : ANARCHY_RANGES) {
            for (int i2 = ints[0]; i2 <= ints[1]; i2++) {
                if (i2 != i && !fullUntil.containsKey(i2)) {
                    arrayList.add(i2);
                }
            }
        }

        return arrayList.isEmpty() ? -1 : (Integer)arrayList.get(ThreadLocalRandom.current().nextInt(arrayList.size()));
    }
}
