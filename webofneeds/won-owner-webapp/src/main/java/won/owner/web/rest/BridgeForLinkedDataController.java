package won.owner.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.rest.LinkedDataRestBridge;
import won.protocol.rest.RDFMediaType;

/**
 * User: ypanchenko Date: 03.09.2015
 *
 * This controller at Owner server-side serves as a bridge for Owner client-side
 * to obtain linked data from a Node: because the linked data on a Node can have
 * restricted access based on WebID, only Owner server-side can provide the
 * client's certificate as proof of having the private key from client's
 * published WebID. Because of this, Owner client-side has to ask Owner-server
 * side to query Node for it, instead of querying directly from Owner
 * client-side.
 */
@Controller
@RequestMapping("/rest/linked-data")
public class BridgeForLinkedDataController implements InitializingBean {

  private String filterQuery;

  @Autowired
  private WONUserDetailService wonUserDetailService;

  @Autowired
  private LinkedDataRestBridge linkedDataRestBridgeOnBehalfOfNeed;

  @Autowired
  private LinkedDataRestBridge linkedDataRestBridge;

  @Override
  public void afterPropertiesSet() throws Exception {
    InputStream is = this.getClass().getResourceAsStream("/linkeddatabridge/filter-query.rq");
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(is, writer, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read filter query file", e);
    }
    this.filterQuery = writer.toString();
  }

  final Logger logger = LoggerFactory.getLogger(getClass());

  /*
   * //for some reason this cannot be used as parameter in restTemplate.execute()
   * private final ResponseExtractor httpResponseResponseExtractor = new
   * ResponseExtractor<ClientHttpResponse>() {
   * 
   * @Override public ClientHttpResponse extractData(final ClientHttpResponse
   * response) throws IOException { return response; } };
   */

  @RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = { "*/*" })
  public void fetchResource(@RequestParam("uri") String resourceUri,
      @RequestParam(value = "requester", required = false) String requesterWebId, final HttpServletResponse response,
      final HttpServletRequest request) throws IOException {

    // prepare restTestmplate that can deal with webID certificate
    RestTemplate restTemplate = null;
    // no webID requested? - don't use one!
    if (requesterWebId == null) {
      restTemplate = linkedDataRestBridge.getRestTemplate();
    } else {
      // check if the requesterWebID actually is an URI
      try {
        new URI(requesterWebId);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(
            "Parameter 'requester' must be a URI. Actual value was: '" + requesterWebId + "'");
      }
      // check if the currently logged in user owns that webid:
      if (currentUserHasIdentity(requesterWebId)) {
        // yes: let them use it
        restTemplate = linkedDataRestBridgeOnBehalfOfNeed.getRestTemplate(requesterWebId);
      } else {
        // no: that's fishy, but we let them make the request without the webid
        restTemplate = linkedDataRestBridge.getRestTemplate();
      }
    }

    // prepare headers to be passed in request for linked data resource
    final HttpHeaders requestHeaders = extractLinkedDataRequestRelevantHeaders(request);

    restTemplate.execute(URI.create(resourceUri), HttpMethod.valueOf(request.getMethod()), new RequestCallback() {
      @Override
      public void doWithRequest(final ClientHttpRequest request) throws IOException {
        request.getHeaders().setAll(requestHeaders.toSingleValueMap());
      }
    }, new ResponseExtractor<Object>() {
      @Override
      public ClientHttpResponse extractData(final ClientHttpResponse originalResponse) throws IOException {
        prepareBridgeResponseOutputStream(originalResponse, response);
        // we don't really need to return anything, so we don't
        return null;
      }
    });

    // by this point, the response is constructed and is ready to be returned to the
    // client
  }

  private void prepareBridgeResponseOutputStream(final ClientHttpResponse originalResponse,
      final HttpServletResponse response) throws IOException {
    // create response headers
    MediaType originalResponseMediaType = originalResponse.getHeaders().getContentType();
    if (originalResponseMediaType == null) {
      // No content-type header: we assume there is no body, as in our application
      // this only happens
      // with 304 NOT MODIFIED responses. We don't copy the body to the response. We
      // log a debug message though
      logger.debug(
          "no Content-Type header found in response from server. Assuming no body, not attempting to copy " + "body");
      copyLinkedDataResponseRelevantHeaders(originalResponse.getHeaders(), response);
      response.setStatus(originalResponse.getRawStatusCode());
    } else {
      if (RDFMediaType.isRDFMediaType(originalResponseMediaType)) {
        copyLinkedDataResponseRelevantHeaders(originalResponse.getHeaders(), response);
        response.setStatus(originalResponse.getRawStatusCode());
        // create response body
        copyResponseBody(originalResponse, response);
        // close response output stream
      } else {
        // the content type is not an RDF media type. We don't like to handle such
        // requests. indicate this with
        // the BAD GATEWAY response status
        copyResponseBody(originalResponse, response);
        response.setStatus(HttpStatus.SC_BAD_GATEWAY);
        /*
         * response.getOutputStream().print("The nodes' response was of type " +
         * originalResponseMediaType +
         * ". For security reasons the owner-server only forwards responses of the following types "
         * + RDFMediaType.rdfMediaTypes.toString());
         */
      }
    }
    response.getOutputStream().flush();
    response.getOutputStream().close();
  }

  private void copyResponseBody(final ClientHttpResponse fromResponse, final HttpServletResponse toResponse)
      throws IOException {

    InputStream is = fromResponse.getBody();
    if (is == null) {
      return;
    }

    MediaType contentType = fromResponse.getHeaders().getContentType();
    if (!RDFMediaType.isRDFMediaType(contentType)) {
      copyResponseDirectly(toResponse, is);
      return;
    }
    copyResponseFiltered(toResponse, is, contentType);
  }

  private void copyResponseFiltered(final HttpServletResponse toResponse, InputStream is, MediaType contentType)
      throws IOException {
    Lang lang = RDFLanguages.contentTypeToLang(contentType.toString());
    Dataset ds = DatasetFactory.createGeneral();
    RDFDataMgr.read(ds, is, lang);
    UpdateRequest update = UpdateFactory.create(this.filterQuery);
    UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update, ds);
    updateProcessor.execute();
    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {
      Model graph = ds.getNamedModel(graphNames.next());
      if (graph.isEmpty()) {
        graphNames.remove();
      }
    }
    RDFDataMgr.write(toResponse.getOutputStream(), ds, lang);
  }

  private void copyResponseDirectly(final HttpServletResponse toResponse, InputStream is) throws IOException {
    org.apache.commons.io.IOUtils.copy(is, toResponse.getOutputStream());
  }

  /**
   * Currently copies all the headers from the given headers to the headers of the
   * response. TODO check with spec if any other headers should not be copied (e.g
   * , it seems Transfer-Encoding should not be copied by proxies, see
   * https://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html#sec19.4.6)
   *
   * @param fromHeaders
   * @param toResponse
   */
  private void copyLinkedDataResponseRelevantHeaders(final HttpHeaders fromHeaders,
      final HttpServletResponse toResponse) {
    for (String headerName : fromHeaders.keySet()) {
      for (String headerValue : fromHeaders.get(headerName)) {

        if ((headerName != "Transfer-Encoding")
            || (headerValue != "chunked") && !toResponse.containsHeader(headerName)) {
          // we allow all transfer codings except chunked, because we don't do chunking
          // here!
          toResponse.setHeader(headerName, headerValue);
        }
      }
    }
  }

  /**
   * Extract all headers that are relevant in a request for linked data resource
   * 
   * @param request
   * @return
   */
  private HttpHeaders extractLinkedDataRequestRelevantHeaders(final HttpServletRequest request) {
    HttpHeaders headers = new HttpHeaders();
    copyHeader(HttpHeaders.ACCEPT, request, headers);
    copyHeader("Prefer", request, headers);
    copyHeader(HttpHeaders.ACCEPT_LANGUAGE, request, headers);
    copyHeader(HttpHeaders.ACCEPT_ENCODING, request, headers);
    copyHeader(HttpHeaders.USER_AGENT, request, headers);
    copyHeader(HttpHeaders.CACHE_CONTROL, request, headers);
    copyHeader(HttpHeaders.IF_NONE_MATCH, request, headers);
    return headers;
  }

  private void copyHeader(final String headerName, final HttpServletRequest fromRequest, final HttpHeaders toHeaders) {
    Enumeration<String> values = fromRequest.getHeaders(headerName);
    while (values.hasMoreElements()) {
      toHeaders.add(headerName, values.nextElement());
    }
  }

  /**
   * Check if the current user has the claimed identity represented by web-id of
   * the need. I.e. if the identity is that of the need that belongs to the user -
   * return true, otherwise - false.
   *
   * @param requesterWebId
   * @return
   */
  private boolean currentUserHasIdentity(final String requesterWebId) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = (User) wonUserDetailService.loadUserByUsername(username);
    Set<URI> needUris = getUserNeedUris(user);
    if (needUris.contains(URI.create(requesterWebId))) {
      return true;
    }
    return false;
  }

  private Set<URI> getUserNeedUris(final User user) {
    Set<URI> needUris = new HashSet<>();
    for (UserNeed userNeed : user.getUserNeeds()) {
      needUris.add(userNeed.getUri());
    }
    return needUris;
  }

}
