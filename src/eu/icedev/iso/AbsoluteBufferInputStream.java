package eu.icedev.iso;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AbsoluteBufferInputStream extends InputStream {
    ByteBuffer buffer;
    private int index;
    final int limit;

    public AbsoluteBufferInputStream(ByteBuffer buffer, int index) {
        this.buffer = buffer;
        this.index = index;
        this.limit = buffer.limit();
    }
    
    public AbsoluteBufferInputStream(ByteBuffer buffer, int index, int limit) {
        this.buffer = buffer;
        this.index = index;
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        if(limit < index+1) {
            return -1;
        }
        return buffer.get(index++) & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (limit < index+1) {
            return -1;
        }

        if (limit < index+len+1) {
            len = limit - index;
        }

        buffer.get(index, b, off, len);
        index += len;
        return len;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n<= 0 || limit < index+1) {
            return 0;
        }

        if (limit < index+n+1) {
            n = (long) limit - index;
        }

        index += n;
        return n;
    }

    @Override
    public int available() throws IOException {
        return Math.max(0, limit - index);
    }
}

