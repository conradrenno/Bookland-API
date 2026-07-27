package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.domain.exception.TokenExpiredException;
import com.devrenno.bookland.auth.domain.valueobject.Token;
import com.devrenno.bookland.websupport.security.AuthErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProviderPort;

    /**
     * A rejected token does not fail the request here — the chain carries on unauthenticated so
     * that public endpoints still work with a stale token in the header. What does happen is that
     * the reason is recorded on the request: it is the only thing that survives to
     * {@code RestAuthenticationEntryPoint}, which is what turns it into a 401 body telling the
     * client whether refreshing is worth attempting.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String tokenValue = authHeader.substring(7);
            try {
                Token token = tokenProviderPort.validate(tokenValue);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        token.email(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + token.role()))
                );
                auth.setDetails(token.userId());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (TokenExpiredException e) {
                reject(request, AuthErrorCode.TOKEN_EXPIRED);
            } catch (Exception e) {
                reject(request, AuthErrorCode.TOKEN_INVALID);
            }
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, AuthErrorCode error) {
        SecurityContextHolder.clearContext();
        request.setAttribute(AuthErrorCode.REQUEST_ATTRIBUTE, error);
    }
}
