package com.plumora.api.admin.presentation;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.admin.application.AdminAuditLogService;
import com.plumora.api.admin.application.AdminService;
import com.plumora.api.shared.security.CustomUserDetailsService;
import com.plumora.api.shared.security.JwtService;
import com.plumora.api.shared.security.RestAccessDeniedHandler;
import com.plumora.api.shared.security.RestAuthenticationEntryPoint;
import com.plumora.api.shared.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that every /admin/** route is gated behind the ADMIN role: unauthenticated requests
 * are rejected before reaching the controller, authenticated non-admins are forbidden, and
 * admins go through to the (mocked) service layer.
 */
@WebMvcTest(controllers = AdminController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AdminService adminService;

	@MockBean
	private AdminAuditLogService auditLogService;

	@MockBean
	private JwtService jwtService;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@Test
	void unauthenticatedRequestIsRejected() throws Exception {
		mockMvc.perform(get("/admin/dashboard"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401));

		verifyNoInteractions(adminService);
	}

	@Test
	void authenticatedNonAdminIsForbidden() throws Exception {
		mockMvc.perform(get("/admin/dashboard").with(user("reader@example.com").roles("READER")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(adminService);
	}

	@Test
	void authenticatedAdminCanAccessDashboard() throws Exception {
		when(adminService.getDashboard()).thenReturn(new AdminDashboardDto(1, 1, 1, 1, 0, 0, 0, 0, 0, List.of()));

		mockMvc.perform(get("/admin/dashboard").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	@Test
	void authenticatedAdminCanAccessUsersAndReportsAndAuditLogs() throws Exception {
		when(adminService.getUsers(null, null, null)).thenReturn(List.of());
		when(adminService.getReports()).thenReturn(List.of());
		when(auditLogService.search(null, null, null, null, null)).thenReturn(List.of());

		mockMvc.perform(get("/admin/users").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
		mockMvc.perform(get("/admin/reports").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
		mockMvc.perform(get("/admin/audit-logs").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	@Test
	void authenticatedAuthorIsForbiddenFromUsersEndpoint() throws Exception {
		mockMvc.perform(get("/admin/users").with(user("author@example.com").roles("AUTHOR")))
			.andExpect(status().isForbidden());

		verifyNoInteractions(adminService);
	}
}
