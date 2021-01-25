package won.node.protocol.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import won.cryptography.rdfsign.DefaultWebIdKeyLoader;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.node.service.persistence.AtomService;
import won.protocol.model.Atom;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Component
@Primary
public class DbBackedWebIdKeyLoader implements WebIdKeyLoader {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    WonKeysReaderWriter wonKeysReaderWriter = new WonKeysReaderWriter();
    @Autowired
    private DefaultWebIdKeyLoader defaultWebIdKeyLoader;
    @Autowired
    private AtomService atomService;

    public DbBackedWebIdKeyLoader() {
    }

    @Override
    @Transactional
    public Set<PublicKey> loadKey(String keyURI)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Set<PublicKey> keys = getKeyFromLocalAtom(keyURI);
        if (keys != null) {
            return keys;
        }
        return defaultWebIdKeyLoader.loadKey(keyURI);
    }

    private Set<PublicKey> getKeyFromLocalAtom(String keyURI) {
        try {
            Optional<Atom> atomOpt = atomService.getAtom(URI.create(keyURI));
            if (atomOpt.isEmpty()) {
                return null;
            }
            Map<String, PublicKey> keys = wonKeysReaderWriter
                            .readFromDataset(atomOpt.get().getDatatsetHolder().getDataset());
            return keys.values().stream().collect(toSet());
        } catch (Exception e) {
            logger.debug("could not load key locally: {}", keyURI);
        }
        return null;
    }
}
