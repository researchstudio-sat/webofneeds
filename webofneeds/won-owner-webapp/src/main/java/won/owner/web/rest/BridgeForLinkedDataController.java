package won.owner.web.rest;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 03.09.2015
 *
 * This controller at Owner server-side serves as a bridge for Owner client-side to obtain linked data from a Node:
 * because the linked data on a Node can have restricted access based on WebID, only Owner server-side can provide the
 * client's certificate as proof of having the private key from client's published WebID. Because of this, Owner
 * client-side has to ask Owner-server side to query Node for it, instead of querying directly from Owner client-side.
 */
@Controller
@RequestMapping("/rest/linked-data")
public class BridgeForLinkedDataController {

  @Autowired
  private WONUserDetailService wonUserDetailService;

  @Autowired
  private LinkedDataRestBridge linkedDataRestBridge;

  final Logger logger = LoggerFactory.getLogger(getClass());

  /* //for some reason this cannot be used as parameter in restTemplate.execute()
  private final ResponseExtractor httpResponseResponseExtractor = new ResponseExtractor<ClientHttpResponse>()
  {
    @Override
    public ClientHttpResponse extractData(final ClientHttpResponse response)
    throws IOException {
      return response;
    }
  };*/



  @RequestMapping(
    value = "/",
    method = RequestMethod.GET,
    produces={"*/*"}
  )
  public void fetchResource(
    @RequestParam("uri") String resourceUri,
    @RequestParam(value ="requester", required = false) String requesterWebId,
    final HttpServletResponse response, final HttpServletRequest request) throws IOException {


    // prepare restTestmplate that can deal with webID certificate
    RestTemplate restTemplate = null;
    if (requesterWebId == null || !currentUserHasIdentity(requesterWebId)) {
      restTemplate = linkedDataRestBridge.getRestTemplate();
    } else {
      restTemplate = linkedDataRestBridge.getRestTemplate(requesterWebId);
    }

    // prepare headers to be passed in request for linked data resource
    final HttpHeaders requestHeaders = extractLinkedDataRequestRelevantHeaders(request);

    restTemplate.execute(
      URI.create(resourceUri),
      HttpMethod.valueOf(request.getMethod()),
      new RequestCallback() {
        @Override
        public void doWithRequest(final ClientHttpRequest request)
                throws IOException {
          request.getHeaders()
                  .setAll(requestHeaders.toSingleValueMap());
        }
      },
      new ResponseExtractor<Object>() {
        @Override
        public ClientHttpResponse extractData(final ClientHttpResponse originalResponse)
                throws IOException {
          prepareBridgeResponseOutputStream(originalResponse, response);
          //we don't really need to return anything, so we don't
          return null;
        }
      });

    // by this point, the response is constructed and is ready to be returned to the client
  }

  private void prepareBridgeResponseOutputStream(final ClientHttpResponse originalResponse, final HttpServletResponse response)
    throws IOException {
    // create response headers
    MediaType originalResponseMediaType = originalResponse.getHeaders().getContentType();
    if(RDFMediaType.isRDFMediaType(originalResponseMediaType)){
      copyLinkedDataResponseRelevantHeaders(originalResponse.getHeaders(), response);
      response.setStatus(originalResponse.getRawStatusCode());
      // create response body
      copyResponseBody(originalResponse, response);
      // close response output stream
    } else {
      response.setStatus(HttpStatus.SC_BAD_GATEWAY);
      response.getOutputStream().print("The nodes' response was of type " + originalResponseMediaType
              + ". For security reasons the owner-server only forwards responses of the following types " +
              RDFMediaType.rdfMediaTypes.toString());
    };
    response.getOutputStream().flush();
    response.getOutputStream().close();
  }

  private void copyResponseBody(final ClientHttpResponse fromResponse, final HttpServletResponse toResponse)
    throws IOException {
    org.apache.commons.io.IOUtils.copy(fromResponse.getBody(), toResponse.getOutputStream());
  }


  /**
   * Currently copies all the headers from the given headers to the headers of the response.
   * TODO check with spec if any other headers should not be copied
   * (e.g , it seems Transfer-Encoding should not be copied by proxies, see https://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html#sec19.4.6)
   *
   * @param fromHeaders
   * @param toResponse
   */
  private void copyLinkedDataResponseRelevantHeaders(final HttpHeaders fromHeaders, final HttpServletResponse toResponse) {
    for (String headerName : fromHeaders.keySet()) {
      for (String headerValue : fromHeaders.get(headerName)) {

          if ((headerName != "Transfer-Encoding") || (headerValue != "chunked")) {
            //we allow all transfer codings except chunked, because we don't do chunking here!
            toResponse.addHeader(headerName, headerValue);
          }
      }
    }
  }

  /**
   * Extract all headers that are relevant in a request for linked data resource
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
    return headers;
  }

  private void copyHeader(final String headerName, final HttpServletRequest fromRequest, final HttpHeaders toHeaders) {
    Enumeration<String> values = fromRequest.getHeaders(headerName);
    while (values.hasMoreElements()) {
      toHeaders.add(headerName, values.nextElement());
    }
  }


  /**
   * Check if the current user has the claimed identity represented by web-id of the need.
   * I.e. if the identity is that of the need that belongs to the user - return true, otherwise - false.
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
    return  needUris;
  }


}

