package fr.luuuccasss.discrimination.client;

import fr.luuuccasss.discrimination.Prejudice;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/** Préjugés des villageois connus du client, reçus du serveur. */
public final class PrejudiceCache {
    private static final Int2ObjectMap<Prejudice> BY_ENTITY = new Int2ObjectOpenHashMap<>();

    private PrejudiceCache() {
    }

    public static void put(int entityId, int prejudice) {
        Prejudice[] values = Prejudice.values();
        if (prejudice >= 0 && prejudice < values.length) BY_ENTITY.put(entityId, values[prejudice]);
    }

    public static Prejudice get(int entityId) {
        return BY_ENTITY.get(entityId);
    }

    public static void clear() {
        BY_ENTITY.clear();
    }
}
