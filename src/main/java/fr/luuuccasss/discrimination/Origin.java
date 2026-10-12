package fr.luuuccasss.discrimination;

import java.util.Map;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerType;

/** Le biome d'origine d'un joueur ou d'un villageois, sous forme de type de villageois (plaines, désert…). */
public final class Origin {
    private static final Map<RegistryKey<VillagerType>, String[]> NAMES = Map.of(
        VillagerType.PLAINS, new String[]{"des plaines", "Plaines", "dans tes plaines"},
        VillagerType.DESERT, new String[]{"du désert", "Désert", "dans ton désert"},
        VillagerType.SNOW, new String[]{"des neiges", "Neiges", "dans tes neiges"},
        VillagerType.TAIGA, new String[]{"de la taïga", "Taïga", "dans ta taïga"},
        VillagerType.SAVANNA, new String[]{"de la savane", "Savane", "dans ta savane"},
        VillagerType.JUNGLE, new String[]{"de la jungle", "Jungle", "dans ta jungle"},
        VillagerType.SWAMP, new String[]{"des marais", "Marais", "dans tes marais"});

    private static final String[] UNKNOWN = {"d'ailleurs", "Ailleurs", "chez toi"};

    /** Couleur de chaque région, pour les bérets des chauvins. */
    private static final Map<RegistryKey<VillagerType>, Integer> COLORS = Map.of(
        VillagerType.PLAINS, 0x3E6FC4, VillagerType.DESERT, 0xD9B45A, VillagerType.SNOW, 0xE8EEF2,
        VillagerType.TAIGA, 0x2F5A3A, VillagerType.SAVANNA, 0xC2632B, VillagerType.JUNGLE, 0x4E9A2E,
        VillagerType.SWAMP, 0x5B5A3A);

    private Origin() {
    }

    /** Le biome où se trouve le joueur, ramené à l'un des sept types de villages. */
    public static RegistryKey<VillagerType> at(ServerPlayerEntity player) {
        return VillagerType.forBiome(player.getWorld().getBiome(player.getBlockPos()));
    }

    /** « du désert », « des plaines »… */
    public static String from(RegistryKey<VillagerType> type) {
        return NAMES.getOrDefault(type, UNKNOWN)[0];
    }

    public static String label(RegistryKey<VillagerType> type) {
        return NAMES.getOrDefault(type, UNKNOWN)[1];
    }

    /** « dans ton désert », « dans tes plaines »… */
    public static String home(RegistryKey<VillagerType> type) {
        return NAMES.getOrDefault(type, UNKNOWN)[2];
    }

    public static int color(RegistryKey<VillagerType> type) {
        return COLORS.getOrDefault(type, 0x777777);
    }

    public static String id(RegistryKey<VillagerType> type) {
        return type.getValue().toString();
    }

    /** Null si ce n'est pas l'un des sept types de villages. */
    public static RegistryKey<VillagerType> byId(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return null;
        RegistryKey<VillagerType> key = RegistryKey.of(RegistryKeys.VILLAGER_TYPE, identifier);
        return NAMES.containsKey(key) ? key : null;
    }

    public static Iterable<RegistryKey<VillagerType>> all() {
        return NAMES.keySet();
    }
}
