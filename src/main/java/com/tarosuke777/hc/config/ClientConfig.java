package com.tarosuke777.hc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient hmsRestClient(RestClient.Builder builder, HmsApiProperties props) {
        return builder.baseUrl(props.getBaseUrl())
                .defaultHeaders(
                        headers -> headers.setBasicAuth(props.getUsername(), props.getPassword()))
                .build();
    }
}
