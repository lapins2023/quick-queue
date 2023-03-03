package io.github.lapins2023.quickqueue;


import jnr.ffi.annotations.In;
import net.openhft.chronicle.bytes.SyncMode;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class QuickQueueTest {
    static {
//        System.setProperty("QKQPsz", String.valueOf(1L << 12));
    }

    @Test
    public void name1() throws IOException {
        System.out.println(Integer.MAX_VALUE);
        System.out.println(Integer.MAX_VALUE / 2);
        System.out.println(Utils.ONE_GB - 1);
        System.out.println(Long.toBinaryString(Utils.ONE_GB - 1));
        System.out.println(Integer.SIZE - Integer.numberOfLeadingZeros(Utils.ONE_GB - 1));
        ByteBuffer allocate = ByteBuffer.allocate(10);
        System.out.println(allocate.limit());
        System.out.println(allocate.capacity());
    }

    //37712
    @Test
    public void name() throws IOException {
        File file = new File("tmp/t1");
        FileUtils.clean(file);
        QuickQueue quickQueue = new QuickQueue(file, "rw");
        {
            for (int i = 0; i < 256; i++) {
                long l = quickQueue.newMessage()
                        .packInt(i)
                        .writeMessage();
                System.out.println(l);
            }
            System.out.println("=============");
            quickQueue.close();
        }


        try (QuickQueueReader reader = quickQueue.createReader()) {
            QuickQueueMessage x;
            while ((x = reader.next()) != null) {
                System.out.println(x.unpackInt());
            }
        }
    }

    //0.46
    @Test
    public void name3() throws IOException, InterruptedException {
        for (int j = 0; j < 10; j++) {
            File file = new File("tmp/t1");
            FileUtils.clean(file);
            QuickQueue quickQueue = new QuickQueue(file, "rw");
            int dom = 10000000;
            new Thread(() -> {
                QuickQueueReader reader = quickQueue.createReader();
                QuickQueueMessage x;
                long start = System.currentTimeMillis();
                while (true) {
                    x = reader.next();
                    if (x == null) {
                        continue;
                    }
                    int i = x.unpackInt();
                    if (i == dom) {
                        break;
                    }
                }
                //0.0206 us
                printUs(start, dom);
                reader.close();
                synchronized (file) {
                    file.notifyAll();
                }
            }).start();
            long s1 = System.currentTimeMillis();
            for (int i = 0; i <= dom; i++) {
                long l = quickQueue.newMessage()
                        .packInt(i)
                        .writeMessage();
            }
            //0.0159 us
            printUs(s1, dom);
            System.out.println("-------");
            synchronized (file) {
                file.wait();
            }
            System.out.println();
            System.out.println("=============");
        }
//        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void name4() throws IOException, InterruptedException {
        File file = new File("tmp/t2");
        FileUtils.clean(file);
        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file).syncMode(SyncMode.SYNC).bufferCapacity(Integer.MAX_VALUE).build();
        ExcerptAppender appender = queue.acquireAppender();
        int dom = 10000000;
//        new Thread(() -> {
//            ExcerptTailer tailer = queue.createTailer();
//            long start = System.currentTimeMillis();
//            while (true) {
//                tailer.readBytes(r -> {
//                    int i = r.readInt();
//                    if (i == dom) {
//                        printUs(start, dom);
//                        synchronized (file) {
//                            file.notifyAll();
//                        }
//                    }
//                });
//            }
//
//        }).start();
        System.out.println(1);
        long s1 = System.currentTimeMillis();
        for (int i = 0; i <= dom; i++) {
            int finalI = i;
            appender.writeText("1");
        }

        printUs(s1, dom);
        System.out.println("=============");
        synchronized (file) {
            file.wait();
        }
//        queue.close();
//        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void name41b() throws InterruptedException, IOException {
//        File file = new File("tmp/t2");
//        FileUtils.clean(file);
//        ChronicleQueue queue = SingleChronicleQueueBuilder.single(file).build();
//        new Thread(() -> {
//            ExcerptTailer tailer = queue.createTailer("a");
//
//            while (true) {
//                tailer.readBytes(b -> {
//                    System.out.println((double) (System.nanoTime() - (b.readLong())) / 1000);
//                });
////                Thread.yield();
//            }
//        }).start();
//
//        ExcerptAppender appender = queue.acquireAppender();
//        for (int i = 0; i < 1000; i++) {
//            appender.writeBytes(b -> b.writeLong(System.nanoTime()));
//            Thread.sleep(300);
//        }
    }

    @Test
    public void name41b1() throws InterruptedException, IOException {
        File file = new File("tmp/t1");
        FileUtils.clean(file);
        QuickQueue quickQueue = new QuickQueue(file, "rw");
        int dom = 10000000;
        new Thread(() -> {
            QuickQueueReader reader = quickQueue.createReader();
            QuickQueueMessage x;
            while (true) {
                x = reader.next();
                if (x == null) {
                    continue;
                }
                System.out.println((double) (System.nanoTime() - (x.unpackLong())) / 1000);
            }
        }).start();
        for (int i = 0; i <= dom; i++) {
            long l = quickQueue.newMessage()
                    .packLong(System.nanoTime())
                    .writeMessage();
            Thread.sleep(300);
        }
    }


    private void printUs(long start, long count) {
//        System.out.println(System.currentTimeMillis() - start);
        System.out.println(((double) (System.currentTimeMillis() - start) / count) * 1000 + " us");
    }
}