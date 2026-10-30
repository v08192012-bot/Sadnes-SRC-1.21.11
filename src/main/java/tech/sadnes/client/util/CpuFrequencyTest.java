package tech.sadnes.client.util;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CpuFrequencyTest {
    public static void main(String[] strings) throws Exception {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor centralProcessor = systemInfo.getHardware().getProcessor();

        for (int i = 0; i < 5; i++) {
            long[] longs = centralProcessor.getCurrentFreq();
            centralProcessor.getMaxFreq();
            double d = 0.0;

            for (long l : longs) {
                d += l;
            }

            double d2 = d / longs.length;
            System.out.println("Avg Freq: " + d2 / 1.0E9 + " GHz");
            Thread.sleep(1000L);
        }
    }
}
