package won.auth.socket.impl;

import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import won.auth.AuthUtils;
import won.auth.model.SocketDefinition;
import won.auth.socket.SocketAuthorizationSource;
import won.auth.socket.SocketAuthorizations;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.net.URI;
import java.util.Optional;

public class LDSocketAuthorizationSource implements SocketAuthorizationSource {
    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public Optional<SocketAuthorizations> getSocketAuthorizations(URI socketDefinitionURI) {
        Dataset data = linkedDataSource.getDataForPublicResource(socketDefinitionURI);
        if (data == null || data.isEmpty()) {
            throw new IllegalStateException(
                            String.format("Could not load RDF for socket definition %s", socketDefinitionURI));
        }
        Shacl2JavaInstanceFactory factory = AuthUtils.instanceFactory();
        Optional<SocketDefinition> sd = factory.accessor(data.getDefaultModel().getGraph())
                        .getInstanceOfType(socketDefinitionURI.toString(), SocketDefinition.class);
        if (sd.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SocketAuthorizations(
                        socketDefinitionURI,
                        sd.get().getLocalAuths(), sd.get()
                                        .getTargetAuths()));
    }
}
