package io.github.lapins2023.quickqueue;

import java.io.IOException;
import java.util.Iterator;

public abstract class QuickQueueReader implements AutoCloseable, Iterable<QuickQueueMessage> {
    public abstract QuickQueueMessage set(long offset) throws IOException;

    public abstract QuickQueueMessage next() throws IOException;

    public abstract QuickQueueMessage last() throws IOException;

    @Override
    public Iterator<QuickQueueMessage> iterator() {
        return new Iterator<QuickQueueMessage>() {
            private QuickQueueMessage quickQueueMessage;

            @Override
            public boolean hasNext() {
                try {
                    return (quickQueueMessage = QuickQueueReader.this.next()) != null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public QuickQueueMessage next() {
                return quickQueueMessage;
            }
        };
    }
}
