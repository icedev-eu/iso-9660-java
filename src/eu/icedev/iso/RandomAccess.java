package eu.icedev.iso;

import java.io.IOException;
import java.io.InputStream;

public interface RandomAccess {

	int size();
	int get(int index);
	int get(int index, byte[] b, int off, int len);
	
	default int get(int index, byte[] b) {
		return get(index, b, 0, b.length);
	}
	
	default InputStream open(int index, int length) {
		return new RandomAccessInputStream(this, index, Math.min(size()-index, length));
	}

	default InputStream open(int index) {
		return new RandomAccessInputStream(this, index, size()-index);
	}
	
	public static class RandomAccessInputStream extends InputStream {
		protected final RandomAccess access;
		protected final int end;
		protected int index;
		
		public RandomAccessInputStream(RandomAccess r, int index, int length) {
			this.access = r;
			this.index = index;
			this.end = index + length;
		}

		@Override
		public int read() throws IOException {
			if(index >= end)
				return -1;
			return access.get(index++);
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if(index >= end)
				return -1;
			
			if(index + len > end)
				len = end - index;
			
			int read = access.get(index, b, off, len);
			
			if(read > 0)
				index += read;
			
			return read;
		}
		
		@Override
		public long skip(long n) throws IOException {
			if(index >= end) {
				return -1;
			}
			
			if(index + n > end) {
				n = end-index;
			}
			
			index += n;
			return n;
		}
		
		
		@Override
		public int available() throws IOException {
			return end-index;
		}
		
	}
}
