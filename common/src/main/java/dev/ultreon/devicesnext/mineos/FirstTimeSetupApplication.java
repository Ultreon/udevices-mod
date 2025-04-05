package dev.ultreon.devicesnext.mineos;

import com.badlogic.gdx.utils.GdxRuntimeException;
import dev.ultreon.devicesnext.mineos.gui.GpuRenderer;
import dev.ultreon.devicesnext.mineos.gui.McButton;
import dev.ultreon.devicesnext.mineos.gui.McLabel;
import dev.ultreon.devicesnext.mineos.sizing.IntSize;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class FirstTimeSetupApplication extends Application {
    private final OperatingSystemImpl operatingSystem;

    public FirstTimeSetupApplication(OperatingSystemImpl operatingSystem, ApplicationId id) {
        super(id);
        this.operatingSystem = operatingSystem;
    }

    @Override
    public void create() {
        super.create();

        createWindow(new Window(this, 0, 0, operatingSystem.getWidth(), operatingSystem.getHeight(), "") {
            {
                this.setBottomMost(true);
                this.setUndecorated(true);
            }

            @Override
            protected void renderBackground(GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
                super.renderBackground(gfx, mouseX, mouseY, partialTicks);
                gfx.fill(0, 0, width, height, 0xff4080f0);
            }

            @Override
            protected @NotNull IntSize getForceSize() {
                return new IntSize(operatingSystem.getWidth(), operatingSystem.getHeight());
            }
        });
        createWindow(new FirstTimeSetupWindow(operatingSystem, this));
    }

    class FirstTimeSetupWindow extends Window {
        private boolean canClose = false;

        public FirstTimeSetupWindow(OperatingSystemImpl operatingSystem, FirstTimeSetupApplication firstTimeSetupApplication) {
            super(firstTimeSetupApplication, 0, 0, operatingSystem.getWidth(), operatingSystem.getHeight(), "First Time Setup");
            this.setUndecorated(true);
            this.setMaximizable(false);
            this.setAbsolute(true);
            this.setTopMost(true);
            this.restore();

            this.add(new McLabel(24, 24, 0, 11, """
                    Welcome to MineOS!
                    This is the first time you've run MineOS on this device.
                    This screen will guide you through the setup process.
                    """));
            McButton aContinue = this.add(new McButton(this.getWidth() - 94, this.getHeight() - 39, 70, 15, "Continue"));
            aContinue.addClickHandler(button -> this.setup());
            this.addOnClosingListener(() -> canClose);
        }

        private void setup() {
            try {
                LibStd libStd = this.application.getLibStd();

                if (!operatingSystem.getFileSystem().isInitialized()) {
                    operatingSystem.getFileSystem().initialize();
                }

                libStd.mkdir("/data", 493);
                libStd.mkdir("/data/appcfg", 493);
                libStd.mkdir("/data/apps", 493);
                libStd.mkdir("/data/kernel", 493);
                libStd.mkdir("/data/kernel/bin", 493);

                if (libStd.mkdir("/apps", 493) == -1)
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");
                if (libStd.mkdir("/apps/@ultreon", 493) == -1)
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");
                if (libStd.mkdir("/apps/@ultreon/mineos", 493) == -1)
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");

                int notepadJs = libStd.open("/apps/@ultreon/mineos/notepad.js", LibStd.O_CREAT | LibStd.O_WRONLY | LibStd.O_TRUNC);
                if (notepadJs == -1) {
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");
                }
                int notepadJson = libStd.open("/apps/@ultreon/mineos/notepad.json", LibStd.O_CREAT | LibStd.O_WRONLY | LibStd.O_TRUNC);

                if (notepadJson == -1) {
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");
                }

                String notepadJsString = new String(readResource("/notepad.js"));
                String notepadJsonString = new String(readResource("/notepad.json"));

                ByteBuffer jsBuf = ByteBuffer.wrap(notepadJsString.getBytes());
                ByteBuffer jsonBuf = ByteBuffer.wrap(notepadJsonString.getBytes());
                libStd.write(notepadJs, jsBuf);
                libStd.write(notepadJson, jsonBuf);

                libStd.close(notepadJs);
                libStd.close(notepadJson);

                jsBuf.clear();
                jsonBuf.clear();

                int installedFlag = libStd.open("/data/installed", LibStd.O_CREAT | LibStd.O_WRONLY | LibStd.O_TRUNC);
                if (installedFlag == -1) {
                    throw new RuntimeException(libStd.strerror() + " (I/O error " + libStd.errno() + ")");
                }
                libStd.write(installedFlag, ByteBuffer.wrap("true".getBytes()));
                libStd.close(installedFlag);

                this.canClose = true;
                application.quit();

                operatingSystem.login();
            } catch (Throwable t) {
                operatingSystem.getLogger().error("Whoops!", t);
                openDialog(MessageDialog.create(application, MessageDialog.Icons.ERROR, "Could not setup MineOS", ChatFormatting.BOLD + t.getClass().getName() + "\n" + ChatFormatting.RESET + t.getMessage()));
            }
        }

        private byte[] readResource(String path) {
            try (InputStream is = FirstTimeSetupApplication.class.getResourceAsStream(path)) {
                if (is != null) {
                    return is.readAllBytes();
                } else {
                    throw new FileNotFoundException();
                }
            } catch (IOException e) {
                throw new GdxRuntimeException("Failed to read resource: " + path, e);
            }
        }

        @Override
        protected @Nullable Vector2i getForcePosition() {
            return new Vector2i(getSystem().getWidth() / 2 - width / 2, getSystem().getHeight() / 2 - height / 2);
        }

        @Override
        public void onCreated() {
            super.onCreated();
        }
    }
}
