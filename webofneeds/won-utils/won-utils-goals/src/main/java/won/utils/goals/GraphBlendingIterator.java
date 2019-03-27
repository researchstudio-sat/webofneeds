package won.utils.goals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.ResourceUtils;

/**
 * Iterator class that iterates over all possible models that could be a valid
 * result from blending two input models. Blending of two subject resource URIs
 * is defined to be valid if there exists at least one statement in each input
 * model with the same objects and predicates for these subjects. Resource URIs
 * of the input models are only considered for blending if they are part of a
 * "variable uri name space" which is passed in the constructor of this class
 * together with the two input models for blending.
 */
public class GraphBlendingIterator implements Iterator<Model> {
    private static int MAX_RESOURCE_PAIR_SIZE = 10;
    private Model dataGraph1;
    private Model dataGraph2;
    private String variableUriPrefix;
    private String blendingUriPrefix;
    private ArrayList<Pair<String, String>> blendingResourceUriPairs;
    private int powerSetIndex = 0;

    /**
     * Initialize the blending iterator
     *
     * @param dataModel1 first input model for blending
     * @param dataModel2 second input model for blending
     * @param variableUriPrefix uri prefix defines which resource URIs are
     * considered for blending
     * @param blendingUriPrefix uri prefix that is used to generate the result URIs
     * of blended resources
     */
    public GraphBlendingIterator(Model dataModel1, Model dataModel2, String variableUriPrefix,
                    String blendingUriPrefix) {
        this.dataGraph1 = ModelFactory.createDefaultModel();
        this.dataGraph1.add(dataModel1);
        this.dataGraph2 = ModelFactory.createDefaultModel();
        this.dataGraph2.add(dataModel2);
        this.variableUriPrefix = variableUriPrefix;
        this.blendingUriPrefix = blendingUriPrefix;
        // find all combinations of unique pairs of variable resources between the two
        // models
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
        return (uri != null && uri.startsWith(variableUriPrefix));
    }

    private boolean isValidBlending(Statement stmt1, Statement stmt2) {
        return (stmt1.getObject().equals(stmt2.getObject()) && stmt1.getPredicate().equals(stmt2.getPredicate()));
    }

    protected int getPowerSetSize() {
        return (1 << blendingResourceUriPairs.size());
    }

    /**
     * Return the next valid power set index of a set of resource URI pairs. Not all
     * power set indices are valid since resource URIs may only occur at most once
     * in pair sets.
     *
     * @return next valid power set index or an index >= power set size if no next
     * valid index exists anymore
     */
    private int getNextValidPowerSetIndex() {
        // check if the combination of pairs is allowed
        int validPowerSetIndex = powerSetIndex - 1;
        boolean isValidPowerSet;
        do {
            validPowerSetIndex++;
            // resource URI may only occur at most once in pair sets
            // e.g. {(A,X),(B,X)} is not allowed since X occurs twice, so a lot of
            // combinations are filtered out here
            // if the same resource URI occurs in both graphs it should also only occur once
            // in a valid set, either
            // as left or right side of a pair. Therefore we can test for valid combinations
            // using a set or list structure
            // by inserting both left and right side pair entries and check for duplicate
            // keys
            isValidPowerSet = true;
            Set<String> resourceSet = new HashSet<>();
            for (int i = 0; i < blendingResourceUriPairs.size(); i++) {
                // check which bits are set in the current setIndex
                if ((validPowerSetIndex & (1 << i)) > 0) {
                    Pair<String, String> currentPair = blendingResourceUriPairs.get(i);
                    if (resourceSet.contains(currentPair.getLeft()) || resourceSet.contains(currentPair.getRight())
                                    || currentPair.getLeft().equals(currentPair.getRight())) {
                        isValidPowerSet = false;
                        break;
                    } else {
                        resourceSet.add(currentPair.getLeft());
                        resourceSet.add(currentPair.getRight());
                    }
                }
            }
        } while (validPowerSetIndex < getPowerSetSize() && !isValidPowerSet);
        // if we found the next valid power set index return it
        if (isValidPowerSet) {
            return validPowerSetIndex;
        }
        // this shows that no next valid power set index exists anymore
        return getPowerSetSize();
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
        blendedModel.setNsPrefixes(dataGraph1.getNsPrefixMap());
        blendedModel.setNsPrefixes(dataGraph2.getNsPrefixMap());
        blendedModel.add(dataGraph1.listStatements());
        blendedModel.add(dataGraph2.listStatements());
        int blendingIndex = 0;
        for (int i = 0; i < blendingResourceUriPairs.size(); i++) {
            // check which bits are set in the current powerSetIndex and blend the
            // corresponding Resource pairs
            if ((powerSetIndex & (1 << i)) > 0) {
                // find a name that is not used in the blended model yet
                String blendedResourceUri;
                do {
                    blendingIndex++;
                    blendedResourceUri = blendingUriPrefix + blendingIndex;
                } while (blendedModel.containsResource(new ResourceImpl(blendedResourceUri)));
                // blend the resources by renaming them
                Pair<String, String> blendingPair = blendingResourceUriPairs.get(i);
                ResourceUtils.renameResource(blendedModel.getResource(blendingPair.getLeft()), blendedResourceUri);
                ResourceUtils.renameResource(blendedModel.getResource(blendingPair.getRight()), blendedResourceUri);
            }
        }
        // increase the power set index to the next valid index and return the blended
        // model
        powerSetIndex++;
        powerSetIndex = getNextValidPowerSetIndex();
        return blendedModel;
    }
}
