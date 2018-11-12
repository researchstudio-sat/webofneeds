package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.solr.utils.MatcherNeedContentPropertyType;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultNeedModelWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BasicNeedQueryFactory extends NeedDatasetQueryFactory {

    public static final Map<MatcherNeedContentPropertyType, String> titleFieldMap;
    static
    {
        titleFieldMap = new HashMap<>();
        titleFieldMap.put(MatcherNeedContentPropertyType.IS,
                "_graph.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(MatcherNeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
        titleFieldMap.put(MatcherNeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_title");
    }

    public static final Map<MatcherNeedContentPropertyType, String> descriptionFieldMap;
    static
    {
        descriptionFieldMap = new HashMap<>();
        descriptionFieldMap.put(MatcherNeedContentPropertyType.IS,
                "_graph.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(MatcherNeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
        descriptionFieldMap.put(MatcherNeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_dc_elements_1.1_description");
    }

    public static final Map<MatcherNeedContentPropertyType, String> tagFieldMap;
    static
    {
        tagFieldMap = new HashMap<>();
        tagFieldMap.put(MatcherNeedContentPropertyType.IS,
                "_graph.http___purl.org_webofneeds_model_hasTag");
        tagFieldMap.put(MatcherNeedContentPropertyType.SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_hasTag");
        tagFieldMap.put(MatcherNeedContentPropertyType.SEEKS_SEEKS,
                "_graph.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_seeks.http___purl.org_webofneeds_model_hasTag");
    }

    public static final Map<MatcherNeedContentPropertyType, String> locationFieldMap;
    static
    {
        locationFieldMap = new HashMap<>();
        locationFieldMap.put(MatcherNeedContentPropertyType.IS, "is_need_location");
        locationFieldMap.put(MatcherNeedContentPropertyType.SEEKS, "seeks_need_location");
        locationFieldMap.put(MatcherNeedContentPropertyType.SEEKS_SEEKS, "seeksSeeks_need_location");
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

    public void addTermsToTitleQuery(String terms, MatcherNeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = titleFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToDescriptionQuery(String terms, MatcherNeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = descriptionFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addTermsToTagQuery(String terms, MatcherNeedContentPropertyType fieldType, double boost) {

        terms = filterCharsAndKeyWords(terms);
        if (terms != null && !terms.trim().isEmpty()) {
            String field = tagFieldMap.get(fieldType);
            SolrQueryFactory qf = new MatchFieldQueryFactory(field, terms);
            qf.setBoost(boost);
            contentFactories.add(qf);
        }
    }

    public void addLocationFilters(Resource contentNode, MatcherNeedContentPropertyType fieldType) {

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

        // return null if there is no content to search for
        if (contentFactories.size() == 0) {
            return null;
        }

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

        // combine all content term query parts with boolean OR operator
        SolrQueryFactory[] contentArray = new SolrQueryFactory[contentFactories.size()];
        BooleanQueryFactory contentQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                contentFactories.toArray(contentArray));

        return boostQueryString + contentQuery;
    }
}
