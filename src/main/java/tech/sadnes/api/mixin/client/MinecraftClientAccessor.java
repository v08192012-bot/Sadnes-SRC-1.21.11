package tech.sadnes.api.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("currentScreen")
    Screen sadnes$getCurrentScreen();

    @Accessor("itemUseCooldown")
    void sadnes$setItemUseCooldown(int i);

    @Invoker("doItemUse")
    void sadnes$invokeDoItemUse();

    @Invoker("doAttack")
    boolean sadnes$invokeDoAttack();

    @Accessor("session")
    Session sadnes$getSession();

    @Mutable
    @Accessor("session")
    void sadnes$setSession(Session session);
}
