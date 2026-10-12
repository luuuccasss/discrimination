package fr.luuuccasss.discrimination;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;

/** Interface ajoutée à ServerPlayer par mixin : le biome d'origine du joueur. */
public interface Profiled {
    /** Null tant que le joueur n'a pas encore été « recensé ». */
    ResourceKey<VillagerType> discrimination$getOrigin();

    void discrimination$setOrigin(ResourceKey<VillagerType> origin);
}
