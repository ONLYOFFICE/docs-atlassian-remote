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

import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.security.RemoteAppJwtService;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import com.onlyoffice.manager.security.JwtManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DownloadControllerTest extends AbstractControllerTest {
    private static final String JIRA_DOWNLOAD_PATH = "/api/v1/download/jira";

    @Value("${app.security.ttl.default}")
    private long ttlDefault;
    @Value("${app.security.secret}")
    private String secret;

    @Autowired
    private RemoteAppJwtService remoteAppJwtService;

    @Autowired
    private JwtManager jwtManager;

    @Test
    public void whenPostJiraDownloadWithInvalidTokenFromEditor_returnUnauthorized() throws Exception {
        Product product = Product.JIRA;
        JiraUser user = DataTest.Users.ADMIN;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                JIRA_DOWNLOAD_PATH,
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(2, ChronoUnit.HOURS));

        when(jiraClient.getSettings(
                any(),
                any()
        )).thenReturn(
                DataTest.Settings.CORRECT_SETTINGS
        );

        mockMvc.perform(get(JIRA_DOWNLOAD_PATH)
                .param("token", token)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    public void whenPostJiraDownloadWithTokenInHeaderFromEditor_returnOk() throws Exception {
        Product product = Product.JIRA;
        JiraUser user = DataTest.Users.ADMIN;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                JIRA_DOWNLOAD_PATH,
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(2, ChronoUnit.HOURS));

        when(jiraClient.getSettings(
                any(),
                any()
        )).thenReturn(
                DataTest.Settings.CORRECT_SETTINGS
        );

        when(jiraClient.getAttachmentData(
                any(),
                any(),
                any()
        )).thenReturn(
                ClientResponse
                        .create(HttpStatus.FOUND)
                        .build()
        );

        Map<String, Object> payload = Map.of("payload", "");
        String tokenFromEditor = jwtManager.createToken(
                objectMapper.convertValue(payload, new TypeReference<Map<String, ?>>() { }),
                "secret"
        );

        mockMvc.perform(get(JIRA_DOWNLOAD_PATH)
                .header("Authorization", "Bearer " + tokenFromEditor)
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isFound());
    }
}
