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

package com.onlyoffice.docs.atlassian.remote.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.web.dto.authorization.AuthorizationRequest;
import com.onlyoffice.docs.atlassian.remote.web.dto.authorization.AuthorizationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/remote/authorization")
@RequiredArgsConstructor
public class RemoteAuthorizationController {
    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${app.security.ttl.default}")
    private long ttlDefault;

    private final RemoteAppJwtService remoteAppJwtService;
    private final SecurityUtils securityUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<AuthorizationResponse> getAuthorization(
            final @Valid @RequestBody AuthorizationRequest request
    ) throws ParseException {
        Context context = securityUtils.getCurrentAppContext();

        Context remoteAppTokenContext = switch (context.getProduct()) {
            case JIRA -> JiraContext.builder()
                    .product(context.getProduct())
                    .cloudId(context.getCloudId())
                    .environmentId(context.getEnvironmentId())
                    .issueId(request.getParentId())
                    .attachmentId(request.getEntityId())
                    .build();
            case CONFLUENCE -> ConfluenceContext.builder()
                    .product(context.getProduct())
                    .cloudId(context.getCloudId())
                    .environmentId(context.getEnvironmentId())
                    .parentId(request.getParentId())
                    .attachmentId(request.getEntityId())
                    .build();
            default ->  throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };

        String token = remoteAppJwtService.encode(
                securityUtils.getCurrentAccountId(),
                "/editor/" + context.getProduct().toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        return ResponseEntity.ok(
                new AuthorizationResponse(baseUrl, token)
        );
    }
}
