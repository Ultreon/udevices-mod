package dev.ultreon.devicesnext.mineos.gui;

import com.google.common.collect.Lists;
import dev.ultreon.devicesnext.client.ScissorStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class McLabel extends McComponent {
    private final List<String> lines = Lists.newArrayList();

    public McLabel(int x, int y, int width, int height, String text) {
        this(x, y, width, height, Component.literal(text));

        this.updateSize();
    }

    private void updateSize() {
        String message = this.getMessage().getString();
        lines.clear();
        message.lines().forEach(this.lines::add);
        this.updateSize(lines);
    }

    public McLabel(int x, int y, int width, int height, Component text) {
        super(x, y, width, height, text);
    }

    private void updateSize(List<String> lines) {
        this.height = (this.font.lineHeight + 1) * lines.size();

        boolean seen = false;
        int best = 0;
        for (String line : lines) {
            int found = this.font.width(line);
            if (!seen || found > best) {
                seen = true;
                best = found;
            }
        }

        this.width = seen ? best : 0;
    }

    @Override
    public void setMessage(@NotNull Component message) {
        super.setMessage(message);
        this.updateSize();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        ScissorStack.scissor(gfx, 0, 0, this.width, this.height, () -> {
            String message = getMessage().getString();
            List<String> lines = message.lines().toList();

            int line = 0;
            for (String s : lines) {
                gfx.drawString(this.font, s, 0, (this.font.lineHeight + 1) * line, 0xffffffff, false);
                line++;
            }
        });
    }

    @FunctionalInterface
    public interface ClickCallback {
        void onClick(McLabel label, int clicks);
    }
}
