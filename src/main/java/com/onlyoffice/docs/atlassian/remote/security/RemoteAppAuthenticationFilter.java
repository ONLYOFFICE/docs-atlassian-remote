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

package com.onlyoffice.docs.atlassian.remote.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.log.LogMessage;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;


@RequiredArgsConstructor
public class RemoteAppAuthenticationFilter extends OncePerRequestFilter {
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository =
            new RequestAttributeSecurityContextRepository();
    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource =
            new WebAuthenticationDetailsSource();
    private final AuthenticationEntryPoint authenticationEntryPoint =
            new BearerTokenAuthenticationEntryPoint();
    private final AuthenticationFailureHandler authenticationFailureHandler =
            new AuthenticationEntryPointFailureHandler(
                    (request, response, exception)
                            -> this.authenticationEntryPoint.commence(request, response, exception)
            );

    private final RemoteAppJwtService remoteAppJwtService;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        String token = request.getParameter("token");
        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());

        if (Objects.isNull(token) || token.isEmpty()) {
            filterChain.doFilter(request, response);
        } else {
            try {
                Jwt jwt = this.getJwt(token, path);

                JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(
                        jwt,
                        Collections.emptyList(),
                        jwt.getSubject()
                );

                jwtAuthenticationToken.setDetails(this.authenticationDetailsSource.buildDetails(request));

                successfulAuthentication(request, response, filterChain, jwtAuthenticationToken);
            } catch (AuthenticationException failed) {
                this.securityContextHolderStrategy.clearContext();
                this.logger.trace("Failed to process authentication request", failed);
                this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
            }
        }
    }

    private void successfulAuthentication(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain,
            final Authentication authenticationResult
    ) throws IOException, ServletException {
        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authenticationResult);
        this.securityContextHolderStrategy.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authenticationResult));
        }

        filterChain.doFilter(request, response);
    }

    private Jwt getJwt(final String token, final String audience) {
        try {
            return remoteAppJwtService.decode(token, audience);
        } catch (BadJwtException failed) {
            this.logger.debug("Failed to authenticate since the JWT was invalid");
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }
    }
}
