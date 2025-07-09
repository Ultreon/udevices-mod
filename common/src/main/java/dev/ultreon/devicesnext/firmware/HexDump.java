package dev.ultreon.devicesnext.firmware;

import java.nio.ByteBuffer;

public class HexDump {

    public static String toHexDump(ByteBuffer buffer, int start, int end) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("Offset      ");
        for (int i = 0; i < 16; i++) {
            sb.append(String.format("%02X ", i));
        }
        sb.append(" | ASCII\n");
        sb.append("----------------------------------------------------------+----------------\n");

        int bufferStart = buffer.position();
        int bufferEnd = buffer.limit();
        int dataLength = bufferEnd - bufferStart;

        if (start < 0 || end < 0 || start >= end || end > dataLength) {
            throw new IllegalArgumentException("Start/end range invalid or outside buffer bounds.");
        }

        for (int lineOffset = (start / 16) * 16; lineOffset < end; lineOffset += 16) {
            boolean hasVisible = false;

            // Check if this line contains any in-range bytes
            for (int i = 0; i < 16; i++) {
                int index = lineOffset + i;
                if (index >= start && index < end) {
                    hasVisible = true;
                    break;
                }
            }

            if (!hasVisible) continue;

            // Offset label
            sb.append(String.format("0x%08X: ", lineOffset));

            // Hex bytes
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                int index = lineOffset + i;
                if (index >= start && index < end) {
                    byte b = buffer.get(bufferStart + index);
                    sb.append(String.format("%02X ", b));

                    // ASCII printable range
                    if (b >= 32 && b <= 126) {
                        ascii.append((char) b);
                    } else {
                        ascii.append('.');
                    }
                } else {
                    sb.append(".. ");
                    ascii.append('.');
                }
            }

            // Append ASCII
            sb.append(" | ").append(ascii).append("\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(64);
        for (int i = 0; i < 64; i++) buffer.put((byte) (i + 32)); // offset for readable ASCII
        buffer.rewind();

        // Example: dump region from byte 20 to 50
        System.out.println(toHexDump(buffer, 20, 50));
    }
}