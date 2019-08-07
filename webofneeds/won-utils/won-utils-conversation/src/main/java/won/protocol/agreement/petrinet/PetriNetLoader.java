package won.protocol.agreement.petrinet;

import uk.ac.imperial.pipe.io.PetriNetIO;
import uk.ac.imperial.pipe.io.PetriNetIOImpl;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;

import java.io.File;
import java.io.FileWriter;
import java.util.Base64;
import java.util.Optional;

public class PetriNetLoader {
    private Optional<PetriNetIOImpl> petriNetIO = Optional.empty();

    private synchronized PetriNetIO getPetriNetIO() {
        if (petriNetIO.isPresent())
            return petriNetIO.get();
        try {
            petriNetIO = Optional.of(new PetriNetIOImpl());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize PetriNetIO", e);
        }
        return petriNetIO.get();
    }

    public PetriNet readBase64EncodedPNML(String base64EncodedPNML) {
        byte[] pnmlBytes = Base64.getDecoder().decode(base64EncodedPNML);
        String pnmlString = new String(pnmlBytes);
        return readPNML(pnmlString);
    }

    public PetriNet readPNML(String pnml) {
        PetriNetIO petriNetIO = getPetriNetIO();
        Optional<File> tempfile = Optional.empty();
        try {
            tempfile = Optional.of(File.createTempFile("petrinet-tmp-", ".xml"));
            try (FileWriter fw = new FileWriter(tempfile.get())) {
                ;
                fw.write(pnml);
            }
            return petriNetIO.read(tempfile.get().getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalStateException("Error reading petrinet from base64 String", e);
        } finally {
            if (tempfile.isPresent()) {
                tempfile.get().delete();
            }
        }
    }
}
