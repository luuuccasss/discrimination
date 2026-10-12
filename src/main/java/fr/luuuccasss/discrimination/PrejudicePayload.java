package fr.luuuccasss.discrimination;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Envoyé aux clients qui ont le mod : le préjugé d'un villageois, pour dessiner son accessoire. */
public record PrejudicePayload(int entityId, int prejudice) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PrejudicePayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Discrimination.MOD_ID, "prejudice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrejudicePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PrejudicePayload::entityId,
        ByteBufCodecs.VAR_INT, PrejudicePayload::prejudice,
        PrejudicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
