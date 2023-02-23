package io.github.lapins2023.quickqueue;

import java.util.function.Function;

class WriterSingle extends QuickQueueWriter {

    private final QuickQueue qkq;
    private final BigBuffer index;

    WriterSingle(QuickQueue qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.DATA_EXT));
        this.qkq = qkq;
        index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
        open();
    }

    private void open() {
        BigBuffer r = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
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
        //////
        if (lastIx < 0) {
            data.offset(0);
            index.offset(0);
        } else {
            index.offset(lastIx);
            long dataOffset = index.getLong();
            int dataLength = index.getLongLowAddressInt();
            data.offset(dataOffset + dataLength);
        }
    }

    public void force0() {
        index.force();
    }

    protected long writeMessage0(int length) {
        long offset = index.offset();
        index.putLong(begin)
                .putLong(length, Utils.FLAG);
        return offset;
    }

    void clean() {
        force();
        index.clean();
        data.clean();
    }
}
