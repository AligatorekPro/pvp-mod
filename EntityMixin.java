package dev.alek.pinktools.mixin;

import dev.alek.pinktools.Config;
import dev.alek.pinktools.HitHighlight;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void pinktools$glow(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!cir.getReturnValue() && self.getWorld().isClient && HitHighlight.isActive(self)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void pinktools$color(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient && HitHighlight.isActive(self)) {
            cir.setReturnValue(Config.get().hitColor);
        }
    }
}
