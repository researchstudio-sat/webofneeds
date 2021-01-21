package won.node.springsecurity.acl;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class WonAclContextFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
        WonAclRequestHelper.putContextInThreadLocal((HttpServletRequest) request);
        try {
            chain.doFilter(request, response);
        } finally {
            WonAclRequestHelper.removeContextFromThreadLocal();
        }
    }

    @Override
    public void destroy() {
    }
}
