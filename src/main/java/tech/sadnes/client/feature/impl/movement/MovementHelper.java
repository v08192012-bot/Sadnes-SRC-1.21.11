package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "MovementHelper",
    description = "feature.impl.movement.movementhelper",
    displayName = "feature.impl.movement.movementhelper",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_ASSIST
)
public final class MovementHelper extends Feature {
    protected final BooleanSetting actions = new BooleanSetting("movementhelper.actions", true);
    private final ModeSetting action = new ModeSetting("movementhelper.action", new String[]{"jump", "sneak"}).setVisibleWhen(() -> this.actions.getValue());
    protected final BooleanSetting onlyWhenBack = new BooleanSetting("movementhelper.onlywhenback", true)
        .setVisibleWhen(() -> this.actions.getValue() && this.action.is("sneak") ? true : false);
    private final NumberSetting factor = new NumberSetting("movementhelper.factor", 0.01F, 0.001F, 0.3F, 0.001F)
        .setVisibleWhen(() -> this.actions.getValue() && this.action.is("jump") ? true : false);
    private final BooleanSetting trapAssist = new BooleanSetting("movementhelper.trapassist", false);

    @EventHandler
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        boolean flag = ServerManager.isNullWorld();
        BooleanSetting booleanSetting = this.trapAssist;
        if (!flag) {
            boolean flag2 = booleanSetting.getValue();
            BooleanSetting booleanSetting2 = this.actions;
            if ((!flag2 || !MoveUtil.steerToSafeEdge(moveInputEvent)) && booleanSetting2.getValue()) {
                boolean flag3 = mc.player.isOnGround();
                ModeSetting modeSetting = this.action;
                if (flag3) {
                    boolean flag4 = modeSetting.is("jump");
                    NumberSetting numberSetting = this.factor;
                    if (flag4 && !isGroundBelow(numberSetting.getValue())) {
                        moveInputEvent.setJump(true);
                        return;
                    }

                    if (modeSetting.is("sneak")) {
                        ClientWorld clientWorld = mc.world;
                        BlockPos blockPos = mc.player.getBlockPos().down();
                        boolean flag5 = clientWorld.getBlockState(blockPos).isAir();
                        BooleanSetting booleanSetting3 = this.onlyWhenBack;
                        if (flag5) {
                            if (!booleanSetting3.getValue()) {
                                moveInputEvent.setSneak(true);
                                return;
                            }

                            float f = moveInputEvent.getForward();
                            if (0.0F >= f) {
                                moveInputEvent.setSneak(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean isGroundBelow(float f) {
        double d = mc.player.getY();
        if (!(0.0 > d)) {
            Box box = mc.player.getBoundingBox();
            double d2 = Float.intBitsToFloat(Float.floatToRawIntBits(f) ^ Integer.MIN_VALUE);
            Box box2 = box.offset(0.0, d2, 0.0);
            ClientWorld clientWorld = mc.world;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            return clientWorld.getBlockCollisions(clientPlayerEntity, box2).iterator().hasNext();
        } else {
            return false;
        }
    }

    public static MovementHelper getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(MovementHelper.class);
    }
}
