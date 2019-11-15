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
package won.protocol.message;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import won.protocol.exception.WonMessageProcessingException;
import won.protocol.util.WonMessageUriHelper;
import won.protocol.vocabulary.WONMSG;

/**
 * Utilities for working with wonMessage objects.
 */
public class WonMessageUtils {
    public static Optional<URI> getSenderAtomURI(WonMessage msg) {
        URI atomUri = msg.getSenderAtomURI();
        if (atomUri == null) {
            atomUri = getSenderAtomURIFromSenderSocketURI(msg, atomUri);
        }
        return Optional.ofNullable(atomUri);
    }

    public static URI getSenderAtomURIFromSenderSocketURI(WonMessage msg, URI atomUri) {
        URI socketUri = msg.getSenderSocketURI();
        if (socketUri != null) {
            atomUri = stripFragment(socketUri);
        }
        return atomUri;
    }

    public static URI getSenderAtomURIRequired(WonMessage msg) {
        return getSenderAtomURI(msg)
                        .orElseThrow(() -> new WonMessageProcessingException("Could not obtain sender atom uri", msg));
    }

    public static Optional<URI> getRecipientAtomURI(WonMessage msg) {
        URI atomUri = msg.getRecipientAtomURI();
        if (atomUri == null) {
            atomUri = getRecipientAtomURIFromRecipientSocketURI(msg);
        }
        return Optional.ofNullable(atomUri);
    }

    public static URI getRecipientAtomURIFromRecipientSocketURI(WonMessage msg) {
        URI socketUri = msg.getRecipientSocketURI();
        if (socketUri != null) {
            return stripFragment(socketUri);
        }
        return null;
    }

    public static URI getRecipientAtomURIRequired(WonMessage msg) {
        return getRecipientAtomURI(msg).orElseThrow(
                        () -> new WonMessageProcessingException("Could not obtain recipient atom uri", msg));
    }

    /**
     * Returns the atom that this message belongs to.
     * 
     * @param message
     * @return
     */
    public static Optional<URI> getParentAtomUri(final WonMessage message, WonMessageDirection direction) {
        if (direction.isFromExternal()) {
            return Optional.of(message.getRecipientAtomURI());
        } else {
            return Optional.of(message.getSenderAtomURI());
        }
    }

    public static URI stripFragment(URI uriWithFragment) {
        URI atomUri;
        Objects.requireNonNull(uriWithFragment);
        // just strip the fragment
        String fragment = uriWithFragment.getRawFragment();
        if (fragment == null) {
            return uriWithFragment;
        }
        String uri = uriWithFragment.toString();
        atomUri = URI.create(uri.substring(0, uri.length() - fragment.length() - 1));
        return atomUri;
    }

    public static URI stripAtomSuffix(URI atomURI) {
        Objects.requireNonNull(atomURI);
        String uri = atomURI.toString();
        return URI.create(uri.replaceFirst("/atom/.+$", ""));
    }

    /**
     * Get the targetAtom of a hint (even if it is a SocketHint).
     * 
     * @param wonMessage
     * @return
     */
    public static URI getHintTargetAtomURIRequired(WonMessage wonMessage) {
        return getHintTargetAtomURI(wonMessage).orElseThrow(
                        () -> new WonMessageProcessingException("Could not obtain target atom uri", wonMessage));
    }

    /**
     * Get the targetAtom of a hint (even if it is a SocketHint).
     * 
     * @param wonMessage
     * @return
     */
    public static Optional<URI> getHintTargetAtomURI(WonMessage wonMessage) {
        URI atomUri = wonMessage.getHintTargetAtomURI();
        if (atomUri == null) {
            URI socketUri = wonMessage.getHintTargetSocketURI();
            if (socketUri != null) {
                atomUri = stripFragment(socketUri);
            }
        }
        return Optional.ofNullable(atomUri);
    }

    public static boolean isValidMessageUri(URI messageURI) {
        if (messageURI == null) {
            return false;
        }
        if (Objects.equals(messageURI, WonMessageUriHelper.getSelfUri())) {
            return false;
        }
        return messageURI.toString().startsWith(WONMSG.MESSAGE_URI_PREFIX);
    }
}
