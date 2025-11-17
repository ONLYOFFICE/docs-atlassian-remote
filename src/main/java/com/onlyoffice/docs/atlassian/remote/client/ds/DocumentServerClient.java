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

package com.onlyoffice.docs.atlassian.remote.client.ds;

import com.onlyoffice.manager.url.UrlManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;


@Component
@RequiredArgsConstructor
public class DocumentServerClient {
    private final WebClient documentSeverWebClient;
    private final UrlManager urlManager;

    public Flux<DataBuffer> getFile(final String url) {
        URI fileUri;
        try {
            fileUri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid file URL: " + url, e);
        }

        URI uri = UriComponentsBuilder.fromUriString(urlManager.getInnerDocumentServerUrl())
                .path(fileUri.getRawPath())
                .query(fileUri.getRawQuery())
                .build()
                .toUri();

        return documentSeverWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToFlux(DataBuffer.class);
    }
}
