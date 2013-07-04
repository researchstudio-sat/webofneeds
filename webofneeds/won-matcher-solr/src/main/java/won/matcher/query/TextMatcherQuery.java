package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import won.protocol.solr.SolrFields;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:13
 * To change this template use File | Settings | File Templates.
 */
public class TextMatcherQuery extends AbstractQuery {
    private String[] fields;

    private MoreLikeThis mlt;

    public TextMatcherQuery(BooleanClause.Occur occur, String[] fields) {
       super(occur);
       this.fields = fields;
    }

    public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument) throws IOException {
        mlt = new MoreLikeThis(indexSearcher.getReader());

        mlt.setMinDocFreq(1);
        mlt.setMinTermFreq(1);
        mlt.setFieldNames(fields);

        //TODO: Could be hacked by having the same id more than once in the database
        int docNum = indexSearcher.getFirstMatch(new Term(SolrFields.FIELD_URL,
                inputDocument.getField(SolrFields.FIELD_URL).getValue().toString()));

        Query query = mlt.like(docNum);

        return query;
    }
}
