package com.chinhbean.bookinghotel.filters;

import com.chinhbean.bookinghotel.components.JwtTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor

public class JwtTokenFilter extends OncePerRequestFilter {
    @Value("${api.prefix}")
    private String apiPrefix;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtils jwtTokenUtils;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (isBypassToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String token = authHeader.substring(7);
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        final Map<String, String> identifier = jwtTokenUtils.extractIdentifier(token);
        if ((identifier != null)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String emailOrPhone = identifier.get("email") != null ? identifier.get("email") : identifier.get("phoneNumber");
            UserDetails userDetails = userDetailsService.loadUserByUsername(emailOrPhone);
            if (jwtTokenUtils.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }


    private boolean isBypassToken(@NonNull HttpServletRequest request) {
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                Pair.of(String.format("%s/users/generate-secret-key", apiPrefix), "GET"),
                Pair.of(String.format("%s/users/login", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/register", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/block-or-enable/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/hotels/get-hotels", apiPrefix), "GET"),
                Pair.of(String.format("%s/hotels/detail/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/hotels/search", apiPrefix), "GET"),
                Pair.of(String.format("%s/hotels/filter", apiPrefix), "POST"),
                Pair.of(String.format("%s/room-types/filter/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/room-types/get-room/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/room-types/get-all-room-status/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/bookings/create-booking", apiPrefix), "POST"),
                Pair.of(String.format("%s/payment/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/users/oauth2/facebook", apiPrefix), "POST"),
                Pair.of(String.format("%s/users/oauth2/google", apiPrefix), "GET"),
                Pair.of(String.format("%s/feedbacks/get-all-feedback/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/feedbacks/get-feedback/**", apiPrefix), "GET"),
                Pair.of(String.format("%s/forgot-password/send-otp/**", apiPrefix), "POST"),
                Pair.of(String.format("%s/forgot-password/verify-otp/**", apiPrefix), "POST"),
                Pair.of(String.format("%s/forgot-password/change-password/**", apiPrefix), "POST"),
                Pair.of("/api-docs", "GET"),
                Pair.of("/api-docs/**", "GET"),
                Pair.of("/swagger-resources", "GET"),
                Pair.of("/swagger-resources/**", "GET"),
                Pair.of("/configuration/ui", "GET"),
                Pair.of("/configuration/security", "GET"),
                Pair.of("/swagger-ui/**", "GET"),
                Pair.of("/swagger-ui.html", "GET"),
                Pair.of("/swagger-ui/index.html", "GET")
        );
        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();
        for (Pair<String, String> token : bypassTokens) {
            String path = token.getFirst();
            String method = token.getSecond();
            // Check if the request path and method match any pair in the bypassTokens list
            if (requestPath.matches(path.replace("**", ".*"))
                    && requestMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }
}
