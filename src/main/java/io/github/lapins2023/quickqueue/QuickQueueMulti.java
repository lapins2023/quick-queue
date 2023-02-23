package io.github.lapins2023.quickqueue;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class QuickQueueMulti {
    final File dir;
    final String name;
    private WriterMulti writer;
    final MappedByteBuffer ixBuffer;
    final long ixAddress;

    public QuickQueueMulti(File dir, String mode, String name) {
        Utils.assertMode(mode);
        this.dir = dir;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != (byte) c) throw new IllegalArgumentException("nameMustAscii");
            if (i > 3) throw new IllegalArgumentException("nameMaxLengthIs3");
        }
        this.name = name;

        try (RandomAccessFile raf = new RandomAccessFile(new File(dir, "ix"), mode)) {
            this.ixBuffer = (MappedByteBuffer) raf.getChannel()
                    .map(FileChannel.MapMode.READ_WRITE, 0, Long.MAX_VALUE).order(Utils.NativeByteOrder);
            this.ixAddress = Utils.getAddress(ixBuffer);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        if (mode.equalsIgnoreCase("rw")) {
            this.writer = new WriterMulti(this);
        } else {
            this.writer = null;
        }

    }

}
