package com.ultreon.mods.lib.client.devicetest.gui;

import com.ultreon.mods.lib.util.ScissorStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class McLabel extends McComponent {
    public McLabel(int x, int y, int width, int height, String text) {
        this(x, y, width, height, Component.literal(text));

        this.updateSize();
    }

    private void updateSize() {
        String message = this.getMessage().getString();
        List<String> lines = message.lines().toList();
        this.updateSize(lines);
    }

    public McLabel(int x, int y, int width, int height, Component text) {
        super(x, y, width, height, text);
    }

    private void updateSize(List<String> lines) {
        this.height = (this.font.lineHeight + 1) * lines.size();
        this.width = lines.stream().mapToInt(this.font::width).max().orElse(0);
    }

    @Override
    public void setMessage(@NotNull Component message) {
        super.setMessage(message);
        this.updateSize();
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        ScissorStack.pushScissorTranslated(gfx, this.getX(), this.getY(), this.width, this.height);
        {
            String message = getMessage().getString();
            List<String> lines = message.lines().toList();

            int line = 0;
            for (String s : lines) {
                gfx.drawString(this.font, s, this.getX(), this.getY() + (this.font.lineHeight + 1) * line, 0xffffffff, false);
                line++;
            }
        }
        ScissorStack.popScissor();
    }

    @FunctionalInterface
    public interface ClickCallback {
        void onClick(McLabel label, int clicks);
    }
}
