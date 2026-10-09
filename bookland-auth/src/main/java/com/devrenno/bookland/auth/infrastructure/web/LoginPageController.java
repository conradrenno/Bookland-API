package com.devrenno.bookland.auth.infrastructure.web;

import com.devrenno.bookland.auth.infrastructure.config.AuthorizationServerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the login form of the Authorization Server, with the storefront's look.
 *
 * <p>Only the {@code GET}. The form posts back to {@code /login}, which Spring Security's own
 * {@code UsernamePasswordAuthenticationFilter} processes in the login chain — this controller never
 * sees a password. Replacing the generated page changes nothing else about the flow: the field names
 * ({@code username}, {@code password}), the CSRF token and the {@code ?error} / {@code ?logout}
 * redirects stay the framework's.
 *
 * <p>The page lives here, not in the BFF, on purpose. The password must be typed on the
 * Authorization Server's origin — that is the point of the authorization code flow — and a form on
 * another origin could not carry this session's CSRF token. So the storefront's look comes here
 * instead: same palette and typefaces as the Next.js front, which the page links back to.
 */
@Controller
@RequiredArgsConstructor
public class LoginPageController {

    private final AuthorizationServerProperties properties;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("storefrontUrl", stripTrailingSlash(properties.getStorefrontUrl()));
        return "login";
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
