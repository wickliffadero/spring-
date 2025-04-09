package com.example.frauddetction.config;

import com.example.frauddetction.Repository.UserAccountRepository;
import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/dashboard/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/ussd/**").permitAll()
                .requestMatchers("/", "/register", "/css/**", "/js/**", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, 
                            HttpServletResponse response, Authentication authentication) 
                            throws IOException, ServletException {
                        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
                            response.sendRedirect("/admin/dashboard");
                        } else {
                            response.sendRedirect("/dashboard");
                        }
                    }
                })
                .permitAll()
                .failureHandler((request, response, exception) -> {
                    String username = request.getParameter("username");
                    UseraccountEntity user = userAccountRepository.findByUsername(username);
                    if (user != null && "BLACKLISTED".equals(user.getAccountStatus())) {
                        response.sendRedirect("/login?blacklisted");
                    } else {
                        response.sendRedirect("/login?error");
                    }
                })
            )
            .logout((logout) -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            UseraccountEntity user = userAccountRepository.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found");
            }
            if ("BLACKLISTED".equals(user.getAccountStatus())) {
                throw new UsernameNotFoundException("Account is blacklisted");
            }
            return user;
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
