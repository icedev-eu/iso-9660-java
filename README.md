# ISO 9660

This is ISO 9660 extractor. Supports reading ISO from a RAW CD MODE 1 image (like those in cue+bin)

# Why

For the challenge, I like to work with these low level formats. There is history in there.

Also I needed a runtime extractor for iso/bin images for currently available XCOM3 releases on Steam and GOG.

# How to use

Example extractor:

    var iso = ISO9660.openIso(isoPath);
    for(var path : iso.getPaths()) {
        var p = Path.of("ISO/" + path);
        if(Files.notExists(p.getParent())) {
            Files.createDirectories(p.getParent());
        }
        
        var stream = iso.openFile(path);
        Files.copy(stream, p);
    }

`stream` does not need to be closed, it's just a wrapper over a bytebuffer with an imposed limit (and elimination of gaps between sectors). Internally the extractor uses memory mapped byte buffer to read contents of the bin/iso and it is impossible to close that resource manually. One needs to "forget" about the archive and wait for the GC cycle to do it's job.

## other resources

Useful resources that helped me with implementation:

https://wiki.osdev.org/ISO_9660
https://github.com/libyal/libodraw/blob/main/documentation/Optical%20disc%20RAW%20format.asciidoc