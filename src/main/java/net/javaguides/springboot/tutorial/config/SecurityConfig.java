package net.javaguides.springboot.tutorial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
        // Allow the signup form to exist without requiring a token for the initial GET request
        .ignoringRequestMatchers("/signup", "/add-user") 
    )
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/home", "/login", "/signup", "/add-user", "/api/send-otp", "/api/verify-otp", "/error").permitAll()
        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
        .anyRequest().authenticated()
    )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/employees/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/home")
                .permitAll()
            );
        return http.build();
    }
}