package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

class WriterMulti extends QuickQueueWriter {
    final BigBuffer index;
    final FileChannel lk;
    final MappedByteBuffer lkBuffer;
    final long lkBuffAddress;
    private final long ixAddress;

    public WriterMulti(QuickQueueMulti qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, new File(qkq.dir, qkq.name), "", Utils.DATA_EXT));
        this.index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
        ixAddress = qkq.ixAddress;
        try {
            try (RandomAccessFile rw = new RandomAccessFile(new File(qkq.dir, qkq.name + ".lk"), "rw")) {
                this.lk = rw.getChannel();
                lk.lock();
                this.lkBuffer = (MappedByteBuffer) lk.map(FileChannel.MapMode.READ_WRITE, 0, 8).order(Utils.NativeByteOrder);
                this.lkBuffAddress = Utils.getAddress(this.lkBuffer);
                long dataEnding = Utils.getLong(lkBuffAddress);
                data.offset(dataEnding);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        n1 = (byte) qkq.name.charAt(0);
        n2 = (byte) qkq.name.charAt(1);
        n3 = (byte) qkq.name.charAt(2);
    }

    @Override
    public void force0() {
        lkBuffer.force();
        index.force();
    }

    private final byte n1;
    private final byte n2;
    private final byte n3;

    @Override
    public long writeMessage0(int length) {
        if (length > 0) {
            Utils.putLong(lkBuffAddress, data.offset());
            long offset = Utils.UNSAFE.getAndAddLong(null, ixAddress, 16);
            index.offset(offset);
            index.putLong(begin)
                    .putLong(length, n1, n2, n3, Utils.FLAG);
            return offset;
        } else {
            return begin;
        }
    }

    public void close() {
        force();
        try {
            lk.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
