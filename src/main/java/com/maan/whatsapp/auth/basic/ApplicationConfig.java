package com.maan.whatsapp.auth.basic;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
public class ApplicationConfig  {

	// User Creation
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        // InMemoryUserDetailsManager setup with two users
        UserDetails admin = User.withUsername("whatsappchatapi")
                .password(encoder.encode("whatsappchatapi@123#"))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //Make the below setting as * to allow connection from any hos
        corsConfiguration.setAllowedOrigins(List.of("*"));
        corsConfiguration.setAllowedMethods(List.of("*"));
       // corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedHeaders(List.of("*"));        
        corsConfiguration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
    
    // Configuring HttpSecurity
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
		.cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/whatsapp/webhook","/insurance/buypolicy/**","/whatsapp/logo").permitAll() // Permit all access to /auth/welcome
                .requestMatchers("/**").authenticated() // Require authentication /**
            )
            
            .httpBasic(Customizer.withDefaults()); // Enable Basic Authentication with default settings
        
        return http.build();
    }

    // Password Encoding
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    private static final String HEALTHCHECK_URL = "/healthCheck/**";
    private static final String[] ALLOW_LIST = {
            HEALTHCHECK_URL,"/", "/resources/**", "/styles/**", "/static/**", "/jasper/**", "/public/**",
    				"/webui/**", "/h2-console/**", "/*.jsp", "/**/*.jsp", "/configuration/**", "/swagger-ui/**", "/ui/**",
    				"/swagger-resources/**", "/api-docs", "/api-docs/**", "/fonts/**", "/v2/api-docs/**", "/*.html",
    				"/**/*.html", "/*.jpg", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.gif", "/**/*.svg",
    				"/**/*.ico", "/**/*.ttf", "/**/*.woff", "/**/*.woff2", "/**/*.otf", "/whatsappflow/**","/whatsapp/webhook","/whatsapp/webhook/meta/**","/whatsapptemplate/document/download/**",
    "/insurance/buypolicy/**","/phoenix/whatsapp/webhook/zambia/**","/phoenix/whatsapp/webhook/namibia/**",
    "/insurance/generate/swaziland/quote","/insurance/phoenix/buypolicy/**","/insurance/generate/zambia/quote",
    "/phoenix/whatsapp/webhook/boatswana","api/recognition/**","/insurance/doc/namibia/response"};

    
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(ALLOW_LIST);
    }
}