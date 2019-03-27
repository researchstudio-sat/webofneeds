package won.cryptography.service;

/**
 * User: fsalcher Date: 18.09.2014
 */
public interface RandomNumberService {
    /**
     * generates a URI safe random string with the given length that does not start
     * with a number. We do this so that we generate URIs for which prefixing will
     * always work with N3.js https://github.com/RubenVerborgh/N3.js/issues/121
     * 
     * @param length
     */
    public String generateRandomString(int length);
}
