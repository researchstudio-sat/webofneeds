package won.protocol.vocabulary.sparql;

/**
 * Created by fsuda on 05.03.2015.
 */
public class WonQueries {
    public static final String SPARQL_PREFIX = "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"
                    + "PREFIX geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#>"
                    + "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"
                    + "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                    + "PREFIX won:   <https://w3id.org/won/core#>" + "PREFIX gr:    <http://purl.org/goodrelations/v1#>"
                    + "PREFIX sioc:  <http://rdfs.org/sioc/ns#>" + "PREFIX ldp:   <http://www.w3.org/ns/ldp#>"
                    + "PREFIX dc:    <http://purl.org/dc/elements/1.1/>"
                    + "PREFIX msg:   <https://w3id.org/won/message#>";
    public static final String SPARQL_ALL_TRIPLES = SPARQL_PREFIX + "SELECT * WHERE { graph ?g {?s ?p ?o} . }";
    public static final String SPARQL_ALL_GRAPHS = SPARQL_PREFIX + "SELECT DISTINCT ?g WHERE {graph ?g {?s ?p ?o }.}";
    public static final String SPARQL_ALL_ATOMS = SPARQL_PREFIX + "SELECT * WHERE " + "{ ?atom won:content ?x; "
                    + "won:atomState ?state. " + "?x   dc:description ?desc; " + "won:tag ?tag; " + "dc:title ?title. "
                    + "?x won:contentDescription ?y. " + "OPTIONAL {" + "?y won:locationSpecification ?loc. "
                    + "?loc won:hasAddress ?address; " + "geo:latitude ?lat; " + "geo:longitude ?lng." + "} "
                    + "OPTIONAL {" + "?y won:timeSpecification ?time. " + "?time won:endTime ?endtime; "
                    + "won:startTime ?starttime; " + "won:recurInfiniteTimes ?recinf; " + "won:recursIn ?recin." + "}"
                    + "}";
    public static final String SPARQL_ATOMS_FILTERED_BY_URI = SPARQL_PREFIX + "SELECT * WHERE "
                    + "{ ?atom won:content ?x; " + "won:atomState ?state. " + "?x  dc:description ?desc; "
                    + "won:tag ?tag; " + "dc:title ?title. " + "?x won:contentDescription ?y. " + "OPTIONAL {"
                    + "?y won:locationSpecification ?loc. " + "?loc won:hasAddress ?address; " + "geo:latitude ?lat; "
                    + "geo:longitude ?lng." + "} " + "OPTIONAL {" + "?y won:timeSpecification ?time. "
                    + "?time won:endTime ?endtime; " + "won:startTime ?starttime; " + "won:recurInfiniteTimes ?recinf; "
                    + "won:recursIn ?recin." + "}" + "FILTER (?atom in (::atom::))" + "}";
    // TODO: PUT STATEMENTS HERE
    public static final String SPARQL_ATOMS_AND_CONNECTIONS = SPARQL_PREFIX + "SELECT ?atom ?connection ?atom2 ?state" + // TODO:
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
                    "WHERE {" + "?atom won:connections ?connections ." + "?connections rdfs:member ?connection ."
                    + "?connection won:targetAtom ?atom2;" + "won:connectionState ?state." +
                    // "?connection2 won:sourceAtom ?atom2 ." +
                    "}";
    public static final String SPARQL_CONNECTIONS_FILTERED_BY_ATOM_URI = SPARQL_PREFIX + "SELECT * WHERE " + "{ "
                    + "?atom won:connections ?connections. " + "?connections rdfs:member ?connection. "
                    + "?connection won:connectionState ?state; " + "won:targetAtom ?targetAtom; "
                    + "won:sourceAtom ?localAtom. " + "FILTER (?atom in (::atom::))" + "}";
    public static final String SPARQL_TEXTMESSAGES_BY_CONNECTION_ORDERED_BY_TIMESTAMP = SPARQL_PREFIX
                    + "SELECT * WHERE { graph ?g { ?s won:textMessage ?msg .} graph ?g2 { ?s msg:receivedTimestamp ?timestamp} graph ?g3 { ?s msg:senderAtom ?atomUri }} ORDER BY DESC(?timestamp)";
    public static final String SPARQL_ATOM2 = SPARQL_PREFIX + "SELECT * WHERE" + "{"
                    + "?connection won:messageContainer ?container. " + "?container rdfs:member ?event. "
                    + "?event won:textMessage ?text." + "}";
    // TODO: DO NOT USE THIS STATEMENT YET! RdfUtils.setSparqlVards DOES NOT PREVENT
    // INJECTION FOR NOW
    public static final String SPARQL_UPDATE_TITLE_OF_ATOM = SPARQL_PREFIX + "DELETE {" + "GRAPH ?graph {"
                    + "?content dc:title ?title." + "}" + "} " + "INSERT {" + "GRAPH ?graph {"
                    + "?content dc:title \"::title::\"" + "}" + "} " + "WHERE {" + "GRAPH ?graph {"
                    + "::atom:: won:content ?content. " + "?content dc:title ?title." + "}" + "}";
    public static final String SPARQL_UPDATE_STATE_FOR_ATOM = SPARQL_PREFIX + "DELETE {" + "GRAPH ?graph {"
                    + "::atom:: won:atomState ?state." + "}" + "} " + "INSERT {" + "GRAPH ?graph {"
                    + "::atom:: won:atomState ::state::" + "}" + "} " + "WHERE {" + "GRAPH ?graph{"
                    + "::atom:: won:atomState ?state." + "}" + "}";
    // public static final String SPARQL_MY_ATOM = "SELECT * WHERE {?atom
    // won:containedInPrivateGraph ?graph. ?atom won:content ?x; won:atomState
    // ?state. ?x dc:description ?desc; won:tag ?tag; dc:title ?title.}";
    // public static final String SPARQL_ATOMS_FILTERED_BY_UUIDS = "SELECT * WHERE {
    // ?atom won:content ?x; won:atomState ?state. ?x dc:description ?desc;
    // won:tag ?tag; dc:title ?title. filter (?atom in
    // (<http://rsa021.researchstudio.at:8080/won/resource/atom/5630666034445812000>))}";
    // queryString = WonQueries.SPARQL_PREFIX +
    // "SELECT ?atom ?connection ?atom2 WHERE {" +
    // //"graph ?g1 { ?atom won:connections ?connections .}" +
    // "?atom won:connections ?connections ." +
    // //"graph ?g2 { ?connections rdfs:member ?connection .}" +
    // "?connections rdfs:member ?connection ." +
    // //"graph ?g3 {?connection won:targetAtom ?atom2.}"+
    // "?connection won:targetAtom ?atom2."+
    // "}";
}
