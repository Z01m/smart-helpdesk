package com.smarthelpdesk.knowledgebase.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Constructed only in the security chain, so it is not registered twice as a servlet filter.
public class AccessTokenFilter extends OncePerRequestFilter {
    private final AccessTokenVerifier verifier;
    public AccessTokenFilter(AccessTokenVerifier verifier) { this.verifier = verifier; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null) {
            Authentication authentication;
            try { authentication = verifier.authenticate(header); }
            catch (RuntimeException ex) { response.sendError(401, "Invalid access token"); return; }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
