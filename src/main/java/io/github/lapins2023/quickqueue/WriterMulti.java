package io.github.lapins2023.quickqueue;

import java.io.File;

class WriterMulti extends QuickQueueWriter {
    private final QuickQueueMulti qkq;
    private final BigBuffer index;

    public WriterMulti(QuickQueueMulti qkq) {
        super(new BigBuffer("rw", Utils.PAGE_SIZE, new File(qkq.dir, qkq.name), "", Utils.DATA_EXT));
        this.qkq = qkq;
        index = new BigBuffer("rw", Utils.PAGE_SIZE, qkq.dir, "", Utils.INDEX_EXT);
        open();
    }

    public void open() {

    }

    @Override
    public void force0() {
        
    }

    @Override

    public long writeMessage0(int length) {
        return 0;
    }
}
