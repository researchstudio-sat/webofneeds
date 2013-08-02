package won.matcher.solr;

import org.apache.solr.common.SolrInputDocument;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: atus
 * Date: 10.07.13
 */
public class DocumentStorage
{
  private static DocumentStorage instance = null;

  private Queue<SolrInputDocument> storage;

  private DocumentStorage()
  {
    storage = new ConcurrentLinkedQueue();
  }

  public static DocumentStorage getInstance()
  {
    if (instance == null)
      instance = new DocumentStorage();

    return instance;
  }

  public void push(SolrInputDocument document)
  {
    storage.add(document);
  }

  public boolean hasNext()
  {
    return !storage.isEmpty();
  }

  public SolrInputDocument pop()
  {
    return storage.poll();
  }

}
