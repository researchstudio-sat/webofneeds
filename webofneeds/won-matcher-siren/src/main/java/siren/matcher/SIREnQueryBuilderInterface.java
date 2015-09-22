package siren.matcher;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;

/**
 * Created by soheilk on 11.08.2015.
 */
public interface SIREnQueryBuilderInterface {

    String sIRENQueryBuilder(NeedObject need) throws QueryNodeException, IOException;
}
