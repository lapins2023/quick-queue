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


    final static String EXT_DATA = ".qd";
    final static String EXT_INDEX = ".qx";
    final static String EXT_MP = ".qm";
    final static byte FLAG = 127;
    final static int IX_MSG_LEN = (1 << 4);
    final static int FLAG_OFF = IX_MSG_LEN - 1;
    final static int MNP_OFF = IX_MSG_LEN - 4;


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

    public static long getLastIx(File dir, boolean onlyFlag) {
        BigBuffer r = new BigBuffer("r", Utils.PAGE_SIZE, dir, "", Utils.EXT_INDEX);
        long lastIx = -1;
        long size = r.size();
        if (size > 0) {
            long start = size - Utils.PAGE_SIZE;
            Function<Integer, Boolean> used =
                    n -> {
                        if (onlyFlag) {
                            return r.offset(start + (n << 4) + Utils.FLAG_OFF).get() == Utils.FLAG;
                        } else {
                            r.offset(start + (n << 4));
                            return r.getLong() != 0 || r.getLong() != 0;
                        }
                    };
            int left = 0;
            int right = (Utils.PAGE_SIZE >> 4) - 1;
            while (left <= right) {
                int mid = left + ((right - left) >> 1);
                if (used.apply(mid)) {
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

    public static long toStamp(int length) {
        return NativeByteOrderBigEndian
                ? ((long) length << INT_SZ) | Utils.FLAG
                : ((long) Utils.FLAG << (LONG_SZ - B_SZ)) | length;
    }


    public static long toStamp(int length, int mpn, byte b) {
        return NativeByteOrderBigEndian ?
                ((long) length << INT_SZ) | ((long) mpn << B_SZ) | b
                : ((long) b << (LONG_SZ - B_SZ)) | ((long) mpn << (INT_SZ)) | length;
    }

    public static int getLength(long stamp) {
        return (int) (Utils.NativeByteOrderBigEndian ? stamp >> INT_SZ : stamp);
    }

    public static File mkdir(File dir) {
        if (dir.exists()) {
            if (dir.isFile()) throw new IllegalArgumentException("NotDirFileExists=" + dir);
        } else {
            if (!dir.mkdirs()) throw new IllegalArgumentException("UnableMkdir=" + dir);
        }
        return dir;
    }

    public static boolean notFlag(long stamp) {
        return ((byte) (NativeByteOrderBigEndian ? stamp : stamp >> LONG_SZ - B_SZ)) != FLAG;
    }

    public static int getMPN(long stamp) {
        long l = NativeByteOrderBigEndian ? stamp << INT_SZ : stamp << B_SZ;
        return (int) (l >> (INT_SZ + B_SZ));
    }


    static int toMPN(String name) {
        if (name.length() != 3) {
            throw new IllegalArgumentException("nameMushLength=3");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c != (byte) c || c == 0) {
                throw new IllegalArgumentException("nameMustA-z,0-9");
            }
            if (!((c >= 48 && c <= 57) || (c >= 65 && c <= 90) || (c >= 97 && c <= 122))) {
                throw new IllegalArgumentException("nameMustA-z,0-9");
            }
        }
        return (((0) << 24) |
                ((name.charAt(0) & 0xff) << 16) |
                ((name.charAt(1) & 0xff) << 8) |
                ((name.charAt(2) & 0xff)));
    }

    static String fromMPN(int mpn) {
        return new String(new byte[]{
                (byte) (mpn >> 16), (byte) (mpn >> 8), (byte) mpn
        });
    }
}
