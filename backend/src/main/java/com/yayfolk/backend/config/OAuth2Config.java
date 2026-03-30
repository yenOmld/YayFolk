package com.yayfolk.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@Configuration
@EnableWebSecurity
public class OAuth2Config {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final org.springframework.core.env.Environment environment;

    public OAuth2Config(JwtAuthenticationFilter jwtAuthenticationFilter,
                        org.springframework.core.env.Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors
                .configurationSource(request -> {
                    org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.Arrays.asList("http://localhost:5173", "http://localhost:5174", "http://u3d6a23b.natappfree.cc", "https://8.162.0.246", "http://8.162.0.246"));
                    config.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.Arrays.asList("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                })
            )
            .csrf().disable()
            // 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests(authorizeRequests ->
                authorizeRequests
                    .antMatchers("/", "/favicon.ico", "/api/oauth/**", "/api/login", "/api/register", "/api/send-code", "/api/reset-password", "/api/translate/**", "/api/public/**").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth2Login -> {
                boolean enabled = hasGithubCredentials();
                if (enabled) {
                    oauth2Login
                        .loginPage("/api/oauth/github/login")
                        .defaultSuccessUrl("http://localhost:5173", true);
                }
            })
            .anonymous().and()
            .exceptionHandling(exceptionHandling ->
                exceptionHandling
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\": 401, \"message\": \"Unauthorized\"}");
                    })
            );
        return http.build();
    }

    private boolean hasGithubCredentials() {
        String id = environment.getProperty("GITHUB_CLIENT_ID");
        String secret = environment.getProperty("GITHUB_CLIENT_SECRET");
        return id != null && !id.trim().isEmpty() && secret != null && !secret.trim().isEmpty();
    }

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    public ClientRegistrationRepository clientRegistrationRepository() {
        org.springframework.security.oauth2.client.registration.ClientRegistration registration =
            org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId("github")
                .clientId(environment.getProperty("GITHUB_CLIENT_ID", "placeholder"))
                .clientSecret(environment.getProperty("GITHUB_CLIENT_SECRET", "placeholder"))
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(environment.getProperty("GITHUB_REDIRECT_URI", "http://localhost:8080/api/oauth/github/callback"))
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
        return new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(registration);
    }



    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .build();

        DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}

