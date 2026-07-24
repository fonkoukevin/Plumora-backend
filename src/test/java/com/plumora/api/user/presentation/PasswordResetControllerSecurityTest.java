package com.plumora.api.user.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.security.CustomUserDetailsService;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.shared.security.RestAccessDeniedHandler;
import com.plumora.api.shared.security.RestAuthenticationEntryPoint;
import com.plumora.api.shared.security.SecurityConfig;
import com.plumora.api.user.application.AuthService;
import com.plumora.api.user.application.PasswordResetService;
import com.plumora.api.user.application.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression test for the bug reported live: the "Mot de passe oublié ?" screen called
 * POST /auth/forgot-password and got back "Authentication is required to access this resource"
 * because the route was never added to SecurityConfig's permitAll list (nor did it exist at all
 * on the backend). Both routes must be reachable with no Authorization header.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class PasswordResetControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;

	@MockBean
	private UserService userService;

	@MockBean
	private PasswordResetService passwordResetService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void forgotPasswordIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"reader@example.com\"}"))
			.andExpect(status().isOk());

		verify(passwordResetService).requestReset(any());
	}

	@Test
	void forgotPasswordRejectsAMalformedEmail() throws Exception {
		mockMvc.perform(post("/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void resetPasswordIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"some-token\",\"newPassword\":\"NewPassword123\"}"))
			.andExpect(status().isOk());

		verify(passwordResetService).resetPassword(any());
	}

	@Test
	void resetPasswordRejectsAPasswordShorterThanEightCharacters() throws Exception {
		mockMvc.perform(post("/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"some-token\",\"newPassword\":\"short\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void resetPasswordReturnsBadRequestForAnInvalidOrExpiredToken() throws Exception {
		doThrow(new BusinessException("This password reset link is invalid or has expired."))
			.when(passwordResetService).resetPassword(any());

		mockMvc.perform(post("/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"expired-token\",\"newPassword\":\"NewPassword123\"}"))
			.andExpect(status().isBadRequest());
	}
}
