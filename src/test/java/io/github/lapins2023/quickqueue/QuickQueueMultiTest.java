package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QuickQueueMultiTest {
    @Test
    public void name() throws IOException, InterruptedException {
        File dir = new File("tmp/t2");
//        FileUtils.clean(dir);
        QuickQueueMulti quickQueueMulti0 = new QuickQueueMulti(dir, "rw", "Az0");
        QuickQueueMulti quickQueueMulti1 = new QuickQueueMulti(dir, "rw", "Az1");
        ExecutorService executorService0 = Executors.newSingleThreadExecutor();
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 30; i++) {
            int finalI = i;
            if (finalI % 2 == 0) {
                executorService0.execute(() -> {
                    System.out.println(Thread.currentThread() + ","
                            + quickQueueMulti0.newMessage().packInt(finalI).writeMessage());
                });
            }else {
                executorService1.execute(() -> {
                    System.out.println(Thread.currentThread() + ","
                            + quickQueueMulti1.newMessage().packInt(finalI).writeMessage());
                });
            }
        }
        executorService0.awaitTermination(1, TimeUnit.DAYS);
        executorService1.awaitTermination(1, TimeUnit.DAYS);
        for (QuickQueueMessage quickQueueMessage : quickQueueMulti0.createReader()) {
            System.out.println(quickQueueMessage.unpackInt());
        }

    }
}