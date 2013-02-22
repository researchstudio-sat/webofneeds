package won.matcher.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import java.io.Reader;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 21.02.13
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
public class TextDescriptionMatcher {

    public void executeQuery() {
       /* IndexReader ir =
        IndexSearcher is =

        MoreLikeThis mlt = new MoreLikeThis(ir);
        Reader target = ... // orig source of doc you want to find similarities to
        Query query = mlt.like( target);

        is.search(query);                   */
        // now the usual iteration thru 'hits' - the only thing to watch for is to make sure
        //you ignore the doc if it matches your 'target' document, as it should be similar to itself
    }
}
