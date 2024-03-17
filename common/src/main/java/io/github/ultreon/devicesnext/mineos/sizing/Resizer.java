package com.ultreon.mods.lib.client.devicetest.sizing;

import org.jetbrains.annotations.Nullable;

public class Resizer {
    public float ratio;
    public float relativeRatio;
    @Nullable
    public Orientation orientation;
    public float sourceWidth;
    public float sourceHeight;

    public Resizer(float srcWidth, float srcHeight) {
        ratio = srcWidth / srcHeight;
        if (srcWidth > srcHeight) {
            relativeRatio = srcWidth / srcHeight;
            orientation = Orientation.LANDSCAPE;
        } else if (srcWidth < srcHeight) {
            relativeRatio = srcHeight / srcWidth;
            orientation = Orientation.PORTRAIT;
        } else {
            relativeRatio = 1f;
            orientation = Orientation.SQUARE;
        }
        sourceWidth = srcWidth;
        sourceHeight = srcHeight;
    }

    public FloatSize crop(float maxWidth, float maxHeight) {
        float aspectRatio;
        float width;
        float height;
        if (sourceWidth < sourceHeight) {
            aspectRatio = (sourceWidth / sourceHeight);
            width = maxWidth;
            height = (float) (int) (width / aspectRatio);
            if (height > maxHeight) {
                aspectRatio = (sourceHeight / sourceWidth);
                height = maxHeight;
                width = (float) (int) (height / aspectRatio);
            }
        } else {
            aspectRatio = (sourceHeight / sourceWidth);
            height = maxHeight;
            width = (float) (int) (height / aspectRatio);
            if (width > maxWidth) {
                aspectRatio = (sourceWidth / sourceHeight);
                width = maxWidth;
                height = (float) (int) (width / aspectRatio);
            }
        }
        return new FloatSize(width, height);
    }

    /**
     * Aspect ratio orientation.
     */
    public enum Orientation {
        LANDSCAPE, SQUARE, PORTRAIT
    }
}