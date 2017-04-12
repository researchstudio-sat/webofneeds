package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class BooleanQueryFactory extends SolrQueryFactory {
    private SolrQueryFactory[] factories;
    private BooleanOperator operator;

    public enum BooleanOperator {
        AND,
        OR
    }

    public BooleanQueryFactory(BooleanOperator op, SolrQueryFactory... factories) {
        operator = op;
        this.factories = factories;
    }

    @Override
    protected String makeQueryString() {

        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 0; i < factories.length; i++) {
            queryBuilder.append(" (");
            queryBuilder.append(factories[i].createQuery());
            queryBuilder.append(") ");
            if (i < factories.length - 1) {
                queryBuilder.append(operator.toString());
                queryBuilder.append("\n");
            }
        }

        return queryBuilder.toString();
    }
}
