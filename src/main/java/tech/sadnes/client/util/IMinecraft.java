package tech.sadnes.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    Window window = mc.getWindow();
}
