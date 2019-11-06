package won.node.service.linkeddata.lookup;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.model.SocketDefinition;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Component
public class SocketLookupFromLinkedData implements SocketLookup {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().getClass());
    @Autowired
    LinkedDataSource linkedDataSource;

    @Override
    public Optional<URI> lookupDefaultSocket(URI atomURI) {
        return WonLinkedDataUtils.getDefaultSocket(atomURI, true, linkedDataSource);
    }

    @Override
    public Optional<SocketDefinition> getSocketConfig(URI socket) {
        try {
            return WonLinkedDataUtils.getSocketDefinitionOfSocket(linkedDataSource, socket);
        } catch (Exception e) {
            logger.info("Failed to load configuation for socket type " + socket, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SocketDefinition> getSocketConfigOfType(URI socketType) {
        try {
            return WonLinkedDataUtils.getSocketDefinition(linkedDataSource, socketType);
        } catch (Exception e) {
            logger.info("Failed to load configuation for socket type " + socketType, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<URI> getSocketType(URI socketURI) {
        return WonLinkedDataUtils.getTypeOfSocket(socketURI,
                        linkedDataSource);
    }

    @Override
    public Optional<Integer> getCapacity(URI socket) {
        Optional<SocketDefinition> localConfig = getSocketConfig(socket);
        if (!localConfig.isPresent() && localConfig.get().getCapacity().isPresent()) {
            return localConfig.get().getCapacity();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getCapacityOfType(URI socketType) {
        Optional<SocketDefinition> localConfig = getSocketConfigOfType(socketType);
        if (!localConfig.isPresent() && localConfig.get().getCapacity().isPresent()) {
            return localConfig.get().getCapacity();
        }
        return Optional.empty();
    }

    @Override
    public boolean isCompatible(URI localSocket, URI targetSocket) {
        Optional<SocketDefinition> localConfig = getSocketConfig(localSocket);
        Optional<SocketDefinition> targetConfig = getSocketConfig(targetSocket);
        if (localConfig.isPresent() && targetConfig.isPresent()) {
            return localConfig.get().isCompatibleWith(targetConfig.get());
        }
        return false;
    }

    @Override
    public boolean isCompatibleSocketTypes(URI localSocketDefinition, URI targetSocketDefinition) {
        Optional<SocketDefinition> localConfig = getSocketConfigOfType(localSocketDefinition);
        Optional<SocketDefinition> targetConfig = getSocketConfigOfType(targetSocketDefinition);
        if (localConfig.isPresent() && targetConfig.isPresent()) {
            return localConfig.get().isCompatibleWith(targetConfig.get());
        }
        return false;
    }

    @Override
    public boolean isAutoOpenSocketType(URI socketDefinition) {
        Optional<SocketDefinition> socketConfig = getSocketConfigOfType(socketDefinition);
        if (socketConfig.isPresent()) {
            return socketConfig.get().isAutoOpen();
        }
        return false;
    }

    @Override
    public boolean isAutoOpen(URI localSocket) {
        Optional<SocketDefinition> socketConfig = getSocketConfig(localSocket);
        if (socketConfig.isPresent()) {
            return socketConfig.get().isAutoOpen();
        }
        return false;
    }
}
