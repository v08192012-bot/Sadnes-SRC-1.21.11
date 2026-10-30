package tech.sadnes.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ProjectionUtil {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static Matrix4f modelViewMatrix;
    private static Matrix4f projectionMatrix;
    private static Frustum frustum;

    public static void updateMatrices(Matrix4f matrix4f, Matrix4f matrix4f2) {
        modelViewMatrix = new Matrix4f(matrix4f);
        projectionMatrix = new Matrix4f(matrix4f2);
        Frustum frustum2 = new Frustum(new Matrix4f(matrix4f), new Matrix4f(matrix4f2));
        Vec3d vec3d = MC.gameRenderer.getCamera().getCameraPos();
        frustum2.setPosition(vec3d.x, vec3d.y, vec3d.z);
        frustum = frustum2;
    }

    public static boolean isVisible(Box box) {
        return frustum == null || frustum.isVisible(box);
    }

    public static boolean isVisible(double d, double d2, double d3) {
        return frustum == null || frustum.intersectPoint(d, d2, d3);
    }

    public static Vec3d getPointInFront(double d) {
        Camera camera = MC.gameRenderer.getCamera();
        Vec3d vec3d = camera.getCameraPos();
        if (modelViewMatrix != null && projectionMatrix != null) {
            Matrix4f matrix4f = new Matrix4f(projectionMatrix).mul(modelViewMatrix).invert();
            Vector3f vector3f = matrix4f.transformProject(new Vector3f(0.0F, 0.0F, -1.0F));
            Vector3f vector3f2 = matrix4f.transformProject(new Vector3f(0.0F, 0.0F, 0.5F));
            Vector3f vector3f3;
            Vector3f vector3f4;
            if (vector3f.lengthSquared() <= vector3f2.lengthSquared()) {
                vector3f3 = vector3f;
                vector3f4 = vector3f2;
            } else {
                vector3f3 = vector3f2;
                vector3f4 = vector3f;
            }

            Vector3f vector3f5 = vector3f4.sub(vector3f3).normalize();
            return vec3d.add(vector3f3.x + vector3f5.x * d, vector3f3.y + vector3f5.y * d, vector3f3.z + vector3f5.z * d);
        } else {
            return vec3d.add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(d));
        }
    }

    public static Vector2f worldToScreen(Vec3d vec3d) {
        return worldToScreen(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vector2f worldToScreen(double d, double d2, double d3) {
        if (modelViewMatrix != null && projectionMatrix != null) {
            Camera camera = MC.gameRenderer.getCamera();
            Vec3d vec3d = camera.getCameraPos();
            float f = (float)(d - vec3d.x);
            float f2 = (float)(d2 - vec3d.y);
            float f3 = (float)(d3 - vec3d.z);
            Vector4f vector4f = new Vector4f(f, f2, f3, 1.0F);
            vector4f.mul(modelViewMatrix).mul(projectionMatrix);
            if (vector4f.w <= 0.0F) {
                return null;
            }

            Vector4f vector4f2 = vector4f.div(vector4f.w);
            float f4 = (vector4f2.x + 1.0F) / 2.0F * GuiScale.getScaledWidthInt();
            float f5 = (1.0F - vector4f2.y) / 2.0F * GuiScale.getScaledHeightInt();
            return new Vector2f(f4, f5);
        } else {
            return null;
        }
    }

    public static float getPixelsPerBlock(double d) {
        return projectionMatrix == null ? 0.0F : (float)(projectionMatrix.m11() * MC.getWindow().getFramebufferHeight() / (2.0 * Math.max(d, 0.5)));
    }

    public static float getScaledSize(float f, double d) {
        return MathHelper.clamp(f * getPixelsPerBlock(d), 1.0F, 36.0F);
    }

    public static Vec3d getInterpolatedPos(Entity entity, float f) {
        return new Vec3d(
            MathHelper.lerp(f, entity.lastRenderX, entity.getX()),
            MathHelper.lerp(f, entity.lastRenderY, entity.getY()),
            MathHelper.lerp(f, entity.lastRenderZ, entity.getZ())
        );
    }

    private ProjectionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
