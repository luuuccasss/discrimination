package fr.luuuccasss.discrimination;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.VillagerType;

public class Discrimination implements ModInitializer {
    public static final String MOD_ID = "discrimination";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if ((world.getTime() + player.getId()) % 10 != 0) continue;
                census(player);
                Gossip.tick(world, player);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof VillagerEntity villager && ServerPlayNetworking.canSend(player, PrejudicePayload.ID)) {
                ServerPlayNetworking.send(player, PrejudicePayload.ID,
                    new PrejudicePayload(villager.getId(), Opinion.prejudice(villager).ordinal()).write());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Gossip.forget(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ProfileCommand.register(dispatcher));
    }

    /** À sa première apparition, le joueur est recensé : il est originaire du biome où il est né. */
    private static void census(ServerPlayerEntity player) {
        Profiled profile = (Profiled) player;
        if (profile.discrimination$getOrigin() != null) return;
        VillagerType origin = Origin.at(player);
        profile.discrimination$setOrigin(origin);
        player.sendMessage(Text.literal("Recensement : tu es officiellement originaire " + Origin.from(origin)
            + ". Les villageois des autres régions s'en souviendront.").formatted(Formatting.GRAY), false);
    }
}
