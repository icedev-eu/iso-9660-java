package eu.icedev.iso;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ISO9660 {
	public static final int SECTOR_SIZE = 2048;
	public static final int MODE1_SECTOR_SIZE = 2352;
	public static final int MODE2_SECTOR_SIZE = 2336;

	private final RandomAccess access;
	
	List<PathRecord> pathTable = new ArrayList<>();
	Map<String, DirectoryRecord> fileMapping = new HashMap<>();
	List<String> filePaths = new ArrayList<String>();

	private int pathTableSize;
	private int locPathL;

	private String systemIdentifier;
	private String volumeIdentifier;
	
	public ISO9660(RandomAccess access) throws IOException {
		this.access = access;
		readVolumeDescriptor();
	}
	
	public NativeDataInputStream openSector(int sector) {
		return new NativeDataInputStream(access.open(SECTOR_SIZE * sector));
	}
	
	public NativeDataInputStream openSector(int sector, int length) {
		return new NativeDataInputStream(access.open(SECTOR_SIZE * sector, length));
	}
	
	public List<String> getPaths() {
		return Collections.unmodifiableList(filePaths);
	}
	
	public InputStream openFile(String path) {
		var entry = fileMapping.get(path);
		if(entry == null)
			return null;
		return access.open(SECTOR_SIZE * entry.dataLocation, entry.dataLength);
	}
	
	private void readVolumeDescriptor() throws IOException {
		var in = openSector(0x10);
		
		int type = in.readInt8();
		String identifier = in.readASCII(5);
		int version = in.readInt8();

		
		readPrimaryVolumeDescriptor(in);
		readPathTable();
		
		for(var path : pathTable) {
			readDirectories(path);
		}
	}

	private void readPrimaryVolumeDescriptor(NativeDataInputStream in) throws IOException {
		in.readInt8(); // unused
		
		systemIdentifier = in.readASCII(32).trim();
		volumeIdentifier = in.readASCII(32).trim();
		
		in.skipNBytes(8); // unused

		
		int spaceSize = in.readInt32();
		in.readInt32(); // big endian
		
		in.skipNBytes(32); // unused
		

		int setSize = in.readInt16();
		int setSizeBE = in.readInt16(); // big endian

		int sequenceNumber = in.readInt16();
		int sequenceNumberBE = in.readInt16();
		
		int logicalBlockSize = in.readInt16();
		int logicalBlockSizeBE = in.readInt16();
		
		pathTableSize = in.readInt32();
		int pathTableSizeBE = in.readInt32();
		
		locPathL = in.readInt32();
		int locOptionalPathL = in.readInt32();
		int locPathM = in.readInt32();
		int locOptionalPathM = in.readInt32();

	}
	
	private void readPathTable() throws IOException {
		var in = openSector(locPathL, pathTableSize);
		
		while(in.available() > 0) {
			PathRecord path = new PathRecord(in);
			pathTable.add(path);
			var index = pathTable.size();

			path.path = path.identifier;
			if(path.parentId < index) {
				var parent = pathTable.get(path.parentId-1);
				path.path = parent.path + "/" + path.identifier;
			}
		}
	}
	
	private void readDirectories(PathRecord path) throws IOException {
		var in = openSector(path.dataLocation);

		int blockCurrent = 0;
		int blockMax = 0;
		
		var first = DirectoryRecord.read(in);

		blockMax = first.dataLength / SECTOR_SIZE - 1;
		
		while(true) {
			var entry = DirectoryRecord.read(in);
			if(entry == null) {
				if(blockCurrent < blockMax) {
					blockCurrent++;
					in = openSector(path.dataLocation + blockCurrent);
					continue;
				}
				break;
			}
			
			String fileName = entry.identifier.split(";")[0];
			String filePath = path.path + "/" + fileName;
			
			if(!entry.isDirectory()) {
				filePaths.add(filePath);
				fileMapping.put(filePath, entry);
			}
		}
	}
	
	
	private static class PathRecord {
		String path;
		final int dataLength;
		final int dataLocation;
		final int parentId;
		final String identifier;
		public PathRecord(NativeDataInputStream in) throws IOException {
			int idlen = in.readInt8();
			dataLength = in.readInt8();
			dataLocation = in.readInt32();
			parentId = in.readInt16();
			identifier = in.readASCII(idlen).trim();
			

			if(idlen % 2 == 1) {
				in.readInt8(); // padding 0
			}
		}
	}
	
	private static class DirectoryRecord {
		final int attributeRecordLength;
		final int dataLocation;
		final int dataLength;
		final long dateTime;
		final int flags;
		final int interleaveFileSize;
		final int interleaveGapSize;
		final int volumeSequenceNumber;
		final String identifier;

		public boolean isNotFinal() {
			return (flags & 0b10000000) != 0;
		}
		
		public boolean isDirectory() {
			return (flags & 0b10) != 0;
		}
		
		
		static DirectoryRecord read(NativeDataInputStream in)  throws IOException {
			int recordLength = in.readInt8();
			
			if(recordLength == 0) {
				return null;
			}
			
			return new DirectoryRecord(in, recordLength);
		}
		
		private DirectoryRecord(NativeDataInputStream in, int length) throws IOException {
			attributeRecordLength = in.readInt8();
			dataLocation = in.readInt32();
			in.readInt32(); // MSB
			dataLength = in.readInt32();
			in.readInt32(); // MSB
			dateTime = in.readLong56();
			flags = in.readInt8();
			interleaveFileSize = in.readInt8();
			interleaveGapSize = in.readInt8();
			volumeSequenceNumber = in.readInt16();
			in.readInt16(); // MSB
			
			int idlen = in.readInt8();
			identifier = in.readASCII(idlen).trim();
			
			
			int read = 33 + idlen;

			if(read % 2 == 1) {
				in.readInt8(); // skip to even
				read++;
			}
			
			if(read<length) {
				in.skip(length-read);
			}
		}
	}

	
	public static ISO9660 openIso(Path isoPath) throws IOException {
        FileChannel channel = (FileChannel) Files.newByteChannel(isoPath, StandardOpenOption.READ);
        var mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN);
        channel.close(); // closing does nothing until mapped bytebuffer gets collected
		return new ISO9660(new RandomBuffer(mapped));
	}
	
	public static ISO9660 openMode1(Path binPath) throws IOException {
        FileChannel channel = (FileChannel) Files.newByteChannel(binPath, StandardOpenOption.READ);
        var mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN);
        channel.close(); // closing does nothing until mapped bytebuffer gets collected
		return new ISO9660(new SectoredAccess(new RandomBuffer(mapped), MODE1_SECTOR_SIZE, SECTOR_SIZE, 16));
	}
	
	public static ISO9660 openMode2(Path binPath) throws IOException {
        FileChannel channel = (FileChannel) Files.newByteChannel(binPath, StandardOpenOption.READ);
        var mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN);
        channel.close(); // closing does nothing until mapped bytebuffer gets collected
		return new ISO9660(new SectoredAccess(new RandomBuffer(mapped), MODE2_SECTOR_SIZE, SECTOR_SIZE, 16));
	}
}
