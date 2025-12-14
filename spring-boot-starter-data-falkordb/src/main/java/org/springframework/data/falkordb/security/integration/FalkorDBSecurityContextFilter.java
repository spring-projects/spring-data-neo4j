/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.integration;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that populates {@link FalkorSecurityContextHolder} from the
 * current Spring Security {@link Authentication} on each request using
 * {@link AuthenticationFalkorSecurityContextAdapter}.
 *
 * This filter is not automatically registered in the Spring Security filter chain;
 * applications should register it explicitly, for example:
 *
 * <pre>
 * http.addFilterAfter(falkorDBSecurityContextFilter, SecurityContextPersistenceFilter.class);
 * </pre>
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorDBSecurityContextFilter extends OncePerRequestFilter {

	private final AuthenticationFalkorSecurityContextAdapter adapter;

	public FalkorDBSecurityContextFilter(AuthenticationFalkorSecurityContextAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			FalkorSecurityContext ctx = this.adapter.fromAuthentication(authentication);
			if (ctx != null) {
				FalkorSecurityContextHolder.setContext(ctx);
			}
			filterChain.doFilter(request, response);
		}
		finally {
			FalkorSecurityContextHolder.clearContext();
		}
	}

}