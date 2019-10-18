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
package won.node.service.impl;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.InitializingBean;

import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.util.WonMessageUriHelper;

/**
 * User: fkleedorfer Date: 06.11.12
 */
public class URIService implements InitializingBean {
    // prefix of any URI
    private String generalURIPrefix;
    // prefix of an atom resource
    private String atomResourceURIPrefix;
    // prefix of a connection resource
    private String connectionResourceURIPrefix;
    // prefix of an event resource
    private String eventResourceURIPrefix;
    // prefix of an attachment resource
    private String attachmentResourceURIPrefix;
    // prefix for URISs of RDF data
    private String dataURIPrefix;
    // prefix for URIs referring to real-world things
    private String resourceURIPrefix;
    // prefix for human readable pages
    private String pageURIPrefix;
    private Pattern connectionEventsPattern;
    private Pattern connectionUriPattern;
    private Pattern atomEventsPattern;
    private Pattern atomUnreadPattern;
    private Pattern atomUriPattern;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.connectionEventsPattern = Pattern.compile(connectionResourceURIPrefix + "/[a-zA-Z0-9]+/events");
        this.connectionUriPattern = Pattern.compile(connectionResourceURIPrefix + "/[a-zA-Z0-9]+");
        this.atomEventsPattern = Pattern.compile(atomResourceURIPrefix + "/[a-zA-Z0-9]+/events");
        this.atomUnreadPattern = Pattern.compile(atomResourceURIPrefix + "/[a-zA-Z0-9]+/unread");
        this.atomUriPattern = Pattern.compile(atomResourceURIPrefix + "/[a-zA-Z0-9]+");
    }

    public boolean isEventURI(URI toCheck) {
        if (toCheck == null)
            return false;
        return toCheck.toString().startsWith(eventResourceURIPrefix);
    }

    public boolean isAtomURI(URI toCheck) {
        if (toCheck == null)
            return false;
        return toCheck.toString().startsWith(atomResourceURIPrefix);
    }

    public boolean isConnectionURI(URI toCheck) {
        if (toCheck == null)
            return false;
        return toCheck.toString().startsWith(connectionResourceURIPrefix);
    }

    public boolean isAtomEventsURI(URI toCheck) {
        if (toCheck == null)
            return false;
        Matcher m = atomEventsPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isAtomUnreadURI(URI toCheck) {
        if (toCheck == null)
            return false;
        Matcher m = atomUnreadPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public boolean isConnectionEventsURI(URI toCheck) {
        if (toCheck == null)
            return false;
        Matcher m = connectionEventsPattern.matcher(toCheck.toString());
        return m.lookingAt();
    }

    public URI getConnectionURIofConnectionEventsURI(URI connectionEventsURI) {
        if (connectionEventsURI == null)
            return null;
        Matcher m = connectionUriPattern.matcher(connectionEventsURI.toString());
        m.find();
        return URI.create(m.group());
    }

    public URI getAtomURIofAtomEventsURI(URI atomEventsURI) {
        if (atomEventsURI == null)
            return null;
        Matcher m = atomUriPattern.matcher(atomEventsURI.toString());
        m.find();
        return URI.create(m.group());
    }

    public URI getAtomURIofAtomUnreadURI(URI atomUnreadURI) {
        if (atomUnreadURI == null)
            return null;
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
     * page URI. If the specified URI doesn't start with the right prefix, it's
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
        if (uri.isAbsolute())
            return uri.toString();
        return URI.create(generalURIPrefix).resolve(uri).toString();
    }

    public URI createAtomURIForId(String id) {
        return URI.create(atomResourceURIPrefix + "/" + id);
    }

    public URI createConnectionsURIForAtom(URI atomURI) {
        return URI.create(atomURI.toString() + "/connections");
    }

    public URI createEventsURIForConnection(URI connURI) {
        return URI.create(connURI.toString() + "/events");
    }

    public URI createConnectionURIForId(String id) {
        return URI.create(connectionResourceURIPrefix + "/" + id);
    }

    public URI createEventURIForId(String id) {
        return WonMessageUriHelper.createMessageURIForId(id);
    }

    public URI createAttachmentURIForId(String id) {
        return URI.create(attachmentResourceURIPrefix + "/" + id);
    }

    public URI createAtomURI(Atom atom) {
        return URI.create(atomResourceURIPrefix + "/" + atom.getId());
    }

    public URI createConnectionURI(Connection con) {
        return URI.create(connectionResourceURIPrefix + "/" + con.getId());
    }

    public void setAtomResourceURIPrefix(final String atomResourceURIPrefix) {
        this.atomResourceURIPrefix = atomResourceURIPrefix;
    }

    public void setConnectionResourceURIPrefix(final String connectionResourceURIPrefix) {
        this.connectionResourceURIPrefix = connectionResourceURIPrefix;
    }

    public void setEventResourceURIPrefix(final String eventResourceURIPrefix) {
        this.eventResourceURIPrefix = eventResourceURIPrefix;
    }

    public void setAttachmentResourceURIPrefix(String attachmentResourceURIPrefix) {
        this.attachmentResourceURIPrefix = attachmentResourceURIPrefix;
    }

    public void setDataURIPrefix(final String dataURIPrefix) {
        this.dataURIPrefix = dataURIPrefix;
    }

    public void setResourceURIPrefix(final String resourceURIPrefix) {
        this.resourceURIPrefix = resourceURIPrefix;
    }

    public void setPageURIPrefix(final String pageURIPrefix) {
        this.pageURIPrefix = pageURIPrefix;
    }

    public void setGeneralURIPrefix(final String generalURIPrefix) {
        this.generalURIPrefix = generalURIPrefix;
    }

    public URI createAtomSysInfoGraphURI(final URI atomURI) {
        // TODO: [SECURITY] it's possible to submit atom data that clashes with this
        // name,
        // which may lead to undefined behavior
        return URI.create(atomURI.toString() + "#sysinfo");
    }

    /**
     * Assumes the specified uri to be of the form [connectionURI]/event/[long event
     * id].
     * 
     * @param eventURI
     * @return
     */
    public Long getEventIdFromEventURI(final URI eventURI) {
        String path = eventURI.getPath();
        return new Long(path.substring(path.lastIndexOf("/") + 1));
    }

    public String getGeneralURIPrefix() {
        return this.generalURIPrefix;
    }

    public URI toLocalMessageURI(URI messageURI) {
        return WonMessageUriHelper.toLocalMessageURI(messageURI, this.eventResourceURIPrefix);
    }

    public URI toGenericMessageURI(URI localMessageURI) {
        return WonMessageUriHelper.toGenericMessageURI(localMessageURI, this.eventResourceURIPrefix);
    }
}
