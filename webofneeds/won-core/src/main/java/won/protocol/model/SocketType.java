package won.protocol.model;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import won.protocol.vocabulary.WON;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 12.09.13 Time: 18:11 To
 * change this template use File | Settings | File Templates.
 */
public enum SocketType {
    ChatSocket("wx-chat", "ChatSocket"), HolderSocket("wx-hold", "HolderSocket"),
    HoldableSocket("wx-hold", "HoldableSocket"),
    GroupSocket("wx-group", "GroupSocket"), ReviewSocket("wx-review", "ReviewSocket"),
    BuddySocket("wx-buddy", "BuddySocket"),
    CoordinatorSocket("CoordinatorSocket"),
    ParticipantSocket("ParticipantSocket"), CommentSocket("CommentSocket"),
    CommentModeratedSocket("CommentModeratedSocket"), CommentUnrestrictedSocket("CommentUnrestrictedSocket"),
    ControlSocket("ControlSocket"), BAPCCoordinatorSocket("BAPCCoordinatorSocket"),
    BAPCParticipantSocket("BAPCParticipantSocket"), BACCCoordinatorSocket("BACCCoordinatorSocket"),
    BACCParticipantSocket("BACCParticipantSocket"), BAAtomicPCCoordinatorSocket("BAAtomicPCCoordinatorSocket"),
    BAAtomicCCCoordinatorSocket("BAAtomicCCCoordinatorSocket");
    private static final Map<String, String> namespaces = new HashMap();
    static {
        namespaces.put("wx-chat", "https://w3id.org/won/ext/chat#");
        namespaces.put("wx-hold", "https://w3id.org/won/ext/hold#");
        namespaces.put("wx-group", "https://w3id.org/won/ext/group#");
        namespaces.put("wx-review", "https://w3id.org/won/ext/review#");
        namespaces.put("wx-buddy", "https://w3id.org/won/ext/buddy#");
        namespaces.put("won", "https://w3id.org/won/core#");
    }

    public static String[] getNames() {
        String[] ret = new String[SocketType.values().length];
        int i = 0;
        for (SocketType ft : SocketType.values())
            ret[i++] = ft.getURI().toString();
        return ret;
    }

    public static SocketType getSocketType(URI uri) {
        if (uri.equals(SocketType.ControlSocket.getURI()))
            return SocketType.ControlSocket;
        else if (uri.equals(SocketType.GroupSocket.getURI()))
            return SocketType.GroupSocket;
        else if (uri.equals(SocketType.ReviewSocket.getURI()))
            return SocketType.ReviewSocket;
        else if (uri.equals(SocketType.ChatSocket.getURI()))
            return SocketType.ChatSocket;
        else if (uri.equals(SocketType.HolderSocket.getURI()))
            return SocketType.HolderSocket;
        else if (uri.equals(SocketType.HoldableSocket.getURI()))
            return SocketType.HoldableSocket;
        else if (uri.equals(SocketType.CoordinatorSocket.getURI()))
            return SocketType.CoordinatorSocket;
        else if (uri.equals(SocketType.ParticipantSocket.getURI()))
            return SocketType.ParticipantSocket;
        else if (uri.equals(SocketType.BAPCCoordinatorSocket.getURI()))
            return SocketType.BAPCCoordinatorSocket;
        else if (uri.equals(SocketType.BAPCParticipantSocket.getURI()))
            return SocketType.BAPCParticipantSocket;
        else if (uri.equals(SocketType.BACCCoordinatorSocket.getURI()))
            return SocketType.BACCCoordinatorSocket;
        else if (uri.equals(SocketType.BACCParticipantSocket.getURI()))
            return SocketType.BACCParticipantSocket;
        else if (uri.equals(SocketType.BAAtomicPCCoordinatorSocket.getURI()))
            return SocketType.BAAtomicPCCoordinatorSocket;
        else if (uri.equals(SocketType.BAAtomicCCCoordinatorSocket.getURI()))
            return SocketType.BAAtomicCCCoordinatorSocket;
        else if (uri.equals(SocketType.CommentSocket.getURI()))
            return SocketType.CommentSocket;
        else if (uri.equals(SocketType.CommentModeratedSocket.getURI()))
            return SocketType.CommentModeratedSocket;
        else if (uri.equals(SocketType.CommentUnrestrictedSocket.getURI()))
            return SocketType.CommentUnrestrictedSocket;
        else {
            return null;
        }
    }

    private String name;
    private String nsPrefix;;

    SocketType(String name) {
        this.name = name;
        this.nsPrefix = WON.DEFAULT_PREFIX;
    }

    SocketType(String prefix, String name) {
        this(name);
        this.nsPrefix = prefix;
    }

    public URI getURI() {
        String ns = namespaces.get(nsPrefix);
        if (ns == null)
            throw new IllegalStateException("No namespace configured for prefix '" + nsPrefix + "'");
        return URI.create(ns + name);
    }
}
