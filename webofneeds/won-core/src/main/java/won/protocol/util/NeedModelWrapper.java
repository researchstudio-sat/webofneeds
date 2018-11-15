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
import won.protocol.model.NeedGraphType;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class wraps the need models (need and sysinfo graphs in a need dataset).
 * It provides abstraction for the need structure of is/seeks content nodes that are part of the need model.
 * It can be used to load and query an existing need dataset (or models).
 * Furthermore it can be used to create a need model by adding triples.
 * <p>
 * Created by hfriedrich on 16.03.2017.
 */
public class NeedModelWrapper {

    // holds all the need data with its different (default and named) models
    protected Dataset needDataset;

    private String sysInfoGraphName;
    private String needModelGraphName;

    /**
     * Create a new need model (incluing sysinfo)
     *
     * @param needUri need uri to create the need models for
     */
    public NeedModelWrapper(String needUri) {

        needDataset = DatasetFactory.createGeneral();
        Model needModel = ModelFactory.createDefaultModel();
        DefaultPrefixUtils.setDefaultPrefixes(needModel);
        needModel.createResource(needUri, WON.NEED);
        Model sysInfoModel = ModelFactory.createDefaultModel();
        DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
        sysInfoModel.createResource(needUri, WON.NEED);
        this.needModelGraphName = needUri + "#need";
        needDataset.addNamedModel(this.needModelGraphName, needModel);
    }

    /**
     * Load a need dataset and extract the need and sysinfo models from it
     *
     * @param ds need dataset to load
     */
    public NeedModelWrapper(Dataset ds) {
        this(ds, false);
    }

    /**
     * Load a need dataset and extract the need and sysinfo models from it
     *
     * @param ds need dataset to load
     * @param addDefaultGraphs if this is set to true a needModelGraph and a sysInfoGraph will be added to the dataset
     */
    public NeedModelWrapper(Dataset ds, boolean addDefaultGraphs) {

        needDataset = ds;
        Resource needNode = getNeedContentNode();
        needNode = (needNode != null) ? needNode : getNeedNode(NeedGraphType.SYSINFO);
        String needUri = (needNode != null) ? needNode.getURI() : null;

        if (addDefaultGraphs && needUri != null) {
            if (getNeedModel() == null) {
                Model needModel = ModelFactory.createDefaultModel();
                needModel.createResource(needUri, WON.NEED);
                DefaultPrefixUtils.setDefaultPrefixes(needModel);
                this.needModelGraphName = "dummy#need";
                needDataset.addNamedModel(this.needModelGraphName, needModel);
            }

            if (getSysInfoModel() == null) {
                Model sysInfoModel = ModelFactory.createDefaultModel();
                sysInfoModel.createResource(needUri, WON.NEED);
                DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
                this.sysInfoGraphName = "dummy#sysinfo";
                needDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            }
        }

    }

    /**
     * Load the need and sysinfo models, if one of these models is null then initialize the other one as default model
     *
     * @param needModel
     * @param sysInfoModel
     */
    public NeedModelWrapper(Model needModel, Model sysInfoModel) {

        needDataset = DatasetFactory.createGeneral();
        String needUri = null;

        if (sysInfoModel != null) {
            this.sysInfoGraphName = "dummy#sysinfo";
            needDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            needUri = getNeedNode(NeedGraphType.SYSINFO).getURI();
        }

        if (needModel != null) {
            this.needModelGraphName = "dummy#need";
            needDataset.addNamedModel(this.needModelGraphName, needModel);
            needUri = getNeedContentNode().getURI();
        }

        if (needUri != null) {
            if (sysInfoModel == null) {

                sysInfoModel = ModelFactory.createDefaultModel();
                DefaultPrefixUtils.setDefaultPrefixes(sysInfoModel);
                sysInfoModel.createResource(needUri, WON.NEED);
                this.sysInfoGraphName = "dummy#sysinfo";
                needDataset.addNamedModel(this.sysInfoGraphName, sysInfoModel);
            }

            if (needModel == null) {

                needModel = ModelFactory.createDefaultModel();
                DefaultPrefixUtils.setDefaultPrefixes(needModel);
                needModel.createResource(needUri, WON.NEED);
                this.needModelGraphName = "dummy#need";
                needDataset.addNamedModel(this.needModelGraphName, needModel);
            }
        }

    }

    /**
     * Indicates if the wrapped data looks like need data.
     * @return
     */
    public static boolean isANeed(Dataset ds){
        NeedModelWrapper wrapper = new NeedModelWrapper(ds, false);
        return wrapper.getNeedContentNode() != null && wrapper.getNeedNode(NeedGraphType.SYSINFO) != null;
    }

    public Model getNeedModel() {
        Iterator<String> modelNameIter = needDataset.listNames();

        if(this.needModelGraphName != null && needDataset.getNamedModel(this.needModelGraphName) != null) {
            return needDataset.getNamedModel(this.needModelGraphName);
        }

        Model defaultModel = needDataset.getDefaultModel();
        if(defaultModel.listSubjectsWithProperty(RDF.type, WON.NEED).hasNext() && ! defaultModel.listSubjectsWithProperty(WON.IS_IN_STATE).hasNext()) {
            return defaultModel;
        }

        while(modelNameIter.hasNext()) {
            String tempModelName = modelNameIter.next();
            Model model = needDataset.getNamedModel(tempModelName);
            if (tempModelName.equals("dummy#sysinfo")) {
                continue;  
            }
            
            if(model.listSubjectsWithProperty(RDF.type, WON.NEED).hasNext() && ! model.listSubjectsWithProperty(WON.IS_IN_STATE).hasNext()) {
                this.needModelGraphName = tempModelName;
                return model;
            }
        }
        return null;
    }

    public Model getSysInfoModel() {
        Iterator<String> modelNameIter = needDataset.listNames();
        if(this.sysInfoGraphName != null && needDataset.getNamedModel(this.sysInfoGraphName) != null) {
            return needDataset.getNamedModel(this.sysInfoGraphName);
        }

        Model defaultModel = needDataset.getDefaultModel();
        if(defaultModel.listSubjectsWithProperty(RDF.type, WON.NEED).hasNext() && defaultModel.listSubjectsWithProperty(WON.IS_IN_STATE).hasNext()){
            return defaultModel;
        }

        while(modelNameIter.hasNext()) {
            String tempModelName = modelNameIter.next();
            if (tempModelName.equals("dummy#need")) {
                continue;  
            }
            Model model = needDataset.getNamedModel(tempModelName);

            if(model.listSubjectsWithProperty(RDF.type, WON.NEED).hasNext() && model.listSubjectsWithProperty(WON.IS_IN_STATE).hasNext()){
                this.sysInfoGraphName = tempModelName;
                return model;
            }
        }
        return null;
    }
    
    public boolean hasDerivedModel() {
        Optional<RDFNode> derivedGraphName = RdfUtils.findFirstPropertyOfO(getNeedNode(NeedGraphType.SYSINFO), WON.HAS_DERIVED_GRAPH);
        if (derivedGraphName.isPresent() && derivedGraphName.get().isURIResource()) {
            String name = derivedGraphName.get().asResource().getURI();
            if (this.needDataset.containsNamedModel(name)) {
                return true;
            }
        }
        return false;
    }
    
    public Optional<Model> getDerivedModel() {
         Optional<RDFNode> derivedGraphName = RdfUtils.findFirstPropertyOfO(getNeedNode(NeedGraphType.SYSINFO), WON.HAS_DERIVED_GRAPH);
         if (derivedGraphName.isPresent() && derivedGraphName.get().isURIResource()) {
             String name = derivedGraphName.get().asResource().getURI();
             if (this.needDataset.containsNamedModel(name)) {
                 return Optional.of(RdfUtils.cloneModel(this.needDataset.getNamedModel(name)));
             }
         }
         return Optional.empty();
    }

    /**
     * get the need or sysinfo model
     *
     * @param graph type specifies the need or sysinfo model to return
     * @return need or sysinfo model
     */
    public Model copyNeedModel(NeedGraphType graph) {
        if (graph.equals(NeedGraphType.NEED)) {
            return RdfUtils.cloneModel(getNeedModel());
        } else {
            return RdfUtils.cloneModel(getSysInfoModel());
        }
    }

    /**
     * get the complete dataset
     *
     * @return copy of needDataset
     */
    public Dataset copyDataset(){
        return RdfUtils.cloneDataset(needDataset);
    }

    /**
     * get the node of the need of either the need model or the sysinfo model
     *
     * @param graph type specifies the need or sysinfo need node to return
     * @return need or sysinfo need node
     */
    protected Resource getNeedNode(NeedGraphType graph) {
        if (graph.equals(NeedGraphType.NEED) && getNeedModel() != null) {
            ResIterator iter = getNeedModel().listSubjectsWithProperty(RDF.type, WON.NEED);
            if (iter.hasNext()) {
                return iter.next();
            }
        } else if (graph.equals(NeedGraphType.SYSINFO) && getSysInfoModel() != null) {
            ResIterator iter = getSysInfoModel().listSubjectsWithProperty(RDF.type, WON.NEED);
            if (iter.hasNext()) {
                return iter.next();
            }
        }

        return null;
    }

    /**
     * get the content node of the need
     * @return
     */
    public Resource getNeedContentNode() {
        return getNeedNode(NeedGraphType.NEED);
    }

    public String getNeedUri() {
        return getNeedContentNode().getURI();
    }

    public void addFlag(Resource flag) {
        getNeedContentNode().addProperty(WON.HAS_FLAG, flag);
    }

    public boolean hasFlag(Resource flag) {
        Resource needRes = getNeedContentNode();
        return needRes != null && needRes.hasProperty(WON.HAS_FLAG, flag);
    }

    public Calendar getDoNotMatchBefore() {
        Statement prop = getNeedContentNode().getProperty(WON.DO_NOT_MATCH_BEFORE);
        if(prop == null) {
            return null;
        }
        RDFNode literal = prop.getObject();
        if(!literal.isLiteral()) {
            return null; //This silently fails with a null, but in our case I think this is preferred to throwing an exception
        }
        Object data = literal.asLiteral().getValue();
        if(data instanceof XSDDateTime) {
            return ((XSDDateTime) data).asCalendar();
        } else {
            return null;
        }
    }

    public Calendar getDoNotMatchAfter() {
        Statement prop = getNeedContentNode().getProperty(WON.DO_NOT_MATCH_AFTER);
        if(prop == null) {
            return null;
        }
        RDFNode literal = prop.getObject();
        if(!literal.isLiteral()) {
            return null; //This silently fails with a null, but in our case I think this is preferred to throwing an exception
        }
        Object data = literal.asLiteral().getValue();
        if(data instanceof XSDDateTime) {
            return ((XSDDateTime) data).asCalendar();
        } else {
            return null;
        }
    }
    
    public void addMatchingContext(String context) {
        getNeedContentNode().addProperty(WON.HAS_MATCHING_CONTEXT, context);
    }
    
    public boolean hasMatchingContext(String context) {
        return getNeedContentNode().hasProperty(WON.HAS_MATCHING_CONTEXT, context);
    }
    
    public void addQuery(String query) {
        getNeedContentNode().addProperty(WON.HAS_QUERY, query);
    }
    
    public Optional<String> getQuery() {
        Statement stmt = getNeedContentNode().getProperty(WON.HAS_QUERY);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getString());
    }
    
    public boolean hasQuery() {
        return getNeedContentNode().hasProperty(WON.HAS_QUERY);
    }
    
    public Collection<String> getMatchingContexts() {
        Collection<String> matchingContexts = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedContentNode(), WON.HAS_MATCHING_CONTEXT);
        while (iter.hasNext()) {
            matchingContexts.add(iter.next().asLiteral().getString());
        }
        return matchingContexts;
    }

    /**
     * Add a facet. The facetURI must be a fragment URI off the need URI, i.e. [needuri]#facetid, or it is
     * just a fragment identifier, which will be interpreted relative to the needURI, i.e. #facetid -> [needuri]#facetid.
     * @param facetUri uniquely identifies this facet of this need
     * @param facetTypeUri the type of the facet, e.g. won:ChatFacet
     */
    public void addFacet(String facetUri, String facetTypeUri) {
        if (facetUri.startsWith("#")) {
            facetUri = getNeedUri()+facetUri;
        } else if (! facetUri.startsWith(getNeedUri())) {
            throw new IllegalArgumentException("The facetURI must start with '[needURI]#' or '#' but was: " +facetUri);
        }
        Resource facet = getNeedModel().getResource(facetUri);
        Resource facetType = getNeedModel().createResource(facetTypeUri);
        getNeedContentNode().addProperty(WON.HAS_FACET, facet);
        facet.addProperty(RDF.type, facetType);
    }
    
    public void setDefaultFacet(String facetUri) {
        Resource facet = getNeedModel().getResource(facetUri);
        getNeedContentNode().addProperty(WON.HAS_DEFAULT_FACET, facet);
    }
    
    public Optional<String> getDefaultFacet() {
        Statement stmt = getNeedContentNode().getProperty(WON.HAS_DEFAULT_FACET);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }
    
    

    public Collection<String> getFacetUris() {
        Collection<String> facetUris = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedContentNode(), WON.HAS_FACET);
        while (iter.hasNext()) {
            facetUris.add(iter.next().asResource().getURI());
        }
        return facetUris;
    }
    
    public Optional<String> getFacetType(String facetUri) {
        Resource facet = getNeedModel().createResource(facetUri);
        if(!getNeedContentNode().hasProperty(WON.HAS_FACET, facet)) {
            return Optional.empty(); 
        }
        Statement stmt = facet.getProperty(RDF.type);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }

    public Collection<Resource> getGoals() {

        Collection<Resource> goalUris = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedContentNode(), WON.GOAL);
        while (iter.hasNext()) {
            goalUris.add(iter.next().asResource());
        }
        return goalUris;
    }

    public Resource getGoal(String uri) {
        return getNeedModel().getResource(uri);
    }

    public Model getShapesGraph(Resource goalNode) {

        if (goalNode != null) {
            NodeIterator nodeIter = getNeedModel().listObjectsOfProperty(goalNode, WON.HAS_SHAPES_GRAPH);
            if (nodeIter.hasNext()) {
                String shapesGraphUri = nodeIter.next().asResource().getURI();
                return needDataset.getNamedModel(shapesGraphUri);
            }
        }

        return null;
    }
    
    public String getShapesGraphName(Resource goalNode) {

        if (goalNode != null) {
            NodeIterator nodeIter = getNeedModel().listObjectsOfProperty(goalNode, WON.HAS_SHAPES_GRAPH);
            if (nodeIter.hasNext()) {
                return nodeIter.next().asResource().getURI();
            }
        }

        return null;
    }

    public Model getDataGraph(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getNeedModel().listObjectsOfProperty(goalNode, WON.HAS_DATA_GRAPH);
            if (nodeIter.hasNext()) {
                String dataGraphUri = nodeIter.next().asResource().getURI();
                return needDataset.getNamedModel(dataGraphUri);
            }
        }

        return null;
    }
    
    
    
    public String getDataGraphName(Resource goalNode) {
        if (goalNode != null) {
            NodeIterator nodeIter = getNeedModel().listObjectsOfProperty(goalNode, WON.HAS_DATA_GRAPH);
            if (nodeIter.hasNext()) {
                return nodeIter.next().asResource().getURI();
            }
        }

        return null;
    }

    public void setNeedState(NeedState state) {

        Resource stateRes = NeedState.ACTIVE.equals(state) ? WON.NEED_STATE_ACTIVE : WON.NEED_STATE_INACTIVE;
        Resource need = getNeedNode(NeedGraphType.SYSINFO);
        need.removeAll(WON.IS_IN_STATE);
        need.addProperty(WON.IS_IN_STATE, stateRes);
    }

    public NeedState getNeedState() {
        Model sysInfoModel = getSysInfoModel();
        sysInfoModel.enterCriticalSection(true);
        RDFNode state = RdfUtils.findOnePropertyFromResource(sysInfoModel, getNeedNode(NeedGraphType.SYSINFO), WON.IS_IN_STATE);
        sysInfoModel.leaveCriticalSection();
        if (state.equals(WON.NEED_STATE_ACTIVE)) {
            return NeedState.ACTIVE;
        } else {
            return NeedState.INACTIVE;
        }
    }

    public ZonedDateTime getCreationDate() {

        String dateString = RdfUtils.findOnePropertyFromResource(
                getSysInfoModel(), getNeedNode(NeedGraphType.SYSINFO), DCTerms.created).asLiteral().getString();
        return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
    }

    public void setConnectionContainerUri(String containerUri) {
        Resource container = getSysInfoModel().createResource(containerUri);
        Resource need = getNeedNode(NeedGraphType.SYSINFO);
        need.removeAll(WON.HAS_CONNECTIONS);
        need.addProperty(WON.HAS_CONNECTIONS, container);
    }

    public String getConnectionContainerUri() {
        return RdfUtils.findOnePropertyFromResource(
                getSysInfoModel(), getNeedNode(NeedGraphType.SYSINFO), WON.HAS_CONNECTIONS).asResource().getURI();
    }

    public void setWonNodeUri(String nodeUri) {

        Resource node = getSysInfoModel().createResource(nodeUri);
        Resource need = getNeedNode(NeedGraphType.SYSINFO);
        need.removeAll(WON.HAS_WON_NODE);
        need.addProperty(WON.HAS_WON_NODE, node);
    }

    public String getWonNodeUri() {
        return RdfUtils.findOnePropertyFromResource(
                getSysInfoModel(), getNeedNode(NeedGraphType.SYSINFO), WON.HAS_WON_NODE).asResource().getURI();
    }

    /**
     * create a goal content node below the need node of the need model.
     * @param uri  uri of the content node, if null then create blank node
     * @return content node created
     */
    public Resource createGoalNode(String uri) {
        Resource contentNode = (uri != null) ? getNeedModel().createResource(uri) : getNeedModel().createResource();
        addGoalPropertyToNeedNode(contentNode);
        return contentNode;
    }

    /**
     * create a goal content node below the need node of the need model.
     * @param uri  uri of the content node, if null then create blank node
     * @return content node created
     */
    public Resource createSeeksNode(String uri) {
        Resource contentNode = (uri != null) ? getNeedModel().createResource(uri) : getNeedModel().createResource();
        addSeeksPropertyToNeedNode(contentNode);
        return contentNode;
    }

    private void addGoalPropertyToNeedNode(RDFNode contentNode) {
        Resource needNode = getNeedContentNode();
        needNode.addProperty(WON.GOAL, contentNode);
    }

    private void addSeeksPropertyToNeedNode(RDFNode contentNode) {
        Resource needNode = getNeedContentNode();
        needNode.addProperty(WON.SEEKS, contentNode);
    }

    public Collection<Resource> getGoalNodes() {
        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = "{ ?needNode a won:Need. ?needNode won:goal ?contentNode. }";

        String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "SELECT DISTINCT ?contentNode WHERE { \n" + queryClause + "\n }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getNeedModel())) {
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
        String queryClause = "{ ?needNode a won:Need. ?needNode won:seeks ?contentNode. FILTER NOT EXISTS { ?needNode won:seeks/won:seeks ?contentNode. } }";
        String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "SELECT DISTINCT ?contentNode WHERE { \n" + queryClause + "\n }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getNeedModel())) {
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
     * get all seeks, seeks_seeks node including the resource for the needcontent itself (former is-node)
     *
     * @return content nodes
     */
    public Collection<Resource> getAllContentNodes() {

        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = null;
        String seeksClause = "{ ?needNode a won:Need. ?needNode won:seeks ?contentNode. FILTER NOT EXISTS { ?needNode won:seeks/won:seeks ?contentNode. } }";
        String seeksSeeksClause = "{ ?needNode a won:Need. ?needNode won:seeks/won:seeks ?contentNode. }";

        queryClause = seeksClause + "UNION \n" + seeksSeeksClause;

        String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "SELECT DISTINCT ?contentNode WHERE { \n" + queryClause + "\n }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getNeedModel())) {
            ResultSet rs = qexec.execSelect();

            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                if (qs.contains("contentNode")) {
                    contentNodes.add(qs.get("contentNode").asResource());
                }
            }

            contentNodes.add(getNeedContentNode());

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
        String seeksSeeksClause = "{ ?needNode a won:Need. ?needNode won:seeks/won:seeks ?contentNode. }";

        queryClause = seeksSeeksClause;

        String queryString = "prefix won: <http://purl.org/webofneeds/model#> \n" +
                "SELECT DISTINCT ?contentNode WHERE { \n" + queryClause + "\n }";

        Query query = QueryFactory.create(queryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, getNeedModel())) {
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

        Resource node = getNeedContentNode();

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
     * Adds a property directly into the contentNode of the need
     * @param p
     * @param value
     */
    public void addPropertyStringValue(Property p, String value) {
        Resource node = getNeedContentNode();
        node.addLiteral(p, value);
    }

    public void addSeeksPropertyStringValue(Property p, String value) {
        Collection<Resource> nodes = getSeeksNodes();
        for (Resource node : nodes) {
            node.addLiteral(p, value);
        }
    }

    public String getContentPropertyStringValue(Resource contentNode, Property p) {

        RDFNode node = RdfUtils.findOnePropertyFromResource(getNeedModel(), contentNode, p);
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
        NodeIterator nodeIterator = getNeedModel().listObjectsOfProperty(contentNode, p);
        while (nodeIterator.hasNext()) {
            Literal literalValue = nodeIterator.next().asLiteral();
            if (language == null || language.equals(literalValue.getLanguage())) {
                values.add(literalValue.getString());
            }
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

        Resource node = getNeedContentNode();
        Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
        values.addAll(valuesOfContentNode);

        return values;
    }

    public Collection<String> getContentPropertyStringValues(Property p, String language) {

        Collection<String> values = new LinkedList<>();

        Resource node = getNeedContentNode();
        Collection valuesOfContentNode = getContentPropertyStringValues(node, p, language);
        values.addAll(valuesOfContentNode);

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

    /**
     * Returns one of the possibly many specified values. The specified preferred languages will be tried first in the specified order.
     * @param contentNode
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Resource contentNode, Property p){
        return getSomeContentPropertyStringValue(contentNode, p, null);
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred languages will be tried first in the specified order.
     * @param contentNode
     * @param preferredLanguages String array of a non-empty language tag as defined by https://tools.ietf.org/html/bcp47. The language tag must be well-formed according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Resource contentNode, Property p, String... preferredLanguages){
        Collection<String> values = null;
        if(preferredLanguages != null){
            for (int i = 0; i < preferredLanguages.length; i++){
                values = getContentPropertyStringValues(contentNode, p, preferredLanguages[i]);
                if (values != null && values.size() > 0) return values.iterator().next();
            }
        }
        values = getContentPropertyStringValues(contentNode, p, null);
        if (values != null && values.size() > 0) return values.iterator().next();
        return null;
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred languages will be tried first in the specified order.
     * @param preferredLanguages String array of a non-empty language tag as defined by https://tools.ietf.org/html/bcp47. The language tag must be well-formed according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getSomeContentPropertyStringValue(Property p, String... preferredLanguages){
        Resource contentNode = getNeedContentNode();
        if(preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                String valueOfContentNode = getSomeContentPropertyStringValue(contentNode, p, preferredLanguages[i]);
                if (valueOfContentNode != null) return valueOfContentNode;
            }
        }
        String valueOfNeedContentNode = getSomeContentPropertyStringValue(contentNode, p);
        if (valueOfNeedContentNode != null) return valueOfNeedContentNode;

        Collection<Resource> nodes = getSeeksNodes();
        if(preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                for (Resource node : nodes) {
                    String valueOfContentNode = getSomeContentPropertyStringValue(node, p, preferredLanguages[i]);
                    if (valueOfContentNode != null) return valueOfContentNode;
                }
            }
        }
        for (Resource node : nodes) {
            String valueOfContentNode = getSomeContentPropertyStringValue(node, p);
            if (valueOfContentNode != null) return valueOfContentNode;
        }
        return null;
    }

    /**
     * Returns one of the possibly many specified values. The specified preferred languages will be tried first in the specified order.
     * @param preferredLanguages String array of a non-empty language tag as defined by https://tools.ietf.org/html/bcp47. The language tag must be well-formed according to section 2.2.9 of https://tools.ietf.org/html/bcp47.
     * @return the string value or null if nothing is found
     */
    public String getNeedContentPropertyStringValue(Property p, String... preferredLanguages){
        Resource node = getNeedContentNode();
        if(preferredLanguages != null) {
            for (int i = 0; i < preferredLanguages.length; i++) {
                String valueOfContentNode = getSomeContentPropertyStringValue(node, p, preferredLanguages[i]);
                if (valueOfContentNode != null) return valueOfContentNode;
            }
        }
        String valueOfContentNode = getSomeContentPropertyStringValue(node, p);
        if (valueOfContentNode != null) return valueOfContentNode;
        return null;
    }


    private RDFNode getContentPropertyObject(Property p) {

        Resource node = getNeedContentNode();
        RDFNode object = null;

        NodeIterator nodeIterator = getNeedModel().listObjectsOfProperty(node, p);
        if (nodeIterator.hasNext()) {
            if (object != null) {
                throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 2);
            }
            object = nodeIterator.next();
        }

        if (object == null) {
            throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 0);
        }

        return object;
    }

    private Node getContentPropertyObject(String propertyPath) {

        Path path = PathParser.parse(propertyPath, DefaultPrefixUtils.getDefaultPrefixes());
        Resource resource = getNeedContentNode();

        Node node = resource.asNode();
        return RdfUtils.getNodeForPropertyPath(getNeedModel(), node, path);
    }

    private boolean isSplittableNode(RDFNode node) {
        return node.isResource() &&
                (node.isAnon() ||
                        (   node.asResource().getURI().startsWith(getNeedUri()) &&
                                (! node.asResource().getURI().equals(getNeedUri()))
                        ));
    }


    private Resource copyNode(Resource node) {
        if (node.isAnon()) return node.getModel().createResource();
        int i = 0;
        String uri = node.getURI() + RandomStringUtils.randomAlphanumeric(4);
        String newUri = uri+"_"+ i;
        while (node.getModel().containsResource(new ResourceImpl(newUri))){
            i++;
            newUri = uri+"_"+i;
        }
        return node.getModel().getResource(newUri);
    }

    /**
     * Returns a copy of the model in which no node reachable from the need node has multiple incoming edges
     * (unless the graph contains a circle, see below). This is achieved by making copies of all nodes that have multiple
     * incoming edges, such that each copy and the original get one of the incoming edges. The outgoing
     * edges of the original are replicated in the copies.
     *
     * Nodes that were newly introduced by this algorithm are never split.
     *
     * In that special case that the graph contains a circle, the resulting graph still contains a circle, and
     * possibly one or more nodes with more than one incoming edge.
     *
     * @return
     */
    public Model normalizeNeedModel() {
        Model copy = RdfUtils.cloneModel(getNeedModel());
        Set<RDFNode> blacklist = new HashSet<>();
        RDFNode needNode = copy.getResource(getNeedUri());
        //System.out.println("model before modification:");
        //RDFDataMgr.write(System.out, copy, Lang.TRIG);
        recursiveCopyWhereMultipleInEdges(needNode);
        //System.out.println("model after modifcation:");
        //RDFDataMgr.write(System.out, copy, Lang.TRIG);
        return copy;

    }

    private void recursiveCopyWhereMultipleInEdges(RDFNode node) {
        ModelModification modelModification = new ModelModification();
        recursiveCopyWhereMultipleInEdges(node, modelModification, new HashSet<>());
        modelModification.modify(node.getModel());
    }

    /**
     * If the specified node that has multiple incoming edges that have already been visited (in depth-first order, i.e.
     * on the way from the root to this node, if this node is not the root), the node is 'split', i.e. one copy is made
     * per such incoming edge. No copies are made for incoming edges from nodes that are discovered further down the tree.
     *
     * When a copy of the node is made, the subgraph reachable from the node is copied as well.
     *
     * This process is done when coming back from a depth-first recursion, i.e. smaller subgraphs are copied
     * before larger subgraphs.
     *
     * @param node
     * @param modelModification
     * @param visited
     */
    private void recursiveCopyWhereMultipleInEdges(RDFNode node, ModelModification modelModification, Collection<RDFNode> visited) {
        //a non-resource is trivially ok
        if (!node.isResource()) return;
        if (visited.contains(node)) return;
        visited.add(node);
        List<Statement> outgoingEdges = node.getModel().listStatements(node.asResource(), null, (RDFNode) null).toList();
        for(Statement stmt: outgoingEdges ){
            recursiveCopyWhereMultipleInEdges(stmt.getObject(), modelModification, visited);
        }

        if (outgoingEdges.size() > 0) {
            Set<Resource> reachableFromNode = findReachableResources(node);
            List<Statement> incomingEdges = node.getModel().listStatements(null, null, node).toList();
            incomingEdges = incomingEdges.stream().filter(stmt ->
                    ! reachableFromNode.contains(stmt.getSubject())).collect(Collectors.toList());
            if (incomingEdges.size() > 1 && isSplittableNode(node)) {
                for (Statement stmt : incomingEdges) {
                    RDFNode copy = recursiveCopy(node, modelModification);
                    Statement newEdge = new StatementImpl(stmt.getSubject(), stmt.getPredicate(), copy);
                    modelModification.add(newEdge);
                    modelModification.remove(stmt);
                    //RDFDataMgr.write(System.out, modelModification.copyAndModify(node.getModel()), Lang.TRIG);
                }
                modelModification.remove(outgoingEdges);
            }
        }
    }

    private boolean isReachableFrom(RDFNode src, RDFNode target){
        return isReachableFrom(src, target, new HashSet<>());
    }

    private boolean isReachableFrom(RDFNode src, RDFNode target, Collection<RDFNode> visited){
        if (src.equals(target)) return true;
        if (!src.isResource()) return false;
        if (visited.contains(src)) return false;
        visited.add(src);
        StmtIterator it = src.getModel().listStatements(src.asResource(), null, (RDFNode) null);
        while(it.hasNext()){
            Statement stmt = it.nextStatement();
            if (isReachableFrom(src, stmt.getObject(), visited)){
                return true;
            }
        }
        return false;
    }

    private Set<Resource> findReachableResources(RDFNode src){
        Set<Resource> reachable = new HashSet<>();
        findReachableResources(src, reachable);
        return reachable;
    }

    private void findReachableResources(RDFNode src, Set<Resource> found){
        if (!src.isResource()) return;
        if (found.contains(src)) return;
        found.add(src.asResource());
        StmtIterator it = src.getModel().listStatements(src.asResource(), null, (RDFNode) null);
        while(it.hasNext()){
            Statement stmt = it.nextStatement();
            findReachableResources(src, found);
        }
    }

    private RDFNode recursiveCopy(RDFNode node, ModelModification modelModification){
        return recursiveCopy(node, modelModification, null, null, new HashSet<>());
    }

    private RDFNode recursiveCopy(RDFNode node, ModelModification modelModification, RDFNode toReplace, RDFNode replacement, Collection<RDFNode> visited){
        if (node.equals(toReplace)) return replacement;
        if (!node.isResource()) return node;
        if (visited.contains(node)) return copyNode(node.asResource());
        visited.add(node);
        RDFNode nodeInCopy;
        if (isSplittableNode(node)) {
            nodeInCopy = copyNode(node.asResource());
            visited.add(nodeInCopy);
        } else {
            return node;
        }
        if (toReplace == null && replacement == null){
            toReplace = node;
            replacement = nodeInCopy;
        }
        List<Statement> outgoingEdges = node.getModel().listStatements(node.asResource(), null, (RDFNode) null).toList();
        for(Statement stmt: outgoingEdges ){
            RDFNode newObject = recursiveCopy(stmt.getObject(), modelModification, toReplace, replacement, visited);
            modelModification.add(new StatementImpl(nodeInCopy.asResource(), stmt.getPredicate(), newObject));
            modelModification.remove(stmt);
            //RDFDataMgr.write(System.out, modelModification.copyAndModify(node.getModel()), Lang.TRIG);
        }
        return nodeInCopy;
    }

    private class ModelModification{
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

        public void add (Statement stmt){
            this.statementsToAdd.add(stmt);
        }

        public void add(Collection<Statement> statements) {
            this.statementsToAdd.addAll(statements);
        }

        public void remove(Statement stmt){
            this.statementsToRemove.add(stmt);
        }

        public void remove(Collection<Statement> statements){
            this.statementsToRemove.addAll(statements);
        }

        public void mergeModificationsFrom(ModelModification other){
            this.statementsToRemove.addAll(other.statementsToRemove);
            this.statementsToAdd.addAll(other.statementsToAdd);
        }

        public Model copyAndModify(Model model) {
            Model ret = RdfUtils.cloneModel(model);
            modify(ret);
            return ret;
        }

        public void modify(Model model){
            model.add(this.statementsToAdd);
            model.remove(this.statementsToRemove);
        }
    }

}
