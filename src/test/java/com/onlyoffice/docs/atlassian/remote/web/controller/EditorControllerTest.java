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
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class EditorControllerTest extends AbstractControllerTest {
    private static final String JIRA_EDITOR_PATH = "/editor/jira";

    @Value("${app.security.ttl.default}")
    private long ttlDefault;

    @Autowired
    private RemoteAppJwtService remoteAppJwtService;

    @Test
    public void whenGetJiraEditorPageWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("mode", Mode.EDIT.name())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithInvalidToken_returnUnauthorized() throws Exception {
        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", "token")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithInvalidTokenAudience_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "invalid_audience",
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithExpiredToken_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                Instant.now().minus(1, ChronoUnit.MINUTES),
                1,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        Thread.sleep(1000);

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithEmptyTokenSubject_returnUnauthorized() throws Exception {
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                "",
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithEmptyTokenContext_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                Map.of()
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithTokenContextWithoutProduct_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        Map<String, Object> contextAsMap = objectMapper.convertValue(
                remoteAppTokenContext,
                new TypeReference<Map<String, Object>>() { }
        );

        contextAsMap.remove("product");

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                contextAsMap
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithTokenContextWithoutCloudId_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        Map<String, Object> contextAsMap = objectMapper.convertValue(
                remoteAppTokenContext,
                new TypeReference<Map<String, Object>>() { }
        );

        contextAsMap.remove("cloudId");

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                contextAsMap
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithTokenContextWithoutIssueId_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        Map<String, Object> contextAsMap = objectMapper.convertValue(
                remoteAppTokenContext,
                new TypeReference<Map<String, Object>>() { }
        );

        contextAsMap.remove("issueId");

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                contextAsMap
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithTokenContextWithoutEntityId_returnUnauthorized() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        Map<String, Object> contextAsMap = objectMapper.convertValue(
                remoteAppTokenContext,
                new TypeReference<Map<String, Object>>() { }
        );

        contextAsMap.remove("attachmentId");

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                contextAsMap
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetJiraEditorPageWithoutModeParameter_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenGetJiraEditorPageWithInvalidMode_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Product product = Product.JIRA;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                        .param("mode", "edit")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenGetJiraEditorPageInEditMode_returnEditorView() throws Exception {
        Product product = Product.JIRA;
        JiraUser user = DataTest.Users.ADMIN;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(2, ChronoUnit.HOURS));

        when(jiraClient.getAttachment(
                any(),
                any(),
                any()
        )).thenReturn(
                DataTest.Attachments.ATTACHMENT
        );

        when(jiraClient.getIssuePermissions(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                DataTest.Permissions.FULL
        );

        when(jiraClient.getUser(
                any(),
                any()
        )).thenReturn(
                DataTest.Users.ADMIN
        );

        when(jiraClient.getSettings(
                any(),
                any()
        )).thenReturn(
                DataTest.Settings.CORRECT_SETTINGS
        );

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                        .param("mode", Mode.EDIT.name())
                ).andExpect(status().isOk())
                .andExpect(view().name("editor"))
                .andExpect(model().attribute("documentServerApiUrl", containsString("https://test-docs-server.com")))
                .andExpect(model().attributeExists("sessionExpires"))
                .andExpect(model().attribute("settings", Map.of("demo", false)));
    }

    @Test
    public void whenGetJiraEditorPageInViewMode_returnEditorView() throws Exception {
        Product product = Product.JIRA;
        JiraUser user = DataTest.Users.ADMIN;

        Context remoteAppTokenContext = JiraContext.builder()
                .product(product)
                .cloudId(DataTest.testCloudId)
                .environmentId(DataTest.testEnvironmentId)
                .issueId("parentId")
                .attachmentId("entityId")
                .build();

        String token = remoteAppJwtService.encode(
                user.getAccountId(),
                "/editor/" + product.toString().toLowerCase(),
                ttlDefault,
                objectMapper.convertValue(remoteAppTokenContext, new TypeReference<Map<String, Object>>() { })
        ).getTokenValue();

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(2, ChronoUnit.HOURS));

        when(jiraClient.getAttachment(
                any(),
                any(),
                any()
        )).thenReturn(
                DataTest.Attachments.ATTACHMENT
        );

        when(jiraClient.getIssuePermissions(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                DataTest.Permissions.FULL
        );

        when(jiraClient.getUser(
                any(),
                any()
        )).thenReturn(
                DataTest.Users.ADMIN
        );

        when(jiraClient.getSettings(
                any(),
                any()
        )).thenReturn(
                DataTest.Settings.CORRECT_SETTINGS
        );

        mockMvc.perform(get(JIRA_EDITOR_PATH)
                        .param("token", token)
                        .param("mode", Mode.VIEW.name())
                ).andExpect(status().isOk())
                .andExpect(view().name("editor"))
                .andExpect(model().attribute("documentServerApiUrl", containsString("https://test-docs-server.com")))
                .andExpect(model().attributeExists("sessionExpires"))
                .andExpect(model().attribute("settings", Map.of("demo", false)));
    }
}
