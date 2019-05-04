package won.protocol.agreement.petrinet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import uk.ac.imperial.pipe.animation.PetriNetAnimator;
import uk.ac.imperial.pipe.models.petrinet.PetriNet;
import uk.ac.imperial.pipe.models.petrinet.Place;
import uk.ac.imperial.pipe.models.petrinet.Transition;

public class PetriNetTest {
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    private String getResourceAsString(String name) {
        try {
            byte[] buffer = new byte[256];
            StringWriter sw = new StringWriter();
            try (InputStream in = getResourceAsStream(name)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int bytesRead = 0;
                while ((bytesRead = in.read(buffer)) > -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return new String(baos.toByteArray(), Charset.defaultCharset());
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not load resource " + name, e);
        }
    }

    private Dataset loadDataset(String path) {
        try {
            InputStream is = null;
            Dataset dataset = null;
            try {
                is = getResourceAsStream(path);
                dataset = DatasetFactory.createGeneral();
                RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            dataset.commit();
            return dataset;
        } catch (Exception e) {
            throw new IllegalStateException("could not load resource " + path, e);
        }
    }

    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void testLoadFromString() {
        String pnml = getResourceAsString("won/protocol/petrinet/petrinet-taxi.xml");
        PetriNet net = new PetriNetLoader().readPNML(pnml);
        Assert.assertTrue(net.getPlaces().stream()
                        .anyMatch(place -> place.getId().equals("https://w3id.org/won/process/taxi#PassengerReady")));
    }

    @Test
    public void testLoadFromBase64String() {
        String pnml = getResourceAsString("won/protocol/petrinet/petrinet-taxi-base64.txt");
        PetriNet net = new PetriNetLoader().readBase64EncodedPNML(pnml);
        Assert.assertTrue(net.getPlaces().stream()
                        .anyMatch(place -> place.getId().equals("https://w3id.org/won/process/taxi#PassengerReady")));
    }

    private PetriNet getTaxiNet() {
        String pnml = getResourceAsString("won/protocol/petrinet/petrinet-taxi.xml");
        return new PetriNetLoader().readPNML(pnml);
    }

    // just test that the correct transitions are enabled upon startup
    @Test
    public void testEnabledTransitions() {
        PetriNet net = getTaxiNet();
        PetriNetAnimator animator = new PetriNetAnimator(net);
        Set<Transition> transitions = animator.getEnabledTransitions();
        Assert.assertTrue(transitions.stream().anyMatch(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#PassengerArrivedAtPickupLocation")));
        Assert.assertTrue(transitions.stream().anyMatch(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#DriverArrivedAtPickupLocation")));
    }

    private Set<Place> getMarkedPlaces(PetriNet net) {
        return net.getPlaces().stream().filter(p -> p.getNumberOfTokensStored() > 0).collect(Collectors.toSet());
    }

    @Test
    public void testSuccessfulTransation() {
        PetriNet net = getTaxiNet();
        PetriNetAnimator animator = new PetriNetAnimator(net);
        Set<Transition> transitions = animator.getEnabledTransitions();
        // expected enabled transitions:
        Assert.assertTrue(transitions.stream().anyMatch(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#PassengerArrivedAtPickupLocation")));
        Assert.assertTrue(transitions.stream().anyMatch(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#DriverArrivedAtPickupLocation")));
        Assert.assertEquals(2, transitions.size());
        // actual transition: passenger arrives
        animator.fireTransition(transitions.stream().filter(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#PassengerArrivedAtPickupLocation"))
                        .findFirst().get());
        transitions = animator.getEnabledTransitions();
        // expected enabled transitions:
        Assert.assertTrue(transitions.stream()
                        .anyMatch(t -> t.getId().equals("https://w3id.org/won/process/taxi#PassengerStoppedWaiting")));
        Assert.assertTrue(transitions.stream().anyMatch(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#DriverArrivedAtPickupLocation")));
        Assert.assertEquals(2, transitions.size());
        // actual transition: driver arrives
        animator.fireTransition(transitions.stream().filter(
                        t -> t.getId().equals("https://w3id.org/won/process/taxi#DriverArrivedAtPickupLocation"))
                        .findFirst().get());
        transitions = animator.getEnabledTransitions();
        // expected enabled transitions:
        Assert.assertTrue(transitions.stream()
                        .anyMatch(t -> t.getId().equals("https://w3id.org/won/process/taxi#ArrivedAtDestination")));
        Assert.assertEquals(1, transitions.size());
        // actual transition: arrive at destination
        animator.fireTransition(transitions.stream()
                        .filter(t -> t.getId().equals("https://w3id.org/won/process/taxi#ArrivedAtDestination"))
                        .findFirst().get());
        transitions = animator.getEnabledTransitions();
        // expected enabled transitions:
        Assert.assertTrue(transitions.stream()
                        .anyMatch(t -> t.getId().equals("https://w3id.org/won/process/taxi#PaymentMade")));
        Assert.assertEquals(1, transitions.size());
        // actual transition: payment is made
        animator.fireTransition(transitions.stream()
                        .filter(t -> t.getId().equals("https://w3id.org/won/process/taxi#PaymentMade")).findFirst()
                        .get());
        transitions = animator.getEnabledTransitions();
        // expected enabled transitions:
        Assert.assertTrue(transitions.stream()
                        .anyMatch(t -> t.getId().equals("https://w3id.org/won/process/taxi#PassengerReviewedDriver")));
        Assert.assertTrue(transitions.stream()
                        .anyMatch(t -> t.getId().equals("https://w3id.org/won/process/taxi#DriverReviewedPassenger")));
        Assert.assertEquals(2, transitions.size());
    }

    @Test
    public void testConversationPetriNets() {
        Dataset conversation = loadDataset("won/protocol/petrinet/conversations/simple-petri-net-one-event.trig");
        PetriNetStates nets = PetriNetStates.of(conversation);
        Collection<PetriNetState> states = nets.getPetrinetStates();
        conversation.end();
        Assert.assertTrue(!states.isEmpty());
        Assert.assertEquals(1, states.size());
        PetriNetState state = states.iterator().next();
        Set<URI> marked = state.getMarkedPlaces();
        Assert.assertEquals(1, marked.size());
        Assert.assertEquals(URI.create("http://example.com/state/end"), marked.iterator().next());
    }

    @Test
    public void testTaxiNoShowPetriNet() {
        Dataset conversation = loadDataset("won/protocol/petrinet/conversations/taxi-no-show.trig");
        PetriNetStates nets = PetriNetStates.of(conversation);
        Collection<PetriNetState> states = nets.getPetrinetStates();
        conversation.end();
        Assert.assertTrue(!states.isEmpty());
        Assert.assertEquals(1, states.size());
        PetriNetState state = states.iterator().next();
        Set<URI> marked = state.getMarkedPlaces();
        Assert.assertEquals(3, marked.size());
        Assert.assertTrue(marked.contains(URI.create("https://w3id.org/won/process/taxi#TaxiNoShow")));
        Assert.assertTrue(marked.contains(URI.create("https://w3id.org/won/process/taxi#PassengerCanReview")));
        Assert.assertTrue(marked.contains(URI.create("https://w3id.org/won/process/taxi#DriverCanReview")));
    }
}
