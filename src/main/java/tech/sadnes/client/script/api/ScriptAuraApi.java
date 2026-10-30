package tech.sadnes.client.script.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptAuraApi {
    private ScriptAuraApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "rotation", objects -> {
            ScriptAuraHooks.registerRotation(script, ScriptValues.argString(objects, 0), ScriptValues.argFunction(objects, 1));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "esp", objects2 -> {
            ScriptAuraHooks.registerEsp(script, ScriptValues.argString(objects2, 0), ScriptValues.argFunction(objects2, 1));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "rangeHook", objects3 -> {
            ScriptAuraHooks.addRangeHook(script, ScriptValues.argFunction(objects3, 0));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "hitMomentHook", objects4 -> {
            ScriptAuraHooks.addHitMomentHook(script, ScriptValues.argFunction(objects4, 0));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "track", objects5 -> {
            Entity entity = ScriptPlayerApi.unwrapEntity(ScriptValues.hasArg(objects5, 0) ? objects5[0] : null);
            if (entity instanceof LivingEntity) {
                ScriptAuraHooks.setTarget(script, (LivingEntity)entity);
                return null;
            } else {
                ScriptAuraHooks.setTarget(script, null);
                return null;
            }
        });
        ScriptValues.putFunction(nativeObject, "target", objects6 -> {
            LivingEntity livingEntity = getCurrentTarget();
            return livingEntity == null ? null : ScriptPlayerApi.wrapEntity(livingEntity);
        });
        ScriptValues.putFunction(nativeObject, "range", objects7 -> {
            AttackAura attackAura = AttackAura.getInstance();
            return attackAura == null ? 3.0 : attackAura.getAttackRange();
        });
        ScriptValues.putFunction(nativeObject, "enabled", objects8 -> {
            AttackAura attackAura2 = AttackAura.getInstance();
            return attackAura2 != null && attackAura2.isEnabled() ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "shouldAttack", objects9 -> {
            AttackAura attackAura3 = AttackAura.getInstance();
            return attackAura3 != null && attackAura3.canAttack() ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "cooldown", objects10 -> AttackTimer.getCooldownProgress());
        ScriptValues.putFunction(nativeObject, "angleTo", objects11 -> {
            LivingEntity livingEntity2 = resolveTarget(objects11);
            if (livingEntity2 == null) {
                return null;
            }

            AttackAura attackAura4 = AttackAura.getInstance();
            float f = (float)(attackAura4 == null ? 3.0 : attackAura4.getAttackRange());
            Vector4f vector4f = CombatUtil.getRotationsTo(livingEntity2, f, false);
            return wrapRotation(vector4f.x, vector4f.y);
        });
        ScriptValues.putFunction(nativeObject, "visibleAngleTo", objects12 -> {
            LivingEntity livingEntity3 = resolveTarget(objects12);
            if (livingEntity3 == null) {
                return null;
            }

            AttackAura attackAura5 = AttackAura.getInstance();
            float f2 = (float)(attackAura5 == null ? 3.0 : attackAura5.getAttackRange());
            Vec3d vec3d = AimPointUtil.getAimPoint(livingEntity3, f2, true);
            if (vec3d == null) {
                return null;
            }

            float[] floats = RotationUtil.getRotationArrayTo(vec3d);
            return wrapRotation(floats[0], floats[1]);
        });
        ScriptValues.putFunction(nativeObject, "lookVec", objects13 -> wrapVec(CombatUtil.getLookVector()));
        ScriptValues.putFunction(nativeObject, "aimDistance", objects14 -> {
            LivingEntity livingEntity4 = resolveTarget(objects14);
            return livingEntity4 == null ? 0.0 : CombatUtil.getRayDistance(livingEntity4);
        });
        ScriptValues.putFunction(
            nativeObject,
            "closestPoint",
            objects15 -> {
                LivingEntity livingEntity5 = resolveTarget(objects15);
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                if (livingEntity5 == null || minecraftClient.player == null) {
                    return null;
                }

                if (ScriptValues.hasArg(objects15, 1)) {
                    return wrapVec(
                        CombatUtil.getClosestPoint(
                            new Vec3d(ScriptValues.argDouble(objects15, 1, 0.0), ScriptValues.argDouble(objects15, 2, 0.0), ScriptValues.argDouble(objects15, 3, 0.0)),
                            livingEntity5
                        )
                    );
                }

                Vec3d vec3d2 = minecraftClient.player.getEyePos();
                return wrapVec(CombatUtil.getClosestPoint(vec3d2, livingEntity5));
            }
        );
        ScriptValues.putFunction(nativeObject, "bestVisiblePoint", objects16 -> {
            LivingEntity livingEntity6 = resolveTarget(objects16);
            if (livingEntity6 == null) {
                return null;
            }

            AttackAura attackAura6 = AttackAura.getInstance();
            float f3 = (float)(attackAura6 == null ? 3.0 : attackAura6.getAttackRange());
            Vec3d vec3d3 = AimPointUtil.getAimPoint(livingEntity6, f3, true);
            return vec3d3 == null ? null : wrapVec(vec3d3);
        });
        ScriptValues.putFunction(
            nativeObject,
            "angleToPoint",
            objects17 -> {
                float[] floats2 = RotationUtil.getRotationArrayTo(
                    new Vec3d(ScriptValues.argDouble(objects17, 0, 0.0), ScriptValues.argDouble(objects17, 1, 0.0), ScriptValues.argDouble(objects17, 2, 0.0))
                );
                return wrapRotation(floats2[0], floats2[1]);
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "canSeePoint",
            objects18 -> {
                MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
                return minecraftClient2.player != null
                        && AimPointUtil.canSee(
                            minecraftClient2.player,
                            new Vec3d(ScriptValues.argDouble(objects18, 0, 0.0), ScriptValues.argDouble(objects18, 1, 0.0), ScriptValues.argDouble(objects18, 2, 0.0))
                        )
                    ? true
                    : false;
            }
        );
        ScriptValues.putFunction(nativeObject, "rayTraceYaw", objects19 -> {
            LivingEntity livingEntity7 = resolveTarget(objects19);
            return livingEntity7 != null && RaycastUtil.isPitchFacing(livingEntity7, ScriptValues.argFloat(objects19, 1, 0.0F)) ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "rayTracePitch", objects20 -> {
            LivingEntity livingEntity8 = resolveTarget(objects20);
            return livingEntity8 != null && RaycastUtil.isYawFacing(livingEntity8, ScriptValues.argFloat(objects20, 1, 0.0F)) ? true : false;
        });
        scriptableObject.put("aura", scriptableObject, nativeObject);
    }

    private static NativeObject wrapVec(Vec3d vec3d) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", vec3d.x);
        ScriptValues.putValue(nativeObject, "y", vec3d.y);
        ScriptValues.putValue(nativeObject, "z", vec3d.z);
        return nativeObject;
    }

    private static NativeObject wrapRotation(float f, float f2) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "yaw", f);
        ScriptValues.putValue(nativeObject, "pitch", f2);
        return nativeObject;
    }

    private static LivingEntity resolveTarget(Object[] objects) {
        Entity entity = ScriptValues.hasArg(objects, 0) ? ScriptPlayerApi.unwrapEntity(objects[0]) : null;
        return entity instanceof LivingEntity ? (LivingEntity)entity : getCurrentTarget();
    }

    private static LivingEntity getCurrentTarget() {
        AttackAura attackAura = AttackAura.getInstance();
        return attackAura != null && MinecraftClient.getInstance().player != null ? attackAura.getCurrentTarget() : null;
    }
}
