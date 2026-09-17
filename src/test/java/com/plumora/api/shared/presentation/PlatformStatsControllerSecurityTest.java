package com.plumora.api.shared.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.shared.application.PlatformStatsService;
import com.plumora.api.shared.security.CustomUserDetailsService;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.shared.security.RestAccessDeniedHandler;
import com.plumora.api.shared.security.RestAuthenticationEntryPoint;
import com.plumora.api.shared.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The landing page's "50k+ Histoires / 12k+ Auteurs / 200k+ Lecteurs" tiles are hardcoded in the
 * frontend and need a public endpoint to read the real counts from before launch - it must be
 * reachable with no Authorization header, same requirement as /auth/forgot-password previously.
 */
@WebMvcTest(controllers = PlatformStatsController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class PlatformStatsControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PlatformStatsService platformStatsService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void platformStatsAreReachableWithoutAuthentication() throws Exception {
		when(platformStatsService.getPublicStats()).thenReturn(new PlatformStatsResponse(50_000, 12_000, 200_000));

		mockMvc.perform(get("/stats/platform"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalBooks").value(50_000))
			.andExpect(jsonPath("$.totalAuthors").value(12_000))
			.andExpect(jsonPath("$.totalReaders").value(200_000));
	}
}
