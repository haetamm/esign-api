package com.esign.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.esign.repository.UserRepository;
import com.esign.service.impl.MyUserDetailService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class BeanConfiguration {

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    public boolean comparePassword(String password, String encodedPassword) {
        PasswordEncoder passwordEncoder = passwordEncoder();
        return passwordEncoder.matches(password, encodedPassword);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> myAuthenticationProviders) {
        return new ProviderManager(myAuthenticationProviders);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            @Qualifier("myUserDetailService") UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public MyUserDetailService myUserDetailService(UserRepository userRepository) {
        return new MyUserDetailService(userRepository);
    }
}
