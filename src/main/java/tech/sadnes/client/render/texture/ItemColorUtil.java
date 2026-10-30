package tech.sadnes.client.render.texture;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import tech.sadnes.api.mixin.accessor.SpriteContentsAccessor;
import tech.sadnes.client.util.color.ColorUtil;

public final class ItemColorUtil {
    private static final Map<Item, Integer> CACHE = new HashMap<>();
    private static final ItemRenderState RENDER_STATE = new ItemRenderState();
    private static final int MIN_ALPHA = 16;

    private ItemColorUtil() {
    }

    public static int getAverageColor(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            Item item = itemStack.getItem();
            Integer integer = CACHE.get(item);
            if (integer != null) {
                return integer;
            }

            Integer integer2 = computeAverageColor(itemStack);
            if (integer2 == null) {
                return ColorUtil.WHITE;
            }

            CACHE.put(item, integer2);
            return integer2;
        } else {
            return ColorUtil.WHITE;
        }
    }

    private static Integer computeAverageColor(ItemStack itemStack) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null) {
            return null;
        }

        minecraftClient.getItemModelManager().clearAndUpdate(RENDER_STATE, itemStack, ItemDisplayContext.GUI, minecraftClient.world, null, 0);
        Sprite sprite = RENDER_STATE.getParticleSprite(Random.create());
        if (sprite == null) {
            return null;
        }

        NativeImage nativeImage = ((SpriteContentsAccessor)sprite.getContents()).sadnes$getImage();
        if (nativeImage == null) {
            return null;
        }

        long l = 0L;
        long l2 = 0L;
        long l3 = 0L;
        long l4 = 0L;
        int i = nativeImage.getWidth();
        int i2 = nativeImage.getHeight();

        for (int i3 = 0; i3 < i2; i3++) {
            for (int i4 = 0; i4 < i; i4++) {
                int i5 = nativeImage.getColorArgb(i4, i3);
                if (ColorUtil.alpha(i5) >= 16) {
                    l += ColorUtil.red(i5);
                    l2 += ColorUtil.green(i5);
                    l3 += ColorUtil.blue(i5);
                    l4++;
                }
            }
        }

        return l4 == 0L ? null : ColorUtil.rgb((int)(l / l4), (int)(l2 / l4), (int)(l3 / l4));
    }
}
