package fr.luuuccasss.discrimination;

import net.minecraft.village.VillagerType;

/** Interface ajoutée à ServerPlayerEntity par mixin : le biome d'origine du joueur. */
public interface Profiled {
    /** Null tant que le joueur n'a pas encore été « recensé ». */
    VillagerType discrimination$getOrigin();

    void discrimination$setOrigin(VillagerType origin);
}
