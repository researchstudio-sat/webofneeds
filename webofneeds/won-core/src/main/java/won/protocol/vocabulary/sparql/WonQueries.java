package won.protocol.vocabulary.sparql;

/**
 * Created by fsuda on 05.03.2015.
 */
public class WonQueries {
    public static final String SPARQL_PREFIX = "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"
                    + "PREFIX geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#>"
                    + "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"
                    + "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                    + "PREFIX won:   <http://purl.org/webofneeds/model#>"
                    + "PREFIX gr:    <http://purl.org/goodrelations/v1#>" + "PREFIX sioc:  <http://rdfs.org/sioc/ns#>"
                    + "PREFIX ldp:   <http://www.w3.org/ns/ldp#>" + "PREFIX dc:    <http://purl.org/dc/elements/1.1/>"
                    + "PREFIX msg:   <http://purl.org/webofneeds/message#>";
    public static final String SPARQL_ALL_TRIPLES = SPARQL_PREFIX + "SELECT * WHERE { graph ?g {?s ?p ?o} . }";
    public static final String SPARQL_ALL_GRAPHS = SPARQL_PREFIX + "SELECT DISTINCT ?g WHERE {graph ?g {?s ?p ?o }.}";
    public static final String SPARQL_ALL_NEEDS = SPARQL_PREFIX + "SELECT * WHERE " + "{ ?need won:hasContent ?x; "
                    + "won:isInState ?state. " + "?x   dc:description ?desc; " + "won:hasTag ?tag; "
                    + "dc:title ?title. " + "?x won:hasContentDescription ?y. " + "OPTIONAL {"
                    + "?y won:hasLocationSpecification ?loc. " + "?loc won:hasAddress ?address; "
                    + "geo:latitude ?lat; " + "geo:longitude ?lng." + "} " + "OPTIONAL {"
                    + "?y won:hasTimespecification ?time. " + "?time won:hasEndTime ?endtime; "
                    + "won:hasStartTime ?starttime; " + "won:hasRecurInfiniteTimes ?recinf; "
                    + "won:hasRecursIn ?recin." + "}" + "}";
    public static final String SPARQL_NEEDS_FILTERED_BY_URI = SPARQL_PREFIX + "SELECT * WHERE "
                    + "{ ?need won:hasContent ?x; " + "won:isInState ?state. " + "?x  dc:description ?desc; "
                    + "won:hasTag ?tag; " + "dc:title ?title. " + "?x won:hasContentDescription ?y. " + "OPTIONAL {"
                    + "?y won:hasLocationSpecification ?loc. " + "?loc won:hasAddress ?address; "
                    + "geo:latitude ?lat; " + "geo:longitude ?lng." + "} " + "OPTIONAL {"
                    + "?y won:hasTimespecification ?time. " + "?time won:hasEndTime ?endtime; "
                    + "won:hasStartTime ?starttime; " + "won:hasRecurInfiniteTimes ?recinf; "
                    + "won:hasRecursIn ?recin." + "}" + "FILTER (?need in (::need::))" + "}";
    // TODO: PUT STATEMENTS HERE
    public static final String SPARQL_NEEDS_AND_CONNECTIONS = SPARQL_PREFIX + "SELECT ?need ?connection ?need2 ?state" + // TODO:
                                                                                                                         // PLEASE
                                                                                                                         // RENAME
                                                                                                                         // THIS,
                                                                                                                         // THE
                                                                                                                         // NAME
                                                                                                                         // ISNT
                                                                                                                         // SAYING
                                                                                                                         // WHATS
                                                                                                                         // GOING
                                                                                                                         // ON
                    "WHERE {" + "?need won:hasConnections ?connections ." + "?connections rdfs:member ?connection ."
                    + "?connection won:hasRemoteNeed ?need2;" + "won:hasConnectionState ?state." +
                    // "?connection2 won:belongsToNeed ?need2 ." +
                    "}";
    public static final String SPARQL_CONNECTIONS_FILTERED_BY_NEED_URI = SPARQL_PREFIX + "SELECT * WHERE " + "{ "
                    + "?need won:hasConnections ?connections. " + "?connections rdfs:member ?connection. "
                    + "?connection won:hasConnectionState ?state; " + "won:hasRemoteNeed ?remoteNeed; "
                    + "won:belongsToNeed ?localNeed. " + "FILTER (?need in (::need::))" + "}";
    public static final String SPARQL_TEXTMESSAGES_BY_CONNECTION_ORDERED_BY_TIMESTAMP = SPARQL_PREFIX
                    + "SELECT * WHERE { graph ?g { ?s won:hasTextMessage ?msg .} graph ?g2 { ?s msg:hasReceivedTimestamp ?timestamp} graph ?g3 { ?s msg:hasSenderNeed ?needUri }} ORDER BY DESC(?timestamp)";
    public static final String SPARQL_NEED2 = SPARQL_PREFIX + "SELECT * WHERE" + "{"
                    + "?connection won:hasEventContainer ?container. " + "?container rdfs:member ?event. "
                    + "?event won:hasTextMessage ?text." + "}";
    // TODO: DO NOT USE THIS STATEMENT YET! RdfUtils.setSparqlVards DOES NOT PREVENT
    // INJECTION FOR NOW
    public static final String SPARQL_UPDATE_TITLE_OF_NEED = SPARQL_PREFIX + "DELETE {" + "GRAPH ?graph {"
                    + "?content dc:title ?title." + "}" + "} " + "INSERT {" + "GRAPH ?graph {"
                    + "?content dc:title \"::title::\"" + "}" + "} " + "WHERE {" + "GRAPH ?graph {"
                    + "::need:: won:hasContent ?content. " + "?content dc:title ?title." + "}" + "}";
    public static final String SPARQL_UPDATE_STATE_FOR_NEED = SPARQL_PREFIX + "DELETE {" + "GRAPH ?graph {"
                    + "::need:: won:isInState ?state." + "}" + "} " + "INSERT {" + "GRAPH ?graph {"
                    + "::need:: won:isInState ::state::" + "}" + "} " + "WHERE {" + "GRAPH ?graph{"
                    + "::need:: won:isInState ?state." + "}" + "}";
    // public static final String SPARQL_MY_NEED = "SELECT * WHERE {?need
    // won:containedInPrivateGraph ?graph. ?need won:hasContent ?x; won:isInState
    // ?state. ?x dc:description ?desc; won:hasTag ?tag; dc:title ?title.}";
    // public static final String SPARQL_NEEDS_FILTERED_BY_UUIDS = "SELECT * WHERE {
    // ?need won:hasContent ?x; won:isInState ?state. ?x dc:description ?desc;
    // won:hasTag ?tag; dc:title ?title. filter (?need in
    // (<http://rsa021.researchstudio.at:8080/won/resource/need/5630666034445812000>))}";
    // queryString = WonQueries.SPARQL_PREFIX +
    // "SELECT ?need ?connection ?need2 WHERE {" +
    // //"graph ?g1 { ?need won:hasConnections ?connections .}" +
    // "?need won:hasConnections ?connections ." +
    // //"graph ?g2 { ?connections rdfs:member ?connection .}" +
    // "?connections rdfs:member ?connection ." +
    // //"graph ?g3 {?connection won:hasRemoteNeed ?need2.}"+
    // "?connection won:hasRemoteNeed ?need2."+
    // "}";
}
