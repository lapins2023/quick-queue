package io.github.lapins2023.quickqueue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public abstract class QuickQueueWriter {
    protected final BigBuffer data;

    protected long begin;

    public QuickQueueWriter newMessage() {
        begin = data.offset();
        return this;
    }

    protected QuickQueueWriter(BigBuffer data) {
        this.data = data;
    }

    public QuickQueueWriter packInt(int v) {
        data.putInt(v);
        return this;
    }

    public QuickQueueWriter packLong(long v) {
        data.putLong(v);
        return this;
    }

    public QuickQueueWriter packByte(byte v) {
        data.put(v);
        return this;
    }

    public QuickQueueWriter packChar(char v) {
        data.putChar(v);
        return this;
    }

    public QuickQueueWriter packFloat(float v) {
        data.putFloat(v);
        return this;
    }

    public QuickQueueWriter packDouble(double v) {
        data.putDouble(v);
        return this;
    }

    public QuickQueueWriter packBoolean(boolean v) {
        data.putBoolean(v);
        return this;
    }

    public QuickQueueWriter packBigDecimal(BigDecimal v) {
        Objects.requireNonNull(v, "BigDecimalIsNull");
        byte[] src = v.unscaledValue().toByteArray();
        int scale = v.scale();
        if (scale <= Byte.MAX_VALUE && src.length <= Byte.MAX_VALUE) {
            packByte((byte) 1);
            packByte((byte) scale);
            packByte((byte) src.length);
            data.put(src, 0, src.length);
        } else {
            packByte((byte) 2);
            packInt(scale);
            packArray(src, 0, src.length);
        }
        return this;
    }

    public QuickQueueWriter packBigInteger(BigInteger v) {
        Objects.requireNonNull(v, "BigIntegerIsNull");
        byte[] src = v.toByteArray();
        packArray(src, 0, src.length);
        return this;
    }

    public QuickQueueWriter packUnicode(String v) {
        Objects.requireNonNull(v, "StringIsNull");
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            packChar(c);
        }
        packChar((char) 0);
        return this;
    }

    public QuickQueueWriter packString(String v) {
        Objects.requireNonNull(v, "StringIsNull");
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            byte b = (byte) c;
            if (b == c) {
                packByte(b);
            } else {
                throw new IllegalArgumentException("PackStringSupportAscii");
            }
        }
        packByte((byte) 0);
        return this;
    }

    public QuickQueueWriter packArray(byte[] src, int offset, int length) {
        data.put(src, offset, length);
        return this;
    }

    public final long writeMessage() {
        int length = Math.toIntExact((data.offset() - begin));
        return writeMessage0(length);
    }

    protected abstract long writeMessage0(int length);

    public abstract void force0();

    public final void force() {
        data.force();
        force0();
    }
}
