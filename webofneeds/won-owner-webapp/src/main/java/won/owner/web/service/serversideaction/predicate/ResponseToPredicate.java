package won.owner.web.service.serversideaction.predicate;

import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;

import won.protocol.message.WonMessage;

public class ResponseToPredicate implements Predicate<WonMessage> {
    
    private URI messageURI;
    
    public ResponseToPredicate(URI messageURI) {
        super();
        Objects.nonNull(messageURI);
        this.messageURI = messageURI;
    }

    @Override
    public boolean test(WonMessage t) {
        return this.messageURI.equals(t.getIsResponseToMessageURI());
    }
}
