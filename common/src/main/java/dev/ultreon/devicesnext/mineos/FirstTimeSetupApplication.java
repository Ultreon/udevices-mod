package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.mineos.gui.McButton;
import dev.ultreon.devicesnext.mineos.gui.McLabel;

import java.io.IOException;
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

        createWindow(new FirstTimeSetupWindow(operatingSystem, this));
    }

    private class FirstTimeSetupWindow extends Window {
        public FirstTimeSetupWindow(OperatingSystemImpl operatingSystem, FirstTimeSetupApplication firstTimeSetupApplication) {
            super(firstTimeSetupApplication, 0, 0, operatingSystem.getWidth(), operatingSystem.getHeight(), "First Time Setup");
            this.setUndecorated(true);
            this.setAbsolute(true);
            this.setTopMost(true);

            this.add(new McLabel(24, 4, 0, this.font.lineHeight, """
                    Welcome to MineOS!
                    This is the first time you've run MineOS on this device.
                    This screen will guide you through the setup process.
                    """));
            McButton aContinue = this.add(new McButton(this.getWidth() - 80, this.getHeight() - 20, 70, 15, "Continue"));
            aContinue.addClickHandler(button -> {
                this.setup();
            });
            this.addOnClosingListener(() -> false);

        }

        private void setup() {
            LibStd stdLib = this.application.getStdLib();

            if (!operatingSystem.getFileSystem().isInitialized()) {
                operatingSystem.getFileSystem().initialize();
            }

            stdLib.mkdir("/data", 0755);
            stdLib.mkdir("/data/appcfg", 0755);
            stdLib.mkdir("/data/apps", 0755);
            stdLib.mkdir("/data/kernel", 0755);
            stdLib.mkdir("/data/kernel/bin", 0755);

            if (stdLib.mkdir("/apps", 0755) == -1)
                throw new RuntimeException(stdLib.strerror() + " (I/O error " + stdLib.errno() + ")");
            if (stdLib.mkdir("/apps/@ultreon", 0755) == -1)
                throw new RuntimeException(stdLib.strerror() + " (I/O error " + stdLib.errno() + ")");
            if (stdLib.mkdir("/data/appcfg/@ultreon", 0755) == -1)
                throw new RuntimeException(stdLib.strerror() + " (I/O error " + stdLib.errno() + ")");

            int notepadJs = stdLib.open("/apps/@ultreon/mineos/notepad.js", stdLib.O_CREAT | stdLib.O_WRONLY | stdLib.O_TRUNC);
            if (notepadJs == -1) {
                throw new RuntimeException(stdLib.strerror() + " (I/O error " + stdLib.errno() + ")");
            }
            int notepadJson = stdLib.open("/data/appcfg/@ultreon/mineos/notepad.json", stdLib.O_CREAT | stdLib.O_WRONLY | stdLib.O_TRUNC);


            if (notepadJson == -1) {
                throw new RuntimeException(stdLib.strerror() + " (I/O error " + stdLib.errno() + ")");
            }

            try {
                String notepadJsString = new String(OperatingSystemImpl.class.getResourceAsStream("/notepad.js").readAllBytes());
                String notepadJsonString = new String(OperatingSystemImpl.class.getResourceAsStream("/notepad.json").readAllBytes());

                ByteBuffer jsBuf = ByteBuffer.wrap(notepadJsString.getBytes());
                ByteBuffer jsonBuf = ByteBuffer.wrap(notepadJsonString.getBytes());
                stdLib.write(notepadJs, jsBuf);
                stdLib.write(notepadJson, jsonBuf);

                stdLib.close(notepadJs);
                stdLib.close(notepadJson);

                jsBuf.clear();
                jsonBuf.clear();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onCreated() {
            super.onCreated();
        }
    }
}
