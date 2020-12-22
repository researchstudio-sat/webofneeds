package won.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import won.auth.model.AtomState;
import won.auth.model.AuthToken;
import won.auth.model.ConnectionState;
import won.auth.model.OperationRequest;
import won.auth.model.TokenSpecification;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.cryptography.service.CryptographyService;

public class AuthUtils {
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
        clone.setReqGraphs(toClone.getReqGraphs());
        clone.setReqGraphTypes(toClone.getReqGraphTypes());
        clone.setReqPosition(toClone.getReqPosition());
        clone.setReqSocket(toClone.getReqSocket());
        clone.setReqSocketType(toClone.getReqSocketType());
        clone.setBearsEncodedTokens(toClone.getBearsEncodedTokens());
        clone.setBearsTokens(toClone.getBearsTokens());
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
        jwtBuilder.signWith(key, SignatureAlgorithm.HS256);
        return jwtBuilder.compact();
    }

    public static Optional<AuthToken> parseToken(String token, WebIdKeyLoader webIdKeyLoader) {
        try {
            Jws<Claims> jwt = Jwts.parserBuilder()
                            .setSigningKey(getKey(token, webIdKeyLoader))
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
            return Optional.empty();
        }
    }

    private static Key getKey(String token, WebIdKeyLoader webIdKeyLoader)
                    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        Jwt<Header, Claims> jwt = Jwts.parserBuilder().build().parseClaimsJwt(token);
        String issuer = jwt.getBody().getIssuer();
        String signer = jwt.getBody().get(JWT_FIELD_SIG, String.class);
        String webid = null;
        if (signer != null) {
            webid = signer;
        } else {
            webid = issuer;
        }
        return webIdKeyLoader.loadKey(webid).stream().findFirst().get();
    }
}
