package won.node.rest;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.service.LinkedDataService;
import won.protocol.util.HTTP;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Date: 2/15/11
 * Time: 12:27 AM
 *
 * @author Florian Kleedorfer
 */

/**
 * TODO: check the working draft here and see to conformance:
 * http://www.w3.org/TR/ldp/
 * Not met yet:
 *
 * 4.1.13 LDPR server responses must contain accurate response ETag header values.
 *
 * add dcterms:modified and dcterms:creator
 *
 * 4.4 HTTP PUT - we don't support that. especially:
 * 4.4.1 If HTTP PUT is performed ... (we do that using the owner protocol)
 *
 * 4.4.2 LDPR clients should use the HTTP If-Match header and HTTP ETags to ensure ...
 *
 * 4.5 HTTP DELETE - we don't support that.
 *
 * 4.6 HTTP HEAD - do we support that?
 *
 * 4.7 HTTP PATCH - we don't support that.
 *
 * 4.8 Common Properties - use common properties!!
 *
 * 5.1.2 Retrieving Only Non-member Properties - not supported (would have to be changed in LinkedDataServiceImpl
 *
 *  see 5.3.2 LDPC - send 404 when non-member-properties is not supported...
 *
 *
 * 5.3.3 first page request: if a Request-URI of “<containerURL>?firstPage” is not supported --> 404
 *
 * 5.3.4 support the firstPage query param
 *
 * 5.3.5 server initiated paging is a good idea (see 5.3.5.1 )
 *
 * 5.3.7 ordering
 *
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

    //date format for Expires header (rfc 1123)
    private static final String DATE_FORMAT_RFC_1123 = "EEE, dd MMM yyyyy HH:mm:ss z";

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

        //TODO: actually the expiry information should be the same as that of the resource that is redirected to
        return markAsNeverExpires(Response.seeOther(URI.create(redirectToURI))).build();
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
      //TODO: actually the expiry information should be the same as that of the resource that is redirected to
        return markAsNeverExpires(Response.seeOther(URI.create(redirectToURI))).build();
    }

    @GET
    @Path("/data/need")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response listNeedURIs(
            @Context UriInfo uriInfo,
            @DefaultValue("-1") @QueryParam("page") int page) {
        logger.debug("listNeedURIs() called");
        Model model = linkedDataService.listNeedURIs(page);
      return markAsAlreadyExpired(addLocationHeaderIfNecessary(Response.ok(model), uriInfo.getRequestUri(), URI.create(this.needResourceURIPrefix))) .build();
    }

    @GET
    @Path("/data/connection")
    @Produces("application/rdf+xml,application/x-turtle,text/turtle,text/rdf+n3,application/json")
    public Response listConnectionURIs(
            @Context UriInfo uriInfo,
            @DefaultValue("-1") @QueryParam("page") int page) {
        logger.debug("listNeedURIs() called");
        Model model = linkedDataService.listConnectionURIs(page);
      return markAsAlreadyExpired(addLocationHeaderIfNecessary(Response.ok(model),uriInfo.getRequestUri(), URI.create(this.connectionResourceURIPrefix))).build();
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
           //TODO: need information does change over time. The immutable need information should never expire, the mutable should
           return markAsAlreadyExpired(addLocationHeaderIfNecessary(Response.ok(model), uriInfo.getRequestUri(), needUri)).build();
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
          //TODO: connection information does change over time. The immutable connection information should never expire, the mutable should
          return markAsNeverExpires(addLocationHeaderIfNecessary(Response.ok(model),uriInfo.getRequestUri(), connectionUri)).build();

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
        URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);

        try {
            Model model = linkedDataService.listConnectionURIs(page, needUri);
            return markAsAlreadyExpired(addLocationHeaderIfNecessary(Response.ok(model),uriInfo.getRequestUri(), needUri)).build();
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

    /**
     * Checks if the actual URI is the same as the canonical URI; if not, adds a Location header to the response builder
     * indicating the canonical URI.
     * @param builder
     * @param actualURI
     * @param canonicalURI
     * @return
     */
    private Response.ResponseBuilder addLocationHeaderIfNecessary(Response.ResponseBuilder builder, URI actualURI, URI canonicalURI){
      if(!canonicalURI.resolve(actualURI).equals(canonicalURI)) {
        //the request URI is the canonical URI, it may be a DNS alias or relative
        //according to http://www.w3.org/TR/ldp/#general we have to include
        //the canonical URI in the lcoation header here
        return builder.header(HTTP.HEADER_LOCATION, canonicalURI.toString());
      }
      return builder;
    }

    /**
     * Sets the Date and Expires header fields such that the response will be treated as 'never expires'
     * (and will therefore be cached forever)
     * @param res
     * @return
     */
    private Response.ResponseBuilder markAsNeverExpires(Response.ResponseBuilder res){
      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123);
      return res
          .header(HTTP.HEADER_EXPIRES, dateFormat.format(getNeverExpiresDate()))
          .header(HTTP.HEADER_DATE, dateFormat.format(new Date()))
          ;
    }

  /**
   * Sets the Date and Expires header fields such that the response will be treated as 'already expired'
   * (and will therefore not be cached)
   * @param res
   * @return
   */
  private Response.ResponseBuilder markAsAlreadyExpired(Response.ResponseBuilder res){
    Date headerDate = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123);
    String formattedDate = dateFormat.format(headerDate);
    return res
      .header(HTTP.HEADER_EXPIRES, formattedDate)
      .header(HTTP.HEADER_DATE, formattedDate)
      ;
  }

    //Calculates a date that, according to http spec, means 'never expires'
    //See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
    private Date getNeverExpiresDate(){
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.set(Calendar.YEAR,cal.get(Calendar.YEAR)+1);
      return cal.getTime();
    }


}
