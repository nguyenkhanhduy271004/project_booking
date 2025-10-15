package com.booking.booking.config;

import com.cloudinary.Cloudinary;
import com.sendgrid.SendGrid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {

    @Value("${spring.sendgrid.api-key}")
    private String apiKey;

    @Value("${cloudinary.cloud_name}")
    private String cloudName;

    @Value("${cloudinary.api_key}")
    private String cloudApiKey;

    @Value("${cloudinary.api_secret}")
    private String cloudApiSecret;

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(apiKey);
    }

    @Bean
    public OidcUserService oidcUserService() {
        return new OidcUserService();
    }


    @Bean
    public Cloudinary cloudinary() {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", cloudApiKey);
        config.put("api_secret", cloudApiSecret);
        config.put("secure", true);
        return new Cloudinary(config);
    }
}
