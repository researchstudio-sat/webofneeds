package won.protocol;

public final class WonConstants {
    private WonConstants() {
    }

    /**
     * Each message container keeps track of the messages that still need to be
     * 'confirmed', i.e. referenced by a subsequent message. All subsequent success
     * responses from the container will reference the unconfirmed messages. When
     * the referencing message is confirmed by the communication partner, the
     * referenced message is no longer regarded as unconfirmed. However, network
     * latency and server load can lead to situations with many 'in-flight'
     * messages. All in-flight messages are unconfirmed, so each message would
     * reference a growing number of other messages, congesting the system even
     * more. To alleviate the situation, the system keeps track of the number of
     * times an unconfirmed message has been referenced, and this number is bounded
     * by N=<code>UNCONFIRMED_QUEUE_SIZE</code>, which means that every unconfirmed
     * message is referenced at most N times by a subsequent message. This may lead
     * to messages ending up unconfirmed.
     */
    public static final int MAX_CONFIRMATIONS = 5;
}
