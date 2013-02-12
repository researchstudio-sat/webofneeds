package won.node.rest;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.service.LinkedDataService;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Date: 2/15/11
 * Time: 12:27 AM
 *
 * @author Florian Kleedorfer
 */

@Path("/") //the servlet-mapping defines where this root path is published externally
public class LinkedDataRestService {
    final Logger logger = LoggerFactory.getLogger(getClass());

    //prefix of a need resource
    private String needResourceURIPrefix;
    //prefix of a connection resource
    private String connectionResourceURIPrefix;
    //prefix for URISs of RDF data
    private String dataURIPrefix;
    //prefix for URIs referring to real-world things
    private String resourceURIPrefix;
    //prefix for human readable pages
    private String pageURIPrefix;

    @Autowired
    private LinkedDataService linkedDataService;

    @GET
    @Path("/resource/{whatever:.*}")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response redirectToData(
            @Context UriInfo uriInfo,
            @PathParam("identifier") String identifier) {
        URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
        URI dataUri = URI.create(this.dataURIPrefix);
        String requestUri = uriInfo.getRequestUri().toString();

        String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), dataUri.getPath());
        return Response.seeOther(URI.create(redirectToURI)).build();
    }

    @GET
    @Path("/resource/{whatever:.*}")
    @Produces("text/html")
    public Response redirectToPage(
            @Context UriInfo uriInfo,
            @PathParam("identifier") String identifier) {
        URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
        URI pageUriPrefix = URI.create(this.pageURIPrefix);
        String requestUri = uriInfo.getRequestUri().toString();

        String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), pageUriPrefix.getPath());
        return Response.seeOther(URI.create(redirectToURI)).build();
    }

    @GET
    @Path("/data/need")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response listNeedURIs(
            @Context UriInfo uriInfo,
            @DefaultValue("-1") @QueryParam("page") int page) {
        logger.debug("listNeedURIs() called");
        Model model = linkedDataService.listNeedURIs(page);
        return Response.ok(model).build();
    }

    @GET
    @Path("/data/connection")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response listConnectionURIs(
            @Context UriInfo uriInfo,
            @DefaultValue("-1") @QueryParam("page") int page) {
        logger.debug("listNeedURIs() called");
        Model model = linkedDataService.listConnectionURIs(page);
        return Response.ok(model).build();
    }


    @GET
    @Path("/data/need/{identifier}")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response readNeed(
            @Context UriInfo uriInfo,
            @PathParam("identifier") String identifier) {
        logger.debug("readNeed() called");
        URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);
        try {
            Model model = linkedDataService.getNeedModel(needUri);
            return Response.ok(model).build();
        } catch (NoSuchNeedException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("/data/connection/{identifier}")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response readConnection(
            @Context UriInfo uriInfo,
            @PathParam("identifier") String identifier) {
        logger.debug("readConnection() called");
        URI connectionUri = URI.create(this.connectionResourceURIPrefix + "/" + identifier);

        try {
            Model model = linkedDataService.getConnectionModel(connectionUri);
            return Response.ok(model).build();
        } catch (NoSuchConnectionException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    @GET
    @Path("/data/need/{identifier}/connections")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response readConnectionsOfNeed(
            @Context UriInfo uriInfo,
            @PathParam("identifier") String identifier,
            @DefaultValue("-1") @QueryParam("page") int page) {
        logger.debug("readConnectionsOfNeed() called");
        URI needURI = URI.create(this.needResourceURIPrefix + "/" + identifier);

        try {
            Model model = linkedDataService.listConnectionURIs(page, needURI);
            return Response.ok(model).build();
        } catch (NoSuchNeedException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    public void setNeedResourceURIPrefix(String needResourceURIPrefix) {
        this.needResourceURIPrefix = needResourceURIPrefix;
    }

    public void setConnectionResourceURIPrefix(String connectionResourceURIPrefix) {
        this.connectionResourceURIPrefix = connectionResourceURIPrefix;
    }

    public void setDataURIPrefix(String dataURIPrefix) {
        this.dataURIPrefix = dataURIPrefix;
    }

    public void setResourceURIPrefix(final String resourceURIPrefix) {
        this.resourceURIPrefix = resourceURIPrefix;
    }

    public void setLinkedDataService(final LinkedDataService linkedDataService) {
        this.linkedDataService = linkedDataService;
    }

    public void setPageURIPrefix(final String pageURIPrefix) {
        this.pageURIPrefix = pageURIPrefix;
    }
}
