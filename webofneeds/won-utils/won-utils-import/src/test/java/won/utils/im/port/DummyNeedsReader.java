package won.utils.im.port;

import org.apache.jena.rdf.model.Model;
import won.protocol.model.BasicNeedType;
import won.protocol.util.NeedModelBuilder;

import java.io.IOException;
import java.util.Calendar;

/**
 * User: ypanchenko
 * Date: 04.09.2014
 */
public class DummyNeedsReader implements NeedDataReader<Model>
{

  private int numberOfDummyNeeds;
  private int counter = 0;

  public DummyNeedsReader(int numberOfDummyNeeds) {
    this.numberOfDummyNeeds = numberOfDummyNeeds;
  }

  @Override
  public boolean hasNext() {
    return counter < numberOfDummyNeeds;
  }

  @Override
  public Model next() {
    return createDummyNeed(counter++);
  }

  @Override
  public void close() throws IOException {
    // this dummy class has nothing to close
  }


  private Model createDummyNeed(final int needId) {
    NeedModelBuilder needModelBuilder = new NeedModelBuilder();

    needModelBuilder.setUri("no:uri");
    needModelBuilder.setTitle("Looking for a Table");
    needModelBuilder.setDescription("Looking for a big dining table, ca. 200x140 size.");

    Calendar calender = Calendar.getInstance();
    needModelBuilder.setCreationDate(calender.getTime());
    calender.set(Calendar.MONTH, needId);
    needModelBuilder.addAvailableAfter(calender.getTime());
    needModelBuilder.setAvailableAtLocation((float) 48.2000, (float) 16.3667);
    needModelBuilder.setBasicNeedType(BasicNeedType.DEMAND);
    needModelBuilder.setCurrency("EUR");
    needModelBuilder.setPriceLimit((double) 100, (double) needId*100);

    // ??
    //needModelBuilder.setFacetTypes()
    //needModelBuilder.setNeedProtocolEndpoint()
    //needModelBuilder.setState()

    return needModelBuilder.build();

  }
}
