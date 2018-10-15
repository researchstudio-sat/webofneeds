package won.protocol.agreement.petrinet;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.vocabulary.WONWF;

public class PetriNetStates {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AgreementProtocolState agreementProtocolState;
    private Dataset conversation;
    private Map<URI, PetriNetState> petrinetStates = new HashMap<>();
    private PetriNetLoader petriNetLoader = new PetriNetLoader();

    
    public static PetriNetStates of(Dataset conversation) {
        return new PetriNetStates(conversation);
    }
    
    public static PetriNetStates of(AgreementProtocolState agreementProtocolState) {
        return new PetriNetStates(agreementProtocolState);
    }
    
    private PetriNetStates(Dataset conversation) {
        this.conversation = conversation;
        this.agreementProtocolState = AgreementProtocolState.of(conversation);
        calculate();
    }
    
    private PetriNetStates(AgreementProtocolState agreementProtocolState) {
        this.agreementProtocolState = agreementProtocolState;
        this.conversation = agreementProtocolState.getConversationDataset();
        calculate();
    }

    private void calculate() {
        // use only agreements and claims, process them in chronological order, leaving out 
        // those that have been cancelled (agreements) or rejected (claims)
        // 1. when we find a petrinet definition: "x wf:hasInlinePetrinetDefinition [base64string]"
        //  1.1. read petrinet definition
        //  1.2 initialize petrinet state from file
        // 2.when we find a transition-firing triple: "x wf:firesTransition [eventURI]"
        //     (transitions are annotated with unique event URIs that fire them)
        //  1. find the transition annotated with the event URI and fire it
        //  2. update the petrinet state

        //get agreement uris in chronological order
        List<URI> agreementUris = agreementProtocolState.getAgreementsInChronologicalOrder(true);
        
        //walk over agreements
        agreementUris.forEach(agreementURI -> {
            Model agreement = agreementProtocolState.getAgreement(agreementURI);
        
            //first, find petri net in current agreement
            loadPetrinetsForAgreement(agreement, agreementURI);
            
            //now see if there are events and execute them as transition firings
            executePetriNetEventsForAgreement(agreement, agreementURI);
        });
        
    }

    private void loadPetrinetsForAgreement(Model agreement, URI agreementUri) {
        StmtIterator it = agreement.listStatements(null, WONWF.HAS_INLINE_PETRI_NET_DEFINITION, (RDFNode) null);
        while(it.hasNext()) {
            Statement stmt = it.next();
            if (stmt.getSubject().isURIResource() && stmt.getObject().isLiteral()) {
                URI petriNetUri = URI.create(stmt.getSubject().asResource().getURI());
                String base64EncodedPnml = stmt.getObject().asLiteral().getString();
                if (petrinetStates.containsKey(petriNetUri)) {
                    logger.info("ignoring redefinition of petri net {} in agreement {}", petriNetUri, agreementUri);
                } else {
                    PetriNet petriNet = petriNetLoader.readBase64EncodedPNML(base64EncodedPnml);
                    PetriNetState state = new PetriNetState(petriNetUri, petriNet);
                    petrinetStates.put(petriNetUri, state);
                }
            }
        }
    }
    
    private void executePetriNetEventsForAgreement(Model agreement, URI agreementUri) {
        StmtIterator it = agreement.listStatements(null, WONWF.FIRES_TRANSITION, (RDFNode) null);
        while(it.hasNext()) {
            Statement stmt = it.next();
            if (stmt.getSubject().isURIResource() && stmt.getObject().isURIResource()) {
                URI petriNetUri = URI.create(stmt.getSubject().asResource().getURI());
                URI eventURI = URI.create(stmt.getObject().asResource().getURI());
                PetriNetState state = petrinetStates.get(petriNetUri);
                if (state != null) {
                    state.fireTransition(eventURI);
                } else {
                    logger.info("ignoring event {} for unknown petri net {} in agreement {}", new Object[] {eventURI, petriNetUri, agreementUri});
                }
            }
        }
    }
    
    
    
    public Collection<PetriNetState> getPetrinetStates() {
        return petrinetStates.values();
    }
    
    public Set<PetriNetUris> getPetriNetUris() {
        Set<PetriNetUris> petriNetUriSet = new HashSet<>();
        this.petrinetStates.values().forEach(petriNetState -> {
            PetriNetUris petriNetUris = new PetriNetUris();
            petriNetUris.setEnabledTransitions(petriNetState.getEnabledTransitions());
            petriNetUris.setMarkedPlaces(petriNetState.getMarkedPlaces());
            petriNetUris.setPetriNetURI(petriNetState.getPetriNetURI());
            petriNetUriSet.add(petriNetUris);
        });
        return petriNetUriSet;
    }
    
}
