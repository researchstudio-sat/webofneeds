package won.owner.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import won.owner.model.UserAtom;
import won.owner.service.impl.WONUserDetailService;
import won.protocol.rest.LinkedDataRestBridge;
import won.protocol.rest.RDFMediaType;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.uriresolver.WonMessageUriResolver;

/**
 * User: ypanchenko Date: 03.09.2015 This controller at Owner server-side serves
 * as a bridge for Owner client-side to obtain linked data from a Node: because
 * the linked data on a Node can have restricted access based on WebID, only
 * Owner server-side can provide the client's certificate as proof of having the
 * private key from client's published WebID. Because of this, Owner client-side
 * has to ask Owner-server side to query Node for it, instead of querying
 * directly from Owner client-side.
 */
@Controller
@RequestMapping("/rest/linked-data")
public class BridgeForLinkedDataController implements InitializingBean {
    private String filterQuery;
    @Autowired
    private WONUserDetailService wonUserDetailService;
    @Autowired
    private LinkedDataRestBridge linkedDataRestBridgeOnBehalfOfAtom;
    @Autowired
    private LinkedDataRestBridge linkedDataRestBridge;
    @Autowired
    private WonMessageUriResolver wonMessageUriResolver;
    @Autowired
    private LinkedDataSource linkedDataSource;

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

    /**
     * Fetches a resource via http. Converts a generic message uri that a client
     * wants to dereference into a local one on the default WoN node. Optionally,
     * the client can send a URL parameter `wonnode=[wonNodeUri]` to ask for
     * conversion on that WoN node. If that fails, the default WoN node is used for
     * conversion. If the conversion fails, the resourceUri is left as-is.
     * 
     * @param uri the URI to fetch
     * @param requesterWebId (optional) the URI of the WebID to use for fetching the
     * resource.
     * @param wonnode (optional) the WoN node URI to use for converting a generic
     * message URI into a WoN-node specific one
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.GET, produces = { "*/*" })
    public void fetchResource(@RequestParam("uri") String resourceUriString,
                    @RequestParam(value = "requester", required = false) String requesterWebId,
                    @RequestParam(value = "wonnode", required = false) String wonNodeUriString,
                    final HttpServletResponse response, final HttpServletRequest request) throws IOException {
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
                restTemplate = linkedDataRestBridgeOnBehalfOfAtom.getRestTemplate(requesterWebId);
            } else {
                // no: that's fishy, but we let them make the request without the webid
                restTemplate = linkedDataRestBridge.getRestTemplate();
            }
        }
        // prepare headers to be passed in request for linked data resource
        final HttpHeaders requestHeaders = extractLinkedDataRequestRelevantHeaders(request);
        // convert the URI if it's a message URI (the resolver will leave it as-is if
        // it's not a generic message URI)
        Optional<URI> wonNodeUri = Optional.ofNullable(wonNodeUriString).map(s -> URI.create(s));
        URI resourceUri = wonMessageUriResolver.toLocalMessageURIForWonNode(URI.create(resourceUriString), wonNodeUri,
                        linkedDataSource);
        restTemplate.execute(resourceUri, HttpMethod.valueOf(request.getMethod()), new RequestCallback() {
            @Override
            public void doWithRequest(final ClientHttpRequest request) throws IOException {
                HttpHeaders newHeaders = request.getHeaders();
                for (Map.Entry<String, List<String>> header : requestHeaders.entrySet()) {
                    for (String headerValue : header.getValue()) {
                        newHeaders.add(header.getKey(), headerValue);
                    }
                }
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
            logger.debug("no Content-Type header found in response from server. Assuming no body, not attempting to copy "
                            + "body");
            copyLinkedDataResponseRelevantHeaders(originalResponse.getHeaders(), response);
            response.setStatus(originalResponse.getRawStatusCode());
        } else {
            if (RDFMediaType.isRDFMediaType(originalResponseMediaType)
                            || originalResponseMediaType.isCompatibleWith(MediaType.TEXT_HTML)) {
                copyLinkedDataResponseRelevantHeaders(originalResponse.getHeaders(), response);
                response.setStatus(originalResponse.getRawStatusCode());
                // create response body
                copyResponseBody(originalResponse, response);
                // close response output stream
            } else {
                // the content type is not an RDF media type and not text/html, which we need
                // for the human-readable view of linked data resources. We don't like to handle
                // such requests. Indicate this with the BAD GATEWAY response status and explain
                // in the response body.
                response.setStatus(HttpStatus.SC_BAD_GATEWAY);
                response.getOutputStream().print("The target server's response was of type " +
                                originalResponseMediaType +
                                ". We only forward responses of the following types "
                                + RDFMediaType.rdfMediaTypes.toString() + " and " + MediaType.TEXT_HTML_VALUE);
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
        Set<String> preexistingHeaders = new HashSet<String>(toResponse.getHeaderNames());
        for (Map.Entry<String, List<String>> header : fromHeaders.entrySet()) {
            for (String headerValue : header.getValue()) {
                String headerName = header.getKey();
                if (headerName.equals("Transfer-Encoding") &&
                                headerValue.equals("chunked")) {
                    // we allow all transfer codings except chunked, because we
                    // don't do chunking here!
                    continue;
                }
                if (preexistingHeaders.contains(headerName)) {
                    // existing headers are not mixed with the ones from the response
                    continue;
                }
                toResponse.addHeader(headerName, headerValue);
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

    private void copyHeader(final String headerName, final HttpServletRequest fromRequest,
                    final HttpHeaders toHeaders) {
        Enumeration<String> values = fromRequest.getHeaders(headerName);
        while (values.hasMoreElements()) {
            toHeaders.add(headerName, values.nextElement());
        }
    }

    /**
     * Check if the current user has the claimed identity represented by web-id of
     * the atom. I.e. if the identity is that of the atom that belongs to the user -
     * return true, otherwise - false.
     *
     * @param requesterWebId
     * @return
     */
    private boolean currentUserHasIdentity(final String requesterWebId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = (User) wonUserDetailService.loadUserByUsername(username);
        Set<URI> atomUris = getUserAtomUris(user);
        if (atomUris.contains(URI.create(requesterWebId))) {
            return true;
        }
        return false;
    }

    private Set<URI> getUserAtomUris(final User user) {
        Set<URI> atomUris = new HashSet<>();
        for (UserAtom userAtom : user.getUserAtoms()) {
            atomUris.add(userAtom.getUri());
        }
        return atomUris;
    }
}
