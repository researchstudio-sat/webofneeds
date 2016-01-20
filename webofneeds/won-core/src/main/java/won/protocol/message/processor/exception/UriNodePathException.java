package won.protocol.message.processor.exception;

import java.net.URI;

/**
 * Indicates that the uri (of event or need) is malformed from the point
 * of view of the node that is supposed to store it. (e.g. the domain
 * if the message event uri does not correspond to the node domain, etc.)
 *
 * User: ypanchenko
 * Date: 24.04.2015
 */
public class UriNodePathException extends WonMessageProcessingException
{
  public UriNodePathException() {
  }

  public UriNodePathException(final URI uri) {
    super(uri.toString());
  }

  public UriNodePathException(final URI uri, final Throwable cause) {
    super(uri.toString(), cause);
  }

  public UriNodePathException(final Throwable cause) {
    super(cause);
  }

  public UriNodePathException(final URI uri, final Throwable cause, final boolean enableSuppression,
                              final boolean writableStackTrace) {
    super(uri.toString(), cause, enableSuppression, writableStackTrace);
  }
}
