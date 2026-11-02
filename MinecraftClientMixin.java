package dev.alek.pinktools.mixin;

import dev.alek.pinktools.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow private int itemUseCooldown;
    @Shadow @org.spongepowered.asm.mixin.Final public GameOptions options;

    private float pinktools$acc = 0f;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void pinktools$fastPlace(CallbackInfo ci) {
        Config c = Config.get();
        if (!c.fastPlace || !options.useKey.isPressed()) {
            pinktools$acc = 0f;
            return;
        }
        pinktools$acc += c.cps / 20f;
        if (pinktools$acc >= 1f) {
            pinktools$acc -= 1f;
            itemUseCooldown = 0;   // pozwól użyć w tym ticku
        } else {
            itemUseCooldown = 1;   // wstrzymaj
        }
    }
}
