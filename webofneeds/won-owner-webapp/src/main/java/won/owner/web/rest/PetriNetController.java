package won.owner.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.petrinet.PetriNetStates;
import won.protocol.agreement.petrinet.PetriNetUris;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.util.Set;

@Controller
@RequestMapping("/rest/petrinet")
public class PetriNetController {
    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfAtom;

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSourceOnBehalfOfAtom = linkedDataSource;
    }

    @RequestMapping(value = "/getPetriNetUris", method = RequestMethod.GET)
    public ResponseEntity<Set<PetriNetUris>> getPetriNetUris(URI connectionUri) {
        return new ResponseEntity<Set<PetriNetUris>>(
                        PetriNetStates.of(getAgreementProtocolState(connectionUri)).getPetriNetUris(), HttpStatus.OK);
    }

    private AgreementProtocolState getAgreementProtocolState(URI connectionUri) {
        try {
            AuthenticationThreadLocal.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
            return WonConversationUtils.getAgreementProtocolState(connectionUri, linkedDataSourceOnBehalfOfAtom);
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }
}
