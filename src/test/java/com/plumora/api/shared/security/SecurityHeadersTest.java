package com.plumora.api.shared.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.book.application.CatalogService;
import com.plumora.api.book.presentation.CatalogController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OWASP A02 (Security Misconfiguration) regression guard: X-Frame-Options,
 * X-Content-Type-Options and Strict-Transport-Security are set by Caddy in production (see
 * deploy/Caddyfile's "security_headers"/"hsts" snippets) and must never come from Spring Security
 * too - a duplicate X-Frame-Options previously disagreed outright (Spring's default DENY vs
 * Caddy's SAMEORIGIN), confirmed live against production with curl -I before this fix.
 */
@WebMvcTest(controllers = CatalogController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=https://app.plumora.fr")
class SecurityHeadersTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CatalogService catalogService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void springDoesNotDuplicateTheHeadersCaddyAlreadySetsInProduction() throws Exception {
		when(catalogService.getBooks(0, 20)).thenReturn(Page.empty());

		mockMvc.perform(get("/catalog/books"))
			.andExpect(status().isOk())
			.andExpect(header().doesNotExist("X-Frame-Options"))
			.andExpect(header().doesNotExist("X-Content-Type-Options"))
			.andExpect(header().doesNotExist("Strict-Transport-Security"));
	}
}
