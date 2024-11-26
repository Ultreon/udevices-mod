package dev.ultreon.devicesnext.network.packets;

import com.ultreon.mods.lib.network.api.packet.PacketToClient;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.ultreon.devicesnext.client.UDevicesModClient;
import dev.ultreon.devicesnext.util.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class GfxCallPacket extends PacketToClient<GfxCallPacket> {
    public BlockPos pos;
    public int ptr;
    public Arguments args;

    public GfxCallPacket(BlockPos pos, int ptr, Arguments args) {
        this.pos = pos;
        this.ptr = ptr;
        this.args = args;
    }

    public static GfxCallPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int ptr = buf.readInt();
        Arguments args = Arguments.read(buf);
        return new GfxCallPacket(pos, ptr, args);
    }

    @Override
    protected void handle() {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> UDevicesModClient.onGfxCall(pos, ptr, args));
    }

    @Override
    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(this.ptr);
        this.args.write(buffer);
    }
}
