package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.adapters.viewmodel.RegisteredUserViewModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Signs the caller in immediately after registration, by writing the session the Authorization
 * Server reads.
 *
 * <p>This is what replaced "and then issue the tokens" at the end of registering. The user has just
 * proven who they are by choosing the password, so asking for it again at
 * {@code /oauth2/authorize} would be theatre. The client goes there next, the server finds a
 * session, and the authorization code comes back without a login form.
 *
 * <p><strong>The principal must be {@link BooklandUserDetails}.</strong> It is the object the token
 * customizer reads the user id from when the code is exchanged; any other principal type and
 * {@code sub} silently falls back to the framework default, which is the e-mail. The password hash
 * is left null on purpose — nothing re-checks a password from a session.
 *
 * <p><strong>Why the session is written by hand.</strong> The registration endpoint is matched by
 * the API chain, which is {@code STATELESS}: its configured {@code SecurityContextRepository} is the
 * null one, so saving through the chain would silently do nothing. That policy is right for the API
 * and wrong for this one endpoint, and the cheapest honest resolution is to create the session
 * explicitly here rather than move the route onto a stateful chain — which would also drag CSRF onto
 * a JSON endpoint that has never had it.
 */
@Component
public class RegistrationSessionEstablisher {

    public void establish(HttpServletRequest request, RegisteredUserViewModel user) {
        BooklandUserDetails principal =
                new BooklandUserDetails(user.id(), user.email(), user.name(), null, user.role(), true);

        // The password factor, as a form login would record it: the caller proved it by choosing the
        // password. The Authorization Server reads the id_token's auth_time from this authority —
        // and, once the session is found for the sid claim, refuses to issue one without it.
        List<GrantedAuthority> authorities = new ArrayList<>(principal.getAuthorities());
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));

        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
