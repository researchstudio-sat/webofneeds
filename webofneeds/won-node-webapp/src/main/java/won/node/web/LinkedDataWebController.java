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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import won.node.service.impl.URIService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.service.LinkedDataService;
import won.protocol.util.HTTP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
@Controller
@RequestMapping("/")
public class LinkedDataWebController
{
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
  private String  nodeResourceURIPrefix;
  @Autowired
  private LinkedDataService linkedDataService;

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
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.getNeedModel(needURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", needURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(needURI).toString());
      return "rdfModelView";
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
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.getConnectionModel(connectionURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", connectionURI.toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(connectionURI).toString());
      return "rdfModelView";
    } catch (NoSuchConnectionException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
    }
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.need}")
  public String showNeedURIListPage(
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listNeedURIs(page);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
      return "rdfModelView";
  }

    @RequestMapping("${uri.path.page}")
    public String showNodeInformationPage(
            @RequestParam(defaultValue="-1") int page,
            HttpServletRequest request,
            Model model,
            HttpServletResponse response) {
        com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.showNodeInformation(page);
        model.addAttribute("rdfModel", rdfModel);
        model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
        return "rdfModelView";
    }



    //webmvc controller method
  @RequestMapping("${uri.path.page.connection}")
  public String showConnectionURIListPage(
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
    com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listConnectionURIs(page);
    model.addAttribute("rdfModel", rdfModel);
    model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
    model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
    return "rdfModelView";
  }

  //webmvc controller method
  @RequestMapping("${uri.path.page.need}/{identifier}/connections/")
  public String showConnectionURIListPage(
      @PathVariable String identifier,
      @RequestParam(defaultValue="-1") int page,
      HttpServletRequest request,
      Model model,
      HttpServletResponse response) {
    URI needURI = uriService.createNeedURIForId(identifier);
    try{
      com.hp.hpl.jena.rdf.model.Model rdfModel = linkedDataService.listConnectionURIs(page,needURI);
      model.addAttribute("rdfModel", rdfModel);
      model.addAttribute("resourceURI", uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
      model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
      return "rdfModelView";
    } catch (NoSuchNeedException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return "notFoundView";
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
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> redirectToData(
      HttpServletRequest request) {
    URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
    URI dataUri = URI.create(this.dataURIPrefix);
    String requestUri = request.getRequestURI();
    String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), dataUri.getPath());
    logger.debug("resource URI requested with data mime type. redirecting from {} to {}", requestUri, redirectToURI);
    if (redirectToURI.equals(requestUri)) {
        logger.debug("redirecting to same URI avoided, sending status 500 instead");
        return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    //TODO: actually the expiry information should be the same as that of the resource that is redirected to
    HttpHeaders headers = addNeverExpiresHeaders(new HttpHeaders());
    headers.add("Location",redirectToURI);
    return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(null, headers, HttpStatus.SEE_OTHER);
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
      HttpServletRequest request) {
    URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
    URI pageUriPrefix = URI.create(this.pageURIPrefix);
    String requestUri = request.getRequestURI();

    String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), pageUriPrefix.getPath());
    logger.debug("resource URI requested with page mime type. redirecting from {} to {}", requestUri, redirectToURI);
      if (redirectToURI.equals(requestUri)) {
          logger.debug("redirecting to same URI avoided, sending status 500 instead");
          return new ResponseEntity<String>("Could not redirect to linked data page", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    //TODO: actually the expiry information should be the same as that of the resource that is redirected to
    HttpHeaders headers = addNeverExpiresHeaders(new HttpHeaders());
    headers.add("Location",redirectToURI);
    return new ResponseEntity<String>("", headers, HttpStatus.SEE_OTHER);
  }

  @RequestMapping(
      value="${uri.path.data.need}",
      method = RequestMethod.GET,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> listNeedURIs(
      HttpServletRequest request,
      @RequestParam(value="page",defaultValue = "-1") int page) {
    logger.debug("listNeedURIs() called");
    com.hp.hpl.jena.rdf.model.Model model = linkedDataService.listNeedURIs(page);
    HttpHeaders headers = addAlreadyExpiredHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), URI.create(this.needResourceURIPrefix)));
    return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);
  }

  @RequestMapping(
      value="${uri.path.data.connection}",
      method = RequestMethod.GET,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> listConnectionURIs(
      HttpServletRequest request,
      @RequestParam(value="page", defaultValue="-1") int page) {
    logger.debug("listNeedURIs() called");
    com.hp.hpl.jena.rdf.model.Model model = linkedDataService.listConnectionURIs(page);
    HttpHeaders headers = addAlreadyExpiredHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), URI.create(this.connectionResourceURIPrefix)));
    return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);
  }


  @RequestMapping(
      value="${uri.path.data.need}/{identifier}",
      method = RequestMethod.GET,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> readNeed(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier) {
    logger.debug("readNeed() called");
    URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);
    try {
      com.hp.hpl.jena.rdf.model.Model model = linkedDataService.getNeedModel(needUri);
      //TODO: need information does change over time. The immutable need information should never expire, the mutable should
      HttpHeaders headers = addNeverExpiresHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), needUri));
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);
    } catch (NoSuchNeedException e) {
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(HttpStatus.NOT_FOUND);
    }

  }

    @RequestMapping(
            value="${uri.path.data}",
            method = RequestMethod.GET,
            produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
    public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> readNode(
            HttpServletRequest request) {
        logger.debug("readNode() called");
        URI nodedUri = URI.create(this.nodeResourceURIPrefix);

            com.hp.hpl.jena.rdf.model.Model model = linkedDataService.getNodeModel();
            //TODO: need information does change over time. The immutable need information should never expire, the mutable should
            HttpHeaders headers = addNeverExpiresHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), nodedUri));
            return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);

    }

  @RequestMapping(
      value="${uri.path.data.connection}/{identifier}",
      method = RequestMethod.GET,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> readConnection(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier) {
    logger.debug("readConnection() called");
    URI connectionUri = URI.create(this.connectionResourceURIPrefix + "/" + identifier);
    try {
      com.hp.hpl.jena.rdf.model.Model model = linkedDataService.getConnectionModel(connectionUri);
      //TODO: connection information does change over time. The immutable connection information should never expire, the mutable should
      HttpHeaders headers = addNeverExpiresHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), connectionUri));
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);

    } catch (NoSuchConnectionException e) {
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(HttpStatus.NOT_FOUND);
    }
  }



  @RequestMapping(
      value="${uri.path.data.need}/{identifier}/connections",
      method = RequestMethod.GET,
      produces={"application/rdf+xml","application/x-turtle","text/turtle","text/rdf+n3","application/json","application/ld+json"})
  public ResponseEntity<com.hp.hpl.jena.rdf.model.Model> readConnectionsOfNeed(
      HttpServletRequest request,
      @PathVariable(value="identifier") String identifier,
      @RequestParam(value="page",defaultValue = "-1") int page) {
    logger.debug("readConnectionsOfNeed() called");
    URI needUri = URI.create(this.needResourceURIPrefix + "/" + identifier);

    try {
      com.hp.hpl.jena.rdf.model.Model model = linkedDataService.listConnectionURIs(page, needUri);
      HttpHeaders headers = addAlreadyExpiredHeaders(addLocationHeaderIfNecessary(new HttpHeaders(), URI.create(request.getRequestURI()), needUri));
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(model, headers, HttpStatus.OK);
    } catch (NoSuchNeedException e) {
      return new ResponseEntity<com.hp.hpl.jena.rdf.model.Model>(HttpStatus.NOT_FOUND);
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
    cal.set(Calendar.YEAR,cal.get(Calendar.YEAR)+1);
    return cal.getTime();
  }



  public void setLinkedDataService(final LinkedDataService linkedDataService)
  {
    this.linkedDataService = linkedDataService;
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
}
