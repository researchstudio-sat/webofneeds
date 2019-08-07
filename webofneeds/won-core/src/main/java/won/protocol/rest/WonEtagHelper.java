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
package won.protocol.rest;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Utils for our very specific way of creating/parsing etags.
 */
public class WonEtagHelper {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final char VERSION_MEDIATYPE_DELIMITER = ' ';
    private String version = null;
    private MediaType mediaType = null;

    private WonEtagHelper(final String version, final MediaType mediaType) {
        this.version = version;
        this.mediaType = mediaType;
    }

    public static WonEtagHelper forVersion(String version) {
        if (version == null)
            return null;
        return new WonEtagHelper(version, null);
    }

    /**
     * Returns a WonEtagHelper if the specified string is a valid ETAG, combining
     * [VERSION] {[DELIMITER] [MEDIA-TYPE]} (curly brackets inticate 'optional')
     * Returns null if the specified etagValue is null or not valid.
     * 
     * @param etagValue
     * @return
     */
    public static WonEtagHelper fromEtagValue(String etagValue) {
        if (etagValue == null)
            return null;
        etagValue = etagValue.trim();
        if (etagValue.startsWith("W/")) {
            logger.debug("weak etag matching is not supported, cannot " + "process: " + etagValue);
            return null;
        }
        if (!etagValue.startsWith("\"")) {
            logger.debug("etag must start with '\"', cannot process: " + etagValue);
            return null;
        }
        if (!etagValue.endsWith("\"")) {
            logger.debug("etag must end with '\"', cannot process: " + etagValue);
            return null;
        }
        int index = etagValue.indexOf(VERSION_MEDIATYPE_DELIMITER);
        if (index == -1) {
            // delimiter not found. Assume only version
            return new WonEtagHelper(etagValue.substring(1, etagValue.length() - 1), null);
        }
        MediaType mt = null;
        try {
            mt = MediaType.parseMediaType(etagValue.substring(index, etagValue.length() - 1));
        } catch (Exception e) {
            logger.debug("not a valid media type in etag value, cannot process: " + etagValue);
            // not a valid media type
            return null;
        }
        return new WonEtagHelper(etagValue.substring(1, index), mt);
    }

    /**
     * Returns a new WonEtagHelper object created from the specified headers if
     * there is an ETAG header and the value parsed for the mediatype is actually
     * compatible with the mediatypes listed in the accept header. We assume that if
     * that is not the case, the server will have to produce a new result as the
     * mediatype cached by the client is different from the one the server will
     * produce now. If there is no Accept header, the helper object will be
     * returned. If parsing the ETAG does not reveal a MediaType, the helperObject
     * will not be returned.
     * 
     * @param headers
     * @return
     */
    public static WonEtagHelper fromHeaderIfCompatibleWithAcceptHeader(HttpHeaders headers) {
        WonEtagHelper helper = fromIfNoneMatchHeader(headers);
        if (helper == null || helper.getMediaType() == null) {
            return null;
        }
        if (headers.getAccept().stream().anyMatch(m -> m.isCompatibleWith(helper.getMediaType()))) {
            return helper;
        }
        return null;
    }

    public static WonEtagHelper fromIfNoneMatchHeader(HttpHeaders headers) {
        List<String> etags = headers.getIfNoneMatch();
        if (etags.size() == 0)
            return null;
        if (etags.size() > 1) {
            logger.info("found more than one If-None-Match header values, only using first one");
        }
        ;
        return fromEtagValue(etags.get(0));
    }

    public static WonEtagHelper fromEtagHeader(HttpHeaders headers) {
        return fromEtagValue(headers.getETag());
    }

    public static void setMediaTypeForEtagHeaderIfPresent(MediaType mediaType, HttpHeaders headers) {
        WonEtagHelper wonEtagHelper = fromEtagHeader(headers);
        if (wonEtagHelper != null) {
            wonEtagHelper.setMediaType(mediaType);
            setEtagHeader(wonEtagHelper, headers);
        }
    }

    public static void setEtagHeader(WonEtagHelper wonEtagHelper, HttpHeaders headers) {
        if (wonEtagHelper.isValid()) {
            headers.setETag(wonEtagHelper.getEtagString());
        }
    }

    public static String getVersionIdentifier(WonEtagHelper wonEtagHelper) {
        if (wonEtagHelper != null) {
            return wonEtagHelper.getVersion();
        }
        return null;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setMediaType(final MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getVersion() {
        return version;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isValid() {
        return version != null;
    }

    public String getEtagString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('"').append(version);
        if (mediaType != null) {
            stringBuilder.append(VERSION_MEDIATYPE_DELIMITER);
            stringBuilder.append(mediaType.toString());
        }
        stringBuilder.append('"');
        return stringBuilder.toString();
    }
}
