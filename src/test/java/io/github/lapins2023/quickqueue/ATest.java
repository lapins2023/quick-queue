package io.github.lapins2023.quickqueue;

import org.junit.Test;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class ATest {


    @Test
    public void r1() throws IOException, InterruptedException {
        File file = new File("tmp/a");
        try {
            FileChannel rw = new RandomAccessFile(file, "rw").getChannel();
            while (true) {
                FileLock x = rw.tryLock();
                Thread.sleep(1);
                if (x != null) {
                    System.out.println(x);
                    break;
                }
            }
            System.out.println(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void r2() throws IOException, InterruptedException {
        File file = new File("tmp/a");
        try {
            FileChannel rw = new RandomAccessFile(file, "rw").getChannel();
            rw.lock();
            System.out.println(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void restartable3b() throws IOException, InterruptedException {

//        ByteBuffer allocate = ByteBuffer.allocate(8);
//        new RandomAccessFile(file, "rw").getChannel().read(allocate);
//        System.out.println(Arrays.toString(allocate.array()));
//        int a = (int) (System.currentTimeMillis() >> 10);
//        System.out.println(a);
//        System.out.println(Integer.toBinaryString(Byte.MAX_VALUE));
//        System.out.println(Integer.toBinaryString(Byte.MIN_VALUE));
//        System.out.println(Integer.toBinaryString(Byte.toUnsignedInt(Byte.MAX_VALUE)));
        String azb1 = "Azb";
        int azb = Utils.toMPN(azb1);
        System.out.println(azb);

        long l = Utils.toStamp(13, azb, Utils.FLAG);
        System.out.println(l);
        System.out.println(Utils.getMPN(l));
    }

    @Test
    public void restartable2abc() {
        BigDecimal x = new BigDecimal("10000.001");
        System.out.println(x.scale());
        System.out.println(x.precision());
        System.out.println(x.unscaledValue());
        MathContext mc = new MathContext(x.precision());
        System.out.println(new BigDecimal(x.unscaledValue(), x.scale()));
    }


    @Test
    public void a1() {
        //257
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8);
        long b = 0;
        for (int a = 0; a < 10; a++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                for (int i = 0; i < 10000; i++) {
                    byteBuffer.clear();
                    byteBuffer.putInt(j).putInt(i);
                    byteBuffer.flip();
                    b = byteBuffer.getLong();
                }
            }
            System.out.println(System.currentTimeMillis() - start + "|" + b);
        }
    }

    @Test
    public void b1() {
        //430,42945378002703
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        long b = 0;
        for (int a = 0; a < 10; a++) {
            long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                for (int i = 0; i < 10000; i++) {
                    byteBuffer.clear();
                    byteBuffer.putInt(j).putInt(i);
                    byteBuffer.flip();
                    b = byteBuffer.getLong();

                }
            }
            System.out.println(System.currentTimeMillis() - start + "|" + b);
        }
    }

    @Test
    public void c7() {
        long i = 1864015815747L;
        System.out.println((int) i);
        System.out.println(i >> 32);
    }

    @Test
    public void c6() {
        //51|42945378002703
        long b = 0;
        for (int a = 0; a < 10; a++) {
            long start = System.currentTimeMillis();
            for (long j = 0; j < 10000; j++) {
                for (int i = 0; i < 10000; i++) {
                    long d = j << 32 + i;
                    b = (int) d;
                    b = d >> 32;
                }
            }
            System.out.println(System.currentTimeMillis() - start + "|" + b);
        }
    }

    @Test
    public void name5() {
        Unsafe unsafe = Utils.UNSAFE;
        long a = unsafe.allocateMemory(8);
        ByteOrder byteOrder;

        try {
            unsafe.putLong(a, 0x0102030405060708L);
            byte b = unsafe.getByte(a);
            switch (b) {
                case 0x01:
                    byteOrder = ByteOrder.BIG_ENDIAN;
                    break;
                case 0x08:
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                    break;
                default:
                    assert false;
                    byteOrder = null;
            }
        } finally {
            unsafe.freeMemory(a);
        }
        System.out.println(byteOrder);
    }
}
