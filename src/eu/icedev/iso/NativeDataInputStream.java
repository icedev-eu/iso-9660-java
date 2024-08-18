package eu.icedev.iso;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** x86 platform oriented input stream that offers reading integers in Little Endian as well as 0 terminated ASCII strings */ 
public class NativeDataInputStream extends FilterInputStream {
    public NativeDataInputStream(InputStream in) {
        super(in);
    }
    
    byte[] temp = new byte[8];
    
    @Override
	public int read(byte[] b, int off, int len) throws IOException {
    	int total = 0;
    	while(total < len) {
    		int read = in.read(b, off+total, len-total);
    		if(read < 0)
    			throw new EOFException();
    		total += read;
    	}
		return total;
	}

    public int readInt8() throws IOException {
        int r = read();
        if(r == -1)
            throw new EOFException();
        return r & 0xFF;
    }
    
    public short readInt16S() throws IOException {
    	read(temp, 0, 2);
        return (short) ((temp[0] & 0xFF) | ((temp[1] & 0xFF) << 8));
    }

    public int readInt16() throws IOException {
    	read(temp, 0, 2);
        return (temp[0] & 0xFF) | ((temp[1] & 0xFF) << 8);
    }

    public int readInt24() throws IOException {
    	read(temp, 0, 3);
        return (temp[0] & 0xFF) | ((temp[1] & 0xFF) << 8) | ((temp[2] & 0xFF) << 16);
    }

    public int readInt32() throws IOException {
    	read(temp, 0, 4);
        return (temp[0] & 0xFF) | ((temp[1] & 0xFF) << 8) | ((temp[2] & 0xFF) << 16) | ((temp[3] & 0xFF) << 24);
    }
    
    public long readLong56() throws IOException {
    	read(temp, 0, 7);
        return (temp[0] & 0xFFL) | ((temp[1] & 0xFFL) << 8) | ((temp[2] & 0xFFL) << 16) | ((temp[3] & 0xFFL) << 24)
        		 | ((temp[4] & 0xFFL) << 32) | ((temp[5] & 0xFFL) << 40) | ((temp[6] & 0xFFL) << 48);
    }
    
    public long readLong64() throws IOException {
    	read(temp, 0, 8);
        return (temp[0] & 0xFFL) | ((temp[1] & 0xFFL) << 8) | ((temp[2] & 0xFFL) << 16) | ((temp[3] & 0xFFL) << 24)
        		 | ((temp[4] & 0xFFL) << 32) | ((temp[5] & 0xFFL) << 40) | ((temp[6] & 0xFFL) << 48) | ((temp[7] & 0xFFL) << 56);
    }
    
    public CharSequence readASCII() throws IOException {
    	StringBuilder builder = new StringBuilder();
    	while(true) {
    		int byt = read();
    		if(byt < 0)
    			throw new EOFException();
    		if(byt == 0)
    			break;
    		builder.append((char)byt);
    	}
        return builder;
    }

    public String readASCII(int length) throws IOException {
        byte[] bytes = readNBytes(length);

        int strlen = 0;
        for(; strlen<bytes.length; strlen++) {
            if(bytes[strlen] == 0) {
                break;
            }
        }

        return new String(bytes, 0, strlen);
    }
    
    public static short invert(short data) {
        return (short)((data&0xFF)<<8 | (data&0xFF00)>>8);
    }

    public static int invert(int data) {
        return (data&0xFF)<<24 | (data&0xFF00)<<8 | (data&0xFF0000)>>8 | (data&0xFF000000)>>>24;
    }

    public static long invert(long data) {
        return (data&0xFFL)<<56 | (data&0xFF00L)<<40 | (data&0xFF0000L)<<24 | (data&0xFF000000L)<<8
                | (data&0xFF00000000L)>>8 | (data&0xFF0000000000L)>>24 | (data&0xFF000000000000L)>>40 | (data&0xFF00000000000000L)>>>56;
    }

}
