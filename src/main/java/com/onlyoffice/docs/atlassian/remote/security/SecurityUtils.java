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
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.FitContext;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.configuration.ForgeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final ForgeProperties forgeProperties;
    private final XForgeTokenRepository xForgeTokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Authentication getCurrentAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (Objects.isNull(authentication)) {
            throw new IllegalStateException("No authentication found in SecurityContext");
        }

        return authentication;
    }

    public Jwt getCurrentPrincipal() {
        Authentication authentication = getCurrentAuthentication();

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException("Authentication principal is not a Jwt");
        }

        return jwt;
    }

    public String getCurrentAccountId() {
        return getCurrentAuthentication().getName();
    }

    public Context getCurrentAppContext() {
        Jwt jwt = getCurrentPrincipal();

        Map<String, Object> contextAsMap = jwt.getClaimAsMap("context");

        if (Objects.isNull(contextAsMap)) {
            throw new IllegalStateException("JWT context claim is missing or invalid");
        }

        Product product = extractProduct(jwt).orElse(null);
        if (Objects.nonNull(product)) {
            FitContext fitContext = objectMapper.convertValue(contextAsMap, FitContext.class);

            return Context.builder()
                    .product(product)
                    .cloudId(fitContext.cloudId())
                    .environmentId(fitContext.environmentId())
                    .build();
        }

        if (!contextAsMap.containsKey("product")) {
            throw new IllegalStateException("JWT context claim is missing or invalid");
        }

        String productFromContext = (String) contextAsMap.get("product");

        switch (Product.valueOf(productFromContext)) {
            case JIRA:
                return (Context) objectMapper.convertValue(contextAsMap, JiraContext.class);
            case CONFLUENCE:
                return (Context) objectMapper.convertValue(contextAsMap, ConfluenceContext.class);
            default:
                throw new UnsupportedOperationException("Unsupported product: " + productFromContext);
        }
    }

    public String getCurrentXForgeSystemTokenId() {
        Context context = getCurrentAppContext();

        return createXForgeSystemTokenId(context.getProduct(), context.getCloudId());
    }

    public String getCurrentXForgeUserTokenId() {
        String accountId = getCurrentAccountId();
        Context context = getCurrentAppContext();

        return createXForgeUserTokenId(context.getProduct(), context.getCloudId(), accountId);
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

    public long getSessionExpires() throws ParseException {
        Instant xForgeSystemTokenExpiration = xForgeTokenRepository.getXForgeTokenExpiration(
                getCurrentXForgeSystemTokenId(),
                XForgeTokenType.SYSTEM
        ).minus(forgeProperties.getToken().getSystem().getRefreshThreshold());

        Instant xForgeUserTokenExpiration = xForgeTokenRepository.getXForgeTokenExpiration(
                getCurrentXForgeUserTokenId(),
                XForgeTokenType.USER
        ).minus(forgeProperties.getToken().getUser().getRefreshThreshold());

        Instant minInstant = xForgeSystemTokenExpiration.compareTo(xForgeUserTokenExpiration) <= 0
                ? xForgeSystemTokenExpiration : xForgeUserTokenExpiration;

        return minInstant.toEpochMilli();
    }

    public Optional<Product> extractProduct(final Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (Objects.isNull(audience) || audience.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(forgeProperties.getProductByAppId(audience.get(0)));
    }
}
