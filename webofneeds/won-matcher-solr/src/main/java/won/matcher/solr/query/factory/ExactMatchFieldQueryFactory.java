package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class ExactMatchFieldQueryFactory extends SolrQueryFactory {
    protected String fieldName;
    protected String value;

    public ExactMatchFieldQueryFactory(String fieldName, String value) {
        this.value = value;
        this.fieldName = fieldName;
    }

    @Override
    protected String makeQueryString() {

        if (fieldName == null || value == null) {
            throw new NullPointerException("fieldName or value may not be null");
        }
        return String.join("", fieldName, " : \"", value, "\"");
    }
}
