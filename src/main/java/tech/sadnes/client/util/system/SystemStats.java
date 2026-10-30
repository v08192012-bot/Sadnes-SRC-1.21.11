package tech.sadnes.client.util.system;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import tech.sadnes.client.deobf.ExactMath;

public final class SystemStats {
    private static long prevIdleTime;
    private static long prevKernelTime;
    private static long prevUserTime;
    private static double baseCpuFrequencyGhz = -1.0;
    private static Pointer pdhQuery;
    private static Pointer pdhPerformanceCounter;
    private static boolean pdhInitialized = false;
    private static Boolean nvmlAvailable;
    private static Pointer nvmlDevice;

    private SystemStats() {
    }

    public static double getCpuUsage() {
        try {
            SystemStats.FileTime fileTime = new SystemStats.FileTime();
            SystemStats.FileTime fileTime2 = new SystemStats.FileTime();
            SystemStats.FileTime fileTime3 = new SystemStats.FileTime();
            if (SystemStats.Kernel32.INSTANCE.GetSystemTimes(fileTime, fileTime2, fileTime3) == 0) {
                return -1.0;
            }

            long l = fileTime.toLong();
            long l2 = fileTime2.toLong();
            long l3 = fileTime3.toLong();
            long l4 = l - prevIdleTime;
            long l5 = l2 - prevKernelTime;
            long l6 = l3 - prevUserTime;
            prevIdleTime = l;
            prevKernelTime = l2;
            prevUserTime = l3;
            long l7 = l5 + l6;
            if (l7 <= 0L) {
                return 0.0;
            }

            double d = (double)(l7 - l4) / l7;
            return Math.max(0.0, Math.min(1.0, d));
        } catch (Throwable throwable) {
            return -1.0;
        }
    }

    private static void initCpuFrequency() {
        try {
            int processors = Runtime.getRuntime().availableProcessors();
            int bufferSize = processors * 24;
            Memory buffer = new Memory(bufferSize);
            if (SystemStats.PowrProf.INSTANCE.CallNtPowerInformation(11, null, 0, buffer, bufferSize) == 0) {
                double totalMhz = 0.0;
                for (int i = 0; i < processors; i++) {
                    totalMhz = ExactMath.add(buffer.getInt((long)i * 24L + 4L), totalMhz);
                }
                baseCpuFrequencyGhz = ExactMath.divide(ExactMath.divide(totalMhz, processors), 1000.0);
            }

            PointerByReference queryRef = new PointerByReference();
            if (SystemStats.Pdh.INSTANCE.PdhOpenQueryW(null, null, queryRef) != 0) {
                return;
            }

            pdhQuery = queryRef.getValue();
            PointerByReference counterRef = new PointerByReference();
            WString counterPath = new WString("\\Processor Information(_Total)\\% Processor Performance");
            if (SystemStats.Pdh.INSTANCE.PdhAddEnglishCounterW(pdhQuery, counterPath, null, counterRef) != 0) {
                return;
            }

            pdhPerformanceCounter = counterRef.getValue();
            pdhInitialized = true;
        } catch (Throwable ignored) {
        }
    }

    public static double getCpuFrequencyGhz() {
        if (!pdhInitialized && baseCpuFrequencyGhz < 0.0) {
            initCpuFrequency();
        }

        if (pdhInitialized && baseCpuFrequencyGhz > 0.0) {
            try {
                if (SystemStats.Pdh.INSTANCE.PdhCollectQueryData(pdhQuery) == 0) {
                    Memory memory = new Memory(24L);
                    if (SystemStats.Pdh.INSTANCE.PdhGetFormattedCounterValue(pdhPerformanceCounter, 512, null, memory) == 0) {
                        return baseCpuFrequencyGhz * (memory.getDouble(8L) / 100.0);
                    }
                }
            } catch (Throwable throwable) {
            }
        }

        return baseCpuFrequencyGhz;
    }

    public static long[] getMemoryTotalAndUsed() {
        try {
            SystemStats.MemoryStatusEx memoryStatusEx = new SystemStats.MemoryStatusEx();
            memoryStatusEx.dwLength = memoryStatusEx.size();
            return SystemStats.Kernel32.INSTANCE.GlobalMemoryStatusEx(memoryStatusEx) == 0 ? null : new long[]{memoryStatusEx.ullTotalPhys, memoryStatusEx.ullTotalPhys - memoryStatusEx.ullAvailPhys};
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static boolean initNvml() {
        Boolean available = nvmlAvailable;
        if (available != null) {
            return available;
        }

        try {
            if (SystemStats.Nvml.INSTANCE.nvmlInit_v2() != 0) {
                nvmlAvailable = false;
                return false;
            }

            PointerByReference deviceRef = new PointerByReference();
            if (SystemStats.Nvml.INSTANCE.nvmlDeviceGetHandleByIndex_v2(0, deviceRef) != 0) {
                nvmlAvailable = false;
                return false;
            }

            nvmlDevice = deviceRef.getValue();
            nvmlAvailable = true;
            return true;
        } catch (Throwable ignored) {
            nvmlAvailable = false;
            return false;
        }
    }

    public static double getGpuUsage() {
        if (!initNvml()) {
            return -1.0;
        }

        try {
            SystemStats.Utilization utilization = new SystemStats.Utilization();
            return SystemStats.Nvml.INSTANCE.nvmlDeviceGetUtilizationRates(nvmlDevice, utilization) != 0 ? -1.0 : utilization.gpu;
        } catch (Throwable throwable) {
            return -1.0;
        }
    }

    @FieldOrder({"low", "high"})
    public static class FileTime extends Structure {
        public int low;
        public int high;

        long toLong() {
            return (long)this.high << 32 | this.low & 4294967295L;
        }
    }

    interface Kernel32 extends StdCallLibrary {
        SystemStats.Kernel32 INSTANCE = Native.load("kernel32", SystemStats.Kernel32.class);

        int GetSystemTimes(SystemStats.FileTime fileTime, SystemStats.FileTime fileTime2, SystemStats.FileTime fileTime3);

        int GlobalMemoryStatusEx(SystemStats.MemoryStatusEx memoryStatusEx);
    }

    @FieldOrder(
        {
                "dwLength",
                "dwMemoryLoad",
                "ullTotalPhys",
                "ullAvailPhys",
                "ullTotalPageFile",
                "ullAvailPageFile",
                "ullTotalVirtual",
                "ullAvailVirtual",
                "ullAvailExtendedVirtual"
        }
    )
    public static class MemoryStatusEx extends Structure {
        public int dwLength;
        public int dwMemoryLoad;
        public long ullTotalPhys;
        public long ullAvailPhys;
        public long ullTotalPageFile;
        public long ullAvailPageFile;
        public long ullTotalVirtual;
        public long ullAvailVirtual;
        public long ullAvailExtendedVirtual;
    }

    interface Nvml extends Library {
        SystemStats.Nvml INSTANCE = Native.load("nvml", SystemStats.Nvml.class);

        int nvmlInit_v2();

        int nvmlDeviceGetHandleByIndex_v2(int i, PointerByReference pointerByReference);

        int nvmlDeviceGetUtilizationRates(Pointer pointer, SystemStats.Utilization utilization);
    }

    interface Pdh extends StdCallLibrary {
        SystemStats.Pdh INSTANCE = Native.load("Pdh", SystemStats.Pdh.class);

        int PdhOpenQueryW(String string, Pointer pointer, PointerByReference pointerByReference);

        int PdhAddEnglishCounterW(Pointer pointer, WString wString, Pointer pointer2, PointerByReference pointerByReference);

        int PdhCollectQueryData(Pointer pointer);

        int PdhGetFormattedCounterValue(Pointer pointer, int i, Pointer pointer2, Pointer pointer3);
    }

    interface PowrProf extends StdCallLibrary {
        SystemStats.PowrProf INSTANCE = Native.load("PowrProf", SystemStats.PowrProf.class);

        int CallNtPowerInformation(int i, Pointer pointer, int i2, Pointer pointer2, int i3);
    }

    @FieldOrder({"gpu", "memory"})
    public static class Utilization extends Structure {
        public int gpu;
        public int memory;
    }
}
