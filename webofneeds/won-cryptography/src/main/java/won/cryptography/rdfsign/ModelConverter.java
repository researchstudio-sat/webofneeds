package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.FileUtils;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Prefix;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.Triple;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class ModelConverter {


    // Converts Signingframework's NamedGraph into Jena's model representation.
    // Here, the simplest (but probably not the most efficient) approach is
    // applied:  Signingframework's reader and Jena's writer are used to
    // transform data. Works just fine. TODO prefixes generalization
    public static Model namedGraphToModel(NamedGraph graph, List<Prefix> prefixes) throws Exception {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        String dfPref = "";
        for (Prefix p : prefixes) {
            writer.write(p.toString());
            //TODO chng prefix handling
            if (p.getPrefix().equals(":")) {
                dfPref = p.getIriContent();
            }
        }
        TriGPlusWriter.writeGraph(writer, graph, 3);
        writer.close();
        String content = os.toString();

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());

        //InputStream is1 = WonSignerVerifierTest.class.getResourceAsStream(RESOURCE_FILE);
        //model1.read(new InputStreamReader(is1), RESOURCE_URI, FileUtils.langTurtle);

        Model model = dataset.getNamedModel(dfPref + graph.getName().replace(":",""));
        for (Prefix pref : prefixes) {
            model.setNsPrefix(pref.getPrefix().replace(":", ""), pref.getIriContent());
        }
        // or default graph of dataset also has prefix map

        return model;
    }

    // converts Jena's model into representation of Signingframework's GraphCollection
    // it might need to be improved as some syntax cases might not be covered here
    // the simplest approach with just using Jena's writer and Signingframework's
    // reader to transform data won't work since Signingframework has problems
    // with recognizing the semantics of []
    public static GraphCollection modelToGraphCollection(String name, Model model, PrefixMapping pm) {
        GraphCollection graphc = new GraphCollection();
        NamedGraph namedGraph = new NamedGraph(enclose(name, "<", ">"), 0, null);
        StmtIterator iterator = model.listStatements();
        while (iterator.hasNext()) {
            Statement stmt = iterator.nextStatement();
            Resource subj = stmt.getSubject();
            Property pred = stmt.getPredicate();
            RDFNode obj =  stmt.getObject();
            Triple gcTriple = null;
            String subjString = null;
            String objString = null;

            if (subj.isURIResource()) {
                subjString = enclose( subj.getURI(), "<", ">");
            } else if (subj.isLiteral()) {
                subjString = enclose(subj.asLiteral().getLexicalForm(), "\"", "\"");
                if (subj.asLiteral().getLanguage() != null) {
                    subjString = enclose(subjString, "", "@" + subj.asLiteral().getLanguage());
                }
            } else if (subj.isAnon()) {
                subjString = enclose(subj.getId().getLabelString(), "_:", "");
            } else {
                System.out.println("TODO 2");
            }

            if (obj.isLiteral()) {
                objString = enclose(obj.asLiteral().getLexicalForm(), "\"", "\"");
                if (obj.asLiteral().getLanguage() != null) {
                    objString = enclose(objString, "", "@" + obj.asLiteral().getLanguage());
                }
            } else if (obj.isAnon()) {
                objString = enclose(obj.asResource().getId().getLabelString(), "_:", "");
            } else if (obj.isURIResource()) {
                objString = enclose(obj.asResource().getURI(), "<", ">");
            } else {
                System.out.println("TODO 3");
            }
            gcTriple = new Triple(subjString, enclose(pred.asResource().getURI(), "<", ">"), objString);
            namedGraph.addTriple(gcTriple);
        }
        graphc.addGraph(namedGraph);

        for (String prefix : pm.getNsPrefixMap().keySet()) {
            graphc.addPrefix(new Prefix(prefix+":","<"+pm.getNsPrefixURI(prefix)+">"));
        }
        graphc.applyPrefixes();

        return graphc;
    }

    private static String enclose(String string, String start, String end) {
        return start + string + end;
    }
}
