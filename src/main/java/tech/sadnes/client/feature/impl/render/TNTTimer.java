package tech.sadnes.client.feature.impl.render;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "TNTTimer",
    description = "feature.impl.render.tnttimer",
    displayName = "feature.impl.render.tnttimer",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class TNTTimer extends Feature {
    private static final float FONT_SIZE = 7.0F;
    private static final float MAX_FUSE_TICKS = 80.0F;

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isHudPost() && mc.player != null && mc.world != null) {
            DrawContext drawContext = renderEvent.getContext();
            float f = renderEvent.getTickDelta();

            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof TntEntity tntEntity) {
                    Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, f);
                    Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y + tntEntity.getHeight() + 0.5, vec3d.z);
                    if (vector2f != null) {
                        int i = Math.max(0, tntEntity.getFuse());
                        String string = String.format(Locale.US, "%.1fs", i / 20.0F);
                        float f2 = MathHelper.clamp(i / 80.0F, 0.0F, 1.0F);
                        int i2 = ColorUtil.interpolate(ColorUtil.rgb(60, 200, 90), ColorUtil.rgb(235, 64, 52), 1.0F - f2);
                        float f3 = Fonts.INTER_MEDIUM.getWidth(string, 7.0F);
                        float f4 = f3 + 8.0F;
                        float f5 = vector2f.x - f4 / 2.0F;
                        float f6 = vector2f.y - 11.0F / 2.0F;
                        GuiRenderUtil.drawPanel(drawContext, f5, f6, f4, 11.0F, 1.0F, 4.0F, false);
                        new RenderUtil(drawContext, f5, f6, f4, 11.0F).textCenter(Fonts.INTER_MEDIUM, string, i2, 7.0F, true);
                    }
                }
            }
        }
    }
}
