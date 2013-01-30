package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import won.protocol.rest.LinkedDataRestClient;
import won.owner.ws.OwnerProtocolNeedWebServiceClient;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedService {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private LinkedDataRestClient linkedDataRestClient;

    @Value("${uri_protocol_won}")
    private String ownerProtocolWONURI;

    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }

    public void setOwnerProtocolWONURI(String ownerProtocolWONURI) {
        this.ownerProtocolWONURI = ownerProtocolWONURI;
    }

    @Override
    public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: ACCEPT called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            proxy.accept(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: DENY called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            proxy.deny(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            proxy.close(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            proxy.sendTextMessage(connectionURI, message);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public URI connectTo(URI needURI, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.info(MessageFormat.format("need-facing: CONNECT_TO called for other need {0}, own need {1} and message {2}", needURI, otherNeedURI, message));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            return proxy.connectTo(needURI, otherNeedURI, message);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listNeedURIs() {
        logger.info("need-facing: LIST_NEED_URIS called");
        try {

            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            /*new Arr
            Model m = linkedDataRestClient.readResourceData(new URI(ownerProtocolWONURI));
            Iterator iter = m.getBag(m.createResource()).iterator() ;
            List<URI> copy = new ArrayList<URI>();
            while (iter.hasNext())
                copy.add((URI) iter.next());
            return copy;           */
            return Arrays.asList(proxy.listNeedURIs());

        //    OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed();
          //  return Arrays.asList(proxy.listNeedURIs());
        //} catch (MalformedURLException e) {
        //    logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        //} catch (NoSuchNeedException e) {
        //    e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        logger.info(MessageFormat.format("need-facing: LIST_NEED_URIS called for page {0}", page));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            return Arrays.asList(proxy.listNeedURIs());
               /*
            Model m = linkedDataRestClient.readResourceData(new URI(baseNeedURI));
            Iterator iter = m.getBag(m.createResource(baseNeedURI)).iterator() ;
            //TODO: paging
            List<URI> copy = new ArrayList<URI>();
            while (iter.hasNext())
                copy.add((URI) iter.next());
            return copy;
                 */
            //    OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed();
            //  return Arrays.asList(proxy.listNeedURIs());
            //} catch (MalformedURLException e) {
            //    logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            //} catch (NoSuchNeedException e) {
            //    e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0}", needURI));
        try {
           OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
           return Arrays.asList(proxy.listConnectionURIs(needURI));
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        logger.info("need-facing: LIST_CONNECTION_URIS called");
        try {

            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            ArrayList<URI> list = new ArrayList<URI>();

            for(URI uri : proxy.listNeedURIs()) {
               list.addAll(Arrays.asList(proxy.listConnectionURIs(uri)));
            }
            return list;
           /* Model m = linkedDataRestClient.readResourceData(new URI(baseConnectionURI));
            Iterator iter = m.getBag(m.createResource(baseConnectionURI)).iterator() ;
            List<URI> copy = new ArrayList<URI>();
            while (iter.hasNext())
                copy.add((URI) iter.next());
            return copy;
               */
            //    OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed();
            //  return Arrays.asList(proxy.listNeedURIs());
            //} catch (MalformedURLException e) {
            //    logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            //} catch (NoSuchNeedException e) {
            //    e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        logger.info(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for page {0}", page));
        try {

            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            ArrayList<URI> list = new ArrayList<URI>();

            for(URI uri : proxy.listNeedURIs()) {
                list.addAll(Arrays.asList(proxy.listConnectionURIs(uri)));
            }
            return list;
            /*
            Model m = linkedDataRestClient.readResourceData(new URI(baseConnectionURI));
            Iterator iter = m.getBag(m.createResource(baseConnectionURI)).iterator() ;
            //TODO: paging
            List<URI> copy = new ArrayList<URI>();
            while (iter.hasNext())
                copy.add((URI) iter.next());
            return copy;
                                             */
            //    OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed();
            //  return Arrays.asList(proxy.listNeedURIs());
            //} catch (MalformedURLException e) {
            //    logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            //} catch (NoSuchNeedException e) {
            //    e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0} and page {1}", needURI, page));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            //TODO: paging
            return Arrays.asList(proxy.listConnectionURIs(needURI));
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: READ_NEED called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            return proxy.readNeed(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: READ_NEED_CONTENT called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            //TODO: Fix Models
            return null;//proxy.readNeedContent(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public won.protocol.model.Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        logger.info(MessageFormat.format("need-facing: READ_CONNECTION called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            return proxy.readConnection(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        logger.info(MessageFormat.format("need-facing: READ_CONNECTION_CONTENT called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(new URI(ownerProtocolWONURI));
            //TODO: Fix Models
            return null;//proxy.readConnectionContent(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        logger.info(MessageFormat.format("need-facing: CREATE_NEED called for need {0}, with content {1} and activate {2}", ownerURI, content, activate));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            return proxy.createNeed(ownerURI, "", activate);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: ACTIVATE called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            proxy.activate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            proxy.deactivate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<Match> getMatches(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: GET_MATCHES called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(new URI(ownerProtocolWONURI));
            return Arrays.asList(proxy.getMatches(needURI));
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.NEED_PROTOCOL_ENDPOINT);
        logger.info("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
        if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }

    private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.NEED_PROTOCOL_ENDPOINT);
        logger.info("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());
        if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }
}
