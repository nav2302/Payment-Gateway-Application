package com.navdeep.security.jwt;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.navdeep.model.Role;
import com.navdeep.service.LocalUserDetailService;
import com.navdeep.util.SecurityCipher;

import lombok.extern.slf4j.Slf4j;

/**
 * @author nav
 *
 */
@Slf4j
public class TokenAuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private LocalUserDetailService customUserDetailsService;
	
	@Value("${authentication-test.auth.accessTokenCookieName}")
	private String accessTokenCookieName;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			String jwt = getJwtToken(request, true);

			if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
				Long userId = tokenProvider.getUserIdFromToken(jwt);

				UserDetails userDetails = customUserDetailsService.loadUserById(userId);
				Collection<? extends GrantedAuthority> authorities = tokenProvider.isAuthenticated(jwt)
						? userDetails.getAuthorities()
						: List.of(new SimpleGrantedAuthority(Role.ROLE_PRE_VERIFICATION_USER));
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, authorities);
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		} catch (Exception ex) {
			log.error("Could not set user authentication in security context", ex);
		}

		filterChain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return SecurityCipher.decrypt(bearerToken.substring(7, bearerToken.length()));
		}
		return null;
	}

	private String getJwtFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null)
			return "";
		for (Cookie cookie : cookies) {
			if (accessTokenCookieName.equals(cookie.getName())) {
				String accessToken = cookie.getValue();
				if (accessToken == null)
					return null;
				return SecurityCipher.decrypt(accessToken);
			}
		}
		return null;
	}

	private String getJwtToken(HttpServletRequest request, boolean fromCookie) {
		if (fromCookie)
			return getJwtFromCookie(request);
		return getJwtFromRequest(request);
	}
}