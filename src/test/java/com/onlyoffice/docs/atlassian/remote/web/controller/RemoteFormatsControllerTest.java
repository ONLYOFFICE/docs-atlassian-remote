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

import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RemoteFormatsControllerTest extends AbstractControllerTest {
    private static final String REQUEST_MAPPING = "/api/v1/remote/formats";

    @Test
    public void whenGetFormatsWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(get(REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetFormats_returnOk() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(get(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


}
