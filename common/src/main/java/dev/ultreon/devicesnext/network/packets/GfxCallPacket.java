package dev.ultreon.devicesnext.network.packets;

import dev.ultreon.devicesnext.client.UDevicesModClient;
import dev.ultreon.devicesnext.util.Arguments;
import dev.ultreon.mods.xinexlib.Env;
import dev.ultreon.mods.xinexlib.EnvExecutor;
import dev.ultreon.mods.xinexlib.network.Networker;
import dev.ultreon.mods.xinexlib.network.packet.PacketToClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public record GfxCallPacket(BlockPos pos, int ptr, Arguments args) implements PacketToClient<GfxCallPacket> {
    public static GfxCallPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int ptr = buf.readInt();
        Arguments args = Arguments.read(buf);
        return new GfxCallPacket(pos, ptr, args);
    }

    @Override
    public void handle(Networker networker) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> UDevicesModClient.onGfxCall(pos, ptr, args));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.ptr);
        this.args.write(buffer);
    }
}
