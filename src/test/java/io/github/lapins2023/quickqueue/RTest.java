package io.github.lapins2023.quickqueue;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;

public class RTest {

    private QuickQueueSingle quickQueueSingle;

    @Before
    public void setUp() throws Exception {
        File file = new File("tmp/t1");
//        Files.delete(file.toPath());
//        Files.createDirectory(file.toPath());
        quickQueueSingle = new QuickQueueSingle(file,"rw");
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
}
