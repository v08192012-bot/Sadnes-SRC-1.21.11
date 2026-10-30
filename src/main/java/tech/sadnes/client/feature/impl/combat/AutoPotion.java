package tech.sadnes.client.feature.impl.combat;

import java.lang.reflect.Array;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.SelfPearlUtil;

@FeatureInfo(
    name = "AutoPotion",
    description = "feature.impl.combat.autopotion",
    displayName = "feature.impl.combat.autopotion",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_INVENTORY
)
public final class AutoPotion extends Feature {
    private final MultiSetting potions = new MultiSetting(
        "autopotion.potions",
        BooleanSetting.of("strength", true),
        BooleanSetting.of("speed", true),
        BooleanSetting.of("fire_resistance", true),
        BooleanSetting.of("healing", true),
        BooleanSetting.of("regeneration", true)
    );
    private final NumberSetting healHealth = new NumberSetting("autopotion.healHealth", 8.0F, 1.0F, 20.0F, 1.0F).setVisibleWhen(() -> this.potions.has("healing"));
    private final ModeSetting regenTrigger = new ModeSetting("autopotion.regenTrigger", new String[]{"no_effect", "health"})
        .setVisibleWhen(() -> this.potions.has("regeneration"));
    private final NumberSetting regenHealth = new NumberSetting("autopotion.regenHealth", 8.0F, 1.0F, 20.0F, 1.0F)
        .setVisibleWhen(() -> this.potions.has("regeneration") && this.regenTrigger.is("health") ? true : false);
    private final BooleanSetting onlyOnGround = new BooleanSetting("autopotion.onlyOnGround", true);
    private final BooleanSetting smartThrow = new BooleanSetting("autopotion.smartThrow", true);
    private final BooleanSetting heal = new BooleanSetting("autopotion.heal", false);
    private final ModeSetting healMode = new ModeSetting("autopotion.healMode", new String[]{"health", "bind"}).setVisibleWhen(this.heal::getValue);
    private final NumberSetting healHp = new NumberSetting("autopotion.healHp", 8.0F, 2.0F, 16.0F, 1.0F)
        .setVisibleWhen(() -> this.heal.getValue() && this.healMode.is("health") ? true : false);
    private final KeybindSetting healBind = new KeybindSetting("autopotion.healBind")
        .setVisibleWhen(() -> this.heal.getValue() && this.healMode.is("bind") ? true : false);
    private final StopWatch throwTimer = new StopWatch();
    private final StopWatch healThrowTimer = new StopWatch();
    private final StopWatch rotationTimer = new StopWatch();
    private long healActiveUntil;
    private boolean healToggled;
    private int rotateTicks;
    private int throwTicks;
    private boolean thrown;
    private AutoPotion.PotionType currentPotion;
    private Slot currentSlot;
    private Vector2f throwRotation;
    private Vector2f healRotation;
    private final Map<AutoPotion.PotionType, Long> lastThrowTimes = new EnumMap<>(AutoPotion.PotionType.class);

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean flag = ServerManager.isNullWorld();
        int i = this.throwTicks;
        Vector2f vector2f = this.throwRotation;
        boolean flag2 = this.thrown;
        Slot slot = this.currentSlot;
        AutoPotion.PotionType potionType = this.currentPotion;
        int i2 = this.rotateTicks;
        int i3;
        int i4;
        Vector2f vector2f2;
        Slot slot2;
        Object object;
        if (!flag) {
            int i5 = mc.player.age;
            int i6 = this.throwTicks;
            Vector2f vector2f3 = this.throwRotation;
            flag2 = this.thrown;
            Slot slot3 = this.currentSlot;
            AutoPotion.PotionType potionType2 = this.currentPotion;
            int i7 = this.rotateTicks;
            if (i5 > 79) {
                boolean flag3 = this.handleHealPotion();
                int i8 = this.throwTicks;
                Vector2f vector2f4 = this.throwRotation;
                flag2 = this.thrown;
                slot2 = this.currentSlot;
                AutoPotion.PotionType potionType3 = this.currentPotion;
                BooleanSetting booleanSetting = this.onlyOnGround;
                int i9 = this.rotateTicks;
                if (flag3) {
                    i3 = i8;
                    i4 = i9;
                    vector2f2 = vector2f4;
                    object = potionType3;
                } else {
                    label128:
                    if (i8 > 0) {
                        int i10;
                        label126: {
                            float f = vector2f4.x;
                            float f2 = this.throwRotation.y;
                            RotationComponent.rotateTo(new Rotation(f, f2), 180.0F, 90.0F, 2, 10);
                            int i11 = this.throwTicks;
                            Vector2f vector2f5 = this.throwRotation;
                            flag2 = this.thrown;
                            Slot slot4 = this.currentSlot;
                            AutoPotion.PotionType potionType4 = this.currentPotion;
                            int i12 = this.rotateTicks;
                            if (flag2) {
                                i4 = i12;
                                vector2f2 = vector2f5;
                                slot2 = slot4;
                                object = potionType4;
                                i10 = i11 - 1;
                                if (i11 - 1 != 0) {
                                    break label126;
                                }
                            } else {
                                boolean flag4 = this.isRotationReached(vector2f5);
                                int i13 = this.throwTicks;
                                Vector2f vector2f6 = this.throwRotation;
                                flag2 = this.thrown;
                                Slot slot5 = this.currentSlot;
                                AutoPotion.PotionType potionType5 = this.currentPotion;
                                int i14 = this.rotateTicks;
                                if (flag4) {
                                    if (!this.canThrowAt(vector2f6)) {
                                        this.resetThrowState();
                                        int i15 = this.throwTicks;
                                        Vector2f vector2f7 = this.throwRotation;
                                        flag2 = this.thrown;
                                        Slot slot6 = this.currentSlot;
                                        AutoPotion.PotionType potionType6 = this.currentPotion;
                                        int i16 = this.rotateTicks;
                                        i3 = i15;
                                        i4 = i16;
                                        vector2f2 = vector2f7;
                                        slot2 = slot6;
                                        object = potionType6;
                                        break label128;
                                    }

                                    ThrowComponent throwComponent = ThrowComponent.getInstance();
                                    int i17 = this.throwTicks;
                                    Vector2f vector2f8 = this.throwRotation;
                                    flag2 = this.thrown;
                                    slot2 = this.currentSlot;
                                    AutoPotion.PotionType potionType7 = this.currentPotion;
                                    int i18 = this.rotateTicks;
                                    if (throwComponent != null) {
                                        throwComponent.useSlot(slot2);
                                        Map map = this.lastThrowTimes;
                                        AutoPotion.PotionType potionType8 = this.currentPotion;
                                        Long longValue = System.currentTimeMillis();
                                        map.put(potionType8, longValue);
                                        this.throwTimer.reset();
                                        int i19 = this.throwTicks;
                                        Vector2f vector2f9 = this.throwRotation;
                                        Slot slot7 = this.currentSlot;
                                        AutoPotion.PotionType potionType9 = this.currentPotion;
                                        int i20 = this.rotateTicks;
                                        flag2 = true;
                                        i4 = i20;
                                        vector2f2 = vector2f9;
                                        slot2 = slot7;
                                        object = potionType9;
                                        i10 = i19 - 1;
                                        if (i19 - 1 != 0) {
                                            flag2 = true;
                                            i3 = i10;
                                            break label128;
                                        }
                                    } else {
                                        i4 = i18;
                                        vector2f2 = vector2f8;
                                        object = potionType7;
                                        i10 = i17 - 1;
                                        if (i17 - 1 != 0) {
                                            break label126;
                                        }
                                    }
                                } else {
                                    i4 = i14;
                                    vector2f2 = vector2f6;
                                    slot2 = slot5;
                                    object = potionType5;
                                    i10 = i13 - 1;
                                    if (i13 - 1 != 0) {
                                        break label126;
                                    }
                                }
                            }

                            this.throwTicks = i10;
                            this.thrown = flag2;
                            this.resetThrowState();
                            int i21 = this.throwTicks;
                            Vector2f vector2f10 = this.throwRotation;
                            flag2 = this.thrown;
                            Slot slot8 = this.currentSlot;
                            AutoPotion.PotionType potionType10 = this.currentPotion;
                            int i22 = this.rotateTicks;
                            i3 = i21;
                            i4 = i22;
                            vector2f2 = vector2f10;
                            slot2 = slot8;
                            object = potionType10;
                            break label128;
                        }

                        i3 = i10;
                    } else {
                        label133: {
                            boolean flag5 = booleanSetting.getValue();
                            StopWatch stopWatch = this.throwTimer;
                            if (flag5) {
                                boolean flag6 = mc.player.isOnGround();
                                int i23 = this.throwTicks;
                                Vector2f vector2f11 = this.throwRotation;
                                flag2 = this.thrown;
                                Slot slot9 = this.currentSlot;
                                AutoPotion.PotionType potionType11 = this.currentPotion;
                                int i24 = this.rotateTicks;
                                if (!flag6) {
                                    i3 = i23;
                                    i4 = i24;
                                    vector2f2 = vector2f11;
                                    slot2 = slot9;
                                    object = potionType11;
                                    break label133;
                                }
                            }

                            boolean flag7 = stopWatch.hasPassed(1.0);
                            i3 = this.throwTicks;
                            flag2 = this.thrown;
                            Slot slot10 = this.currentSlot;
                            AutoPotion.PotionType potionType12 = this.currentPotion;
                            Slot slot11;
                            AutoPotion.PotionType potionType13;
                            if (flag7) {
                                boolean flag8 = this.isAuraAttacking();
                                i3 = this.throwTicks;
                                flag2 = this.thrown;
                                Slot slot12 = this.currentSlot;
                                AutoPotion.PotionType potionType14 = this.currentPotion;
                                if (flag8) {
                                    slot2 = slot12;
                                    i4 = 0;
                                    vector2f2 = null;
                                    object = potionType14;
                                    break label133;
                                }

                                AutoPotion.PotionType[] potionTypes = AutoPotion.PotionType.values();
                                int i25 = Array.getLength(potionTypes);
                                int i26 = this.throwTicks;
                                Vector2f vector2f12 = this.throwRotation;
                                flag2 = this.thrown;
                                Slot slot13 = this.currentSlot;
                                AutoPotion.PotionType potionType15 = this.currentPotion;
                                int i27 = this.rotateTicks;
                                if (i25 > 0) {
                                    int i28 = i25 & 2147483647;
                                    long l = 0L;
                                    int i29 = i26;
                                    int i30 = i27;
                                    Vector2f vector2f13 = vector2f12;
                                    Slot slot14 = slot13;
                                    potionType13 = potionType15;

                                    while (true) {
                                        this.throwTicks = i29;
                                        this.throwRotation = vector2f13;
                                        this.thrown = flag2;
                                        this.currentSlot = slot14;
                                        this.currentPotion = potionType13;
                                        this.rotateTicks = i30;
                                        int i31 = (int)l;
                                        Object object2 = Array.get(potionTypes, i31);
                                        MultiSetting multiSetting = this.potions;
                                        String string = ((AutoPotion.PotionType)object2).settingKey;
                                        boolean flag9 = multiSetting.has(string);
                                        int i32 = this.throwTicks;
                                        Vector2f vector2f14 = this.throwRotation;
                                        boolean flag10 = this.thrown;
                                        Slot slot15 = this.currentSlot;
                                        AutoPotion.PotionType potionType16 = this.currentPotion;
                                        int i33 = this.rotateTicks;
                                        boolean flag11;
                                        if (flag9) {
                                            boolean flag12 = this.needsPotion((AutoPotion.PotionType)object2);
                                            int i34 = this.throwTicks;
                                            Vector2f vector2f15 = this.throwRotation;
                                            boolean flag13 = this.thrown;
                                            Slot slot16 = this.currentSlot;
                                            AutoPotion.PotionType potionType17 = this.currentPotion;
                                            int i35 = this.rotateTicks;
                                            if (flag12) {
                                                long l2 = System.currentTimeMillis();
                                                Map map2 = this.lastThrowTimes;
                                                Long longValue2 = 0L;
                                                long l3 = (Long)map2.getOrDefault(object2, longValue2);
                                                int i36 = this.throwTicks;
                                                Vector2f vector2f16 = this.throwRotation;
                                                boolean flag14 = this.thrown;
                                                Slot slot17 = this.currentSlot;
                                                AutoPotion.PotionType potionType18 = this.currentPotion;
                                                int i37 = this.rotateTicks;
                                                if (l2 - l3 >= 1200L) {
                                                    Slot slot18 = this.findPotionSlot((AutoPotion.PotionType)object2);
                                                    int i38 = this.throwTicks;
                                                    Vector2f vector2f17 = this.throwRotation;
                                                    flag2 = this.thrown;
                                                    Slot slot19 = this.currentSlot;
                                                    potionType13 = this.currentPotion;
                                                    int i39 = this.rotateTicks;
                                                    StopWatch stopWatch2 = this.rotationTimer;
                                                    if (slot18 != null) {
                                                        if (object2 != null) {
                                                            Vector2f vector2f18;
                                                            Slot slot20;
                                                            AutoPotion.PotionType potionType19;
                                                            label90: {
                                                                Vector2f vector2f19;
                                                                Vector2f vector2f20;
                                                                if (vector2f17 == null) {
                                                                    boolean flag15 = stopWatch2.hasPassedAndReset(200.0);
                                                                    int i40 = this.throwTicks;
                                                                    Vector2f vector2f21 = this.throwRotation;
                                                                    flag2 = this.thrown;
                                                                    Slot slot21 = this.currentSlot;
                                                                    AutoPotion.PotionType potionType20 = this.currentPotion;
                                                                    if (flag15) {
                                                                        Vector2f vector2f22 = this.computeThrowRotation();
                                                                        int i41 = this.throwTicks;
                                                                        flag2 = this.thrown;
                                                                        Slot slot22 = this.currentSlot;
                                                                        AutoPotion.PotionType potionType21 = this.currentPotion;
                                                                        i3 = i41;
                                                                        vector2f18 = vector2f22;
                                                                        slot20 = slot22;
                                                                        potionType19 = potionType21;
                                                                        if (vector2f22 == null) {
                                                                            break label90;
                                                                        }

                                                                        vector2f19 = vector2f22;
                                                                        vector2f20 = vector2f22;
                                                                    } else {
                                                                        i3 = i40;
                                                                        vector2f18 = vector2f21;
                                                                        slot20 = slot21;
                                                                        potionType19 = potionType20;
                                                                        if (vector2f21 == null) {
                                                                            break label90;
                                                                        }

                                                                        vector2f19 = vector2f21;
                                                                        vector2f20 = vector2f21;
                                                                    }
                                                                } else {
                                                                    vector2f19 = vector2f17;
                                                                    vector2f20 = vector2f17;
                                                                }

                                                                this.throwRotation = vector2f20;
                                                                float f3 = vector2f19.x;
                                                                float f4 = this.throwRotation.y;
                                                                RotationComponent.rotateTo(new Rotation(f3, f4), 180.0F, 90.0F, 2, 10);
                                                                Vector2f vector2f23 = this.throwRotation;
                                                                int i42 = this.rotateTicks + 1;
                                                                this.rotateTicks = i42;
                                                                boolean flag16 = this.isRotationReached(vector2f23);
                                                                int i43 = this.throwTicks;
                                                                Vector2f vector2f24 = this.throwRotation;
                                                                flag2 = this.thrown;
                                                                Slot slot23 = this.currentSlot;
                                                                AutoPotion.PotionType potionType22 = this.currentPotion;
                                                                int i44 = this.rotateTicks;
                                                                if (!flag16) {
                                                                    if (i44 >= 12) {
                                                                        flag2 = false;
                                                                        i3 = 4;
                                                                        i4 = i44;
                                                                        vector2f2 = vector2f24;
                                                                        slot2 = slot18;
                                                                        object = object2;
                                                                    } else {
                                                                        i3 = i43;
                                                                        i4 = i44;
                                                                        vector2f2 = vector2f24;
                                                                        slot2 = slot23;
                                                                        object = potionType22;
                                                                    }
                                                                } else {
                                                                    flag2 = false;
                                                                    i3 = 4;
                                                                    i4 = i44;
                                                                    vector2f2 = vector2f24;
                                                                    slot2 = slot18;
                                                                    object = object2;
                                                                }
                                                                break label133;
                                                            }

                                                            vector2f2 = vector2f18;
                                                            i4 = 0;
                                                            slot2 = slot20;
                                                            object = potionType19;
                                                        } else {
                                                            i3 = i38;
                                                            i4 = 0;
                                                            vector2f2 = null;
                                                            slot2 = slot19;
                                                            object = potionType13;
                                                        }
                                                        break label133;
                                                    }

                                                    flag11 = flag2;
                                                    i29 = i38;
                                                    i30 = i39;
                                                    vector2f13 = vector2f17;
                                                    slot14 = slot19;
                                                } else {
                                                    vector2f13 = vector2f16;
                                                    flag11 = flag14;
                                                    i29 = i36;
                                                    i30 = i37;
                                                    slot14 = slot17;
                                                    potionType13 = potionType18;
                                                }
                                            } else {
                                                vector2f13 = vector2f15;
                                                flag11 = flag13;
                                                i29 = i34;
                                                i30 = i35;
                                                slot14 = slot16;
                                                potionType13 = potionType17;
                                            }
                                        } else {
                                            vector2f13 = vector2f14;
                                            flag11 = flag10;
                                            i29 = i32;
                                            i30 = i33;
                                            slot14 = slot15;
                                            potionType13 = potionType16;
                                        }

                                        if (Integer.toUnsignedLong(i28) == ++l) {
                                            flag2 = flag11;
                                            i3 = i29;
                                            slot11 = slot14;
                                            i4 = 0;
                                            break;
                                        }

                                        flag2 = flag11;
                                    }
                                } else {
                                    i3 = i26;
                                    slot11 = slot13;
                                    potionType13 = potionType15;
                                    i4 = 0;
                                }
                            } else {
                                slot11 = slot10;
                                potionType13 = potionType12;
                                i4 = 0;
                            }

                            vector2f2 = null;
                            slot2 = slot11;
                            object = potionType13;
                        }
                    }
                }
            } else {
                i3 = i6;
                i4 = i7;
                vector2f2 = vector2f3;
                slot2 = slot3;
                object = potionType2;
            }
        } else {
            i3 = i;
            i4 = i2;
            vector2f2 = vector2f;
            slot2 = slot;
            object = potionType;
        }

        this.throwTicks = i3;
        this.throwRotation = vector2f2;
        this.thrown = flag2;
        this.currentSlot = slot2;
        AutoPotion.PotionType potionType23 = (AutoPotion.PotionType)object;
        this.currentPotion = potionType23;
        this.rotateTicks = i4;
    }

    private Vector2f computeThrowRotation() {
        if (this.smartThrow.getValue()) {
            return SelfPearlUtil.findBestRotation();
        }

        float f = mc.player.getYaw();
        return new Vector2f(f, 90.0F);
    }

    private boolean canThrowAt(Vector2f vector2f) {
        return this.smartThrow.getValue() ? SelfPearlUtil.getLandingScore(vector2f) > 0.0 : true;
    }

    private void resetThrowState() {
        this.throwTicks = 0;
        this.currentPotion = null;
        this.currentSlot = null;
        this.thrown = false;
        this.rotateTicks = 0;
        this.throwRotation = null;
    }

    private boolean isRotationReached(Vector2f vector2f) {
        float f = mc.player.getYaw();
        float f2 = vector2f.x;
        float f3 = Math.abs(MathHelper.wrapDegrees(ExactMath.subtract(f, f2)));
        if (1.5F > f3) {
            float f4 = mc.player.getPitch();
            float f5 = vector2f.y;
            float f6 = Math.abs(ExactMath.subtract(f4, f5));
            return 1.5F > f6;
        } else {
            return false;
        }
    }

    private boolean isAuraAttacking() {
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null) {
            if (attackAura.isEnabled()) {
                return attackAura.target != null ? mc.player.getAttackCooldownProgress(1.0F) > 0.5F : false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean needsPotion(AutoPotion.PotionType potionType) {
        float f = mc.player.getHealth();
        float f2 = mc.player.getAbsorptionAmount();
        int i = potionType.ordinal();
        NumberSetting numberSetting = this.healHealth;
        ModeSetting modeSetting = this.regenTrigger;
        float f3 = ExactMath.add(f, f2);
        if (i != 4) {
            if (i == 3) {
                return numberSetting.getValue() > f3;
            }

            ClientPlayerEntity clientPlayerEntity = mc.player;
            RegistryEntry registryEntry = potionType.effect;
            return !clientPlayerEntity.hasStatusEffect(registryEntry);
        } else {
            boolean flag = modeSetting.is("health");
            NumberSetting numberSetting2 = this.regenHealth;
            if (flag) {
                if (!(numberSetting2.getValue() > f3)) {
                    return false;
                }
            } else {
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                RegistryEntry registryEntry2 = potionType.effect;
                if (clientPlayerEntity2.hasStatusEffect(registryEntry2)) {
                    return false;
                }
            }

            return true;
        }
    }

    private Slot findPotionSlot(AutoPotion.PotionType potionType) {
        long l = 0L;

        do {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = (int)l;
            ItemStack itemStack = playerInventory.getStack(i);
            RegistryEntry registryEntry = potionType.effect;
            if (this.hasPotionEffect(itemStack, registryEntry)) {
                ScreenHandler screenHandler = mc.player.currentScreenHandler;
                if (Long.compareUnsigned(l, 9L) < 0) {
                    int i2 = (int)l + 36;
                    int i3 = (int)Integer.toUnsignedLong(i2);
                    return screenHandler.getSlot(i3);
                }

                int i4 = (int)l;
                return screenHandler.getSlot(i4);
            }
        } while (++l != 36L);

        return null;
    }

    private boolean hasPotionEffect(ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry) {
        if (itemStack.getItem() instanceof SplashPotionItem) {
            ComponentType componentType = DataComponentTypes.POTION_CONTENTS;
            PotionContentsComponent potionContentsComponent = PotionContentsComponent.DEFAULT;
            Iterator iterator2 = itemStack.getOrDefault(componentType, potionContentsComponent).getEffects().iterator();

            while (iterator2.hasNext()) {
                if (((StatusEffectInstance)iterator2.next()).getEffectType() == registryEntry) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean handleHealPotion() {
        boolean flag = this.heal.getValue();
        ModeSetting modeSetting = this.healMode;
        Vector2f vector2f = this.healRotation;
        boolean flag2;
        Vector2f vector2f2;
        boolean flag3;
        if (flag) {
            boolean flag4 = modeSetting.is("bind");
            boolean flag5 = this.healToggled;
            if (!flag4) {
                flag5 = false;
            }

            long l;
            label69: {
                this.healToggled = flag5;
                if (modeSetting.is("bind")) {
                    boolean flag6 = this.isHealBindHeld();
                    l = this.healActiveUntil;
                    if (!flag6) {
                        break label69;
                    }
                } else {
                    float f = mc.player.getHealth();
                    float f2 = this.healHp.getValue();
                    l = this.healActiveUntil;
                    if (!(f2 >= f)) {
                        break label69;
                    }
                }

                long l2 = System.currentTimeMillis();
                l = 500L + l2;
            }

            this.healActiveUntil = l;
            long l3 = System.currentTimeMillis();
            flag2 = this.healToggled;
            long l4 = this.healActiveUntil;
            if (l3 < l4) {
                Slot slot = this.findHealingPotionSlot();
                flag2 = this.healToggled;
                Vector2f vector2f3 = this.healRotation;
                StopWatch stopWatch = this.rotationTimer;
                label63:
                if (slot != null) {
                    Vector2f vector2f4;
                    if (vector2f3 != null) {
                        vector2f4 = vector2f3;
                    } else {
                        boolean flag7 = stopWatch.hasPassedAndReset(200.0);
                        flag2 = this.healToggled;
                        vector2f4 = this.healRotation;
                        if (flag7) {
                            vector2f4 = this.computeThrowRotation();
                            flag2 = this.healToggled;
                            if (vector2f4 == null) {
                                vector2f2 = vector2f4;
                                flag3 = true;
                                break label63;
                            }

                            vector2f3 = vector2f4;
                        } else {
                            if (vector2f4 == null) {
                                vector2f2 = vector2f4;
                                flag3 = true;
                                break label63;
                            }

                            vector2f3 = vector2f4;
                        }
                    }

                    this.healRotation = vector2f4;
                    float f3 = vector2f3.x;
                    float f4 = this.healRotation.y;
                    RotationComponent.rotateTo(new Rotation(f3, f4), 180.0F, 90.0F, 2, 10);
                    Vector2f vector2f5 = this.healRotation;
                    boolean flag8 = this.isRotationReached(vector2f5);
                    flag2 = this.healToggled;
                    Vector2f vector2f6 = this.healRotation;
                    StopWatch stopWatch2 = this.healThrowTimer;
                    if (flag8) {
                        double d = Double.longBitsToDouble(modeSetting.is("bind") ? 4639200197400854528L : 4636033603912859648L);
                        boolean flag9 = stopWatch2.hasPassed(d);
                        flag2 = this.healToggled;
                        Vector2f vector2f7 = this.healRotation;
                        label52:
                        if (flag9) {
                            boolean flag10 = this.canThrowAt(vector2f7);
                            flag2 = this.healToggled;
                            if (flag10) {
                                ThrowComponent throwComponent = ThrowComponent.getInstance();
                                flag2 = this.healToggled;
                                Vector2f vector2f8 = this.healRotation;
                                if (throwComponent == null) {
                                    vector2f2 = vector2f8;
                                    flag3 = true;
                                    break label52;
                                }

                                throwComponent.useSlot(slot);
                                stopWatch2.reset();
                                flag2 = this.healToggled;
                                flag3 = true;
                            } else {
                                flag3 = true;
                            }

                            vector2f2 = null;
                        } else {
                            vector2f2 = vector2f7;
                            flag3 = true;
                        }
                    } else {
                        flag3 = true;
                        vector2f2 = vector2f6;
                    }
                } else {
                    vector2f2 = vector2f3;
                    flag3 = false;
                }
            } else {
                vector2f2 = null;
                flag3 = false;
            }
        } else {
            flag2 = false;
            vector2f2 = vector2f;
            flag3 = false;
        }

        this.healToggled = flag2;
        this.healRotation = vector2f2;
        return flag3;
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getScreen() == null) {
            int i = screenMouseClickEvent.getButton();
            this.handleHealBindPress(i);
        }
    }

    @EventHandler
    public void onMouse(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && screenKeyEvent.getScreen() == null) {
            int i = screenKeyEvent.getKey();
            this.handleHealBindPress(i);
        }
    }

    private void handleHealBindPress(int i) {
        boolean flag = this.heal.getValue();
        ModeSetting modeSetting = this.healMode;
        if (flag) {
            boolean flag2 = modeSetting.is("bind");
            KeybindSetting keybindSetting = this.healBind;
            if (flag2) {
                Iterator iterator2 = keybindSetting.getBinds().iterator();
                if (iterator2.hasNext()) {
                    do {
                        AutoPotion autoPotion;
                        boolean flag3;
                        label31: {
                            ValueBind valueBind = (ValueBind)iterator2.next();
                            BindMode bindMode = valueBind.getMode();
                            BindMode bindMode2 = BindMode.TOGGLE;
                            boolean flag4 = this.healToggled;
                            boolean flag5;
                            if (bindMode == bindMode2) {
                                boolean flag6 = valueBind.matches(i);
                                boolean flag7 = this.healToggled;
                                if (flag6) {
                                    if (flag7) {
                                        autoPotion = this;
                                        flag3 = false;
                                    } else {
                                        autoPotion = this;
                                        flag3 = true;
                                    }
                                    break label31;
                                }

                                flag5 = flag7;
                            } else {
                                flag5 = flag4;
                            }

                            autoPotion = this;
                            flag3 = flag5;
                        }

                        autoPotion.healToggled = flag3;
                    } while (iterator2.hasNext());
                }
            }
        }
    }

    private Slot findHealingPotionSlot() {
        long l = 0L;
        Slot slot = null;

        do {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = (int)l;
            ItemStack itemStack = playerInventory.getStack(i);
            if (itemStack.getItem() instanceof SplashPotionItem) {
                ComponentType componentType = DataComponentTypes.POTION_CONTENTS;
                PotionContentsComponent potionContentsComponent = PotionContentsComponent.DEFAULT;
                Iterator iterator2 = itemStack.getOrDefault(componentType, potionContentsComponent).getEffects().iterator();
                if (iterator2.hasNext()) {
                    byte b = 0;
                    byte b2 = 1;

                    while (true) {
                        RegistryEntry registryEntry = ((StatusEffectInstance)iterator2.next()).getEffectType();
                        RegistryEntry registryEntry2 = StatusEffects.INSTANT_HEALTH;
                        boolean flag = iterator2.hasNext();
                        boolean flag2 = registryEntry == registryEntry2;
                        boolean flag3 = registryEntry != registryEntry2;
                        byte b3 = flag2 ? 1 : b;
                        byte b4 = flag3 ? 0 : b2;
                        if (!flag) {
                            if (b3 != 0) {
                                ScreenHandler screenHandler = mc.player.currentScreenHandler;
                                boolean flag4 = Long.compareUnsigned(l, 9L) >= 0;
                                int i2 = (int)l + 36;
                                int i3 = (int)(flag4 ? l : Integer.toUnsignedLong(i2));
                                Slot slot2 = screenHandler.getSlot(i3);
                                if (b4 != 0) {
                                    return slot2;
                                }

                                slot = slot == null ? slot2 : slot;
                            }
                            break;
                        }

                        b = b3;
                        b2 = b4;
                    }
                }
            }
        } while (++l != 36L);

        return slot;
    }

    private boolean isHealBindHeld() {
        Iterator iterator2 = this.healBind.getBinds().iterator();
        boolean flag = iterator2.hasNext();
        boolean flag2 = this.healToggled;
        boolean flag3;
        if (flag) {
            do {
                ValueBind valueBind = (ValueBind)iterator2.next();
                BindMode bindMode = valueBind.getMode();
                BindMode bindMode2 = BindMode.HOLD;
                if (bindMode == bindMode2 && valueBind.isHeld()) {
                    return true;
                }

                flag3 = iterator2.hasNext();
                flag2 = this.healToggled;
            } while (flag3);
        }

        return flag2;
    }

    enum PotionType {
        STRENGTH("strength", StatusEffects.STRENGTH),
        SPEED("speed", StatusEffects.SPEED),
        FIRE_RESISTANCE("fire_resistance", StatusEffects.FIRE_RESISTANCE),
        HEALING("healing", StatusEffects.INSTANT_HEALTH),
        REGENERATION("regeneration", StatusEffects.REGENERATION);

        final String settingKey;
        final RegistryEntry<StatusEffect> effect;

        PotionType(String string, RegistryEntry<StatusEffect> registryEntry) {
            this.settingKey = string;
            this.effect = registryEntry;
        }
    }
}
