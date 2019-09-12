package won.protocol.service;

import java.net.URI;

/**
 * Service for managing won node related information and for generating URIs
 * required for messaging and creation of resources.
 */
public interface WonNodeInformationService {
    WonNodeInfo getWonNodeInformation(URI wonNodeURI);

    /**
     * Generates a random event URI according to the URI pattern of the default won
     * node.
     *
     * @return
     */
    URI generateEventURI();

    /**
     * Generates a random event URI according to the URI pattern of the specified
     * won node.
     *
     * @param wonNodeURI
     * @return
     */
    URI generateEventURI(URI wonNodeURI);

    /**
     * Checks if the passed event URI is matching the won node default pattern
     * 
     * @param eventURI
     * @return
     */
    boolean isValidEventURI(URI eventURI);

    /**
     * Checks if the passed event URI is matching the won node pattern
     * 
     * @param eventURI
     * @param wonNodeURI
     * @return
     */
    boolean isValidEventURI(URI eventURI, URI wonNodeURI);

    /**
     * Generates a random connection URI according to the URI pattern of the default
     * won node.
     *
     * @return
     */
    URI generateConnectionURI();

    /**
     * Generates a random connection URI according to the URI pattern of the
     * specified won node.
     *
     * @param wonNodeURI
     * @return
     */
    URI generateConnectionURI(URI wonNodeURI);

    /**
     * Checks if the passed connection URI is matching the won default node pattern
     * 
     * @param connectionURI
     * @return
     */
    boolean isValidConnectionURI(URI connectionURI);

    /**
     * Checks if the passed event URI is matching the won node pattern
     * 
     * @param connectionURI
     * @param wonNodeURI
     * @return
     */
    boolean isValidConnectionURI(URI connectionURI, URI wonNodeURI);

    /**
     * Generates a random atom URI according to the URI pattern of the default won
     * node.
     *
     * @return
     */
    URI generateAtomURI();

    /**
     * Generates a random atom URI according to the URI pattern of the specified won
     * node.
     *
     * @param wonNodeURI
     * @return
     */
    URI generateAtomURI(URI wonNodeURI);

    /**
     * Checks if the passed atom URI is matching the won default node pattern
     * 
     * @param atomURI
     * @return
     */
    boolean isValidAtomURI(URI atomURI);

    /**
     * Checks if the passed atom URI is matching the won node pattern
     * 
     * @param atomURI
     * @param wonNodeURI
     * @return
     */
    boolean isValidAtomURI(URI atomURI, URI wonNodeURI);

    URI getDefaultWonNodeURI();

    /**
     * Obtains the won node uri associated with the specified atom or connection
     * resource.
     *
     * @param resourceURI
     * @return the won node URI or null if none is found.
     */
    URI getWonNodeUri(URI resourceURI);
}
