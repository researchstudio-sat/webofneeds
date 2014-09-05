package won.utils.im.port;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.component.needproducer.impl.DirectoryBasedNeedProducer;
import won.bot.framework.component.needproducer.impl.MailFileNeedProducer;

import java.io.File;
import java.io.IOException;

/**
 * User: ypanchenko
 * Date: 04.09.2014
 */
public class NeedDataMailsReader implements NeedDataReader<Model>
{
  DirectoryBasedNeedProducer producer;

  public NeedDataMailsReader(String folderPath) {
    producer = new DirectoryBasedNeedProducer();
    producer.setDirectory(new File(folderPath));
    producer.setFileBasedNeedProducer(new MailFileNeedProducer());
  }

  @Override
  public boolean hasNext() {
    return !producer.isExhausted();
  }

  @Override
  public Model next() {
    return producer.create();
  }

  @Override
  public void close() throws IOException {
    // no need to close the producer
  }
}
