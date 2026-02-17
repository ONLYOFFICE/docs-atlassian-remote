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

import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import com.onlyoffice.docs.atlassian.remote.web.dto.authorization.AuthorizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RemoteAuthorizationControllerTest extends AbstractControllerTest {
    private static final String REQUEST_MAPPING = "/api/v1/remote/authorization";

    @Test
    public void whenPostRemoteAuthorizationWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenPostRemoteAuthorizationWithoutXForgeSystemHeader_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "parentId",
                "entityId"
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
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithoutXForgeUserHeader_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "parentId",
                "entityId"
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithoutRequestBody_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithInvalidRequestBody_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{invalid json}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithNullParentId_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                null,
                "entityId"
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithEmptyParentId_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "",
                "entityId"
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationWithNullEntityId_returnBadRequest() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "parentId",
                null
        );

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    public void whenPostRemoteAuthorizationSuccessfully_returnOk() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "parentId",
                "entityId"
        );

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remoteAppUrl").value(APP_BASE_URL))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    public void whenPostRemoteAuthorizationSuccessfully_verifyTokensSaved() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        AuthorizationRequest authRequest = new AuthorizationRequest(
                "parentId",
                "entityId"
        );

        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.SYSTEM)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
        when(xForgeTokenRepository.getXForgeTokenExpiration(anyString(), eq(XForgeTokenType.USER)))
                .thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .header("x-forge-oauth-system", DataTest.testXForgeOAuthSystemToken)
                        .header("x-forge-oauth-user", DataTest.testXForgeOAuthUserToken)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(authRequest))
                )
                .andExpect(status().isOk());

        verify(xForgeTokenRepository, times(2)).saveXForgeToken(anyString(), anyString(), any());
        verify(xForgeTokenRepository).saveXForgeToken(
                anyString(),
                eq(DataTest.testXForgeOAuthSystemToken),
                eq(XForgeTokenType.SYSTEM)
        );
        verify(xForgeTokenRepository).saveXForgeToken(
                anyString(),
                eq(DataTest.testXForgeOAuthUserToken),
                eq(XForgeTokenType.USER)
        );
    }
}
