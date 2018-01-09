package won.utils.goals;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator class that iterates over all possible models that could be a valid result from blending two input models.
 * Blending of two subject resource URIs is defined to be valid if there exists at least one statement in each
 * input model with the same objects and predicates for these subjects.
 * Resource URIs of the input models are only considered for blending if they are part of a "variable uri name space"
 * which is passed in the constructor of this class together with the two input models for blending.
 */
public class GraphBlendingIterator implements Iterator<Model> {

    private static int MAX_RESOURCE_PAIR_SIZE = 16;

    private Model dataGraph1;
    private Model dataGraph2;
    private String variableUriPrefix;
    private String blendingUriPrefix;
    private ArrayList<Pair<String, String>> blendingResourceUriPairs;
    private int powerSetIndex = 0;
    private int blendingIndex = 0;

    /**
     * Initialize the blending iterator
     *
     * @param dataModel1 first input model for blending
     * @param dataModel2 second input model for blending
     * @param variableUriPrefix uri prefix defines which resource URIs are considered for blending
     * @param blendingUriPrefix uri prefix that is used to generate the result URIs of blended resources
     */
    public GraphBlendingIterator(Model dataModel1, Model dataModel2, String variableUriPrefix, String blendingUriPrefix) {

        this.dataGraph1 = ModelFactory.createDefaultModel();
        this.dataGraph1.add(dataModel1);
        this.dataGraph2 = ModelFactory.createDefaultModel();
        this.dataGraph2.add(dataModel2);
        this.variableUriPrefix = variableUriPrefix;
        this.blendingUriPrefix = blendingUriPrefix;

        // find all combinations of unique pairs of variable resources between the two models
        // for whose statements blending is valid
        blendingResourceUriPairs = new ArrayList<>();
        for (Statement stmt1 : dataModel1.listStatements().toList()) {
            if (isVariableResource(stmt1.getSubject().getURI())) {
                for (Statement stmt2 : dataModel2.listStatements().toList()) {
                    if (isVariableResource(stmt2.getSubject().getURI()) && isValidBlending(stmt1, stmt2)) {
                        Pair blendingPair = Pair.of(stmt1.getSubject().getURI(), stmt2.getSubject().getURI());
                        if (!blendingResourceUriPairs.contains(blendingPair)) {
                            blendingResourceUriPairs.add(blendingPair);
                        }
                    }
                }
            }
        }

        if (blendingResourceUriPairs.size() > MAX_RESOURCE_PAIR_SIZE) {
            throw new IllegalArgumentException("too many blending possibilities for these input models");
        }
    }

    private boolean isVariableResource(String uri) {
        return uri.startsWith(variableUriPrefix);
    }

    private boolean isValidBlending(Statement stmt1, Statement stmt2) {
        return (stmt1.getObject().equals(stmt2.getObject()) && stmt1.getPredicate().equals(stmt2.getPredicate()));
    }

    protected int getPowerSetSize() {
        return (1 << blendingResourceUriPairs.size());
    }

    @Override
    public boolean hasNext() {
        return (powerSetIndex < getPowerSetSize());
    }

    @Override
    public Model next() {

        if (!hasNext()) {
            throw new NoSuchElementException("No more elements available");
        }

        // add all triples together in one graph
        Model blendedModel = ModelFactory.createDefaultModel();
        blendedModel.add(dataGraph1.listStatements());
        blendedModel.add(dataGraph2.listStatements());

        for (int i = 0; i < blendingResourceUriPairs.size(); i++) {

            // check which bits are set in the current powerSetIndex and blend the corresponding Resource pairs
            if ((powerSetIndex & (1 << i)) > 0) {

                // find a name that is not used in the blended model yet
                String blendedResourceUri;
                do {
                    blendingIndex++;
                    blendedResourceUri = blendingUriPrefix + blendingIndex;
                } while (blendedModel.getResource(blendedResourceUri) != null);

                // blend the resources by renaming them
                Pair<String, String> blendingPair = blendingResourceUriPairs.get(i);
                ResourceUtils.renameResource(blendedModel.getResource(blendingPair.getLeft()), blendedResourceUri);
                ResourceUtils.renameResource(blendedModel.getResource(blendingPair.getRight()), blendedResourceUri);
            }
        }

        return blendedModel;
    }

}
