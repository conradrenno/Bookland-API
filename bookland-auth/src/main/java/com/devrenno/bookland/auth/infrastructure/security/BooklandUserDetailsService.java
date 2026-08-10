package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Where the traditional Spring Security flow enters the project — and where it must stop.
 *
 * <p>Password comparison moves from {@code LoginService}, which did it by hand through
 * {@code PasswordEncoderPort}, to {@code DaoAuthenticationProvider}, which does it against what this
 * service returns. That is the whole reason the Authorization Server can replace the hand-rolled
 * login: the framework already owns this part.
 *
 * <p>Confined to the auth module by design. If {@code UserDetails} ever turns up in orders or
 * catalog, something has escaped: those modules are resource servers and know only claims.
 */
@Service
@RequiredArgsConstructor
public class BooklandUserDetailsService implements UserDetailsService {

    private final UserLookupPort userLookupPort;

    @Override
    public BooklandUserDetails loadUserByUsername(String email) {
        return userLookupPort.findByEmail(email)
                .map(BooklandUserDetails::from)
                .orElseThrow(() -> new UsernameNotFoundException("No user for " + email));
    }
}
