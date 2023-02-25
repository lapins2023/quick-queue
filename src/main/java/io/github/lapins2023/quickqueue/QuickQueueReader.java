package io.github.lapins2023.quickqueue;

import java.nio.BufferUnderflowException;
import java.util.Iterator;

public class QuickQueueReader implements AutoCloseable, Iterable<QuickQueueMessage> {
    private final BigBuffer index;
    private final BigBuffer data;

    QuickQueueReader(QuickQueue qkq) {
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
            long len = Utils.getLongLowInt(index.getLong());
            mark = false;
            return message.reset(offset, dataOffset, len);
        } else {
            return null;
        }
    }

    @Override
    public Iterator<QuickQueueMessage> iterator() {
        return new Iterator<QuickQueueMessage>() {
            private QuickQueueMessage quickQueueMessage;

            @Override
            public boolean hasNext() {
                return (quickQueueMessage = QuickQueueReader.this.next()) != null;
            }

            @Override
            public QuickQueueMessage next() {
                return quickQueueMessage;
            }
        };
    }

    public void close() {
        index.clean();
        data.clean();
    }


}
