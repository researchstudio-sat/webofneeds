/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.node.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import won.cryptography.service.RegistrationServer;
import won.node.service.linkeddata.generate.LinkedDataService;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.AtomInformationService;
import won.node.springsecurity.acl.WonAclRequestHelper;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.WonProtocolException;
import won.protocol.message.WonMessageType;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.DataWithEtag;
import won.protocol.rest.WonEtagHelper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.CNT;
import won.protocol.vocabulary.HTTP;
import won.protocol.vocabulary.WONMSG;

/**
 * TODO: check the working draft here and see to conformance:
 * http://www.w3.org/TR/ldp/ TODO: edit according to the latest version of the
 * spec Not met yet:
 * <p>
 * 4.1.13 LDPR server responses must contain accurate response ETag header
 * values.
 * <p>
 * add dcterms:modified and dcterms:creator
 * <p>
 * 4.4 HTTP PUT - we don't support that. especially: 4.4.1 If HTTP PUT is
 * performed ... (we do that using the owner protocol)
 * <p>
 * 4.4.2 LDPR clients should use the HTTP If-Match header and HTTP ETags to
 * ensure ...
 * <p>
 * 4.5 HTTP DELETE - we don't support that.
 * <p>
 * 4.6 HTTP HEAD - do we support that?
 * <p>
 * 4.7 HTTP PATCH - we don't support that.
 * <p>
 * 4.8 Common Properties - use common properties!!
 * <p>
 * 5.1.2 Retrieving Only Non-member Properties - not supported (would have to be
 * changed in LinkedDataServiceImpl
 * <p>
 * see 5.3.2 LDPC - send 404 when non-member-properties is not supported...
 * <p>
 * <p>
 * 5.3.3 first page request: if a Request-URI of
 * “{@literal <containerURL>}?firstPage” is not supported {@literal -->} 404
 * <p>
 * 5.3.4 support the firstPage query param
 * <p>
 * 5.3.5 server initiated paging is a good idea (see 5.3.5.1 )
 * <p>
 * 5.3.7 ordering
 */
@Controller
@RequestMapping("/")
public class LinkedDataWebController implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // full prefix of an atom resource
    private String atomResourceURIPrefix;
    // full prefix of a connection resource
    private String connectionResourceURIPrefix;
    // prefix for URISs of RDF data
    private String dataURIPrefix;
    // prefix for URIs referring to real-world things
    private String resourceURIPrefix;
    // prefix for human readable pages
    private String pageURIPrefix;
    private String nodeResourceURIPrefix;
    @Autowired
    private LinkedDataService linkedDataService;
    @Autowired
    private RegistrationServer registrationServer;
    // date format for Expires header (rfc 1123)
    private static final String DATE_FORMAT_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z";
    // timeout for resources that clients may cache for a short term
    private static final int SHORT_TERM_CACHE_TIMEOUT_SECONDS = 600;
    @Autowired
    private URIService uriService;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.atomResourceURIPrefix = this.nodeResourceURIPrefix + "/atom";
        this.connectionResourceURIPrefix = this.nodeResourceURIPrefix + "/connection";
        logger.info("setting prefixes: atom: {}, connection: {}, ", new Object[] { this.atomResourceURIPrefix,
                        this.connectionResourceURIPrefix });
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String showIndexPage() {
        return "index";
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom/{identifier}")
    public String showAtomPage(@PathVariable String identifier, Model model,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        URI atomURI = uriService.createAtomURIForId(identifier);
        Dataset rdfDataset = linkedDataService
                        .getAtomDataset(atomURI, null, WonAclRequestHelper.getWonAclEvaluationContext(request))
                        .getData();
        if (rdfDataset == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
        // TODO AUTH: extract acl graph and interpret
        model.addAttribute("rdfDataset", rdfDataset);
        model.addAttribute("resourceURI", atomURI.toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(atomURI).toString());
        return "rdfDatasetView";
    }

    /**
     * This request URL should be protected by WebID filter because the result
     * contains events data - which is data with restricted access. See
     * filterChainProxy in node-context.xml.
     *
     * @param identifier
     * @param model
     * @param response
     * @return
     */
    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom/{identifier}/deep")
    public String showDeepAtomPage(@PathVariable String identifier, Model model,
                    HttpServletRequest request, HttpServletResponse response,
                    @RequestParam(value = "layer-size", required = false) Integer layerSize) {
        try {
            URI atomURI = uriService.createAtomURIForId(identifier);
            Dataset rdfDataset = linkedDataService.getAtomDataset(atomURI, true, layerSize,
                            WonAclRequestHelper.getWonAclEvaluationContext(request));
            // TODO AUTH: extract acl graph and interpret
            // TODO AUTH: Research how to handle lower layers
            logger.warn("TODO: apply ACLs to atom/{identifier}/deep request!");
            model.addAttribute("rdfDataset", rdfDataset);
            model.addAttribute("resourceURI", atomURI.toString());
            model.addAttribute("dataURI", uriService.toDataURIIfPossible(atomURI).toString());
            return "rdfDatasetView";
        } catch (NoSuchAtomException | NoSuchConnectionException | NoSuchMessageException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom/{atomId}/c/{identifier}")
    public String showConnectionPage(@PathVariable String atomId, @PathVariable String identifier, Model model,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        URI connectionURI = uriService.createConnectionURIForId(atomId, identifier);
        DataWithEtag<Dataset> rdfDataset = linkedDataService.getConnectionDataset(connectionURI, true, null);
        if (rdfDataset.isNotFound()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
        model.addAttribute("rdfDataset", rdfDataset.getData());
        model.addAttribute("resourceURI", connectionURI.toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(connectionURI).toString());
        return "rdfDatasetView";
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom/{atomId}/c/{identifier}/msg")
    public String showConnectionEventsPage(
                    @PathVariable String atomId,
                    @PathVariable String identifier,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "type", required = false) String type,
                    @RequestParam(value = "deep", required = false, defaultValue = "false") boolean deep, Model model,
                    HttpServletRequest request,
                    HttpServletResponse response) {
        try {
            URI connectionURI = uriService.createConnectionURIForId(atomId, identifier);
            String eventsURI = connectionURI.toString() + "/msg";
            Dataset rdfDataset;
            WonMessageType msgType = getMessageType(type);
            if (page == null && resumeBefore == null && resumeAfter == null) {
                // all events, does not support type filtering for clients that do not support
                // paging
                rdfDataset = linkedDataService.listConnectionEventURIs(connectionURI, deep);
            } else if (page != null) {
                // a page having particular page number is requested
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                                .listConnectionEventURIs(connectionURI, page, null, msgType, deep);
                rdfDataset = resource.getContent();
            } else if (resumeBefore != null) {
                // a page that precedes the item identified by the resumeBefore is requested
                URI referenceEvent;
                try {
                    referenceEvent = new URI(resumeBefore);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeBefore must be a full, valid message URI");
                }
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                                .listConnectionEventURIsAfter(connectionURI, referenceEvent, null, msgType, deep);
                rdfDataset = resource.getContent();
            } else {
                // a page that follows the item identified by the resumeAfter is requested
                URI referenceEvent;
                try {
                    referenceEvent = new URI(resumeAfter);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeAfter must be a full, valid message URI");
                }
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                                .listConnectionEventURIsBefore(connectionURI, referenceEvent, null, msgType, deep);
                rdfDataset = resource.getContent();
            }
            model.addAttribute("rdfDataset", rdfDataset);
            model.addAttribute("resourceURI", eventsURI);
            model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(eventsURI)).toString());
            return "rdfDatasetView";
        } catch (NoSuchConnectionException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
    }

    /**
     * This request URL should be protected by WebID filter because the result
     * contains events data - which is data with restricted access. See
     * filterChainProxy in node-context.xml.
     *
     * @param identifier
     * @param model
     * @param response
     * @return
     */
    // webmvc controller method
    @RequestMapping("${uri.path.page}/msg/{identifier}")
    public String showEventPage(@PathVariable(value = "identifier") String identifier, Model model,
                    HttpServletResponse response) {
        URI eventURI = uriService.createMessageURIForId(identifier);
        return createDatasetResponse(model, response, eventURI);
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/attachment/{identifier}")
    public String showAttachmentPage(@PathVariable(value = "identifier") String identifier, Model model,
                    HttpServletResponse response) {
        // TODO AUTH: load atom dataset
        // TODO AUTH: extract acl graph and interpret
        URI attachmentURI = uriService.createAttachmentURIForId(identifier);
        return createDatasetResponse(model, response, attachmentURI);
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom")
    public String showAtomURIListPage(@RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "modifiedafter", required = false) String modifiedAfter,
                    @RequestParam(value = "createdafter", required = false) String createdAfter,
                    @RequestParam(value = "filterBySocketTypeUri", required = false) String filterBySocketTypeUriString,
                    @RequestParam(value = "filterByAtomTypeUri", required = false) String filterByAtomTypeUriString,
                    @RequestParam(value = "state", required = false) String state, HttpServletRequest request,
                    Model model, HttpServletResponse response) throws IOException, ParseException {
        Dataset rdfDataset;
        AtomState atomState = getAtomState(state);
        URI filterBySocketTypeUri = getURIOrNull(filterBySocketTypeUriString);
        URI filterByAtomTypeUri = getURIOrNull(filterByAtomTypeUriString);
        if (page == null && resumeBefore == null && resumeAfter == null && modifiedAfter == null
                        && createdAfter == null) {
            rdfDataset = linkedDataService.listAtomURIs(atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        } else if (page != null) {
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService.listPagedAtomURIs(page,
                            null,
                            atomState);
            rdfDataset = resource.getContent();
        } else if (resumeBefore != null) {
            URI referenceAtom;
            try {
                referenceAtom = new URI(resumeBefore);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("resumeBefore must be a full, valid atom URI");
            }
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                            .listPagedAtomURIsAfter(referenceAtom, null, atomState);
            rdfDataset = resource.getContent();
        } else if (resumeAfter != null) { // resumeAfter != null
            URI referenceAtom;
            try {
                referenceAtom = new URI(resumeAfter);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("resumeAfter must be a full, valid atom URI");
            }
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                            .listPagedAtomURIsBefore(referenceAtom, null, atomState);
            rdfDataset = resource.getContent();
        } else if (createdAfter != null) {
            // do not support paging for now
            rdfDataset = linkedDataService.listAtomURIsCreatedAfter(new DateParameter(createdAfter).getDate(),
                            atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        } else {
            // modifiedafter != null && createdafter == null
            // do not support paging for now
            rdfDataset = linkedDataService.listAtomURIsModifiedAfter(new DateParameter(modifiedAfter).getDate(),
                            atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        }
        model.addAttribute("rdfDataset", rdfDataset);
        model.addAttribute("resourceURI",
                        uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
        return "rdfDatasetView";
    }

    @RequestMapping(value = "${uri.path.data}/atom", method = RequestMethod.GET, produces = { "application/ld+json",
                    "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> listAtomURIs(HttpServletRequest request, HttpServletResponse response,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore, // assumes newest-first
                                                                                                 // ordering, newer than
                                                                                                 // this
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter, // assumes newest-first
                                                                                               // ordering, older than
                                                                                               // this
                    @RequestParam(value = "modifiedafter", required = false) String modifiedAfter,
                    @RequestParam(value = "createdafter", required = false) String createdAfter,
                    @RequestParam(value = "filterBySocketTypeUri", required = false) String filterBySocketTypeUriString,
                    @RequestParam(value = "filterByAtomTypeUri", required = false) String filterByAtomTypeUriString,
                    @RequestParam(value = "state", required = false) String state) throws IOException, ParseException {
        logger.debug("listAtomURIs() for page " + page + " called");
        Dataset rdfDataset;
        HttpHeaders headers = new HttpHeaders();
        Integer preferedSize = getPreferredSize(request);
        String passableQuery = getPassableQueryMap("state", state, "modifiedafter", modifiedAfter, "createdafter",
                        createdAfter, "filterBySocketTypeUri", filterBySocketTypeUriString, "filterByAtomTypeUri",
                        filterByAtomTypeUriString);
        AtomState atomState = getAtomState(state);
        URI filterBySocketTypeUri = getURIOrNull(filterBySocketTypeUriString);
        URI filterByAtomTypeUri = getURIOrNull(filterByAtomTypeUriString);
        if (preferedSize == null && modifiedAfter == null && createdAfter == null) {
            rdfDataset = linkedDataService.listAtomURIs(atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        } else if (page == null && resumeBefore == null && resumeAfter == null && modifiedAfter == null
                        && createdAfter == null) {
            // return latest atoms
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService.listPagedAtomURIs(1,
                            preferedSize, atomState);
            rdfDataset = resource.getContent();
            addPagedResourceInSequenceHeader(headers, URI.create(this.atomResourceURIPrefix), resource, passableQuery);
        } else if (page != null) {
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService.listPagedAtomURIs(page,
                            preferedSize, atomState);
            rdfDataset = resource.getContent();
            addPagedResourceInSequenceHeader(headers, URI.create(this.atomResourceURIPrefix), resource, passableQuery);
        } else if (resumeBefore != null) {
            // resumebefore means we are paging and we are interested in atoms newer than
            // the reference
            URI referenceAtom;
            try {
                referenceAtom = new URI(resumeBefore);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("resumeBefore must be a full, valid atom URI");
            }
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                            .listPagedAtomURIsAfter(referenceAtom, preferedSize, atomState);
            rdfDataset = resource.getContent();
            addPagedResourceInSequenceHeader(headers, URI.create(this.atomResourceURIPrefix), resource, passableQuery);
        } else if (resumeAfter != null) {
            // resumebefore means we are paging and we are interested in atoms older than
            // the reference
            URI referenceAtom;
            try {
                referenceAtom = new URI(resumeAfter);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("resumeAfter must be a full, valid atom URI");
            }
            AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                            .listPagedAtomURIsBefore(referenceAtom, preferedSize, atomState);
            rdfDataset = resource.getContent();
            addPagedResourceInSequenceHeader(headers, URI.create(this.atomResourceURIPrefix), resource, passableQuery);
        } else if (createdAfter != null) {
            // do not support paging for now
            rdfDataset = linkedDataService.listAtomURIsCreatedAfter(new DateParameter(createdAfter).getDate(),
                            atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        } else {
            // modifiedafter != null && createdafter == null
            // do not support paging for now
            rdfDataset = linkedDataService.listAtomURIsModifiedAfter(new DateParameter(modifiedAfter).getDate(),
                            atomState, filterBySocketTypeUri, filterByAtomTypeUri);
        }
        addLocationHeaderIfNecessary(headers, URI.create(request.getRequestURI()),
                        URI.create(this.atomResourceURIPrefix));
        addMutableResourceHeaders(headers);
        addCORSHeader(headers);
        return new ResponseEntity<>(rdfDataset, headers, HttpStatus.OK);
    }

    @RequestMapping("${uri.path.page}")
    public String showNodeInformationPage(HttpServletRequest request, Model model, HttpServletResponse response) {
        Dataset rdfDataset = linkedDataService.getNodeDataset();
        model.addAttribute("rdfDataset", rdfDataset);
        model.addAttribute("resourceURI",
                        uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
        model.addAttribute("dataURI", uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
        return "rdfDatasetView";
    }

    // TODO AUTH Check if we can safely delete this
    // webmvc controller method
    @RequestMapping("${uri.path.page}/connection")
    public String showConnectionURIListPage(@RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "deep", defaultValue = "false") boolean deep,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "timeof", required = false) String timestamp, HttpServletRequest request,
                    Model model, HttpServletResponse response) {
        try {
            DateParameter dateParam = new DateParameter(timestamp);
            Dataset rdfDataset;
            if (page != null) {
                rdfDataset = linkedDataService.listConnections(page, null, dateParam.getDate(), deep).getContent();
            } else if (resumeBefore != null) {
                URI connURI;
                try {
                    connURI = new URI(resumeBefore);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeBefore must be a full, valid connection URI");
                }
                rdfDataset = linkedDataService.listConnectionsAfter(connURI, null, dateParam.getDate(), deep)
                                .getContent();
            } else if (resumeAfter != null) {
                URI connURI;
                try {
                    connURI = new URI(resumeAfter);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeAfter must be a full, valid connection URI");
                }
                rdfDataset = linkedDataService.listConnectionsBefore(connURI, null, dateParam.getDate(), deep)
                                .getContent();
            } else {
                // all the connections; does not support date filtering for clients that do not
                // support paging
                rdfDataset = linkedDataService.listConnections(deep).getContent();
            }
            model.addAttribute("rdfDataset", rdfDataset);
            model.addAttribute("resourceURI",
                            uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
            model.addAttribute("dataURI",
                            uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
            return "rdfDatasetView";
        } catch (ParseException e) {
            model.addAttribute("error", "could not parse timestamp parameter");
            return "notFoundView";
        } catch (NoSuchConnectionException e) {
            model.addAttribute("error", "could not add connection data for " + e.getUnknownConnectionURI().toString());
            return "notFoundView";
        }
    }

    // webmvc controller method
    @RequestMapping("${uri.path.page}/atom/{identifier}/c")
    public String showConnectionURIListPage(@PathVariable String identifier,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "deep", defaultValue = "false") boolean deep,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "type", required = false) String type,
                    @RequestParam(value = "timeof", required = false) String timestamp,
                    @RequestParam(value = "state", required = false) String state, HttpServletRequest request,
                    Model model, HttpServletResponse response) {
        URI atomURI = uriService.createAtomURIForId(identifier);
        try {
            // TODO: post-filter returned connections
            ConnectionState connectionState = getConnectionState(state);
            DateParameter dateParam = new DateParameter(timestamp);
            WonMessageType eventsType = getMessageType(type);
            Dataset rdfDataset;
            if (page != null) {
                rdfDataset = linkedDataService
                                .listConnections(page, atomURI, null, eventsType, dateParam.getDate(), deep, true,
                                                connectionState)
                                .getContent();
            } else if (resumeBefore != null) {
                URI connURI;
                try {
                    connURI = new URI(resumeBefore);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeBefore must be a full, valid connection URI");
                }
                rdfDataset = linkedDataService.listConnectionsAfter(atomURI, connURI, null, eventsType,
                                dateParam.getDate(), deep, true, connectionState).getContent();
            } else if (resumeAfter != null) {
                URI connURI;
                try {
                    connURI = new URI(resumeAfter);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeAfter must be a full, valid connection URI");
                }
                rdfDataset = linkedDataService.listConnectionsBefore(atomURI, connURI, null, eventsType,
                                dateParam.getDate(), deep, true, connectionState).getContent();
            } else {
                // all the connections of the atom; does not support type and date filtering for
                // clients that do not support
                // paging
                rdfDataset = linkedDataService.listConnections(atomURI, deep, true, connectionState).getContent();
            }
            model.addAttribute("rdfDataset", rdfDataset);
            model.addAttribute("resourceURI",
                            uriService.toResourceURIIfPossible(URI.create(request.getRequestURI())).toString());
            model.addAttribute("dataURI",
                            uriService.toDataURIIfPossible(URI.create(request.getRequestURI())).toString());
            return "rdfDatasetView";
        } catch (ParseException e) {
            model.addAttribute("error", "could not parse timestamp parameter");
            return "notFoundView";
        } catch (NoSuchAtomException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        } catch (NoSuchConnectionException e) {
            logger.warn("did not find connection that should be connected to atom. connection:{}",
                            e.getUnknownConnectionURI());
            return "notFoundView"; // TODO: should display an error view
        }
    }

    /**
     * If the HTTP 'Accept' header is an RDF MIME type (as listed in the 'produces'
     * value of the RequestMapping annotation), a redirect to a data uri is sent.
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "${uri.path.resource}/**", method = RequestMethod.GET, produces = { "application/ld+json",
                    "application/trig", "application/n-quads", "*/*" })
    public ResponseEntity<String> redirectToData(HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
        URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
        URI dataUri = URI.create(this.dataURIPrefix);
        String requestUri = getRequestUriWithQueryString(request);
        String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), dataUri.getPath());
        logger.debug("resource URI requested with data mime type. redirecting from {} to {}", requestUri,
                        redirectToURI);
        if (redirectToURI.equals(requestUri)) {
            logger.debug("redirecting to same URI avoided, sending status 500 instead");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // TODO: actually the expiry information should be the same as that of the
        // resource that is redirected to
        HttpHeaders headers = new HttpHeaders();
        headers = addExpiresHeadersBasedOnRequestURI(headers, requestUri);
        // headers.setLocation(URI.create(redirectToURI));
        addCORSHeader(headers);
        setResponseHeaders(response, headers);
        response.sendRedirect(redirectToURI);
        return null;
    }

    private void setResponseHeaders(final HttpServletResponse response, final HttpHeaders headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                response.setHeader(entry.getKey(), value);
            }
        }
    }

    private String getRequestUriWithQueryString(final HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestUri += "?" + queryString;
        }
        return requestUri;
    }

    /**
     * If the HTTP 'Accept' header is 'text/html' (as listed in the 'produces' value
     * of the RequestMapping annotation), a redirect to a page uri is sent.
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "${uri.path.resource}/**", method = RequestMethod.GET, produces = "text/html")
    public ResponseEntity<String> redirectToPage(HttpServletRequest request, HttpServletResponse response)
                    throws IOException {
        URI resourceUriPrefix = URI.create(this.resourceURIPrefix);
        URI pageUriPrefix = URI.create(this.pageURIPrefix);
        String requestUri = getRequestUriWithQueryString(request);
        String redirectToURI = requestUri.replaceFirst(resourceUriPrefix.getPath(), pageUriPrefix.getPath());
        logger.debug("resource URI requested with page mime type. redirecting from {} to {}", requestUri,
                        redirectToURI);
        if (redirectToURI.equals(requestUri)) {
            logger.debug("redirecting to same URI avoided, sending status 500 instead");
            return new ResponseEntity<>("\"Could not redirect to linked data page\"", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // TODO: actually the expiry information should be the same as that of the
        // resource that is redirected to
        HttpHeaders headers = new HttpHeaders();
        headers = addExpiresHeadersBasedOnRequestURI(headers, requestUri);
        addCORSHeader(headers);
        // add a location header
        // headers.add("Location",redirectToURI);
        setResponseHeaders(response, headers);
        response.sendRedirect(redirectToURI);
        return null;
    }

    /**
     * If the request URI is the URI of a list page (list of atoms, list of
     * connections), or an atom uri, it gets the header that says 'already expired'
     * so that crawlers re-download these data. For other URIs, the 'never expires'
     * header is added.
     *
     * @param headers
     * @param requestUri
     * @return
     */
    private HttpHeaders addExpiresHeadersBasedOnRequestURI(HttpHeaders headers, final String requestUri) {
        // now, we want to suppress the 'never expires' header information
        // for /resource/atom and resource/connection so that crawlers always re-fetch
        // these data
        URI requestUriAsURI = URI.create(requestUri);
        if (uriService.isConnectionMessagesURI(requestUriAsURI)
                        || uriService.isAtomMessagesURI(requestUriAsURI)
                        || uriService.isAtomURI(requestUriAsURI)) {
            addMutableResourceHeaders(headers);
        } else {
            addImmutableResourceHeaders(headers);
        }
        return headers;
    }

    private static AtomState getAtomState(final String state) {
        if (state != null) {
            return AtomState.parseString(state);
        } else {
            return null;
        }
    }

    private static ConnectionState getConnectionState(final String state) {
        if (state != null) {
            return ConnectionState.parseString(state);
        } else {
            return null;
        }
    }

    private static URI getURIOrNull(final String uri) {
        if (uri != null) {
            return URI.create(uri);
        } else {
            return null;
        }
    }

    private Integer getPreferredSize(final HttpServletRequest request) {
        Integer preferedSize = null;
        Enumeration<String> preferValue = request.getHeaders("Prefer");
        if (preferValue != null) {
            // TODO share prefer pattern between methods, check the supported syntax
            // according to HTTP protocol, and take
            // into account that client preference can also include max-triple-count and
            // max-kbyte-count:
            Pattern pattern = Pattern.compile("(return=representation; max-member-count=)(\"?)([0-9]+)(\"?)");
            while (preferValue.hasMoreElements() && preferedSize == null) {
                String value = preferValue.nextElement();
                Matcher matcher = pattern.matcher(value);
                if (matcher.find()) {
                    preferedSize = Integer.valueOf(matcher.group(3));
                }
            }
        }
        return preferedSize;
    }

    // TODO AUTH: check if we can safely delete this method
    @RequestMapping(value = "${uri.path.data}/connection", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> listConnectionURIs(HttpServletRequest request,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "timeof", required = false) String timestamp,
                    @RequestParam(value = "modifiedafter", required = false) String modifiedAfter,
                    @RequestParam(value = "deep", defaultValue = "false") boolean deep) {
        Dataset rdfDataset;
        HttpHeaders headers = new HttpHeaders();
        Integer preferedSize = getPreferredSize(request);
        try {
            // even when the timestamp is not provided (null), we need to fix the time (if
            // null, then to current),
            // because we will return prev/next links which make no sense if the time is not
            // fixed
            DateParameter dateParam = new DateParameter(timestamp);
            String passableMap = getPassableQueryMap("timeof", dateParam.getTimestamp(), "deep",
                            Boolean.toString(deep));
            // if no preferred size provided by the client => the client does not support
            // paging, return everything:
            if (preferedSize == null && modifiedAfter == null) {
                // all connections; does not support date filtering for clients that do not
                // support paging
                rdfDataset = linkedDataService.listConnections(deep).getContent();
            } else if (modifiedAfter != null) {
                // paging is not implemented for modified connections for now!
                rdfDataset = linkedDataService
                                .listModifiedConnectionsAfter(new DateParameter(modifiedAfter).getDate(), deep)
                                .getContent();
            } else if (page != null) {
                // return latest by the given timestamp
                AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                .listConnections(page, preferedSize, dateParam.getDate(), deep);
                rdfDataset = resource.getContent();
                addPagedConnectionResourceInSequenceHeader(headers, URI.create(this.connectionResourceURIPrefix),
                                resource, page, passableMap);
                // resume before parameter specified - display the connections with activities
                // before the specified event id
            } else if (resumeBefore == null && resumeAfter == null) {
                // return latest by the given timestamp
                AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                .listConnections(1, preferedSize, dateParam.getDate(), deep);
                rdfDataset = resource.getContent();
                addPagedConnectionResourceInSequenceHeader(headers, URI.create(this.connectionResourceURIPrefix),
                                resource, passableMap);
                // resume before parameter specified - display the connections with activities
                // before the specified event id
            } else {
                if (resumeBefore != null) {
                    URI resumeConnURI;
                    try {
                        resumeConnURI = new URI(resumeBefore);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("resumeBefore must be a full, valid connection URI");
                    }
                    AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                    .listConnectionsAfter(resumeConnURI, preferedSize, dateParam.getDate(), deep);
                    rdfDataset = resource.getContent();
                    addPagedConnectionResourceInSequenceHeader(headers, URI.create(this.connectionResourceURIPrefix),
                                    resource, passableMap);
                    // resume after parameter specified - display the connections with activities
                    // after the specified event id:
                } else { // if (resumeAfter != null)
                    URI resumeConnURI;
                    try {
                        resumeConnURI = new URI(resumeAfter);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("resumeAfter must be a full, valid connection URI");
                    }
                    AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                    .listConnectionsBefore(resumeConnURI, preferedSize, dateParam.getDate(), deep);
                    rdfDataset = resource.getContent();
                    addPagedConnectionResourceInSequenceHeader(headers, URI.create(this.connectionResourceURIPrefix),
                                    resource, passableMap);
                }
            }
        } catch (ParseException e) {
            logger.warn("could not parse timestamp into Date:{}", timestamp);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (NoSuchConnectionException e) {
            logger.warn("did not find connection that should be connected to atom. connection:{}",
                            e.getUnknownConnectionURI());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        addLocationHeaderIfNecessary(headers, URI.create(request.getRequestURI()),
                        URI.create(this.connectionResourceURIPrefix));
        addMutableResourceHeaders(headers);
        addCORSHeader(headers);
        return new ResponseEntity<>(rdfDataset, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "${uri.path.data}/atom/{identifier}", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readAtom(HttpServletRequest request,
                    @PathVariable(value = "identifier") String identifier) {
        logger.debug("readAtom() called");
        return getResponseEntity(identifier, request, new EtagSupportingDataLoader<Dataset>() {
            @Override
            public URI createUriForIdentifier(final String identifier) {
                return URI.create(atomResourceURIPrefix + "/" + identifier);
            }

            @Override
            public DataWithEtag<Dataset> loadDataWithEtag(final URI uri, final String etag) {
                return linkedDataService.getAtomDataset(uri, etag,
                                WonAclRequestHelper.getWonAclEvaluationContext(request));
            }

            @Override
            public void addHeaders(final HttpHeaders headers) {
                addCORSHeader(headers);
                addPublicHeaders(headers);
            }
        });
    }

    @RequestMapping(value = "${uri.path.data}/atom/{identifier}/unread", method = RequestMethod.POST, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<org.apache.jena.rdf.model.Model> readUnreadInformationPost(
                    @PathVariable(value = "identifier") String identifier,
                    @RequestParam(value = "lastSeenMessageUris", required = false) List<URI> lastSeenMessageUris) {
        /*
         * information we want: atom-level: unread count, date of first unread, date of
         * last unread per connection: connection uri, unread count, date of first
         * unread, date of last unread
         */
        URI atomURI = URI.create(atomResourceURIPrefix + "/" + identifier);
        org.apache.jena.rdf.model.Model unreadInfo = this.linkedDataService.getUnreadInformationForAtom(atomURI,
                        lastSeenMessageUris);
        return new ResponseEntity<>(unreadInfo, HttpStatus.OK);
    }

    @RequestMapping(value = "${uri.path.data}/atom/{identifier}/unread", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<org.apache.jena.rdf.model.Model> readUnreadInformationGet(
                    @PathVariable(value = "identifier") String identifier,
                    @RequestParam(value = "lastSeenMessageUris", required = false) List<URI> lastSeenMessageUris) {
        /*
         * information we want: atom-level: unread count, date of first unread, date of
         * last unread per connection: connection uri, unread count, date of first
         * unread, date of last unread
         */
        URI atomURI = URI.create(atomResourceURIPrefix + "/" + identifier);
        org.apache.jena.rdf.model.Model unreadInfo = this.linkedDataService.getUnreadInformationForAtom(atomURI,
                        lastSeenMessageUris);
        return new ResponseEntity<>(unreadInfo, HttpStatus.OK);
    }

    /**
     * This request URL should be protected by WebID filter because the result
     * contains events data - which is data with restricted access. See
     * filterChainProxy in node-context.xml.
     *
     * @param request
     * @param identifier
     * @return
     */
    @RequestMapping(value = "${uri.path.data}/atom/{identifier}/deep", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readAtomDeep(HttpServletRequest request,
                    @PathVariable(value = "identifier") String identifier,
                    @RequestParam(value = "layer-size", required = false) Integer layerSize) {
        logger.debug("readAtom() called");
        URI atomUri = URI.create(this.atomResourceURIPrefix + "/" + identifier);
        try {
            Dataset dataset = linkedDataService.getAtomDataset(atomUri, true, layerSize,
                            WonAclRequestHelper.getWonAclEvaluationContext(request));
            // TODO: atom information does change over time. The immutable atom information
            // should never expire, the mutable should
            HttpHeaders headers = new HttpHeaders();
            addCORSHeader(headers);
            return new ResponseEntity<>(dataset, headers, HttpStatus.OK);
        } catch (NoSuchAtomException | NoSuchConnectionException | NoSuchMessageException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "${uri.path.resource}/atom/{identifier}/token", method = RequestMethod.GET, produces = {
                    "application/json" })
    public ResponseEntity<Collection<String>> requestToken(
                    HttpServletRequest request,
                    @RequestParam String scopes) {
        Set<String> tokens = WonAclRequestHelper.getGrantedTokens(request);
        if (tokens != null) {
            return new ResponseEntity<Collection<String>>(tokens, HttpStatus.OK);
        }
        return new ResponseEntity<Collection<String>>(Collections.emptySet(), HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "${uri.path.data}", method = RequestMethod.GET, produces = { "application/ld+json",
                    "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readNode(HttpServletRequest request) {
        logger.debug("readNode() called");
        // URI nodeUri = URI.create(this.nodeResourceURIPrefix);
        Dataset model = linkedDataService.getNodeDataset();
        // TODO: atom information does change over time. The immutable atom information
        // should never expire, the mutable should
        HttpHeaders headers = new HttpHeaders();
        addCORSHeader(headers);
        addHeadersForShortTermCaching(headers);
        headers.add(HttpHeaders.CACHE_CONTROL, "public");
        return new ResponseEntity<>(model, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "${uri.path.data}/atom/{atomId}/c/{identifier}", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readConnection(HttpServletRequest request,
                    @PathVariable String atomId,
                    @PathVariable(value = "identifier") String identifier) {
        logger.debug("readConnection() called");
        URI connectionURI = uriService.createConnectionURIForId(atomId, identifier);
        return getResponseEntity(connectionURI.toString(), request, new EtagSupportingDataLoader<Dataset>() {
            @Override
            public URI createUriForIdentifier(final String connectionURL) {
                return URI.create(connectionURL);
            }

            @Override
            public DataWithEtag<Dataset> loadDataWithEtag(final URI uri, final String etag) {
                return linkedDataService.getConnectionDataset(uri, true, etag);
            }

            @Override
            public void addHeaders(final HttpHeaders headers) {
                addCORSHeader(headers);
                addPublicHeaders(headers);
            }
        });
    }

    @RequestMapping(value = "${uri.path.data}/atom/{atomId}/c/{identifier}/msg", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readConnectionEvents(HttpServletRequest request,
                    @PathVariable String atomId,
                    @PathVariable(value = "identifier") String identifier,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "type", required = false) String type,
                    @RequestParam(value = "deep", required = false, defaultValue = "false") boolean deep) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logger.debug("readConnection() called");
        Dataset rdfDataset;
        HttpHeaders headers = new HttpHeaders();
        Integer preferedSize = getPreferredSize(request);
        URI connectionUri = uriService.createConnectionURIForId(atomId, identifier);
        URI connectionEventsURI = URI.create(connectionUri.toString() + "/" + "msg");
        WonMessageType msgType = getMessageType(type);
        try {
            String passableMap = getPassableQueryMap("type", type);
            if (preferedSize == null) {
                // client doesn't not support paging - return all members; does not support type
                // filtering for clients that do
                // not support paging
                rdfDataset = linkedDataService.listConnectionEventURIs(connectionUri, deep);
            } else if (resumeBefore == null && resumeAfter == null) {
                // if page == null -> return page with latest events
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService.listConnectionEventURIs(
                                connectionUri, page != null ? page : 1, preferedSize, msgType, deep); // FIXME:
                                                                                                      // does not
                                                                                                      // respect
                                                                                                      // preferredSize
                                                                                                      // if deep is
                                                                                                      // used
                rdfDataset = resource.getContent();
                if (page == null) {
                    addPagedResourceInSequenceHeader(headers, connectionEventsURI, resource, passableMap);
                } else {
                    addPagedResourceInSequenceHeader(headers, connectionEventsURI, resource, page, passableMap);
                }
            } else if (resumeBefore != null) {
                // a page that precedes the item identified by the resumeBefore is requested
                URI referenceEvent;
                try {
                    referenceEvent = new URI(resumeBefore);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeBefore must be a full, valid message URI");
                }
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                                .listConnectionEventURIsAfter(connectionUri, referenceEvent, preferedSize, msgType,
                                                deep);
                rdfDataset = resource.getContent();
                addPagedResourceInSequenceHeader(headers, connectionEventsURI, resource, passableMap);
            } else {
                // a page that follows the item identified by the resumeAfter is requested
                URI referenceEvent;
                try {
                    referenceEvent = new URI(resumeAfter);
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("resumeAfter must be a full, valid message URI");
                }
                AtomInformationService.PagedResource<Dataset, URI> resource = linkedDataService
                                .listConnectionEventURIsBefore(connectionUri, referenceEvent, preferedSize, msgType,
                                                deep);
                rdfDataset = resource.getContent();
                addPagedResourceInSequenceHeader(headers, connectionEventsURI, resource, passableMap);
            }
        } catch (NoSuchConnectionException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // TODO: events list information does change over time, unless the connection is
        // closed and cannot be reopened.
        // The events list of immutable connection information should never expire, the
        // mutable should
        addLocationHeaderIfNecessary(headers, URI.create(request.getRequestURI()),
                        URI.create(this.connectionResourceURIPrefix));
        addMutableResourceHeaders(headers);
        addCORSHeader(headers);
        stopWatch.stop();
        logger.debug("readConnectionEvents took " + stopWatch.getLastTaskTimeMillis() + " millis");
        return new ResponseEntity<>(rdfDataset, headers, HttpStatus.OK);
    }

    private WonMessageType getMessageType(final String type) {
        if (type != null) {
            return WonMessageType.valueOf(type);
        } else {
            return null;
        }
    }

    /**
     * This request URL should be protected by WebID filter because the result
     * contains events data - which is data with restricted access. See
     * filterChainProxy in node-context.xml.
     *
     * @param request
     * @param identifier
     * @return
     */
    @RequestMapping(value = "${uri.path.data}/msg/{identifier}", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readEvent(@PathVariable(value = "identifier") String identifier,
                    HttpServletRequest request, HttpServletResponse response) {
        // get etag from headers, extract version identifier
        logger.debug("readConnectionEvent() called");
        return getResponseEntity(identifier, request, new EtagSupportingDataLoader<Dataset>() {
            @Override
            public URI createUriForIdentifier(final String identifier) {
                return uriService.createMessageURIForId(identifier);
            }

            @Override
            public DataWithEtag<Dataset> loadDataWithEtag(final URI uri, final String etag) {
                return linkedDataService.getDatasetForUri(uri, etag);
            }

            @Override
            public void addHeaders(final HttpHeaders headers) {
                addCORSHeader(headers);
                addPrivateHeaders(headers);
                addImmutableResourceHeaders(headers);
            }
        });
    }

    private <T> ResponseEntity<T> getResponseEntity(String identifier, final HttpServletRequest request,
                    EtagSupportingDataLoader<T> loader) {
        HttpHeaders requestHeaders = getHttpHeaders(request);
        WonEtagHelper requestEtagHelper = WonEtagHelper.fromHeaderIfCompatibleWithAcceptHeader(requestHeaders);
        String versionIdentifier = WonEtagHelper.getVersionIdentifier(requestEtagHelper);
        // fetch the data if required
        logger.debug("using version identifier {}", versionIdentifier);
        URI entityUri = loader.createUriForIdentifier(identifier);
        DataWithEtag<T> dataWithEtag = loader.loadDataWithEtag(entityUri, versionIdentifier);
        // prepare the response headers
        HttpHeaders headers = new HttpHeaders();
        loader.addHeaders(headers);
        // set the etag headers
        setEtagHeaderForResponse(headers, dataWithEtag, requestEtagHelper);
        // return the response
        return getResponseEntityForPossiblyNotModifiedResult(dataWithEtag, headers);
    }

    @RequestMapping(value = "${uri.path.data}/attachment/{identifier}", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads", "*/*" })
    public ResponseEntity<Dataset> readAttachment(HttpServletRequest request,
                    @PathVariable(value = "identifier") String identifier) {
        logger.debug("readAttachment() called");
        URI attachmentURI = uriService.createAttachmentURIForId(identifier);
        DataWithEtag<Dataset> data = linkedDataService.getDatasetForUri(attachmentURI, null);
        if (!data.isNotFound()) {
            HttpHeaders headers = new HttpHeaders();
            addCORSHeader(headers);
            String mimeTypeOfResponse = RdfUtils.findFirst(data.getData(), model -> {
                String content = getObjectOfPropertyAsString(model, CNT.BYTES);
                if (content == null)
                    return null;
                return getObjectOfPropertyAsString(model, WONMSG.contentType);
            });
            if (mimeTypeOfResponse != null) {
                // we found a base64 encoded attachment, we obtained its contentType, so we set
                // it as the
                // contentType of the response.
                Set<MediaType> producibleMediaTypes = new HashSet<>();
                producibleMediaTypes.add(MediaType.valueOf(mimeTypeOfResponse));
                request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, producibleMediaTypes);
            }
            return new ResponseEntity<>(data.getData(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private String getObjectOfPropertyAsString(org.apache.jena.rdf.model.Model model, Property property) {
        NodeIterator nodeIteratr = model.listObjectsOfProperty(property);
        if (!nodeIteratr.hasNext())
            return null;
        String ret = nodeIteratr.next().asLiteral().getString();
        if (nodeIteratr.hasNext()) {
            throw new IncorrectPropertyCountException("found more than one property of cnt:bytes", 1, 2);
        }
        return ret;
    }

    /**
     * Get the RDF for the connections of the specified atom.
     *
     * @param request
     * @param identifier
     * @param deep If true, connection data is added to the model (not only
     * connection URIs). Default: false.
     * @param page taken into account only if client supports paging; in that case
     * the specified page is returned
     * @param resumeBefore taken into account only if client supports paging; in
     * that case the page with connections URIs that precede the connection having
     * resumeBefore is returned
     * @param resumeAfter taken into account only if client supports paging; in that
     * case the page with connections URIs that follow the connection having
     * resumeAfter are returned
     * @param type only connection events of the given type are considered when
     * ordering returned connections. Default: all event types.
     * @param timestamp only connection events that where created before the given
     * time are considered when ordering returned connections. Default: current
     * time.
     * @return
     */
    @RequestMapping(value = "${uri.path.data}/atom/{identifier}/c", method = RequestMethod.GET, produces = {
                    "application/ld+json", "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> readConnectionsOfAtom(HttpServletRequest request,
                    @PathVariable(value = "identifier") String identifier,
                    @RequestParam(value = "socket", required = false) String socket,
                    @RequestParam(value = "targetSocket", required = false) String targetSocket,
                    @RequestParam(value = "deep", defaultValue = "false") boolean deep,
                    @RequestParam(value = "p", required = false) Integer page,
                    @RequestParam(value = "resumebefore", required = false) String resumeBefore,
                    @RequestParam(value = "resumeafter", required = false) String resumeAfter,
                    @RequestParam(value = "type", required = false) String type,
                    @RequestParam(value = "timeof", required = false) String timestamp,
                    @RequestParam(value = "state", required = false) String state) {
        logger.debug("readConnectionsOfAtom() called");
        // TODO: pass aclevaluator and operationRequest down to linkeddataservice as an
        // additional filter
        URI atomUri = uriService.createAtomURIForId(identifier);
        Dataset rdfDataset;
        HttpHeaders headers = new HttpHeaders();
        Integer preferedSize = getPreferredSize(request);
        URI connectionsURI = URI.create(atomUri.toString() + "/c");
        try {
            ConnectionState connectionState = getConnectionState(state);
            WonMessageType eventsType = getMessageType(type);
            DateParameter dateParam = new DateParameter(timestamp);
            String passableQuery = getPassableQueryMap("type", type, "timeof", dateParam.getTimestamp(), "deep",
                            Boolean.toString(deep));
            // if no preferred size provided by the client => the client does not support
            // paging, return everything:
            if (socket != null && targetSocket != null) {
                rdfDataset = linkedDataService.listConnection(URI.create(socket), URI.create(targetSocket), deep);
            } else if (preferedSize == null) {
                // does not support date and type filtering for clients that do not support
                // paging
                rdfDataset = linkedDataService.listConnections(atomUri, deep, true, connectionState).getContent();
                // if no page or resume parameter is specified, display the latest connections:
            } else if (page == null && resumeBefore == null && resumeAfter == null) {
                AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                .listConnections(1, atomUri, preferedSize, eventsType, dateParam.getDate(), deep, true,
                                                connectionState);
                rdfDataset = resource.getContent();
                addPagedConnectionResourceInSequenceHeader(headers, connectionsURI, resource, passableQuery);
            } else if (page != null) {
                AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService.listConnections(
                                page, atomUri, preferedSize, eventsType, dateParam.getDate(), deep, true,
                                connectionState);
                rdfDataset = resource.getContent();
                addPagedConnectionResourceInSequenceHeader(headers, connectionsURI, resource, page, passableQuery);
            } else {
                // resume before parameter specified - display the connections with activities
                // before the specified event id:
                if (resumeBefore != null) {
                    URI resumeConnURI;
                    try {
                        resumeConnURI = new URI(resumeBefore);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("resumeBefore must be a full, valid connection URI");
                    }
                    AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                    .listConnectionsAfter(atomUri, resumeConnURI, preferedSize, eventsType,
                                                    dateParam.getDate(), deep, true, connectionState);
                    rdfDataset = resource.getContent();
                    addPagedConnectionResourceInSequenceHeader(headers, connectionsURI, resource, passableQuery);
                    // resume after parameter specified - display the connections with activities
                    // after the specified event id:
                } else { // if (resumeAfter != null)
                    URI resumeConnURI;
                    try {
                        resumeConnURI = new URI(resumeAfter);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("resumeAfter must be a full, valid connection URI");
                    }
                    AtomInformationService.PagedResource<Dataset, Connection> resource = linkedDataService
                                    .listConnectionsBefore(atomUri, resumeConnURI, preferedSize, eventsType,
                                                    dateParam.getDate(), deep, true, connectionState);
                    rdfDataset = resource.getContent();
                    addPagedConnectionResourceInSequenceHeader(headers, connectionsURI, resource, passableQuery);
                }
            }
            // append the required headers
            addMutableResourceHeaders(headers);
            addLocationHeaderIfNecessary(headers, URI.create(request.getRequestURI()), connectionsURI);
            addCORSHeader(headers);
            return new ResponseEntity<>(rdfDataset, headers, HttpStatus.OK);
        } catch (ParseException e) {
            logger.warn("could not parse timestamp into Date:{}", timestamp);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (NoSuchAtomException e) {
            logger.warn("did not find atom {}", e.getUnknownAtomURI());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (NoSuchConnectionException e) {
            logger.warn("did not find connection that should be connected to atom. connection:{}",
                            e.getUnknownConnectionURI());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Checks if the actual URI is the same as the canonical URI; if not, adds a
     * Location header to the response builder indicating the canonical URI.
     *
     * @param headers
     * @param actualURI
     * @param canonicalURI
     * @return the headers map with added header values
     */
    private HttpHeaders addLocationHeaderIfNecessary(HttpHeaders headers, URI actualURI, URI canonicalURI) {
        if (!canonicalURI.resolve(actualURI).equals(canonicalURI)) {
            // the request URI is the canonical URI, it may be a DNS alias or relative
            // according to http://www.w3.org/TR/ldp/#general we have to include
            // the canonical URI in the lcoation header here
            headers.add(HTTP.HEADER_LOCATION, canonicalURI.toString());
        }
        return headers;
    }

    /**
     * Adds headers describing the paged resource according to
     * https://www.w3.org/TR/ldp-paging/ (here implemented version is
     * http://www.w3.org/TR/2015/NOTE-ldp-paging-20150630/) that inform the client
     * about the following properties of the pages resource:
     * <p>
     * Link: <uri>; rel="canonical"; etag="tag" - which resource it is a page of,
     * and current tag of the resource Link: <http://www.w3.org/ns/ldp#Page>;
     * rel="type" - that this is one in-sequence page resource Link:
     * <http://www.w3.org/ns/ldp#Resource>; rel="type" - that this is a LDP Resource
     * (should be Container in our case?) Link: <uri?p=x>; rel="next" - that the
     * next in-sequence page resource exists and is retrievable at the given uri
     *
     * @param headers headers to which paged resource headers should be added
     * @param canonicalURI uri of the LDP Resource
     * @param page page of the Paged LDP Resource
     * @return the headers map with added header values
     */
    private void addPagedResourceInSequenceHeader(final HttpHeaders headers, final URI canonicalURI,
                    final AtomInformationService.PagedResource<Dataset, URI> resource, final int page,
                    String queryPart) {
        headers.add(HttpHeaders.LINK,
                        "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\", <http://www.w3.org/ns/ldp#Page>; rel=\"type\"");
        // Link: <http://example.org/customer-relations?p=2>; rel="next"
        if (resource.hasNext()) {
            int nextPage = page + 1;
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?p=" + nextPage + queryPart + ">; rel=\"next\"");
        }
        if (resource.hasPrevious() && page > 1) {
            int prevPage = page - 1;
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?p=" + prevPage + queryPart + ">; rel=\"prev\"");
        }
        headers.add(HttpHeaders.LINK, "<" + canonicalURI.toString() + ">; rel=\"canonical\"");
    }

    private void addPagedResourceInSequenceHeader(final HttpHeaders headers, final URI canonicalURI,
                    final AtomInformationService.PagedResource<Dataset, URI> resource, String queryPart) {
        headers.add(HttpHeaders.LINK,
                        "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\", <http://www.w3.org/ns/ldp#Page>; rel=\"type\"");
        if (resource.hasNext()) {
            String id = resource.getResumeAfter().toString();
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?resumeafter=" + id + queryPart + ">; rel=\"next\"");
        }
        if (resource.hasPrevious()) {
            String id = resource.getResumeBefore().toString();
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?resumebefore=" + id + queryPart + ">; rel=\"prev\"");
        }
        headers.add(HttpHeaders.LINK, "<" + canonicalURI.toString() + ">; rel=\"canonical\"");
    }

    /**
     * Adds headers describing the paged resource according to
     * https://www.w3.org/TR/ldp-paging/ (here implemented version is
     * http://www.w3.org/TR/2015/NOTE-ldp-paging-20150630/) that inform the client
     * about the following properties of the pages resource:
     * <p>
     * Link: <uri>; rel="canonical"; etag="tag" - which resource it is a page of,
     * and current tag of the resource Link: <http://www.w3.org/ns/ldp#Page>;
     * rel="type" - that this is one in-sequence page resource Link:
     * <http://www.w3.org/ns/ldp#Resource>; rel="type" - that this is a LDP Resource
     * (should be Container in our case?) Link: <uri?p=x>; rel="next" - that the
     * next in-sequence page resource exists and is retrievable at the given uri
     *
     * @param headers headers to which paged resource headers should be added
     * @param canonicalURI uri of the LDP Resource
     * @param page page of the Paged LDP Resource
     * @return the headers map with added header values
     */
    private void addPagedConnectionResourceInSequenceHeader(final HttpHeaders headers, final URI canonicalURI,
                    final AtomInformationService.PagedResource<Dataset, Connection> resource, final int page,
                    String queryPart) {
        headers.add(HttpHeaders.LINK,
                        "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\", <http://www.w3.org/ns/ldp#Page>; rel=\"type\"");
        // Link: <http://example.org/customer-relations?p=2>; rel="next"
        if (resource.hasNext()) {
            int nextPage = page + 1;
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?p=" + nextPage + queryPart + ">; rel=\"next\"");
        }
        if (resource.hasPrevious() && page > 1) {
            int prevPage = page - 1;
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?p=" + prevPage + queryPart + ">; rel=\"prev\"");
        }
        headers.add(HttpHeaders.LINK, "<" + canonicalURI.toString() + ">; rel=\"canonical\"");
    }

    private void addPagedConnectionResourceInSequenceHeader(final HttpHeaders headers, final URI canonicalURI,
                    final AtomInformationService.PagedResource<Dataset, Connection> resource, String queryPart) {
        headers.add(HttpHeaders.LINK,
                        "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\", <http://www.w3.org/ns/ldp#Page>; rel=\"type\"");
        if (resource.hasNext()) {
            String id = resource.getResumeAfter().getConnectionURI().toString();
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?resumeafter=" + id + queryPart + ">; rel=\"next\"");
        }
        if (resource.hasPrevious()) {
            String id = resource.getResumeBefore().getConnectionURI().toString();
            headers.add(HttpHeaders.LINK,
                            "<" + canonicalURI.toString() + "?resumebefore=" + id + queryPart + ">; rel=\"prev\"");
        }
        headers.add(HttpHeaders.LINK, "<" + canonicalURI.toString() + ">; rel=\"canonical\"");
    }

    private String getPassableQueryMap(String... nameValue) {
        StringBuilder queryPart = new StringBuilder();
        for (int i = 0; i < nameValue.length; i++) {
            if (nameValue[i + 1] != null) {
                queryPart.append("&").append(nameValue[i]).append("=").append(nameValue[i + 1]);
            }
            i++;
        }
        return queryPart.toString();
    }

    /**
     * Headers: types of resources * public immutable * public mutable * private
     * immutable * private mutable * public short-term cacheable * privet short-term
     * cacheable
     */
    private void addPrivateHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CACHE_CONTROL, "private");
        // with no-store, the items don't survive a browser reload - but if the browser
        // is closed, the items are gone
        // headers.add(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private void addPublicHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CACHE_CONTROL, "public");
    }

    private void addImmutableResourceHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=31536000");
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123, Locale.ENGLISH);
        headers.add(HTTP.HEADER_EXPIRES, dateFormat.format(getNeverExpiresDate()));
        headers.add(HTTP.HEADER_DATE, dateFormat.format(new Date()));
    }

    private void addMutableResourceHeaders(HttpHeaders headers) {
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=0");
        headers.add(HttpHeaders.CACHE_CONTROL, "must-revalidate");
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123, Locale.ENGLISH);
        headers.add(HTTP.HEADER_EXPIRES, "0");
        headers.add(HTTP.HEADER_DATE, dateFormat.format(new Date()));
    }

    /**
     * Sets the Expires and Cache-Control header fields such that the response will
     * be cached for a few minutes. Useful for data that might change during a
     * server reconfiguration but is otherwise quite stable.
     *
     * @param headers
     * @return the headers map with added header values
     */
    private HttpHeaders addHeadersForShortTermCaching(HttpHeaders headers) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123, Locale.ENGLISH);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, SHORT_TERM_CACHE_TIMEOUT_SECONDS);
        headers.add(HTTP.HEADER_EXPIRES, dateFormat.format(cal.getTime()));
        headers.add(HTTP.HEADER_DATE, dateFormat.format(new Date()));
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=" + SHORT_TERM_CACHE_TIMEOUT_SECONDS);
        return headers;
    }

    // Calculates a date that, according to http spec, means 'never expires'
    // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
    private Date getNeverExpiresDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        return cal.getTime();
    }

    /**
     * Adds the CORS headers required for client side cross-site requests. See
     * http://www.w3.org/TR/cors/
     *
     * @param headers
     */
    private void addCORSHeader(final HttpHeaders headers) {
        headers.add("Access-Control-Allow-Origin", "*");
    }

    private HttpHeaders getHttpHeaders(final HttpServletRequest request) {
        ServletServerHttpRequest servletRequest = new ServletServerHttpRequest(request);
        return servletRequest.getHeaders();
    }

    private <T> ResponseEntity<T> getResponseEntityForPossiblyNotModifiedResult(final DataWithEtag<T> datasetWithEtag,
                    final HttpHeaders headers) {
        if (datasetWithEtag != null) {
            if (datasetWithEtag.isDeleted()) {
                return new ResponseEntity<>(headers, HttpStatus.GONE);
            } else if (datasetWithEtag.isChanged()) {
                return new ResponseEntity<>(datasetWithEtag.getData(), headers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Depending on whether the data has changed, use the old etag or create a new
     * one.
     *
     * @param headers
     * @param datasetWithEtag
     * @param requestEtagHelper
     */
    private <T> void setEtagHeaderForResponse(final HttpHeaders headers, final DataWithEtag<T> datasetWithEtag,
                    final WonEtagHelper requestEtagHelper) {
        // check if the data has changed
        if (datasetWithEtag.isChanged()) {
            logger.debug("ETAG comparison shows that data has changed or no etag was present");
            // data has changed: create a new etag and put it into the header
            WonEtagHelper responseEtagHelper = WonEtagHelper.forVersion(datasetWithEtag.getEtag());
            if (responseEtagHelper != null) {
                WonEtagHelper.setEtagHeader(responseEtagHelper, headers);
            }
        } else {
            // data has not changed: use the old etag value for the response ETag header
            WonEtagHelper.setEtagHeader(requestEtagHelper, headers);
        }
    }

    public void setLinkedDataService(final LinkedDataService linkedDataService) {
        this.linkedDataService = linkedDataService;
    }

    public void setRegistrationServer(final RegistrationServer registrationServer) {
        this.registrationServer = registrationServer;
    }

    public void setUriService(final URIService uriService) {
        this.uriService = uriService;
    }

    public void setAtomResourceURIPrefix(String atomResourceURIPrefix) {
        this.atomResourceURIPrefix = atomResourceURIPrefix;
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

    @RequestMapping(value = "${uri.path.resource}", method = RequestMethod.POST, produces = { "text/plain" })
    public ResponseEntity<String> register(@RequestParam("register") String registeredType, HttpServletRequest request)
                    throws CertificateException, UnsupportedEncodingException {
        logger.debug("REGISTERING " + registeredType);
        PreAuthenticatedAuthenticationToken authentication = (PreAuthenticatedAuthenticationToken) SecurityContextHolder
                        .getContext().getAuthentication();
        if (authentication == null) {
            throw new BadCredentialsException("Could not register: PreAuthenticatedAuthenticationToken expected");
        }
        // Object principal = authentication.getPrincipal();
        Object credentials = authentication.getCredentials();
        X509Certificate cert;
        if (credentials instanceof X509Certificate) {
            cert = (X509Certificate) credentials;
        } else {
            throw new BadCredentialsException("Could not register: expected to find a X509Certificate in the request");
        }
        try {
            if ("owner".equals(registeredType)) {
                String result = registrationServer.registerOwner(cert);
                logger.debug("successfully registered owner");
                return new ResponseEntity<>(result, HttpStatus.OK);
            }
            if ("node".equals(registeredType)) {
                String result = registrationServer.registerNode(cert);
                logger.debug("successfully registered node");
                return new ResponseEntity<>(result, HttpStatus.OK);
            } else {
                String supportedTypesMsg = "Request parameter error; supported 'register' parameter values: 'owner', 'node'";
                logger.debug(supportedTypesMsg);
                return new ResponseEntity<>(supportedTypesMsg, HttpStatus.BAD_REQUEST);
            }
        } catch (WonProtocolException e) {
            logger.info("Could not register " + registeredType, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private class DateParameter {
        private String timestamp;
        private Date date;

        /**
         * Creates date parameter from String timestamp, assumes timestamp format is ISO
         * 8601. If timestamp is null, the parameter is assigned current time value.
         *
         * @param timestamp
         */
        DateParameter(final String timestamp) throws ParseException {
            // timestamp string is expected in ISO 8601 format (UTC)
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (timestamp == null) {
                this.date = new Date();
                this.timestamp = format.format(date);
            } else {
                this.date = format.parse(timestamp);
                this.timestamp = timestamp;
            }
        }

        /**
         * Gets time from String timestamp.
         *
         * @return
         */
        Date getDate() {
            return date;
        }

        /**
         * Gets String timestamp.
         *
         * @return
         */
        String getTimestamp() {
            return timestamp;
        }
    }

    private String createDatasetResponse(Model model, HttpServletResponse response, URI eventURI) {
        DataWithEtag<Dataset> data = linkedDataService.getDatasetForUri(eventURI, null);
        if (model != null && !data.isNotFound()) {
            model.addAttribute("rdfDataset", data.getData());
            model.addAttribute("resourceURI", eventURI.toString());
            model.addAttribute("dataURI", uriService.toDataURIIfPossible(eventURI).toString());
            return "rdfDatasetView";
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "notFoundView";
        }
    }
}
