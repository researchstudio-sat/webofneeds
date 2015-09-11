package siren_matcher;

import com.sindicetech.siren.qparser.tree.dsl.ConciseQueryBuilder;
import com.sindicetech.siren.qparser.tree.dsl.ConciseTwigQuery;
import com.sindicetech.siren.qparser.tree.dsl.NodeQuery;
import com.sindicetech.siren.qparser.tree.dsl.TwigQuery;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/** This is an implementaion of SIREnQueryBuilderInterface that only uses a "need title" for making a SIREn matching
 * @author soheil
 * @date on 11.08.2015.
 */
public class SIREnTitleBasedQueryBuilder  implements SIREnQueryBuilderInterface {



    public String sIRENQueryBuilder(NeedObject needObject) throws QueryNodeException, IOException {


        //Since use the concise model to be able to also query over attributes
        ConciseQueryBuilder build = new ConciseQueryBuilder();
        ConciseTwigQuery topTwig = build.newTwig("@graph");

        TwigQuery twigBasicNeedType = null;
        //First of all, we have to cinsider the BasicNeedType
        switch (needObject.getBasicNeedType().toLowerCase()) { //Attention: lower-case
            case "http://purl.org/webofneeds/model#supply": // Demands has to be matched
                twigBasicNeedType = build.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType")
                        .with(build.newNode("'http://purl.org/webofneeds/model#demand'").setAttribute("@id"));
                break;
            case "http://purl.org/webofneeds/model#demand":
                twigBasicNeedType = build.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType")
                        .with(build.newNode("'http://purl.org/webofneeds/model#supply'").setAttribute("@id"));
                break;
            case "http://purl.org/webofneeds/model#dotogether":
                    twigBasicNeedType = build.newTwig("http://purl.org/webofneeds/model#hasBasicNeedType")
                            .with(build.newNode("'http://purl.org/webofneeds/model#dotogether'").setAttribute("@id"));
                break;
        }

        // processing the title and make some queries out of it

        QueryNLPProcessor qNLPP = new QueryNLPProcessor();


        String[] tokenizedTitlePhrase = qNLPP.extractRelevantWordTokens(needObject.getNeedTitle());

        ArrayList<TwigQuery> twigTitleArrayList = new ArrayList<TwigQuery>();

        for (int i=0; i<tokenizedTitlePhrase.length && i<Configuration.NUMBER_OF_CONSIDERED_TOKENS;i++) {
            twigTitleArrayList.add(build.newTwig("http://purl.org/webofneeds/model#hasContent")
                    .with(build.newNode(tokenizedTitlePhrase[i]).setAttribute("http://purl.org/dc/elements/1.1/title")));
        }

        if(twigBasicNeedType!=null)
            topTwig.with(twigBasicNeedType);
        for(int j=0; j<twigTitleArrayList.size();j++) {
            topTwig.optional(twigTitleArrayList.get(j));
        }

        return topTwig.toString();

    }

}
