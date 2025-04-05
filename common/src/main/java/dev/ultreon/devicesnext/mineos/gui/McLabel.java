package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.google.common.collect.Lists;
import dev.ultreon.devicesnext.client.ScissorStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class McLabel extends McComponent {
    private final List<String> lines = Lists.newArrayList();
    private final GlyphLayout layout = new GlyphLayout();

    public McLabel(int x, int y, int width, int height, String text) {
        super(x, y, width, height, text);

        this.updateSize();
    }

    private void updateSize() {
        String message = this.getMessage();
        lines.clear();
        message.lines().forEach(this.lines::add);
        this.updateSize(lines);
    }

    private void updateSize(List<String> lines) {
        this.height = (int) ((this.font.getLineHeight() + 1) * lines.size());

        boolean seen = false;
        int best = 0;
        for (String line : lines) {
            int found = this.width(line);
            if (!seen || found > best) {
                seen = true;
                best = found;
            }
        }

        this.width = seen ? best : 0;
    }

    private int width(String line) {
        this.layout.setText(font, line);
        return (int) this.layout.width;
    }

    public void setMessage(@NotNull String message) {
        super.setMessage(message);
        this.updateSize();
    }

    @Override
    public void render(@NotNull GpuRenderer gfx, int mouseX, int mouseY, float partialTicks) {
        ScissorStack.scissor(gfx, 0, 0, this.width, this.height, () -> {
            String message = getMessage();
            List<String> lines = message.lines().toList();

            int line = 0;
            for (String s : lines) {
                gfx.drawString(s, 0, (int) ((this.font.getLineHeight() + 1) * line), 0xffffffff, false);
                line++;
            }
        });
    }

    @FunctionalInterface
    public interface ClickCallback {
        void onClick(McLabel label, int clicks);
    }
}
