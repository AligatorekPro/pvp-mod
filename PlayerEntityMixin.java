package dev.alek.pinktools.mixin;

import dev.alek.pinktools.Config;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void pinktools$friendColor(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!self.getWorld().isClient) return;
        Config c = Config.get();
        if (!c.nickHighlight || !c.isFriend(self.getName().getString())) return;
        TextColor color = TextColor.fromRgb(c.nickColor);
        cir.setReturnValue(cir.getReturnValue().copy().styled(s -> s.withColor(color)));
    }
}
