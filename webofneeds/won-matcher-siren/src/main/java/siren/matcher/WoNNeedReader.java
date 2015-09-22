package siren.matcher;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.vocabulary.DC;
import org.apache.solr.client.solrj.SolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;


/**
 * Created by soheilk on 25.08.2015.
 */
public class WoNNeedReader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public NeedObject readWoNNeedFromDeserializedNeedDataset(Dataset dataset, SolrServer solrServer) {
        NeedObject needObject = new NeedObject();

        String title = RdfUtils.findOnePropertyFromResource(dataset, null, DC.title).asLiteral().toString();
        log.debug("Need title: " + title);
        needObject.setNeedTitle(title);

        String textDescription = RdfUtils.findOnePropertyFromResource(dataset, null, WON.HAS_TEXT_DESCRIPTION).asLiteral
          ().toString();
        log.debug("Need Description: " + textDescription);
        needObject.setNeedDescription(textDescription);

        String basicNeedType = RdfUtils.findOnePropertyFromResource(dataset, null, WON.HAS_BASIC_NEED_TYPE).asResource().toString();
        log.debug("Basic Need Type is: " + basicNeedType);
        needObject.setBasicNeedType(basicNeedType);

        String needResourceUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();
        log.debug("needResourceUri is: " + needResourceUri);
        needObject.setNeedResourceUri(needResourceUri);


        String needDataUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();
        log.debug("needDataUri is: " + needDataUri.replace("resource","data"));
        needObject.setNeedResourceUri(needDataUri.replace("resource", "data"));

        return needObject;
    }

}
