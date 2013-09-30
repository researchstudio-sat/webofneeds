package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.*;
import won.protocol.model.FacetType;
import won.protocol.service.*;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public abstract class FacetImpl implements ConnectionCommunicationService, OwnerFacingNeedCommunicationService,
        NeedFacingNeedCommunicationService, MatcherFacingNeedCommunicationService, NeedManagementService {
    public abstract FacetType getFacetType();

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void hint(URI needURI, URI otherNeed, double score, URI originator, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI connect(URI needURI, URI otherNeedURI, URI otherConnectionURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
