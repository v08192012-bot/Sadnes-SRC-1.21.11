package tech.sadnes.client.util;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import org.lwjgl.glfw.GLFWNativeWin32;

public class DarkTitleBar {
    public static void enableDarkTitleBar(long l) {
        long l2 = GLFWNativeWin32.glfwGetWin32Window(l);
        HWND hwnd = new HWND(new Pointer(l2));
        Memory memory = new Memory(4L);
        memory.setInt(0L, 1);
        DarkTitleBar.DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, 20, memory, 4);
    }

    public interface DwmApi extends StdCallLibrary {
        DarkTitleBar.DwmApi INSTANCE = Native.loadLibrary("dwmapi", DarkTitleBar.DwmApi.class);

        int DwmSetWindowAttribute(HWND hwnd, int i, Pointer pointer, int i2);
    }
}
