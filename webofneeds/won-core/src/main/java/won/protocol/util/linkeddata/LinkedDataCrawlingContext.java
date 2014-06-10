package won.protocol.util.linkeddata;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: sbyim
 * Date: 04.06.14
 */
public class LinkedDataCrawlingContext
{
  private int maxDepth;
  private int maxRequest;
  private int timeout;
  private List<URI> crawledURIs;

  public LinkedDataCrawlingContext(int maxDepth, int maxRequest, int timeout){
    this.maxDepth = maxDepth;
    this.maxRequest = maxRequest;
    this.timeout = timeout;
    this.crawledURIs = new ArrayList<>();
  }

  public void addCrawledURI(URI crawledURI){
    this.crawledURIs.add(crawledURI);
  }
  public boolean checkIfAlreadyCrawled(URI uri){
    if (crawledURIs.contains(uri)){
      return true;
    }else{
      return false;
    }
  }

}
