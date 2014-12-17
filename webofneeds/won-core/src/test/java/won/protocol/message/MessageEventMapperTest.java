package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * User: ypanchenko
 * Date: 13.08.2014
 */
public class MessageEventMapperTest
{

//  private static final String RESOURCE_FILE =
//    "/need-lifecycle_with_message_02adj/01_create_need/01_OA_to_WN1.trig";
  private static final String RESOURCE_FILE =
    "/need-lifecycle_with_message_02adj/01_create_need/01_OA_to_WN1-without-sig.trig";

  private static final String MESSAGE_EVENT_GRAPH_URI = "http://www.example" +
    ".com/resource/need/randomNeedID_1/event/0#data";


  private Model createMessageEventTestModel() throws IOException {
    Dataset dataset = Utils.createTestDataset(RESOURCE_FILE);
    return dataset.getNamedModel(MESSAGE_EVENT_GRAPH_URI);
  }

  @Test
  @Ignore
  public void testMessageEventMappingRT() throws Exception {

    Model eventModel = createMessageEventTestModel();
    MessageEventMapper mapper = new MessageEventMapper();
    MessageEvent event = mapper.fromModel(eventModel);

    Assert.assertEquals("CreateMessage", event.getMessageType().getResource().getLocalName());
    Assert.assertEquals(2, event.getHasContent().size());
    //TODO implement signature support first
    //Assert.assertEquals(2, event.getSignatures().size());

    Model eventModelRT = mapper.toModel(event);

    Assert.assertTrue(eventModel.isIsomorphicWith(eventModelRT));
  }
}
