package io.github.lapins2023.quickqueue;

import java.nio.BufferUnderflowException;
import java.util.Iterator;

public class QuickQueueReaderSingle extends QuickQueueReader {
    private final BigBuffer index;
    private final BigBuffer data;

    QuickQueueReaderSingle(QuickQueueSingle qkq) {
        index = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_INDEX);
        data = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_DATA);
        message = new QuickQueueMessage(data);
    }


    public QuickQueueMessage setOffset(long offset) {
        if (offset < 0) {
            return null;
        } else {
            if ((offset >> 4) << 4 != offset) {
                throw new IllegalArgumentException("Offset=" + offset);
            }
            index.offset(offset);
            return next();
        }
    }

    private boolean mark = false;
    private final QuickQueueMessage message;

    public QuickQueueMessage next() {
        byte b;
        if (mark) {
            b = index.getMark();
        } else {
            try {
                b = index.markGet(Utils.FLAG_OFF);
                mark = true;
            } catch (BufferUnderflowException e) {
                return null;
            }
        }
        if (b == Utils.FLAG) {
            long offset = index.offset();
            long dataOffset = index.getLong();
            long len = Utils.getLength(index.getLong());
            mark = false;
            return message.reset(offset, dataOffset, len);
        } else {
            return null;
        }
    }

    public void close() {
        index.clean();
        data.clean();
    }

}
