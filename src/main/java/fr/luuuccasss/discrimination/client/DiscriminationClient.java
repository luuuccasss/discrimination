package fr.luuuccasss.discrimination.client;

import fr.luuuccasss.discrimination.PrejudicePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.VillagerEntityRenderer;

public class DiscriminationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PrejudicePayload.ID, (client, handler, buf, sender) -> {
            PrejudicePayload payload = PrejudicePayload.read(buf);
            client.execute(() -> PrejudiceCache.put(payload.entityId(), payload.prejudice()));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PrejudiceCache.clear());
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof VillagerEntityRenderer villager) helper.register(new AccessoriesFeatureRenderer(villager));
        });
    }
}
