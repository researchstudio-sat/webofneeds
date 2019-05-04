package won.utils.im.port;

import java.io.IOException;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public class AtomDataImporter {
    public static void process(AtomDataReader reader, AtomDataWriter writer) throws IOException {
        while (reader.hasNext()) {
            writer.write(reader.next());
        }
    }

    public static void main(String[] args) throws IOException {
        try (
                        // TODO set reader parameters, e.g source, etc taken from args
                        // can make extra arguments to specify which reader and writer
                        // should be used
                        AtomDataReader reader = new AtomDataReaderImpl();
                        // assumes output directory is the last argument
                        AtomDataWriter writer = new ModelsRdfFolderWriter(args[args.length - 1])) {
            AtomDataImporter.process(reader, writer);
        }
    }
}
