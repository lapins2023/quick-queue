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
        Utils.assertMode(mode);
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
            if (pb != null && pb.page == page) return pb;
            if (pbM != null && pbM.page == page) return pbM;
            File file = new File(dir, fileNamePrefix + page + fileNameSuffix);
            boolean r = mode.equals("r");
            if (r && (!file.exists() || file.length() < pageSize)) {
                return null;
            }
            if (cOffset(page, pageSize) < 0) {
                throw new Exception("PageOverflow=" + page);
            }
            try (RandomAccessFile raf = new RandomAccessFile(file, mode)) {
                if (r && raf.length() < pageSize) {
                    return null;
                }
                try (FileChannel channel = raf.getChannel()) {
                    MappedByteBuffer map = (MappedByteBuffer) channel
                            .map(r ? FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE
                                    , 0, pageSize)
                            .order(Utils.NativeByteOrder);
                    return new PageBuffer(page, map);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("MMapFailed=" + page, t);
        }
    }

    private PageBuffer pb;

    private long cOffset(long page, int pos) {
        return (page << pageBitSize) + pos;
    }

    public long offset() {
        int page;
        try {
            page = pb.page;
        } catch (NullPointerException e) {
            throw new UnsupportedOperationException("CurrPageNotOpen");
        }
        return cOffset(page, pb.buffer.position());
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
        if (pb == null || pb.page != page) {
            PageBuffer curr_ = getPageBuffer(page);
            if (curr_ == null) throw new BufferUnderflowException();
            if (pb != null) ((DirectBuffer) pb.buffer).cleaner().clean();
            pb = curr_;
        }
        if (pb.buffer.position() != pos) {
            pb.uPosition(pos);
        }
        return this;
    }

    public BigBuffer skip(int skip) {
        if (pb.buffer.remaining() > skip) {
            pb.uPosition(pb.buffer.position() + skip);
        } else {
            offset(offset() + skip);
        }
        return this;
    }

    private final ByteBuffer tmp = ByteBuffer.allocateDirect(Long.BYTES)
            .order(Utils.NativeByteOrder);

    public BigBuffer putInt(int i) {
        try {
            pb.buffer.putInt(i);
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

    public BigBuffer putShort(short i) {
        try {
            pb.buffer.putShort(i);
        } catch (BufferOverflowException e) {
            tmp.clear();
            tmp.putShort(i);
            tmp.flip();
            while (tmp.hasRemaining()) {
                put(tmp.get());
            }
        }
        return this;
    }


    public int getInt() {
        try {
            return pb.buffer.getInt();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Integer.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getInt();
        }
    }

    public short getShort() {
        try {
            return pb.buffer.getShort();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Short.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getShort();
        }
    }

    public BigBuffer putLong(long i) {
        try {
            pb.buffer.putLong(i);
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
            return pb.buffer.getChar();
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
            pb.buffer.putChar(v);
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
            pb.buffer.get(dst, offset, length);
        } catch (BufferUnderflowException e) {
            int end = offset + length;
            for (int i = offset; i < end; i++)
                dst[i] = get();
        }
    }

    public void put(byte[] src, int offset, int length) {
        Objects.requireNonNull(src, "ByteArraysIsNull");
        try {
            pb.buffer.put(src, offset, length);
        } catch (BufferUnderflowException e) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                this.put(src[i]);
            }
        }
    }

    public long getLong() {
        try {
            return pb.buffer.getLong();
        } catch (BufferUnderflowException e) {
            tmp.clear();
            for (int i = 0; i < Long.BYTES; i++) {
                tmp.put(get());
            }
            tmp.flip();
            return tmp.getLong();
        }
    }

    public boolean compareAndSwapLong(long expect, long update) {
        return pb.buffer.remaining() >= 8 && Utils.UNSAFE.compareAndSwapLong(null, pb.address + pb.buffer.position(), expect, update);
    }

    public BigBuffer put(byte v) {
        try {
            pb.buffer.put(v);
        } catch (BufferOverflowException t) {
            offset(offset());
            put(v);
        }
        return this;
    }

    public byte get() {
        try {
            return pb.buffer.get();
        } catch (BufferUnderflowException e) {
            offset(offset());
            return get();
        }
    }


    private int markPos;
    private PageBuffer pbM;


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
        pbM = getPageBuffer(page);
        if (pbM == null) {
            throw new BufferUnderflowException();
        }
        byte b = pbM.buffer.get(pos);
        markPos = pos;
        return b;
    }

    public byte getMark() {
        try {
            return Utils.getByte(pbM.address + markPos);
        } catch (NullPointerException t) {
            throw new UnsupportedOperationException("NotMarked");
        }
    }

    public void clean() {
        if (pb != null) {
            ((DirectBuffer) pb.buffer).cleaner().clean();
        }
        if (pbM != null) {
            ((DirectBuffer) pbM.buffer).cleaner().clean();
        }
        pb = null;
        pbM = null;
        markPos = 0;
    }


    public void force() {
        pb.buffer.force();
    }

    @Override
    public String toString() {
        return "BigBuffer{[" + (pb == null ? "-1" : offset()) + "]" + pb + "}";
    }


}
