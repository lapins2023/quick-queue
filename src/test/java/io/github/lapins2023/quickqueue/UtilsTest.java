package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class UtilsTest {

    @Test
    public void toLong() {
        long l = Utils.toLong(126);
        ByteBuffer allocate = ByteBuffer.allocate(8).order(Utils.NativeByteOrder);
        allocate.putLong(l);
        System.out.println(Arrays.toString(allocate.array()));
    }
    public static long toLong(int lowInt, byte h1B, byte h2B, byte h3B, byte h4B) {
        return true ?
                ((long) lowInt << Integer.SIZE) + ((long) h1B << (Integer.SIZE - Byte.SIZE)) + ((long) h2B << (Integer.SIZE - (Byte.SIZE * 2))) + ((long) h3B << (Integer.SIZE - (Byte.SIZE * 3))) + h4B
                : ((long) h4B << (64 - 8)) + ((long) h3B << (64 - 8 * 2)) + ((long) h2B << (64 - 8 * 3)) + ((long) h1B << (64 - 8 * 4)) + lowInt;
    }
    @Test
    public void testToLong() {
        System.out.println(3 << 0);
        long l = toLong(126, (byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteBuffer allocate = ByteBuffer.allocate(8);
        allocate.putLong(l);
        System.out.println(Arrays.toString(allocate.array()));
    }
}