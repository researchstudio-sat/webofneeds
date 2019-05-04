package won.protocol.service;

import java.net.URI;

/**
 * Service for managing won node related information and for generating URIs
 * required for messaging and creation of resources.
 */
public interface WonNodeInformationService {
    public WonNodeInfo getWonNodeInformation(URI wonNodeURI);

    /**
     * Generates a random event URI according to the URI pattern of the default won
     * node.
     *
     * @return
     */
    public URI generateEventURI();

    /**
     * Generates a random event URI according to the URI pattern of the specified
     * won node.
     *
     * @param wonNodeURI
     * @return
     */
    public URI generateEventURI(URI wonNodeURI);

    /**
     * Checks if the passed event URI is matching the won node default pattern
     * 
     * @param eventURI
     * @return
     */
    public boolean isValidEventURI(URI eventURI);

    /**
     * Checks if the passed event URI is matching the won node pattern
     * 
     * @param eventURI
     * @param wonNodeURI
     * @return
     */
    public boolean isValidEventURI(URI eventURI, URI wonNodeURI);

    /**
     * Generates a random connection URI according to the URI pattern of the default
     * won node.
     *
     * @return
     */
    public URI generateConnectionURI();

    /**
     * Generates a random connection URI according to the URI pattern of the
     * specified won node.
     *
     * @param wonNodeURI
     * @return
     */
    public URI generateConnectionURI(URI wonNodeURI);

    /**
     * Checks if the passed connection URI is matching the won default node pattern
     * 
     * @param connectionURI
     * @return
     */
    public boolean isValidConnectionURI(URI connectionURI);

    /**
     * Checks if the passed event URI is matching the won node pattern
     * 
     * @param connectionURI
     * @param wonNodeURI
     * @return
     */
    public boolean isValidConnectionURI(URI connectionURI, URI wonNodeURI);

    /**
     * Generates a random atom URI according to the URI pattern of the default won
     * node.
     *
     * @return
     */
    public URI generateAtomURI();

    /**
     * Generates a random atom URI according to the URI pattern of the specified won
     * node.
     *
     * @param wonNodeURI
     * @return
     */
    public URI generateAtomURI(URI wonNodeURI);

    /**
     * Checks if the passed atom URI is matching the won default node pattern
     * 
     * @param atomURI
     * @return
     */
    public boolean isValidAtomURI(URI atomURI);

    /**
     * Checks if the passed atom URI is matching the won node pattern
     * 
     * @param atomURI
     * @param wonNodeURI
     * @return
     */
    public boolean isValidAtomURI(URI atomURI, URI wonNodeURI);

    public URI getDefaultWonNodeURI();

    /**
     * Obtains the won node uri associated with the specified atom or connection
     * resource.
     *
     * @param resourceURI
     * @return the won node URI or null if none is found.
     */
    public URI getWonNodeUri(URI resourceURI);
}
