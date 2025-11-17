/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/download")
public class DownloadController {
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final JiraClient jiraClient;
    private final XForgeTokenRepository xForgeTokenRepository;

    @GetMapping("jira")
    public ResponseEntity<Void> downloadJira(final @RequestHeader Map<String, String> headers) {
        if (settingsManager.isSecurityEnabled()) {
            String securityHeader = settingsManager.getSecurityHeader();
            String securityHeaderValue = Optional.ofNullable(headers.get(securityHeader))
                    .orElse(headers.get(securityHeader.toLowerCase()));
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (!Objects.isNull(securityHeaderValue) && securityHeaderValue.startsWith(authorizationPrefix))
                    ? securityHeaderValue.substring(authorizationPrefix.length()) : securityHeaderValue;

            if (Objects.isNull(token) || token.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Access denied: Not found authorization token"
                );
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied: " + e.getMessage());
            }
        }

        JiraContext jiraContext = (JiraContext) SecurityUtils.getCurrentAppContext();

        ClientResponse clientResponse = jiraClient.getAttachmentData(
                jiraContext.getCloudId().toString(),
                jiraContext.getAttachmentId(),
                xForgeTokenRepository.getXForgeToken(
                        SecurityUtils.getCurrentXForgeUserTokenId(),
                        XForgeTokenType.USER
                )
        );

        HttpHeaders httpHeaders = new HttpHeaders();
        clientResponse.headers().asHttpHeaders().forEach((httpHeader, values) -> {
            if (!httpHeader.equalsIgnoreCase("Transfer-Encoding")) {
                httpHeaders.put(httpHeader, values);
            }
        });

        return ResponseEntity
                .status(clientResponse.statusCode())
                .headers(httpHeaders)
                .build();
    }
}
