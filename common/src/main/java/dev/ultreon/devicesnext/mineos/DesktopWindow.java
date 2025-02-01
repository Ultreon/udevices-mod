package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.api.Color;
import dev.ultreon.devicesnext.mineos.gui.McImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.Resource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

public class DesktopWindow extends Window {
    private final McImage wallpaper;
    private Color backgroundColor;

    public DesktopWindow(DesktopApplication application) {
        super(application, 0, 0, application.getSystem().getWidth(), application.getSystem().getHeight(), Component.empty());
        this.setAbsolute(true);

        this.wallpaper = new McImage(0, 0, 0, 0);
        this.setBackgroundColor(Color.black);
        this.add(this.wallpaper);
        this.addOnClosingListener(() -> false);
        this.setUndecorated(true);

        assert this.minecraft != null;
        Optional<Resource> resource = this.minecraft.getResourceManager().getResource(UDevicesMod.res("textures/wallpaper.png"));
        if (resource.isEmpty()) {
            return;
        }
        Path path = Path.of("./wallpaper.png");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream stream = resource.get().open()) {
                Files.copy(stream, path);
            } catch (Exception e) {
                UDevicesMod.LOGGER.warn("Failed to extract wallpaper file:", e);
            }
        }
        this.loadWallpaper(path);
    }

    @Override
    public void onCreated() {
        super.onCreated();

        this.setBottomMost(true);
    }

    @Override
    protected void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.setX(0);
        this.setY(0);
        this.setWidth(this.getScreenWidth());
        this.setHeight(this.getScreenHeight());
        this.wallpaper.setX(0);
        this.wallpaper.setY(0);
        this.wallpaper.setWidth(getWidth());
        this.wallpaper.setHeight(getHeight());

        this.setBottomMost(true);
    }

    public void loadWallpaper(File file) {
        this.wallpaper.loadFrom(file);
        this.backgroundColor = null;
    }

    public void loadWallpaper(Path path) {
        this.wallpaper.loadFrom(path);
        this.backgroundColor = null;
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }
}
