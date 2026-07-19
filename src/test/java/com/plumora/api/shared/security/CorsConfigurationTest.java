package com.plumora.api.shared.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that CORS is enforced according to app.cors.allowed-origins (SecurityConfig) rather
 * than accepting any origin: only the domains explicitly configured may call the API from a
 * browser.
 */
@WebMvcTest(controllers = CatalogController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=https://app.plumora.fr,https://www.plumora.fr")
class CorsConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private CatalogService catalogService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void preflightFromAConfiguredOriginIsAllowed() throws Exception {
		mockMvc.perform(options("/catalog/books")
				.header(HttpHeaders.ORIGIN, "https://app.plumora.fr")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.plumora.fr"));
	}

	@Test
	void preflightFromAnUnconfiguredOriginIsRejected() throws Exception {
		mockMvc.perform(options("/catalog/books")
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void actualRequestFromAConfiguredOriginReceivesTheCorsHeader() throws Exception {
		when(catalogService.getBooks(0, 20)).thenReturn(Page.empty());

		mockMvc.perform(get("/catalog/books").header(HttpHeaders.ORIGIN, "https://www.plumora.fr"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://www.plumora.fr"));
	}

	@Test
	void actualRequestFromAnUnconfiguredOriginDoesNotReceiveTheCorsHeader() throws Exception {
		when(catalogService.getBooks(0, 20)).thenReturn(Page.empty());

		mockMvc.perform(get("/catalog/books").header(HttpHeaders.ORIGIN, "https://evil.example"))
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
