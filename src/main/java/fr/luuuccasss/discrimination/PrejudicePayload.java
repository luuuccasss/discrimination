package fr.luuuccasss.discrimination;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Envoyé aux clients qui ont le mod : le préjugé d'un villageois, pour dessiner son accessoire. */
public record PrejudicePayload(int entityId, int prejudice) implements CustomPayload {
    public static final CustomPayload.Id<PrejudicePayload> ID = new CustomPayload.Id<>(Identifier.of(Discrimination.MOD_ID, "prejudice"));
    public static final PacketCodec<RegistryByteBuf, PrejudicePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, PrejudicePayload::entityId,
        PacketCodecs.VAR_INT, PrejudicePayload::prejudice,
        PrejudicePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
