package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class QuickQueueMultiTest {
    @Test
    public void name() throws IOException {
        File dir = new File("tmp/t2");
        FileUtils.clean(dir);
        QuickQueueMulti quickQueueMulti = new QuickQueueMulti(dir, "rw", "Azz");
        for (int i = 0; i < 3; i++) {
            System.out.println(quickQueueMulti.newMessage()
                    .packInt(i)
                    .writeMessage());
        }
        for (QuickQueueMessage quickQueueMessage : quickQueueMulti.createReader()) {
            System.out.println(quickQueueMessage.unpackInt());
        }

    }
}