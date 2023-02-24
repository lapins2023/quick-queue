package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FileUtils {
    public static File empty(String temp) throws IOException {
        File file = new File("tmp/" + temp);
        clean(file);
        return file;
    }

    public static void clean(File dir) throws IOException {
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                clean(file);
            }
            assert file.delete();
        }
    }

    @Test
    public void name2() throws IOException, InterruptedException {
        File file = new File("tmp/qq");
        File file1 = new File(file, "aa");
        MappedByteBuffer map = new RandomAccessFile(file1, "rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 16);
        long address = Utils.getAddress(map);
        System.out.println(address);
        new Thread(() -> {
            while (true) {
                long andAddLong = Utils.UNSAFE.getAndAddLong(null, address, 1);
                System.out.println(Thread.currentThread().getName() + "," + andAddLong);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void name5() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++) {
            MappedByteBuffer rw = new RandomAccessFile(new File("tmp/" + 1), "rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, Utils.ONE_GB);
        }
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void name3() throws IOException, InterruptedException {
        File file = new File("tmp/qq");
        File file1 = new File(file, "aa");
        MappedByteBuffer map = new RandomAccessFile(file1, "rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 16);
        long address = Utils.getAddress(map);
        System.out.println(address);
        new Thread(() -> {
            while (true) {
                long andAddLong = Utils.UNSAFE.getAndAddLong(null, address, 1);
                System.out.println(Thread.currentThread().getName() + "," + andAddLong);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread.sleep(Integer.MAX_VALUE);
    }

}
