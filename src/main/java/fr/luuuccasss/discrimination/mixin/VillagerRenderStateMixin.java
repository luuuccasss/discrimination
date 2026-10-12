package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Prejudice;
import fr.luuuccasss.discrimination.client.AccessoryRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerRenderState.class)
public abstract class VillagerRenderStateMixin implements AccessoryRenderState {
    @Unique private Prejudice discrimination$prejudice;

    @Override
    public Prejudice discrimination$getPrejudice() {
        return discrimination$prejudice;
    }

    @Override
    public void discrimination$setPrejudice(Prejudice prejudice) {
        discrimination$prejudice = prejudice;
    }
}
