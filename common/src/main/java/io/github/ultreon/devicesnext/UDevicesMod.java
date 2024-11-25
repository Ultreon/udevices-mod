package io.github.ultreon.devicesnext;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.github.ultreon.devicesnext.client.UDevicesModClient;

public class UDevicesMod
{
	public static final String MOD_ID = "udevices";

	public static void init() {
		EnvExecutor.runInEnv(Env.CLIENT, () -> UDevicesModClient::init);
	}
}
