package eu.icedev.iso;

public class SectoredAccess implements RandomAccess {
	final int sector_size;// = 2352;
	final int data_offset;// = 16;
	final int data_length;// = 2048;
	int length;
	
	RandomAccess in;
	
	public SectoredAccess(RandomAccess in, int sectorSize, int dataLength, int dataOffset) {
		sector_size = sectorSize;
		data_length = dataLength;
		data_offset = dataOffset;
		this.in = in;
		int sectors = in.size() / sector_size;
		length = sectors * data_length + (in.size() % sector_size - data_offset);
	}
	

	public int get(int index) {
		int sector = index / data_length;
		int block_start = sector * sector_size + data_offset;
		int offset = (index % data_length);
		return in.get(block_start + offset);
	}
	
	@Override
	public int get(int index, byte[] b, int off, int len) {
		int total = 0;
		
		while(total < len) {
			int sector = index / data_length;
			int block_start = sector * sector_size + data_offset;
			int offset = (index % data_length);
			
			int read = in.get(block_start + offset, b, total+off, Math.min(len-total, data_length-offset));
			if(read <0)
				return 0;
			
			total += read;
			index += read;
			
		}
		
		return total;
	}
	
	@Override
	public int size() {
		return length;
	}

}
