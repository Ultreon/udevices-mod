package com.ultreon.mods.lib.client.devicetest.sizing;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class AbstractSize {
    public abstract Number getWidth();

    public abstract Number getHeight();

    public int getIntWidth() {
        return (int) getWidth();
    }

    public int getIntHeight() {
        return (int) getHeight();
    }

    public long getLongWidth() {
        return (long) getWidth();
    }

    public long getLongHeight() {
        return (long) getHeight();
    }

    public float getFloatWidth() {
        return (float) getWidth();
    }

    public float getFloatHeight() {
        return (float) getHeight();
    }

    public double getDoubleWidth() {
        return (double) getWidth();
    }

    public double getDoubleHeight() {
        return (double) getHeight();
    }

    public BigInteger getBigIntegerWidth() {
        return new BigInteger(getWidth().toString());
    }

    public BigInteger getBigIntegerHeight() {
        return new BigInteger(getHeight().toString());
    }

    public BigDecimal getBigDecimalWidth() {
        return new BigDecimal(getWidth().toString());
    }

    public BigDecimal getBigDecimalHeight() {
        return new BigDecimal(getHeight().toString());
    }
}
