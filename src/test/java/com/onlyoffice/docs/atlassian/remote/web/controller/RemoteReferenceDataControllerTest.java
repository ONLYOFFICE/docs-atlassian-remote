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

import com.onlyoffice.docs.atlassian.remote.api.Product;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceResults;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import com.onlyoffice.docs.atlassian.remote.web.dto.referencedata.ReferenceDataRequest;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RemoteReferenceDataControllerTest extends AbstractControllerTest {
    private static final String REQUEST_MAPPING = "/api/v1/remote/reference-data";
    private static final String TEST_PARENT_ID = "page:parentPageId";

    @Test
    public void whenPostReferenceDataWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReferenceDataRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenPostReferenceDataWithoutParentId_returnBadRequest() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReferenceDataRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostReferenceDataWithoutBody_returnBadRequest() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostReferenceDataWithNullReferenceData_returnOk() throws Exception {
        ReferenceDataRequest request = new ReferenceDataRequest(null, "filename.docx");

        when(xForgeTokenRepository.getXForgeToken(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(DataTest.testXForgeOAuthUserToken);

        when(confluenceClient.getAttachmentsForContent(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(new ConfluenceResults<>(
                        List.of(DataTest.ConfluenceAttachments.ATTACHMENT_WITH_EDIT)
                )));
        when(confluenceClient.getSettings(any(), any()))
                .thenReturn(Mono.just(DataTest.ConfluenceSettings.CORRECT_SETTINGS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void whenPostReferenceDataWithMatchingInstanceId_returnOk() throws Exception {
        String currentInstanceId = Product.CONFLUENCE + ":" + DataTest.testCloudId;
        ReferenceData referenceData = ReferenceData.builder()
                .instanceId(currentInstanceId)
                .fileKey("att123")
                .build();
        ReferenceDataRequest request = new ReferenceDataRequest(referenceData, "filename.docx");

        when(xForgeTokenRepository.getXForgeToken(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(DataTest.testXForgeOAuthUserToken);

        when(confluenceClient.getAttachment(any(), any(), any()))
                .thenReturn(Mono.just(DataTest.ConfluenceAttachments.ATTACHMENT_WITH_EDIT));
        when(confluenceClient.getSettings(any(), any()))
                .thenReturn(Mono.just(DataTest.ConfluenceSettings.CORRECT_SETTINGS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void whenPostReferenceDataWithMatchingInstanceIdAndAttachmentNotFound_returnOk() throws Exception {
        String currentInstanceId = Product.CONFLUENCE + ":" + DataTest.testCloudId;
        ReferenceData referenceData = ReferenceData.builder()
                .instanceId(currentInstanceId)
                .fileKey("att123")
                .build();
        ReferenceDataRequest request = new ReferenceDataRequest(referenceData, "filename.docx");

        when(xForgeTokenRepository.getXForgeToken(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(DataTest.testXForgeOAuthUserToken);

        when(confluenceClient.getAttachment(any(), any(), any()))
                .thenThrow(new WebClientResponseException(
                        HttpStatus.NOT_FOUND.value(), "", null, null, null
                ));

        when(confluenceClient.getAttachmentsForContent(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(new ConfluenceResults<>(
                        List.of(DataTest.ConfluenceAttachments.ATTACHMENT_WITH_EDIT)
                )));
        when(confluenceClient.getSettings(any(), any()))
                .thenReturn(Mono.just(DataTest.ConfluenceSettings.CORRECT_SETTINGS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void whenPostReferenceDataWithNonMatchingInstanceId_returnOk() throws Exception {
        ReferenceData referenceData = ReferenceData.builder()
                .instanceId("other-instance-id")
                .fileKey("att123")
                .build();
        ReferenceDataRequest request = new ReferenceDataRequest(referenceData, "filename.docx");

        when(xForgeTokenRepository.getXForgeToken(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(DataTest.testXForgeOAuthUserToken);

        when(confluenceClient.getAttachmentsForContent(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(new ConfluenceResults<>(
                        List.of(DataTest.ConfluenceAttachments.ATTACHMENT_WITH_EDIT)
                )));
        when(confluenceClient.getSettings(any(), any()))
                .thenReturn(Mono.just(DataTest.ConfluenceSettings.CORRECT_SETTINGS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", CONFLUENCE_APP_ID)
                                        .claim("principal", DataTest.ConfluenceUsers.ADMIN.getAccountId())
                                        .claim("context", Map.of(
                                                "cloudId", DataTest.testCloudId,
                                                "environmentId", DataTest.testEnvironmentId
                                        ))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .param("parentId", TEST_PARENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
