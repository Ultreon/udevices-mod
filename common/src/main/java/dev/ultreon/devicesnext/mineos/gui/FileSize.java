package dev.ultreon.devicesnext.mineos.gui;

public class FileSize {
    public enum Unit {
        B,
        KB,
        MB,
        GB,
        TB,
        PB,
        EB,
        ZB,
        YB,
        RB,
        QB
    }

    private final long bytes;

    private FileSize(long bytes) {
        this.bytes = bytes;
    }

    public static FileSize ofQb(int qb) {
        return new FileSize(qb * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofRb(int rb) {
        return new FileSize(rb * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofYb(int yb) {
        return new FileSize(yb * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofZb(int zb) {
        return new FileSize(zb * 1024L * 1024L * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofEb(int eb) {
        return new FileSize(eb * 1024L * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofTb(int tb) {
        return new FileSize(tb * 1024L * 1024L * 1024L * 1024L);
    }

    public static FileSize ofGb(int gb) {
        return new FileSize(gb * 1024L * 1024L * 1024L);
    }

    public static FileSize ofMb(int mb) {
        return new FileSize(mb * 1024L * 1024L);
    }

    public static FileSize ofKb(int kb) {
        return new FileSize(kb * 1024L);
    }

    public static FileSize ofB(int b) {
        return new FileSize(b);
    }

    public long getBytes() {
        return bytes;
    }
    
    public String toString() {
        if (bytes < 1024L) {
            return bytes + " B";
        } else if (bytes < 1024L * 1024L) {
            return "%.2f KB".formatted(toKb());
        } else if (bytes < 1024L * 1024L * 1024L) {
            return "%.2f MB".formatted(toMb());
        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
            return "%.2f GB".formatted(toGb());
        } else if (bytes < 1024L * 1024L * 1024L * 1024L * 1024L) {
            return "%.2f TB".formatted(toTb());
        } else if (bytes < 1024L * 1024L * 1024L * 1024L * 1024L * 1024L) {
            return "%.2f PB".formatted(toPb());
        } else {
            return "%.2f EB".formatted(toEb());
        }
    }

    public String toString(Unit unit) {
        return "%.2f".formatted(toUnit(unit)) + " " + unit;
    }
    
    public double toUnit(Unit unit) {
        return switch (unit) {
            case B -> bytes;
            case KB -> toKb();
            case MB -> toMb();
            case GB -> toGb();
            case TB -> toTb();
            case PB -> toPb();
            case EB -> toEb();
            case ZB -> toZb();
            case YB -> toYb();
            case RB -> toRb();
            case QB -> toQb();
        };
    }
    
    public long toBytes() {
        return bytes;
    }
    
    public double toKb() {
        return bytes / 1024.0;
    }
    
    public double toMb() {
        return bytes / 1024.0 / 1024.0;
    }
    
    public double toGb() {
        return bytes / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toTb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toPb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toEb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toZb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toYb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toRb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
    
    public double toQb() {
        return bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0;
    }
}
