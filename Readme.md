# Quick-Queue

Quick-Queue是一个Java进程内高性能，低延迟，零拷贝，持久化，消息队列。可做为进程间共享内存通信使用。

## Quick-Queue特性

* 支持超低延迟的持久化的消息队列。
* 单线程支持高达每秒数百万次写入。
* 基本类型数据零拷贝，优化的低GC的代码，大幅减少STW产生。
* 支持多线程/多进程读取。支持顺序写，顺序读，随机读。
* 可实现同进程以及进程间"发布订阅"模式，作为进程间共享内存通信组件。
* 支持多副本同步和主从切换功能。解决单点故障风险，防止数据丢失，提供高可用。（Pro功能）
* 支持数据压缩功能，减少硬盘使用。（Pro功能）

## 使用示例

### 初始化

```jshelllanguage
//dataDir需要是一个目录，当目录不存在时，会自动创建。
    QuickQueue quickQueue = new QuickQueue(dataDir);
```

### 写入队列

```jshelllanguage
    //打开写入
    Writer writer = quickQueue.openWrite();
    for (int i = 0; i < 10; i++) {
        long offset = writer.newMessage()
                .packInt(i)
                .packBigDecimal(BigDecimal.valueOf(i)) //BigDecimal使用二进制序列化的方式，如需要跨语言可以使用String类型或Decimal128
                .packString(String.valueOf(i)) //// packString只支持ascii，如果需要存储Unicode如中文请使用packUnicode
                .packBoolean(i % 2 == 0)
                .writeMessage(); //调用writeMessage进行消息写入
        //offset是消息的Id，可以使用offset进行消息读取。offset为有序递增
        System.out.println("offset=" + offset);
    }
```

### 读取队列

仅读取一次，可以设置从offset开始读

```jshelllanguage
{
    for (QuickQueueMessage message : quickQueue.createReader()) {
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
    QuickQueueReader reader = quickQueue.createReader();
    //setOffset 会返回当前message
    System.out.println(reader.setOffset(80).unpackInt());
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
```

一直读，当有数据写入时可实时读取到，也可设置从offset开始读

```jshelllanguage
{
    QuickQueueReader reader = quickQueue.createReader();
    reader.setOffset(32);
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
```

随机读取offset对应的消息

```jshelllanguage
{
    QuickQueueReader reader = quickQueue.createReader();
    QuickQueueMessage message = reader.setOffset(32);
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
```

## 场景使用示例

### 发布订阅场景

生产者和消费者以及消费者间可以同进程也可跨进程

#### 单生产者,单/多消费者

生产者

```jshelllanguage
for (int i = 0; i < 1000000; i++) {
    long offset = quickQueue.newMessage()
            .packInt(i)
            .writeMessage();
    System.out.println("w] " + offset + ":" + i);
    Thread.sleep(500);
}
```

消费者

```jshelllanguage
{
    QuickQueueReader reader = quickQueue.createReader();
    QuickQueueMessage message;
    while (true) {
        if ((message = reader.next()) != null) {
            System.out.println("r] " + Thread.currentThread().getName() + "|" + message.getOffset() + ":" + message.unpackInt());
        } else {
            Thread.yield();
        }
    }
}
```

#### 不同Topic多生产者，单消费者顺序消费

**不同消息主题的多生产者**，每个生产者写入各自的Quick-Queue队列中，消费者分别读取队列，然后放到线程安全的本地队列中。

```jshelllanguage
{
    long offset = quickQueueProducer1.newMessage()
            .packLong(topic1)
            .writeMessage();
    System.out.println("w1] " + offset + ":" + i);
    long offset2 = quickQueueProducer2.newMessage()
            .packLong(topic2)
            .writeMessage();
    System.out.println("w2] " + offset + ":" + i);
}
```

消费者分别读取队列，然后放到本地线程安全队列中进行消费。

```jshelllanguage
{
    ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();
    new Thread(() -> {
        QuickQueueReader reader = quickQueue1.createReader();
        QuickQueueMessage message;
        while (true) {
            if ((message = reader.next()) != null) {
                queue.add(message.unpackLong());
            } else {
                Thread.yield();
            }
        }
    }).start();
    new Thread(() -> {
        QuickQueueReader reader = quickQueue2.createReader();
        QuickQueueMessage message;
        while (true) {
            if ((message = reader.next()) != null) {
                queue.add(message.unpackLong());
            } else {
                Thread.yield();
            }
        }
    }).start();
    while (true) {
        Long value = queue.poll();
    }
}
```

#### 同Topic多生产者，单/多消费者

**同消息主题的多生产者**，每个生产者写入各自的Quick-Queue队列中。

```jshelllanguage
long offset = quickQueueProducerSelf.newMessage()
        .packLong(id)//有序ID，比如高精度时间等
        .writeMessage();
    System.out.println("w] " + offset + ":" + i);
```

消费者读取所有的Quick-Queue队列，通过ID排序消费

```jshelllanguage
{
    class Reader {
        final QuickQueueReader reader;
        long id;
        QuickQueueMessage message;

        Reader(QuickQueueReader reader) {
            this.reader = reader;
        }
    }
    Reader[] readers = {
            new Reader(quickQueue1.createReader(0)),
            new Reader(quickQueue2.createReader(200)),
            new Reader(quickQueue3.createReader(10))
    };
    while (true) {
        int lowest = -1;
        //排序可以优化
        for (int i = 0; i < readers.length; i++) {
            Reader reader = readers[i];
            if (reader.message == null) {
                if ((reader.message = reader.reader.next()) != null)
                    reader.id = reader.message.unpackLong();
            }
            if (reader.message == null) continue;
            if (lowest == -1) lowest = i;
            else if (reader.id < readers[lowest].id) lowest = i;
        }
        if (lowest > -1) {
            Reader r = readers[lowest];
            QuickQueueMessage message = r.message;
            ///TODO onMessage
            if ((r.message = r.reader.next()) != null) r.id = r.message.unpackLong();
        }
    }
}
```

### 数据结构描述

使用第一个字节进行数据结构类型的描述

```jshelllanguage
{
    quickQueue.newMessage()
            .packByte((byte) 1)//成交信息
            .packDouble(1.1)//tradePrice
            .packDouble(1.2)//tradeAmount
            .writeMessage();
    quickQueue.newMessage()
            .packByte((byte) 2)//订单信息
            .packString("orderId")//orderId
            .packBigDecimal(new BigDecimal("0.1"))//price
            .writeMessage();
    for (QuickQueueMessage message : quickQueue.createReader()) {
        byte type = message.unpackByte();
        if (type == 1) {
            double tradePx = message.unpackDouble();
            double tradeAmount = message.unpackDouble();
            System.out.println("tradePx=" + tradePx + ",tradeAmount=" + tradeAmount);
        } else if (type == 2) {
            String orderId = message.unpackString();
            BigDecimal px = message.unpackBigDecimal();
            System.out.println("orderId=" + orderId + ",px=" + px);
        }
    }
}
```

### 事件日志流

该场景可实现WAL机制，从而实现数据可靠的内存操作。建议使用Pro版本保障数据可靠。

```jshelllanguage
{
    //.....读取数据快照到内存中.....
    HashMap<String, BigDecimal> assetsMap = new HashMap<>();
    assetsMap.put("U-A", new BigDecimal("5"));
    assetsMap.put("U-B", new BigDecimal("3"));
    for (int i = 0; i < 2; i++) {
        //U-A 转"2"到 U-B
        //校验操作是否通过
        BigDecimal userA_Amt = assetsMap.get("U-A");
        BigDecimal transferAmt = new BigDecimal("2");
        if (userA_Amt.compareTo(transferAmt) < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal newAAmt = userA_Amt.subtract(transferAmt);
        BigDecimal newBAmt = assetsMap.getOrDefault("U-B", BigDecimal.ZERO).add(transferAmt);
        //存储事件日志
        byte MsgType_Transfer = 10;
        quickQueue.newMessage()
                .packByte(MsgType_Transfer)
                .packLong(System.currentTimeMillis())
                .packString("U-A") //发出方：A
                .packString("U-B") //接收方：B
                .packBigDecimal(new BigDecimal("2")) //转账金额
                .packBigDecimal(newAAmt)//划转后A的金额
                .packBigDecimal(newBAmt)//划转后B的金额
                .writeMessage();
        //修改内存数据,多线程应用中需注意一致性
        assetsMap.put("U-A", newAAmt);
        assetsMap.put("U-B", newBAmt);
    }
    //.....消费事件消息到快照中.....
}
```

## 结构详解

实现思路借鉴非连续内存中的页式管理。利用MMap进行分页，使用 页号+位移量 组成连续的逻辑地址。

文件结构

```
dataDir:
    0.qd //数据页文件
    0.qx //索引页文件
    ....
```

消息结构

```
索引：
索引使用两个long组成，所以offset总是16的倍数，
第一个long为消息在dataBuffer的开始位置，第二long由消息长度和一个字节的结束标识组成。
结束标识会因大端或小端原因，导致出现在long的高字节位或低字节位，但始终会在高地址位。
读取时会通过自旋的方式对结束标识进行读取。当出现结束标识时开始进行消息读取。

dataBeginOffset(long) | (messageSize(4字节) 保留段(3字节) 结束标识(1字节))
offset 组成为 (page << pageBitSize) + pos
```

## Pro版本功能

* 多副本一致性写入
* 主从切换
* 数据压缩

## 注意

* QuickQueue不是线程安全的。
* 字节顺序取决于NativeByteOrder，非Java默认的大端。
* 消息读取时，不是类型安全的，并有溢出风险的，读取时候要十分小心。
* QuickQueue每个文件页的大小默认为1GB，每页大小必须为2的幂次方可通过`System.setProperty("QKQPsz",...)`修改，不建议修改。
* 队列容量最大长度取决于索引Buffer地址或者数据Buffer地址最大值需小于 `(IntMax << pageBitSize) + pos`
  ,因为Page为Int类型，不建议每页设置过小，当每页为1GB时，最大值为2048PB
* 单个消息体的最大长度为 Integer.Max
* 单个大于1字节的数据可能会在不同数据页文件中。
* 默认为异步刷盘，硬盘操作使用内存映射文件所以即使应用崩溃时，系统也会自动完成刷盘。也可以使用QuickQueue.force()
  进行强制刷盘，但会造成性能损失，Pro版本多副本一致性写入可解决故障风险并仅轻微影响性能。
