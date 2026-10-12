package fr.luuuccasss.discrimination.mixin;

import fr.luuuccasss.discrimination.Prejudice;
import fr.luuuccasss.discrimination.client.AccessoryRenderState;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerEntityRenderState.class)
public abstract class VillagerEntityRenderStateMixin implements AccessoryRenderState {
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
