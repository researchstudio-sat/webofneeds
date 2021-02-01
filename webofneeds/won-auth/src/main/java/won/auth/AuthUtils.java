package won.auth;

import io.jsonwebtoken.*;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.model.*;
import won.cryptography.rdfsign.WebIdKeyLoader;

import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.security.PrivateKey;
import java.util.*;

public class AuthUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String JWT_FIELD_SIG = "sig";
    private static final String JWT_FIELD_SCOPE = "scope";

    public static Optional<Long> getExpiresAfterSecondsLong(TokenSpecification spec) {
        if (spec.getExpiresAfterInteger() != null) {
            return Optional.of(spec.getExpiresAfterInteger().longValue());
        } else if (spec.getExpiresAfterLong() != null) {
            return Optional.of(spec.getExpiresAfterLong().longValue());
        } else if (spec.getExpiresAfterBigInteger() != null) {
            return Optional.of(spec.getExpiresAfterBigInteger().longValueExact());
        }
        return Optional.empty();
    }

    public static Optional<Integer> getExpiresAfterSecondsInteger(TokenSpecification spec) {
        if (spec.getExpiresAfterInteger() != null) {
            return Optional.of(spec.getExpiresAfterInteger().intValue());
        } else if (spec.getExpiresAfterLong() != null) {
            return Optional.of(spec.getExpiresAfterLong().intValue());
        } else if (spec.getExpiresAfterBigInteger() != null) {
            return Optional.of(spec.getExpiresAfterBigInteger().intValueExact());
        }
        return Optional.empty();
    }

    public static won.protocol.model.ConnectionState fromAuthConnectionState(
                    won.auth.model.ConnectionState connectionState) {
        switch (connectionState) {
            case CLOSED:
                return won.protocol.model.ConnectionState.CLOSED;
            case CONNECTED:
                return won.protocol.model.ConnectionState.CONNECTED;
            case SUGGESTED:
                return won.protocol.model.ConnectionState.SUGGESTED;
            case REQUEST_RECEIVED:
                return won.protocol.model.ConnectionState.REQUEST_RECEIVED;
            case REQUEST_SENT:
                return won.protocol.model.ConnectionState.REQUEST_SENT;
            default:
                throw new IllegalArgumentException("Cannot convert connection state " + connectionState);
        }
    }

    public static ConnectionState toAuthConnectionState(
                    won.protocol.model.ConnectionState connectionState) {
        switch (connectionState) {
            case CLOSED:
                return ConnectionState.CLOSED;
            case CONNECTED:
                return ConnectionState.CONNECTED;
            case SUGGESTED:
                return ConnectionState.SUGGESTED;
            case REQUEST_RECEIVED:
                return ConnectionState.REQUEST_RECEIVED;
            case REQUEST_SENT:
                return ConnectionState.REQUEST_SENT;
            default:
                throw new IllegalArgumentException("Cannot convert connection state " + connectionState);
        }
    }

    public static won.protocol.model.AtomState fromAuthAtomState(AtomState atomState) {
        switch (atomState) {
            case ACTIVE:
                return won.protocol.model.AtomState.ACTIVE;
            case INACTIVE:
                return won.protocol.model.AtomState.INACTIVE;
            case DELETED:
                return won.protocol.model.AtomState.DELETED;
            default:
                throw new IllegalArgumentException("Cannot convert atom state " + atomState);
        }
    }

    public static won.auth.model.AtomState toAuthAtomState(won.protocol.model.AtomState atomState) {
        switch (atomState) {
            case ACTIVE:
                return won.auth.model.AtomState.ACTIVE;
            case INACTIVE:
                return won.auth.model.AtomState.INACTIVE;
            case DELETED:
                return won.auth.model.AtomState.DELETED;
            default:
                throw new IllegalArgumentException("Cannot convert atom state " + atomState);
        }
    }

    /**
     * Returns a shallow clone of the provided <code>OperationRequest</code>. The
     * <code>set*()</code> methods are safe to use on the result.
     * 
     * @param toClone
     * @return
     */
    public static OperationRequest cloneShallow(OperationRequest toClone) {
        if (toClone == null) {
            return null;
        }
        OperationRequest clone = new OperationRequest();
        clone.setOperationTokenOperationExpression(toClone.getOperationTokenOperationExpression());
        clone.setOperationSimpleOperationExpression(toClone.getOperationSimpleOperationExpression());
        clone.setOperationMessageOperationExpression(toClone.getOperationMessageOperationExpression());
        clone.setRequestor(toClone.getRequestor());
        clone.setReqAtom(toClone.getReqAtom());
        clone.setReqAtomState(toClone.getReqAtomState());
        clone.setReqAtomMessages(toClone.getReqAtomMessages());
        clone.setReqConnection(toClone.getReqConnection());
        clone.setReqConnections(toClone.getReqConnections());
        clone.setReqConnectionMessage(toClone.getReqConnectionMessage());
        clone.setReqConnectionMessages(toClone.getReqConnectionMessages());
        clone.setReqConnectionState(toClone.getReqConnectionState());
        clone.setReqConnectionTargetAtom(toClone.getReqConnectionTargetAtom());
        clone.setReqGraphs(Collections.unmodifiableSet(toClone.getReqGraphs()));
        clone.setReqGraphTypes(Collections.unmodifiableSet(toClone.getReqGraphTypes()));
        clone.setReqPosition(toClone.getReqPosition());
        clone.setReqSocket(toClone.getReqSocket());
        clone.setReqSocketType(toClone.getReqSocketType());
        clone.setBearsEncodedTokens(Collections.unmodifiableSet(toClone.getBearsEncodedTokens()));
        clone.setBearsTokens(Collections.unmodifiableSet(toClone.getBearsTokens()));
        return clone;
    }

    /**
     * Creates a JSON Web Token using `iss`, `sub`, `exp` and `iat` from the
     * provided AuthToken. `sig` is set as the provided `webid` if it is different
     * from the value of `iss`.
     * 
     * @param authToken
     * @param key
     * @param webId
     * @return
     */
    public static String toJWT(AuthToken authToken, PrivateKey key, String webId) {
        Header header = Jwts.jwsHeader();
        Objects.requireNonNull(authToken);
        Objects.requireNonNull(key);
        Objects.requireNonNull(webId);
        Objects.requireNonNull(authToken.getTokenExp());
        Objects.requireNonNull(authToken.getTokenIat());
        Objects.requireNonNull(authToken.getTokenIss());
        Objects.requireNonNull(authToken.getTokenSub());
        JwtBuilder jwtBuilder = Jwts.builder()
                        .setHeader((Map) header)
                        .setIssuer(authToken.getTokenIss().toString())
                        .setIssuedAt(authToken.getTokenIat().asCalendar().getTime())
                        .setExpiration(authToken.getTokenExp().asCalendar().getTime())
                        .setSubject(authToken.getTokenSub().toString());
        if (!authToken.getTokenIss().toString().equals(webId)) {
            jwtBuilder.claim(JWT_FIELD_SIG, webId);
        }
        if (authToken.getTokenScopeURI() != null) {
            jwtBuilder.claim(JWT_FIELD_SCOPE, authToken.getTokenScopeURI().toString());
        } else if (authToken.getTokenScopeString() != null) {
            jwtBuilder.claim(JWT_FIELD_SCOPE, authToken.getTokenScopeString());
        } else {
            throw new IllegalArgumentException("Cannot create token without scope field");
        }
        jwtBuilder.signWith(key, SignatureAlgorithm.ES256);
        return jwtBuilder.compact();
    }

    public static Optional<AuthToken> parseToken(String token, WebIdKeyLoader webIdKeyLoader) {
        try {
            SigningKeyResolver keyResolver = new WebIdKeyLoadingSigningKeyResolver(webIdKeyLoader);
            Jws<Claims> jwt = Jwts.parserBuilder()
                            .setSigningKeyResolver(keyResolver)
                            .build()
                            .parseClaimsJws(token);
            AuthToken authToken = new AuthToken();
            String scope = jwt.getBody().get(JWT_FIELD_SCOPE, String.class);
            try {
                authToken.setTokenScopeURI(new URI(scope));
            } catch (URISyntaxException e) {
                authToken.setTokenScopeString(scope);
            }
            Claims claims = jwt.getBody();
            authToken.setTokenSub(URI.create(claims.getSubject()));
            authToken.setTokenIss(URI.create(claims.getIssuer()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(claims.getIssuedAt());
            authToken.setTokenIat(new XSDDateTime(cal));
            cal.setTime(claims.getExpiration());
            authToken.setTokenExp(new XSDDateTime(cal));
            String sig = claims.get(JWT_FIELD_SIG, String.class);
            if (sig != null) {
                authToken.setTokenSig(URI.create(sig));
            }
            return Optional.of(authToken);
        } catch (Exception e) {
            logger.debug("Error parsing token", e);
            return Optional.empty();
        }
    }

    /**
     * Returns the RDF representation of the specified object in TTL format.
     *
     * @param authEntity
     * @return
     */
    public static String toRdfString(GeneralVisitorHost authEntity) {
        Graph g = RdfOutput.toGraph(authEntity);
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, g, Lang.TTL);
        return writer.toString();
    }

    private static class WebIdKeyLoadingSigningKeyResolver extends SigningKeyResolverAdapter {
        private WebIdKeyLoader webIdKeyLoader;

        public WebIdKeyLoadingSigningKeyResolver(WebIdKeyLoader webIdKeyLoader) {
            this.webIdKeyLoader = webIdKeyLoader;
        }

        @Override
        public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
            String issuer = claims.getIssuer();
            String signer = claims.get(JWT_FIELD_SIG, String.class);
            String webid = null;
            if (signer != null) {
                webid = signer;
            } else {
                webid = issuer;
            }
            try {
                return webIdKeyLoader.loadKey(webid).stream().findFirst().get();
            } catch (Exception e) {
                logger.debug("cannot resolve signing key for {}", webid, e);
            }
            return null;
        }
    }
}
