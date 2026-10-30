package tech.sadnes.api.mixin.client;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowIconMixin {
    @Shadow
    @Final
    private long handle;

    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
    private void sadnes$setClientIcon(ResourcePack resourcePack, Icons icons, CallbackInfo callbackInfo) {
        int i = GLFW.glfwGetPlatform();
        if (i == 393217 || i == 393220) {
            ArrayList<NativeImage> nativeImages = new ArrayList<>();

            for (String string : new String[]{"16x16", "32x32", "48x48", "128x128", "256x256"}) {
                try (InputStream inputStream = WindowIconMixin.class.getResourceAsStream("/assets/minecraft/icons/icon_" + string + ".png")) {
                    if (inputStream != null) {
                        nativeImages.add(NativeImage.read(inputStream));
                    }
                } catch (Exception exception) {
                }
            }

            if (!nativeImages.isEmpty()) {
                ArrayList<ByteBuffer> byteBuffers = new ArrayList<>(nativeImages.size());

                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    int i2 = nativeImages.size();
                    Buffer buffer = GLFWImage.malloc(i2, memoryStack);

                    for (int i3 = 0; i3 < i2; i3++) {
                        NativeImage nativeImage = nativeImages.get(i3);
                        ByteBuffer byteBuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
                        byteBuffers.add(byteBuffer);
                        byteBuffer.asIntBuffer().put(nativeImage.copyPixelsAbgr());
                        buffer.position(i3);
                        buffer.width(nativeImage.getWidth());
                        buffer.height(nativeImage.getHeight());
                        buffer.pixels(byteBuffer);
                    }

                    GLFW.glfwSetWindowIcon(this.handle, buffer.position(0));
                    callbackInfo.cancel();
                } finally {
                    byteBuffers.forEach(MemoryUtil::memFree);
                    nativeImages.forEach(NativeImage::close);
                }
            }
        }
    }
}
