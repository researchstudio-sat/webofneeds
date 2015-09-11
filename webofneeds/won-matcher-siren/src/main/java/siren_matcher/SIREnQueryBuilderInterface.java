package siren_matcher;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.solr.client.solrj.SolrQuery;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by soheilk on 11.08.2015.
 */
public interface SIREnQueryBuilderInterface {

    public String sIRENQueryBuilder(NeedObject need) throws QueryNodeException, IOException;

}
