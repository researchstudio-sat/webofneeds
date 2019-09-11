package won.protocol.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.AtomGraphType;
import won.protocol.model.AtomState;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMATCH;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class wraps the atom models (atom and sysinfo graphs in an atom
 * dataset). It provides abstraction for the atom structure of is/seeks content
 * nodes that are part of the atom model. It can be used to load and query an
 * existing atom dataset (or models). Furthermore it can be used to create an
 * atom model by adding triples.
 * <p>
 * Created by hfriedrich on 16.03.2017.
 */
public class AtomModelWrapper {
    // holds all the atom data with its different (default and named) models
    protected Dataset atomDataset;
    private String sysInfoGraphName;
    private String atomModelGraphName;

    /**
     * Create a new atom model (incluing sysinfo)
     *
     * @param atomUri atom uri to create the atom models for
     */
    public AtomModelWrapper(URI atomUri) {
        this(atomUri.toString());
    }

    /**
     * Create a new atom model (incluing sysinfo)
     *
     * @param atomUri atom uri to create the atom models for
     */
    public AtomModelWrapper(String atomUri) {
        atomDataset = DatasetFactory.createGeneral();
        Model atomModel = ModelFactory.createDefaultModel();
        DefaultPrefixUtils.setDefaultPrefixes(atomModel);
        atomModel.createResource(atomUri, WON.Atom);
        Model sysInfoModel = ModelFactory.createDefaultModel();
        DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
        sysInfoModel.createResource(atomUri, WON.Atom);
        this.atomModelGraphName = atomUri + "#atom";
        atomDataset.addNamedModel(this.atomModelGraphName, atomModel);
    }

    /**
     * Load an atom dataset and extract the atom and sysinfo models from it
     *
     * @param ds atom dataset to load
     */
    public AtomModelWrapper(Dataset ds) {
        this(ds, false);
    }

    /**
     * Load an atom dataset and extract the atom and sysinfo models from it
     *
     * @param ds atom dataset to load
     * @param addDefaultGraphs if this is set to true an atomModelGraph and a
     * sysInfoGraph will be added to the dataset
     */
    public AtomModelWrapper(Dataset ds, boolean addDefaultGraphs) {
        atomDataset = ds;
        Resource atomNode = getAtomContentNode();
        atomNode = (atomNode != null) ? atomNode : getAtomNode(AtomGraphType.SYSINFO);
        String atomUri = (atomNode != null) ? atomNode.getURI() : null;
        if (addDefaultGraphs && atomUri != null) {
            if (getAtomModel() == null) {
                Model atomModel = ModelFactory.createDefaultModel();
                atomModel.createResource(atomUri, WON.Atom);
                DefaultPrefixUtils.setDefaultPrefixes(atomModel);
                this.atomModelGraphName = "dummy#atom";
                atomDataset.addNamedModel(this.atomModelGraphName, atomModel);
            }
            if (getSysInfoModel() == null) {
                Model sysInfoModel = ModelFactory.createDefaultModel();
                sysInfoModel.createResource(atomUri, WON.Atom);
                DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
                this.sysInfoGraphName = "dummy#sysinfo";
                atomDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            }
        }
    }

    /**
     * Load the atom and sysinfo models, if one of these models is null then
     * initialize the other one as default model
     *
     * @param atomModel
     * @param sysInfoModel
     */
    public AtomModelWrapper(Model atomModel, Model sysInfoModel) {
        atomDataset = DatasetFactory.createGeneral();
        String atomUri = null;
        if (sysInfoModel != null) {
            this.sysInfoGraphName = "dummy#sysinfo";
            atomDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            atomUri = getAtomNode(AtomGraphType.SYSINFO).getURI();
        }
        if (atomModel != null) {
            this.atomModelGraphName = "dummy#atom";
            atomDataset.addNamedModel(this.atomModelGraphName, atomModel);
            atomUri = getAtomContentNode().getURI();
        }
        if (atomUri != null) {
            if (sysInfoModel == null) {
                sysInfoModel = ModelFactory.createDefaultModel();
                DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
                sysInfoModel.createResource(atomUri, WON.Atom);
                this.sysInfoGraphName = "dummy#sysinfo";
                atomDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            }
            if (atomModel == null) {
                atomModel = ModelFactory.createDefaultModel();
                DefaultPrefixUtils.setDefaultPrefixes(atomModel);
                atomModel.createResource(atomUri, WON.Atom);
                this.atomModelGraphName = "dummy#atom";
                atomDataset.addNamedModel(this.atomModelGraphName, atomModel);
            }
        }
    }

    /**
     * Indicates if the wrapped data looks like atom data.
     * 
     * @return
     */
    public static boolean isAAtom(Dataset ds) {
        if (ds == null || ds.isEmpty())
            return false;
        AtomModelWrapper wrapper = new AtomModelWrapper(ds, false);
        return wrapper.getAtomContentNode() != null && wrapper.getAtomNode(AtomGraphType.SYSINFO) != null;
    }

    public Model getAtomModel() {
        Iterator<String> modelNameIter = atomDataset.listNames();
        if (this.atomModelGraphName != null && atomDataset.getNamedModel(this.atomModelGraphName) != null) {
            return atomDataset.getNamedModel(this.atomModelGraphName);
        }
        Model defaultModel = atomDataset.getDefaultModel();
        if (defaultModel.listSubjectsWithProperty(RDF.type, WON.Atom).hasNext()
                        && !defaultModel.listSubjectsWithProperty(WON.atomState).hasNext()) {
            return defaultModel;
        }
        while (modelNameIter.hasNext()) {
            String tempModelName = modelNameIter.next();
            Model model = atomDataset.getNamedModel(tempModelName);
            if (tempModelName.equals("dummy#sysinfo")) {
                continue;
            }
            if (model.listSubjectsWithProperty(RDF.type, WON.Atom).hasNext()
                            && !model.listSubjectsWithProperty(WON.atomState).hasNext()) {
                this.atomModelGraphName = tempModelName;
                return model;
            }
        }
        return null;
    }

    public Model getSysInfoModel() {
        Iterator<String> modelNameIter = atomDataset.listNames();
        if (this.sysInfoGraphName != null && atomDataset.getNamedModel(this.sysInfoGraphName) != null) {
            return atomDataset.getNamedModel(this.sysInfoGraphName);
        }
        Model defaultModel = atomDataset.getDefaultModel();
        if (defaultModel.listSubjectsWithProperty(RDF.type, WON.Atom).hasNext()
                        && defaultModel.listSubjectsWithProperty(WON.atomState).hasNext()) {
            return defaultModel;
        }
        while (modelNameIter.hasNext()) {
            String tempModelName = modelNameIter.next();
            if (tempModelName.equals("dummy#atom")) {
                continue;
            }
            Model model = atomDataset.getNamedModel(tempModelName);
            if (model.listSubjectsWithProperty(RDF.type, WON.Atom).hasNext()
                            && model.listSubjectsWithProperty(WON.atomState).hasNext()) {
                this.sysInfoGraphName = tempModelName;
                return model;
            }
        }
        return null;
    }

    public boolean hasDerivedModel() {
        Optional<RDFNode> derivedGraphName = RdfUtils.findFirstPropertyOfO(getAtomNode(AtomGraphType.SYSINFO),
                        WON.derivedGraph);
        if (derivedGraphName.isPresent() && derivedGraphName.get().isURIResource()) {
            String name = derivedGraphName.get().asResource().getURI();
            if (this.atomDataset.containsNamedModel(name)) {
                return true;
            }
        }
        return false;
    }

    public Optional<Model> getDerivedModel() {
        Optional<RDFNode> derivedGraphName = RdfUtils.findFirstPropertyOfO(getAtomNode(AtomGraphType.SYSINFO),
                        WON.derivedGraph);
        if (derivedGraphName.isPresent() && derivedGraphName.get().isURIResource()) {
            String name = derivedGraphName.get().asResource().getURI();
            if (this.atomDataset.containsNamedModel(name)) {
                return Optional.of(RdfUtils.cloneModel(this.atomDataset.getNamedModel(name)));
            }
        }
        return Optional.empty();
    }

    /**
     * get the atom or sysinfo model
     *
     * @param graph type specifies the atom or sysinfo model to return
     * @return atom or sysinfo model
     */
    public Model copyAtomModel(AtomGraphType graph) {
        if (graph.equals(AtomGraphType.ATOM)) {
            return RdfUtils.cloneModel(getAtomModel());
        } else {
            return RdfUtils.cloneModel(getSysInfoModel());
        }
    }

    /**
     * get the complete dataset
     *
     * @return copy of atomDataset
     */
    public Dataset copyDataset() {
        return RdfUtils.cloneDataset(atomDataset);
    }

    /**
     * get the node of the atom of either the atom model or the sysinfo model
     *
     * @param graph type specifies the atom or sysinfo atom node to return
     * @return atom or sysinfo atom node
     */
    protected Resource getAtomNode(AtomGraphType graph) {
        if (graph.equals(AtomGraphType.ATOM) && getAtomModel() != null) {
            ResIterator iter = getAtomModel().listSubjectsWithProperty(RDF.type, WON.Atom);
            if (iter.hasNext()) {
                return iter.next();
            }
        } else if (graph.equals(AtomGraphType.SYSINFO) && getSysInfoModel() != null) {
            ResIterator iter = getSysInfoModel().listSubjectsWithProperty(RDF.type, WON.Atom);
            if (iter.hasNext()) {
                return iter.next();
            }
        }
        return null;
    }

    /**
     * get the content node of the atom
     * 
     * @return
     */
    public Resource getAtomContentNode() {
        return getAtomNode(AtomGraphType.ATOM);
    }

    public String getAtomUri() {
        return getAtomContentNode().getURI();
    }

    public void addFlag(Resource flag) {
        getAtomContentNode().addProperty(WONMATCH.flag, flag);
    }

    public boolean flag(Resource flag) {
        Resource atomRes = getAtomContentNode();
        return atomRes != null && atomRes.hasProperty(WONMATCH.flag, flag);
    }

    public Calendar getDoNotMatchBefore() {
        Statement prop = getAtomContentNode().getProperty(WONMATCH.doNotMatchBefore);
        if (prop == null) {
            return null;
        }
        RDFNode literal = prop.getObject();
        if (!literal.isLiteral()) {
            return null; // This silently fails with a null, but in our case I think this is preferred to
                         // throwing an exception
        }
        Object data = literal.asLiteral().getValue();
        if (data instanceof XSDDateTime) {
            return ((XSDDateTime) data).asCalendar();
        } else {
            return null;
        }
    }

    public Calendar getDoNotMatchAfter() {
        Statement prop = getAtomContentNode().getProperty(WONMATCH.doNotMatchAfter);
        if (prop == null) {
            return null;
        }
        RDFNode literal = prop.getObject();
        if (!literal.isLiteral()) {
            return null; // This silently fails with a null, but in our case I think this is preferred to
                         // throwing an exception
        }
        Object data = literal.asLiteral().getValue();
        if (data instanceof XSDDateTime) {
            return ((XSDDateTime) data).asCalendar();
        } else {
            return null;
        }
    }

    public void addMatchingContext(String context) {
        getAtomContentNode().addProperty(WONMATCH.matchingContext, context);
    }

    public boolean matchingContext(String context) {
        return getAtomContentNode().hasProperty(WONMATCH.matchingContext, context);
    }

    public void addQuery(String query) {
        getAtomContentNode().addProperty(WONMATCH.sparqlQuery, query);
    }

    public Optional<String> getQuery() {
        Statement stmt = getAtomContentNode().getProperty(WONMATCH.sparqlQuery);
        if (stmt == null)
            return Optional.empty();
        return Optional.of(stmt.getString());
    }

    public boolean sparqlQuery() {
        return getAtomContentNode().hasProperty(WONMATCH.sparqlQuery);
    }

    public Collection<String> getMatchingContexts() {
        Collection<String> matchingContexts = new LinkedList<>();
        NodeIterator iter = getAtomModel().listObjectsOfProperty(getAtomContentNode(), WONMATCH.matchingContext);
        while (iter.hasNext()) {
            matchingContexts.add(iter.next().asLiteral().getString());
        }
        return matchingContexts;
    }

    /**
     * Add a socket. The socketURI must be a fragment URI off the atom URI, i.e.
     * [atomuri]#socketid, or it is just a fragment identifier, which will be
     * interpreted relative to the atomURI, i.e. #socketid -> [atomuri]#socketid.
     * 
     * @param socketUri uniquely identifies this socket of this atom
     * @param socketTypeUri the type of the socket, e.g. won:ChatSocket
     */
    public void addSocket(String socketUri, String socketTypeUri) {
        if (socketUri.startsWith("#")) {
            socketUri = getAtomUri() + socketUri;
        } else if (!socketUri.startsWith(getAtomUri())) {
            throw new IllegalArgumentException(
                            "The socketURI must start with '[atomURI]#' or '#' but was: " + socketUri);
        }
        Resource socket = getAtomModel().getResource(socketUri);
        Resource socketType = getAtomModel().createResource(socketTypeUri);
        getAtomContentNode().addProperty(WON.socket, socket);
        socket.addProperty(WON.socketDefinition, socketType);
    }

    public void setDefaultSocket(String socketUri) {
        if (socketUri.startsWith("#")) {
            socketUri = getAtomUri() + socketUri;
        } else if (!socketUri.startsWith(getAtomUri())) {
            throw new IllegalArgumentException(
                            "The socketURI must start with '[atomURI]#' or '#' but was: " + socketUri);
        }
        Resource socket = getAtomModel().getResource(socketUri);
        getAtomContentNode().addProperty(WON.defaultSocket, socket);
    }

    public Optional<String> getDefaultSocket() {
        Statement stmt = getAtomContentNode().getProperty(WON.defaultSocket);
        if (stmt == null)
            return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }

    public Collection<String> getSocketUris() {
        Collection<String> socketUris = new LinkedList<>();
        NodeIterator iter = getAtomModel().listObjectsOfProperty(getAtomContentNode(), WON.socket);
        while (iter.hasNext()) {
            socketUris.add(iter.next().asResource().getURI());
        }
        return socketUris;
    }

    public Map<URI, URI> getSocketTypeUriMap() {
        Map<URI, URI> socketTypeUriMap = new HashMap<>();
        Collection<String> socketUris = getSocketUris();
        socketUris.forEach(socketUri -> {
            Optional<URI> socketTypeUri = getSocketTypeUri(socketUri);
            socketTypeUri.ifPresent(uri -> socketTypeUriMap.put(URI.create(socketUri), uri));
        });
        return socketTypeUriMap;
    }

    public Optional<String> getSocketType(String socketUri) {
        Resource socket = getAtomModel().createResource(socketUri);
        if (!getAtomContentNode().hasProperty(WON.socket, socket)) {
            return Optional.empty();
        }
        Statement stmt = socket.getProperty(WON.socketDefinition);
        if (stmt == null)
            return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }

    public Optional<URI> getSocketTypeUri(String socketUri) {
        Resource socket = getAtomModel().createResource(socketUri);
        if (!getAtomContentNode().hasProperty(WON.socket, socket)) {
            return Optional.empty();
        }
        Statement stmt = socket.getProperty(WON.socketDefinition);
        if (stmt == null)
            return Optional.empty();
        return Optional.of(URI.create(stmt.getObject().toString()));
    }

    public Collection<Resource> getGoals() {
        Collection<Resource> goalUris = new LinkedList<>();
        NodeIterator iter = getAtomModel().listObjectsOfProperty(getAtomContentNode(), WON.goal);
        while (iter.hasNext()) {
            goalUris.add(iter.next().asResource());
        }
        return goalUris;
    }

    public Resource getGoal(String uri) {
        return getAtomModel().getResource(uri);
    }

    public Model getShapesGraph(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getAtomModel().listObjectsOfProperty(goalNode, WON.shapesGraph);
            if (nodeIter.hasNext()) {
                String shapesGraphUri = nodeIter.next().asResource().getURI();
                return atomDataset.getNamedModel(shapesGraphUri);
            }
        }
        return null;
    }

    public String getShapesGraphName(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getAtomModel().listObjectsOfProperty(goalNode, WON.shapesGraph);
            if (nodeIter.hasNext()) {
                return nodeIter.next().asResource().getURI();
            }
        }
        return null;
    }

    public Model getDataGraph(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getAtomModel().listObjectsOfProperty(goalNode, WON.dataGraph);
            if (nodeIter.hasNext()) {
                String dataGraphUri = nodeIter.next().asResource().getURI();
                return atomDataset.getNamedModel(dataGraphUri);
            }
        }
        return null;
    }

    public String getDataGraphName(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getAtomModel().listObjectsOfProperty(goalNode, WON.dataGraph);
            if (nodeIter.hasNext()) {
                return nodeIter.next().asResource().getURI();
            }
        }
        return null;
    }

    public void setAtomState(AtomState state) {
        Resource stateRes = AtomState.ACTIVE.equals(state) ? WON.ATOM_STATE_ACTIVE : WON.ATOM_STATE_INACTIVE;
        Resource atom = getAtomNode(AtomGraphType.SYSINFO);
        atom.removeAll(WON.atomState);
        atom.addProperty(WON.atomState, stateRes);
    }

    public AtomState getAtomState() {
        Model sysInfoModel = getSysInfoModel();
        sysInfoModel.enterCriticalSection(true);
        RDFNode state = RdfUtils.findOnePropertyFromResource(sysInfoModel, getAtomNode(AtomGraphType.SYSINFO),
                        WON.atomState);
        sysInfoModel.leaveCriticalSection();
        if (state.equals(WON.ATOM_STATE_ACTIVE)) {
            return AtomState.ACTIVE;
        } else if (state.equals(WON.ATOM_STATE_INACTIVE)) {
            return AtomState.INACTIVE;
        } else if (state.equals(WON.ATOM_STATE_DELETED)) {
            return AtomState.DELETED;
        }
        throw new IllegalStateException("Unrecognized atom state: " + state);
    }

    public ZonedDateTime getCreationDate() {
        String dateString = RdfUtils.findOnePropertyFromResource(getSysInfoModel(), getAtomNode(AtomGraphType.SYSINFO),
                        DCTerms.created).asLiteral().getString();
        return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
    }

    public ZonedDateTime getModifiedDate() {
        String dateString = RdfUtils.findOnePropertyFromResource(getSysInfoModel(), getAtomNode(AtomGraphType.SYSINFO),
                        DCTerms.modified).asLiteral().getString();
        return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
    }

    public void setConnectionContainerUri(String containerUri) {
        Resource container = getSysInfoModel().createResource(containerUri);
        Resource atom = getAtomNode(AtomGraphType.SYSINFO);
        atom.removeAll(WON.connections);
        atom.addProperty(WON.connections, container);
    }

    public String getConnectionContainerUri() {
        return RdfUtils.findOnePropertyFromResource(getSysInfoModel(), getAtomNode(AtomGraphType.SYSINFO),
                        WON.connections).asResource().getURI();
    }

    public void setWonNodeUri(String nodeUri) {
        Resource node = getSysInfoModel().createResource(nodeUri);
        Resource atom = getAtomNode(AtomGraphType.SYSINFO);
        atom.removeAll(WON.wonNode);
        atom.addProperty(WON.wonNode, node);
    }

    public String getWonNodeUri() {
        return RdfUtils.findOnePropertyFromResource(getSysInfoModel(), getAtomNode(AtomGraphType.SYSINFO), WON.wonNode)
                        .asResource().getURI();
    }

    /**
     * create a goal content node below the atom node of the atom model.
     * 
     * @param uri uri of the content node, if null then create blank node
     * @return content node created
     */
    public Resource createGoalNode(String uri) {
        Resource contentNode = (uri != null) ? getAtomModel().createResource(uri) : getAtomModel().createResource();
        addGoalPropertyToAtomNode(contentNode);
        return contentNode;
    }

    /**
     * create a goal content node below the atom node of the atom model.
     * 
     * @param uri uri of the content node, if null then create blank node
     * @return content node created
     */
    public Resource createSeeksNode(String uri) {
        Resource contentNode = (uri != null) ? getAtomModel().createResource(uri) : getAtomModel().createResource();
        addSeeksPropertyToAtomNode(contentNode);
        return contentNode;
    }

    private void addGoalPropertyToAtomNode(RDFNode contentNode) {
        Resource atomNode = getAtomContentNode();
        atomNode.addProperty(WON.goal, contentNode);
    }

    private void addSeeksPropertyToAtomNode(RDFNode contentNode) {
        Resource atomNode = getAtomContentNode();
        atomNode.addProperty(WONMATCH.seeks, contentNode);
    }

    public Collection<Resource> getGoalNodes() {
        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = "{ ?atomNode a won:Atom. ?atomNode won:goal ?contentNode. }";
        String queryString = "prefix won: <https://w3id.org/won/core#> \n"
                        + "prefix match: <https://w3id.org/won/matching#> \n"
                        + "prefix con: <https://w3id.org/won/content#> \n"
                        + "SELECT DISTINCT ?contentNode WHERE { \n"
                        + queryClause + "\n }";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getAtomModel())) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if (qs.contains("contentNode")) {
                    contentNodes.add(qs.get("contentNode").asResource());
                }
            }
            return contentNodes;
        }
    }

    /**
     * get all content nodes of a specified type
     *
     * @param type specifies which content nodes to return (IS, SEEKS, ALL, ...)
     * @return content nodes
     */
    public Collection<Resource> getSeeksNodes() {
        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = "{ ?atomNode a won:Atom. ?atomNode match:seeks ?contentNode. FILTER NOT EXISTS { ?atomNode match:seeks/match:seeks ?contentNode. } }";
        String queryString = "prefix won: <https://w3id.org/won/core#> \n"
                        + "prefix match: <https://w3id.org/won/matching#> \n"
                        + "prefix con: <https://w3id.org/won/content#> \n"
                        + "SELECT DISTINCT ?contentNode WHERE { \n"
                        + queryClause + "\n }";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getAtomModel())) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if (qs.contains("contentNode")) {
                    contentNodes.add(qs.get("contentNode").asResource());
                }
            }
            return contentNodes;
        }
    }

    /**
     * get all seeks, seeks_seeks node including the resource for the atomcontent
     * itself (former is-node)
     *
     * @return content nodes
     */
    public Collection<Resource> getAllContentNodes() {
        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = null;
        String seeksClause = "{ ?atomNode a won:Atom. ?atomNode match:seeks ?contentNode. FILTER NOT EXISTS { ?atomNode match:seeks/match:seeks ?contentNode. } }";
        String seeksSeeksClause = "{ ?atomNode a won:Atom. ?atomNode match:seeks/match:seeks ?contentNode. }";
        queryClause = seeksClause + "UNION \n" + seeksSeeksClause;
        String queryString = "prefix won: <https://w3id.org/won/core#> \n"
                        + "prefix match: <https://w3id.org/won/matching#> \n"
                        + "prefix con: <https://w3id.org/won/content#> \n"
                        + "SELECT DISTINCT ?contentNode WHERE { \n"
                        + queryClause + "\n }";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getAtomModel())) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if (qs.contains("contentNode")) {
                    contentNodes.add(qs.get("contentNode").asResource());
                }
            }
            contentNodes.add(getAtomContentNode());
            return contentNodes;
        }
    }

    /**
     * get all content nodes of a specified type
     *
     * @param type specifies which content nodes to return (IS, SEEKS, ALL, ...)
     * @return content nodes
     */
    public Collection<Resource> getSeeksSeeksNodes() {
        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = null;
        String seeksSeeksClause = "{ ?atomNode a won:Atom. ?atomNode match:seeks/match:seeks ?contentNode. }";
        queryClause = seeksSeeksClause;
        String queryString = "prefix won: <https://w3id.org/won/core#> \n"
                        + "prefix match: <https://w3id.org/won/matching#> \n"
                        + "prefix con: <https://w3id.org/won/content#> \n"
                        + "SELECT DISTINCT ?contentNode WHERE { \n"
                        + queryClause + "\n }";
        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getAtomModel())) {
            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if (qs.contains("contentNode")) {
                    contentNodes.add(qs.get("contentNode").asResource());
                }
            }
            return contentNodes;
        }
    }

    public void setContentPropertyStringValue(Property p, String value) {
        Resource node = getAtomContentNode();
        node.removeAll(p);
        node.addLiteral(p, value);
    }

    public void setSeeksPropertyStringValue(Property p, String value) {
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            node.removeAll(p);
            node.addLiteral(p, value);
        }
    }

    /**
     * Adds a property directly into the contentNode of the atom
     * 
     * @param p
     * @param value
     */
    public void addPropertyStringValue(Property p, String value) {
        Resource node = getAtomContentNode();
        node.addLiteral(p, value);
    }

    public void addSeeksPropertyStringValue(Property p, String value) {
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            node.addLiteral(p, value);
        }
    }

    public String getContentPropertyStringValue(Resource contentNode, Property p) {
        RDFNode node = RdfUtils.findOnePropertyFromResource(getAtomModel(), contentNode, p);
        if (node != null && node.isLiteral()) {
            return node.asLiteral().getString();
        }
        return null;
    }

    public String getContentPropertyStringValue(Property p) {
        return getContentPropertyObject(p).asLiteral().getString();
    }

    public String getContentPropertyStringValue(String propertyPath) {
        Node node = getContentPropertyObject(propertyPath);
        return node != null ? node.getLiteralLexicalForm() : null;
    }

    public Collection<String> getContentPropertyStringValues(Resource contentNode, Property p, String language) {
        Collection<String> values = new LinkedList<>();
        NodeIterator nodeIterator = getAtomModel().listObjectsOfProperty(contentNode, p);
        while (nodeIterator.hasNext()) {
            Literal literalValue = nodeIterator.next().asLiteral();
            if (language == null || language.equals(literalValue.getLanguage())) {
                values.add(literalValue.getString());
            }
        }
        return values;
    }

    public Collection<RDFNode> getContentPropertyObjects(Resource contentNode, Property p) {
        Collection<RDFNode> values = new LinkedList<>();
        NodeIterator nodeIterator = getAtomModel().listObjectsOfProperty(contentNode, p);
        while (nodeIterator.hasNext()) {
            values.add(nodeIterator.next());
        }
        return values;
    }

    public Collection<String> getAllContentPropertyStringValues(Property p, String language) {
        Collection<String> values = new LinkedList<>();
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
            values.addAll(valuesOfContentNode);
        }
        Resource node = getAtomContentNode();
        Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
        values.addAll(valuesOfContentNode);
        return values;
    }

    public Collection<String> getContentPropertyStringValues(Property p, String language) {
        Resource node = getAtomContentNode();
        Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
        Collection<String> values = new LinkedList<>(valuesOfContentNode);
        return values;
    }

    public Collection<String> getSeeksPropertyStringValues(Property p) {
        return getSeeksPropertyStringValues(p, null);
    }

    public Collection<String> getSeeksPropertyStringValues(Property p, String language) {
        Collection<String> values = new LinkedList<>();
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
            values.addAll(valuesOfContentNode);
        }
        return values;
    }

    public Collection<RDFNode> getSeeksPropertyObjects(Property p) {
        Collection<RDFNode> values = new LinkedList<>();
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            Collection valuesOfContentNode = getContentPropertyObjects(node, p);
            values.addAll(valuesOfContentNode);
        }
        return values;
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred
     * languages will be tried first in the specified order.
     * 
     * @param contentNode
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Resource contentNode, Property p) {
        return getSomeContentPropertyStringValue(contentNode, p, null);
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred
     * languages will be tried first in the specified order.
     * 
     * @param contentNode
     * @param preferredLanguages String array of a non-empty language tag as defined
     * by https://tools.ietf.org/html/bcp47. The language tag must be well-formed
     * according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Resource contentNode, Property p, String... preferredLanguages) {
        Collection<String> values = null;
        if (preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                values = getContentPropertyStringValues(contentNode, p, preferredLanguages[i]);
                if (values != null && values.size() > 0)
                    return values.iterator().next();
            }
        }
        values = getContentPropertyStringValues(contentNode, p, null);
        if (values != null && values.size() > 0)
            return values.iterator().next();
        return null;
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred
     * languages will be tried first in the specified order.
     * 
     * @param preferredLanguages String array of a non-empty language tag as defined
     * by https://tools.ietf.org/html/bcp47. The language tag must be well-formed
     * according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Property p, String... preferredLanguages) {
        Resource contentNode = getAtomContentNode();
        if (preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                String valueOfContentNode = getSomeContentPropertyStringValue(contentNode, p, preferredLanguages[i]);
                if (valueOfContentNode != null)
                    return valueOfContentNode;
            }
        }
        String valueOfAtomContentNode = getSomeContentPropertyStringValue(contentNode, p);
        if (valueOfAtomContentNode != null)
            return valueOfAtomContentNode;
        Collection<Resource> nodes = getSeeksNodes();
        if (preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                for (Resource node : nodes) {
                    String valueOfContentNode = getSomeContentPropertyStringValue(node, p, preferredLanguages[i]);
                    if (valueOfContentNode != null)
                        return valueOfContentNode;
                }
            }
        }
        for (Resource node : nodes) {
            String valueOfContentNode = getSomeContentPropertyStringValue(node, p);
            if (valueOfContentNode != null)
                return valueOfContentNode;
        }
        return null;
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred
     * languages will be tried first in the specified order.
     * 
     * @param preferredLanguages String array of a non-empty language tag as defined
     * by https://tools.ietf.org/html/bcp47. The language tag must be well-formed
     * according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getAtomContentPropertyStringValue(Property p, String... preferredLanguages) {
        Resource node = getAtomContentNode();
        if (preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                String valueOfContentNode = getSomeContentPropertyStringValue(node, p, preferredLanguages[i]);
                if (valueOfContentNode != null)
                    return valueOfContentNode;
            }
        }
        String valueOfContentNode = getSomeContentPropertyStringValue(node, p);
        if (valueOfContentNode != null)
            return valueOfContentNode;
        return null;
    }

    private RDFNode getContentPropertyObject(Property p) {
        Resource node = getAtomContentNode();
        RDFNode object = null;
        NodeIterator nodeIterator = getAtomModel().listObjectsOfProperty(node, p);
        if (nodeIterator.hasNext()) {
            if (object != null) {
                throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(),
                                1, 2);
            }
            object = nodeIterator.next();
        }
        if (object == null) {
            throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1,
                            0);
        }
        return object;
    }

    public Collection<RDFNode> getContentPropertyObjects(Property p) {
        Resource node = getAtomContentNode();
        Collection valuesOfContentNode = getContentPropertyObjects(node, p);
        Collection<RDFNode> values = new LinkedList<>(valuesOfContentNode);
        return values;
    }

    private Node getContentPropertyObject(String propertyPath) {
        Path path = PathParser.parse(propertyPath, DefaultPrefixUtils.getDefaultPrefixes());
        Resource resource = getAtomContentNode();
        Node node = resource.asNode();
        return RdfUtils.getNodeForPropertyPath(getAtomModel(), node, path);
    }

    private boolean isSplittableNode(RDFNode node) {
        return node.isResource() && (node.isAnon() || (node.asResource().getURI().startsWith(getAtomUri())
                        && (!node.asResource().getURI().equals(getAtomUri()))));
    }

    private Resource copyNode(Resource node) {
        if (node.isAnon())
            return node.getModel().createResource();
        int i = 0;
        String uri = node.getURI() + RandomStringUtils.randomAlphanumeric(4);
        String newUri = uri + "_" + i;
        while (node.getModel().containsResource(new ResourceImpl(newUri))) {
            i++;
            newUri = uri + "_" + i;
        }
        return node.getModel().getResource(newUri);
    }

    /**
     * Returns a copy of the model in which no node reachable from the atom node has
     * multiple incoming edges (unless the graph contains a circle, see below). This
     * is achieved by making copies of all nodes that have multiple incoming edges,
     * such that each copy and the original get one of the incoming edges. The
     * outgoing edges of the original are replicated in the copies. Nodes that were
     * newly introduced by this algorithm are never split. In that special case that
     * the graph contains a circle, the resulting graph still contains a circle, and
     * possibly one or more nodes with more than one incoming edge.
     *
     * @return
     */
    public Model normalizeAtomModel() {
        Model copy = RdfUtils.cloneModel(getAtomModel());
        Set<RDFNode> blacklist = new HashSet<>();
        RDFNode atomNode = copy.getResource(getAtomUri());
        // System.out.println("model before modification:");
        // RDFDataMgr.write(System.out, copy, Lang.TRIG);
        recursiveCopyWhereMultipleInEdges(atomNode);
        // System.out.println("model after modifcation:");
        // RDFDataMgr.write(System.out, copy, Lang.TRIG);
        return copy;
    }

    private void recursiveCopyWhereMultipleInEdges(RDFNode node) {
        ModelModification modelModification = new ModelModification();
        recursiveCopyWhereMultipleInEdges(node, modelModification, new HashSet<>());
        modelModification.modify(node.getModel());
    }

    /**
     * If the specified node that has multiple incoming edges that have already been
     * visited (in depth-first order, i.e. on the way from the root to this node, if
     * this node is not the root), the node is 'split', i.e. one copy is made per
     * such incoming edge. No copies are made for incoming edges from nodes that are
     * discovered further down the tree. When a copy of the node is made, the
     * subgraph reachable from the node is copied as well. This process is done when
     * coming back from a depth-first recursion, i.e. smaller subgraphs are copied
     * before larger subgraphs.
     *
     * @param node
     * @param modelModification
     * @param visited
     */
    private void recursiveCopyWhereMultipleInEdges(RDFNode node, ModelModification modelModification,
                    Collection<RDFNode> visited) {
        // a non-resource is trivially ok
        if (!node.isResource())
            return;
        if (visited.contains(node))
            return;
        visited.add(node);
        List<Statement> outgoingEdges = node.getModel().listStatements(node.asResource(), null, (RDFNode) null)
                        .toList();
        for (Statement stmt : outgoingEdges) {
            recursiveCopyWhereMultipleInEdges(stmt.getObject(), modelModification, visited);
        }
        if (outgoingEdges.size() > 0) {
            Set<Resource> reachableFromNode = findReachableResources(node);
            List<Statement> incomingEdges = node.getModel().listStatements(null, null, node).toList();
            incomingEdges = incomingEdges.stream().filter(stmt -> !reachableFromNode.contains(stmt.getSubject()))
                            .collect(Collectors.toList());
            if (incomingEdges.size() > 1 && isSplittableNode(node)) {
                for (Statement stmt : incomingEdges) {
                    RDFNode copy = recursiveCopy(node, modelModification);
                    Statement newEdge = new StatementImpl(stmt.getSubject(), stmt.getPredicate(), copy);
                    modelModification.add(newEdge);
                    modelModification.remove(stmt);
                    // RDFDataMgr.write(System.out,
                    // modelModification.copyAndModify(node.getModel()), Lang.TRIG);
                }
                modelModification.remove(outgoingEdges);
            }
        }
    }

    private boolean isReachableFrom(RDFNode src, RDFNode target) {
        return isReachableFrom(src, target, new HashSet<>());
    }

    private boolean isReachableFrom(RDFNode src, RDFNode target, Collection<RDFNode> visited) {
        if (src.equals(target))
            return true;
        if (!src.isResource())
            return false;
        if (visited.contains(src))
            return false;
        visited.add(src);
        StmtIterator it = src.getModel().listStatements(src.asResource(), null, (RDFNode) null);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            if (isReachableFrom(src, stmt.getObject(), visited)) {
                return true;
            }
        }
        return false;
    }

    private Set<Resource> findReachableResources(RDFNode src) {
        Set<Resource> reachable = new HashSet<>();
        findReachableResources(src, reachable);
        return reachable;
    }

    private void findReachableResources(RDFNode src, Set<Resource> found) {
        if (!src.isResource())
            return;
        if (found.contains(src))
            return;
        found.add(src.asResource());
        StmtIterator it = src.getModel().listStatements(src.asResource(), null, (RDFNode) null);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            findReachableResources(src, found);
        }
    }

    private RDFNode recursiveCopy(RDFNode node, ModelModification modelModification) {
        return recursiveCopy(node, modelModification, null, null, new HashSet<>());
    }

    private RDFNode recursiveCopy(RDFNode node, ModelModification modelModification, RDFNode toReplace,
                    RDFNode replacement, Collection<RDFNode> visited) {
        if (node.equals(toReplace))
            return replacement;
        if (!node.isResource())
            return node;
        if (visited.contains(node))
            return copyNode(node.asResource());
        visited.add(node);
        RDFNode nodeInCopy;
        if (isSplittableNode(node)) {
            nodeInCopy = copyNode(node.asResource());
            visited.add(nodeInCopy);
        } else {
            return node;
        }
        if (toReplace == null && replacement == null) {
            toReplace = node;
            replacement = nodeInCopy;
        }
        List<Statement> outgoingEdges = node.getModel().listStatements(node.asResource(), null, (RDFNode) null)
                        .toList();
        for (Statement stmt : outgoingEdges) {
            RDFNode newObject = recursiveCopy(stmt.getObject(), modelModification, toReplace, replacement, visited);
            modelModification.add(new StatementImpl(nodeInCopy.asResource(), stmt.getPredicate(), newObject));
            modelModification.remove(stmt);
            // RDFDataMgr.write(System.out,
            // modelModification.copyAndModify(node.getModel()), Lang.TRIG);
        }
        return nodeInCopy;
    }

    private class ModelModification {
        private List<Statement> statementsToAdd;
        private List<Statement> statementsToRemove;

        public ModelModification() {
            this.statementsToAdd = new LinkedList<>();
            this.statementsToRemove = new LinkedList<>();
        }

        public Collection<Statement> getStatementsToAdd() {
            return Collections.unmodifiableCollection(statementsToAdd);
        }

        public Collection<Statement> getStatementsToRemove() {
            return Collections.unmodifiableCollection(statementsToRemove);
        }

        public void add(Statement stmt) {
            this.statementsToAdd.add(stmt);
        }

        public void add(Collection<Statement> statements) {
            this.statementsToAdd.addAll(statements);
        }

        public void remove(Statement stmt) {
            this.statementsToRemove.add(stmt);
        }

        public void remove(Collection<Statement> statements) {
            this.statementsToRemove.addAll(statements);
        }

        public void mergeModificationsFrom(ModelModification other) {
            this.statementsToRemove.addAll(other.statementsToRemove);
            this.statementsToAdd.addAll(other.statementsToAdd);
        }

        public Model copyAndModify(Model model) {
            Model ret = RdfUtils.cloneModel(model);
            modify(ret);
            return ret;
        }

        public void modify(Model model) {
            model.add(this.statementsToAdd);
            model.remove(this.statementsToRemove);
        }
    }
}
