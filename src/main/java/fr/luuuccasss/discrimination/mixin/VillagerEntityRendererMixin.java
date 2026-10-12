package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.client.AccessoryRenderState;
import fr.luuuccasss.discrimination.client.PrejudiceCache;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntityRenderer.class)
public abstract class VillagerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/passive/VillagerEntity;Lnet/minecraft/client/render/entity/state/VillagerEntityRenderState;F)V",
        at = @At("TAIL"))
    private void discrimination$copyPrejudice(VillagerEntity villager, VillagerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((AccessoryRenderState) state).discrimination$setPrejudice(PrejudiceCache.get(villager.getId()));
    }
}
