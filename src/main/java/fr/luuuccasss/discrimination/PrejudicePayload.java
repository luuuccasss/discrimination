package fr.luuuccasss.discrimination;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** Envoyé aux clients qui ont le mod : le préjugé d'un villageois, pour dessiner son accessoire. */
public record PrejudicePayload(int entityId, int prejudice) {
    public static final Identifier ID = new Identifier(Discrimination.MOD_ID, "prejudice");

    public PacketByteBuf write() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeVarInt(prejudice);
        return buf;
    }

    public static PrejudicePayload read(PacketByteBuf buf) {
        return new PrejudicePayload(buf.readVarInt(), buf.readVarInt());
    }
}
