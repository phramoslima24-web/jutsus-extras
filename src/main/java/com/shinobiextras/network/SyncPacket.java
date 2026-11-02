package com.shinobiextras.network;

import com.shinobiextras.jutsu.ClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> cliente: jutsus aprendidos (mascara de bits) e o selecionado. */
public class SyncPacket {
    private final int mask;
    private final int selected;

    public SyncPacket(int mask, int selected) {
        this.mask = mask;
        this.selected = selected;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf buf) {
        buf.writeInt(m.mask);
        buf.writeInt(m.selected);
    }

    public static SyncPacket decode(FriendlyByteBuf buf) {
        return new SyncPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> ClientState.set(m.mask, m.selected));
        c.setPacketHandled(true);
    }
}
