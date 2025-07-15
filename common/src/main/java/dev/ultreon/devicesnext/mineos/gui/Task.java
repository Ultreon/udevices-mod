package dev.ultreon.devicesnext.mineos.gui;

import com.badlogic.gdx.graphics.g2d.Batch;
import space.earlygrey.shapedrawer.ShapeDrawer;

@FunctionalInterface
public interface Task {
    void run(Batch batch, ShapeDrawer drawer);
}
