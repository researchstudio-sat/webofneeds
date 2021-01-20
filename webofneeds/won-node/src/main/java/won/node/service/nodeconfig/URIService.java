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
package won.node.service.nodeconfig;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import won.protocol.model.Atom;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.util.WonUriCheckHelper;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: fkleedorfer Date: 06.11.12
 */
@Component
public class URIService implements InitializingBean {
    private static final String TOKEN_ENDPOINT_SUFFIX = "/token";
    // prefix of any URI
    private String generalURIPrefix;
    // prefix of an atom resource
    private String atomResourceURIPrefix;
    // prefix of a connection resource
    private String connectionResourceURIPrefix;
    // prefix of an event resource
    private String messageResourceURIPrefix;
    // prefix of an attachment resource
    private String attachmentResourceURIPrefix;
    // prefix for URISs of RDF data
    private String dataURIPrefix;
    // prefix for URIs referring to real-world things
    private String resourceURIPrefix;
    // prefix for human readable pages
    private String pageURIPrefix;
    private Pattern connectionMessagesPattern;
    private Pattern connectionUriPattern;
    private Pattern atomMessagesPattern;
    private Pattern atomUnreadPattern;
    private Pattern atomUriPattern;
    private Pattern messageUriPattern;
    private Pattern connectionContainerUriPattern;
    private Pattern tokenEndpointUriPattern;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.atomResourceURIPrefix = this.resourceURIPrefix + "/atom";
        this.connectionResourceURIPrefix = this.resourceURIPrefix + "/connection";
        this.messageResourceURIPrefix = this.resourceURIPrefix + "/msg";
        this.attachmentResourceURIPrefix = this.resourceURIPrefix + "/attachment";
        this.connectionMessagesPattern = Pattern
                        .compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+/c/[\\-\\._~\\+a-zA-Z0-9]+/msg\\b");
        this.messageUriPattern = Pattern
                        .compile(messageResourceURIPrefix
                                        + "/[123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ]+\\b");
        this.connectionUriPattern = Pattern
                        .compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+/c/[\\-\\._~\\+a-zA-Z0-9]+\\b");
        this.connectionContainerUriPattern = Pattern.compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+/c\\b");
        this.atomMessagesPattern = Pattern.compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+/msg\\b");
        this.atomUnreadPattern = Pattern.compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+/unread\\b");
        this.atomUriPattern = Pattern.compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+\\b");
        this.tokenEndpointUriPattern = Pattern
                        .compile(atomResourceURIPrefix + "/[\\-\\._~\\+a-zA-Z0-9]+" + TOKEN_ENDPOINT_SUFFIX + "\\b");
    }

    public boolean isMessageURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        return messageUriPattern.matcher(toCheck.toString()).lookingAt();
    }

    public boolean isAtomURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        return atomUriPattern.matcher(toCheck.toString()).lookingAt();
    }

    public boolean isAtomMessagesURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        Matcher m = atomMessagesPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isAtomUnreadURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        Matcher m = atomUnreadPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isTokenEndpointURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        Matcher m = tokenEndpointUriPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isConnectionContainerURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        Matcher m = connectionContainerUriPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isConnectionURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        return WonUriCheckHelper.isValidConnectionURI(this.atomResourceURIPrefix, toCheck.toString());
    }

    public boolean isConnectionMessagesURI(URI toCheck) {
        if (toCheck == null) {
            return false;
        }
        Matcher m = connectionMessagesPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public URI getConnectionURIofConnectionMessagesURI(URI connectionMessagesURI) {
        if (connectionMessagesURI == null) {
            return null;
        }
        Matcher m = connectionUriPattern.matcher(connectionMessagesURI.toString());
        m.find();
        return URI.create(m.group());
    }

    public URI getAtomURIofAtomMessagesURI(URI atomMessagesURI) {
        if (atomMessagesURI == null) {
            return null;
        }
        Matcher m = atomUriPattern.matcher(atomMessagesURI.toString());
        m.find();
        return URI.create(m.group());
    }

    /**
     * Attempts to find the atom uri in any sub-uri (e.g atom messages, connection,
     * connection messages)
     *
     * @param subUri
     * @return the atom uri or null if the argument is null or the atom uri pattern
     * is not present.
     */
    public URI getAtomURIofSubURI(URI subUri) {
        if (subUri == null) {
            return null;
        }
        Matcher m = atomUriPattern.matcher(subUri.toString());
        if (!m.find()) {
            return null;
        }
        return URI.create(m.group());
    }

    public URI getAtomURIofAtomUnreadURI(URI atomUnreadURI) {
        if (atomUnreadURI == null) {
            return null;
        }
        Matcher m = atomUriPattern.matcher(atomUnreadURI.toString());
        m.find();
        return URI.create(m.group());
    }

    /**
     * Transforms the specified URI, which may be a resource URI or a page URI, to a
     * data URI. If the specified URI doesn't start with the right prefix, it's
     * returned unchanged.
     *
     * @param pageOrResourceURI
     * @return
     */
    public URI toDataURIIfPossible(URI pageOrResourceURI) {
        String fromURI = resolveAgainstGeneralURIPrefix(pageOrResourceURI);
        if (fromURI.startsWith(this.pageURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.pageURIPrefix, this.dataURIPrefix));
        }
        if (fromURI.startsWith(this.resourceURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.resourceURIPrefix, this.dataURIPrefix));
        }
        return pageOrResourceURI;
    }

    /**
     * Transforms the specified URI, which may be a resource URI or a page URI, to a
     * page URI. If the specified URI doesn't start with the right prefix, it's
     * returned unchanged.
     *
     * @param dataOrResourceURI
     * @return
     */
    public URI toPageURIIfPossible(URI dataOrResourceURI) {
        String fromURI = resolveAgainstGeneralURIPrefix(dataOrResourceURI);
        if (fromURI.startsWith(this.dataURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.dataURIPrefix, this.pageURIPrefix));
        }
        if (fromURI.startsWith(this.resourceURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.resourceURIPrefix, this.pageURIPrefix));
        }
        return dataOrResourceURI;
    }

    /**
     * Transforms the specified URI, which may be a resource URI or a page URI, to a
     * resource URI. If the specified URI doesn't start with the right prefix, it's
     * returned unchanged.
     *
     * @param pageOrDataURI
     * @return
     */
    public URI toResourceURIIfPossible(URI pageOrDataURI) {
        String fromURI = resolveAgainstGeneralURIPrefix(pageOrDataURI);
        if (fromURI.startsWith(this.dataURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.dataURIPrefix, this.resourceURIPrefix));
        }
        if (fromURI.startsWith(this.pageURIPrefix)) {
            return URI.create(fromURI.replaceFirst(this.pageURIPrefix, this.resourceURIPrefix));
        }
        return pageOrDataURI;
    }

    private String resolveAgainstGeneralURIPrefix(final URI uri) {
        if (uri.isAbsolute()) {
            return uri.toString();
        }
        return URI.create(generalURIPrefix).resolve(uri).toString();
    }

    public URI createAtomURIForId(String id) {
        return URI.create(atomResourceURIPrefix + "/" + id);
    }

    public URI createConnectionURIForId(String atomId, String connectionId) {
        return URI.create(atomResourceURIPrefix + "/" + atomId + "/c/" + connectionId);
    }

    public URI createConnectionContainerURIForAtom(URI atomURI) {
        return URI.create(atomURI.toString() + "/c");
    }

    public URI createMessageContainerURIForConnection(URI connURI) {
        return URI.create(connURI.toString() + "/msg");
    }

    public URI createMessageURIForId(String id) {
        return WonMessageUriHelper.createMessageURIForId(id);
    }

    public URI createAttachmentURIForId(String id) {
        return URI.create(attachmentResourceURIPrefix + "/" + id);
    }

    public URI createAclGraphURIForAtomURI(URI atomUri) {
        return URI.create(atomUri + Atom.ACL_GRAPH_URI_FRAGMENT);
    }

    public URI createSysInfoGraphURIForAtomURI(final URI atomURI) {
        // TODO: [SECURITY] it's possible to submit atom data that clashes with this
        // name,
        // which may lead to undefined behavior
        return URI.create(atomURI + "#sysinfo");
    }

    public String getAtomResourceURIPrefix() {
        return atomResourceURIPrefix;
    }

    public String getMessageResourceURIPrefix() {
        return messageResourceURIPrefix;
    }

    public void setDataURIPrefix(final String dataURIPrefix) {
        this.dataURIPrefix = dataURIPrefix;
    }

    public String getResourceURIPrefix() {
        return resourceURIPrefix;
    }

    public void setResourceURIPrefix(final String resourceURIPrefix) {
        this.resourceURIPrefix = resourceURIPrefix;
    }

    public void setPageURIPrefix(final String pageURIPrefix) {
        this.pageURIPrefix = pageURIPrefix;
    }

    public String getGeneralURIPrefix() {
        return this.generalURIPrefix;
    }

    public void setGeneralURIPrefix(final String generalURIPrefix) {
        this.generalURIPrefix = generalURIPrefix;
    }

    public URI toLocalMessageURI(URI messageURI) {
        return WonMessageUriHelper.toLocalMessageURI(messageURI, this.messageResourceURIPrefix);
    }

    public URI toGenericMessageURI(URI localMessageURI) {
        return WonMessageUriHelper.toGenericMessageURI(localMessageURI, this.messageResourceURIPrefix);
    }
}
