package io.github.lapins2023.quickqueue;


import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public class BigBuffer {
    private final int pageSize;
    private final int pageBitSize;
    private final int pageMaxPos;
    private final File dir;
    private final String fileNamePrefix;
    private final String fileNameSuffix;

    private final String mode;

    public BigBuffer(String mode, int pageSize, File dir, String fileNamePrefix, String fileNameSuffix) {
        if (!("rw".equalsIgnoreCase(mode) || "r".equalsIgnoreCase(mode)))
            throw new IllegalArgumentException("Illegal mode=" + mode);
        this.pageSize = pageSize;
        pageMaxPos = this.pageSize - 1;
        if ((this.pageSize & pageMaxPos) != 0) {
            throw new IllegalArgumentException("QKQPageSizeMust2<<");
        }
        pageBitSize = Integer.SIZE - Integer.numberOfLeadingZeros(pageMaxPos);
        this.mode = mode.toLowerCase(Locale.ROOT);
        this.dir = dir;
        this.fileNamePrefix = fileNamePrefix;
        this.fileNameSuffix = fileNameSuffix;
    }

    private PageBuffer getPageBuffer(int page) {
        try {
            if (curr != null && curr.page == page) return curr;
            if (markPageBuff != null && markPageBuff.page == page) return markPageBuff;
            File file = new File(dir, fileNamePrefix + page + fileNameSuffix);
            if (mode.equals("r") && !file.exists()) {
                return null;
            }
            if (cOffset(page, pageSize) < 0) {
                throw new Exception("PageOverflow=" + page);
            }
            try (RandomAccessFile rw = new RandomAccessFile(file, mode)) {
                MappedByteBuffer map = (MappedByteBuffer) rw
                        .getChannel()
                        .map(mode.equals("r") ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE
                                , 0, pageSize)
                        .order(Utils.NativeByteOrder);
                return new PageBuffer(page, map);
            }
        } catch (Throwable t) {
            throw new RuntimeException("getMMapRWFailed=" + page, t);
        }
    }

    private PageBuffer curr;

    private long cOffset(long page, int pos) {
        return (page << pageBitSize) + pos;
    }

    public long offset() {
        int page;
        try {
            page = curr.page;
        } catch (NullPointerException e) {
            throw new UnsupportedOperationException("CurrPageNotOpen");
        }
        return cOffset(page, curr.position());
    }


    public long size() {
        ArrayList<Integer> pages = new ArrayList<>();
        for (String name : Objects.requireNonNull(dir.list())) {
            if (name.endsWith(fileNameSuffix) && name.startsWith(fileNamePrefix)) {
                pages.add(Integer.parseInt(
                        name.substring(fileNamePrefix.length(), name.length() - fileNameSuffix.length())));
            }
        }
        pages.sort(Comparator.reverseOrder());
        if (pages.size() == 0) {
            return 0;
        } else {
            return cOffset(pages.get(0), pageSize);
        }
    }

    public BigBuffer offset(long offset) {
        int pos = (int) offset & pageMaxPos;
        int page = (int) (offset >> pageBitSize);
        if (curr == null || curr.page != page) {
            PageBuffer curr_ = getPageBuffer(page);
            if (curr_ == null) throw new BufferUnderflowException();
            if (curr != null) ((DirectBuffer) curr.buffer).cleaner().clean();
            curr = curr_;
        }
        if (curr.buffer.position() != pos) {
            curr.buffer.position(pos);
        }
        return this;
    }


    private final ByteBuffer tmp = ByteBuffer.allocateDirect(Long.BYTES)
            .order(Utils.NativeByteOrder);

    public BigBuffer putInt(int i) {
        try {
            curr.buffer.putInt(i);
        } catch (BufferOverflowException e) {
            tmp.clear();
            tmp.putInt(i);
            tmp.flip();
            while (tmp.hasRemaining()) {
                put(tmp.get());
            }
        }
        return this;
    }


    public int getInt() {
        try {
            return curr.buffer.getInt();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Integer.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getInt();
        }
    }

    public BigBuffer putLong(long i) {
        try {
            curr.buffer.putLong(i);
        } catch (BufferOverflowException e) {
            tmp.clear();
            tmp.putLong(i);
            tmp.flip();
            while (tmp.hasRemaining()) {
                put(tmp.get());
            }
        }
        return this;
    }


    public char getChar() {
        try {
            return curr.buffer.getChar();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Character.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getChar();
        }
    }

    public void putChar(char v) {
        try {
            curr.buffer.putChar(v);
        } catch (BufferOverflowException e) {
            tmp.clear();
            tmp.putChar(v);
            tmp.flip();
            while (tmp.hasRemaining()) {
                put(tmp.get());
            }
        }
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public void putFloat(float v) {
        putInt(Float.floatToRawIntBits(v));
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public void putDouble(double v) {
        putLong(Double.doubleToRawLongBits(v));
    }

    public boolean getBoolean() {
        return get() != 0;
    }

    public void putBoolean(boolean v) {
        put((byte) (v ? 1 : 0));
    }

    public void get(byte[] dst, int offset, int length) {
        try {
            curr.buffer.get(dst, offset, length);
        } catch (BufferUnderflowException e) {
            int end = offset + length;
            for (int i = offset; i < end; i++)
                dst[i] = get();
        }
    }

    public void put(byte[] src, int offset, int length) {
        Objects.requireNonNull(src, "ByteArraysIsNull");
        try {
            curr.buffer.put(src, offset, length);
        } catch (BufferUnderflowException e) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                this.put(src[i]);
            }
        }
    }

    public long getLong() {
        try {
            return curr.buffer.getLong();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Long.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getLong();
        }
    }

    public BigBuffer putLong(int lowAddress, byte highestAddressByte) {
        long l = Utils.NativeByteOrderBigEndian ? ((long) lowAddress << 32) + highestAddressByte : ((long) highestAddressByte << (64 - 8)) + lowAddress;
        return putLong(l);
    }

    public int getLongLowAddressInt() {
        long l = getLong();
        return (int) (Utils.NativeByteOrderBigEndian ? l >> 32 : l);
    }

    public BigBuffer put(byte v) {
        try {
            curr.buffer.put(v);
        } catch (BufferOverflowException t) {
            offset(offset());
            put(v);
        }
        return this;
    }

    public byte get() {
        try {
            return curr.buffer.get();
        } catch (BufferUnderflowException e) {
            offset(offset());
            return get();
        }
    }


    private int markPos;
    private PageBuffer markPageBuff;


    //只提供byte的原子操作
    public byte markGet(long skip) {
        long currOffset;
        try {
            currOffset = offset();
        } catch (UnsupportedOperationException e) {
            offset(0);
            return markGet(skip);
        }
        long offset = currOffset + skip;
        int pos = (int) offset & pageMaxPos;
        int page = (int) (offset >> pageBitSize);
        markPageBuff = getPageBuffer(page);
        if (markPageBuff == null) {
            throw new BufferUnderflowException();
        }
        byte b = markPageBuff.buffer.get(pos);
        markPos = pos;
        return b;
    }

    public byte getMark() {
        try {
            return Utils.getByte(markPageBuff.address + markPos);
        } catch (NullPointerException t) {
            throw new UnsupportedOperationException("NotMarked");
        }
    }

    public void clean() {
        if (curr != null) {
            ((DirectBuffer) curr.buffer).cleaner().clean();
        }
        if (markPageBuff != null) {
            ((DirectBuffer) markPageBuff.buffer).cleaner().clean();
        }
    }


    public void force() {
        curr.buffer.force();
    }

    @Override
    public String toString() {
        return "BigBuffer{" + "curr=" + curr + '}';
    }
}
