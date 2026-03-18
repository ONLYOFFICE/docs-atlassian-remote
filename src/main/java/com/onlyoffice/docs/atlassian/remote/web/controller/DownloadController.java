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

import com.onlyoffice.docs.atlassian.remote.api.BitbucketFileId;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceFileId;
import com.onlyoffice.docs.atlassian.remote.api.ConfluenceContext;
import com.onlyoffice.docs.atlassian.remote.api.Context;
import com.onlyoffice.docs.atlassian.remote.api.BitbucketContext;
import com.onlyoffice.docs.atlassian.remote.api.JiraContext;
import com.onlyoffice.docs.atlassian.remote.api.XForgeTokenType;
import com.onlyoffice.docs.atlassian.remote.client.confluence.ConfluenceClient;
import com.onlyoffice.docs.atlassian.remote.client.jira.JiraClient;
import com.onlyoffice.docs.atlassian.remote.security.SecurityUtils;
import com.onlyoffice.docs.atlassian.remote.security.XForgeTokenRepository;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/download")
public class DownloadController {
    private final SettingsManager settingsManager;
    private final JwtManager jwtManager;
    private final JiraClient jiraClient;
    private final ConfluenceClient confluenceClient;
    private final XForgeTokenRepository xForgeTokenRepository;
    private final SecurityUtils securityUtils;

    @Qualifier("bitbucketWebClient")
    private final WebClient bitbucketWebClient;

    @GetMapping({"jira", "confluence"})
    public ResponseEntity<Void> download(final @RequestHeader Map<String, String> headers) {
        if (settingsManager.isSecurityEnabled()) {
            String securityHeader = settingsManager.getSecurityHeader();
            String securityHeaderValue = Optional.ofNullable(headers.get(securityHeader))
                    .orElse(headers.get(securityHeader.toLowerCase()));
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (!Objects.isNull(securityHeaderValue) && securityHeaderValue.startsWith(authorizationPrefix))
                    ? securityHeaderValue.substring(authorizationPrefix.length()) : securityHeaderValue;

            if (Objects.isNull(token) || token.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Access denied: Not found authorization token"
                );
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied: " + e.getMessage());
            }
        }

        Context context = securityUtils.getCurrentAppContext();

        ClientResponse clientResponse = switch (context.getProduct()) {
            case JIRA -> {
                JiraContext jiraContext = (JiraContext) context;


                yield jiraClient.getAttachmentData(
                        jiraContext.getCloudId().toString(),
                        jiraContext.getAttachmentId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                );
            }
            case CONFLUENCE -> {
                ConfluenceContext confluenceContext = (ConfluenceContext) context;
                ConfluenceFileId confluenceFileId = ConfluenceFileId.parse(
                        confluenceContext.getParentId(),
                        confluenceContext.getAttachmentId()
                );

                yield confluenceClient.getAttachmentData(
                        confluenceContext.getCloudId().toString(),
                        confluenceFileId.getParentId(),
                        confluenceFileId.getAttachmentId(),
                        xForgeTokenRepository.getXForgeToken(
                                securityUtils.getCurrentXForgeUserTokenId(),
                                XForgeTokenType.USER
                        )
                );
            }
            default -> throw new UnsupportedOperationException("Unsupported product: " + context.getProduct());
        };

        try {
            HttpStatusCode status = clientResponse.statusCode();
            HttpHeaders httpHeaders = new HttpHeaders();
            clientResponse.headers().asHttpHeaders().forEach((httpHeader, values) -> {
                if (!httpHeader.equalsIgnoreCase("Transfer-Encoding")) {
                    httpHeaders.put(httpHeader, values);
                }
            });

            return ResponseEntity
                    .status(status)
                    .headers(httpHeaders)
                    .build();
        } finally {
            clientResponse.releaseBody().block();
        }
    }

    @GetMapping("bitbucket")
    public ResponseEntity<StreamingResponseBody> downloadBitbucket(
            final @RequestHeader Map<String, String> headers,
            final HttpServletResponse httpServletResponse
    ) {
        BitbucketContext bitbucketContext = (BitbucketContext) securityUtils.getCurrentAppContext();
        if (settingsManager.isSecurityEnabled()) {
            String securityHeader = settingsManager.getSecurityHeader();
            String securityHeaderValue = Optional.ofNullable(headers.get(securityHeader))
                    .orElse(headers.get(securityHeader.toLowerCase()));
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (!Objects.isNull(securityHeaderValue) && securityHeaderValue.startsWith(authorizationPrefix))
                    ? securityHeaderValue.substring(authorizationPrefix.length()) : securityHeaderValue;

            if (Objects.isNull(token) || token.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Access denied: Not found authorization token"
                );
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied: " + e.getMessage());
            }
        }

        BitbucketFileId bitbucketFileId = BitbucketFileId.parse(
                bitbucketContext.getRepositoryId(),
                bitbucketContext.getFileId(),
                bitbucketContext.getLocale()
        );

        Flux<DataBuffer> dataBufferFlux = bitbucketWebClient.get()
            .uri("/2.0/repositories/{workspaceId}/{repoId}/src/{commit}/{path}",
                    "{" + bitbucketContext.getCloudId().toString() + "}",
                    "{" + bitbucketFileId.getRepositoryId() + "}",
                    bitbucketFileId.getCommit(),
                    bitbucketFileId.getFilePath()
            )
            .headers(h -> h.setBearerAuth(
                        xForgeTokenRepository.getXForgeToken(
                        securityUtils.getCurrentXForgeUserTokenId(),
                        XForgeTokenType.USER
            )))
            .exchangeToFlux(clientResponse -> {
                clientResponse.headers().asHttpHeaders().forEach((httpHeader, values) -> {
                    if (!httpHeader.equalsIgnoreCase("Transfer-Encoding")) {
                        httpServletResponse.setHeader(httpHeader, values.getFirst());
                    }
                });

                httpServletResponse.setStatus(clientResponse.statusCode().value());

                return clientResponse.bodyToFlux(DataBuffer.class);
            });


        StreamingResponseBody streamingResponseBody = outputStream -> {
            WritableByteChannel channel = Channels.newChannel(outputStream);

            DataBufferUtils.write(dataBufferFlux, channel)
                    .doOnComplete(() -> {
                        try {
                            channel.close();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .blockLast();
        };

        return ResponseEntity
                .ok()
                .body(streamingResponseBody);
    }

}
