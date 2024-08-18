package eu.icedev.iso;

import java.nio.ByteBuffer;

public class RandomBuffer implements RandomAccess {
	private ByteBuffer buffer;

	public RandomBuffer(ByteBuffer bb) {
		this.buffer = bb;
	}

	@Override
	public int get(int index) {
		if(index >= size())
			return -1;
		return buffer.get(index) & 0xFF;
	}
	
	@Override
	public int get(int index, byte[] b, int off, int len) {
		if(index >= size())
			return -1;
		
		if(index+len > size()) {
			len = Math.max(0, size()-index);
		}
		
		buffer.get(index, b, off, len);
		return len;
	}

	@Override
	public int size() {
		return buffer.limit();
	}


}
