package tech.sadnes.client.util.player;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import tech.sadnes.api.mixin.accessor.EntityAccessor;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.client.feature.impl.player.NoDelay;
import tech.sadnes.client.util.IMinecraft;

public final class MovementSimulator implements IMinecraft {
    private final ClientPlayerEntity player;
    private final World world;
    private final PlayerInput input;
    private final float yaw;
    private final float pitch;
    private final boolean sprinting;
    private final Object2DoubleMap<TagKey<Fluid>> fluidHeights;
    private final Set<TagKey<Fluid>> submergedFluids;
    private static int ticksSinceReset;
    private boolean moved;
    private Vec3d position;
    private Box boundingBox;
    private boolean onGround;
    private boolean touchingWater;
    private Vec3d velocity;
    private float fallDistance;
    private int jumpCooldown;
    private boolean jumping;
    private boolean gliding;
    private boolean horizontalCollision;
    private boolean swimming;
    private boolean submergedInWater;
    private float forwardInput;
    private float sidewaysInput;
    private int simulatedTicks;

    private MovementSimulator(ClientPlayerEntity clientPlayerEntity) {
        this.player = clientPlayerEntity;
        this.world = clientPlayerEntity.getEntityWorld();
        this.input = clientPlayerEntity.input.playerInput;
        this.position = clientPlayerEntity.getEntityPos();
        this.velocity = clientPlayerEntity.getVelocity();
        Box box = clientPlayerEntity.getBoundingBox();
        this.boundingBox = new Box(box.getMinPos(), box.getMaxPos());
        this.yaw = clientPlayerEntity.getYaw();
        this.pitch = clientPlayerEntity.getPitch();
        this.sprinting = clientPlayerEntity.isSprinting();
        this.fallDistance = (float)clientPlayerEntity.fallDistance;
        this.jumpCooldown = ((LivingEntityAccessor)clientPlayerEntity).sadnes$getJumpingCooldown();
        this.jumping = ((LivingEntityAccessor)clientPlayerEntity).sadnes$isJumping();
        this.gliding = clientPlayerEntity.isGliding();
        this.onGround = clientPlayerEntity.isOnGround();
        this.horizontalCollision = clientPlayerEntity.horizontalCollision;
        this.touchingWater = clientPlayerEntity.isTouchingWater();
        this.swimming = clientPlayerEntity.isSwimming();
        this.submergedInWater = clientPlayerEntity.isSubmergedInWater();
        this.fluidHeights = new Object2DoubleArrayMap<>(((EntityAccessor)clientPlayerEntity).sadnes$getFluidHeight());
        this.submergedFluids = new HashSet<>(((EntityAccessor)clientPlayerEntity).sadnes$getSubmergedFluidTag());
    }

    public static void incrementTicks() {
        ticksSinceReset = Math.min(ticksSinceReset + 1, 41);
    }

    public static void resetTicks() {
        ticksSinceReset = 0;
    }

    public static boolean isRecentlyReset() {
        return ticksSinceReset <= 10;
    }

    public static MovementSimulator simulate(int i) {
        MovementSimulator movementSimulator = new MovementSimulator(mc.player);

        for (int i2 = 0; i2 < i; i2++) {
            movementSimulator.tick();
        }

        return movementSimulator;
    }

    private void tick() {
        this.simulatedTicks++;
        if (!(this.position.y <= -70.0)) {
            this.updateInput();
            this.updateWaterState();
            this.updateSubmergedState();
            this.updateSwimming();
            if (this.jumpCooldown > 0) {
                this.jumpCooldown--;
            }

            this.jumping = this.input.jump();
            double d = Math.abs(this.velocity.x) < 0.003 ? 0.0 : this.velocity.x;
            double d2 = Math.abs(this.velocity.y) < 0.003 ? 0.0 : this.velocity.y;
            double d3 = Math.abs(this.velocity.z) < 0.003 ? 0.0 : this.velocity.z;
            if (this.onGround) {
                this.gliding = false;
            }

            this.velocity = new Vec3d(d, d2, d3);
            if (this.jumping) {
                double d4 = this.isInLava() ? this.fluidHeights.getDouble(FluidTags.LAVA) : this.fluidHeights.getDouble(FluidTags.WATER);
                boolean flag = this.touchingWater && d4 > 0.0;
                double d5 = this.getSwimHeight();
                boolean flag2 = this.onGround && !(d4 > d5);
                if ((!flag || flag2) && (!this.isInLava() || flag2)) {
                    if ((this.onGround || flag && d4 <= d5) && this.jumpCooldown == 0) {
                        this.jump();
                        NoDelay noDelay = NoDelay.getInstance();
                        if (noDelay == null || !noDelay.isJumpDelayRemoved()) {
                            this.jumpCooldown = 10;
                        }
                    }
                } else {
                    this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
                }
            }

            if (this.hasEffect(StatusEffects.SLOW_FALLING) || this.hasEffect(StatusEffects.LEVITATION)) {
                this.fallDistance = 0.0F;
            }

            this.travel(new Vec3d(this.sidewaysInput * 0.98F, 0.0, this.forwardInput * 0.98F));
        }
    }

    private void updateInput() {
        this.forwardInput = this.input.forward() == this.input.backward() ? 0.0F : (this.input.forward() ? 1.0F : -1.0F);
        this.sidewaysInput = this.input.left() == this.input.right() ? 0.0F : (this.input.left() ? 1.0F : -1.0F);
        if (this.input.sneak()) {
            this.sidewaysInput *= 0.3F;
            this.forwardInput *= 0.3F;
        }
    }

    private void travel(Vec3d vec3d) {
        if (this.swimming && !this.player.hasVehicle()) {
            double d = this.getRotationVector().y;
            double d2 = d < -0.2 ? 0.085 : 0.06;
            BlockPos blockPos = new BlockPos(MathHelper.floor(this.position.x), MathHelper.floor(this.position.y + 1.0 - 0.1), MathHelper.floor(this.position.z));
            if (d <= 0.0 || this.input.jump() || !this.world.getBlockState(blockPos).getFluidState().isEmpty()) {
                this.velocity = this.velocity.add(0.0, (d - this.velocity.y) * d2, 0.0);
            }
        }

        double d3 = this.velocity.y;
        double d4 = 0.08;
        boolean flag = this.velocity.y <= 0.0;
        if (this.velocity.y <= 0.0 && this.hasEffect(StatusEffects.SLOW_FALLING)) {
            d4 = 0.01;
            this.fallDistance = 0.0F;
        }

        if (this.touchingWater && this.player.shouldSwimInFluids()) {
            double d5 = this.position.y;
            float f = this.sprinting ? 0.9F : 0.8F;
            float f2 = 0.02F;
            float f3 = (float)this.player.getAttributes().getValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.onGround) {
                f3 *= 0.5F;
            }

            if (f3 > 0.0F) {
                f += (0.54600006F - f) * f3 / 3.0F;
                f2 = 0.02F + (0.1F - 0.02F) * f3 / 3.0F;
            }

            if (this.hasEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }

            this.updateVelocity(f2, vec3d);
            this.move(this.velocity);
            Vec3d vec3d2 = this.velocity;
            if (this.horizontalCollision && this.isClimbing()) {
                vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
            }

            this.velocity = this.player.applyFluidMovingSpeed(d4, flag, vec3d2.multiply(f, 0.8, f));
            if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.position.y + d5, this.velocity.z)) {
                this.velocity = new Vec3d(this.velocity.x, 0.3, this.velocity.z);
            }
        } else if (this.isInLava() && this.player.shouldSwimInFluids()) {
            double d6 = this.position.y;
            this.updateVelocity(0.02F, vec3d);
            this.move(this.velocity);
            if (this.fluidHeights.getDouble(FluidTags.LAVA) <= this.getSwimHeight()) {
                this.velocity = this.player.applyFluidMovingSpeed(d4, flag, this.velocity.multiply(0.5, 0.8, 0.5));
            } else {
                this.velocity = this.velocity.multiply(0.5);
            }

            if (!this.player.hasNoGravity()) {
                this.velocity = this.velocity.add(0.0, -d4 / 4.0, 0.0);
            }

            if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.position.y + d6, this.velocity.z)) {
                this.velocity = new Vec3d(this.velocity.x, 0.3, this.velocity.z);
            }
        } else if (this.gliding) {
            this.travelGliding(d4);
        } else {
            BlockPos blockPos2 = this.getVelocityAffectingPos();
            float f4 = this.world.getBlockState(blockPos2).getBlock().getSlipperiness();
            float f5 = this.onGround ? f4 * 0.91F : 0.91F;
            Vec3d vec3d3 = this.applyMovementInput(vec3d, f4);
            double d7 = vec3d3.y;
            if (this.hasEffect(StatusEffects.LEVITATION)) {
                StatusEffectInstance statusEffectInstance = this.getEffect(StatusEffects.LEVITATION);
                if (statusEffectInstance != null) {
                    d7 += (0.05 * (statusEffectInstance.getAmplifier() + 1) - vec3d3.y) * 0.2;
                }
            } else if (this.world.isClient() && !this.world.isChunkLoaded(blockPos2)) {
                d7 = this.position.y > this.world.getBottomY() ? -0.1 : 0.0;
            } else if (!this.player.hasNoGravity()) {
                d7 -= d4;
            }

            this.velocity = this.player.hasNoDrag() ? new Vec3d(vec3d3.x, d7, vec3d3.z) : new Vec3d(vec3d3.x * f5, d7 * 0.98F, vec3d3.z * f5);
        }

        if (this.player.getAbilities().flying && !this.player.hasVehicle()) {
            this.velocity = new Vec3d(this.velocity.x, d3 * 0.6, this.velocity.z);
            this.fallDistance = 0.0F;
        }
    }

    private void travelGliding(double d) {
        if (this.velocity.y > -0.5) {
            this.fallDistance = 1.0F;
        }

        Vec3d vec3d = this.getRotationVector();
        float f = this.pitch * (float) (Math.PI / 180.0);
        double d2 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double d3 = this.velocity.horizontalLength();
        double d4 = vec3d.length();
        float f2 = MathHelper.cos(f);
        float f3 = (float)(f2 * (f2 * Math.min(1.0, d4 / 0.4)));
        Vec3d vec3d2 = this.velocity.add(0.0, d * (-1.0 + f3 * 0.75), 0.0);
        if (vec3d2.y < 0.0 && d2 > 0.0) {
            double d5 = vec3d2.y * -0.1 * f3;
            vec3d2 = vec3d2.add(vec3d.x * d5 / d2, d5, vec3d.z * d5 / d2);
        }

        if (f < 0.0F && d2 > 0.0) {
            double d6 = d3 * -MathHelper.sin(f) * 0.04;
            vec3d2 = vec3d2.add(-vec3d.x * d6 / d2, d6 * 3.2, -vec3d.z * d6 / d2);
        }

        if (d2 > 0.0) {
            vec3d2 = vec3d2.add((vec3d.x / d2 * d3 - vec3d2.x) * 0.1, 0.0, (vec3d.z / d2 * d3 - vec3d2.z) * 0.1);
        }

        this.velocity = vec3d2.multiply(0.99, 0.98, 0.99);
        this.move(this.velocity);
    }

    private Vec3d applyMovementInput(Vec3d vec3d, float f) {
        this.updateVelocity(this.getMovementSpeed(f), vec3d);
        this.velocity = this.applyClimbingSpeed(this.velocity);
        this.move(this.velocity);
        Vec3d vec3d2 = this.velocity;
        BlockState blockState = this.world.getBlockState(toBlockPos(this.position));
        boolean flag;
        if (blockState != null && blockState.isOf(Blocks.POWDER_SNOW)) {
            flag = PowderSnowBlock.canWalkOnPowderSnow(this.player);
        } else {
            flag = false;
        }

        if ((this.horizontalCollision || this.jumping) && (this.isClimbing() || flag)) {
            vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
        }

        return vec3d2;
    }

    private Vec3d applyClimbingSpeed(Vec3d vec3d) {
        if (!this.isClimbing()) {
            return vec3d;
        }

        this.fallDistance = 0.0F;
        double d = MathHelper.clamp(vec3d.x, -0.15F, 0.15F);
        double d2 = MathHelper.clamp(vec3d.z, -0.15F, 0.15F);
        double d3 = Math.max(vec3d.y, -0.15F);
        if (d3 < 0.0 && !this.world.getBlockState(toBlockPos(this.position)).isOf(Blocks.SCAFFOLDING) && this.player.isHoldingOntoLadder()) {
            d3 = 0.0;
        }

        return new Vec3d(d, d3, d2);
    }

    private void move(Vec3d vec3d) {
        Vec3d vec3d2 = this.adjustForCollisions(this.adjustForSneaking(vec3d));
        if (vec3d2.lengthSquared() > 1.0E-10) {
            this.moved = true;
            this.position = this.position.add(vec3d2);
            this.boundingBox = this.player.getDimensions(this.player.getPose()).getBoxAt(this.position);
        }

        boolean flag = !MathHelper.approximatelyEquals(vec3d.x, vec3d2.x);
        boolean flag2 = !MathHelper.approximatelyEquals(vec3d.z, vec3d2.z);
        boolean flag3 = vec3d.y != vec3d2.y;
        this.horizontalCollision = flag || flag2;
        this.onGround = flag3 && vec3d.y < 0.0;
        if (!this.touchingWater) {
            this.updateWaterState();
        }

        if (this.onGround) {
            this.fallDistance = 0.0F;
        } else if (vec3d.y < 0.0) {
            this.fallDistance = this.fallDistance - (float)vec3d.y;
        }

        if (this.horizontalCollision || flag3) {
            this.velocity = new Vec3d(flag ? 0.0 : this.velocity.x, this.onGround ? 0.0 : this.velocity.y, flag2 ? 0.0 : this.velocity.z);
        }
    }

    private Vec3d adjustForCollisions(Vec3d vec3d) {
        Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.position);
        List list = List.of();
        Vec3d vec3d2 = vec3d.lengthSquared() == 0.0 ? vec3d : Entity.adjustMovementForCollisions(this.player, vec3d, box, this.world, list);
        boolean flag = vec3d.x != vec3d2.x;
        boolean flag2 = vec3d.y != vec3d2.y;
        boolean flag3 = vec3d.z != vec3d2.z;
        boolean flag4 = this.onGround || flag2 && vec3d.y < 0.0;
        float f = this.player.getStepHeight();
        if (f > 0.0F && flag4 && (flag || flag3)) {
            Vec3d vec3d3 = Entity.adjustMovementForCollisions(this.player, new Vec3d(vec3d.x, f, vec3d.z), box, this.world, list);
            Vec3d vec3d4 = Entity.adjustMovementForCollisions(this.player, new Vec3d(0.0, f, 0.0), box.stretch(vec3d.x, 0.0, vec3d.z), this.world, list);
            Vec3d vec3d5 = Entity.adjustMovementForCollisions(this.player, new Vec3d(vec3d.x, 0.0, vec3d.z), box.offset(vec3d4), this.world, list).add(vec3d4);
            if (vec3d4.y < f && vec3d5.horizontalLengthSquared() > vec3d3.horizontalLengthSquared()) {
                vec3d3 = vec3d5;
            }

            return vec3d3.horizontalLengthSquared() <= vec3d2.horizontalLengthSquared()
                ? vec3d2
                : vec3d3.add(Entity.adjustMovementForCollisions(this.player, new Vec3d(0.0, -vec3d3.y + vec3d.y, 0.0), box.offset(vec3d3), this.world, list));
        } else {
            return vec3d2;
        }
    }

    private Vec3d adjustForSneaking(Vec3d vec3d) {
        if (this.input.sneak() && !(vec3d.y > 0.0) && this.isAboveGround()) {
            double d = vec3d.x;
            double d2 = vec3d.z;

            while (d != 0.0 && this.world.isSpaceEmpty(this.player, this.boundingBox.offset(d, -0.5, 0.0))) {
                if (d < 0.05 && d >= -0.05) {
                    d = 0.0;
                    break;
                }

                d += d > 0.0 ? -0.05 : 0.05;
            }

            while (d2 != 0.0 && this.world.isSpaceEmpty(this.player, this.boundingBox.offset(0.0, -0.5, d2))) {
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    break;
                }

                d2 += d2 > 0.0 ? -0.05 : 0.05;
            }

            while (d != 0.0 && d2 != 0.0 && this.world.isSpaceEmpty(this.player, this.boundingBox.offset(d, -0.5, d2))) {
                d = d < 0.05 && d >= -0.05 ? 0.0 : (d > 0.0 ? d - 0.05 : d + 0.05);
                if (d2 < 0.05 && d2 >= -0.05) {
                    d2 = 0.0;
                    break;
                }

                d2 += d2 > 0.0 ? -0.05 : 0.05;
            }

            return new Vec3d(d, vec3d.y, d2);
        } else {
            return vec3d;
        }
    }

    private boolean isAboveGround() {
        return this.onGround || this.fallDistance < 0.5 && !this.world.isSpaceEmpty(this.player, this.boundingBox.offset(0.0, this.fallDistance - 0.5, 0.0));
    }

    private void jump() {
        float f = 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoost();
        this.velocity = this.velocity.add(0.0, f - this.velocity.y, 0.0);
        if (this.sprinting) {
            float f2 = (float)Math.toRadians(this.yaw);
            this.velocity = this.velocity.add(-MathHelper.sin(f2) * 0.2, 0.0, MathHelper.cos(f2) * 0.2);
        }
    }

    private float getJumpVelocityMultiplier() {
        float f = this.world.getBlockState(toBlockPos(this.position)).getBlock().getJumpVelocityMultiplier();
        float f2 = this.world.getBlockState(this.getVelocityAffectingPos()).getBlock().getJumpVelocityMultiplier();
        return f == 1.0F ? f2 : f;
    }

    private float getJumpBoost() {
        return !this.hasEffect(StatusEffects.JUMP_BOOST) ? 0.0F : 0.1F * (this.getEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
    }

    private float getMovementSpeed(float f) {
        if (this.onGround) {
            return 0.1F * (0.21600002F / (f * f * f));
        } else {
            return this.input.sprint() ? 0.026F : 0.02F;
        }
    }

    private void updateVelocity(float f, Vec3d vec3d) {
        this.velocity = this.velocity.add(EntityAccessor.sadnes$invokeMovementInputToVelocity(vec3d, f, this.yaw));
    }

    private void updateWaterState() {
        Entity entity = this.player.getVehicle();
        if (entity instanceof BoatEntity && !((BoatEntity)entity).isSubmergedInWater()) {
            this.touchingWater = false;
        } else {
            this.touchingWater = this.updateFluidMovement(FluidTags.WATER, 0.014);
            if (this.touchingWater) {
                this.fallDistance = 0.0F;
            }
        }
    }

    private void updateSubmergedState() {
        this.submergedInWater = this.submergedFluids.contains(FluidTags.WATER);
        this.submergedFluids.clear();
        double d = this.position.y + this.player.getEyeHeight(this.player.getPose()) - 0.11111111F;
        if (!(
            this.player.getVehicle() instanceof BoatEntity boatEntity
                && !boatEntity.isSubmergedInWater()
                && boatEntity.getBoundingBox().maxY >= d
                && boatEntity.getBoundingBox().minY <= d
        )) {
            BlockPos blockPos = BlockPos.ofFloored(this.position.x, d, this.position.z);
            FluidState fluidState = this.world.getFluidState(blockPos);
            double d2 = blockPos.getY() + fluidState.getHeight(this.world, blockPos);
            if (d2 > d) {
                this.submergedFluids.addAll(fluidState.streamTags().toList());
            }
        }
    }

    private void updateSwimming() {
        if (this.swimming) {
            if (this.sprinting && this.touchingWater && !this.player.hasVehicle()) {
                this.swimming = true;
            } else {
                this.swimming = false;
            }
        } else if (this.sprinting
            && this.submergedInWater
            && this.touchingWater
            && !this.player.hasVehicle()
            && this.world.getFluidState(toBlockPos(this.position)).isIn(FluidTags.WATER)) {
            this.swimming = true;
        } else {
            this.swimming = false;
        }
    }

    private boolean updateFluidMovement(TagKey<Fluid> tagKey, double d) {
        Box box = this.boundingBox.expand(1.0);
        boolean flag = !this.world
            .isRegionLoaded(MathHelper.floor(box.minX), MathHelper.floor(box.minZ), MathHelper.ceil(box.maxX), MathHelper.ceil(box.maxZ));
        if (flag) {
            return false;
        }

        Box box2 = this.boundingBox.contract(0.001);
        int i = MathHelper.floor(box2.minX);
        int i2 = MathHelper.ceil(box2.maxX);
        int i3 = MathHelper.floor(box2.minY);
        int i4 = MathHelper.ceil(box2.maxY);
        int i5 = MathHelper.floor(box2.minZ);
        int i6 = MathHelper.ceil(box2.maxZ);
        double d2 = 0.0;
        boolean flag2 = false;
        Vec3d vec3d = Vec3d.ZERO;
        int i7 = 0;
        Mutable mutable = new Mutable();

        for (int i8 = i; i8 < i2; i8++) {
            for (int i9 = i3; i9 < i4; i9++) {
                for (int i10 = i5; i10 < i6; i10++) {
                    mutable.set(i8, i9, i10);
                    FluidState fluidState = this.world.getFluidState(mutable);
                    if (fluidState.isIn(tagKey)) {
                        double d3 = i9 + fluidState.getHeight(this.world, mutable);
                        if (!(d3 < box2.minY)) {
                            flag2 = true;
                            d2 = Math.max(d3 - box2.minY, d2);
                            Vec3d vec3d2 = fluidState.getVelocity(this.world, mutable);
                            if (d2 < 0.4) {
                                vec3d2 = vec3d2.multiply(d2);
                            }

                            vec3d = vec3d.add(vec3d2);
                            i7++;
                        }
                    }
                }
            }
        }

        if (vec3d.length() > 0.0) {
            if (i7 > 0) {
                vec3d = vec3d.multiply(1.0 / i7);
            }

            Vec3d vec3d3 = vec3d.multiply(d);
            if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && vec3d3.length() < 0.0045) {
                vec3d3 = vec3d3.normalize().multiply(0.0045);
            }

            this.velocity = this.velocity.add(vec3d3);
        }

        this.fluidHeights.put(tagKey, d2);
        return flag2;
    }

    private boolean isClimbing() {
        BlockPos blockPos = toBlockPos(this.position);
        BlockState blockState = this.world.getBlockState(blockPos);
        return blockState.isIn(BlockTags.CLIMBABLE) ? true : blockState.getBlock() instanceof TrapdoorBlock && this.canClimbTrapdoor(blockPos, blockState);
    }

    private boolean canClimbTrapdoor(BlockPos blockPos, BlockState blockState) {
        if (!blockState.get(TrapdoorBlock.OPEN)) {
            return false;
        }

        BlockState blockState2 = this.world.getBlockState(blockPos.down());
        return blockState2.isOf(Blocks.LADDER) && blockState2.get(LadderBlock.FACING).equals(blockState.get(TrapdoorBlock.FACING));
    }

    private boolean doesNotCollide(double d, double d2, double d3) {
        Box box = this.boundingBox.offset(d, d2, d3);
        return this.world.isSpaceEmpty(this.player, box) && !this.world.containsFluid(box);
    }

    private Vec3d getRotationVector() {
        float f = (float)(this.pitch * Math.PI / 180.0);
        float f2 = (float)(-this.yaw * Math.PI / 180.0);
        float f3 = MathHelper.cos(f2);
        float f4 = MathHelper.sin(f2);
        float f5 = MathHelper.cos(f);
        float f6 = MathHelper.sin(f);
        return new Vec3d(f4 * f5, -f6, f3 * f5);
    }

    private BlockPos getVelocityAffectingPos() {
        return BlockPos.ofFloored(this.position.x, this.boundingBox.minY - 0.5000001, this.position.z);
    }

    private static BlockPos toBlockPos(Vec3d vec3d) {
        return new BlockPos(MathHelper.floor(vec3d.x), MathHelper.floor(vec3d.y), MathHelper.floor(vec3d.z));
    }

    private boolean isInLava() {
        return this.fluidHeights.getDouble(FluidTags.LAVA) > 0.0;
    }

    private double getSwimHeight() {
        return this.player.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
    }

    private boolean hasEffect(RegistryEntry<StatusEffect> registryEntry) {
        return this.getEffect(registryEntry) != null;
    }

    private StatusEffectInstance getEffect(RegistryEntry<StatusEffect> registryEntry) {
        StatusEffectInstance statusEffectInstance = this.player.getStatusEffect(registryEntry);
        return statusEffectInstance != null && statusEffectInstance.getDuration() >= this.simulatedTicks ? statusEffectInstance : null;
    }

    public boolean hasMoved() {
        return this.moved;
    }

    public Vec3d getPosition() {
        return this.position;
    }

    public Box getBoundingBox() {
        return this.boundingBox;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isTouchingWater() {
        return this.touchingWater;
    }

    public float getFallDistance() {
        return this.fallDistance;
    }
}
