package br.com.nandesk.shared.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** ETag nos GET públicos (9.14): a revalidação com {@code If-None-Match} devolve 304 sem corpo. */
@Configuration(proxyBeanMethods = false)
public class WebConfig {

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> publicEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns(
                "/api/v1/markets/*",
                "/api/v1/markets",
                "/api/v1/stalls/*",
                "/api/v1/search",
                "/api/v1/search/*",
                "/api/v1/categories",
                "/api/v1/categories/*",
                "/api/v1/products/*");
        registration.setName("publicEtagFilter");
        return registration;
    }
}
