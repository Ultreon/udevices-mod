package dev.ultreon.devicesnext.network.packets;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.util.Arguments;
import dev.ultreon.mods.xinexlib.network.Networker;
import dev.ultreon.mods.xinexlib.network.packet.PacketToServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record GfxCallbackPacket(BlockPos pos, int ptr, Arguments args) implements PacketToServer<GfxCallbackPacket> {
    public static GfxCallbackPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int ptr = buf.readInt();
        Arguments args = Arguments.read(buf);
        return new GfxCallbackPacket(pos, ptr, args);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.ptr);
        this.args.write(buffer);
    }

    @Override
    public void handle(Networker networker, @NotNull ServerPlayer sender) {
        UDevicesMod.onGfxCallback(sender, pos, ptr, args);
    }
}
