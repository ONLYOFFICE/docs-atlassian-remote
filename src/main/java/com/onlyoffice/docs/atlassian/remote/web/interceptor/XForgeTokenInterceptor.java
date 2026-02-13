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

package com.onlyoffice.docs.atlassian.remote.web.interceptor;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.configuration.ForgeProperties;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Slf4j
public class XForgeTokenInterceptor implements HandlerInterceptor {
    private final ForgeProperties forgeProperties;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final SecurityUtils securityUtils;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws ParseException, IOException {
        String xForgeOauthUserHeader = forgeProperties.getToken().getUser().getHeader();
        String xForgeOauthSystemHeader = forgeProperties.getToken().getSystem().getHeader();

        String xForgeUserToken = request.getHeader(xForgeOauthUserHeader);
        String xForgeSystemToken = request.getHeader(xForgeOauthSystemHeader);

        if (Objects.isNull(xForgeUserToken)) {
            log.warn("Required header '" + xForgeOauthUserHeader + "' is not present.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        if (Objects.isNull(xForgeSystemToken)) {
            log.warn("Required header '" + xForgeOauthSystemHeader + "' is not present.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        String userTokenError = validateXForgeToken(xForgeUserToken,
                forgeProperties.getToken().getUser().getRefreshThreshold());
        if (Objects.nonNull(userTokenError)) {
            String message = XForgeTokenType.USER + " token validation failed: " + userTokenError;
            log.warn(message);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
            return false;
        }

        String systemTokenError = validateXForgeToken(xForgeSystemToken,
                forgeProperties.getToken().getSystem().getRefreshThreshold());
        if (Objects.nonNull(systemTokenError)) {
            String message = XForgeTokenType.SYSTEM + " token validation failed: " + systemTokenError;
            log.warn(message);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
            return false;
        }

        Context context = securityUtils.getCurrentAppContext();
        xForgeTokenRepository.saveXForgeToken(
                securityUtils.createXForgeSystemTokenId(context.getProduct(), context.getCloudId()),
                xForgeSystemToken,
                XForgeTokenType.SYSTEM
        );

        xForgeTokenRepository.saveXForgeToken(
                securityUtils.createXForgeUserTokenId(
                        context.getProduct(),
                        context.getCloudId(),
                        securityUtils.getCurrentAccountId()
                ),
                xForgeUserToken,
                XForgeTokenType.USER
        );

        return true;
    }

    private String validateXForgeToken(final String token,
                                        final Duration refreshThreshold) throws ParseException {
        JWT jwt = JWTParser.parse(token);
        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();

        if (Objects.isNull(expirationTime)) {
            return "Token does not contain an expiration time claim";
        }

        Instant expiration = expirationTime.toInstant();
        Instant now = Instant.now();

        if (now.isAfter(expiration)) {
            return "Token has expired at " + expiration;
        }

        if (now.plus(refreshThreshold).isAfter(expiration)) {
            return "Token expires at " + expiration
                    + ", which is within the refresh threshold of " + refreshThreshold;
        }

        return null;
    }
}
