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
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnection;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnectionId;
import com.onlyoffice.docs.atlassian.remote.repository.DemoServerConnectionRepository;
import com.onlyoffice.docs.atlassian.remote.web.data.DataTest;
import com.onlyoffice.utils.ConfigurationUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RemoteSettingsControllerTest extends AbstractControllerTest {
    private static final String REQUEST_MAPPING = "/api/v1/remote/settings";

    @Autowired
    private DemoServerConnectionRepository demoServerConnectionRepository;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Test
    public void whenGetSettingsWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(get(REQUEST_MAPPING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenGetSettingsWithExistingDemoConnection_returnSettingsWithTrialData() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;
        Date startDate = new Date();
        String startDateString = dateFormat.format(startDate);
        int trialPeriod = 30;

        DemoServerConnectionId connectionId = DemoServerConnectionId.builder()
                .cloudId(DataTest.testCloudId)
                .product(Product.JIRA)
                .build();

        DemoServerConnection connection = DemoServerConnection.builder()
                .id(connectionId)
                .startDate(startDateString)
                .build();

        demoServerConnectionRepository.save(connection);

        try (MockedStatic<ConfigurationUtils> mockedUtils = mockStatic(ConfigurationUtils.class)) {
            mockedUtils.when(ConfigurationUtils::getDemoTrialPeriod).thenReturn(trialPeriod);

            mockMvc.perform(get(REQUEST_MAPPING)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt
                                            .claim("aud", JIRA_APP_ID)
                                            .claim("principal", user.getAccountId())
                                            .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                    )
                            )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.demoAvailable").value(true))
                    .andExpect(jsonPath("$.demoStart").exists())
                    .andExpect(jsonPath("$.demoEnd").exists());
        }
    }

    @Test
    public void whenGetSettingsWithoutExistingDemoConnection_returnDefaultSettings() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        mockMvc.perform(get(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoAvailable").value(true))
                .andExpect(jsonPath("$.demoStart").doesNotExist())
                .andExpect(jsonPath("$.demoEnd").doesNotExist());
    }

    @Test
    public void whenGetSettingsWithExpiredTrial_returnExpiredSettings() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        Calendar pastDate = Calendar.getInstance();
        pastDate.add(Calendar.DATE, -40);
        Date startDate = pastDate.getTime();
        String startDateString = dateFormat.format(startDate);
        int trialPeriod = 30;

        DemoServerConnectionId connectionId = DemoServerConnectionId.builder()
                .cloudId(DataTest.testCloudId)
                .product(Product.JIRA)
                .build();

        DemoServerConnection connection = DemoServerConnection.builder()
                .id(connectionId)
                .startDate(startDateString)
                .build();

        demoServerConnectionRepository.save(connection);

        try (MockedStatic<ConfigurationUtils> mockedUtils = mockStatic(ConfigurationUtils.class)) {
            mockedUtils.when(ConfigurationUtils::getDemoTrialPeriod).thenReturn(trialPeriod);

            mockMvc.perform(get(REQUEST_MAPPING)
                            .with(SecurityMockMvcRequestPostProcessors.jwt()
                                    .jwt(jwt -> jwt
                                            .claim("aud", JIRA_APP_ID)
                                            .claim("principal", user.getAccountId())
                                            .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                    )
                            )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.demoAvailable").value(false))
                    .andExpect(jsonPath("$.demoStart").exists())
                    .andExpect(jsonPath("$.demoEnd").exists());
        }
    }

    @Test
    public void whenPostSettingsWithoutAuthorization_returnUnauthorized() throws Exception {
        mockMvc.perform(post(REQUEST_MAPPING))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenPostSettingsWithoutExistingDemoConnection_createNewTrialAndReturnData() throws Exception {
        JiraUser user = DataTest.Users.ADMIN;

        DemoServerConnectionId connectionId = DemoServerConnectionId.builder()
                .cloudId(DataTest.testCloudId)
                .product(Product.JIRA)
                .build();

        demoServerConnectionRepository.deleteById(connectionId);

        mockMvc.perform(post(REQUEST_MAPPING)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt
                                        .claim("aud", JIRA_APP_ID)
                                        .claim("principal", user.getAccountId())
                                        .claim("context", Map.of("cloudId", DataTest.testCloudId))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoAvailable").value(true))
                .andExpect(jsonPath("$.demoStart").exists())
                .andExpect(jsonPath("$.demoEnd").exists());
    }
}
