package io.github.lapins2023.quickqueue;

import java.nio.MappedByteBuffer;

class PageBuffer {
    final int page;
    final MappedByteBuffer buffer;
    final long address;

    PageBuffer(int page, MappedByteBuffer buffer) {
        this.page = page;
        this.buffer = buffer;
        address = Utils.getAddress(buffer);
    }


    @Override
    public String toString() {
        return "PageBuffer[(" + page + ")" + buffer + "@" + address + ']';
    }
    
    public void uPosition(int pos) {
        buffer.position(pos);
    }
}
