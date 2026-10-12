package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Origin;
import fr.luuuccasss.discrimination.Profiled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.VillagerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements Profiled {
    @Unique private VillagerType discrimination$origin;

    @Override
    public VillagerType discrimination$getOrigin() {
        return discrimination$origin;
    }

    @Override
    public void discrimination$setOrigin(VillagerType origin) {
        discrimination$origin = origin;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void discrimination$write(NbtCompound nbt, CallbackInfo ci) {
        if (discrimination$origin != null) nbt.putString("DiscriminationOrigin", Origin.id(discrimination$origin));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void discrimination$read(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("DiscriminationOrigin")) discrimination$origin = Origin.byId(nbt.getString("DiscriminationOrigin"));
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void discrimination$copyFrom(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        discrimination$origin = ((Profiled) old).discrimination$getOrigin();
    }
}
