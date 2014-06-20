package won.cryptography.service;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusReader;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import javax.crypto.Cipher;
import java.io.*;

/**
 * User: fsalcher
 * Date: 12.06.2014
 */
public class CryptographyUtils {

    public static boolean checkForUnlimitedSecurityPolicy() {

        try {
            int size = Cipher.getMaxAllowedKeyLength("RC5");
            System.out.println("max allowed key size: " + size);
            return  size < 256;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static GraphCollection modelToGraphCollection(Model model, String name) throws Exception {

        File temp = File.createTempFile("coin", "trig");
        FileOutputStream os = new FileOutputStream(temp);
        Dataset dataset = DatasetFactory.create(model);
        //Dataset dataset = DatasetFactory.createMem();
        //dataset.addNamedModel(name, model);
        RDFDataMgr.write(os, dataset, RDFFormat.TRIG) ;
        System.out.println(temp.getAbsolutePath());

        GraphCollection gc = TriGPlusReader.readFile(temp.getPath(), true);
        temp.delete();

        return gc;

    }

}
