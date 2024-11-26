package dev.ultreon.devicesnext.util;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;

public class Arguments {
    private final Object[] args;

    public Arguments(int len) {
        this.args = new Object[len];
    }

    public void setByte(int index, byte value) {
        this.args[index] = value;
    }

    public void setShort(int index, short value) {
        this.args[index] = value;
    }

    public void setInt(int index, int value) {
        this.args[index] = value;
    }

    public void setLong(int index, long value) {
        this.args[index] = value;
    }

    public void setFloat(int index, float value) {
        this.args[index] = value;
    }

    public void setDouble(int index, double value) {
        this.args[index] = value;
    }

    public void setString(int index, String value) {
        this.args[index] = value;
    }

    public void setBoolean(int index, boolean value) {
        this.args[index] = value;
    }

    public void setChar(int index, char value) {
        this.args[index] = value;
    }

    public byte getByte(int index) {
        return (byte) this.args[index];
    }

    public short getShort(int index) {
        return (short) this.args[index];
    }

    public int getInt(int index) {
        return (int) this.args[index];
    }

    public long getLong(int index) {
        return (long) this.args[index];
    }

    public float getFloat(int index) {
        return (float) this.args[index];
    }

    public double getDouble(int index) {
        return (double) this.args[index];
    }

    public String getString(int index) {
        return (String) this.args[index];
    }

    public boolean getBoolean(int index) {
        return (boolean) this.args[index];
    }

    public char getChar(int index) {
        return (char) this.args[index];
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.args.length);
        for (Object arg : this.args) {
            if (arg instanceof String) {
                buf.writeByte(0);
                buf.writeUtf((String) arg);
            } else if (arg instanceof Byte) {
                buf.writeByte(1);
                buf.writeByte((Byte) arg);
            } else if (arg instanceof Short) {
                buf.writeByte(2);
                buf.writeShort((Short) arg);
            } else if (arg instanceof Integer) {
                buf.writeByte(3);
                buf.writeInt((Integer) arg);
            } else if (arg instanceof Long) {
                buf.writeByte(4);
                buf.writeLong((Long) arg);
            } else if (arg instanceof Float) {
                buf.writeByte(5);
                buf.writeFloat((Float) arg);
            } else if (arg instanceof Double) {
                buf.writeByte(6);
                buf.writeDouble((Double) arg);
            } else if (arg instanceof Boolean) {
                buf.writeByte(7);
                buf.writeBoolean((Boolean) arg);
            } else if (arg instanceof Character) {
                buf.writeByte(8);
                buf.writeChar((Character) arg);
            } else {
                throw new IllegalArgumentException("Invalid argument type: " + arg.getClass().getName());
            }
        }
    }

    public static Arguments read(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        Arguments args = new Arguments(len);
        for (int i = 0; i < len; i++) {
            int type = buf.readByte();
            if (type == 0) {
                args.setString(i, buf.readUtf());
            } else if (type == 1) {
                args.setByte(i, buf.readByte());
            } else if (type == 2) {
                args.setShort(i, buf.readShort());
            } else if (type == 3) {
                args.setInt(i, buf.readInt());
            } else if (type == 4) {
                args.setLong(i, buf.readLong());
            } else if (type == 5) {
                args.setFloat(i, buf.readFloat());
            } else if (type == 6) {
                args.setDouble(i, buf.readDouble());
            } else if (type == 7) {
                args.setBoolean(i, buf.readBoolean());
            } else if (type == 8) {
                args.setChar(i, buf.readChar());
            } else {
                throw new IllegalArgumentException("Invalid argument type ID: " + type);
            }
        }
        return args;
    }

    public String toString() {
        return Arrays.toString(this.args);
    }
}
