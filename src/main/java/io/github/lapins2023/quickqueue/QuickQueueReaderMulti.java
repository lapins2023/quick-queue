package io.github.lapins2023.quickqueue;

import java.io.File;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class QuickQueueReaderMulti implements AutoCloseable, Iterable<QuickQueueMessage> {
    private final BigBuffer index;
    private final QuickQueueMulti qkq;
    private final Map<Integer, Data> data = new HashMap<>();

    public class Data {
        final BigBuffer buffer;
        final QuickQueueMessage message;
        int last;

        public Data(int mpn) {
            buffer = new BigBuffer("r", Utils.PAGE_SIZE, Utils.mkdir(new File(qkq.dir, Utils.fromMPN(mpn))), "", Utils.EXT_DATA);
            message = new QuickQueueMessage(buffer);
        }
    }

    QuickQueueReaderMulti(QuickQueueMulti qkq) {
        index = new BigBuffer("r", Utils.PAGE_SIZE, qkq.dir, "", Utils.EXT_INDEX);
        this.qkq = qkq;
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

    public QuickQueueMessage next() {
        byte b;
        if (mark) {
            b = index.getMark();
        } else {
            try {
                b = index.markGet(Utils.MNP_OFF);
                mark = true;
            } catch (BufferUnderflowException e) {
                return null;
            }
        }
        if (b > 0) {
            long offset = index.offset();
            long dataOffset = index.getLong();
            long stamp = index.getLong();
            if (Utils.notFlag(stamp)) {
                index.offset(offset);
                return null;
            }
            int mpn = Utils.getMPN(stamp);
            long len = Utils.getLength(stamp);
            QuickQueueMessage message =
                    data.computeIfAbsent(mpn, Data::new)
                            .message.reset(offset, dataOffset, len);
            mark = false;
            return message;
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
                return (quickQueueMessage = QuickQueueReaderMulti.this.next()) != null;
            }

            @Override
            public QuickQueueMessage next() {
                return quickQueueMessage;
            }
        };
    }

    public void close() {
        index.clean();
        for (Data data : data.values()) {
            data.buffer.clean();
        }
    }


}
