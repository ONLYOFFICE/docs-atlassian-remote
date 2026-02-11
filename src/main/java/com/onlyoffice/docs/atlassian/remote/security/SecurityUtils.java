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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class SecurityUtils {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Jwt getCurrentPrincipal() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (Objects.isNull(authentication)) {
            throw new IllegalStateException("No authentication found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException("Authentication principal is not a Jwt");
        }

        return jwt;
    }

    public Context getCurrentAppContext() {
        Jwt jwt = getCurrentPrincipal();

        Map<String, Object> contextAsMap = jwt.getClaimAsMap("context");
        if (contextAsMap == null || !contextAsMap.containsKey("product")) {
            throw new IllegalStateException("JWT context claim is missing or invalid");
        }

        String product = (String) contextAsMap.get("product");

        switch (Product.valueOf(product)) {
            case JIRA:
                return (Context) objectMapper.convertValue(contextAsMap, JiraContext.class);
            default:
                throw new UnsupportedOperationException("Unsupported product: " + product);
        }
    }

    public String getCurrentXForgeSystemTokenId() {
        Context context = getCurrentAppContext();

        return createXForgeSystemTokenId(context.getProduct(), context.getCloudId());
    }

    public String getCurrentXForgeUserTokenId() {
        Jwt principal = getCurrentPrincipal();
        Context context = getCurrentAppContext();

        return createXForgeUserTokenId(context.getProduct(), context.getCloudId(), principal.getSubject());
    }

    public String createXForgeSystemTokenId(final Product product, final UUID cloudId) {
        return String.format(
                "%s:%s",
                product,
                cloudId
        );
    }

    public String createXForgeUserTokenId(final Product product, final UUID cloudId, final String accountId) {
        return String.format(
                "%s:%s:%s",
                product,
                cloudId,
                accountId
        );
    }

}
