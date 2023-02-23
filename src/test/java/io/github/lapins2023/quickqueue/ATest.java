package io.github.lapins2023.quickqueue;

import org.junit.Test;
import sun.misc.Unsafe;

import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class ATest {


    @Test
    public void restartable3b() {
        BigInteger bigInteger = BigInteger.valueOf(Long.MAX_VALUE);
        System.out.println(bigInteger.shiftLeft(64));
        System.out.println(Long.MAX_VALUE);
        System.out.println(Integer.toBinaryString(Integer.MAX_VALUE));
//        System.out.println(Integer.toBinaryString(Byte.MAX_VALUE));
//        System.out.println(Integer.toBinaryString(Byte.MIN_VALUE));
//        System.out.println(Integer.toBinaryString(Byte.toUnsignedInt(Byte.MAX_VALUE)));
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
