package dev.ultreon.devicesnext.mineos;

public record Insets(int left, int top, int right, int bottom) {

    @Override
    public String toString() {
        return "Insets[" +
                "left=" + left + ", " +
                "top=" + top + ", " +
                "right=" + right + ", " +
                "bottom=" + bottom + ']';
    }


}
