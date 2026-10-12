package fr.luuuccasss.discrimination;

import java.util.UUID;

/** Interface ajoutée à VillagerEntity par mixin : son préjugé et l'habitude qu'il a de chaque joueur. */
public interface Prejudiced {
    /** Null tant qu'il n'a pas été tiré au sort. */
    Prejudice discrimination$getPrejudice();

    void discrimination$setPrejudice(Prejudice prejudice);

    /** Nombre d'échanges (et de cadeaux) avec ce joueur. */
    int discrimination$getFamiliarity(UUID player);

    void discrimination$setFamiliarity(UUID player, int value);
}
