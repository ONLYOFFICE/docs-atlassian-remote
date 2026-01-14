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

package com.onlyoffice.docs.atlassian.remote.web.data;

import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermission;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissions;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissionsKey;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraSettings;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Status;
import com.onlyoffice.model.settings.SettingsConstants;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class DataTest {
    private DataTest() {
    }

    public static UUID testCloudId = UUID.fromString("c0201358-a0eb-41ea-84a0-d15d3221dc23");
    public static String testXForgeOAuthUserToken = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiL"
        + "CJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6NDEyNDI0Nzg0NywiaWF0IjoxNzU3NDkyNjQ3fQ.bnQNpHlPdrMS3jdEjR-k-HMeg3J3ss"
        + "XUGu7RPZH8Ha8";
    public static String testXForgeOAuthSystemToken = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXI"
        + "iLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6NDEyNDI0Nzg0NywiaWF0IjoxNzU3NDkyNjQ3fQ.bnQNpHlPdrMS3jdEjR-k-HMeg3J3"
        + "ssXUGu7RPZH8Ha8";

    public static class Users {
        public static final JiraUser ADMIN = JiraUser.builder()
                .accountId("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
                .displayName("admin")
                .locale("en-US")
                .avatarUrls(Map.of())
                .build();
    }

    public static class Attachments {
        public static final JiraAttachment ATTACHMENT = JiraAttachment.builder()
                .id(Math.abs(new Random().nextLong()))
                .filename("filename.docx")
                .author(Users.ADMIN)
                .build();
    }

    public static class Permissions {
        public static final JiraPermissions FULL = JiraPermissions.builder()
                .permissions(
                        Map.of(
                                JiraPermissionsKey.CREATE_ATTACHMENTS,
                                new JiraPermission(JiraPermissionsKey.CREATE_ATTACHMENTS.name(), true),
                                JiraPermissionsKey.DELETE_ALL_ATTACHMENTS,
                                new JiraPermission(JiraPermissionsKey.DELETE_ALL_ATTACHMENTS.name(), true),
                                JiraPermissionsKey.DELETE_OWN_ATTACHMENTS,
                                new JiraPermission(JiraPermissionsKey.DELETE_OWN_ATTACHMENTS.name(), true)
                        )
                )
                .build();
    }

    public static class Settings {
        public static final JiraSettings CORRECT_SETTINGS = new JiraSettings(
               Map.of(
                       SettingsConstants.URL, "https://test-docs-server.com",
                       SettingsConstants.SECURITY_KEY, "secret"
               )
        );
    }

    public static class Callbacks {
        public static Callback getTestCallback() {
            Callback callback = new Callback();

            callback.setStatus(Status.EDITING);

            return callback;
        }
    }
}
