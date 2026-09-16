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
import com.plumora.api.user.application.EmailVerificationService;
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
 * Same regression shape as PasswordResetControllerSecurityTest: both routes must be reachable
 * with no Authorization header, or a registered user with an unconfirmed inbox has no way to
 * activate their account.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class EmailVerificationControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthService authService;

	@MockBean
	private UserService userService;

	@MockBean
	private PasswordResetService passwordResetService;

	@MockBean
	private EmailVerificationService emailVerificationService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void verifyEmailIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/auth/verify-email")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"some-token\"}"))
			.andExpect(status().isOk());

		verify(emailVerificationService).verifyEmail(any());
	}

	@Test
	void verifyEmailReturnsBadRequestForAnInvalidOrExpiredToken() throws Exception {
		doThrow(new BusinessException("This email verification link is invalid or has expired."))
			.when(emailVerificationService).verifyEmail(any());

		mockMvc.perform(post("/auth/verify-email")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"expired-token\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void resendVerificationIsReachableWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/auth/resend-verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"reader@example.com\"}"))
			.andExpect(status().isOk());

		verify(emailVerificationService).resendVerificationEmail(any());
	}

	@Test
	void resendVerificationRejectsAMalformedEmail() throws Exception {
		mockMvc.perform(post("/auth/resend-verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"not-an-email\"}"))
			.andExpect(status().isBadRequest());
	}
}
