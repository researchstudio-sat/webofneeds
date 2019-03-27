package won.utils.im.port;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public class ModelRdfFileWriter implements NeedDataWriter<Model> {
    private OutputStream out;

    public ModelRdfFileWriter(final File outputFile) throws IOException {
        out = new FileOutputStream(outputFile);
    }

    @Override
    public void write(final Model model) {
        RDFDataMgr.write(out, model, Lang.TURTLE);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
