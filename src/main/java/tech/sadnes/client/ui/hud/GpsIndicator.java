package tech.sadnes.client.ui.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventBus;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.font.RollingNumberRenderer;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class GpsIndicator {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final GpsIndicator INSTANCE = new GpsIndicator();
    private static final Animation FADE_ANIMATION = new Animation();
    private static final RollingNumberRenderer DISTANCE_TEXT = new RollingNumberRenderer();
    private static final Radius RADIUS = Radius.all(5.5F);
    private static volatile boolean active;
    private static double targetX;
    private static double targetZ;
    private static String serverKey;
    private static String dimensionKey;
    private static float arrowAngle;
    private static boolean angleInitialized;

    public static void register() {
        EventBus eventBus = Sadnes.getEventBus();
        GpsIndicator gpsIndicator = INSTANCE;
        eventBus.subscribe(gpsIndicator);
    }

    public static void setTarget(double d, double d2) {
        if (!active) {
            DISTANCE_TEXT.reset();
        }

        targetX = d;
        targetZ = d2;
        serverKey = getServerKey();
        dimensionKey = getDimensionKey();
        active = true;
        angleInitialized = false;
        FADE_ANIMATION.animate(1.0, 0.3, Easings.CUBIC_OUT, true);
    }

    public static void clear() {
        active = false;
        FADE_ANIMATION.animate(0.0, 0.18, Easings.CUBIC_IN, true);
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        if (active && (!getServerKey().equals(serverKey) || !getDimensionKey().equals(dimensionKey))) {
            active = false;
            angleInitialized = false;
            FADE_ANIMATION.forceValue(0.0);
            ChatUtil.print(Language.translate("command.gps.worldchanged"));
        }
    }

    private static String getServerKey() {
        if (MC.getCurrentServerEntry() != null && MC.getCurrentServerEntry().address != null) {
            return MC.getCurrentServerEntry().address;
        } else {
            return MC.getServer() != null ? MC.getServer().getSaveProperties().getLevelName() : "singleplayer";
        }
    }

    private static String getDimensionKey() {
        return MC.world != null ? MC.world.getRegistryKey().getValue().toString() : "";
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isHudPost() && MC.player != null && MC.world != null) {
            FADE_ANIMATION.update();
            float f = FADE_ANIMATION.get();
            if (active || !(f <= 0.002F)) {
                MsdfFont msdfFont = Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM : Fonts.getDefault();
                if (msdfFont != null) {
                    DrawContext drawContext = renderEvent.getContext();
                    double d = targetX - MC.gameRenderer.getCamera().getCameraPos().x;
                    double d2 = targetZ - MC.gameRenderer.getCamera().getCameraPos().z;
                    double d3 = Math.cos(Math.toRadians(MC.gameRenderer.getCamera().getYaw()));
                    double d4 = Math.sin(Math.toRadians(MC.gameRenderer.getCamera().getYaw()));
                    float f2 = (float)Math.toDegrees(Math.atan2(-(d2 * d3 - d * d4), -(d * d3 + d2 * d4)));
                    float f3 = MathHelper.clamp(MathUtil.getFrameTime() * 16.0F, 0.0F, 1.0F);
                    arrowAngle = angleInitialized ? arrowAngle + MathHelper.wrapDegrees(f2 - arrowAngle) * f3 : f2;
                    angleInitialized = true;
                    float f4 = Math.min(GuiScale.getScaledWidthInt(), GuiScale.getScaledHeightInt()) * 0.145F;
                    float f5 = (float)Math.toRadians(arrowAngle);
                    float f6 = GuiScale.getScaledWidthInt() / 2.0F + (float)Math.cos(f5) * f4;
                    float f7 = GuiScale.getScaledHeightInt() / 2.0F - 12.0F;
                    float f8 = f7 + (float)Math.sin(f5) * f4;
                    float f9 = 0.72F + 0.28F * f;
                    float f10 = 16.0F * f9;
                    float f11 = (float)(System.currentTimeMillis() % 2400L) / 2400.0F;
                    RenderUtil renderUtil = new RenderUtil(drawContext, f6 - f10 / 2.0F, f8 - f10 / 2.0F, f10, f10);
                    renderUtil.progressRing(270.0F - arrowAngle, 55.0F, Theme.getPrimary(), f, f11, 0.55F);
                    Vec3d vec3d = MC.player.getLerpedPos(renderEvent.getTickDelta());
                    DISTANCE_TEXT.update(Math.round(Math.hypot(targetX - vec3d.x, targetZ - vec3d.z)));
                    String string = Language.translate("gps.marker.unit", "m");
                    float f12 = DISTANCE_TEXT.getWidth(msdfFont, 6.0F);
                    float f13 = msdfFont.getWidth(string, 6.0F);
                    float f14 = f12 + f13 + 9.0F;
                    float f15 = f6 - f14 / 2.0F;
                    float f16 = f8 + f10 / 2.0F + 2.0F;
                    RenderUtil renderUtil2 = new RenderUtil(drawContext, f15, f16, f14, 11.0F);
                    renderUtil2.blur(f, RADIUS, RenderUtil.BlurQuality.LOW);
                    renderUtil2.rect(Theme.withAlpha(Theme.getSurfaceDarkest(), 0.72F * f), RADIUS);
                    float f17 = f16 + (11.0F - msdfFont.getHeight(6.0F)) / 2.0F;
                    float f18 = f15 + 4.0F;
                    if (ScissorStack.push(drawContext, f15, f16, f14, 11.0F)) {
                        try {
                            DISTANCE_TEXT.render(drawContext, msdfFont, f18, f17, 6.0F, Theme.withAlpha(Theme.getTextColor(), f));
                        } finally {
                            ScissorStack.pop(drawContext);
                        }
                    }

                    msdfFont.draw(drawContext, string, f18 + f12 + 1.0F, f17, Theme.withAlpha(Theme.getSecondaryText(), f), 6.0F);
                }
            }
        }
    }
}
