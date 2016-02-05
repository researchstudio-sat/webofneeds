/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.web;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import won.cryptography.service.RegistrationServer;
import won.node.service.impl.URIService;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonProtocolException;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.CNT;
import won.protocol.vocabulary.HTTP;
import won.protocol.vocabulary.WONMSG;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TODO: check the working draft here and see to conformance:
 * http://www.w3.org/TR/ldp/
 * TODO: edit according to the latest version of the spec
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
@Controller
@RequestMapping("/")
public class
  LinkedDataWebController
{
  final Logger logger = LoggerFactory.getLogger(getClass());
  //full prefix of a need resource
  private String needResourceURIPrefix;
  //path of a need resource
  private String needResourceURIPath;
  //full prefix of a connection resource
  private String connectionResourceURIPrefix;
  //path of a connection resource
  private String connectionResourceURIPath;
  //prefix for URISs of RDF data
  private String dataURIPrefix;
  //prefix for URIs referring to real-world things
  private String resourceURIPrefix;
  //prefix for human readable pages
  private String pageURIPrefix;
  private String  nodeResourceURIPrefix;
  @Autowired
  private LinkedDataService linkedDataService;

  @Autowired
  private RegistrationServer registrationServer;

  //date format for Expires header (rfc 1123)
  private static final String DATE_FORMAT_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z";


  @Autowired
  private URIService uriService;



  @RequestMapping(value="/", method = RequestMethod.GET)
  public String showIndexPage(){
    return "index";
  }



    //webmvc controller method
  @RequestMapping("${uri.path.page.need}/{identifier}")
  public String showNeedPage(@PathVariable String identifier, Model model, HttpServletResponse response) {
    try {
      URI needURI = uriService.createNeedURIForId(identifier);
      Dataset rdfDataset = linkedDataService.getNeedDataset(needURI);
      model.addAttribute("rdfDataset", rdfDataset);
      model.addAttribute("resourceURI", needURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(needURI).toString());
      return "rdfDatasetView";
    } catch (NoSuchNeedException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.connection}/{identifier}")
  public String showConnectionPage(@PathVariable String identifier, Model model, HttpServletResponse response) {
    try {
      URI connectionURI = uriService.createConnectionURIForId(identifier);
      Dataset rdfDataset = linkedDataService.getConnectionDataset(connectionURI, true);
      model.addAttribute("rdfDataset", rdfDataset);
      model.addAttribute("resourceURI", connectionURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(connectionURI).toString());
      return "rdfDatasetView";
    } catch (NoSuchConnectionException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.connection}/{identifier}/events")
  public String showConnectionEventsPage(@PathVariable String identifier, Model model, HttpServletResponse response) {

    try {
      URI connectionURI = uriService.createConnectionURIForId(identifier);
      String eventsURI = connectionURI.toString() + "/events";
      Dataset rdfDataset = linkedDataService.listConnectionEventURIs(connectionURI);
      model.addAttribute("rdfDataset", rdfDataset);
      model.addAttribute("resourceURI", eventsURI);
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(eventsURI)).toString());
      return "rdfDatasetView";
    } catch (NoSuchConnectionException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.event}/{identifier}")
  public String showEventPage(@PathVariable(value = "identifier") String identifier,
                              Model model,
                              HttpServletResponse response) {
    URI eventURI = uriService.createEventURIForId(identifier);
    Dataset rdfDataset = linkedDataService.getDatasetForUri(eventURI);
    if (model != null) {
      model.addAttribute("rdfDataset", rdfDataset);
      model.addAttribute("resourceURI", eventURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(eventURI).toString());
      return "rdfDatasetView";
    } else {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

    //webmvc controller method
    @RequestMapping("${uri.path.page.attachment}/{identifier}")
    public String showAttachmentPage(@PathVariable(value = "identifier") String identifier,
                                Model model,
                                HttpServletResponse response) {
        URI attachmentURI = uriService.createAttachmentURIForId(identifier);
        Dataset rdfDataset = linkedDataService.getDatasetForUri(attachmentURI);
        if (model != null) {
            model.addAttribute("rdfDataset", rdfDataset);
            model.addAttribute("resourceURI", attachmentURI.toString());
            model.addAttribute("dataURI", uriService.toDataURIIfPossible(attachmentURI).toString());
            return "rdfDatasetView";
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
    }

  //webmvc controller method
  @RequestMapping("${uri.path.page.need}")
  public String showNeedURIListPage(
      @RequestParam(value="p", required=false) Integer page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response)  throws IOException {

    Dataset rdfDataset = null;

    // TODO keep consistent with linked data paged resource behavior when no page is specified
      if (page == null) {
        //String redirectToURI = getRequestUriWithAddedQuery(request, "p=1");
        //response.sendRedirect(redirectToURI);
        //return null;
        // temporarily leave the behavior of returning all the need uris - for compatibility with matcher crawler
        rdfDataset = linkedDataService.listNeedURIs();
      } else {
        // TODO probably at least the Link to the next/previous page should be added to the headers, as in the case of RDF
        // returned resource

        rdfDataset = linkedDataService.listNeedURIs(page).getContent();

      }

    model.addAttribute("rdfDataset", rdfDataset);
    model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
    model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
    return "rdfDatasetView";

  }

    @RequestMapping("${uri.path.page}")
    public String showNodeInformationPage(
            HttpServletRequest request,
            Model model,
            HttpServletResponse response) {
        Dataset rdfDataset = linkedDataService.getNodeDataset();
        model.addAttribute("rdfDataset", rdfDataset);
        model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
        return "rdfDatasetView";
    }



    //webmvc controller method
  @RequestMapping("${uri.path.page.connection}")
  public String showConnectionURIListPage(
      @RequestParam(value="p",defaultValue = "-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
    Dataset rdfDataset = linkedDataService.listConnectionURIs(page);
    model.addAttribute("rdfDataset", rdfDataset);
    model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
    model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
    return "rdfDatasetView";
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.need}/{identifier}/connections/")
  public String showConnectionURIListPage(
      @PathVariable String identifier,
      @RequestParam(value="p",defaultValue = "-1") int page,
      @RequestParam(value="deep",defaultValue = "false") boolean deep,
      HttpServletRequest request,
      Model webmvcModel,
      HttpServletResponse response) {
    URI needURI = uriService.createNeedURIForId(identifier);
    try{
      Dataset dataset = linkedDataService.listConnectionURIs(page, needURI);
        if (deep){
            addDeepConnectionData(needURI.toString(), dataset);
        }
      webmvcModel.addAttribute("rdfDataset", dataset);
      webmvcModel.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
      webmvcModel.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
      return "rdfDatasetView";
    } catch (NoSuchNeedException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    } catch (NoSuchConnectionException e) {
        logger.warn("did not find connection that should be connected to need. connection:{}",
                    e.getUnknownConnectionURI());
        return "notFoundView"; //TODO: should display an error view
    }
  }

    private void addDeepConnectionData(String needUri, Dataset dataset) throws NoSuchConnectionException {
        //add the connection model to each connection
        //TODO: use a more principled way to find the connections resource!
        Resource connectionsResource = dataset.getDefaultModel().getResource(needUri + "/connections/");
        NodeIterator it = dataset.getDefaultModel().listObjectsOfProperty(connectionsResource, RDFS.member);
        while (it.hasNext()){
            RDFNode node = it.next();
            Dataset connectionDataset =
                    this.linkedDataService.getConnectionDataset(URI.create(node.asResource().getURI()), false); //do not include event data
            RdfUtils.addDatasetToDataset(dataset, connectionDataset);
        }
    }

  /**
   * If the HTTP 'Accept' header is an RDF MIME type
   * (as listed in the 'produces' value of the RequestMapping annotation),
   * a redirect to a data uri is sent.
   * @param request
   * @return
   */
    @RequestMapping(
      value="${uri.path.resource}/**",
      method = RequestMethod.GET,
      produces={"application/ld+json",
                "application/trig",
                "application/n-quads",
                "*/*"})
  public ResponseEntity<String> redirectToData(
      HttpServletRequest request, HttpServletResponse response) throws IOException {
    URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
    URI dataUri = URI.create(this.dataURIPrefix);
    String requestUri = getRequestUriWithQueryString(request);
    String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), dataUri.getPath());
    logger.debug("resource URI requested with data mime type. redirecting from {} to {}", requestUri, redirectToURI);
    if (redirectToURI.equals(requestUri)) {
        logger.debug("redirecting to same URI avoided, sending status 500 instead");
        return new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    //TODO: actually the expiry information should be the same as that of the resource that is redirected to
    HttpHeaders headers = new HttpHeaders();
    headers = addExpiresHeadersBasedOnRequestURI(headers, requestUri);
    //headers.setLocation(URI.create(redirectToURI));
    addCORSHeader(headers);
      setResponseHeaders(response, headers);
      response.sendRedirect(redirectToURI);
    return null;
  }

  public void setResponseHeaders(final HttpServletResponse response, final HttpHeaders headers) {
    for(Map.Entry<String, List<String>> entry : headers.entrySet()){
      for (String value : entry.getValue()) {
        response.setHeader(entry.getKey(), value);
      }
    }
  }

  private String getRequestUriWithQueryString(final HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String queryString = request.getQueryString();
    if (queryString != null){
      requestUri += "?" + queryString;
    }
    return requestUri;
  }

  private String getRequestUriWithAddedQuery(final HttpServletRequest request, String query) {
    String requestUri = request.getRequestURI();
    String queryString = request.getQueryString();
    if (queryString == null || queryString.length() <= 2) {
      requestUri += "?" + query;
    } else {
      requestUri += "?" + queryString + "&" + query;
    }
    return requestUri;
  }


  /**
     * If the HTTP 'Accept' header is 'text/html'
     * (as listed in the 'produces' value of the RequestMapping annotation),
     * a redirect to a page uri is sent.
     * @param request
     * @return
     */
  @RequestMapping(
      value="${uri.path.resource}/**",
      method = RequestMethod.GET,
      produces="text/html")
  public ResponseEntity<String> redirectToPage(
      HttpServletRequest request, HttpServletResponse response)  throws IOException {
    URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
    URI pageUriPrefix = URI.create(this.pageURIPrefix);
    String requestUri = getRequestUriWithQueryString(request);
    String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), pageUriPrefix.getPath());
    logger.debug("resource URI requested with page mime type. redirecting from {} to {}", requestUri, redirectToURI);
    if (redirectToURI.equals(requestUri)) {
        logger.debug("redirecting to same URI avoided, sending status 500 instead");
        return new ResponseEntity<String>("\"Could not redirect to linked data page\"", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    //TODO: actually the expiry information should be the same as that of the resource that is redirected to
    HttpHeaders headers = new HttpHeaders();
    headers = addExpiresHeadersBasedOnRequestURI(headers, requestUri);
    addCORSHeader(headers);
    //add a location header
    //headers.add("Location",redirectToURI);
    setResponseHeaders(response, headers);
    response.sendRedirect(redirectToURI);
    return null;
  }

  /**
   * If the request URI is the URI of a list page (list of needs, list of connections) it gets the
   * header that says 'already expired' so that crawlers re-download these data. For other URIs, the
   * 'never expires' header is added.
   * @param headers
   * @param requestUri
   * @return
   */
  public HttpHeaders addExpiresHeadersBasedOnRequestURI(HttpHeaders headers, final String requestUri) {
    //now, we want to suppress the 'never expires' header information
    //for /resource/need and resource/connection so that crawlers always re-fetch these data
    URI requestUriAsURI = URI.create(requestUri);
    String requestPath = requestUriAsURI.getPath();
    if (! (requestPath.replaceAll("/$","").endsWith(this.connectionResourceURIPath.replaceAll("/$", "")) ||
           requestPath.replaceAll("/$","").endsWith(this.needResourceURIPath.replaceAll("/$", "")))) {

    } else {
      headers = addAlreadyExpiredHeaders(headers);
    }
    return headers;
  }

  @RequestMapping(
    value="${uri.path.data.need}",
    method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> listNeedURIs(HttpServletRequest request, HttpServletResponse response,
    @RequestParam(value="p", required=false) Integer page) throws IOException {
    logger.debug("listNeedURIs() for page " + page + " called");

    Dataset rdfDataset = null;
    HttpHeaders headers = new HttpHeaders();
    if (page == null) {
      //by default we redirect to the first page
      //TODO although for us it would make sense to redirect to the last one, as matcher, owner gui, etc. would be
      // interested mostly in the latest needs/events... How to do it?
      // (so that we in accordance with the specification https://www.w3.org/TR/ldp-paging/)
      //String redirectToURI = getRequestUriWithAddedQuery(request, "p=1");
      //response.sendRedirect(redirectToURI);
      //return null;
      // temporarily leave the behavior of returning all the need uris - for compatibility with matcher crawler:
      rdfDataset = linkedDataService.listNeedURIs();
    } else {
      //Dataset model = linkedDataService.listNeedURIs(page);
      NeedInformationService.PagedResource<Dataset> resource = linkedDataService.listNeedURIs(page);
      rdfDataset = resource.getContent();
      addPagedResourceInSequenceHeader(headers, URI.create(this.needResourceURIPrefix), page, resource.hasNext());
    }
    headers = addAlreadyExpiredHeaders(
      addLocationHeaderIfNecessary(headers, URI.create(request.getRequestURI()),
                                   URI.create(this.needResourceURIPrefix)));
    addCORSHeader(headers);

    return new ResponseEntity<Dataset>(rdfDataset, headers, HttpStatus.OK);

  }


  @RequestMapping(
      value="${uri.path.data.connection}",
      method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> listConnectionURIs(
      HttpServletRequest request,
      @RequestParam(value="p", defaultValue="-1") int page) {
    logger.debug("listNeedURIs() called");
    Dataset model = linkedDataService.listConnectionURIs(page);
    HttpHeaders headers = addAlreadyExpiredHeaders(
      addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()),
                                   URI.create(this.connectionResourceURIPrefix)));
    addCORSHeader(headers);
    return new ResponseEntity<Dataset>(model, headers, HttpStatus.OK);
  }


  @RequestMapping(
      value="${uri.path.data.need}/{identifier}",
      method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> readNeed(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier) {
    logger.debug("readNeed() called");
    URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);
    try {
      Dataset dataset = linkedDataService.getNeedDataset(needUri);
      //TODO: need information does change over time. The immutable need information should never expire, the mutable should
      HttpHeaders headers = new HttpHeaders();
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(dataset, headers, HttpStatus.OK);
    } catch (NoSuchNeedException e) {

      return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
    }

  }

    @RequestMapping(
        value="${uri.path.data}",
        method = RequestMethod.GET,
      produces={"application/ld+json",
                "application/trig",
                "application/n-quads"})
    public ResponseEntity<Dataset> readNode(
            HttpServletRequest request) {
        logger.debug("readNode() called");
        URI nodeUri = URI.create(this.nodeResourceURIPrefix);
        Dataset model = linkedDataService.getNodeDataset();
        //TODO: need information does change over time. The immutable need information should never expire, the mutable should
        HttpHeaders headers = new HttpHeaders();
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(model, headers, HttpStatus.OK);
    }

  @RequestMapping(
      value="${uri.path.data.connection}/{identifier}",
      method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> readConnection(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier) {
    logger.debug("readConnection() called");
    URI connectionUri = URI.create(this.connectionResourceURIPrefix + "/" + identifier);
    try {
      Dataset model = linkedDataService.getConnectionDataset(connectionUri, true);
      //TODO: connection information does change over time. The immutable connection information should never expire, the mutable should
      HttpHeaders headers =new HttpHeaders();
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(model, headers, HttpStatus.OK);

    } catch (NoSuchConnectionException e) {
      return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
    }
  }


  @RequestMapping(
    value="${uri.path.data.connection}/{identifier}/events",
    method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> readConnectionEvents(
    HttpServletRequest request,
    @PathVariable(value="identifier") String identifier) {
    logger.debug("readConnection() called");
    URI connectionUri = URI.create(this.connectionResourceURIPrefix + "/" + identifier);
    try {
      Dataset dataset = linkedDataService.listConnectionEventURIs(connectionUri);
      //TODO: events list information does change over time, unless the connection is closed and cannot be reopened.
      // The events list of immutable connection information should never expire, the mutable should
      HttpHeaders headers =new HttpHeaders();
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(dataset, headers, HttpStatus.OK);

    } catch (NoSuchConnectionException e) {
      return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
    }
  }


  @RequestMapping(
    value="${uri.path.data.event}/{identifier}",
    method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> readEvent(
    HttpServletRequest request,
    @PathVariable(value = "identifier") String identifier) {
    logger.debug("readConnectionEvent() called");

    URI eventURI = uriService.createEventURIForId(identifier);
    Dataset rdfDataset = linkedDataService.getDatasetForUri(eventURI);
    if (rdfDataset != null) {
      HttpHeaders headers = new HttpHeaders();
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(rdfDataset, headers, HttpStatus.OK);
    } else {
      return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
    }

  }

    @RequestMapping(
            value="${uri.path.data.attachment}/{identifier}",
            method = RequestMethod.GET,
            produces={"application/ld+json",
                    "application/trig",
                    "application/n-quads",
                    "*/*"})
    public ResponseEntity<Dataset> readAttachment(
            HttpServletRequest request,
            @PathVariable(value = "identifier") String identifier) {
        logger.debug("readAttachment() called");

        URI attachmentURI = uriService.createAttachmentURIForId(identifier);
        Dataset rdfDataset = linkedDataService.getDatasetForUri(attachmentURI);
        if (rdfDataset != null) {
            HttpHeaders headers = new HttpHeaders();
            addCORSHeader(headers);
          String mimeTypeOfResponse = RdfUtils.findFirst(rdfDataset, new RdfUtils.ModelVisitor<String>() {
            @Override
            public String visit(com.hp.hpl.jena.rdf.model.Model model) {
              String content = getObjectOfPropertyAsString(model, CNT.BYTES);
              if (content == null) return null;
              String contentType = getObjectOfPropertyAsString(model, WONMSG.CONTENT_TYPE);
              return contentType;
            }
          });
          if (mimeTypeOfResponse != null){
            //we found a base64 encoded attachment, we obtained its contentType, so we set it as the
            //contentType of the response.
            Set<MediaType> producibleMediaTypes = new HashSet<>();
            producibleMediaTypes.add(MediaType.valueOf(mimeTypeOfResponse));
            request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, producibleMediaTypes);
          }
          return new ResponseEntity<Dataset>(rdfDataset, headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
        }
    }

  private String getObjectOfPropertyAsString(com.hp.hpl.jena.rdf.model.Model model, Property property){
    NodeIterator nodeIteratr = model.listObjectsOfProperty(property);
    if (!nodeIteratr.hasNext()) return null;
    String ret = nodeIteratr.next().asLiteral().getString();
    if (nodeIteratr.hasNext()) {
      throw new IncorrectPropertyCountException("found more than one property of cnt:bytes", 1, 2);
    }
    return ret;
  }
    

    /**
     * Get the RDF for the connections of the specified need.
     * @param request
     * @param identifier
     * @param page if used, returns the specified page number.
     * @param deep If true, connection data is added to the model (not only connection URIs). Default: false.
     * @return
     */
  @RequestMapping(
      value="${uri.path.data.need}/{identifier}/connections",
      method = RequestMethod.GET,
    produces={"application/ld+json",
              "application/trig",
              "application/n-quads"})
  public ResponseEntity<Dataset> readConnectionsOfNeed(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier,
      @RequestParam(value="p",defaultValue = "-1") int page,
      @RequestParam(value="deep",defaultValue = "false") boolean deep) {
    logger.debug("readConnectionsOfNeed() called");
    URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);

    try {
        Dataset model = null;
        HttpHeaders headers = null;
        model = linkedDataService.listConnectionURIs(page, needUri);
        if (deep){
            //add the connection model to each connection
            addDeepConnectionData(needUri.toString(), model);
        }
        //append the required headers
        headers = addAlreadyExpiredHeaders(addLocationHeaderIfNecessary(
                new HttpHeaders(), URI.create(request.getRequestURI()), needUri));
      addCORSHeader(headers);
      return new ResponseEntity<Dataset>(model, headers, HttpStatus.OK);
    } catch (NoSuchNeedException e) {
      return new ResponseEntity<Dataset>(HttpStatus.NOT_FOUND);
    } catch (NoSuchConnectionException e) {
      logger.warn("did not find connection that should be connected to need. connection:{}",e.getUnknownConnectionURI());
      return new ResponseEntity<Dataset>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }




  /**
   * Checks if the actual URI is the same as the canonical URI; if not, adds a Location header to the response builder
   * indicating the canonical URI.
   * @param headers
   * @param actualURI
   * @param canonicalURI
   * @return the headers map with added header values
   */
  private HttpHeaders addLocationHeaderIfNecessary(HttpHeaders headers, URI actualURI, URI canonicalURI){
    if(!canonicalURI.resolve(actualURI).equals(canonicalURI)) {
      //the request URI is the canonical URI, it may be a DNS alias or relative
      //according to http://www.w3.org/TR/ldp/#general we have to include
      //the canonical URI in the lcoation header here
      headers.add(HTTP.HEADER_LOCATION, canonicalURI.toString());
    }
    return headers;
  }


  /**
   * Adds headers describing the paged resource according to https://www.w3.org/TR/ldp-paging/
   * (here implemented version is http://www.w3.org/TR/2015/NOTE-ldp-paging-20150630/)
   * that inform the client about the following properties of the pages resource:
   *
   * Link: <uri>; rel="canonical"; etag="tag" - which resource it is a page of, and current tag of the resource
   * Link: <http://www.w3.org/ns/ldp#Page>; rel="type" - that this is one in-sequence page resource
   * Link: <http://www.w3.org/ns/ldp#Resource>; rel="type" - that this is a LDP Resource (should be Container in our case?)
   * Link: <uri?p=x>; rel="next" - that the next in-sequence page resource exists and is retrievable at the given uri
   *
   * @param headers headers to which paged resource headers should be added
   * @param canonicalURI uri of the LDP Resource
   * @param page page of the Paged LDP Resource
   * @param hasNext whether more pages exist
   * @return the headers map with added header values
   */
  private void addPagedResourceInSequenceHeader(final HttpHeaders headers, final URI canonicalURI, final int page,
                                                final boolean hasNext) {

    headers.add("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\", <http://www.w3.org/ns/ldp#Page>; rel=\"type\"");
    //Link: <http://example.org/customer-relations?p=2>; rel="next"
    if (hasNext) {
      int nextPage = page + 1;
      headers.add("Link", "<" + canonicalURI.toString() + "?p=" + nextPage + ">; rel=\"next\"");
    }
    headers.add("Link", "<" + canonicalURI.toString() + ">; rel=\"canonical\"");

  }

  /**
   * Sets the Date and Expires header fields such that the response will be treated as 'never expires'
   * (and will therefore be cached forever)
   * @param headers
   * @return the headers map with added header values
   */
  private HttpHeaders addNeverExpiresHeaders(HttpHeaders headers){
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123);
    headers.add(HTTP.HEADER_EXPIRES, dateFormat.format(getNeverExpiresDate()));
    headers.add(HTTP.HEADER_DATE, dateFormat.format(new Date()));
    return headers;
  }

  /**
   * Sets the Date and Expires header fields such that the response will be treated as 'already expired'
   * (and will therefore not be cached)
   * @param headers
   * @return the headers map with added header values
   */
  private  HttpHeaders addAlreadyExpiredHeaders(HttpHeaders headers){
    Date headerDate = new Date();
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123);
    String formattedDate = dateFormat.format(headerDate);
    headers.add(HTTP.HEADER_EXPIRES, formattedDate);
    headers.add(HTTP.HEADER_DATE, formattedDate);
    return headers;
  }

  //Calculates a date that, according to http spec, means 'never expires'
  //See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
  private Date getNeverExpiresDate(){
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
    return cal.getTime();
  }

  /**
   * Adds the CORS headers required for client side cross-site requests.
   * See http://www.w3.org/TR/cors/
   * @param headers
   */
  private void addCORSHeader(final HttpHeaders headers) {
    headers.add("Access-Control-Allow-Origin", "*");
  }




  public void setLinkedDataService(final LinkedDataService linkedDataService)
  {
    this.linkedDataService = linkedDataService;
  }

  public void setRegistrationServer(final RegistrationServer registrationServer) {
    this.registrationServer = registrationServer;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
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

  public void setPageURIPrefix(final String pageURIPrefix) {
    this.pageURIPrefix = pageURIPrefix;
  }

    public String getNodeResourceURIPrefix() {
        return nodeResourceURIPrefix;
    }

    public void setNodeResourceURIPrefix(String nodeResourceURIPrefix) {
        this.nodeResourceURIPrefix = nodeResourceURIPrefix;
    }

  public void setNeedResourceURIPath(final String needResourceURIPath) {
    this.needResourceURIPath = needResourceURIPath;
  }

  public void setConnectionResourceURIPath(final String connectionResourceURIPath) {
    this.connectionResourceURIPath = connectionResourceURIPath;
  }

  @RequestMapping(
    value="${uri.path.resource}",
    method = RequestMethod.POST,
    produces={"text/plain"})
  public ResponseEntity<String> register(@RequestParam("register") String registeredType, HttpServletRequest
    request) {

    logger.debug("REGISTERING " + registeredType);
    String supportedTypesMsg = "Request parameter error; supported 'register' parameter values: 'owner', 'node'";

    if (registeredType == null) {
      logger.warn(supportedTypesMsg);
      return new ResponseEntity<String>(supportedTypesMsg, HttpStatus.BAD_REQUEST);
    }

    Object certificateChainObj = request.getAttribute("javax.servlet.request.X509Certificate");

    try {
      if (registeredType.equals("owner")) {
        String result = registrationServer.registerOwner(certificateChainObj);
        return new ResponseEntity<String>(result, HttpStatus.OK);
      }
      if (registeredType.equals("node")) {
        String result = registrationServer.registerNode(certificateChainObj);
        return new ResponseEntity<String>(result, HttpStatus.OK);
      } else {
        logger.warn(supportedTypesMsg);
        return new ResponseEntity<String>(supportedTypesMsg, HttpStatus.BAD_REQUEST);
      }
    } catch (WonProtocolException e) {
      logger.warn("Could not register " + registeredType, e);
      return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

  }
}
