package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 28.07.2016.
 */
public abstract class SolrQueryFactory {
    protected double boost = 1.0;

    public void setBoost(double boost) {
        this.boost = boost;
    }

    public String toString() {
        return createQuery();
    }

    abstract protected String makeQueryString();

    public String createQuery() {

        String queryString = makeQueryString();
        if (queryString == null) {
            return null;
        }

        if (boost != 1.0) {
            return "(" + queryString + ")^" + boost;
        }

        return queryString;
    }

}
