package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import org.springframework.beans.factory.annotation.Value;
import won.protocol.model.Need;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class RDFFileStorageService implements RDFStorageService {

    @Value ("${rdf.file.path}")
    private String path;

    @Override
    public void storeContent(Need need, Model graph) {
        FileOutputStream out = null;

        if(path.equals(""))
            path = System.getProperty("java.io.tmpdir");

        try {
            out = new FileOutputStream(new File(path, getFileName(need)));
            graph.write(out, "TTL");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            if(out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
        }
    }

    @Override
    public Model loadContent(Need need) {
        InputStream in = null;

        if(path.equals(""))
            path = System.getProperty("java.io.tmpdir");

        try {
            Model m = ModelFactory.createDefaultModel();
            // use the FileManager to find the input file
            in = FileManager.get().open(path + "/" + getFileName(need));
            if (in == null) {
                throw new IllegalArgumentException(
                        "File: offer.ttl not found");
            }
            m.read(in, null, "TTL");
        } finally {
            if(in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private String getFileName(Need n) {
        return n.getId() + ".ttl";
    }
}
