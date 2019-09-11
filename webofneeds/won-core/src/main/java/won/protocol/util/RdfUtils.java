package won.protocol.util;

import com.google.common.collect.Iterators;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.eval.PathEval;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.tdb.TDB;
import org.apache.jena.util.FileUtils;
import org.apache.jena.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IncorrectPropertyCountException;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utilities for RDF manipulation with Jena.
 */
public class RdfUtils {
    public static final RDFNode EMPTY_RDF_NODE = null;
    private static final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static String toString(Model model) {
        String ret = "";
        if (model != null) {
            StringWriter sw = new StringWriter();
            model.write(sw, "TTL");
            ret = sw.toString();
        }
        return ret;
    }

    public static Model toModel(String content) {
        return readRdfSnippet(content, FileUtils.langTurtle);
    }

    /**
     * Converts a Jena Dataset into a TriG string
     *
     * @param dataset Dataset containing RDF which will be converted
     * @return <code>String</code> containing TriG serialized RDF from the dataset
     */
    public static String toString(Dataset dataset) {
        String result = "";
        if (dataset != null) {
            StringWriter sw = new StringWriter();
            RDFDataMgr.write(sw, dataset, RDFFormat.TRIG.getLang());
            result = sw.toString();
        }
        return result;
    }

    /**
     * Converts a <code>String</code> containing TriG formatted RDF into a Jena
     * Dataset
     *
     * @param content String with the TriG formatted RDF
     * @return Jena Dataset containing the RDF from content
     */
    public static Dataset toDataset(String content) {
        return toDataset(content, RDFFormat.TRIG);
    }

    public static Dataset toDataset(String content, RDFFormat rdfFormat) {
        if (content != null) {
            return toDataset(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), rdfFormat);
        } else
            return DatasetFactory.createGeneral();
    }

    public static Dataset toDataset(InputStream stream, RDFFormat rdfFormat) {
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, stream, rdfFormat.getLang());
        try {
            stream.close();
        } catch (IOException ex) {
            logger.warn("An exception occurred.", ex);
        }
        return dataset;
    }

    /**
     * Clones the specified model (its statements and ns prefixes) and returns the
     * clone.
     * 
     * @param original
     * @return
     */
    public static Model cloneModel(Model original) {
        Model clonedModel = ModelFactory.createDefaultModel();
        original.enterCriticalSection(Lock.READ);
        try {
            StmtIterator it = original.listStatements();
            while (it.hasNext()) {
                clonedModel.add(it.nextStatement());
            }
            clonedModel.setNsPrefixes(original.getNsPrefixMap());
        } finally {
            original.leaveCriticalSection();
        }
        return clonedModel;
    }

    public static Dataset cloneDataset(Dataset dataset) {
        if (dataset == null)
            return null;
        boolean existingTransaction = dataset.isInTransaction();
        if (!existingTransaction) {
            dataset.begin(ReadWrite.READ);
        }
        Dataset clonedDataset = DatasetFactory.createGeneral();
        clonedDataset.begin(ReadWrite.WRITE);
        Model model = dataset.getDefaultModel();
        if (model != null) {
            clonedDataset.setDefaultModel(cloneModel(model));
        }
        for (Iterator<String> modelNames = dataset.listNames(); modelNames.hasNext();) {
            String modelName = modelNames.next();
            clonedDataset.addNamedModel(modelName, cloneModel(dataset.getNamedModel(modelName)));
        }
        clonedDataset.commit();
        if (!existingTransaction) {
            dataset.end();
        }
        return clonedDataset;
    }

    /**
     * Applies Model.isIsomorphicWith to all models in both datasets that have the
     * same name, and checks that there are no more models in one than in the other.
     * 
     * @param dataset
     * @param otherDataset
     * @return
     */
    public static boolean isIsomorphicWith(Dataset dataset, Dataset otherDataset) {
        if (dataset == null)
            throw new IllegalArgumentException("dataset must not be null");
        if (otherDataset == null)
            throw new IllegalArgumentException("otherDataset must not be null");
        // first, check the default graph:
        Model defaultModel = dataset.getDefaultModel();
        Model otherDefaultModel = otherDataset.getDefaultModel();
        if (!areModelsIsomorphic(defaultModel, otherDefaultModel)) {
            return false;
        }
        // now, check all named models
        Iterator<String> namesIt = dataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            Model model = dataset.getNamedModel(name);
            if (!otherDataset.containsNamedModel(name)) {
                // treat empty named graphs same as non-existent ones: check if the named graph
                // in the otherDataset is empty
                if (dataset.getNamedModel(name).isEmpty()) {
                    return true;
                }
            }
            Model otherModel = otherDataset.getNamedModel(name);
            if (!areModelsIsomorphic(model, otherModel)) {
                return false;
            }
        }
        // check if the other dataset contains named models not contained in the dataset
        namesIt = otherDataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            if (!dataset.containsNamedModel(name)) {
                // treat empty named graphs same as non-existent ones: check if the named graph
                // in the otherDataset is empty
                return otherDataset.getNamedModel(name).isEmpty();
            }
        }
        return true;
    }

    public static boolean areModelsIsomorphic(Model model, Model otherModel) {
        if (model != null) {
            if (otherModel == null) {
                return false;
            } else {
                return model.isIsomorphicWith(otherModel);
            }
        } else {
            if (otherModel != null) {
                return false;
            }
        }
        throw new IllegalArgumentException("both models are null");
    }

    public static Pair<Model> diff(Model firstModel, Model secondModel) {
        if (firstModel == null && secondModel == null)
            return null;
        Model cloneFirst = null;
        Model cloneSecond = null;
        if (firstModel != null) {
            cloneFirst = cloneModel(firstModel);
        }
        if (secondModel != null) {
            cloneSecond = cloneModel(secondModel);
        }
        if (firstModel == null || secondModel == null) {
            return new Pair<>(cloneFirst, cloneSecond);
        }
        // both models are non-null:
        // remove all statements from clones that exist in the other one, too (excluding
        // blank nodes)
        StmtIterator it = firstModel.listStatements();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            if (stmt.getSubject().isAnon() || stmt.getObject().isAnon()) {
                Resource s = stmt.getSubject();
                Property p = stmt.getPredicate();
                RDFNode o = stmt.getObject();
                if (s.isAnon())
                    s = null;
                if (o.isAnon())
                    o = null;
                StmtIterator it2 = cloneSecond.listStatements(s, p, o);
                while (it2.hasNext()) {
                    Statement toRemove = it2.nextStatement();
                    if (stmt.getSubject().isAnon() && !toRemove.getSubject().isAnon())
                        continue;
                    if (stmt.getObject().isAnon() && !toRemove.getObject().isAnon())
                        continue;
                    it2.remove();
                }
            } else {
                cloneSecond.remove(stmt);
            }
        }
        it = secondModel.listStatements();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            if (stmt.getSubject().isAnon() || stmt.getObject().isAnon()) {
                Resource s = stmt.getSubject();
                Property p = stmt.getPredicate();
                RDFNode o = stmt.getObject();
                if (s.isAnon())
                    s = null;
                if (o.isAnon())
                    o = null;
                StmtIterator it2 = cloneFirst.listStatements(s, p, o);
                while (it2.hasNext()) {
                    Statement toRemove = it2.nextStatement();
                    if (stmt.getSubject().isAnon() && !toRemove.getSubject().isAnon())
                        continue;
                    if (stmt.getObject().isAnon() && !toRemove.getObject().isAnon())
                        continue;
                    it2.remove();
                }
            } else {
                cloneFirst.remove(stmt);
            }
        }
        return new Pair<>(cloneFirst, cloneSecond);
    }

    public static Pair<Dataset> diff(Dataset firstDataset, Dataset secondDataset) {
        Dataset firstResultDataset = DatasetFactory.createGeneral();
        Dataset secondResultDataset = DatasetFactory.createGeneral();
        // first, diff the default models:
        Pair<Model> modelDiff = diff(firstDataset.getDefaultModel(), secondDataset.getDefaultModel());
        firstResultDataset.setDefaultModel(modelDiff.getFirst());
        secondResultDataset.setDefaultModel(modelDiff.getSecond());
        // now find models of same name and diff them
        Iterator<String> namesIt = firstDataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            if (secondDataset.containsNamedModel(name)) {
                modelDiff = diff(firstDataset.getNamedModel(name), secondDataset.getNamedModel(name));
                if (modelDiff.getFirst() != null && !modelDiff.getFirst().isEmpty()) {
                    firstResultDataset.addNamedModel(name, modelDiff.getFirst());
                }
                if (modelDiff.getSecond() != null && !modelDiff.getSecond().isEmpty()) {
                    secondResultDataset.addNamedModel(name, modelDiff.getSecond());
                }
            } else {
                firstResultDataset.addNamedModel(name, cloneModel(firstDataset.getNamedModel(name)));
            }
        }
        namesIt = secondDataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            if (!firstDataset.containsNamedModel(name)) {
                secondResultDataset.addNamedModel(name, cloneModel(secondDataset.getNamedModel(name)));
            }
        }
        return new Pair<>(firstResultDataset, secondResultDataset);
    }

    public static class Pair<E> {
        private E first;
        private E second;

        public Pair(E first, E second) {
            this.first = first;
            this.second = second;
        }

        public E getFirst() {
            return first;
        }

        public E getSecond() {
            return second;
        }
    }

    /**
     * Set's the model's base URI. (i.e. the URI prefix for all relative or null
     * relative URIs). Does nothing else.
     * 
     * @param model
     */
    public static void setBaseURI(final Model model, final String baseURI) {
        model.getNsPrefixMap().put("", baseURI);
    }

    /**
     * Sets the model's base URI (i.e. the URI prefix for all relative or null
     * relative URIs). If the base URI was set previously, replace it in all
     * statements with the new base URI. Note that replacement operation does not do
     * substring replacement, so any resources that were created as relative to the
     * base uri may or may not be altered, depending on the model implementation.
     * 
     * @param model
     * @param baseURI
     */
    public static void replaceBaseURI(final Model model, final String baseURI) {
        replaceBaseURI(model, baseURI, false);
    }

    public static void replaceBaseURI(final Model model, final String baseURI, boolean renamePrefixedURIs) {
        // we assume that the RDF content is self-referential, i.e., it 'talks about
        // itself': the graph is connected to
        // the public resource URI which, when de-referenced, returns that graph. So,
        // triples referring to the 'null relative URI'
        // (see http://www.w3.org/2012/ldp/track/issues/20 ) will be changed to refer to
        // the newly created atom URI instead.
        // this implies that the default URI prefix of the document (if set) will have
        // to be changed to the atom URI.
        // check if there is a default URI prefix.
        // - If not, we just change the default prefix and that should automatically
        // alter all
        // null relative uris to refer to the newly set prefix.
        // - If there is one, fetch it as a resource and 'rename' it (i.e., replace all
        // statements with exchanged name)
        Resource oldBase = model.getResource(model.getNsPrefixURI(""));
        if (oldBase != null) {
            if (renamePrefixedURIs) {
                renameResourceWithPrefix(model, oldBase.toString(), baseURI);
            }
            ResourceUtils.renameResource(oldBase, baseURI);
        }
        // whatever the base uri (default URI prefix) was, set it to the atom URI.
        model.setNsPrefix("", baseURI);
    }

    /**
     * Rename resources in all models of the dataset, replacing the prefix string by
     * the replacement string.
     * 
     * @param dataset
     * @param prefix
     * @param replacement
     */
    public static void renameResourceWithPrefix(Dataset dataset, String prefix, String replacement) {
        visit(dataset, (ModelVisitor<Void>) model -> {
            renameResourceWithPrefix(model, prefix, replacement);
            return null;
        });
        Iterator<String> modelNames = dataset.listNames();
        Map<String, String> toReplace = new HashMap<>();
        while (modelNames.hasNext()) {
            String modelName = modelNames.next();
            if (modelName.startsWith(prefix)) {
                String newModelName = replacement + modelName.substring(prefix.length());
                toReplace.put(modelName, newModelName);
            }
        }
        toReplace.entrySet().stream().forEach(e -> {
            Model model = dataset.getNamedModel(e.getKey());
            dataset.removeNamedModel(e.getKey());
            dataset.addNamedModel(e.getValue(), model);
        });
    }

    /**
     * Renames all URI resources in the model that start with the specified prefix
     * by replacing it with replacement.
     */
    public static void renameResourceWithPrefix(Model model, String prefix, String replacement) {
        /*
         * listURIResources(model) .filter(r -> { System.out.println("Filter, prefix: "
         * + prefix + "replacement: " + replacement); return
         * r.getURI().startsWith(prefix); }) .map(r -> ResourceUtils .renameResource(r,
         * r.getURI().replaceFirst(prefix, replacement)));
         */
        Set<Resource> uriResources = listUriResources(model);
        for (Resource r : uriResources) {
            if (r.getURI().startsWith(prefix)) {
                ResourceUtils.renameResource(r, replacement + r.getURI().substring(prefix.length()));
            }
        }
    }

    /*
     * public static Stream<Resource> listURIResources(Model model) { Set<Resource>
     * uriResources = new HashSet(); StmtIterator it = model.listStatements();
     * while(it.hasNext()) { Statement s = it.next(); Resource subject =
     * s.getSubject(); RDFNode object = s.getObject(); if (subject.isURIResource())
     * { uriResources.add(subject); } if (object.isURIResource()) {
     * uriResources.add(object.asResource()); } } return uriResources.stream(); }
     */
    public static Set<Resource> listUriResources(Model model) {
        Set<Resource> uriResources = new HashSet<>();
        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement s = it.next();
            Resource subject = s.getSubject();
            RDFNode object = s.getObject();
            if (subject.isURIResource()) {
                uriResources.add(subject);
            }
            if (object.isURIResource()) {
                uriResources.add(object.asResource());
            }
        }
        return uriResources;
    }

    public static void replaceBaseURI(final Dataset dataset, final String baseURI) {
        replaceBaseURI(dataset, baseURI, false);
    }

    public static void replaceBaseURI(final Dataset dataset, final String baseURI, boolean renamePrefixedURIs) {
        visit(dataset, model -> {
            replaceBaseURI(model, baseURI, renamePrefixedURIs);
            return null;
        });
    }

    /**
     * Replaces the base URI that's set as the model's default URI prfefix in all
     * statements by replacement and changes the base URI to replacement. Does not
     * do anything if no base URI is set.
     *
     * @param model
     * @param replacement
     */
    public static void replaceBaseResource(final Model model, final Resource replacement) {
        replaceBaseResource(model, replacement, false);
    }

    public static void replaceBaseResource(final Model model, final Resource replacement, boolean renamePrefixedURIs) {
        String baseURI = model.getNsPrefixURI("");
        if (baseURI == null)
            return;
        Resource baseUriResource = model.getResource(baseURI);
        if (renamePrefixedURIs) {
            renameResourceWithPrefix(model, baseUriResource.getURI(), replacement.getURI());
        } else {
            replaceResourceInModel(baseUriResource, replacement);
        }
        model.setNsPrefix("", replacement.getURI());
    }

    public static void replaceBaseResource(Dataset dataset, final Resource replacement) {
        replaceBaseResource(dataset, replacement, false);
    }

    public static void replaceBaseResource(Dataset dataset, final Resource replacement, boolean renamePrefixedURIs) {
        visit(dataset, model -> {
            replaceBaseResource(model, replacement, renamePrefixedURIs);
            return null;
        });
    }

    /**
     * Modifies the specified resources' model, replacing resource with replacement.
     * 
     * @param resource
     * @param replacement
     */
    public static void replaceResourceInModel(final Resource resource, final Resource replacement) {
        logger.debug("replacing resource '{}' with resource '{}'", resource, replacement);
        if (!resource.getModel().equals(replacement.getModel()))
            throw new IllegalArgumentException("resource and replacement must be from the same model");
        Model model = resource.getModel();
        Model modelForNewStatements = ModelFactory.createDefaultModel();
        StmtIterator iterator = model.listStatements(resource, null, (RDFNode) null);
        while (iterator.hasNext()) {
            Statement origStmt = iterator.next();
            Statement newStmt = new StatementImpl(replacement, origStmt.getPredicate(), origStmt.getObject());
            iterator.remove();
            modelForNewStatements.add(newStmt);
        }
        iterator = model.listStatements(null, null, resource);
        while (iterator.hasNext()) {
            Statement origStmt = iterator.next();
            Statement newStmt = new StatementImpl(origStmt.getSubject(), origStmt.getPredicate(), replacement);
            iterator.remove();
            modelForNewStatements.add(newStmt);
        }
        model.add(modelForNewStatements);
    }

    public static void removeResource(Model model, Resource resource) {
        // remove statements where resource is subject
        model.removeAll(resource, null, null);
        // remove statements where resource is object
        model.removeAll(null, null, resource);
    }

    /**
     * Creates a new model that contains both specified models' content. The base
     * resource is that of model1, all triples in model2 that are attached to the
     * its base resource are modified so as to be attached to the base resource of
     * the result.
     * 
     * @param model1
     * @param model2
     * @return
     */
    public static Model mergeModelsCombiningBaseResource(final Model model1, final Model model2) {
        if (logger.isDebugEnabled()) {
            logger.debug("model1:\n{}", writeModelToString(model1, Lang.TTL));
            logger.debug("model2:\n{}", writeModelToString(model2, Lang.TTL));
        }
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(mergeNsPrefixes(model1.getNsPrefixMap(), model2.getNsPrefixMap()));
        result.add(model1);
        result.add(model2);
        if (logger.isDebugEnabled()) {
            logger.debug("result (before merging base resources):\n{}", writeModelToString(result, Lang.TTL));
        }
        Resource baseResource1 = getBaseResource(model1);
        Resource baseResource2 = getBaseResource(model2);
        replaceResourceInModel(result.getResource(baseResource1.getURI()), result.getResource(baseResource2.getURI()));
        String prefix = model1.getNsPrefixURI("");
        if (prefix != null) {
            result.setNsPrefix("", prefix);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("result (after merging base resources):\n{}", writeModelToString(result, Lang.TTL));
        }
        return result;
    }

    /**
     * Finds the resource representing the model's base resource, i.e. the resource
     * with the model's base URI. If no such URI is specified, a dummy base URI is
     * set and a resource is returned referencing that URI.
     *
     * @param model
     * @return
     */
    public static Resource findOrCreateBaseResource(Model model) {
        String baseURI = model.getNsPrefixURI("");
        if (baseURI == null) {
            model.setNsPrefix("", "no:uri");
            baseURI = model.getNsPrefixURI("");
        }
        return model.getResource(baseURI);
    }

    /**
     * Returns the resource representing the model's base resource, i.e. the
     * resource with the model's base URI.
     * 
     * @param model
     * @return
     */
    public static Resource getBaseResource(Model model) {
        String baseURI = model.getNsPrefixURI("");
        if (baseURI == null) {
            return model.getResource("");
        } else {
            return model.getResource(baseURI);
        }
    }

    public static String writeModelToString(final Model model, final Lang lang) {
        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, model, lang);
        return out.toString();
    }

    /**
     * Returns a copy of the specified resources' model where resource is replaced
     * by replacement.
     * 
     * @param resource
     * @param replacement
     * @return
     */
    public static Model replaceResource(Resource resource, Resource replacement) {
        if (!resource.getModel().equals(replacement.getModel()))
            throw new IllegalArgumentException("resource and replacement must be from the same model");
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(resource.getModel().getNsPrefixMap());
        StmtIterator it = resource.getModel().listStatements();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            Resource subject = stmt.getSubject();
            Resource predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
            if (subject.equals(resource)) {
                subject = replacement;
            }
            if (predicate.equals(resource)) {
                predicate = replacement;
            }
            if (object.equals(resource)) {
                object = replacement;
            }
            Triple triple = new Triple(subject.asNode(), predicate.asNode(), object.asNode());
            result.getGraph().add(triple);
        }
        return result;
    }

    public static Set<URI> getGraphUris(Dataset dataset) {
        Iterator<String> urisIterator = dataset.listNames();
        Set<URI> uris = new HashSet<>();
        while (urisIterator.hasNext()) {
            uris.add(URI.create(urisIterator.next()));
        }
        return uris;
    }

    /**
     * Adds the specified objectModel to the model of the specified subject. In the
     * objectModel, the resource that is identified by the objectModel's base URI
     * (the "" URI prefix) will be replaced by a newly created blank node(B, see
     * later). All content of the objectModel is added to the model of the subject.
     * An additional triple (subject, property, B) is added. Moreover, the Namespace
     * prefixes are merged.
     * 
     * @param subject
     * @param property
     * @param objectModel caution - will be modified
     */
    public static void attachModelByBaseResource(final Resource subject, final Property property,
                    final Model objectModel) {
        attachModelByBaseResource(subject, property, objectModel, true);
    }

    /**
     * Adds the specified objectModel to the model of the specified subject. In the
     * objectModel, the resource that is identified by the objectModel's base URI
     * (the "" URI prefix) will be replaced by a newly created blank node(B, see
     * later). All content of the objectModel is added to the model of the subject.
     * An additional triple (subject, property, B) is added. Moreover, the Namespace
     * prefixes are merged.
     * 
     * @param subject
     * @param property
     * @param objectModel caution - will be modified
     * @param replaceBaseResourceByBlankNode
     */
    public static void attachModelByBaseResource(final Resource subject, final Property property,
                    final Model objectModel, final boolean replaceBaseResourceByBlankNode) {
        Model subjectModel = subject.getModel();
        // either explicitly use blank node, or do so if there is no base resource
        // prefix
        // as the model may have triples containing the null relative URI.
        // we still want to attach these and try to get them by using
        // the empty URI, and replacing that resource by a blank node
        if (replaceBaseResourceByBlankNode || objectModel.getNsPrefixURI("") == null) {
            // create temporary resource and replace objectModel's base resource to avoid
            // clashes
            String tempURI = "tmp:" + Integer.toHexString(objectModel.hashCode());
            replaceBaseResource(objectModel, objectModel.createResource(tempURI));
            // merge models
            subjectModel.add(objectModel);
            // replace temporary resource by blank node
            Resource blankNode = subjectModel.createResource();
            subject.addProperty(property, blankNode);
            replaceResourceInModel(subjectModel.getResource(tempURI), blankNode);
            // merge the prefixes, but don't add the objectModel's default prefix in any
            // case - we don't want it to end up as
            // the resulting model's base prefix.
            Map<String, String> objectModelPrefixes = objectModel.getNsPrefixMap();
            objectModelPrefixes.remove("");
            subjectModel.setNsPrefixes(mergeNsPrefixes(subjectModel.getNsPrefixMap(), objectModelPrefixes));
        } else {
            String baseURI = objectModel.getNsPrefixURI("");
            Resource baseResource = objectModel.getResource(baseURI); // getResource because it may exist already
            subjectModel.add(objectModel);
            baseResource = subjectModel.getResource(baseResource.getURI());
            subject.addProperty(property, baseResource);
            RdfUtils.replaceBaseResource(subjectModel, baseResource);
        }
    }

    /**
     * Creates a new Map object containing all prefixes from both specified maps.
     * When prefix mappings clash, the mappings from prioritaryPrefixes are used.
     * 
     * @param prioritaryPrefixes
     * @param additionalPrefixes
     * @return
     */
    public static Map<String, String> mergeNsPrefixes(final Map<String, String> prioritaryPrefixes,
                    final Map<String, String> additionalPrefixes) {
        Map<String, String> mergedPrefixes = new HashMap<>();
        mergedPrefixes.putAll(additionalPrefixes);
        mergedPrefixes.putAll(prioritaryPrefixes); // overwrites the additional prefixes when clashing
        return mergedPrefixes;
    }

    /**
     * Reads the InputStream into a Model. Sets a 'fantasy URI' as base URI and
     * handles it gracefully if the model read from the string defines its own base
     * URI. Special care is taken that the null relative URI ('<>') is replaced by
     * the baseURI.
     * 
     * @param in
     * @param rdfLanguage
     * @return a Model (possibly empty)
     */
    public static Model readRdfSnippet(final InputStream in, final String rdfLanguage) {
        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
        if (in == null)
            return model;
        String baseURI = "no:uri";
        model.setNsPrefix("", baseURI);
        model.read(in, baseURI, rdfLanguage);
        String baseURIAfterReading = model.getNsPrefixURI("");
        if (baseURIAfterReading == null) {
            model.setNsPrefix("", baseURI);
        } else if (!baseURI.equals(baseURIAfterReading)) {
            // the string representation of the model specified a base URI, but we used a
            // different one for reading.
            // We need to make sure that the one that is now set is the only one used
            ResourceUtils.renameResource(model.getResource(baseURI), model.getNsPrefixURI(""));
        }
        return model;
    }

    /**
     * Reads the InputStream into a Model. Sets a 'fantasy URI' as base URI and
     * handles it gracefully if the model read from the string defines its own base
     * URI. Special care is taken that the null relative URI ('<>') is replaced by
     * the baseURI.
     * 
     * @param in
     * @param rdfLanguage
     * @return a Model (possibly empty)
     */
    public static Model readRdfSnippet(final Reader in, final String rdfLanguage) {
        org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
        if (in == null)
            return model;
        String baseURI = "no:uri";
        model.setNsPrefix("", baseURI);
        model.read(in, baseURI, rdfLanguage);
        String baseURIAfterReading = model.getNsPrefixURI("");
        if (baseURIAfterReading == null) {
            model.setNsPrefix("", baseURI);
        } else if (!baseURI.equals(baseURIAfterReading)) {
            // the string representation of the model specified a base URI, but we used a
            // different one for reading.
            // We need to make sure that the one that is now set is the only one used
            ResourceUtils.renameResource(model.getResource(baseURI), model.getNsPrefixURI(""));
        }
        return model;
    }

    /**
     * Reads the specified string into a Model. Sets a 'fantasy URI' as base URI and
     * handles it gracefully if the model read from the string defines its own base
     * URI. Special care is taken that the null relative URI ('<>') is replaced by
     * the baseURI.
     * 
     * @param rdfAsString
     * @param rdfLanguage
     * @return a Model (possibly empty)
     */
    public static Model readRdfSnippet(final String rdfAsString, final String rdfLanguage) {
        return readRdfSnippet(new StringReader(rdfAsString), rdfLanguage);
    }

    /**
     * Evaluates the path on the model obtained by dereferencing the specified
     * resourceURI. If the path resolves to multiple resources, only the first one
     * is returned. <br />
     * <br />
     * Note: For more information on property paths, see
     * http://jena.sourceforge.net/ARQ/property_paths.html <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * 
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard);
     * </pre>
     * 
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     * @throws IllegalArgumentException if the node found by the path is not a URI
     */
    public static URI getURIPropertyForPropertyPath(final Model model, final URI resourceURI, Path propertyPath) {
        return toURI(getNodeForPropertyPath(model, resourceURI, propertyPath));
    }

    /**
     * Evaluates the path on all models in the dataset obtained by dereferencing the
     * specified resourceURI. If the path resolves to multiple resources, only the
     * first one is returned. <br />
     * <br />
     * Note: For more information on property paths, see
     * http://jena.sourceforge.net/ARQ/property_paths.html <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * 
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard);
     * </pre>
     * 
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     * @throws IllegalArgumentException if the node found by the path is not a URI
     */
    public static URI getURIPropertyForPropertyPath(final Dataset dataset, final URI resourceURI, Path propertyPath) {
        return toURI(getNodeForPropertyPath(dataset, resourceURI, propertyPath));
    }

    public static Iterator<URI> getURIsForPropertyPath(final Model model, final URI resourceURI, Path propertyPath) {
        Iterator<Node> nodeIterator = getNodesForPropertyPath(model, resourceURI, propertyPath);
        return new ProjectingIterator<Node, URI>(nodeIterator) {
            @Override
            public URI next() {
                return toURI(this.baseIterator.next());
            }
        };
    }

    public static Iterator<URI> getURIsForPropertyPath(final Dataset dataset, final URI resourceURI,
                    Path propertyPath) {
        Iterator<Node> nodeIterator = getNodesForPropertyPath(dataset, resourceURI, propertyPath);
        return new ProjectingIterator<Node, URI>(nodeIterator) {
            @Override
            public URI next() {
                return toURI(this.baseIterator.next());
            }
        };
    }

    /**
     * Evaluates the property path by executing a sparql query.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Iterator<RDFNode> getNodesForPropertyPathByQuery(final Dataset dataset, final URI resourceURI,
                    Path propertyPath) {
        String queryString = "select ?obj where { GRAPH <urn:x-arq:UnionGraph> { ?resource " + propertyPath.toString()
                        + " ?obj } }";
        Query query = QueryFactory.create(queryString);
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add("?resource", dataset.getDefaultModel().createResource(resourceURI.toString()));
        try (QueryExecution qExec = QueryExecutionFactory.create(query, dataset, initialBinding)) {
            qExec.getContext().set(TDB.symUnionDefaultGraph, true);
            final ResultSet results = qExec.execSelect();
            LinkedList<RDFNode> resultNodes = new LinkedList<>();
            while (results.hasNext()) {
                QuerySolution soln = results.next();
                RDFNode result = soln.get("obj");
                resultNodes.add(result);
            }
            return resultNodes.iterator();
        }
    }

    /**
     * Sets the vars of a given sparql query Replaces every instance of ::var:: with
     * the given object (this can only be an URI or a List of URIS at the moment)
     * 
     * @param stmt
     * @param var that will be replaced
     * @param obj object that is replacing the variable
     * @return replaced statement
     */
    public static String setSparqlVars(String stmt, String var, Object obj) {
        StringBuilder replacement = new StringBuilder();
        if (obj instanceof URI) {
            replacement.append("<").append(obj.toString()).append(">");
        } else if (obj instanceof List) {
            for (Object itm : (List) obj) {
                if (itm instanceof URI) {
                    replacement.append("<").append(itm.toString()).append(">,");
                }
            }
            replacement.deleteCharAt(replacement.length() - 1);
        }
        return stmt.replaceAll("::" + var + "::", replacement.toString());
    }

    /**
     * Sets the vars of a given sparql query Replaces every instance of ::var:: with
     * the given object (this can only be an URI or a List of URIS at the moment)
     * 
     * @param stmt
     * @param varMap replaces the key with the object within the given statement
     * @return replaced statement
     */
    public static String setSparqlVars(String stmt, Map<String, Object> varMap) {
        for (Map.Entry<String, Object> entry : varMap.entrySet()) {
            stmt = setSparqlVars(stmt, entry.getKey(), entry.getValue());
        }
        return stmt;
    }

    /**
     * Evaluates a property path on the specified dataset by executing a sparql
     * query and returns an iterator of URIs.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Iterator<URI> getURIsForPropertyPathByQuery(final Dataset dataset, final URI resourceURI,
                    Path propertyPath) {
        Iterator<RDFNode> nodeIterator = getNodesForPropertyPathByQuery(dataset, resourceURI, propertyPath);
        return new ProjectingIterator<RDFNode, URI>(nodeIterator) {
            @Override
            public URI next() {
                return toURI(this.baseIterator.next());
            }
        };
    }

    /**
     * Evaluates the path on the model obtained by dereferencing the specified
     * resourceURI. If the path resolves to multiple resources, only the first one
     * is returned. <br />
     * <br />
     * Note: For more information on property paths, see
     * http://jena.sourceforge.net/ARQ/property_paths.html <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * 
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard);
     * </pre>
     * 
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     */
    public static String getStringPropertyForPropertyPath(final Model model, final URI resourceURI, Path propertyPath) {
        return toString(getNodeForPropertyPath(model, resourceURI, propertyPath));
    }

    /**
     * Evaluates the path on each model in the dataset obtained by dereferencing the
     * specified resourceURI. If the path resolves to multiple resources, only the
     * first one is returned. <br />
     * <br />
     * Note: For more information on property paths, see
     * http://jena.sourceforge.net/ARQ/property_paths.html <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * 
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard);
     * </pre>
     * 
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     */
    public static String getStringPropertyForPropertyPath(final Dataset dataset, final URI resourceURI,
                    Path propertyPath) {
        return toString(getNodeForPropertyPath(dataset, resourceURI, propertyPath));
    }

    /**
     * Returns the literal lexical form of the specified node or null if the node is
     * null.
     * 
     * @param node
     * @return
     */
    public static String toString(Node node) {
        if (node == null)
            return null;
        return node.getLiteralLexicalForm();
    }

    /**
     * Returns the URI of the specified node or null if the node is null. If the
     * node does not represent a resource, an UnsupportedOperationException is
     * thrown.
     * 
     * @param node
     * @return
     */
    public static URI toURI(Node node) {
        if (node == null)
            return null;
        return URI.create(node.getURI());
    }

    /**
     * Returns the URI of the specified RDFNode or null if the node is null. If the
     * node does not represent a resource, a ResourceRequiredException is thrown.
     * 
     * @param node
     * @return
     */
    public static URI toURI(RDFNode node) {
        if (node == null)
            return null;
        return URI.create(node.asResource().getURI());
    }

    /**
     * Returns the first RDF node found in the specified model for the specified
     * property path.
     * 
     * @param model
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Node getNodeForPropertyPath(final Model model, URI resourceURI, Path propertyPath) {
        // Iterator<Node> result = PathEval.eval(model.getGraph(),
        // model.getResource(resourceURI.toString()).asNode(),
        // propertyPath);
        Iterator<Node> result = PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(),
                        propertyPath, Context.emptyContext);
        if (!result.hasNext())
            return null;
        return result.next();
    }

    public static Node getNodeForPropertyPath(final Model model, Node node, Path propertyPath) {
        // Iterator<Node> result = PathEval.eval(model.getGraph(),
        // model.getResource(resourceURI.toString()).asNode(),
        // propertyPath);
        Iterator<Node> result = PathEval.eval(model.getGraph(), node, propertyPath, Context.emptyContext);
        if (!result.hasNext())
            return null;
        return result.next();
    }

    /**
     * Returns the first RDF node found in the specified dataset for the specified
     * property path.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Node getNodeForPropertyPath(final Dataset dataset, final URI resourceURI, final Path propertyPath) {
        return findFirst(dataset, model -> getNodeForPropertyPath(model, resourceURI, propertyPath));
    }

    /**
     * Evaluates the specified path in the specified model, starting with the
     * specified resourceURI.
     * 
     * @param model
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Iterator<Node> getNodesForPropertyPath(final Model model, URI resourceURI, Path propertyPath) {
        return PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(),
                        propertyPath, Context.emptyContext);
    }

    /**
     * Evaluates the specified path in the specified model, starting with the
     * specified resourceURI.
     * 
     * @param model
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static <T> Iterator<T> getObjectsForPropertyPath(final Model model, URI resourceURI, Path propertyPath,
                    Function<Node, T> mapper) {
        return getObjectStreamForPropertyPath(model, resourceURI, propertyPath, mapper).iterator();
    }

    public static <T> Stream<T> getObjectStreamForPropertyPath(final Model model, URI resourceURI, Path propertyPath,
                    Function<Node, T> mapper) {
        Iterator<Node> result = PathEval.eval(model.getGraph(), model.getResource(resourceURI.toString()).asNode(),
                        propertyPath, Context.emptyContext);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(result, Spliterator.ORDERED), false)
                        .map(mapper);
    }

    /**
     * Evaluates the specified path in the specified dataset, starting with the
     * specified resourceURI.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static <T> Iterator<T> getObjectsForPropertyPath(final Dataset dataset, URI resourceURI, Path propertyPath,
                    Function<Node, T> mapper) {
        return getObjectStreamForPropertyPath(dataset, resourceURI, propertyPath, mapper).iterator();
    }

    /**
     * Evaluates the specified path in the specified dataset, starting with the
     * specified resourceURI.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static <T> Stream<T> getObjectStreamForPropertyPath(final Dataset dataset, URI resourceURI,
                    Path propertyPath, Function<Node, T> mapper) {
        return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(new DefaultModelSelector().select(dataset),
                                        Spliterator.ORDERED), false)
                        .flatMap(model -> getObjectStreamForPropertyPath(model, resourceURI, propertyPath, mapper));
    }

    /**
     * Evaluates the specified path in the specified dataset, starting with the
     * specified resourceURI.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static <T> Stream<T> getObjectStreamOfProperty(final Dataset dataset, URI resourceURI, URI property,
                    Function<RDFNode, T> mapper) {
        return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(new DefaultModelSelector().select(dataset),
                                        Spliterator.ORDERED), false)
                        .flatMap(model -> getObjectStreamOfProperty(model, resourceURI, property, mapper));
    }

    public static <T> Stream<T> getObjectStreamOfProperty(final Model model, URI resourceURI, URI property,
                    Function<RDFNode, T> mapper) {
        NodeIterator it = model.listObjectsOfProperty(model.createResource(resourceURI.toString()),
                        model.createProperty(property.toString()));
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false).map(mapper);
    }

    public static <T> List<T> getObjectsOfProperty(final Dataset dataset, final URI resource, final URI property,
                    Function<RDFNode, T> resultMapper) {
        return RdfUtils.visitFlattenedToList(dataset, (ModelVisitor<Collection<T>>) model -> {
            Resource res = model.getResource(resource.toString());
            if (res == null) {
                return Collections.emptyList();
            }
            NodeIterator it = model.listObjectsOfProperty(res, model.createProperty(property.toString()));
            List<T> ret = new ArrayList<>();
            while (it.hasNext()) {
                RDFNode node = it.next();
                ret.add(resultMapper.apply(node));
            }
            return ret;
        });
    }

    public static <T> Optional<T> getFirstObjectOfProperty(final Dataset dataset, final URI resource,
                    final URI property, Function<RDFNode, T> resultMapper) {
        T result = RdfUtils.findFirst(dataset, model -> {
            Resource subj = model.getResource(resource.toString());
            Property pred = model.getProperty(property.toString());
            if (pred == null) {
                return null;
            }
            Statement stmt = model.getProperty(subj, pred);
            RDFNode obj = stmt.getObject();
            return resultMapper.apply(obj);
        });
        return Optional.ofNullable(result);
    }

    public static <T> Optional<T> getFirstStatementMapped(final Dataset dataset, final URI subject, final URI predicate,
                    final URI object, Function<Statement, T> resultMapper) {
        T result = RdfUtils.findFirst(dataset, model -> {
            Resource subj = subject == null ? null : model.getResource(subject.toString());
            Property pred = predicate == null ? null : model.getProperty(predicate.toString());
            RDFNode obj = object == null ? null : model.getResource(object.toString());
            StmtIterator it = model.listStatements(subj, pred, obj);
            if (it.hasNext()) {
                return resultMapper.apply(it.next());
            }
            return null;
        });
        return Optional.ofNullable(result);
    }

    /**
     * Evaluates the specified path in each model of the specified dataset, starting
     * with the specified resourceURI.
     * 
     * @param dataset
     * @param resourceURI
     * @param propertyPath
     * @return
     */
    public static Iterator<Node> getNodesForPropertyPath(final Dataset dataset, final URI resourceURI,
                    final Path propertyPath) {
        return Iterators.concat(visit(dataset, model -> getNodesForPropertyPath(model, resourceURI, propertyPath)));
    }

    public static URI toUriOrNull(final Object uriStringOrNull) {
        if (uriStringOrNull == null)
            return null;
        return URI.create(uriStringOrNull.toString());
    }

    /**
     * Dataset visitor used for repeated application of model operations in a
     * dataset.
     */
    public static interface ModelVisitor<T> {
        public T visit(Model model);
    }

    /**
     * ModelSelector used to select which models in a dataset to visit.
     */
    public static interface ModelSelector {
        public Iterator<Model> select(Dataset dataset);
    }

    /**
     * ResultCombiner which combines to results of type T and returns it.
     */
    public static interface ResultCombiner<T> {
        public T combine(T first, T second);
    }

    /**
     * Selector that selects all models, including the default model. The first
     * model is the default model, the named models are returned in the order
     * specified by Dataset.listNames().
     */
    public static class DefaultModelSelector implements ModelSelector {
        @Override
        public Iterator<Model> select(final Dataset dataset) {
            List ret = new LinkedList<Model>();
            Model model = dataset.getDefaultModel();
            if (model != null) {
                ret.add(model);
            }
            for (Iterator<String> modelNames = dataset.listNames(); modelNames.hasNext();) {
                ret.add(dataset.getNamedModel(modelNames.next()));
            }
            return ret.iterator();
        }
    }

    public static Stream<Statement> toStatementStream(final Dataset dataset) {
        return toStatementStream(dataset, new DefaultModelSelector());
    }

    public static Stream<Statement> toStatementStream(final Dataset dataset, final ModelSelector modelSelector) {
        List<Statement> results = new LinkedList<>();
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            Model m = modelIterator.next();
            for (StmtIterator stmtIt = m.listStatements(); stmtIt.hasNext();) {
                results.add(stmtIt.next());
            }
        }
        return results.stream();
    }

    public static Stream<Statement> toStatementStream(final Model model) {
        List<Statement> results = new LinkedList<>();
        for (StmtIterator stmtIt = model.listStatements(); stmtIt.hasNext();) {
            results.add(stmtIt.next());
        }
        return results.stream();
    }

    public static Stream<Model> toModelStream(final Dataset dataset) {
        List<Model> ret = new LinkedList<>();
        Model model = dataset.getDefaultModel();
        if (model != null) {
            ret.add(model);
        }
        for (Iterator<String> modelNames = dataset.listNames(); modelNames.hasNext();) {
            ret.add(dataset.getNamedModel(modelNames.next()));
        }
        return ret.stream();
    }

    public static Stream<NamedModel> toNamedModelStream(final Dataset dataset, boolean includeDefaultModel) {
        List<NamedModel> ret = new LinkedList<>();
        if (includeDefaultModel) {
            Model model = dataset.getDefaultModel();
            if (model != null) {
                ret.add(new NamedModel(null, model));
            }
        }
        for (Iterator<String> modelNames = dataset.listNames(); modelNames.hasNext();) {
            String name = modelNames.next();
            ret.add(new NamedModel(name, dataset.getNamedModel(name)));
        }
        return ret.stream();
    }

    public static class NamedModel {
        private String name;
        private Model model;

        public NamedModel(String name, Model model) {
            super();
            this.name = name;
            this.model = model;
        }

        public String getName() {
            return name;
        }

        public Model getModel() {
            return model;
        }
    }

    private static ModelSelector DEFAULT_MODEL_SELECTOR = new DefaultModelSelector();

    /**
     * Returns a thread-safe, shared default model selector.
     * 
     * @return
     */
    public static ModelSelector getDefaultModelSelector() {
        return DEFAULT_MODEL_SELECTOR;
    };

    /**
     * Calls the specified ModelVisitor's visit method on each model of the dataset
     * that is selected by the ModelSelector. The rsults are collected in the
     * returned iterator.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @param <T>
     * @return
     */
    public static <T> Iterator<T> visit(Dataset dataset, ModelVisitor<T> visitor, ModelSelector modelSelector) {
        List<T> results = new LinkedList<>();
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            results.add(visitor.visit(modelIterator.next()));
        }
        return results.iterator();
    }

    public static <T> Iterator<T> visit(Dataset dataset, ModelVisitor<T> visitor) {
        return visit(dataset, visitor, getDefaultModelSelector());
    }

    /**
     * Visits all models and flattens the collections returned by the visitor into
     * one list.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @param <T>
     * @return
     */
    public static <E, T extends Collection<E>> List<E> visitFlattenedToList(Dataset dataset, ModelVisitor<T> visitor,
                    ModelSelector modelSelector) {
        List<E> results = new ArrayList<>();
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            results.addAll(visitor.visit(modelIterator.next()));
        }
        return results;
    }

    public static <E, T extends Collection<E>> List<E> visitFlattenedToList(Dataset dataset, ModelVisitor<T> visitor) {
        return visitFlattenedToList(dataset, visitor, getDefaultModelSelector());
    }

    /**
     * Visits all models and flattens the NodeIterator returned by the visitor into
     * one. Returns null if all visitors return null.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @return
     */
    public static NodeIterator visitFlattenedToNodeIterator(Dataset dataset, ModelVisitor<NodeIterator> visitor,
                    ModelSelector modelSelector) {
        NodeIterator it = null;
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            NodeIterator currentIt = visitor.visit(modelIterator.next());
            if (it == null) {
                it = currentIt;
            } else {
                it.andThen(currentIt);
            }
        }
        return it;
    }

    public static NodeIterator visitFlattenedToNodeIterator(Dataset dataset, ModelVisitor<NodeIterator> visitor) {
        return visitFlattenedToNodeIterator(dataset, visitor, getDefaultModelSelector());
    }

    /**
     * Returns the first non-null result obtained by calling the specified
     * ModelVisitor's visit method in the order defined by the specified
     * ModelSelector.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @param <T>
     * @return
     */
    public static <T> T findFirst(Dataset dataset, ModelVisitor<T> visitor, ModelSelector modelSelector) {
        List<T> results = new LinkedList<>();
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            T result = visitor.visit(modelIterator.next());
            if (result != null)
                return result;
        }
        return null;
    }

    public static <T> T findFirst(Dataset dataset, ModelVisitor<T> visitor) {
        return findFirst(dataset, visitor, getDefaultModelSelector());
    }

    /**
     * Returns the result obtained by calling the specified ModelVisitor's visit
     * method in the order defined by the specified ModelSelector. Throws an
     * IncorrectPropertyCountException if no result or more than one result is
     * found. ModelVisitors should implement the same exception strategy.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @param allowSame if true, multiple results will be checked with equals() and
     * if they are are equal, no exception is thrown
     * @param <T>
     * @return
     */
    public static <T> T findOne(Dataset dataset, ModelVisitor<T> visitor, ModelSelector modelSelector,
                    boolean allowSame) {
        T result = null;
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            T newResult = visitor.visit(modelIterator.next());
            if (newResult != null) {
                if (result != null) {
                    if (!allowSame || !result.equals(newResult)) {
                        throw new IncorrectPropertyCountException("Results were found in more than " + "one model", 1,
                                        2);
                    }
                }
                result = newResult;
            }
        }
        if (result == null)
            throw new IncorrectPropertyCountException("No result found", 1, 0);
        return result;
    }

    public static <T> T findOne(Dataset dataset, ModelVisitor<T> visitor, boolean allowSame) {
        return findOne(dataset, visitor, getDefaultModelSelector(), allowSame);
    }

    /**
     * Returns the result obtained by calling the specified ModelVisitor's visit
     * method where the result is combined with the ResultCombiner's combine method.
     * 
     * @param dataset
     * @param visitor
     * @param modelSelector
     * @param resultCombiner
     * @param <T>
     * @return
     */
    public static <T> T applyMethod(Dataset dataset, ModelVisitor<T> visitor, ModelSelector modelSelector,
                    ResultCombiner<T> resultCombiner) {
        T result = null;
        for (Iterator<Model> modelIterator = modelSelector.select(dataset); modelIterator.hasNext();) {
            T newResult = visitor.visit(modelIterator.next());
            if (result != null)
                result = resultCombiner.combine(result, newResult);
            else
                result = newResult;
        }
        return result;
    }

    public static <T> T applyMethod(Dataset dataset, ModelVisitor<T> visitor, ResultCombiner<T> resultCombiner) {
        return applyMethod(dataset, visitor, getDefaultModelSelector(), resultCombiner);
    }

    /**
     * Finds the first resource which is a specified property of a specified
     * resource.
     *
     * @param dataset <code>Dataset</code> to look into
     * @param resourceURI
     * @param p
     * @return <code>URI</code> of the resource
     */
    public static RDFNode findFirstPropertyFromResource(Dataset dataset, final URI resourceURI, final Property p) {
        return RdfUtils.findFirst(dataset, model -> findFirstPropertyFromResource(model, resourceURI, p));
    }

    /**
     * Finds resource which is a specified property of a specified resource. If
     * multiple (non equal) resources are found an exception is thrown.
     *
     * @param dataset <code>Dataset</code> to look into
     * @param resourceURI
     * @param p
     * @return <code>URI</code> of the resource
     */
    public static RDFNode findOnePropertyFromResource(Dataset dataset, final URI resourceURI, final Property p) {
        return RdfUtils.findOne(dataset, model -> findOnePropertyFromResource(model, resourceURI, p), true);
    }

    public static RDFNode findFirstPropertyFromResource(Dataset dataset, final Resource resource, final Property p) {
        return RdfUtils.findFirst(dataset, model -> findFirstPropertyFromResource(model, resource, p));
    }

    public static RDFNode findOnePropertyFromResource(Dataset dataset, final Resource resource, final Property p) {
        return RdfUtils.findOne(dataset, model -> findOnePropertyFromResource(model, resource, p), true);
    }

    /**
     * Finds the first resource which is a specified property of a specified
     * resource.
     *
     * @param model <code>Model</code> to look into
     * @param resourceURI
     * @param property
     * @return <code>URI</code> of the resource
     */
    public static RDFNode findFirstPropertyFromResource(Model model, URI resourceURI, Property property) {
        Resource resource = null;
        if (resourceURI != null) {
            resource = model.getResource(resourceURI.toString());
        }
        return findFirstPropertyFromResource(model, resource, property);
    }

    /**
     * Finds resource which is a specified property of a specified resource. If
     * multiple (non equal) resources are found an exception is thrown.
     *
     * @param model <code>Model</code> to look into
     * @param resourceURI
     * @param property
     * @return <code>URI</code> of the resource
     */
    public static RDFNode findOnePropertyFromResource(Model model, URI resourceURI, Property property) {
        Resource resource = null;
        if (resourceURI != null) {
            resource = model.getResource(resourceURI.toString());
        }
        return findOnePropertyFromResource(model, resource, property);
    }

    public static RDFNode findFirstPropertyOf(Resource resource, Property property) {
        return findFirstPropertyFromResource(resource.getModel(), resource, property);
    }

    public static Optional<RDFNode> findFirstPropertyOfO(Resource resource, Property property) {
        return Optional.ofNullable(findFirstPropertyOf(resource, property));
    }

    public static RDFNode findFirstPropertyFromResource(final Model model, final Resource resource,
                    final Property property) {
        Objects.nonNull(model);
        Objects.nonNull(resource);
        Objects.nonNull(property);
        NodeIterator iterator = model.listObjectsOfProperty(resource, property);
        if (iterator.hasNext())
            return iterator.next();
        return null;
    }

    public static RDFNode findOnePropertyFromResource(final Model model, final Resource resource,
                    final Property property) {
        List<RDFNode> foundNodes = new ArrayList<>();
        NodeIterator iterator = model.listObjectsOfProperty(resource, property);
        while (iterator.hasNext()) {
            foundNodes.add(iterator.next());
        }
        if (foundNodes.size() == 0)
            return null;
        else if (foundNodes.size() == 1)
            return foundNodes.get(0);
        else if (foundNodes.size() > 1) {
            RDFNode n = foundNodes.get(0);
            for (RDFNode node : foundNodes) {
                if (!node.equals(n))
                    throw new IncorrectPropertyCountException(1, 2);
            }
            return n;
        } else
            return null;
    }

    public static Resource findOneSubjectResource(Dataset dataset, Property property, RDFNode object) {
        return RdfUtils.findOne(dataset, model -> findOneOrNoSubjectResource(model, property, object), true);
    }

    public static List<Resource> findSubjectResources(Dataset dataset, Property property, RDFNode object) {
        return RdfUtils.visitFlattenedToList(dataset, model -> {
            List<Resource> ret = new ArrayList<>();
            ResIterator it = model.listSubjectsWithProperty(property, object);
            while (it.hasNext()) {
                ret.add(it.next());
            }
            return ret;
        });
    }

    public static Resource findOneSubjectResource(Model model, Property property, RDFNode object) {
        Resource resource = null;
        ResIterator iter = model.listSubjectsWithProperty(property, object);
        while (iter.hasNext()) {
            resource = iter.next();
            if (iter.hasNext()) {
                throw new IncorrectPropertyCountException("expecting exactly one subject resource for property "
                                + property.getURI() + " and object " + object.toString(), 1, 2);
            }
        }
        if (resource == null) {
            throw new IncorrectPropertyCountException("expecting exactly one subject resource for property "
                            + property.getURI() + " and object " + object.toString(), 1, 0);
        }
        return resource;
    }

    public static Resource findOneOrNoSubjectResource(Model model, Property property, RDFNode object) {
        Resource resource = null;
        ResIterator iter = model.listSubjectsWithProperty(property, object);
        while (iter.hasNext()) {
            resource = iter.next();
            if (iter.hasNext()) {
                throw new IncorrectPropertyCountException("expecting one or no subject resource for property "
                                + property.getURI() + " and object " + object.toString(), 1, 2);
            }
        }
        return resource;
    }

    /**
     * Finds the first triple in the specified model that has the specified propery
     * and object. The subject is expected to be a resource.
     * 
     * @param model
     * @param property
     * @param object
     * @param allowMultiple if false, will throw an IllegalArgumentException if more
     * than one triple is found
     * @param allowNone if false, will throw an IllegalArgumentException if no
     * triple is found
     * @return
     */
    public static URI findFirstSubjectUri(Model model, Property property, RDFNode object, boolean allowMultiple,
                    boolean allowNone) {
        URI retVal = null;
        StmtIterator it = model.listStatements(null, property, object);
        if (!it.hasNext() && !allowNone)
            throw new IllegalArgumentException("expecting at least one triple");
        if (it.hasNext()) {
            retVal = URI.create(it.nextStatement().getSubject().asResource().toString());
        }
        if (!allowMultiple && it.hasNext())
            throw new IllegalArgumentException("not expecting more than one triple");
        return retVal;
    }

    public static URI findFirstObjectUri(Model model, Property property, RDFNode object, boolean allowMultiple,
                    boolean allowNone) {
        URI retVal = null;
        StmtIterator it = model.listStatements(null, property, object);
        if (!it.hasNext() && !allowNone)
            throw new IllegalArgumentException("expecting at least one triple");
        if (it.hasNext()) {
            retVal = URI.create(it.nextStatement().getObject().asResource().toString());
        }
        if (!allowMultiple && it.hasNext())
            throw new IllegalArgumentException("not expecting more than one triple");
        return retVal;
    }

    /**
     * Creates a new graph URI for the specified dataset by appending a specified
     * string (toAppend) and then n alphanumeric characters to the specified String.
     * It is guaranteed that the resulting URI is not used as a graph name in the
     * specified dataset. Note that the implementation is not synchronized, so
     * concurrent executions of the method may result in identical URIs being
     * returned. If both the specified baseURI and the toAppend string contain a
     * hash sign ('#'), the hash-part will be removed from the base uri before the
     * result will be crated.
     *
     * @param baseURI the URI to be extended.
     * @param toAppend a string that will be appended directly to the URI.
     * @param length number of alphanumeric characters that are appended to
     * <code>toAppend</code>.
     * @param dataset the dataset that will be checked to determine if the resulting
     * URI is new.
     * @return an URI that is previously unused as a graph URI.
     */
    public static URI createNewGraphURI(String baseURI, String toAppend, int length, final Dataset dataset) {
        return createNewGraphURI(baseURI, toAppend, length, graphUri -> !dataset.containsNamedModel(graphUri));
    }

    /**
     * Creates a new graph URI for the specified dataset by appending a specified
     * string (toAppend) and then n alphanumeric characters to the specified String.
     * It is guaranteed that the resulting URI is not used as a graph name in the
     * specified dataset. Note that the implementation is not synchronized, so
     * concurrent executions of the method may result in identical URIs being
     * returned. If both the specified baseURI and the toAppend string contain a
     * hash sign ('#'), the hash-part will be removed from the base uri before the
     * result will be crated.
     *
     * @param baseURI the URI to be extended.
     * @param toAppend a string that will be appended directly to the URI.
     * @param length number of alphanumeric characters that are appended to
     * <code>toAppend</code>.
     * @return an URI that is previously unused as a graph URI.
     */
    public static URI createNewGraphURI(String baseURI, String toAppend, int length, GraphNameCheck check) {
        if (toAppend.contains("#")) {
            int hashIndex = baseURI.indexOf('#');
            if (hashIndex > -1) {
                baseURI = baseURI.substring(0, hashIndex);
            }
        }
        int maxTries = 5;
        for (int i = 0; i < maxTries; i++) {
            String graphName = baseURI + toAppend + randomString.nextString(length);
            if (check.isGraphUriOk(graphName)) {
                return URI.create(graphName);
            }
        }
        throw new IllegalStateException("Tried " + maxTries + " times to generate a new graph URI (" + length
                        + " random" + " characters), but were unable to generate a previously unused one; giving up.");
    }

    public static void addAllStatements(Model toModel, Model fromModel) {
        StmtIterator stmtIterator = fromModel.listStatements();
        while (stmtIterator.hasNext()) {
            toModel.add(stmtIterator.nextStatement());
        }
    }

    public static void addPrefixMapping(Model toModel, Model fromModel) {
        for (String prefix : fromModel.getNsPrefixMap().keySet()) {
            String uri = toModel.getNsPrefixURI(prefix);
            if (uri == null) { // if no such prefix-uri yet, add it
                toModel.setNsPrefix(prefix, fromModel.getNsPrefixURI(prefix));
            } else {
                if (!uri.equals(fromModel.getNsPrefixURI(prefix))) {
                    // prefix-uri collision, redefine prefix
                    int counter = 2;
                    while (!toModel.getNsPrefixMap().containsKey(prefix + counter)) {
                        counter++;
                    }
                    toModel.setNsPrefix(prefix + counter, fromModel.getNsPrefixURI(prefix));
                } /*
                   * else { // prefix-uri is already there, do nothing }
                   */
            }
        }
    }

    public static List<String> getModelNames(Dataset dataset) {
        List<String> modelNames = new ArrayList<>();
        Iterator<String> names = dataset.listNames();
        while (names.hasNext()) {
            modelNames.add(names.next());
        }
        return modelNames;
    }

    public static String writeDatasetToString(final Dataset dataset, final Lang lang) {
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, dataset, lang);
        return sw.toString();
    }

    public static Dataset readDatasetFromString(final String data, final Lang lang) {
        StringReader sr = new StringReader(data);
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, sr, "no:uri", lang);
        return dataset;
    }

    /**
     * Returns the names of all models that mention the specified resource.
     * 
     * @param resource
     * @return
     */
    public static Set<String> getModelsOfResource(Dataset dataset, RDFNode resource) {
        return toNamedModelStream(dataset, false)
                        .map(nm -> nm.model.containsResource(resource) ? nm.name : null)
                        .filter(name -> name != null).collect(Collectors.toSet());
    }

    /**
     * Returns the names of all models that mention the specified resource in the
     * subject of a triple
     * 
     * @param resource
     * @return
     */
    public static Set<String> getModelsOfSubjectResource(Dataset dataset, RDFNode resource) {
        return toNamedModelStream(dataset, false)
                        .map(nm -> nm.model.contains(resource.asResource(), null, (RDFNode) null) ? nm.name
                                        : null)
                        .filter(name -> name != null).collect(Collectors.toSet());
    }

    /**
     * Adds the second dataset to the first one, merging default models and models
     * with identical name.
     * 
     * @param baseDataset
     * @param toBeAddedtoBase
     * @param replaceNamedModel if true, named graphs are not merged but replaced
     * @return the modified baseDataset
     */
    public static Dataset addDatasetToDataset(final Dataset baseDataset, final Dataset toBeAddedtoBase,
                    boolean replaceNamedModel) {
        if (baseDataset == null)
            throw new IllegalArgumentException("baseDataset must not be null");
        if (toBeAddedtoBase == null)
            throw new IllegalArgumentException("toBeAddedToBase must not be null");
        baseDataset.getDefaultModel().add(toBeAddedtoBase.getDefaultModel());
        for (Iterator<String> nameIt = toBeAddedtoBase.listNames(); nameIt.hasNext();) {
            String modelName = nameIt.next();
            if (baseDataset.containsNamedModel(modelName)) {
                if (replaceNamedModel) {
                    baseDataset.removeNamedModel(modelName);
                    baseDataset.addNamedModel(modelName, toBeAddedtoBase.getNamedModel(modelName));
                } else {
                    baseDataset.getNamedModel(modelName).add(toBeAddedtoBase.getNamedModel(modelName));
                }
            } else {
                baseDataset.addNamedModel(modelName, toBeAddedtoBase.getNamedModel(modelName));
            }
        }
        return baseDataset;
    }

    /**
     * Adds the second dataset to the first one, merging default models and models
     * with identical name.
     * 
     * @param baseDataset
     * @param toBeAddedtoBase
     * @return the modified baseDataset
     */
    public static Dataset addDatasetToDataset(final Dataset baseDataset, final Dataset toBeAddedtoBase) {
        return addDatasetToDataset(baseDataset, toBeAddedtoBase, false);
    }

    public static Model mergeAllDataToSingleModel(final Dataset ds) {
        // merge default graph and all named graph data into the default graph to be
        // able to query all of it at once
        Model mergedModel = ModelFactory.createDefaultModel();
        Iterator<String> nameIter = ds.listNames();
        mergedModel.add(ds.getDefaultModel());
        while (nameIter.hasNext()) {
            mergedModel.add(ds.getNamedModel(nameIter.next()));
        }
        return mergedModel;
    }

    /**
     * Adds all triples of the dataset to the model.
     * 
     * @param dataset
     * @param model
     */
    public static void copyDatasetTriplesToModel(final Dataset dataset, final Model model) {
        assert dataset != null : "dataset must not be null";
        assert model != null : "model must not be null";
        visit(dataset, datasetModel -> {
            model.add(datasetModel);
            return null;
        });
    }

    /**
     * Condense a model to a minimum of statements by iteratively removing single
     * statements and testing if the condensed model is still valid. A test function
     * is used that can be passed to test for the validity of the model in every
     * step.
     *
     * @param model input model to be condensed
     * @param isModelValidTest test function should return true if the model is
     * valid (previous condensation step was ok) and false otherwise
     * @return the condensed model
     */
    public static Model condenseModelByIterativeTesting(Model model, Function<Model, Boolean> isModelValidTest) {
        Model condensedModel = RdfUtils.cloneModel(model);
        boolean done = false;
        while (!done) {
            done = true;
            for (Statement stmt : condensedModel.listStatements().toList()) {
                Model backupModel = RdfUtils.cloneModel(condensedModel);
                condensedModel.remove(stmt);
                if (isModelValidTest.apply(condensedModel)) {
                    done = false;
                } else {
                    condensedModel = backupModel;
                }
            }
        }
        return condensedModel;
    }

    public static interface GraphNameCheck {
        public boolean isGraphUriOk(String graphUri);
    }
}
