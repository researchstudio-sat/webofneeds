package siren.matcher;

import com.sindicetech.siren.qparser.tree.dsl.ConciseQueryBuilder;
import com.sindicetech.siren.qparser.tree.dsl.ConciseTwigQuery;
import com.sindicetech.siren.qparser.tree.dsl.TwigQuery;
import config.SirenMatcherConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This is an implementaion of SIREnQueryBuilderInterface that only uses a "need description" for making a SIREn matching
 *
 * @author soheilk
 * @date on 11.08.2015.
 */
@Component
public class SIREnTagBasedQueryBuilder implements SIREnQueryBuilderInterface {

    @Autowired
    private SirenMatcherConfig config;

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

        // processing the description and make some queries out of it

        QueryNLPProcessor qNLPP = new QueryNLPProcessor();

        String[] tokenizedDescriptionPhrase = qNLPP.extractRelevantWordTokens(needObject.getNeedDescription());

        ArrayList<TwigQuery> twigDescriptionArrayList = new ArrayList<TwigQuery>();

        for (int i = 0; i < tokenizedDescriptionPhrase.length && i < config.getConsideredQueryTokens(); i++) {
            twigDescriptionArrayList.add(build.newTwig("http://purl.org/webofneeds/model#hasContent")
                    .with(build.newNode(tokenizedDescriptionPhrase[i]).setAttribute("http://purl.org/webofneeds/model#hasTag")));
        }

        if (twigBasicNeedType != null)
            topTwig.with(twigBasicNeedType);
        for (int j = 0; j < twigDescriptionArrayList.size(); j++) {
            topTwig.optional(twigDescriptionArrayList.get(j));
        }

        return topTwig.toString();

    }

}
