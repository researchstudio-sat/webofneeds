package won.node.springsecurity.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import won.auth.AuthUtils;
import won.auth.model.AuthToken;
import won.cryptography.rdfsign.WebIdKeyLoader;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

public class WonAclAuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private WebIdKeyLoader webIdKeyLoader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
        try {
            logger.debug("checking for bearer token...");
            Optional<String> encodedToken = WonAclRequestHelper.getBearerToken(request);
            if (encodedToken.isPresent()) {
                logger.debug("found a bearer token, trying to decode it...");
                Optional<AuthToken> decodedToken = AuthUtils.parseToken(encodedToken.get(), webIdKeyLoader);
                if (decodedToken.isPresent()) {
                    logger.debug("bearer token decoded successfully for bearer {} issued by {} ",
                                    decodedToken.get().getTokenSub(), decodedToken.get().getTokenIss());
                    Authentication authentication = new WonAclTokenAuthentication(encodedToken.get(),
                                    decodedToken.get());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    logger.debug("unable to decode bearer token");
                }
            } else {
                logger.debug("no bearer token found");
            }
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}
