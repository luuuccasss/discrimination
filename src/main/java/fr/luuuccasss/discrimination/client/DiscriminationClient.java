package fr.luuuccasss.discrimination.client;

import fr.luuuccasss.discrimination.PrejudicePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.renderer.entity.VillagerRenderer;

public class DiscriminationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PrejudicePayload.TYPE,
            (payload, context) -> PrejudiceCache.put(payload.entityId(), payload.prejudice()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PrejudiceCache.clear());
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof VillagerRenderer villager) helper.register(new AccessoriesLayer(villager));
        });
    }
}
