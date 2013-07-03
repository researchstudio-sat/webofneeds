package won.matcher.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similar.MoreLikeThis;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:13
 * To change this template use File | Settings | File Templates.
 */
public class TextMatcherQuery {
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";

    private MoreLikeThis mlt;
    private Query query;

    public TextMatcherQuery(IndexReader ir) {
        mlt = new MoreLikeThis(ir);
        mlt.setMinDocFreq(1);
        mlt.setMinTermFreq(1);
        mlt.setFieldNames(new String[]{FIELD_TITLE, FIELD_DESCRIPTION});
    }

    public Query getQuery(int docNum) throws IOException {
       return mlt.like(docNum);
    }
}
