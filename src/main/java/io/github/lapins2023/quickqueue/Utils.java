package io.github.lapins2023.quickqueue;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.function.Function;

abstract class Utils {
    static final int ONE_KB = 1024;
    static final int ONE_MB = ONE_KB * ONE_KB;
    static final int ONE_GB = ONE_KB * ONE_MB;


    final static String DATA_EXT = ".qd";
    final static String INDEX_EXT = ".qx";
    final static byte FLAG = 127;
    final static int IX_MSG_LEN = (1 << 4);
    final static int FLAG_OFF = IX_MSG_LEN - 1;


    final static int PAGE_SIZE = Integer.parseInt(System.getProperty("QKQPsz", String.valueOf(ONE_GB)));


    static final Unsafe UNSAFE;

    static final boolean NativeByteOrderBigEndian;
    static final ByteOrder NativeByteOrder;

    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = () -> {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(null);
            };

            UNSAFE = AccessController.doPrivileged(action);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load unsafe", ex);
        }
        long a = UNSAFE.allocateMemory(8);
        try {
            UNSAFE.putLong(a, 0x0102030405060708L);
            byte b = UNSAFE.getByte(a);
            switch (b) {
                case 0x01:
                    NativeByteOrderBigEndian = true;
                    NativeByteOrder = ByteOrder.BIG_ENDIAN;
                    break;
                case 0x08:
                    NativeByteOrderBigEndian = false;
                    NativeByteOrder = ByteOrder.LITTLE_ENDIAN;
                    break;
                default:
                    throw new RuntimeException("Unable to GetByteOrder");
            }
        } finally {
            UNSAFE.freeMemory(a);
        }
    }


    static long getAddress(MappedByteBuffer buffer) {
        return ((DirectBuffer) buffer).address();
    }

    static void assertMode(String mode) {
        if (!(mode.equalsIgnoreCase("r") || mode.equalsIgnoreCase("rw")))
            throw new IllegalArgumentException("mode must r,rw");
    }

    public static long getLastIx(File dir) {
        BigBuffer r = new BigBuffer("r", Utils.PAGE_SIZE, dir, "", Utils.INDEX_EXT);
        long lastIx = -1;
        long size = r.size();
        if (size > 0) {
            long start = size - Utils.PAGE_SIZE;
            Function<Integer, Boolean> flag =
                    n -> r.offset(start + (n << 4) + Utils.FLAG_OFF).get() == Utils.FLAG;
            int left = 0;
            int right = (Utils.PAGE_SIZE >> 4) - 1;
            while (left <= right) {
                int mid = left + ((right - left) >> 1);
                if (flag.apply(mid)) {
                    lastIx = start + ((long) mid << 4);
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        r.clean();
        return lastIx;
    }

    static byte getByte(long address) {
        return UNSAFE.getByte(address);
    }

    static long getLong(long address) {
        return UNSAFE.getLong(address);
    }

    public static void putLong(long address, long v) {
        UNSAFE.putLong(address, v);
    }

    private final static int INT_SZ = Integer.SIZE;
    private final static int LONG_SZ = Long.SIZE;
    private final static int B_SZ = Byte.SIZE;

    public static long toLong(int lowInt, byte highestByte) {
        return NativeByteOrderBigEndian
                ? ((long) lowInt << INT_SZ) + highestByte
                : ((long) highestByte << (LONG_SZ - B_SZ)) + lowInt;
    }

    public static long toLong(int lowInt, byte h1B, byte h2B, byte h3B, byte h4B) {
        return NativeByteOrderBigEndian ?
                ((long) lowInt << INT_SZ) + ((long) h1B << (INT_SZ - B_SZ)) + ((long) h2B << (INT_SZ - (B_SZ * 2))) + ((long) h3B << (INT_SZ - (B_SZ * 3))) + h4B
                : ((long) h4B << (LONG_SZ - B_SZ)) + ((long) h3B << (LONG_SZ - B_SZ * 2)) + ((long) h2B << (LONG_SZ - B_SZ * 3)) + ((long) h1B << (LONG_SZ - B_SZ * 4)) + lowInt;
    }

    public static int getLongLowInt(long l) {
        return (int) (Utils.NativeByteOrderBigEndian ? l >> INT_SZ : l);
    }
}
