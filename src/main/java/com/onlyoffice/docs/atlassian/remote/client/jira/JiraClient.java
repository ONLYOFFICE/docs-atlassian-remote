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

package com.onlyoffice.docs.atlassian.remote.client.jira;

import com.onlyoffice.docs.atlassian.remote.aop.RequestCacheable;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraAttachment;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissions;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraPermissionsKey;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraSettings;
import com.onlyoffice.docs.atlassian.remote.client.jira.dto.JiraUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class JiraClient {
    private final WebClient atlassianWebClient;

    @RequestCacheable
    public JiraUser getUser(final String cloudId, final String token) {
        return atlassianWebClient.get()
                .uri("/ex/jira/{cloudId}/rest/api/3/myself", cloudId)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<JiraUser>() { })
                .block();
    }

    @RequestCacheable
    public JiraAttachment getAttachment(final UUID cloudId, final String attachmentId, final String token) {
        return atlassianWebClient.get()
                .uri("/ex/jira/{cloudId}/rest/api/3/attachment/{attachmentId}", cloudId, attachmentId)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<JiraAttachment>() { })
                .block();
    }

    public ClientResponse getAttachmentData(final String cloudId, final String attachmentId, final String token) {
        return atlassianWebClient.get()
                .uri("/ex/jira/{cloudId}/rest/api/3/attachment/content/{attachmentId}",
                        cloudId, attachmentId)
                .headers(h -> h.setBearerAuth(token))
                .exchangeToMono(Mono::just)
                .block();
    }

    public List<JiraAttachment> createAttachment(final UUID cloudId, final String issueId, final Flux<DataBuffer> file,
                                                 final String fileName, final String token) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", file, DataBuffer.class)
                .filename(fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return atlassianWebClient.post()
                .uri("/ex/jira/{cloudId}/rest/api/3/issue/{issueKey}/attachments", cloudId, issueId)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set("X-Atlassian-Token", "no-check");
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<JiraAttachment>>() { })
                .block();
    }

    public void deleteAttachment(final UUID cloudId, final String attachmentId, final String token) {
        atlassianWebClient.delete()
                .uri("/ex/jira/{cloudId}/rest/api/3/attachment/{attachmentId}", cloudId, attachmentId)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set("X-Atlassian-Token", "no-check");
                })
                .exchangeToMono(Mono::just)
                .block();
    }

    @RequestCacheable
    public JiraPermissions getIssuePermissions(final UUID cloudId, final String issueId,
                                               final List<JiraPermissionsKey> permissions, final String token) {
        return atlassianWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ex/jira/{cloudId}/rest/api/3/mypermissions")
                        .queryParam("issueId", issueId)
                        .queryParam("permissions", permissions)
                        .build(cloudId)
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<JiraPermissions>() { })
                .block();
    }

    @RequestCacheable
    public JiraSettings getSettings(final String settingsKey, final String token) {
        return atlassianWebClient.post()
                .uri("/forge/storage/kvs/v1/secret/get")
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .bodyValue(Map.of("key", settingsKey))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<JiraSettings>() { })
                .block();
    }
}
