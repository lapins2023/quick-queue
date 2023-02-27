package io.github.lapins2023.quickqueue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.NotActiveException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QuickQueueMultiTest {
    @Test
    public void name1() throws IOException, InterruptedException {
        File dir = new File("tmp/t2");
        FileUtils.clean(dir);
        new Thread(() -> {
            QuickQueueMulti quickQueueMulti0 = new QuickQueueMulti(dir, "rw", "AA0");
            for (int i = 0; i < 30; i++) {
                quickQueueMulti0.newMessage().packInt(i).writeMessage();
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(() -> {
            QuickQueueMulti quickQueueMulti1 = new QuickQueueMulti(dir, "rw", "AA1");
            for (int i = 0; i < 30; i++) {
                quickQueueMulti1.newMessage().packInt(i * 100).writeMessage();

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        QuickQueueReaderMulti reader = new QuickQueueMulti(dir).createReader();
        while (true) {
            QuickQueueMessage next;
            if ((next = reader.next()) != null) {
                System.out.println(next.unpackInt());
            }
            Thread.sleep(1);
        }
    }

    @Test
    public void name() throws IOException, InterruptedException {
        File dir = new File("tmp/t2");
//        FileUtils.clean(dir);
        int pcount = 5;
        QuickQueueMulti[] multis = new QuickQueueMulti[pcount];
        ExecutorService[] ts = new ExecutorService[pcount];
        for (int i = 0; i < multis.length; i++) {
            multis[i] = new QuickQueueMulti(dir);
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

    @Test
    public void name3() throws IOException, InterruptedException {
        for (int j = 0; j < 10; j++) {
            File file = new File("tmp/t1");
            FileUtils.clean(file);
            QuickQueueMulti quickQueue = new QuickQueueMulti(file, "rw", "AA1");
            int dom = 10000000;
            new Thread(() -> {
                QuickQueueReaderMulti reader = quickQueue.createReader();
                QuickQueueMessage x;
                long start = System.currentTimeMillis();
                while (true) {
                    try {
                        x = reader.next();
                    } catch (NotActiveException e) {
                        throw new RuntimeException(e);
                    }
                    if (x == null) {
                        continue;
                    }
                    int i = x.unpackInt();
                    if (i == dom) {
                        break;
                    }
                }
                printUs(start, dom);
                reader.close();
                synchronized (file) {
                    file.notifyAll();
                }
            }).start();
            long s1 = System.currentTimeMillis();
            for (int i = 0; i <= dom; i++) {
                long l = quickQueue.newMessage()
                        .packInt(i)
                        .writeMessage();
            }
            printUs(s1, dom);
            System.out.println("-------");
            synchronized (file) {
                file.wait();
            }
            System.out.println();
            System.out.println("=============");
        }
//        Thread.sleep(Integer.MAX_VALUE);
    }

    private void printUs(long start, long count) {
        System.out.println(((double) (System.currentTimeMillis() - start) / count) * 1000 + " us");
    }
}