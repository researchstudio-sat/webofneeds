package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.Coordinate;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.util.DefaultNeedModelWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BasicNeedQueryFactory extends NeedDatasetQueryFactory {

    private static final String NEED_TYPE_DUMMY_FIELD = "_graph._type";
    private static final String NEED_TYPE_DUMMY_FIELD_CONTENT = "http\\://purl.org/webofneeds/model#Need";

    public static final Map<NeedContentPropertyType, String> titleFieldMap;
    static
    {
        titleFieldMap = new HashMap<>();
        titleFieldMap.put(NeedContentPropertyType.IS,
                "_graph.http___purl.org_webofneeds_model_is.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(NeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(NeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
    }

    public static final Map<NeedContentPropertyType, String> descriptionFieldMap;
    static
    {
        descriptionFieldMap = new HashMap<>();
        descriptionFieldMap.put(NeedContentPropertyType.IS,
                "_graph.http___purl.org_webofneeds_model_is.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(NeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(NeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
    }

    public static final Map<NeedContentPropertyType, String> tagFieldMap;
    static
    {
        tagFieldMap = new HashMap<>();
        tagFieldMap.put(NeedContentPropertyType.IS,
                "_graph.http___purl.org_webofneeds_model_is.http___purl.org_webofneeds_model_hasTag");
        tagFieldMap.put(NeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_hasTag");
        tagFieldMap.put(NeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_hasTag");
    }

    public static final Map<NeedContentPropertyType, String> locationFieldMap;
    static
    {
        locationFieldMap = new HashMap<>();
        locationFieldMap.put(NeedContentPropertyType.IS, "is_need_location");
        locationFieldMap.put(NeedContentPropertyType.SEEKS, "seeks_need_location");
        locationFieldMap.put(NeedContentPropertyType.SEEKS_SEEKS, "seeksSeeks_need_location");
    }

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected ArrayList<SolrQueryFactory> contentFactories;
    protected ArrayList<SolrQueryFactory> locationFactories;
    protected DefaultNeedModelWrapper needModelWrapper;

    public BasicNeedQueryFactory(final Dataset need) {
        super(need);
        contentFactories = new ArrayList<>();
        locationFactories = new ArrayList<>();
        needModelWrapper = new DefaultNeedModelWrapper(need);
    }

    public void addTermsToTitleQuery(String terms, NeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = titleFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToDescriptionQuery(String terms, NeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = descriptionFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToTagQuery(String terms, NeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = tagFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addLocationFilters(Resource contentNode, NeedContentPropertyType fieldType) {

        Coordinate coordinate = needModelWrapper.getLocationCoordinate(contentNode);
        if (coordinate != null) {
            locationFactories.add(new GeoDistBoostQueryFactory(locationFieldMap.get(fieldType), coordinate.getLatitude(), coordinate.getLongitude()));
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

        // boost the query with a location distance factor
        // add up all the reverse query boost components and add 1 so that the multiplicative boost factor is at least 1
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

        // create a dummy query, this is the minimal part of a query that has no search term content
        // so that we at least create some valid query, in this case we just search for "all other needs"
        MatchFieldQueryFactory dummyQuery = new MatchFieldQueryFactory(NEED_TYPE_DUMMY_FIELD, NEED_TYPE_DUMMY_FIELD_CONTENT);
        contentFactories.add(dummyQuery);

        // combine all content term query parts with boolean OR operator
        SolrQueryFactory[] contentArray = new SolrQueryFactory[contentFactories.size()];
        BooleanQueryFactory contentQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                contentFactories.toArray(contentArray));

        return boostQueryString + contentQuery;
    }
}
