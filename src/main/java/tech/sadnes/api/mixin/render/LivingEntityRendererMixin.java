package tech.sadnes.api.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.ChinaHatHolder;
import tech.sadnes.api.duck.LabelHideable;
import tech.sadnes.api.duck.TransparentAlphaHolder;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.movement.Airstuck;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.feature.impl.render.ChinaHat;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.feature.impl.render.HitColor;
import tech.sadnes.client.feature.impl.render.SeeInvisibles;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.network.BackendService;
import tech.sadnes.client.party.PartyManager;
import tech.sadnes.client.party.PartyMemberView;
import tech.sadnes.client.render.ChinaHatPhysics;
import tech.sadnes.client.render.ChinaHatRenderer;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final ThreadLocal<LivingEntityRenderState> sadnes$state = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> sadnes$hitColored = ThreadLocal.withInitial(() -> false);

    @Shadow
    protected abstract void setupTransforms(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack, float f, float f2);

    @Shadow
    protected abstract void scale(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack);

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$setState(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo callbackInfo) {
        sadnes$state.set(livingEntityRenderState);
        TransparentPlayers transparentPlayers = TransparentPlayers.getInstance();
        if (transparentPlayers != null && transparentPlayers.isTransparent(livingEntityRenderState)) {
            TransparentPlayers.setRenderAlpha(transparentPlayers.getAlpha(livingEntityRenderState));
        } else {
            TransparentPlayers.setRenderAlpha(1.0F);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void sadnes$clearState(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo callbackInfo) {
        sadnes$state.remove();
        sadnes$hitColored.set(false);
        TransparentPlayers.resetRenderAlpha();
    }

    @Unique
    private static boolean sadnes$isParty(String string) {
        BackendService backendService = Sadnes.getInstance().getBackend();
        PartyManager partyManager = backendService != null ? backendService.getPartyManager() : null;
        if (partyManager != null && partyManager.inParty()) {
            for (PartyMemberView partyMemberView : partyManager.getMemberViews()) {
                if (string.equalsIgnoreCase(partyMemberView.username())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sadnes$chinaHat(LivingEntityRenderState livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo callbackInfo) {
        ChinaHatPhysics chinaHatPhysics = ((ChinaHatHolder)livingEntityRenderState).sadnes$chinaHat();
        if (chinaHatPhysics != null) {
            ChinaHat chinaHat = ChinaHat.getInstance();
            if (chinaHat != null && chinaHat.isEnabled() && ((LivingEntityRenderer)(Object)this).getModel() instanceof ModelWithHead modelWithHead) {
                matrixStack.push();
                if (livingEntityRenderState.isInPose(EntityPose.SLEEPING)) {
                    Direction direction = livingEntityRenderState.sleepingDirection;
                    if (direction != null) {
                        float f = livingEntityRenderState.standingEyeHeight - 0.1F;
                        matrixStack.translate(-direction.getOffsetX() * f, 0.0F, -direction.getOffsetZ() * f);
                    }
                }

                float f2 = livingEntityRenderState.baseScale;
                matrixStack.scale(f2, f2, f2);
                this.setupTransforms(livingEntityRenderState, matrixStack, livingEntityRenderState.bodyYaw, f2);
                matrixStack.scale(-1.0F, -1.0F, 1.0F);
                this.scale(livingEntityRenderState, matrixStack);
                matrixStack.translate(0.0F, -1.501F, 0.0F);
                modelWithHead.getHead().applyTransform(matrixStack);
                float f3 = chinaHat.getHeightOffset(
                    ((ChinaHatHolder)livingEntityRenderState).sadnes$chinaHatHelmet(), livingEntityRenderState.getData(Cosmetics.COSMETICS_KEY), FiguraAvatarManager.getHeadHeight(livingEntityRenderState)
                );
                matrixStack.translate(0.0F, -f3, 0.0F);
                matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180.0F));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                ChinaHatRenderer.render(matrixStack, chinaHat.getHatStyle(), chinaHat.getAlpha(), chinaHat.getColor(), chinaHat.getRadius(), chinaHatPhysics);
                matrixStack.pop();
            }
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void sadnes$renderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Baritone baritone = Baritone.getInstance();
        if (baritone != null && baritone.isFreeLooking() && livingEntity == minecraftClient.player) {
            livingEntityRenderState.pitch = baritone.getInterpolatedPitch(f);
            livingEntityRenderState.bodyYaw = baritone.getInterpolatedYaw(f);
            livingEntityRenderState.relativeHeadYaw = 0.0F;
        }

        Airstuck airstuck = Airstuck.getInstance();
        if (airstuck != null && airstuck.isEnabled() && airstuck.isStateSaved() && livingEntity == minecraftClient.player) {
            livingEntityRenderState.pitch = airstuck.getSavedPitch();
            livingEntityRenderState.bodyYaw = airstuck.getSavedBodyYaw();
            livingEntityRenderState.relativeHeadYaw = MathHelper.wrapDegrees(airstuck.getSavedYaw() - airstuck.getSavedBodyYaw());
        }

        SeeInvisibles seeInvisibles = SeeInvisibles.getInstance();
        if (seeInvisibles != null && seeInvisibles.shouldRender(livingEntity)) {
            livingEntityRenderState.invisibleToPlayer = false;
        }

        TransparentPlayers transparentPlayers = TransparentPlayers.getInstance();
        ((TransparentAlphaHolder)livingEntityRenderState).sadnes$setTransparentAlpha(transparentPlayers != null ? transparentPlayers.getSmoothedAlpha(livingEntity, livingEntityRenderState) : 1.0F);
        ((LabelHideable)livingEntityRenderState).sadnes$setHideLabel(false);
        ((ChinaHatHolder)livingEntityRenderState).sadnes$setChinaHat(null);
        if (livingEntity instanceof PlayerEntity playerEntity) {
            Cosmetics cosmetics = Cosmetics.getInstance();
            livingEntityRenderState.setData(Cosmetics.COSMETICS_KEY, cosmetics != null ? cosmetics.getCosmetics(playerEntity) : null);
            ChinaHat chinaHat = ChinaHat.getInstance();
            if (chinaHat != null && chinaHat.isEnabled()) {
                boolean flag = livingEntity == minecraftClient.player;
                boolean flag2;
                if (flag) {
                    flag2 = chinaHat.shouldRender(true, false, false);
                } else {
                    String string = playerEntity.getGameProfile().name();
                    flag2 = chinaHat.shouldRender(false, Sadnes.getInstance().getFriendManager().isFriend(string), sadnes$isParty(string));
                }

                if (flag2) {
                    ((ChinaHatHolder)livingEntityRenderState).sadnes$setChinaHat(chinaHat.updateHatState(livingEntity, livingEntityRenderState.bodyYaw + livingEntityRenderState.relativeHeadYaw));
                    ((ChinaHatHolder)livingEntityRenderState).sadnes$setChinaHatHelmet(!playerEntity.getEquippedStack(EquipmentSlot.HEAD).isEmpty());
                }
            }

            Esp esp = Esp.getInstance();
            if (esp != null && esp.isEnabled()) {
                EspTargetType espTargetType = Esp.getTargetType(playerEntity);
                if (espTargetType != null) {
                    for (EspElement espElement : esp.getElements(espTargetType)) {
                        if (espElement.usesItems() && espElement.isActive()) {
                            livingEntityRenderState.displayName = null;
                            ((LabelHideable)livingEntityRenderState).sadnes$setHideLabel(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
        ),
        index = 2
    )
    private boolean sadnes$transparentPlayersLayer(boolean flag) {
        LivingEntityRenderState livingEntityRenderState = sadnes$state.get();
        TransparentPlayers transparentPlayers = TransparentPlayers.getInstance();
        return flag || livingEntityRenderState != null && transparentPlayers != null && transparentPlayers.isTransparent(livingEntityRenderState);
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        ),
        index = 5
    )
    private int sadnes$hitColorOverlay(int i) {
        HitColor hitColor = HitColor.getInstance();
        boolean flag = i >>> 16 == 3;
        boolean flag2;
        if (hitColor != null && hitColor.isEnabled()) {
            flag2 = flag;
        } else {
            flag2 = false;
        }

        sadnes$hitColored.set(flag2);
        return flag2 ? OverlayTexture.DEFAULT_UV : i;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
        ),
        index = 6
    )
    private int sadnes$playerAlpha(int i) {
        if (Boolean.TRUE.equals(sadnes$hitColored.get())) {
            sadnes$hitColored.set(false);
            HitColor hitColor = HitColor.getInstance();
            if (hitColor != null && hitColor.isEnabled()) {
                return hitColor.getColor();
            }
        }

        LivingEntityRenderState livingEntityRenderState = sadnes$state.get();
        if (livingEntityRenderState == null) {
            return i;
        }

        TransparentPlayers transparentPlayers = TransparentPlayers.getInstance();
        if (transparentPlayers != null && transparentPlayers.isTransparent(livingEntityRenderState)) {
            return transparentPlayers.applyStateAlpha(i, livingEntityRenderState);
        }

        SeeInvisibles seeInvisibles = SeeInvisibles.getInstance();
        return seeInvisibles != null && seeInvisibles.shouldRenderState(livingEntityRenderState) ? seeInvisibles.applyAlpha(i) : i;
    }
}
