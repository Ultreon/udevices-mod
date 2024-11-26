package dev.ultreon.devicesnext.network.packets;

import com.ultreon.mods.lib.network.api.packet.PacketToServer;
import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.util.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class GfxCallbackPacket extends PacketToServer<GfxCallbackPacket> {
    public BlockPos pos;
    public int ptr;
    public Arguments args;

    public GfxCallbackPacket(BlockPos pos, int ptr, Arguments args) {
        this.pos = pos;
        this.ptr = ptr;
        this.args = args;
    }

    public static GfxCallbackPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int ptr = buf.readInt();
        Arguments args = Arguments.read(buf);
        return new GfxCallbackPacket(pos, ptr, args);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.ptr);
        this.args.write(buffer);
    }

    @Override
    protected void handle(@NotNull ServerPlayer sender) {
        UDevicesMod.onGfxCallback(sender, pos, ptr, args);
    }
}
