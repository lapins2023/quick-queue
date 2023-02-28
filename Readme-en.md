# Quick-Queue

Quick-Queue is a Java in-process high-performance, low-latency, zero-copy, persistent, message queue. It can be used as
shared memory communication between processes.

## Quick-Queue Features

* Support ultra-low latency persistent message queue.
* Single thread supports up to millions of writes per second.
* Zero copy of basic type data, optimized low-GC code, greatly reducing STW generation.
* Support multi-thread, multi-process reading. Support sequential write, sequential read, random read.
* It can realize the "publish and subscribe" mode between the same process and between processes. As an inter-process
  shared memory communication component, it can realize multi-process lock-free writing.
* Support multi-copy synchronization and master-slave switching functions. Solve the risk of single point of failure,
  prevent data loss, and provide high availability. (Pro feature)
* Support data compression function to reduce hard disk usage. (Pro feature)

## Example

### quick-start

```jshelllanguage
//dataDir needs to be a directory, if the directory does not exist, it will be created automatically.
    QuickQueue quickQueue = new QuickQueue(dataDir, "rw");
```

### write

```jshelllanguage
    for (int i = 0; i < 10; i++) {
    long offset = writer.newMessage()
            .packInt(i)
            .packBigDecimal(BigDecimal.valueOf(i)) //BigDecimal uses binary serialization
            .packString(String.valueOf(i)) //// packString only supports ascii, if you need to store Unicode such as Chinese, please use packUnicode
            .packBoolean(i % 2 == 0)
            .writeMessage(); //Call writeMessage to write index
    //offset is the Id of the message, you can use offset to read the message. offset is sequential increment
    System.out.println("offset=" + offset);
}
```

### read queue

Only read once, you can set to start reading from offset

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
    //setOffset will return the current message
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

Always read, when there is data written, it can be read in real time, or it can be set to start reading from offset

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
            Thread.sleep(1);
        }
    }
}
```

Randomly read the message corresponding to offset

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

## Usage

### Publish and subscribe

Producers, consumers, and consumers can be in the same process or across processes

#### Single Producer, Single/Multiple Consumers

Producer

```jshelllanguage
for (int i = 0; i < 1000000; i++) {
    long offset = quickQueue.newMessage()
            .packInt(i)
            .writeMessage();
    System.out.println("w] " + offset + ":" + i);
    Thread.sleep(500);
}
```

Consumer

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

#### Multiple producers of different topics, single consumer sequential consumption

**Multiple producers of different message topics**, each producer writes to its own Quick-Queue queue, and consumers
read the queue separately.

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

Consumers read the queues separately, and can use one thread for each queue to read, or use one thread to switch between
reads.

Single thread switching read

```jshelllanguage
{
    QuickQueueReader reader = quickQueue1.createReader();
    QuickQueueReader reader = quickQueue2.createReader();

    QuickQueueMessage message;
    while (true) {
        if ((message = reader.next()) != null || (message = reader.next()) != null) {
            message.unpackLong();
        } else {
            Thread.yield();
        }
    }
}
```

Multi-threads read separately, and then merge into the local thread-safe queue

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

#### Same topic with multiple producers, single/multiple consumers

**Multiple producers of the same message topic** can be implemented in two ways.

1. Through the lock-free queue, multiple producers write to the same queue.
2. Each producer writes to its own queue and sorts when reading.

##### lock-free queue

Use QuickQueueMulti, the third parameter is the producer name, which must be a combination of 3 letters or numbers

```jshelllanguage
{
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
```

##### Write to their respective queues, sort and consume when reading

Each producer writes to its own Quick-Queue queue

```jshelllanguage
long offset = quickQueueProducerSelf.newMessage()
        .packLong(id)//Ordered ID, such as high-precision time, etc.
        .writeMessage();
    System.out.println("w] " + offset + ":" + i);
```

Consumers read all Quick-Queue queues and sort consumption by ID

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
        //example only
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
            ///onMessage
            if ((r.message = r.reader.next()) != null) r.id = r.message.unpackLong();
        }
    }
}
```

### Data structure description

Use the first byte to describe the data structure type

```jshelllanguage
{
    quickQueue.newMessage()
            .packByte((byte) 1)//transaction information
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

### Event log stream

The WAL mechanism can be implemented to achieve reliable data memory operations. It is recommended to use the Pro
version to ensure data reliability.

```jshelllanguage
{
    //.....Read data snapshot into memory.....
    HashMap<String, BigDecimal> assetsMap = new HashMap<>();
    assetsMap.put("U-A", new BigDecimal("5"));
    assetsMap.put("U-B", new BigDecimal("3"));
    for (int i = 0; i < 2; i++) {
        //U-A 转"2"到 U-B
        //Whether the verification operation passed
        BigDecimal userA_Amt = assetsMap.get("U-A");
        BigDecimal transferAmt = new BigDecimal("2");
        if (userA_Amt.compareTo(transferAmt) < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal newAAmt = userA_Amt.subtract(transferAmt);
        BigDecimal newBAmt = assetsMap.getOrDefault("U-B", BigDecimal.ZERO).add(transferAmt);
        //Storage Event Log
        byte MsgType_Transfer = 10;
        quickQueue.newMessage()
                .packByte(MsgType_Transfer)
                .packLong(System.currentTimeMillis())
                .packString("U-A") //Sender: A
                .packString("U-B") //Receiver: B
                .packBigDecimal(new BigDecimal("2")) //transfer amount
                .packBigDecimal(newAAmt)//The amount of A after the transfer
                .packBigDecimal(newBAmt)//The amount of B after the transfer
                .writeMessage();
        //Modify memory data, pay attention to consistency in multi-threaded applications
        assetsMap.put("U-A", newAAmt);
        assetsMap.put("U-B", newBAmt);
    }
    //.....消费事件消息到快照中.....
}
```

## Detailed explanation

The implementation idea is based on the paging management in non-contiguous memory. Use MMap for paging, and use page
number + displacement to form continuous logical addresses.

file structure

```
dataDir:
    0.qd //data file
    0.qx //index file
    MP1.qm //Multi-producer is a profile lock, only generated when using multi-producer
    MP1://Data files for multi-producers, only when multi-producers are used
      0.qd //data file
    ....
```

message structure

```
index:
The index is composed of two longs, so offset is always a multiple of 16,
The first long is the start position of the message in the dataBuffer, and the second long is composed of the message length and a one-byte end identifier.
The end flag will appear in the high byte or low byte of long due to big end or little end, but it will always be in the high address bit.
When reading, the end identifier will be read by spinning. Message reading starts when the end mark appears.

dataBeginOffset(long) | (messageSize(4字节) Producer name for multiple producers(3字节) 结束标识(1字节))
offset = (page << pageBitSize) + pos
```

Multi-producer lock-free mode

```
The multi-production mode is implemented in a CAS lock-free manner.
When writing, the second long data will be written in the form of CAS, including the name of the producer. When the writing is successful, the producer profile information file will be written, and then the other field information of the index file will be written.
When reading, the corresponding data file will be read through the producer name.
When reading, the producer name can be read, but after the index message end mark cannot be read, it will try to use the file lock to determine whether the producer is running. If the file lock is obtained, it means that the current producer is no longer running. An exception is thrown, and the message is skipped.
After the producer restarts, there is a probability that the unfinished index will be completed.
```

BigDecimal serialization

```
Composition: (identification) (1 byte) | scale | byteArrayLength| unscaledValueBigIntegerByteArray
When both scale and byteArrayLength are less than 128: the identifier is 0 scale and byteArrayLength occupy 1 byte each
Otherwise: the identifier is 1 scale and byteArrayLength occupy 4 bytes each
```

## Pro version features

* Multi-copy consistent write
* Master-slave switching
* data compression

## Notice

* QuickQueue is not thread-safe.
* The byte order depends on NativeByteOrder, which is not Java's default big endian.
* When reading the message, it is not type-safe, and there is a risk of overflow, so be very careful when reading it.
* The default size of each file page in QuickQueue is 1GB, and the size of each page must be a power of 2 before it can
  be modified by `System.setProperty("QKQPsz",...)`, which is not recommended.
* The maximum length of the queue capacity depends on the index Buffer address or the maximum data Buffer address must
  be less than `(IntMax << pageBitSize) + pos`
  , because the Page is of type Int, it is not recommended to set too small a page, when each page is 1GB, the maximum
  value is 2048PB
* The maximum length of a single message body is Integer.Max
* A single data larger than 1 byte may be in different data page files.
* The default is asynchronous flashing, and the hard disk operation uses memory-mapped files, so even if the application
  crashes, the system will automatically complete the flashing. You can also use QuickQueue.force()
  Forced disk flashing will cause performance loss, and the Pro version of multi-copy consistent writing can solve the
  risk of failure and only slightly affect performance.
