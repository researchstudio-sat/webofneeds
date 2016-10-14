package won.matcher.service.crawler.msg;

import org.apache.jena.riot.Lang;

/**
 * Created by hfriedrich on 14.10.2016.
 *
 * Crawl message that has the target (optionally) resource already in it.
 */
public class ResourceCrawlUriMessage extends CrawlUriMessage
{
  private String serializedResource;
  private Lang serializationFormat;

  public ResourceCrawlUriMessage(final String uri, final String baseUri, final String wonNodeUri, final STATUS status, final long crawlDate) {
    super(uri, baseUri, wonNodeUri, status, crawlDate);
  }

  public ResourceCrawlUriMessage(final String uri, final String baseUri, final STATUS status, final long crawlDate) {
    super(uri, baseUri, status, crawlDate);
  }

  public String getSerializedResource() {
    return serializedResource;
  }

  public void setSerializedResource(final String serializedResource) {
    this.serializedResource = serializedResource;
  }

  public Lang getSerializationFormat() {
    return serializationFormat;
  }

  public void setSerializationFormat(final Lang serializationFormat) {
    this.serializationFormat = serializationFormat;
  }
}
