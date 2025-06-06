package dev.ultreon.devicesnext.mineos;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ultreon.devicesnext.mineos.gui.McComponent;
import dev.ultreon.devicesnext.mineos.gui.McImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

public class StartButton extends McComponent {
    private Callback callback = null;
    private final McImage image = new McImage();
    private final TaskbarWindow taskbar;

    public StartButton(TaskbarWindow taskbar, int x, int y, int width, int height, Component text) {
        super(x, y, width, height, text);
        this.taskbar = taskbar;
    }

    @Override
    public void renderComponent(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.fill(0, 0, this.width, this.height, 0xff101010);
        RenderSystem.enableBlend();
        gfx.setColor(1, 1, 1, isMouseOver(mouseX, mouseY) ? 1 : 0.5f);
        this.image.setX((this.width - this.image.getWidth()) / 2);
        this.image.setY((this.height - this.image.getHeight()) / 2);
        this.image.setWidth(16);
        this.image.setHeight(16);
        this.image.renderComponent(gfx, mouseX, mouseY, partialTicks);
        gfx.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean preMouseClicked(double mouseX, double mouseY, int button) {
        Callback callback = this.callback;
        if (callback == null) return false;
        return callback.click(this);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void loadIcon(ResourceLocation location, int width, int height) {
        this.image.setResource(location, width, height);
    }

    public void loadIcon(File file) {
        this.image.loadFrom(file);
    }

    public void loadIcon(Path path) {
        this.image.loadFrom(path);
    }

    @FunctionalInterface
    public interface Callback {
        boolean click(StartButton button);
    }
}
