package com.loanmust.loan.config;

import com.loanmust.loan.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Staff sign-in and role-based URL rules. The two public surfaces - the referral form a
 * partner shares (/refer/**) and the loan proposal a customer opens (/p/**) - are the only
 * unauthenticated pages besides sign-in. CSRF stays on for every form in the application.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Finance lands on the money screen, data entry on leads, everyone else on the dashboard. */
    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean finance = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_FINANCE"));
            boolean dataEntry = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DATA_ENTRY"));
            String target = finance ? "/finance" : dataEntry ? "/leads" : "/dashboard";
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/css/**", "/js/**", "/images/**", "/favicon.ico", "/favicon.svg",
                        "/login", "/error", "/", "/refer/**", "/p/**", "/no-access",
                        "/actuator/health", "/actuator/health/**").permitAll()
                // Administration is limited to Owner and Admin.
                .requestMatchers("/admin/**", "/settings/**", "/audit/**").hasAnyRole("OWNER", "ADMIN")
                // Money screens: bank payouts, referral payouts and fees.
                .requestMatchers("/commission/**", "/finance/**", "/referral-payouts/**")
                    .hasAnyRole("OWNER", "ADMIN", "FINANCE", "MANAGER")
                // People screens.
                .requestMatchers("/employees/**", "/attendance/**").hasAnyRole("OWNER", "ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, denied) ->
                        response.sendRedirect(request.getContextPath() + "/no-access"))
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }
}
