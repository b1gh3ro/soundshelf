package com.soundshelf.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class RestClientConfig {

    /**
     * iTunes answers with {@code Content-Type: text/javascript} — a leftover from its
     * JSONP days — so the default Jackson converter refuses the body. Teaching this
     * one client to read that media type is cheaper than parsing responses by hand.
     */
    @Bean
    public RestClient itunesRestClient(AppProperties properties) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("text/javascript"),
                MediaType.TEXT_PLAIN));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.itunes().timeout().toMillis());
        requestFactory.setReadTimeout((int) properties.itunes().timeout().toMillis());

        return RestClient.builder()
                .baseUrl(properties.itunes().baseUrl())
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(jsonConverter);
                })
                .build();
    }

    @Bean
    public RestClient anthropicRestClient(AppProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout((int) properties.ai().timeout().toMillis());

        return RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .requestFactory(requestFactory)
                .build();
    }
}
