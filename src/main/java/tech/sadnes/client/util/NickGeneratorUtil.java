package tech.sadnes.client.util;

import com.namegen.NickGenerator;
import java.util.concurrent.ThreadLocalRandom;

public final class NickGeneratorUtil {
    private static volatile NickGenerator generator;
    private static volatile boolean loadFailed;

    private NickGeneratorUtil() {
    }

    private static NickGenerator getGenerator() {
        NickGenerator nickGenerator = generator;
        if (nickGenerator == null && !loadFailed) {
            synchronized (NickGeneratorUtil.class) {
                nickGenerator = generator;
                if (nickGenerator == null && !loadFailed) {
                    try {
                        generator = nickGenerator = NickGenerator.fromBundledModel().temperature(0.9).topP(0.92).lengthRange(4, 14);
                    } catch (Throwable throwable) {
                        loadFailed = true;
                    }
                }
            }
        }

        return nickGenerator;
    }

    public static String generate() {
        NickGenerator nickGenerator = getGenerator();
        if (nickGenerator != null) {
            try {
                synchronized (nickGenerator) {
                    return nickGenerator.generate();
                }
            } catch (Throwable throwable) {
            }
        }

        return generateFallback();
    }

    private static String generateFallback() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        String[] strings = new String[]{"neo", "dark", "pro", "mr", "x", "shadow", "frost", "void"};
        String[] strings2 = new String[]{"gamer", "wolf", "fox", "king", "ghost", "raven", "storm", "blade"};
        return strings[threadLocalRandom.nextInt(strings.length)] + strings2[threadLocalRandom.nextInt(strings2.length)] + (100 + threadLocalRandom.nextInt(900));
    }
}
