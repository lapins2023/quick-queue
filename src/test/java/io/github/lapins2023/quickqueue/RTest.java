package io.github.lapins2023.quickqueue;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class RTest {

    private QuickQueueSingle quickQueueSingle;

    @Before
    public void setUp() throws Exception {
//        File file = new File("tmp/t1");
//        Files.delete(file.toPath());
//        Files.createDirectory(file.toPath());
//        quickQueueSingle = new QuickQueueSingle(file, "rw");
    }

    @Test
    public void t1() {
        for (int i = 0; i < 10; i++) {
            long offset = quickQueueSingle.newMessage()
                    .packInt(i)
                    .packBigDecimal(BigDecimal.valueOf(i)) //BigDecimal使用二进制序列化的方式，如需要跨语言可以使用String类型或BJSON Decimal128
                    .packString(String.valueOf(i)) //// packString只支持ascii，如果需要存储Unicode如中文请使用packUnicode
                    .packBoolean(i % 2 == 0)
                    .writeMessage(); //调用writeMessage进行消息写入
            //offset是消息的Id，可以使用offset进行消息读取。offset为有序递增
            System.out.println("offset=" + offset);
        }
    }

    @Test
    public void t2() {
        for (QuickQueueMessage message : quickQueueSingle.createReader()) {
            int intVal = message.unpackInt();
            BigDecimal decimalVal = message.unpackBigDecimal();
            String stringVal = message.unpackString();
            boolean b = message.unpackBoolean();
            System.out.println(message.getOffset()
                    + ":intVal=" + intVal
                    + ",decimalVal=" + decimalVal
                    + ",stringVal=" + stringVal
                    + ",boolean=" + b);
        }
        System.out.println("---------");
        QuickQueueReaderSingle reader = quickQueueSingle.createReader();
        System.out.println(reader.offset(80).unpackInt());
        reader.forEach((message) -> {
            int intVal = message.unpackInt();
            BigDecimal decimalVal = message.unpackBigDecimal();
            String stringVal = message.unpackString();
            boolean b = message.unpackBoolean();
            System.out.println(message.getOffset()
                    + ":intVal=" + intVal
                    + ",decimalVal=" + decimalVal
                    + ",stringVal=" + stringVal
                    + ",boolean=" + b);
        });
    }

    @Test
    public void t3() throws InterruptedException {
        QuickQueueReaderSingle reader = quickQueueSingle.createReader();
        reader.offset(32);
        while (true) {
            QuickQueueMessage message = reader.next();
            if (message != null) {
                int intVal = message.unpackInt();
                BigDecimal decimalVal = message.unpackBigDecimal();
                String stringVal = message.unpackString();
                boolean b = message.unpackBoolean();
                System.out.println(message.getOffset()
                        + ":intVal=" + intVal
                        + ",decimalVal=" + decimalVal
                        + ",stringVal=" + stringVal
                        + ",boolean=" + b);
            } else {
                Thread.sleep(1);//有实时性要求应用中可使用Thread.sleep(0)或Thread.yield或者BusyWait
            }
        }
    }

    @Test
    public void name31() throws Exception {
        ConcurrentLinkedQueue<Integer> queue1 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> queue2 = new ConcurrentLinkedQueue<>();
        new Thread(() -> {
            while (true) {
                Integer poll = queue1.poll();
                if (poll == null) {
                    continue;
                }
                queue2.add(poll);
            }
        }).start();
        Thread.sleep(3_000);
        new Thread(() -> {
            queue1.add(0);
            AtomicLong start = new AtomicLong(0);
            while (true) {
                Integer i = queue2.poll();
                if (i == null) {
                    continue;
                }
                if (i < 1000000) {
                    if (i == 0) {
                        System.out.println("start");
                        start.set(System.currentTimeMillis());
                    }
                    queue1.add(++i);
                } else {
                    System.out.println("onMessage==" + i);
                    //48402
                    System.out.println("use=" + (System.currentTimeMillis() - start.get()));
                    System.exit(0);
                }
            }
        }).start();
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void d3() throws InterruptedException, IOException {
        File file = new File("tmp/t1");
        File file2 = new File("tmp/t2");
        FileUtils.clean(file);
        FileUtils.clean(file2);
        QuickQueueSingle q1 = new QuickQueueSingle(file, "rw");
        QuickQueueSingle q2 = new QuickQueueSingle(file2, "rw");

        new Thread(() -> {
            try {
                QuickQueueReaderSingle reader = q1.createReader();
                int z = -1;
                while (true) {
                    QuickQueueMessage m = reader.next();
                    if (m == null) {
                        continue;
                    }
                    int i = m.unpackInt();
                    if (i < z) {
                        while (true) {
                            m.pos(0);
                            System.out.println("b:" + m.unpackInt() + ",");
                            Thread.sleep(10_00);
                        }
                    }
                    z = i;
                    q2.newMessage()
                            .packInt(i)
                            .writeMessage();
                }
            } catch (Exception t) {
                throw new RuntimeException(t);
            }
        }).start();
        Thread.sleep(3_000);
        new Thread(() -> {
            try {
                q1.newMessage().packInt(0).writeMessage();
                AtomicLong start = new AtomicLong(0);
                QuickQueueReaderSingle reader = q2.createReader();
                int z = -1;
                while (true) {
                    QuickQueueMessage m = reader.next();
                    if (m == null) {
                        continue;
                    }
                    int i = m.unpackInt();
                    if (i < z) {
                        while (true) {
                            m.pos(0);
                            System.out.println("a:" + m.unpackInt() + ",");
                            Thread.sleep(10_00);
                        }
                    }
                    z = i;
                    if (i < 1000000) {
                        if (i == 0) {
                            System.out.println("start");
                            start.set(System.currentTimeMillis());
                        }
                        q1.newMessage().packInt(++i).writeMessage();
                    } else {
                        System.out.println("onMessage==" + i);
                        //38629
                        System.out.println("use=" + (System.currentTimeMillis() - start.get()));
                        System.exit(0);
                    }
                }
            } catch (Exception t) {
                throw new RuntimeException(t);
            }
        }).start();
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void d4() throws InterruptedException, IOException {
        File file = new File("tmp/t1");
        File file2 = new File("tmp/t2");
        FileUtils.clean(file);
        FileUtils.clean(file2);
        QuickQueue q1 = new QuickQueueMulti(file, "rw", "A11");
        QuickQueue q2 = new QuickQueueMulti(file2, "rw", "A12");

        Thread thread = new Thread(() -> {
            try {
                QuickQueueReader reader = q1.createReader();
                int z = -1;
                while (true) {
                    QuickQueueMessage m = reader.next();
                    if (m == null) {
                        continue;
                    }
                    int i = m.unpackInt();
                    if (i < z) {
                        while (true) {
                            m.pos(0);
                            System.out.println("o:" + m.unpackInt() + ",");
                            Thread.sleep(10_00);
                        }
                    }
                    z = i;
                    long l = q2.newMessage()
                            .packInt(i)
                            .writeMessage();
//                    System.out.println(l + ":" + i);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setName("b");
        thread.start();
        Thread.sleep(3_000);
        Thread start1 = new Thread(() -> {
            try {
                AtomicLong start = new AtomicLong(0);
                QuickQueueReader reader = q2.createReader();
                q1.newMessage().packInt(0).writeMessage();
                int z = -1;
                while (true) {
                    QuickQueueMessage m = reader.next();
                    if (m == null) {
                        continue;
                    }
                    int i = m.unpackInt();
//                    System.out.println(m.getOffset() + "," + i);
                    if (i < 1000000) {
                        if (i == 0) {
                            System.out.println("start");
                            start.set(System.currentTimeMillis());
                        }
                        if (i < z) {
                            while (true) {
                                m.pos(0);
                                System.out.println("b:" + m.unpackInt() + ",");
                                Thread.sleep(10_00);
                            }
                        }
                        z = i;
                        q1.newMessage().packInt(++i).writeMessage();
                    } else {
                        System.out.println("onMessage==" + i);
                        //484
                        System.out.println("use=" + (System.currentTimeMillis() - start.get()));
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        start1.setName("a");
        start1.start();
        Thread.sleep(Integer.MAX_VALUE);
    }
}
