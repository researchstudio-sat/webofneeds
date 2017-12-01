package won.utils.goals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphBlending {


    /**
     *
     * @param dataGraph1
     * @param dataGraph2
     * @param blendingUriPrefix must be a unique prefix
     * @return
     */
    public static Model blendSimple(Model dataGraph1, Model dataGraph2, String blendingUriPrefix) {

        Map<String, String> prefixes = new HashMap<>();
        prefixes.putAll(dataGraph1.getNsPrefixMap());
        prefixes.putAll(dataGraph2.getNsPrefixMap());

        Model blendedModel = ModelFactory.createDefaultModel();
        blendedModel.setNsPrefixes(prefixes);

        // add all triples together in one graph
        blendedModel.add(dataGraph1.listStatements());
        blendedModel.add(dataGraph2.listStatements());

        // for every pair of triples check if they have same predicate and object
        // if so, remove one triple pair and rename the subject resources of both to match
        // repeat until we cannot blend nodes anymore
        boolean nodesBlended;
        int numBlended = 0;
        do {
            nodesBlended = false;
            List<Statement> stmts = blendedModel.listStatements().toList();
            for (int i = 0; i < stmts.size(); i++) {
                for (int j = i + 1; j < stmts.size(); j++) {
                    Statement stmt1 = stmts.get(i);
                    Statement stmt2 = stmts.get(j);
                    if (stmt1.getObject().equals(stmt2.getObject()) && stmt1.getPredicate().equals(stmt2.getPredicate())) {
                        blendedModel.remove(stmt2);
                        String blendedResourceUri = blendingUriPrefix + numBlended;
                        ResourceUtils.renameResource(stmt1.getSubject(), blendedResourceUri);
                        ResourceUtils.renameResource(stmt2.getSubject(), blendedResourceUri);
                        numBlended++;
                        nodesBlended = true;

                        // break for loops
                        i = stmts.size();
                        break;
                    }
                }
            }
        } while (nodesBlended);

        return blendedModel;
    }
}
