package won.matcher.solr.query.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.matcher.solr.utils.MatcherAtomContentPropertyType;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BasicAtomQueryFactory extends AtomDatasetQueryFactory {
    public static final Map<MatcherAtomContentPropertyType, String> titleFieldMap;
    static {
        titleFieldMap = new HashMap<>();
        titleFieldMap.put(MatcherAtomContentPropertyType.IS, "_graph.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(MatcherAtomContentPropertyType.SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(MatcherAtomContentPropertyType.SEEKS_SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
    }
    public static final Map<MatcherAtomContentPropertyType, String> descriptionFieldMap;
    static {
        descriptionFieldMap = new HashMap<>();
        descriptionFieldMap.put(MatcherAtomContentPropertyType.IS,
                        "_graph.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(MatcherAtomContentPropertyType.SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(MatcherAtomContentPropertyType.SEEKS_SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
    }
    public static final Map<MatcherAtomContentPropertyType, String> tagFieldMap;
    static {
        tagFieldMap = new HashMap<>();
        tagFieldMap.put(MatcherAtomContentPropertyType.IS, "_graph.http___purl.org_webofneeds_model_tag");
        tagFieldMap.put(MatcherAtomContentPropertyType.SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_tag");
        tagFieldMap.put(MatcherAtomContentPropertyType.SEEKS_SEEKS,
                        "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_tag");
    }
    public static final Map<MatcherAtomContentPropertyType, String> locationFieldMap;
    static {
        locationFieldMap = new HashMap<>();
        locationFieldMap.put(MatcherAtomContentPropertyType.IS, "is_atom_location");
        locationFieldMap.put(MatcherAtomContentPropertyType.SEEKS, "seeks_atom_location");
        locationFieldMap.put(MatcherAtomContentPropertyType.SEEKS_SEEKS, "seeksSeeks_atom_location");
    }
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected ArrayList<SolrQueryFactory> contentFactories;
    protected ArrayList<SolrQueryFactory> locationFactories;
    protected DefaultAtomModelWrapper atomModelWrapper;

    public BasicAtomQueryFactory(final Dataset atom) {
        super(atom);
        contentFactories = new ArrayList<>();
        locationFactories = new ArrayList<>();
        atomModelWrapper = new DefaultAtomModelWrapper(atom);
    }

    public void addTermsToTitleQuery(String terms, MatcherAtomContentPropertyType fieldType, double boost) {
        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = titleFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToDescriptionQuery(String terms, MatcherAtomContentPropertyType fieldType, double boost) {
        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = descriptionFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToTagQuery(String terms, MatcherAtomContentPropertyType fieldType, double boost) {
        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = tagFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addLocationFilters(Resource contentNode, MatcherAtomContentPropertyType fieldType) {
        Coordinate coordinate = atomModelWrapper.getLocationCoordinate(contentNode);
        if (coordinate != null) {
            locationFactories.add(new GeoDistBoostQueryFactory(locationFieldMap.get(fieldType),
                            coordinate.getLatitude(), coordinate.getLongitude()));
        }
    }

    private String filterCharsAndKeyWords(String text) {
        if (text == null) {
            return null;
        }
        // filter all special characters and number
        text = text.replaceAll("[^A-Za-z ]", " ");
        text = text.replaceAll("[^A-Za-z ]", " ");
        text = text.replaceAll("NOT ", " ");
        text = text.replaceAll("AND ", " ");
        text = text.replaceAll("OR ", " ");
        text = text.replaceAll(" NOT", " ");
        text = text.replaceAll(" AND", " ");
        text = text.replaceAll(" OR", " ");
        text = text.replaceAll("\\s+", " ");
        return text;
    }

    @Override
    protected String makeQueryString() {
        // return null if there is no content to search for
        if (contentFactories.size() == 0) {
            return null;
        }
        // boost the query with a location distance factor
        // add up all the reverse query boost components and add 1 so that the
        // multiplicative boost factor is at least 1
        String boostQueryString = "";
        if (locationFactories.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("sum(1");
            for (SolrQueryFactory queryFactory : locationFactories) {
                sb.append(",").append(queryFactory.makeQueryString());
            }
            sb.append(")");
            MultiplicativeBoostQueryFactory boostQueryFactory = new MultiplicativeBoostQueryFactory(sb.toString());
            boostQueryString = boostQueryFactory.makeQueryString();
        }
        // combine all content term query parts with boolean OR operator
        SolrQueryFactory[] contentArray = new SolrQueryFactory[contentFactories.size()];
        BooleanQueryFactory contentQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                        contentFactories.toArray(contentArray));
        return boostQueryString + contentQuery;
    }
}
