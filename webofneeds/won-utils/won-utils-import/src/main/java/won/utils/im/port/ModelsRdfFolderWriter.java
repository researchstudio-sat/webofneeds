package won.utils.im.port;

import java.io.File;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public class ModelsRdfFolderWriter implements NeedDataWriter<Model> {
    private File outputFolder;
    private ModelRdfFileWriter fileWriter;
    private int counter;

    public ModelsRdfFolderWriter(final String outputFolderPath) throws IOException {
        outputFolder = new File(outputFolderPath);
        counter = 0;
    }

    @Override
    public void write(Model model) throws IOException {
        String fileName = String.format(String.format("%05d.ttl", counter++));
        File outputFile = new File(outputFolder, fileName);
        fileWriter = new ModelRdfFileWriter(outputFile);
        fileWriter.write(model);
        fileWriter.close();
    }

    @Override
    public void close() throws IOException {
        fileWriter.close();
    }
}
