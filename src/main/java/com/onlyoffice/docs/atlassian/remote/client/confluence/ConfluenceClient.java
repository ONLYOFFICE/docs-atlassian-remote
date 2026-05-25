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

package com.onlyoffice.docs.atlassian.remote.client.confluence;

import com.onlyoffice.docs.atlassian.remote.aop.RequestCacheable;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceAttachment;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceContent;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceResults;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceSettings;
import com.onlyoffice.docs.atlassian.remote.client.confluence.dto.ConfluenceUser;
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
public class ConfluenceClient {
    private final WebClient atlassianWebClient;

    @RequestCacheable
    public Mono<ConfluenceUser> getUser(final UUID cloudId, final String token) {
        return atlassianWebClient.get()
                .uri("/ex/confluence/{cloudId}/wiki/rest/api/user/current", cloudId)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceUser>() { })
                .cache();
    }

    @RequestCacheable
    public Mono<ConfluenceContent> getContent(final UUID cloudId, final String contentType, final String id,
                                        final String token) {
        return atlassianWebClient.get()
                .uri(
                        "/ex/confluence/{cloudId}/wiki/api/v2/{contentType}s/{id}",
                        cloudId,
                        contentType,
                        id
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceContent>() { })
                .cache();
    }

    @RequestCacheable
    public Mono<ConfluenceResults<ConfluenceAttachment>> getAttachmentsForContent(final UUID cloudId,
                                                                                  final String contentType,
                                                                                  final String id,
                                                                                  final String fileName,
                                                                                  final String token) {
        return atlassianWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ex/confluence/{cloudId}/wiki/api/v2/{contentType}s/{id}/attachments")
                        .queryParam("filename", fileName)
                        .build(cloudId, contentType, id)
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceResults<ConfluenceAttachment>>() { })
                .cache();
    }

    @RequestCacheable
    public Mono<ConfluenceAttachment> getAttachment(final UUID cloudId, final String attachmentId, final String token) {
        return atlassianWebClient.get()
                .uri(
                        "/ex/confluence/{cloudId}/wiki/api/v2/attachments/{attachmentId}?include-operations=true",
                        cloudId,
                        attachmentId
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceAttachment>() { })
                .cache();
    }

    public ClientResponse getAttachmentData(final String cloudId, final String parentId, final String attachmentId,
                                            final String token) {
        return atlassianWebClient.get()
                .uri("/ex/confluence/{cloudId}/wiki/rest/api/content/{parentId}"
                                + "/child/attachment/{attachmentId}/download",
                        cloudId,
                        parentId,
                        attachmentId
                )
                .headers(h -> h.setBearerAuth(token))
                .exchangeToMono(Mono::just)
                .block();
    }

    public List<ConfluenceAttachment> createAttachment(final UUID cloudId, final String parentId,
                                                       final Flux<DataBuffer> file, final String fileName,
                                                       final String token) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", file, DataBuffer.class)
                .filename(fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return atlassianWebClient.post()
                .uri(
                        "/ex/confluence/{cloudId}/wiki/rest/api/content/{parentId}"
                                + "/child/attachment",
                        cloudId,
                        parentId
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set("X-Atlassian-Token", "no-check");
                })
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceResults<ConfluenceAttachment>>() { })
                .block()
                .getResults();
    }

    public ConfluenceAttachment updateAttachmentData(final UUID cloudId, final String parentId,
                                                     final String attachmentId, final Flux<DataBuffer> file,
                                                     final String token) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", file, DataBuffer.class)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        return atlassianWebClient.post()
                .uri(
                        "/ex/confluence/{cloudId}/wiki/rest/api/content/{parentId}"
                                + "/child/attachment/{attachmentId}/data",
                        cloudId,
                        parentId,
                        attachmentId
                )
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                    httpHeaders.set("X-Atlassian-Token", "no-check");
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceAttachment>() { })
                .block();
    }

    @RequestCacheable
    public Mono<ConfluenceSettings> getSettings(final String settingsKey, final String token) {
        return atlassianWebClient.post()
                .uri("/forge/storage/kvs/v1/secret/get")
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(token);
                })
                .bodyValue(Map.of("key", settingsKey))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ConfluenceSettings>() { })
                .cache();
    }
}
