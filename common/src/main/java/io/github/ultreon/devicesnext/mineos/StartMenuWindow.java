package com.ultreon.mods.lib.client.devicetest;

import com.ultreon.mods.lib.UltreonLib;
import com.ultreon.mods.lib.client.devicetest.exception.McAppNotFoundException;
import com.ultreon.mods.lib.client.devicetest.exception.McNoPermissionException;
import com.ultreon.mods.lib.client.devicetest.exception.McSecurityException;
import com.ultreon.mods.lib.client.devicetest.gui.McButton;
import com.ultreon.mods.lib.client.devicetest.gui.McContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.Locale;

public abstract class StartMenuWindow extends Window {
    public static final int HEIGHT = 220;
    public static final int WIDTH = 110;
    private final McContainer list;

    public StartMenuWindow(Application application, int x, int y) {
        super(application, x, y, WIDTH, HEIGHT, "Start Menu");

        this.setUndecorated(true);
        this.setAbsolute(true);

        McButton powerOff = this.add(new McButton(10, this.getHeight() - 24, getWidth() - 20, 15, "Power Off", Icons.POWER_OFF));
        powerOff.addClickHandler(button -> {
            try {
                this.application.getSystem().shutdown(application);
            } catch (McNoPermissionException e) {
                throw new RuntimeException(e);
            }
        });

        McContainer list = new McContainer(0, 30, this.width, this.height - 64, Component.empty()) {

        };

        try {
            int listY = 0;
            for (ApplicationId id : this.application.getSystem().getApplications(this.application)) {
                McButton add = list.add(new McButton(0, listY, this.width, 15, id.getName()));
                add.addClickHandler(button -> {
                    try {
                        this.application.spawnApplication(id);
                    } catch (McAppNotFoundException | McNoPermissionException e) {
                        throw new RuntimeException(e);
                    } catch (McSecurityException e) {
                        this.openDialog(MessageDialog.create(this.getApplication(), MessageDialog.Icons.ERROR, Component.literal("Error"), Component.literal("Can't open application!")));
                    }
                });
                listY += 16;
            }
        } catch (McSecurityException e) {
            throw new RuntimeException(e);
        }
        this.list = this.add(list);
    }

    @Override
    protected void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(gfx, mouseX, mouseY, partialTicks);

        gfx.drawString(this.font, this.minecraft.getUser().getName(), 10, 10, 0xffffff, true);
    }

    @Override
    protected abstract @Nullable Vector2i getForcePosition();

    @Override
    public void onFocusLost() {
        this.close();
    }

    public enum Icons implements Icon {
        POWER_OFF;

        private static final int SIZE = 7;
        private final ResourceLocation resource;

        private Icons() {
            this.resource = UltreonLib.res("textures/gui/device/start_menu/icon_" + name().toLowerCase(Locale.ROOT) + ".png");
        }

        @Override
        public ResourceLocation resource() {
            return resource;
        }

        @Override
        public int width() {
            return SIZE;
        }

        @Override
        public int height() {
            return SIZE;
        }

        @Override
        public int vHeight() {
            return SIZE;
        }

        @Override
        public int uWidth() {
            return SIZE;
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
            return SIZE;
        }

        @Override
        public int texHeight() {
            return SIZE;
        }
    }
}
