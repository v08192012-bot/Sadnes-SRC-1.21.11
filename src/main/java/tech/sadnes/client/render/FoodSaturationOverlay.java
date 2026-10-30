package tech.sadnes.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class FoodSaturationOverlay {
    private static final boolean APPLESKIN_LOADED = FabricLoader.getInstance().isModLoaded("appleskin");
    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");

    private FoodSaturationOverlay() {
    }

    public static boolean shouldRender(PlayerEntity playerEntity) {
        return !APPLESKIN_LOADED && playerEntity.getHungerManager().getSaturationLevel() >= 0.5F;
    }

    public static void render(DrawContext drawContext, PlayerEntity playerEntity, int i, int i2) {
        float f = playerEntity.getHungerManager().getSaturationLevel();
        int i3 = i - 10;

        for (int i4 = 0; i4 < 10; i4++) {
            int i5 = i2 - i4 * 8 - 9;
            float f2 = f - i4 * 2;
            if (!(f2 < 0.5F)) {
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, i5, i3, 9, 9);
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, f2 >= 1.5F ? FOOD_FULL : FOOD_HALF, i5, i3, 9, 9);
            }
        }
    }
}
