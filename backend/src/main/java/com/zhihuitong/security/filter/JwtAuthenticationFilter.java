package com.zhihuitong.security.filter;

import com.zhihuitong.security.model.LoginPrincipal;
import com.zhihuitong.security.model.LoginSession;
import com.zhihuitong.security.service.JwtTokenService;
import com.zhihuitong.security.service.LoginSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService tokenService;
    private final LoginSessionService sessionService;

    public JwtAuthenticationFilter(JwtTokenService tokenService, LoginSessionService sessionService) {
        this.tokenService = tokenService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String sessionId = tokenService.parseSessionId(token);
                LoginSession session = sessionService.get(sessionId);
                if (session != null) {
                    LoginPrincipal principal = LoginPrincipal.fromSession(session);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException exception) {
                log.debug("JWT authentication failed: {}", exception.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String path = request.getRequestURI();
        if (path.startsWith("/photo/") || path.startsWith("/ws/")) {
            String queryToken = request.getParameter("access_token");
            return queryToken == null || queryToken.isBlank() ? null : queryToken;
        }
        return null;
    }
}
