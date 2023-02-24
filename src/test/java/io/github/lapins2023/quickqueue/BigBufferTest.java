package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;


public class BigBufferTest {

    int size = (int) (1L << 12);

    @Test
    public void offset() throws InterruptedException {
//        System.out.println(Long.toBinaryString(0L << PAGE_BIT_SIZE));
//        System.out.println(Long.toBinaryString(1L << PAGE_BIT_SIZE));
//        System.out.println(Long.toBinaryString(PAGE_SIZE));
//        System.out.println(PAGE_BIT_SIZE);

//        System.out.println(((1L) << (PAGE_BIT_SIZE)) + 2);
//        System.out.println((2L) << (PAGE_BIT_SIZE) | 0);

    }

    @Test
    public void name1() throws IOException {
//        long offset = 4096 * 2 - 1;
//        int pos = (int) (offset & PAGE_LIMIT);
//        int page = Math.toIntExact(offset >> PAGE_BIT_SIZE);
//        System.out.println(pos);
//        System.out.println(page);

        File dir = new File("tmp/qkq/t1");
        FileUtils.clean(dir);
        BigBuffer bigBuffer = new BigBuffer("rw", size, dir, "d-", ".d");
        BigBuffer bigBuffer1 = bigBuffer.putLong( (byte) 1);
    }

    @Test
    public void name() throws IOException {
//        System.out.println(BigBuffer.PAGE_SIZE);
        File dir = new File("tmp/qkq/t1");
        FileUtils.clean(dir);
        BigBuffer bigBuffer = new BigBuffer("rw", size, dir, "d-", ".d");
//        System.out.println(bigBuffer.offset());
        for (int i = 0; i < 10000; i++) {
            BigBuffer put = bigBuffer.put((byte) i);
            bigBuffer.putInt(i);
        }
        System.exit(0);
//        bigBuffer.offset(0);
//        for (int i = 0; i < 100000; i++) {
//            System.out.println(bigBuffer.get() + "|" + bigBuffer.getInt());
//        }
    }
}