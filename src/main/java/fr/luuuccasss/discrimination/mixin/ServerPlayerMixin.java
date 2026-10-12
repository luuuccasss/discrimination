package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Origin;
import fr.luuuccasss.discrimination.Profiled;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements Profiled {
    @Unique private ResourceKey<VillagerType> discrimination$origin;

    @Override
    public ResourceKey<VillagerType> discrimination$getOrigin() {
        return discrimination$origin;
    }

    @Override
    public void discrimination$setOrigin(ResourceKey<VillagerType> origin) {
        discrimination$origin = origin;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void discrimination$write(ValueOutput output, CallbackInfo ci) {
        if (discrimination$origin != null) output.putString("DiscriminationOrigin", Origin.id(discrimination$origin));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void discrimination$read(ValueInput input, CallbackInfo ci) {
        input.getString("DiscriminationOrigin").ifPresent(id -> discrimination$origin = Origin.byId(id));
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void discrimination$restoreFrom(ServerPlayer old, boolean alive, CallbackInfo ci) {
        discrimination$origin = ((Profiled) old).discrimination$getOrigin();
    }
}
