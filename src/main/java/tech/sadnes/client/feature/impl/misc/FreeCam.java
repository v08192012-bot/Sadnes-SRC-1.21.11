package tech.sadnes.client.feature.impl.misc;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import java.lang.reflect.Array;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.GameRendererAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CameraRotationEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.player.RaycastUtil;

@FeatureInfo(
    name = "FreeCam",
    description = "feature.impl.misc.freecam",
    displayName = "feature.impl.misc.freecam",
    category = Category.VISUALS,
    subcategory = Subcategory.CAMERA
)
public class FreeCam extends Feature {
    private final NumberSetting speed = new NumberSetting("freecam.speed", 1.0F, 0.1F, 5.0F, 0.1F);
    private final BooleanSetting allowWalk = new BooleanSetting("freecam.allowWalk", false);
    private final BooleanSetting clickWalk = new BooleanSetting("freecam.clickWalk", false);
    private BlockPos walkTarget;
    private Vec3d position = Vec3d.ZERO;
    private Vec3d prevPosition;
    private float yaw;
    private float pitch;
    private Perspective savedPerspective;

    public FreeCam() {
        this.clickWalk.setAliases("freecam.dotaMode");
        this.prevPosition = Vec3d.ZERO;
        this.savedPerspective = Perspective.FIRST_PERSON;
    }

    public static FreeCam getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(FreeCam.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.options != null) {
            Perspective perspective = mc.options.getPerspective();
            this.savedPerspective = perspective;
            GameOptions gameOptions = mc.options;
            Perspective perspective2 = Perspective.FIRST_PERSON;
            gameOptions.setPerspective(perspective2);
        }

        if (mc.player != null) {
            Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
            this.prevPosition = vec3d;
            this.position = vec3d;
            float f = mc.gameRenderer.getCamera().getYaw();
            this.yaw = f;
            float f2 = mc.gameRenderer.getCamera().getPitch();
            this.pitch = f2;
            mc.worldRenderer.scheduleTerrainUpdate();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.walkTarget = null;
        if (mc.options != null) {
            GameOptions gameOptions = mc.options;
            Perspective perspective = this.savedPerspective;
            gameOptions.setPerspective(perspective);
            mc.worldRenderer.scheduleTerrainUpdate();
        } else {
            mc.worldRenderer.scheduleTerrainUpdate();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null) {
            if (mc.options != null) {
                GameOptions gameOptions = mc.options;
                Perspective perspective = Perspective.FIRST_PERSON;
                gameOptions.setPerspective(perspective);
                Vec3d vec3d = this.position;
                this.prevPosition = vec3d;
                boolean flag = mc.options.forwardKey.isPressed();
                boolean flag2 = mc.options.backKey.isPressed();
                float f = this.getAxisInput(flag, flag2);
                boolean flag3 = mc.options.leftKey.isPressed();
                boolean flag4 = mc.options.rightKey.isPressed();
                float f2 = this.getAxisInput(flag3, flag4);
                boolean flag5 = mc.options.jumpKey.isPressed();
                boolean flag6 = mc.options.sneakKey.isPressed();
                float f3 = this.getAxisInput(flag5, flag6);
                Vec3d vec3d2 = this.position;
                Vec3d vec3d3 = this.prevPosition;
                int i = !Float.isNaN(f) ? (f == 0.0F ? 0 : 1) : 0;
                if (!Float.isNaN(f) && i == 0) {
                    int i2 = !Float.isNaN(f2) ? (f2 == 0.0F ? 0 : 1) : 0;
                    if (!Float.isNaN(f2) && i2 == 0) {
                        int i3 = !Float.isNaN(f3) ? (f3 == 0.0F ? 0 : 1) : 0;
                        if (!Float.isNaN(f3) && i3 == 0) {
                            this.position = vec3d2;
                            this.prevPosition = vec3d3;
                            return;
                        }
                    }
                }

                Vec3d vec3d4;
                Vec3d vec3d5;
                NumberSetting numberSetting;
                int i4;
                float f4;
                label80: {
                    this.position = vec3d2;
                    this.prevPosition = vec3d3;
                    float f5 = ExactMath.multiply(f2, f2);
                    float f6 = ExactMath.multiply(f, f);
                    float f7 = MathHelper.sqrt(ExactMath.add(f5, f6));
                    vec3d4 = this.position;
                    vec3d5 = this.prevPosition;
                    numberSetting = this.speed;
                    float f8 = ExactMath.divide(f, f7);
                    if (!Float.isNaN(f7) && 1.0F < f7) {
                        i4 = Float.floatToRawIntBits(f8);
                        if (!(f7 > 1.0F)) {
                            f4 = f2;
                            break label80;
                        }
                    } else {
                        i4 = Float.floatToRawIntBits(f);
                        if (!(f7 > 1.0F)) {
                            f4 = f2;
                            break label80;
                        }
                    }

                    f4 = ExactMath.divide(f2, f7);
                }

                this.position = vec3d4;
                this.prevPosition = vec3d5;
                float f9 = numberSetting.getValue();
                double d = Math.toRadians(this.yaw);
                double d2 = Math.sin(d);
                double d3 = Math.cos(d);
                Vec3d vec3d6 = this.position;
                double d4 = f4;
                double d5 = ExactMath.multiply(d3, d4);
                double d6 = Float.intBitsToFloat(i4);
                double d7 = ExactMath.multiply(d2, d6);
                double d8 = ExactMath.subtract(d5, d7);
                double d9 = f9;
                double d10 = ExactMath.multiply(d8, d9);
                double d11 = ExactMath.multiply(f3, d9);
                double d12 = ExactMath.multiply(d2, d4);
                double d13 = ExactMath.multiply(d3, d6);
                double d14 = ExactMath.multiply(ExactMath.add(d12, d13), d9);
                Vec3d vec3d7 = vec3d6.add(d10, d11, d14);
                this.position = vec3d7;
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.setEnabled(false);
    }

    @EventHandler
    public void onMouseLook(MouseLookEvent mouseLookEvent) {
        float f = this.yaw;
        double d = mouseLookEvent.getDeltaX();
        float f2 = this.pitch;
        double d2 = ExactMath.multiply(d, 0.15F);
        double d3 = f;
        float f3 = (float)ExactMath.add(d2, d3);
        this.yaw = f3;
        float f4 = MathHelper.clamp(ExactMath.add(ExactMath.multiply((float)mouseLookEvent.getDeltaY(), 0.15F), f2), -90.0F, 90.0F);
        this.pitch = f4;
        mouseLookEvent.cancel();
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (this.clickWalk.getValue() && screenKeyEvent.isPress() && screenKeyEvent.isKey(1)) {
            if (screenKeyEvent.getScreen() != null) {
                return;
            }

            if (mc.player == null) {
                return;
            }

            ClientWorld clientWorld = mc.world;
            float f = this.yaw;
            float f2 = this.pitch;
            if (clientWorld == null) {
                return;
            }

            Vec3d vec3d = RaycastUtil.getDirection(f, f2);
            if (!(vec3d.y >= -0.01)) {
                Vec3d vec3d2 = mc.gameRenderer.getCamera().getCameraPos();
                double d = vec3d2.y;
                int i = mc.world.getBottomY();
                double d2 = vec3d.y;
                double d3 = i;
                double d4 = ExactMath.add(ExactMath.subtract(d, d3), 1.0);
                int i2 = (int)Double.doubleToRawLongBits(d2);
                int i3 = (int)(Double.doubleToRawLongBits(d2) >>> 32) ^ Integer.MIN_VALUE;
                double d5 = Double.longBitsToDouble(Integer.toUnsignedLong(i2) | (long)i3 << 32);
                double d6 = Math.min(ExactMath.divide(d4, d5), 512.0);
                ClientWorld clientWorld2 = mc.world;
                Vec3d vec3d3 = vec3d.multiply(d6);
                Vec3d vec3d4 = vec3d2.add(vec3d3);
                ShapeType shapeType = ShapeType.OUTLINE;
                FluidHandling fluidHandling = FluidHandling.NONE;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                RaycastContext raycastContext = new RaycastContext(vec3d2, vec3d4, shapeType, fluidHandling, clientPlayerEntity);
                BlockHitResult blockHitResult = clientWorld2.raycast(raycastContext);
                Type type = blockHitResult.getType();
                Type type2 = Type.BLOCK;
                if (type != type2) {
                    return;
                }

                Vec3d vec3d5 = blockHitResult.getPos();
                double d7 = vec3d2.squaredDistanceTo(vec3d5);
                if (!(1.0 > d7)) {
                    IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    BlockPos blockPos = blockHitResult.getBlockPos().up();
                    BlockPos blockPos2 = this.walkTarget;
                    if (blockPos2 != null) {
                        double d8 = blockPos2.getSquaredDistance(blockPos);
                        if (2.0 >= d8 && iBaritone.getCustomGoalProcess().isActive()) {
                            iBaritone.getPathingBehavior().cancelEverything();
                            this.walkTarget = null;
                            return;
                        }
                    }

                    this.walkTarget = blockPos;
                    ICustomGoalProcess iCustomGoalProcess = iBaritone.getCustomGoalProcess();
                    GoalBlock goalBlock = new GoalBlock(blockPos);
                    iCustomGoalProcess.setGoalAndPath(goalBlock);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (this.allowWalk.getValue()) {
            if (mc.currentScreen != null) {
                moveInputEvent.cancel();
                return;
            }

            if (mc.player != null) {
                int i;
                label132: {
                    int i2 = Key.KEY_UP.getKeyCode();
                    if (i2 >= 0) {
                        MinecraftClient minecraftClient = MinecraftClient.getInstance();
                        if (minecraftClient != null && minecraftClient.getWindow() != null) {
                            long l = minecraftClient.getWindow().getHandle();
                            if (l != 0L) {
                                if (Integer.compareUnsigned(i2, 8) < 0) {
                                    int i3 = i2 & 7;
                                    if (GLFW.glfwGetMouseButton(l, i3) == 1) {
                                        i = 1065353216;
                                        break label132;
                                    }
                                } else {
                                    int i4 = i2 & 2147483647;
                                    if (GLFW.glfwGetKey(l, i4) == 1) {
                                        i = 1065353216;
                                        break label132;
                                    }
                                }
                            }
                        }
                    }

                    int i5 = Key.KEY_DOWN.getKeyCode();
                    if (i5 >= 0) {
                        MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
                        if (minecraftClient2 == null) {
                            i = 0;
                            break label132;
                        }

                        if (minecraftClient2.getWindow() == null) {
                            i = 0;
                            break label132;
                        }

                        long l2 = minecraftClient2.getWindow().getHandle();
                        if (l2 == 0L) {
                            i = 0;
                            break label132;
                        }

                        if (Integer.compareUnsigned(i5, 7) > 0) {
                            int i6 = i5 & 2147483647;
                            if (GLFW.glfwGetKey(l2, i6) == 1) {
                                i = -1082130432;
                                break label132;
                            }
                        } else {
                            int i7 = i5 & 7;
                            if (GLFW.glfwGetMouseButton(l2, i7) == 1) {
                                i = -1082130432;
                                break label132;
                            }
                        }
                    }

                    i = 0;
                }

                int i8;
                label134: {
                    float f = Float.intBitsToFloat(i);
                    moveInputEvent.setForward(f);
                    int i9 = Key.KEY_LEFT.getKeyCode();
                    if (i9 >= 0) {
                        MinecraftClient minecraftClient3 = MinecraftClient.getInstance();
                        if (minecraftClient3 != null && minecraftClient3.getWindow() != null) {
                            long l3 = minecraftClient3.getWindow().getHandle();
                            if (l3 != 0L) {
                                if (Integer.compareUnsigned(i9, 8) < 0) {
                                    int i10 = i9 & 7;
                                    if (GLFW.glfwGetMouseButton(l3, i10) == 1) {
                                        i8 = 1065353216;
                                        break label134;
                                    }
                                } else {
                                    int i11 = i9 & 2147483647;
                                    if (GLFW.glfwGetKey(l3, i11) == 1) {
                                        i8 = 1065353216;
                                        break label134;
                                    }
                                }
                            }
                        }
                    }

                    int i12 = Key.KEY_RIGHT.getKeyCode();
                    if (i12 >= 0) {
                        MinecraftClient minecraftClient4 = MinecraftClient.getInstance();
                        if (minecraftClient4 == null) {
                            i8 = 0;
                            break label134;
                        }

                        if (minecraftClient4.getWindow() == null) {
                            i8 = 0;
                            break label134;
                        }

                        long l4 = minecraftClient4.getWindow().getHandle();
                        if (l4 == 0L) {
                            i8 = 0;
                            break label134;
                        }

                        if (Integer.compareUnsigned(i12, 7) > 0) {
                            int i13 = i12 & 2147483647;
                            if (GLFW.glfwGetKey(l4, i13) == 1) {
                                i8 = -1082130432;
                                break label134;
                            }
                        } else {
                            int i14 = i12 & 7;
                            if (GLFW.glfwGetMouseButton(l4, i14) == 1) {
                                i8 = -1082130432;
                                break label134;
                            }
                        }
                    }

                    i8 = 0;
                }

                float f2 = Float.intBitsToFloat(i8);
                moveInputEvent.setStrafe(f2);
                moveInputEvent.setJump(false);
                moveInputEvent.setSneak(false);
                int i15 = Key.KEY_UP.getKeyCode();
                if (i15 >= 0) {
                    MinecraftClient minecraftClient5 = MinecraftClient.getInstance();
                    if (minecraftClient5 != null && minecraftClient5.getWindow() != null) {
                        long l5 = minecraftClient5.getWindow().getHandle();
                        if (l5 != 0L) {
                            if (Integer.compareUnsigned(i15, 8) < 0) {
                                int i16 = i15 & 7;
                                if (GLFW.glfwGetMouseButton(l5, i16) != 1) {
                                    return;
                                }
                            } else {
                                int i17 = i15 & 2147483647;
                                if (GLFW.glfwGetKey(l5, i17) != 1) {
                                    return;
                                }
                            }

                            mc.player.setSprinting(true);
                            ThrowComponent.getInstance();
                            ThrowComponent.rotationTimer.setDelay(40L);
                            return;
                        }
                    }
                }

                return;
            }
        }

        moveInputEvent.cancel();
    }

    @EventHandler
    public void onCameraRotation(CameraRotationEvent cameraRotationEvent) {
        float f = this.yaw;
        cameraRotationEvent.setYaw(f);
        float f2 = this.pitch;
        cameraRotationEvent.setPitch(f2);
    }

    public Vec3d getInterpolatedPosition(float f) {
        Vec3d vec3d = this.prevPosition;
        Vec3d vec3d2 = this.position;
        double d = f;
        return vec3d.lerp(vec3d2, d);
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (mc.player != null && mc.world != null && !mc.options.hudHidden) {
            Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
            String string = Language.translate("freecam.coordinates");
            Object[] objects = new Object[3];
            Integer integer = MathHelper.floor(vec3d.x);
            Array.set(objects, 0, integer);
            Integer integer2 = MathHelper.floor(vec3d.y);
            Array.set(objects, 1, integer2);
            Integer integer3 = MathHelper.floor(vec3d.z);
            Array.set(objects, 2, integer3);
            String string2 = String.format(string, objects);
            GameRendererAccessor gameRendererAccessor = (GameRendererAccessor)mc.gameRenderer;
            Camera camera = mc.gameRenderer.getCamera();
            float f = render2DEvent.getTickDelta();
            float f2 = gameRendererAccessor.sadnes$getFov(camera, f, true);
            int i = GuiScale.getScaledHeightInt();
            double d = Math.tan(Math.toRadians(10.0));
            double d2 = Math.tan(Math.toRadians(ExactMath.multiply(f2, 0.5F)));
            DrawContext drawContext = render2DEvent.getContext();
            int i2 = GuiScale.getScaledWidthInt();
            float f3 = ExactMath.multiply(i, 0.5F);
            double d3 = f3;
            double d4 = ExactMath.divide(d, d2);
            float f4 = ExactMath.add(ExactMath.add((float)ExactMath.multiply(d3, d4), f3), -4.0F);
            float f5 = i2;
            RenderUtil renderUtil = new RenderUtil(drawContext, 0.0F, f4, f5, 8.0F);
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            renderUtil.textCenter(msdfFont, string2, -1, 6.5F, true);
        }
    }

    private float getAxisInput(boolean flag, boolean flag2) {
        if (flag) {
            return flag != flag2 ? 1.0F : 0.0F;
        } else {
            return flag != flag2 ? -1.0F : 0.0F;
        }
    }
}
