package tech.sadnes.client.feature.impl.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.JumpEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.shader.EntityWarpRenderer;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.setting.curve.BezierCurve;
import tech.sadnes.client.setting.curve.CurvePreset;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "JumpCircle",
    description = "feature.impl.render.jumpcircle",
    displayName = "feature.impl.render.jumpcircle",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class JumpCircle extends Feature {
    private final NumberSetting radius = new NumberSetting("jumpcircle.radius", 1.4F, 0.3F, 3.0F, 0.1F);
    private final NumberSetting lifetime = new NumberSetting("jumpcircle.lifetime", 0.8F, 0.2F, 15.0F, 0.1F, Unit.SECOND);
    private final NumberSetting width = new NumberSetting("jumpcircle.width", 2.0F, 1.0F, 10.0F, 0.1F);
    private final ModeSetting colorMode = new ModeSetting("jumpcircle.colorMode", new String[]{"client", "custom", "rainbow"});
    private final ColorSetting customColor = new ColorSetting("jumpcircle.customColor", -1).setVisibleWhen(() -> this.colorMode.is("custom"));
    private final ModeSetting animation = new ModeSetting("jumpcircle.animation", new String[]{"none", "draw", "pulse", "burst", "ripple", "squash"});
    private final BooleanSetting physics = new BooleanSetting("jumpcircle.physics", true);
    private final NumberSetting gravity = new NumberSetting("jumpcircle.gravity", 100.0F, 0.0F, 500.0F, 5.0F, Unit.PERCENT);
    private final NumberSetting stiffness = new NumberSetting("jumpcircle.stiffness", 100.0F, 0.0F, 300.0F, 5.0F, Unit.PERCENT);
    private final List<JumpCircleRing> circles = new ArrayList<>();
    private final int[] segmentColors = new int[80];
    private final float[] normalX = new float[80];
    private final float[] normalZ = new float[80];
    private static final int MAX_CIRCLES = 12;
    private static final float PHYSICS_LIFT = 0.25F;
    private static final int SEGMENTS = 80;
    private static final float[] CIRCLE_SHAPE = createCircleShape(80);
    private static final float HEIGHT_OFFSET = 0.05F;
    private static final float WIDTH_SCALE = 0.06F;
    private static final float WARP_SCALE = 2.0F;
    private static final float WARP_MIX = 1.0F;
    private static final int WARP_CURVE_COLOR = encodeCurveColor(CurvePreset.MAGNET.createCurve());

    public static JumpCircle getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(JumpCircle.class);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.circles.clear();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.circles.clear();
    }

    @EventHandler
    public void onJump(JumpEvent jumpEvent) {
        if (mc.player != null) {
            if (this.circles.size() >= 12) {
                this.circles.remove(0);
            }

            float f = this.physics.getValue() ? 0.25F : 0.0F;
            this.circles
                .add(new JumpCircleRing(CIRCLE_SHAPE, mc.player.getX(), mc.player.getY(), mc.player.getZ(), this.radius.getValue(), mc.player.getYaw(), f));
        }
    }

    private List<Vec3d> applyAnimation(List<Vec3d> vec3ds, Vec3d vec3d, float f) {
        boolean flag = this.animation.is("burst");
        boolean flag2 = this.animation.is("ripple");
        boolean flag3 = this.animation.is("squash");
        if (!flag && !flag2 && !flag3) {
            return vec3ds;
        }

        float f2 = 1.0F;
        if (flag) {
            float f3 = Math.min(1.0F, f * 6.0F);
            f2 = f3 >= 1.0F ? 1.0F : 1.0F - (float)(Math.pow(2.0, -9.0 * f3) * Math.cos(f3 * 22.0));
        }

        float f4 = flag3 ? MathHelper.sin(f * (float) (Math.PI * 4)) * 0.18F * (1.0F - f * 0.5F) : 0.0F;
        float f5 = 1.0F - f * 0.5F;
        int i = vec3ds.size();
        ArrayList arrayList = new ArrayList(i);

        for (int i2 = 0; i2 < i; i2++) {
            Vec3d vec3d2 = (Vec3d)vec3ds.get(i2);
            double d = vec3d2.x - vec3d.x;
            double d2 = vec3d2.z - vec3d.z;
            double d3 = vec3d.x + d * f2 * (1.0F + f4);
            double d4 = vec3d.z + d2 * f2 * (1.0F - f4);
            double d5 = vec3d2.y;
            if (flag2) {
                float f6 = (float)i2 / i;
                d5 += MathHelper.sin((f6 * 4.0F - f * 2.0F) * (float) (Math.PI * 2)) * 0.12F * f5;
            }

            arrayList.add(new Vec3d(d3, d5, d4));
        }

        return arrayList;
    }

    private static float[] createCircleShape(int i) {
        float[] floats = new float[i * 2];

        for (int i2 = 0; i2 < i; i2++) {
            double d = (Math.PI * 2) * i2 / i;
            floats[i2 * 2] = (float)Math.cos(d);
            floats[i2 * 2 + 1] = (float)Math.sin(d);
        }

        return floats;
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent playerTickEvent) {
        if (!this.circles.isEmpty()) {
            int i = this.getLifetimeTicks();
            boolean flag = this.physics.getValue();
            double d = this.gravity.getValue().floatValue() / 100.0;
            double d2 = this.stiffness.getValue().floatValue() / 100.0;
            Iterator iterator2 = this.circles.iterator();

            while (iterator2.hasNext()) {
                JumpCircleRing jumpCircleRing = (JumpCircleRing)iterator2.next();
                jumpCircleRing.tick();
                if (flag) {
                    jumpCircleRing.simulate(d, d2);
                }

                if (jumpCircleRing.isExpired(i)) {
                    iterator2.remove();
                }
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && mc.world != null && !this.circles.isEmpty()) {
            int i = this.getLifetimeTicks();
            int i2 = this.getBaseColor();
            float f = renderEvent.getTickDelta();
            Entry entry = renderEvent.getMatrices().peek();
            boolean flag = this.animation.is("draw");
            float f2 = this.width.getValue() * 0.06F;
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            double d = Double.MAX_VALUE;
            int i3 = this.circles.size();

            try (BufferAllocator bufferAllocator = new BufferAllocator(i3 * 80 * 8 * 20 + 256)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                for (int i4 = 0; i4 < i3; i4++) {
                    JumpCircleRing jumpCircleRing = this.circles.get(i4);
                    float f3 = jumpCircleRing.getFade(i, f);
                    if (!(f3 <= 0.001F) && ProjectionUtil.isVisible(jumpCircleRing.getBoundingBox().expand(0.5))) {
                        float f4 = jumpCircleRing.getProgress(i, f);
                        float f5 = flag ? jumpCircleRing.getDrawProgress(i, f) : 1.0F;
                        List list = this.applyAnimation(jumpCircleRing.getRenderPoints(f, 0.05F), jumpCircleRing.getCenter(), f4);
                        d = Math.min(d, Math.sqrt(jumpCircleRing.getCenter().squaredDistanceTo(vec3d)));
                        this.computeSegmentColors(list.size(), f5, f4, i2, f3);
                        this.buildRibbon(bufferBuilder, entry, list, vec3d, f2 * 0.5F);
                    }
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer == null) {
                    return;
                }

                float f6 = Math.max(1, mc.getWindow().getFramebufferHeight());
                float f7 = ProjectionUtil.getPixelsPerBlock(d) * f2 * 0.5F;
                float f8 = 2.0F * MathHelper.clamp(0.25F * f7 * f7 / (0.25F * f6), 0.0F, 1.0F);
                float f9 = MathHelper.clamp(ProjectionUtil.getPixelsPerBlock(d) * f2 * 0.2F, 0.3F, 16.0F);
                EntityWarpRenderer.INSTANCE
                    .renderWithPipeline(
                        Pipelines.ROPE_WARP,
                        f8,
                        f9,
                        WARP_CURVE_COLOR,
                        1.0F,
                        () -> MeshDrawUtil.drawLabeled(Pipelines.RIBBON_DEPTH, builtBuffer, "sadnes jumpcircle warp mask")
                    );
            }
        }
    }

    private static int encodeCurveColor(BezierCurve bezierCurve) {
        return ColorUtil.rgb(
            Math.round(255.0F * MathHelper.clamp(bezierCurve.evaluate(0.0F), 0.0F, 1.0F)),
            Math.round(255.0F * MathHelper.clamp(bezierCurve.evaluate(0.5F), 0.0F, 1.0F)),
            Math.round(255.0F * MathHelper.clamp(bezierCurve.evaluate(1.0F), 0.0F, 1.0F))
        );
    }

    private int getBaseColor() {
        return this.colorMode.is("custom") ? this.customColor.getValue() : Theme.getPrimary();
    }

    private void buildRibbon(BufferBuilder bufferBuilder, Entry entry, List<Vec3d> vec3ds, Vec3d vec3d, float f) {
        int i = vec3ds.size();
        if (i >= 2) {
            double d = 0.0;
            double d2 = 0.0;
            double d3 = 0.0;

            for (int i2 = 0; i2 < i; i2++) {
                Vec3d vec3d2 = (Vec3d)vec3ds.get(i2);
                d += vec3d2.x;
                d2 += vec3d2.y;
                d3 += vec3d2.z;
            }

            double d4 = d / i - vec3d.x;
            double d5 = d2 / i - vec3d.y;
            double d6 = d3 / i - vec3d.z;
            double d7 = Math.sqrt(d4 * d4 + d5 * d5 + d6 * d6);
            float f2 = d7 < 1.0E-6 ? 1.0F : (float)Math.abs(d5 / d7);

            for (int i3 = 0; i3 < i; i3++) {
                Vec3d vec3d3 = (Vec3d)vec3ds.get((i3 + i - 1) % i);
                Vec3d vec3d4 = (Vec3d)vec3ds.get((i3 + 1) % i);
                double d8 = -(vec3d4.z - vec3d3.z);
                double d9 = vec3d4.x - vec3d3.x;
                double d10 = Math.sqrt(d8 * d8 + d9 * d9);
                if (d10 < 1.0E-6) {
                    this.normalX[i3] = i3 > 0 ? this.normalX[i3 - 1] : 0.0F;
                    this.normalZ[i3] = i3 > 0 ? this.normalZ[i3 - 1] : 0.0F;
                } else {
                    double d11 = f / d10;
                    this.normalX[i3] = (float)(d8 * d11);
                    this.normalZ[i3] = (float)(d9 * d11);
                }
            }

            for (int i4 = 0; i4 < i; i4++) {
                int i5 = (i4 + 1) % i;
                Vec3d vec3d5 = (Vec3d)vec3ds.get(i4);
                Vec3d vec3d6 = (Vec3d)vec3ds.get(i5);
                float f3 = (float)(vec3d5.x - vec3d.x);
                float f4 = (float)(vec3d5.y - vec3d.y);
                float f5 = (float)(vec3d5.z - vec3d.z);
                float f6 = (float)(vec3d6.x - vec3d.x);
                float f7 = (float)(vec3d6.y - vec3d.y);
                float f8 = (float)(vec3d6.z - vec3d.z);
                int i6 = ColorUtil.multiplyAlpha(this.segmentColors[i4], f2);
                int i7 = ColorUtil.multiplyAlpha(this.segmentColors[i5], f2);
                int i8 = ColorUtil.multiplyAlpha(this.segmentColors[i4], 1.0F - f2);
                int i9 = ColorUtil.multiplyAlpha(this.segmentColors[i5], 1.0F - f2);
                bufferBuilder.vertex(entry, f3 + this.normalX[i4], f4, f5 + this.normalZ[i4]).color(i6);
                bufferBuilder.vertex(entry, f3 - this.normalX[i4], f4, f5 - this.normalZ[i4]).color(i6);
                bufferBuilder.vertex(entry, f6 - this.normalX[i5], f7, f8 - this.normalZ[i5]).color(i7);
                bufferBuilder.vertex(entry, f6 + this.normalX[i5], f7, f8 + this.normalZ[i5]).color(i7);
                bufferBuilder.vertex(entry, f3, f4 + f, f5).color(i8);
                bufferBuilder.vertex(entry, f3, f4 - f, f5).color(i8);
                bufferBuilder.vertex(entry, f6, f7 - f, f8).color(i9);
                bufferBuilder.vertex(entry, f6, f7 + f, f8).color(i9);
            }
        }
    }

    private void computeSegmentColors(int i, float f, float f2, int i2, float f3) {
        boolean flag = this.animation.is("pulse");
        boolean flag2 = this.colorMode.is("rainbow");
        int i3 = flag2 ? i2 : shiftHue(i2);
        int i4 = Math.min(i, this.segmentColors.length);
        float f4 = f * i4;
        float f5 = 360.0F / Math.max(1, i4);

        for (int i5 = 0; i5 < i4; i5++) {
            float f6 = Math.max(0.0F, Math.min(1.0F, f4 - i5));
            if (flag) {
                float f7 = 0.5F + 0.5F * MathHelper.sin((f2 * 3.0F - (float)i5 / i4 * 4.0F) * (float) (Math.PI * 2));
                f6 *= 0.3F + 0.7F * f7 * f7 * f7;
            }

            int i6 = flag2 ? ColorUtil.rainbow(10, Math.round(i5 * f5), 0.75F, 1.0F, 1.0F) : ColorUtil.fade(10, Math.round(i5 * f5), i2, i3);
            this.segmentColors[i5] = ColorUtil.multiplyAlpha(i6, f3 * f6);
        }
    }

    private static int shiftHue(int i) {
        float[] floats = ColorUtil.toHsb(i);
        return ColorUtil.fromHsba((floats[0] + 0.28F) % 1.0F, Math.min(1.0F, floats[1] * 1.2F), floats[2], ColorUtil.alphaf(i));
    }

    private int getLifetimeTicks() {
        return Math.max(1, Math.round(this.lifetime.getValue() * 20.0F));
    }
}
