package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Origin;
import fr.luuuccasss.discrimination.Profiled;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.village.VillagerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements Profiled {
    @Unique private RegistryKey<VillagerType> discrimination$origin;

    @Override
    public RegistryKey<VillagerType> discrimination$getOrigin() {
        return discrimination$origin;
    }

    @Override
    public void discrimination$setOrigin(RegistryKey<VillagerType> origin) {
        discrimination$origin = origin;
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void discrimination$write(WriteView view, CallbackInfo ci) {
        if (discrimination$origin != null) view.putString("DiscriminationOrigin", Origin.id(discrimination$origin));
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void discrimination$read(ReadView view, CallbackInfo ci) {
        view.getOptionalString("DiscriminationOrigin").ifPresent(id -> discrimination$origin = Origin.byId(id));
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void discrimination$copyFrom(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        discrimination$origin = ((Profiled) old).discrimination$getOrigin();
    }
}
