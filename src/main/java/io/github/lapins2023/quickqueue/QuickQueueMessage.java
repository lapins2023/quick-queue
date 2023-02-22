package io.github.lapins2023.quickqueue;

import java.math.BigDecimal;
import java.math.BigInteger;

public class QuickQueueMessage {
    private long offset;
    private long limit;
    private final BigBuffer payload;

    QuickQueueMessage(BigBuffer data) {
        this.payload = data;
    }

    public QuickQueueMessage reset(long offset, long dataOffset, long len) {
        this.offset = offset;
        this.limit = dataOffset + len;
        payload.offset(dataOffset);
        return this;
    }

    public QuickQueueMessage skipFloat(int count) {
        return skipByte(Float.BYTES * count);
    }

    public QuickQueueMessage skipDouble(int count) {
        return skipByte(Double.BYTES * count);
    }

    public QuickQueueMessage skipLong(int count) {
        return skipByte(Long.BYTES * count);
    }

    public QuickQueueMessage skipInt(int count) {
        return skipByte(Integer.BYTES * count);
    }

    public QuickQueueMessage skipChar(int count) {
        return skipByte(Character.BYTES * count);
    }

    public QuickQueueMessage skipByte(int count) {
        payload.offset(payload.offset() + Byte.BYTES * count);
        return this;
    }

    public QuickQueueMessage pos(int newPosition) {
        payload.offset(offset + newPosition);
        return this;
    }


    //
    public long getOffset() {
        return offset;
    }

    public long getLimit() {
        return limit;
    }

    public int unpackInt() {
        return payload.getInt();
    }

    public long unpackLong() {
        return payload.getLong();
    }

    //
    public byte unpackByte() {
        return payload.get();
    }

    public char unpackChar() {
        return payload.getChar();
    }

    public float unpackFloat() {
        return payload.getFloat();
    }

    public double unpackDouble() {
        return payload.getDouble();
    }

    public boolean unpackBoolean() {
        return payload.get() == 1;
    }

    public BigDecimal unpackBigDecimal() {
        byte mn = unpackByte();
        int scale;
        int len;
        if (mn == 1) {
            scale = unpackByte();
            len = unpackByte();
        } else if (mn == 2) {
            scale = unpackInt();
            len = unpackInt();
        } else {
            throw new UnsupportedOperationException("unpackBigDecimalUnknownMagicNum");
        }
        byte[] bytes = new byte[len];
        payload.get(bytes, 0, len);
        return new BigDecimal(new BigInteger(bytes), scale);
    }

    public BigInteger unpackBigInteger() {
        return new BigInteger(unpackArray());
    }

    private char[] charBuff = new char[16];
    private int charBuffCount = 0;

    public String unpackUnicode() {
        charBuffCount = 0;
        while (true) {
            char c = unpackChar();
            if (appendChar(c)) {
                break;
            }
        }
        return new String(charBuff, 0, charBuffCount);
    }

    public String unpackString() {
        charBuffCount = 0;
        while (true) {
            char c = (char) unpackByte();
            if (appendChar(c)) {
                break;
            }
        }
        return new String(charBuff, 0, charBuffCount);
    }

    private boolean appendChar(char c) {
        if (c == (char) 0) {
            return true;
        }
        try {
            charBuff[charBuffCount] = c;
        } catch (ArrayIndexOutOfBoundsException e) {
            char[] chars = new char[(int) (charBuff.length * 1.5)];
            System.arraycopy(charBuff, 0, chars, 0, charBuff.length);
            charBuff = chars;
            charBuff[charBuffCount] = c;
        }
        charBuffCount++;
        return false;
    }


    public byte[] unpackArray() {
        int len = unpackInt();
        byte[] bytes = new byte[len];
        payload.get(bytes, 0, len);
        return bytes;
    }

}
