package io.github.ultreon.devicesnext.api;

public class Color {
    public static Color black = new Color(0, 0, 0);
    public float r, g, b, a;

    public Color(float r, float g, float b) {
        this(r, g, b, 1);
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color() {

    }
}
