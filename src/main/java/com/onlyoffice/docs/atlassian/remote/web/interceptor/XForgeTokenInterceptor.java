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

import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
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
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Slf4j
public class XForgeTokenInterceptor implements HandlerInterceptor {
    private static final String X_FORGE_OAUTH_USER_HEADER = "x-forge-oauth-user";
    private static final String X_FORGE_OAUTH_SYSTEM_HEADER = "x-forge-oauth-system";

    private final XForgeTokenRepository xForgeTokenRepository;
    private final SecurityUtils securityUtils;

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws ParseException, IOException {
        String xForgeUserToken = request.getHeader(X_FORGE_OAUTH_USER_HEADER);
        String xForgeSystemToken = request.getHeader(X_FORGE_OAUTH_SYSTEM_HEADER);

        if (Objects.isNull(xForgeUserToken)) {
            log.warn("Required header '" + X_FORGE_OAUTH_USER_HEADER + "' is not present.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        if (Objects.isNull(xForgeSystemToken)) {
            log.warn("Required header '" + X_FORGE_OAUTH_SYSTEM_HEADER + "' is not present.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
}
