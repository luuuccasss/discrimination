package fr.luuuccasss.discrimination;

import net.minecraft.registry.RegistryKey;
import net.minecraft.village.VillagerType;

/** Interface ajoutée à ServerPlayerEntity par mixin : le biome d'origine du joueur. */
public interface Profiled {
    /** Null tant que le joueur n'a pas encore été « recensé ». */
    RegistryKey<VillagerType> discrimination$getOrigin();

    void discrimination$setOrigin(RegistryKey<VillagerType> origin);
}
