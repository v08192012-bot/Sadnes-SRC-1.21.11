package tech.sadnes.client.feature.impl.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.InventoryUtil;

final class FunTimeHitSwapHandler implements IMinecraft {
    private static FunTimeHitSwapHandler activeHandler;
    private FunTimeHitSwapHandler.SwapState state = FunTimeHitSwapHandler.SwapState.IDLE;
    private Slot weaponSlot;
    private ItemStack swappedStack = ItemStack.EMPTY;
    private int stateTicks;
    private long armedTime = -1L;

    void tick(boolean flag) {
        FunTimeHitSwapHandler.SwapState swapState = this.state;
        FunTimeHitSwapHandler.SwapState swapState2 = FunTimeHitSwapHandler.SwapState.IDLE;
        FunTimeHitSwapHandler.SwapState swapState3 = this.state;
        int i = this.stateTicks;
        if (swapState2 != swapState) {
            i++;
        }

        int i2;
        long l;
        FunTimeHitSwapHandler funTimeHitSwapHandler;
        FunTimeHitSwapHandler.SwapState swapState4;
        Slot slot;
        label55: {
            this.stateTicks = i;
            FunTimeHitSwapHandler.SwapState swapState5 = FunTimeHitSwapHandler.SwapState.IN_HAND;
            FunTimeHitSwapHandler.SwapState swapState6 = this.state;
            i2 = this.stateTicks;
            l = this.armedTime;
            Slot slot2 = this.weaponSlot;
            funTimeHitSwapHandler = activeHandler;
            if (swapState5 != swapState3) {
                FunTimeHitSwapHandler.SwapState swapState7 = FunTimeHitSwapHandler.SwapState.RETURNED;
                FunTimeHitSwapHandler.SwapState swapState8 = this.state;
                i2 = this.stateTicks;
                l = this.armedTime;
                Slot slot3 = this.weaponSlot;
                FunTimeHitSwapHandler funTimeHitSwapHandler2 = activeHandler;
                if (swapState7 != swapState6) {
                    long l2;
                    FunTimeHitSwapHandler.SwapState swapState9;
                    FunTimeHitSwapHandler funTimeHitSwapHandler3;
                    label47: {
                        l2 = AttackTimer.getTimer().getTime();
                        FunTimeHitSwapHandler.SwapState swapState10 = this.state;
                        FunTimeHitSwapHandler.SwapState swapState11 = FunTimeHitSwapHandler.SwapState.ARMED;
                        swapState9 = this.state;
                        i2 = this.stateTicks;
                        long l3 = this.armedTime;
                        slot = this.weaponSlot;
                        funTimeHitSwapHandler3 = activeHandler;
                        if (swapState11 == swapState10) {
                            if (!flag) {
                                l2 = l3;
                                break label47;
                            }

                            if (i2 >= 3) {
                                break label47;
                            }
                        } else {
                            if (!flag) {
                                swapState4 = swapState9;
                                l = l3;
                                funTimeHitSwapHandler = funTimeHitSwapHandler3;
                                break label55;
                            }

                            if (l2 != l3) {
                                this.state = swapState9;
                                this.stateTicks = i2;
                                this.armedTime = l3;
                                this.weaponSlot = slot;
                                activeHandler = funTimeHitSwapHandler3;
                                Slot slot4 = CombatUtil.findBestOffhandSlot();
                                FunTimeHitSwapHandler.SwapState swapState12 = this.state;
                                i2 = this.stateTicks;
                                l = this.armedTime;
                                slot = this.weaponSlot;
                                FunTimeHitSwapHandler funTimeHitSwapHandler4 = activeHandler;
                                if (slot4 != null) {
                                    this.weaponSlot = slot4;
                                    FunTimeHitSwapHandler.SwapState swapState13 = FunTimeHitSwapHandler.SwapState.ARMED;
                                    this.state = swapState13;
                                    this.stateTicks = 0;
                                    activeHandler = this;
                                    MovementPauseComponent.pause(this);
                                    FunTimeHitSwapHandler.SwapState swapState14 = this.state;
                                    i2 = this.stateTicks;
                                    l = this.armedTime;
                                    Slot slot5 = this.weaponSlot;
                                    FunTimeHitSwapHandler funTimeHitSwapHandler5 = activeHandler;
                                    swapState4 = swapState14;
                                    slot = slot5;
                                    funTimeHitSwapHandler = funTimeHitSwapHandler5;
                                } else {
                                    swapState4 = swapState12;
                                    funTimeHitSwapHandler = funTimeHitSwapHandler4;
                                }
                                break label55;
                            }
                        }

                        swapState4 = swapState9;
                        l = l3;
                        funTimeHitSwapHandler = funTimeHitSwapHandler3;
                        break label55;
                    }

                    this.state = swapState9;
                    this.stateTicks = i2;
                    this.armedTime = l2;
                    this.weaponSlot = slot;
                    activeHandler = funTimeHitSwapHandler3;
                    this.reset();
                    FunTimeHitSwapHandler.SwapState swapState15 = this.state;
                    i2 = this.stateTicks;
                    l = this.armedTime;
                    Slot slot6 = this.weaponSlot;
                    FunTimeHitSwapHandler funTimeHitSwapHandler6 = activeHandler;
                    swapState4 = swapState15;
                    slot = slot6;
                    funTimeHitSwapHandler = funTimeHitSwapHandler6;
                    break label55;
                }

                swapState4 = swapState8;
                slot = slot3;
                funTimeHitSwapHandler = funTimeHitSwapHandler2;
                if (i2 < 2) {
                    break label55;
                }
            } else {
                swapState4 = swapState6;
                slot = slot2;
                if (i2 < 2) {
                    break label55;
                }
            }

            this.reset();
            FunTimeHitSwapHandler.SwapState swapState16 = this.state;
            i2 = this.stateTicks;
            l = this.armedTime;
            Slot slot7 = this.weaponSlot;
            FunTimeHitSwapHandler funTimeHitSwapHandler7 = activeHandler;
            swapState4 = swapState16;
            slot = slot7;
            funTimeHitSwapHandler = funTimeHitSwapHandler7;
        }

        this.state = swapState4;
        this.stateTicks = i2;
        this.armedTime = l;
        this.weaponSlot = slot;
        activeHandler = funTimeHitSwapHandler;
    }

    AttackAura.AttackHooks createSwapAction(boolean flag) {
        FunTimeHitSwapHandler.SwapState swapState = this.state;
        FunTimeHitSwapHandler.SwapState swapState2 = FunTimeHitSwapHandler.SwapState.ARMED;
        Slot slot = this.weaponSlot;
        if (!flag) {
            if (swapState2 != swapState) {
                return AttackAura.AttackHooks.NONE;
            }

            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
            if (screenHandler != playerScreenHandler2) {
                return AttackAura.AttackHooks.NONE;
            }

            double d = InventoryUtil.getAttackDamage(slot.getStack());
            double d2 = InventoryUtil.getAttackDamage(mc.player.getOffHandStack());
            if (d > d2) {
                int i = slot.id;
                Runnable runnable = () -> {
                    if (ServerManager.getFunTimeAnarchy() <= 100) {
                        InventoryUtil.clickSlot(i, 40, SlotActionType.SWAP);
                        this.state = FunTimeHitSwapHandler.SwapState.RETURNED;
                    }
                };
                Runnable runnable2 = () -> {
                    this.swappedStack = slot.getStack().copy();
                    InventoryUtil.clickSlot(i, 40, SlotActionType.SWAP);
                    this.state = FunTimeHitSwapHandler.SwapState.IN_HAND;
                    this.stateTicks = 0;
                };
                return new AttackAura.AttackHooks(runnable2, runnable);
            }
        } else {
            if (swapState2 != swapState) {
                return AttackAura.AttackHooks.NONE;
            }

            ScreenHandler screenHandler2 = mc.player.currentScreenHandler;
            PlayerScreenHandler playerScreenHandler3 = mc.player.playerScreenHandler;
            if (screenHandler2 != playerScreenHandler3) {
                return AttackAura.AttackHooks.NONE;
            }

            double d3 = InventoryUtil.getAttackDamage(slot.getStack());
            double d4 = InventoryUtil.getAttackDamage(mc.player.getOffHandStack());
            if (d3 > d4) {
                return AttackAura.AttackHooks.NONE;
            }
        }

        return AttackAura.AttackHooks.NONE;
    }

    void onAttackFinished() {
        if (this.state == FunTimeHitSwapHandler.SwapState.IN_HAND || this.state == FunTimeHitSwapHandler.SwapState.RETURNED) {
            this.reset();
        }
    }

    void reset() {
        FunTimeHitSwapHandler.SwapState swapState = this.state;
        FunTimeHitSwapHandler.SwapState swapState2 = FunTimeHitSwapHandler.SwapState.IDLE;
        ItemStack itemStack = this.swappedStack;
        FunTimeHitSwapHandler funTimeHitSwapHandler = activeHandler;
        ItemStack itemStack2;
        FunTimeHitSwapHandler funTimeHitSwapHandler2;
        if (swapState2 != swapState) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            FunTimeHitSwapHandler.SwapState swapState3 = this.state;
            FunTimeHitSwapHandler.SwapState swapState4;
            boolean flag;
            if (clientPlayerEntity != null) {
                ItemStack itemStack3 = mc.player.getOffHandStack();
                ItemStack itemStack4 = this.swappedStack;
                boolean flag2 = ItemStack.areItemsAndComponentsEqual(itemStack3, itemStack4);
                swapState4 = this.state;
                if (flag2) {
                    flag = true;
                } else {
                    FunTimeHitSwapHandler.SwapState swapState5 = swapState4;
                    flag = false;
                    swapState4 = swapState5;
                }
            } else {
                FunTimeHitSwapHandler.SwapState swapState6 = swapState3;
                flag = false;
                swapState4 = swapState6;
            }

            FunTimeHitSwapHandler.SwapState swapState7 = FunTimeHitSwapHandler.SwapState.IN_HAND;
            FunTimeHitSwapHandler.SwapState swapState8 = this.state;
            Slot slot = this.weaponSlot;
            FunTimeHitSwapHandler.SwapState swapState9;
            if (swapState7 != swapState4) {
                swapState9 = swapState8;
            } else if (flag) {
                int i = slot.id;
                SlotActionType slotActionType = SlotActionType.SWAP;
                InventoryUtil.clickSlot(i, 40, slotActionType);
                swapState9 = this.state;
            } else {
                swapState9 = swapState8;
            }

            if (FunTimeHitSwapHandler.SwapState.ARMED != swapState9) {
                MovementPauseComponent.closeAndResume(this);
            } else {
                MovementPauseComponent.resume(this);
            }

            FunTimeHitSwapHandler.SwapState swapState10 = FunTimeHitSwapHandler.SwapState.IDLE;
            this.state = swapState10;
            this.weaponSlot = null;
            itemStack2 = ItemStack.EMPTY;
            funTimeHitSwapHandler2 = activeHandler;
            if (funTimeHitSwapHandler2 == this) {
                funTimeHitSwapHandler2 = null;
            }
        } else {
            itemStack2 = itemStack;
            funTimeHitSwapHandler2 = funTimeHitSwapHandler;
        }

        this.swappedStack = itemStack2;
        activeHandler = funTimeHitSwapHandler2;
    }

    static boolean isActive() {
        return activeHandler != null;
    }

    static boolean isSwapBlocked() {
        AutoMace autoMace = AutoMace.getInstance();
        AutoTotem autoTotem = AutoTotem.getInstance();
        if (autoTotem != null && autoTotem.isEnabled()) {
            if (autoTotem.isInDanger()) {
                if (mc.currentScreen != null) {
                    return true;
                }

                if (autoMace != null) {
                    autoMace.isActive();
                    return true;
                }

                return true;
            }

            if (mc.currentScreen != null) {
                return true;
            }

            if (autoMace == null) {
                return GuiMoveComponent.isActive() || !MovementPauseComponent.hasPendingTasks();
            }
        } else {
            if (mc.currentScreen != null) {
                return true;
            }

            if (autoMace == null) {
                return GuiMoveComponent.isActive() || !MovementPauseComponent.hasPendingTasks();
            }
        }

        return autoMace.isActive() ? true : GuiMoveComponent.isActive() || !MovementPauseComponent.hasPendingTasks();
    }

    enum SwapState {
        IDLE,
        ARMED,
        IN_HAND,
        RETURNED;
    }
}
