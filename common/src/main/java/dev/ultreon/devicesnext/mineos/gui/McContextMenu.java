package dev.ultreon.devicesnext.mineos.gui;

import net.minecraft.network.chat.Component;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class McContextMenu extends McComponent {
    private final List<McMenuItem> items = Lists.newArrayList();

    public McContextMenu(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    public McMenuItem add(Component name, McMenuItem.Callback callback) {
        return add(new McMenuItem(name, callback));
    }

    public McMenuItem add(McMenuItem item) {
        this.items.add(item);
        return item;
    }

    public void remove(McMenuItem item) {
        this.items.remove(item);
    }

    void open(int mouseX, int mouseY) {
        this.setPosition(mouseX, mouseY);
        this.visible = true;
    }

    public void close() {
        this.visible = false;
    }
}
