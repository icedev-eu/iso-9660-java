import java.nio.file.Files;
import java.nio.file.Path;

import eu.icedev.iso.ISO9660;

public class TestIso {

	public static void main(String[] args) throws Exception {
        var isoPath = Path.of("cd.iso");
        var iso = ISO9660.openIso(isoPath);
        
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
