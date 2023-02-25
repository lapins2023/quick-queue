package io.github.lapins2023.quickqueue;

class WriterSingle extends QuickQueueWriter {

    private final BigBuffer index;

    WriterSingle(QuickQueue qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_DATA));
        index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_INDEX);
        ////////
        long lastIx = Utils.getLastIx(qkq.dir, true);
        if (lastIx < 0) {
            data.offset(0);
            index.offset(0);
        } else {
            index.offset(lastIx);
            long dataOffset = index.getLong();
            int dataLength = Utils.getLongLowInt(index.getLong());
            data.offset(dataOffset + dataLength);
        }
    }

    public void force0() {
        index.force();
    }

    protected long writeMessage0(int length) {
        long offset = index.offset();
        index.putLong(begin)
                .putLong(Utils.toStamp(length));
        return offset;
    }

    void clean() {
        force();
        index.clean();
        data.clean();
    }
}
