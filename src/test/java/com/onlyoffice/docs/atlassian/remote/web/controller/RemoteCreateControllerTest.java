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

import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import com.onlyoffice.docs.atlassian.remote.web.dto.create.CreateRequest;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RemoteCreateControllerTest extends AbstractControllerTest {
    private static final String REQUEST_MAPPING = "/api/v1/remote/create";

    @Test
    public void whenPostRemoteCreateWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenPostRemoteCreateWithInvalidRequestBody_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{invalid json}")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteCreateWithNullTitle_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        CreateRequest createRequest = new CreateRequest(
                "parentId",
                null,
                DocumentType.WORD,
                user.getLocale()
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteCreateWithEmptyParentId_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        CreateRequest createRequest = new CreateRequest(
                "",
                "title",
                DocumentType.WORD,
                user.getLocale()
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteCreateWithNullDocumentType_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        CreateRequest createRequest = new CreateRequest(
                "parentId",
                "title",
                null,
                user.getLocale()
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteCreateWithoutXForgeUserHeader_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteCreateWithoutRightsForCreate_returnForbidden() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        CreateRequest createRequest = new CreateRequest(
                "parentId",
                "title",
                DocumentType.WORD,
                user.getLocale()
        );

        when(jiraClient.createAttachment(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(
                new WebClientResponseException(
                        HttpStatus.FORBIDDEN.value(),
                        "",
                        null,
                        null,
                        null
                )
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenPostRemoteCreate_returnOk() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        CreateRequest createRequest = new CreateRequest(
                "parentId",
                "title",
                DocumentType.WORD,
                user.getLocale()
        );

        when(jiraClient.createAttachment(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(
                List.of(DataTest.Attachments.ATTACHMENT)
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isOk());
    }

    @Test
    public void whenPostRemoteCreateWithDifferentDocumentTypes_returnOk() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        DocumentType[] documentTypes = {DocumentType.WORD, DocumentType.CELL, DocumentType.SLIDE};

        for (DocumentType docType : documentTypes) {
            CreateRequest createRequest = new CreateRequest(
                    "parentId",
                    "title",
                    docType,
                    user.getLocale()
            );

            when(jiraClient.createAttachment(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(DataTest.Attachments.ATTACHMENT));

            mockMvc.perform(post(REQUEST_MAPPING)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt
                                            .claim("aud", JIRA_APP_ID)
                                            .claim("principal", user.getAccountId())
                                            .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                    )
                            )
                            .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                            .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(objectMapper.writeValueAsString(createRequest))
                    )
                    .andExpect(status().isOk());
        }
    }

}
