package com.plumora.api.shared.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomUserDetailsService userDetailsService;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	public SecurityConfig(
		JwtAuthenticationFilter jwtAuthenticationFilter,
		CustomUserDetailsService userDetailsService,
		RestAuthenticationEntryPoint restAuthenticationEntryPoint,
		RestAccessDeniedHandler restAccessDeniedHandler
	) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.userDetailsService = userDetailsService;
		this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
		this.restAccessDeniedHandler = restAccessDeniedHandler;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			// OWASP A02 (Security Misconfiguration) - X-Frame-Options, X-Content-Type-Options and
			// Strict-Transport-Security are already set by Caddy in production (see Caddyfile's
			// "security_headers"/"hsts" snippets, the documented single source of truth for these
			// three) - Spring Security's own defaults for exactly these three were duplicating
			// them, and X-Frame-Options disagreed outright (Spring's default DENY vs Caddy's
			// SAMEORIGIN), confirmed live against production with curl -I. Disabled here so the
			// response carries each header exactly once, from the one place that owns it.
			// Spring's other header defaults (Cache-Control on sensitive responses, X-XSS-Protection)
			// are untouched: Caddy does not set those.
			.headers(headers -> headers
				.frameOptions(frame -> frame.disable())
				.contentTypeOptions(contentType -> contentType.disable())
				.httpStrictTransportSecurity(hsts -> hsts.disable())
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(restAuthenticationEntryPoint)
				.accessDeniedHandler(restAccessDeniedHandler)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/google").permitAll()
				.requestMatchers(HttpMethod.GET, "/catalog/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/external-books/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
				// EndpointRequest builds its matcher from Actuator's own resolved endpoint paths
				// (WebMvcEndpointHandlerMapping) rather than a hand-written string pattern: a
				// plain requestMatchers("/actuator/health", ...) intermittently mismatched the
				// actual dispatched path in some execution contexts (observed as a spurious 401
				// from TestRestTemplate-driven integration tests), because actuator endpoints
				// aren't registered as regular @RequestMapping routes that PathPatternRequestMatcher
				// resolves the same way. This is the Spring Boot-recommended, robust way to permit
				// specific actuator endpoints regardless of that.
				.requestMatchers(EndpointRequest.to("health", "info")).permitAll()
				.anyRequest().authenticated()
			)
			.authenticationProvider(authenticationProvider())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	@Bean
	AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
		@Value("${app.cors.allowed-origins}") String allowedOrigins
	) {
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
