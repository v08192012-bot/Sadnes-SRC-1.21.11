package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.event.CancellableEvent;

public final class AttackEntityEvent extends CancellableEvent {
    private final PlayerEntity player;
    private final Entity target;
    private final AttackEntityEvent.Stage stage;
    private int weaponSlot = -1;

    public PlayerEntity getPlayer() {
        return this.player;
    }

    public Entity getTarget() {
        return this.target;
    }

    public AttackEntityEvent.Stage getStage() {
        return this.stage;
    }

    public int getWeaponSlot() {
        return this.weaponSlot;
    }

    public AttackEntityEvent(PlayerEntity playerEntity, Entity entity, AttackEntityEvent.Stage stage2) {
        this.player = playerEntity;
        this.target = entity;
        this.stage = stage2;
    }

    public void setWeaponSlot(int i) {
        this.weaponSlot = i;
    }

    public enum Stage {
        PRE,
        POST;
    }
}
