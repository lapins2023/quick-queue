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
        System.out.println(quickQueueMulti.newMessage()
                .packInt(1)
                .packInt(2)
                .packInt(3)
                .writeMessage());
    }
}