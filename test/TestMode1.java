import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import eu.icedev.iso.ISO9660;

public class TestMode1 {

	public static void main(String[] args) throws Exception {
		// not reading cue file, assuming it's mode 1 with sector size of 2352
		var iso = ISO9660.openMode1(Path.of("cd.bin"));
        for(var path : iso.getPaths()) {
        	
        	var p = Path.of("ISO/" + path);
        	
        	if(Files.notExists(p.getParent())) {
        		Files.createDirectories(p.getParent());
        	}
        	
        	var stream = iso.openFile(path);
        	
        	System.out.println("Extract file: " + p);
        	Files.copy(stream, p);
        }
	}

}
