package dev.ultreon.devicesnext.mineos;

import dev.ultreon.devicesnext.UDevicesMod;
import dev.ultreon.devicesnext.client.ScissorStack;
import dev.ultreon.devicesnext.mineos.gui.McLabel;
import dev.ultreon.devicesnext.mineos.sizing.IntSize;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class MessageDialog extends DialogWindow {
    private McLabel description;
    private Icon icon;

    private MessageDialog(@NotNull Application window, int width, int height, String title) {
        super(window, width, height, title);
    }

    private MessageDialog(@NotNull Application window, int width, int height, Component title) {
        super(window, width, height, title);

        this.description = this.add(new McLabel(24, 4, 0, this.font.lineHeight, ""));
        this.setMaximizable(false);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(gfx, mouseX, mouseY, partialTicks);

        final Icon icon = this.icon;
        if (icon != null) {
            icon.render(gfx, 4, 4, 16, 16);
            description.setX(icon.width() + 8);
        } else {
            description.setX(4);
        }

        int descLength = this.description.getWidth();
        var lines = this.description.getMessage().getString().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n");
        int textHeight = this.description.getHeight();
        if (textHeight < 16) {
            this.description.setY(4 + (textHeight - 9) / 2);
        } else {
            this.description.setY(4);
        }
    }

    @Override
    protected @Nullable IntSize getForceSize() {
        int descLength = this.description.getWidth();
        var lines = this.description.getMessage().getString().replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n");
        int textHeight = this.description.getHeight();
        if (icon != null) {
            this.description.setX(icon.width() + 8);
        } else {
            this.description.setX(4);
        }
        if (textHeight < 16) {
            this.description.setY(4 + (textHeight - 9) / 2);
        } else {
            this.description.setY(4);
        }
        return new IntSize(descLength + (icon != null ? icon.width() + 4 : 0) + 8, Math.max(this.description.getY() + this.description.getHeight() + 8, 24));
    }

    public Component getDescription() {
        return description.getMessage();
    }

    public void setDescription(Component description) {
        this.description.setMessage(description);
    }

    public static MessageDialog create(Application application, Icon icon, Component title, Component description) {
        MessageDialog dialog = new MessageDialog(application, 1, 1, title);
        dialog.setDescription(description);
        dialog.icon = icon;
        return dialog;
    }

    public enum Icons implements Icon {
        INFO, WARNING, ERROR, QUESTION;

        private final ResourceLocation resource;

        Icons() {
            this.resource = UDevicesMod.res("textures/gui/device/messagebox/icon_" + name().toLowerCase(Locale.ROOT) + ".png");
        }

        Icons(ResourceLocation resource) {
            this.resource = resource;
        }

        @Override
        public ResourceLocation resource() {
            return resource;
        }

        @Override
        public int width() {
            return 16;
        }

        @Override
        public int height() {
            return 16;
        }

        @Override
        public int vHeight() {
            return 16;
        }

        @Override
        public int uWidth() {
            return 16;
        }

        @Override
        public int v() {
            return 0;
        }

        @Override
        public int u() {
            return 0;
        }

        @Override
        public int texWidth() {
            return 16;
        }

        @Override
        public int texHeight() {
            return 16;
        }
    }
}
