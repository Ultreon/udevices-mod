package dev.ultreon.devicesnext.mixin;

import dev.ultreon.devicesnext.gpu.GfxFunctionMap;
import dev.ultreon.mcgdx.impl.GdxScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GdxScreen.class, remap = false)
public interface GdxScreenAccessor {
    @Accessor(value = "mcGfx", remap = false)
    GuiGraphics getMcGfx();
}
