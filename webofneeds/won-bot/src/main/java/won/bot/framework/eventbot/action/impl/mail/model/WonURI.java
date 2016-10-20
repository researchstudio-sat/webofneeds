package won.bot.framework.eventbot.action.impl.mail.model;

import java.io.Serializable;
import java.net.URI;

/* Helper Class to encapsulate an uri and the type
 */
public class WonURI implements Serializable{
    private URI uri;
    private UriType type;

    public WonURI(URI uri, UriType type) {
        this.uri = uri;
        this.type = type;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public UriType getType() {
        return type;
    }

    public void setType(UriType type) {
        this.type = type;
    }
}
