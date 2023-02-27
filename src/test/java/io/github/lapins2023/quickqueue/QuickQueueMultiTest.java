package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QuickQueueMultiTest {
    @Test
    public void name() throws IOException, InterruptedException {
        File dir = new File("tmp/t2");
//        FileUtils.clean(dir);
        int pcount = 5;
        QuickQueueMulti[] multis = new QuickQueueMulti[pcount];
        ExecutorService[] ts = new ExecutorService[pcount];
        for (int i = 0; i < multis.length; i++) {
            multis[i] = new QuickQueueMulti(dir, "rw", "Az" + i);
            ts[i] = Executors.newSingleThreadExecutor();
        }
        for (int i = 0; i < 3000; i++) {
            int finalI = i;
            int b = finalI % pcount;
            ts[b].execute(() -> multis[b].newMessage().packInt(finalI).writeMessage());
        }
        for (ExecutorService t : ts) {
            t.shutdown();
            t.awaitTermination(1, TimeUnit.DAYS);
        }
        System.out.println("===============");
        ArrayList<Integer> list = new ArrayList<>();
        for (QuickQueueMessage quickQueueMessage : multis[0].createReader()) {
            int x = quickQueueMessage.unpackInt();
            System.out.println(x);
            list.add(x);
        }
        list.sort(Comparator.naturalOrder());
        System.out.println(list);
        System.out.println(new HashSet<>(list).size());
    }
}