package fr.luuuccasss.discrimination;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;

public class Discrimination implements ModInitializer {
    public static final String MOD_ID = "discrimination";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(PrejudicePayload.TYPE, PrejudicePayload.CODEC);
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            for (ServerPlayer player : level.players()) {
                if ((level.getGameTime() + player.getId()) % 10 != 0) continue;
                census(player);
                Gossip.tick(level, player);
            }
        });
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof Villager villager && ServerPlayNetworking.canSend(player, PrejudicePayload.TYPE)) {
                ServerPlayNetworking.send(player, new PrejudicePayload(villager.getId(), Opinion.prejudice(villager).ordinal()));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Gossip.forget(handler.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ProfileCommand.register(dispatcher));
    }

    /** À sa première apparition, le joueur est recensé : il est originaire du biome où il est né. */
    private static void census(ServerPlayer player) {
        Profiled profile = (Profiled) player;
        if (profile.discrimination$getOrigin() != null) return;
        ResourceKey<VillagerType> origin = Origin.at(player);
        profile.discrimination$setOrigin(origin);
        player.sendSystemMessage(Component.literal("Recensement : tu es officiellement originaire " + Origin.from(origin)
            + ". Les villageois des autres régions s'en souviendront.").withStyle(ChatFormatting.GRAY));
    }
}
