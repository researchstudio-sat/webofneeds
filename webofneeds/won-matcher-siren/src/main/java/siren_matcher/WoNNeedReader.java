package siren_matcher;

import java.util.logging.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.vocabulary.DC;
import org.apache.solr.client.solrj.SolrServer;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Created by soheilk on 25.08.2015.
 */
public class WoNNeedReader {

    private final static Logger LOGGER = Logger.getLogger(WoNNeedReader.class.getName());

    public NeedObject readWoNNeedFromDeserializedNeedDataset(Dataset dataset, SolrServer solrServer) {
        NeedObject needObject = new NeedObject();

        String title = RdfUtils.findOnePropertyFromResource(dataset, null, DC.title).asLiteral().toString();
        if (Configuration.ACTIVATE_DEBUGGING_LOGGS)
            LOGGER.info("Need title: " + title);
        needObject.setNeedTitle(title);

        String textDescription = RdfUtils.findOnePropertyFromResource(dataset, null, WON.HAS_TEXT_DESCRIPTION).asLiteral
                ().toString();
        if (Configuration.ACTIVATE_DEBUGGING_LOGGS)
            LOGGER.info("Need Description: " + textDescription);
        needObject.setNeedDescription(textDescription);


        String basicNeedType = RdfUtils.findOnePropertyFromResource(dataset, null, WON.HAS_BASIC_NEED_TYPE).asResource().toString();
        if (Configuration.ACTIVATE_DEBUGGING_LOGGS)
            LOGGER.info("Basic Need Type is: " + basicNeedType);
        needObject.setBasicNeedType(basicNeedType);

        String needResourceUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();
        if (Configuration.ACTIVATE_DEBUGGING_LOGGS)
            LOGGER.info("needResourceUri is: " + needResourceUri);
        needObject.setNeedResourceUri(needResourceUri);


        String needDataUri = WonRdfUtils.NeedUtils.getNeedURI(dataset).toString();
        if (Configuration.ACTIVATE_DEBUGGING_LOGGS)
            LOGGER.info("needDataUri is: " + needDataUri.replace("resource","data"));
        needObject.setNeedResourceUri(needDataUri.replace("resource","data"));


        NeedIndexer needIndexer = new NeedIndexer();
        needIndexer.indexer_jsonld_format(dataset);

        return needObject;
    }

}
