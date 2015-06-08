import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.vocabulary.DC;
import common.event.NeedEvent;
import common.service.HttpRequestService;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.StringWriter;

/**
 * User: hfriedrich
 * Date: 15.06.2015
 */
public class DatasetMain
{
  public static void main(String[] args) throws IOException {

    String needUri = "http://rsa021.researchstudio.at:8080/won/resource/need/6818246844947104000";
    HttpRequestService http = new HttpRequestService();
    Dataset dataset = http.requestDataset(needUri);
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, dataset, RDFFormat.TRIG.getLang());
    NeedEvent needEvent = new NeedEvent(needUri, "http://rsa021.researchstudio.at:8080/won/resource",
                                        NeedEvent.TYPE.CREATED, dataset);

    String title = RdfUtils.findOnePropertyFromResource(dataset, null, DC.title).asLiteral().toString();
    System.out.println(title);
  }


}
