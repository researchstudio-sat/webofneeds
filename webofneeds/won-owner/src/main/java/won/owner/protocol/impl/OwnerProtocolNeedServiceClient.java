package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.owner.service.impl.URIService;
import won.owner.ws.OwnerProtocolNeedWebServiceClient;
import won.protocol.exception.*;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.WON;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Stack;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedService {
    /* default wsdl location */
    private static final String WSDL_LOCATION =  "?wsdl";

    /* Linked Data default paths */
    private static final String NEED_URI_PATH_PREFIX = "/data/need";
    private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
    private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";

    final Logger logger = LoggerFactory.getLogger(getClass());

    private LinkedDataRestClient linkedDataRestClient;

    private URIService uriService;


    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }

    public void setUriService(final URIService uriService)
    {
      this.uriService = uriService;
    }

  @Override
    public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: ACCEPT called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.accept(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: DENY called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.deny(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.close(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.debug(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            proxy.sendTextMessage(connectionURI, message);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public URI connectTo(URI needURI, URI otherNeedURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.info(MessageFormat.format("need-facing: CONNECT_TO called for other need {0}, own need {1} and message {2}", needURI, otherNeedURI, message));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(needURI);
            return proxy.connectTo(needURI, otherNeedURI, message);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
        return null;
    }

    @Override
    public Collection<URI> listNeedURIs() {
        logger.debug("need-facing: LIST_NEED_URIS called");
        return getHardcodedResource(NEED_URI_PATH_PREFIX);
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        logger.debug(MessageFormat.format("need-facing: LIST_NEED_URIS called for page {0}", page));
        return getHardcodedResource(NEED_URI_PATH_PREFIX + "?page=" + page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0}", needURI));

        // TODO: probably wrong
        return getHardcodedResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX);
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        logger.debug("need-facing: LIST_CONNECTION_URIS called");
        return getHardcodedResource(CONNECTION_URI_PATH_PREFIX);
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for page {0}", page));
        return getHardcodedResource(CONNECTION_URI_PATH_PREFIX + "?page=" + page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0} and page {1}", needURI, page));
        return getHardcodedResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX + "?page=" + page);
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: READ_NEED called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(needURI);
            return proxy.readNeed(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        }
        return null;
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: READ_NEED_CONTENT called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy =getOwnerProtocolEndpointForNeed(needURI);
            Model m = ModelFactory.createDefaultModel();

            StringReader sr = new StringReader(proxy.readNeedContent(needURI));
            m.read(sr, null, "TTL");
            return m;
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
        return null;
    }

    @Override
    public won.protocol.model.Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        logger.debug(MessageFormat.format("need-facing: READ_CONNECTION called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            return proxy.readConnection(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
        return null;
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        logger.debug(MessageFormat.format("need-facing: READ_CONNECTION_CONTENT called for connection {0}", connectionURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            //TODO: Fix Models
            return null;//proxy.readConnectionContent(connectionURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
        return null;
    }

    public URI createNeed(URI ownerURI, Model content, boolean activate, String wonURI) throws IllegalNeedContentException {
        logger.info(MessageFormat.format("need-facing: CREATE_NEED called for need {0}, with content {1} and activate {2}", ownerURI, content, activate));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getHardcodedOwnerProtocolEndpointForNeed(wonURI);

            StringWriter sw = new StringWriter();
            content.write(sw, "TTL");

            return proxy.createNeed(ownerURI, sw.toString(), activate);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        logger.info(MessageFormat.format("need-facing: CREATE_NEED called for need {0}, with content {1} and activate {2}", ownerURI, content, activate));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getHardcodedOwnerProtocolEndpointForNeed();

            StringWriter sw = new StringWriter();
            content.write(sw, "TTL");

            return proxy.createNeed(ownerURI, sw.toString(), activate);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: ACTIVATE called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy =getOwnerProtocolEndpointForNeed(needURI);
            proxy.activate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy =getOwnerProtocolEndpointForNeed(needURI);
            proxy.deactivate(needURI);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public Collection<Match> getMatches(URI needURI) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: GET_MATCHES called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy =getOwnerProtocolEndpointForNeed(needURI);
            return Arrays.asList(proxy.getMatches(needURI));
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            e.printStackTrace();
        } 
        return null;
    }

    private Collection<URI> getHardcodedResource(URI needURI, String res) throws NoSuchNeedException {

        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
        logger.debug("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
        if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);

        Model mUris = linkedDataRestClient.readResourceData(
                URI.create(needProtocolEndpoint.toString() + res));


        Stack<URI> uris = new Stack<URI>();
        for(Statement stmt : mUris.listStatements().toList()) {
            uris.push(URI.create(stmt.getObject().toString()));
        }

        return uris;
    }

    private Collection<URI> getHardcodedResource(String res) {
        Model mUris = linkedDataRestClient.readResourceData(
                URI.create(this.uriService.getDefaultOwnerProtocolNeedServiceEndpointURI().toString() + res));


        Stack<URI> uris = new Stack<URI>();
        for(Statement stmt : mUris.listStatements().toList()) {
            uris.push(URI.create(stmt.getObject().toString()));
        }

        return uris;
    }

    private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
        logger.debug("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
        if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION).toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }
    //TODO: workaround until we can work with multiple WON nodes: protocol URI is hard-coded in spring properties
    private OwnerProtocolNeedWebServiceEndpoint getHardcodedOwnerProtocolEndpointForNeed(String ownerProtocolWONURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(ownerProtocolWONURI + WSDL_LOCATION).toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }

    //TODO: workaround until we can work with multiple WON nodes: protocol URI is hard-coded in spring properties
    private OwnerProtocolNeedWebServiceEndpoint getHardcodedOwnerProtocolEndpointForNeed() throws NoSuchNeedException, MalformedURLException
    {

        //TODO: fetch endpoint information for the need and store in db?
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create((this.uriService.getDefaultOwnerProtocolNeedServiceEndpointURI().toString()+ WSDL_LOCATION)).toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }

    private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
        logger.debug("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());
        if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);
        OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION).toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }
}
