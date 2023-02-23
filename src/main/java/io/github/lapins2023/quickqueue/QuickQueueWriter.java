package io.github.lapins2023.quickqueue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Function;

public class QuickQueueWriter {

    private final QuickQueue qkq;

    private BigBuffer data;
    //
    private BigBuffer index;

    QuickQueueWriter(QuickQueue qkq) {
        this.qkq = qkq;
        data = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dataDir, "", Utils.DATA_EXT);
        open();
    }

    private void open() {
        BigBuffer r = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dataDir, "", Utils.INDEX_EXT);
        long lastIx = -1;
        long size = r.size();
        if (size > 0) {
            long start = size - Utils.PAGE_SIZE;
            Function<Integer, Boolean> flag =
                    n -> r.offset(start + (n << 4) + Utils.FLAG_OFF).get() == Utils.FLAG;
            int left = 0;
            int right = (Utils.PAGE_SIZE >> 4) - 1;
            while (left <= right) {
                int mid = left + ((right - left) >> 1);
                if (flag.apply(mid)) {
                    lastIx = start + ((long) mid << 4);
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        r.clean();
        //////
        index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dataDir, "", Utils.INDEX_EXT);
        data = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dataDir, "", Utils.DATA_EXT);
        if (lastIx < 0) {
            data.offset(0);
            index.offset(0);
        } else {
            index.offset(lastIx);
            long lastDataOffset = index.getLong();
            int lastDataLength = index.getLongLowAddressInt();
            data.offset(lastDataOffset + lastDataLength);
        }
    }

    void force() {
        data.force();
        index.force();
    }

    long begin;

    public QuickQueueWriter newMessage() {
        begin = data.offset();
        return this;
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

    public long writeMessage() {
        long end = data.offset();
        long off = index.offset();
        index.putLong(begin)
                .putLong(Math.toIntExact((end - begin)), Utils.FLAG);
        return off;
    }

    public void clean() {
        force();
        index.clean();
        data.clean();
    }
}
