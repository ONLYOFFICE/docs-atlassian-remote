/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.docs.atlassian.remote.configuration;

import com.onlyoffice.docs.atlassian.remote.security.RemoteAppAuthenticationFilter;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.time.Duration;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final RemoteAppJwtService remoteAppJwtService;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-ttl-in-days}")
    private long jwkTTLInDays;

    @Bean
    public SecurityFilterChain remoteAppAuthorizationFilterChain(final HttpSecurity http) throws Exception {
        http
                .securityMatchers(requestMatcherConfigurer -> {
                    requestMatcherConfigurer
                            .requestMatchers("/api/v1/remote/**");
                })
                .authorizeHttpRequests(auth ->
                        auth
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .addFilterAfter(new ForwardedHeaderFilter(), WebAsyncManagerIntegrationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public SecurityFilterChain editorAuthorizationFilterChain(final HttpSecurity http) throws Exception {
        http
                .securityMatchers(requestMatcherConfigurer -> {
                    requestMatcherConfigurer
                            .requestMatchers("/**");
                })
                .authorizeHttpRequests(auth ->
                                auth
                                        .requestMatchers("/api/v1/health").permitAll()
                                        .requestMatchers("/editor/**").authenticated()
                                        .requestMatchers("/api/**").authenticated()
                                        .anyRequest().permitAll()
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .exceptionHandling(
                        exceptions -> {
                                exceptions.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint());
                        }
                )
                .addFilterBefore(
                        new RemoteAppAuthenticationFilter(remoteAppJwtService),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(new ForwardedHeaderFilter(), WebAsyncManagerIntegrationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    JwkSetUriJwtDecoderBuilderCustomizer jwkCustomizer(final CacheManager cacheManager) {
        return (builder) -> {
            builder.cache(cacheManager.getCache("spring:forge-jwks"));
        };
    }

    @Bean
    public CacheManager cacheManager(final RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofDays(jwkTTLInDays)))
                .build();
    }
}
