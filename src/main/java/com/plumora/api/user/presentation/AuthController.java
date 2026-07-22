package com.plumora.api.user.presentation;

import com.plumora.api.user.application.AuthService;
import com.plumora.api.user.application.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	public AuthController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/google")
	public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
		return authService.loginWithGoogle(request);
	}

	@GetMapping("/me")
	public UserResponse me(Principal principal) {
		return UserMapper.toResponse(userService.getCurrentUser(principal.getName()));
	}
}
