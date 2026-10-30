package tech.sadnes.client.ui.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventBus;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class WaypointRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final List<WaypointRenderer.Waypoint> WAYPOINTS = new ArrayList<>();
    private final List<WaypointRenderer.Waypoint> visible = new ArrayList<>();
    private long frameCounter;
    private long lastFrameNanos;
    private int accentColor;
    private int timerColor;
    private int iconColor;
    private int backgroundColor;

    public static void register() {
        EventBus eventBus = Sadnes.getEventBus();
        WaypointRenderer waypointRenderer = new WaypointRenderer();
        eventBus.subscribe(waypointRenderer);
    }

    public static void add(String string, double d, double d2, double d3) {
        remove(string);
        WAYPOINTS.add(new WaypointRenderer.Waypoint(string, Icon.LOCATION_DOT, d, d2, d3, getServerKey(), 0L, -1));
    }

    public static void addTimed(String string, Icon icon, int i, int i2, int i3, int i4) {
        double d = i2 + 0.5;
        double d2 = i4 + 0.5;
        String string2 = getServerKey();
        WAYPOINTS.removeIf(
            waypoint -> waypoint.lifetime > 0L
                && waypoint.server.equalsIgnoreCase(string2)
                && waypoint.anarchy == i
                && (waypoint.name.equalsIgnoreCase(string) || waypoint.distanceSqXZ(d, d2) < 16.0)
        );
        WAYPOINTS.add(new WaypointRenderer.Waypoint(string, icon, d, i3, d2, string2, 720000L, i));
    }

    public static boolean remove(String string) {
        return WAYPOINTS.removeIf(waypoint -> waypoint.name.equalsIgnoreCase(string));
    }

    public static boolean rename(String string, String string2) {
        WAYPOINTS.removeIf(waypoint -> waypoint.name.equalsIgnoreCase(string2) && !waypoint.name.equalsIgnoreCase(string));

        for (WaypointRenderer.Waypoint waypoint2 : WAYPOINTS) {
            if (waypoint2.name.equalsIgnoreCase(string)) {
                waypoint2.name = string2;
                return true;
            }
        }

        return false;
    }

    public static void clear() {
        WAYPOINTS.clear();
    }

    public static List<String> getNames() {
        String string = getServerKey();
        long l = System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();

        for (WaypointRenderer.Waypoint waypoint : WAYPOINTS) {
            if (waypoint.server.equalsIgnoreCase(string) && !waypoint.isExpired(l)) {
                arrayList.add(waypoint.name);
            }
        }

        return arrayList;
    }

    private static String getServerKey() {
        return MC.getCurrentServerEntry() != null && MC.getCurrentServerEntry().address != null ? MC.getCurrentServerEntry().address : "singleplayer";
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isHudPre() && MC.player != null && MC.world != null && Fonts.INTER_MEDIUM != null) {
            if (Fonts.INTER_SEMIBOLD == null) {
                return;
            }

            long l = System.currentTimeMillis();
            long l2 = System.nanoTime();
            float f = this.lastFrameNanos == 0L ? 0.0F : Math.min((float)(l2 - this.lastFrameNanos) / 1.0E9F, 0.1F);
            this.lastFrameNanos = l2;
            this.frameCounter++;
            WAYPOINTS.removeIf(waypoint -> waypoint.isExpired(l));
            this.collectVisible(l);
            this.resolveOverlaps(f);
            this.accentColor = Theme.getPrimary();
            this.timerColor = ColorUtil.luminance(this.accentColor) < 0.25F ? Theme.tintWhite(0.6F) : this.accentColor;
            this.iconColor = Theme.getContrastColor(this.accentColor);
            this.backgroundColor = ColorUtil.interpolate(Theme.getBase(), this.accentColor, 0.1F);
            DrawContext drawContext = renderEvent.getContext();
            int i = this.visible.size();

            for (int i2 = i - 1; i2 >= 0; i2--) {
                this.renderWaypoint(drawContext, this.visible.get(i2), l);
            }
        }
    }

    private void collectVisible(long l) {
        this.visible.clear();
        if (!WAYPOINTS.isEmpty()) {
            String string = getServerKey();
            int i = ServerManager.getAnarchyNumber();
            float f = GuiScale.getScaledWidth();
            float f2 = GuiScale.getScaledHeight();

            for (WaypointRenderer.Waypoint waypoint : WAYPOINTS) {
                if (waypoint.server.equalsIgnoreCase(string)) {
                    Vector2f vector2f = ProjectionUtil.worldToScreen(waypoint.x, waypoint.y + 1.5, waypoint.z);
                    if (vector2f != null) {
                        double d = Math.sqrt(MC.player.squaredDistanceTo(waypoint.x, waypoint.y + 0.5, waypoint.z));
                        float f3 = MathHelper.clamp((float)(d - 3.0) / 6.0F, 0.35F, 1.0F);
                        waypoint.alpha = waypoint.getFadeAlpha(l) * f3;
                        if (!(waypoint.alpha <= 0.01F)) {
                            waypoint.otherAnarchy = waypoint.anarchy > 0 && waypoint.anarchy != i;
                            waypoint.updateLayout(d);
                            if (vector2f.x + waypoint.width > 0.0F && vector2f.x - waypoint.width < f && vector2f.y + waypoint.height > 0.0F && vector2f.y - waypoint.height < f2) {
                                waypoint.screenX = MathUtil.roundToStep(vector2f.x, 0.5F);
                                waypoint.screenY = MathUtil.roundToStep(vector2f.y, 0.5F);
                                this.visible.add(waypoint);
                            }
                        }
                    }
                }
            }

            this.visible.sort((waypoint2, waypoint3) -> Double.compare(waypoint2.distance, waypoint3.distance));
        }
    }

    private void resolveOverlaps(float f) {
        float f2 = 1.0F - (float)Math.exp(-14.0F * f);
        int i = this.visible.size();

        for (int i2 = 0; i2 < i; i2++) {
            WaypointRenderer.Waypoint waypoint = this.visible.get(i2);
            float f3 = waypoint.screenY - waypoint.height / 2.0F;
            float f4 = f3;

            for (int i3 = 0; i3 < i2; i3++) {
                WaypointRenderer.Waypoint waypoint2 = this.visible.get(i3);
                boolean flag = Math.abs(waypoint.screenX - waypoint2.screenX) * 2.0F < waypoint.width + waypoint2.width + 4.0F;
                if (f4 < waypoint2.layoutY + waypoint2.height + 2.0F && waypoint2.layoutY < f4 + waypoint.height + 2.0F && flag) {
                    f4 = waypoint2.layoutY - waypoint.height - 2.0F;
                    i3 = -1;
                }
            }

            waypoint.layoutY = f4;
            WaypointRenderer.Waypoint waypoint3;
            float f5;
            if (waypoint.lastFrame != this.frameCounter - 1L) {
                waypoint3 = waypoint;
                f5 = f3 - f4;
            } else {
                waypoint3 = waypoint;
                f5 = waypoint.offsetY + (f3 - f4 - waypoint.offsetY) * f2;
            }

            waypoint3.offsetY = f5;
            waypoint.lastFrame = this.frameCounter;
            waypoint.drawY = MathUtil.roundToStep(f3 - waypoint.offsetY, 0.5F);
            waypoint.overlapAlpha = MathHelper.clamp(1.0F - Math.min(waypoint.offsetY, -waypoint.drawY) / 4.0F, 0.0F, 1.0F);
        }
    }

    private void renderWaypoint(DrawContext drawContext, WaypointRenderer.Waypoint waypoint, long l) {
        float f = waypoint.alpha * waypoint.overlapAlpha;
        if (!(f <= 0.01F)) {
            float f2 = MathUtil.roundToStep(waypoint.screenX - waypoint.width / 2.0F, 0.5F);
            float f3 = waypoint.drawY;
            Radius radius = Radius.all(waypoint.height / 2.0F);
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, waypoint.width, waypoint.height)
                .blurGlowBorder(
                    f,
                    this.backgroundColor,
                    0.7F,
                    Theme.getNearBackground(),
                    3.0F,
                    Theme.getShadowStrength(f),
                    Easings.QUAD_IN,
                    radius,
                    RenderUtil.BlurQuality.LOW,
                    false
                );
            this.drawIcon(drawContext, waypoint, f2 + 2.5F, f3 + 2.5F, l, f);
            renderUtil.textMiddleLeft(Fonts.INTER_SEMIBOLD, waypoint.name, waypoint.nameOffset, 0.0F, Theme.withAlpha(Theme.getTextColor(), f), 6.5F);
            drawSeparator(drawContext, f2 + waypoint.separatorOffset, f3 + waypoint.height / 2.0F, f);
            renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, waypoint.distanceText, waypoint.distanceOffset, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), f), 6.0F);
            if (waypoint.otherAnarchy) {
                drawSeparator(drawContext, f2 + waypoint.anarchySeparatorOffset, f3 + waypoint.height / 2.0F, f);
                renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, waypoint.anarchyText, waypoint.anarchyOffset, 0.0F, Theme.withAlpha(this.timerColor, f), 6.0F);
            }
        }
    }

    private static void drawSeparator(DrawContext drawContext, float f, float f2, float f3) {
        new RenderUtil(drawContext, f, f2 - 3.5F, 0.75F, 7.0F).rect(Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f3), Radius.all(0.375F));
    }

    private void drawIcon(DrawContext drawContext, WaypointRenderer.Waypoint waypoint, float f, float f2, long l, float f3) {
        boolean flag;
        float f4;
        float f5;
        if (waypoint.lifetime > 0L) {
            flag = true;
            f4 = 10.0F;
            f5 = 6.0F;
        } else {
            flag = false;
            f4 = 14.0F;
            f5 = 8.0F;
        }

        float f6 = f5;
        float f7 = (16.0F - f4) / 2.0F;
        float f8 = (f4 - f6) / 2.0F;
        new RenderUtil(drawContext, f + f7, f2 + f7, f4, f4)
            .rect(Theme.withAlpha(this.accentColor, f3), Radius.all(f4 / 2.0F))
            .icon(waypoint.icon, f8, f8, f6, Theme.withAlpha(this.iconColor, f3));
        if (flag) {
            long l2 = waypoint.getRemaining(l);
            float f9 = 360.0F * MathHelper.clamp((float)l2 / (float)waypoint.lifetime, 0.0F, 1.0F);
            float f10 = l2 < 60000L ? 0.6F + 0.4F * (float)Math.abs(Math.sin(l / 320.0)) : 1.0F;
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, 16.0F, 16.0F).circle(1.25F, Theme.withAlpha(Theme.getSecondaryText(), 0.22F * f3));
            if (f9 > 0.5F) {
                renderUtil.arc(0.0F, f9, 1.25F, this.timerColor, f3 * f10);
            }
        }
    }

    static final class Waypoint {
        String name;
        final Icon icon;
        final double x;
        final double y;
        final double z;
        final String server;
        final long lifetime;
        final int anarchy;
        private final StopWatch timer = new StopWatch();
        double distance;
        String distanceText;
        String anarchyText;
        private String cachedName;
        private long cachedDistance = -1L;
        long lastFrame = -1L;
        boolean otherAnarchy;
        private boolean cachedOtherAnarchy;
        float alpha;
        float screenX;
        float screenY;
        float width;
        float height;
        float nameOffset;
        float separatorOffset;
        float distanceOffset;
        float anarchySeparatorOffset;
        float anarchyOffset;
        float layoutY;
        float offsetY;
        float drawY;
        float overlapAlpha;

        long getRemaining(long l) {
            return this.timer.getTime() + this.lifetime - l;
        }

        boolean isExpired(long l) {
            return this.lifetime > 0L && this.getRemaining(l) <= 0L;
        }

        double distanceSqXZ(double d, double d2) {
            double d3 = this.x - d;
            double d4 = this.z - d2;
            return d3 * d3 + d4 * d4;
        }

        float getFadeAlpha(long l) {
            float f = MathHelper.clamp((float)this.timer.getElapsed() / 200.0F, 0.0F, 1.0F);
            return this.lifetime <= 0L ? f : Math.min(f, MathHelper.clamp((float)this.getRemaining(l) / 600.0F, 0.0F, 1.0F));
        }

        void updateLayout(double d) {
            this.distance = d;
            long l = Math.round(d);
            if (l != this.cachedDistance || !this.name.equals(this.cachedName) || this.otherAnarchy != this.cachedOtherAnarchy) {
                this.cachedName = this.name;
                this.cachedDistance = l;
                this.cachedOtherAnarchy = this.otherAnarchy;
                this.distanceText = String.valueOf(l);
                this.height = 21.0F;
                this.nameOffset = 22.5F;
                this.separatorOffset = this.nameOffset + Fonts.INTER_SEMIBOLD.getWidth(this.name, 6.5F) + 4.0F;
                this.distanceOffset = this.separatorOffset + 4.75F;
                float f = this.distanceOffset + Fonts.INTER_MEDIUM.getWidth(this.distanceText, 6.0F);
                if (this.otherAnarchy) {
                    this.anarchyText = "/an" + this.anarchy;
                    this.anarchySeparatorOffset = f + 4.0F;
                    this.anarchyOffset = this.anarchySeparatorOffset + 4.75F;
                    f = this.anarchyOffset + Fonts.INTER_MEDIUM.getWidth(this.anarchyText, 6.0F);
                }

                this.width = f + 7.0F;
            }
        }

        public Waypoint(String string, Icon icon2, double d, double d2, double d3, String string2, long l, int i) {
            if (string == null) {
                throw new NullPointerException("name is marked non-null but is null");
            }

            this.name = string;
            this.icon = icon2;
            this.x = d;
            this.y = d2;
            this.z = d3;
            this.server = string2;
            this.lifetime = l;
            this.anarchy = i;
        }
    }
}
