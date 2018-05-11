package won.matcher.sparql.query.factory;

import org.apache.jena.query.Dataset;

import won.matcher.sparql.query.factory.DefaultNeedQueryFactory;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class TestNeedQueryFactory extends DefaultNeedQueryFactory
{
    public TestNeedQueryFactory(Dataset need) {
        super(need);
    }
}

