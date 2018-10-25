package won.protocol.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import won.protocol.exception.DataIntegrityException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.NeedContentPropertyType;
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
        Resource needNode = getNeedNode(NeedGraphType.NEED);
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
            needUri = getNeedNode(NeedGraphType.NEED).getURI();
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
        return wrapper.getNeedNode(NeedGraphType.NEED) != null && wrapper.getNeedNode(NeedGraphType.SYSINFO) != null;
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

    public String getNeedUri() {
        return getNeedNode(NeedGraphType.NEED).getURI();
    }

    public void addFlag(Resource flag) {
        getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_FLAG, flag);
    }

    public boolean hasFlag(Resource flag) {
    	Resource needRes = getNeedNode(NeedGraphType.NEED);
    	if (needRes == null) return false;
        return needRes.hasProperty(WON.HAS_FLAG, flag);
    }

    public Calendar getDoNotMatchBefore() {
        Statement prop = getNeedNode(NeedGraphType.NEED).getProperty(WON.DO_NOT_MATCH_BEFORE);
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
        Statement prop = getNeedNode(NeedGraphType.NEED).getProperty(WON.DO_NOT_MATCH_AFTER);
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
        getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_MATCHING_CONTEXT, context);
    }
    
    public boolean hasMatchingContext(String context) {
        return getNeedNode(NeedGraphType.NEED).hasProperty(WON.HAS_MATCHING_CONTEXT, context);
    }
    
    public void addQuery(String query) {
        getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_QUERY, query);
    }
    
    public Optional<String> getQuery() {
        Statement stmt = getNeedNode(NeedGraphType.NEED).getProperty(WON.HAS_QUERY);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getString());
    }
    
    public boolean hasQuery() {
        return getNeedNode(NeedGraphType.NEED).hasProperty(WON.HAS_QUERY);
    }
    
    public Collection<String> getMatchingContexts() {
        Collection<String> matchingContexts = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedNode(NeedGraphType.NEED), WON.HAS_MATCHING_CONTEXT);
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
        getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_FACET, facet);
        facet.addProperty(RDF.type, facetType);
    }
    
    public void setDefaultFacet(String facetUri) {
        Resource facet = getNeedModel().getResource(facetUri);
        getNeedNode(NeedGraphType.NEED).addProperty(WON.HAS_DEFAULT_FACET, facet);
    }
    
    public Optional<String> getDefaultFacet() {
        Statement stmt = getNeedNode(NeedGraphType.NEED).getProperty(WON.HAS_DEFAULT_FACET);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }
    
    

    public Collection<String> getFacetUris() {
        Collection<String> facetUris = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedNode(NeedGraphType.NEED), WON.HAS_FACET);
        while (iter.hasNext()) {
            facetUris.add(iter.next().asResource().getURI());
        }
        return facetUris;
    }
    
    public Optional<String> getFacetType(String facetUri) {
        Resource facet = getNeedModel().createResource(facetUri);
        if(! getNeedNode(NeedGraphType.NEED).hasProperty(WON.HAS_FACET, facet)) {
            return Optional.empty(); 
        }
        Statement stmt = facet.getProperty(RDF.type);
        if (stmt == null) return Optional.empty();
        return Optional.of(stmt.getObject().toString());
    }

    public Collection<Resource> getGoals() {

        Collection<Resource> goalUris = new LinkedList<>();
        NodeIterator iter = getNeedModel().listObjectsOfProperty(getNeedNode(NeedGraphType.NEED), WON.GOAL);
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
                String shapesGraphUri = nodeIter.next().asResource().getURI();
                return shapesGraphUri;
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
                String dataGraphUri = nodeIter.next().asResource().getURI();
                return dataGraphUri;
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
     * create a content node below the need node of the need model.
     *
     * @param type specifies which property (e.g. IS, SEEKS, ...) is used to connect the need node with the content node
     * @param uri  uri of the content node, if null then create blank node
     * @return content node created
     */
    public Resource createContentNode(NeedContentPropertyType type, String uri) {

        if (NeedContentPropertyType.ALL.equals(type)) {
            throw new IllegalArgumentException("NeedContentPropertyType.ALL not defined for this method");
        }

        Resource contentNode = (uri != null) ? getNeedModel().createResource(uri) : getNeedModel().createResource();
        addContentPropertyToNeedNode(type, contentNode);
        return contentNode;
    }

    private void addContentPropertyToNeedNode(NeedContentPropertyType type, RDFNode contentNode) {

        Resource needNode = getNeedNode(NeedGraphType.NEED);
        if (NeedContentPropertyType.IS.equals(type)) {
            needNode.addProperty(WON.IS, contentNode);
        } else if (NeedContentPropertyType.SEEKS.equals(type)) {
            needNode.addProperty(WON.SEEKS, contentNode);
        } else if (NeedContentPropertyType.SEEKS_SEEKS.equals(type)) {
            Resource intermediate = getNeedModel().createResource();
            needNode.addProperty(WON.SEEKS, intermediate);
            intermediate.addProperty(WON.SEEKS, contentNode);
        } else if (NeedContentPropertyType.IS_AND_SEEKS.equals(type)) {
            needNode.addProperty(WON.IS, contentNode);
            needNode.addProperty(WON.SEEKS, contentNode);
        } else if (NeedContentPropertyType.GOAL.equals(type)) {
            needNode.addProperty(WON.GOAL, contentNode);
        }
    }

    public NeedContentPropertyType getContentPropertyType(Resource contentNode) {

        boolean is = getContentNodes(NeedContentPropertyType.IS).size() > 0;
        boolean seeks = getContentNodes(NeedContentPropertyType.SEEKS).size() > 0;
        boolean seeksSeeks = getContentNodes(NeedContentPropertyType.SEEKS_SEEKS).size() > 0;

        if (is && seeks && seeksSeeks) {
            return NeedContentPropertyType.ALL;
        } else if (is && seeks) {
            return NeedContentPropertyType.IS_AND_SEEKS;
        } else if (is) {
            return NeedContentPropertyType.IS;
        } else if (seeks) {
            return NeedContentPropertyType.SEEKS;
        } else if (seeksSeeks) {
            return NeedContentPropertyType.SEEKS_SEEKS;
        }

        return null;
    }

    /**
     * get all content nodes of a specified type
     *
     * @param type specifies which content nodes to return (IS, SEEKS, ALL, ...)
     * @return content nodes
     */
    public Collection<Resource> getContentNodes(NeedContentPropertyType type) {

        Collection<Resource> contentNodes = new LinkedList<>();
        String queryClause = null;
        String isClause = "{ ?needNode a won:Need. ?needNode won:is ?contentNode. }";
        String isAndSeeksClause = "{ ?needNode a won:Need. ?needNode won:is ?contentNode. ?needNode won:seeks ?contentNode. }";
        String seeksClause = "{ ?needNode a won:Need. ?needNode won:seeks ?contentNode. FILTER NOT EXISTS { ?needNode won:seeks/won:seeks ?contentNode. } }";
        String seeksSeeksClause = "{ ?needNode a won:Need. ?needNode won:seeks/won:seeks ?contentNode. }";
        String goalClause = "{ ?needNode a won:Need. ?needNode won:goal ?contentNode. }";

        switch (type) {
            case IS:
                queryClause = isClause;
                break;
            case SEEKS:
                queryClause = seeksClause;
                break;
            case IS_AND_SEEKS:
                queryClause = isAndSeeksClause;
                break;
            case SEEKS_SEEKS:
                queryClause = seeksSeeksClause;
                break;
            case GOAL:
                queryClause = goalClause;
                break;
            case ALL:
                queryClause = isClause + "UNION \n" + seeksClause + "UNION \n" + seeksSeeksClause;
        }

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

    public void setContentPropertyStringValue(NeedContentPropertyType type, Property p, String value) {

        Collection<Resource> nodes = getContentNodes(type);
        for (Resource node : nodes) {
            node.removeAll(p);
            node.addLiteral(p, value);
        }
    }

    public void addContentPropertyStringValue(NeedContentPropertyType type, Property p, String value) {

        Collection<Resource> nodes = getContentNodes(type);
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

    public String getContentPropertyStringValue(NeedContentPropertyType type, Property p) {
        return getContentPropertyObject(type, p).asLiteral().getString();
    }

    public String getContentPropertyStringValue(NeedContentPropertyType type, String propertyPath) {
        Node node = getContentPropertyObject(type, propertyPath);
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

    public Collection<String> getContentPropertyStringValues(NeedContentPropertyType type, Property p, String language) {

        Collection<String> values = new LinkedList<>();
        Collection<Resource> nodes = getContentNodes(type);
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
    public String getSomeContentPropertyStringValue(NeedContentPropertyType type, Property p, String... preferredLanguages){
        Collection<Resource> nodes = getContentNodes(type);
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


    private RDFNode getContentPropertyObject(NeedContentPropertyType type, Property p) {

        Collection<Resource> nodes = getContentNodes(type);
        RDFNode object = null;
        for (Resource node : nodes) {
            NodeIterator nodeIterator = getNeedModel().listObjectsOfProperty(node, p);
            if (nodeIterator.hasNext()) {
                if (object != null) {
                    throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 2);
                }
                object = nodeIterator.next();
            }
        }

        if (object == null) {
            throw new IncorrectPropertyCountException("expected exactly one occurrence of property " + p.getURI(), 1, 0);
        }

        return object;
    }

    private Node getContentPropertyObject(NeedContentPropertyType type, String propertyPath) {

        Path path = PathParser.parse(propertyPath, DefaultPrefixUtils.getDefaultPrefixes());
        Collection<Resource> nodes = getContentNodes(type);

        if (nodes.size() != 1) {
            throw new IncorrectPropertyCountException("expected exactly one occurrence of object for property path " +
                    propertyPath, 1, nodes.size());
        }

        Node node = nodes.iterator().next().asNode();
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
        RDFNode needNode = copy.getResource(getNeedUri().toString());
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
        return recursiveCopy(node, modelModification, null,null, new HashSet<>());
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
