package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @EnableMethodSecurity liga o @PreAuthorize usado nos services
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Regras da API (/api/**): login por HTTP Basic em cada requisição, sem sessão.
    // Como não usa cookie de sessão, não precisa de token CSRF.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/desconto", "/api/pedidos/*/cancelamento").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/api/pedidos/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("GERENTE")
                        .requestMatchers("/api/clientes/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").authenticated()
                        .requestMatchers("/api/produtos/**").hasRole("GERENTE")
                        .anyRequest().denyAll() // qualquer outra coisa (ex.: DELETE na auditoria) é bloqueada
                );
        return http.build();
    }

    // Regras das telas: login por formulário e CSRF ligado (padrão do Spring Security).
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/acesso-negado", "/error", "/css/**", "/dist/**").permitAll()
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/auditoria/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/pedidos/*/desconto", "/pedidos/*/cancelar").hasRole("GERENTE")
                        .requestMatchers("/pedidos/novo", "/pedidos/salvar", "/pedidos/*/editar", "/pedidos/*/status").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers("/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")
                        .requestMatchers("/clientes/*/excluir").hasRole("GERENTE")
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole("ATENDENTE")
                        .requestMatchers("/clientes/**").hasAnyRole("GERENTE", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET, "/produtos").authenticated()
                        .requestMatchers("/produtos/**").hasRole("GERENTE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"));

        return http.build();
    }
}
