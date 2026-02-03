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

package com.onlyoffice.docs.atlassian.remote.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.UUID;


@Data
@SuperBuilder
public class Context {
    private Product product;
    private UUID cloudId;
    private UUID environmentId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Context(
            final @NonNull @JsonProperty("product") Product product,
            final @NonNull @JsonProperty("cloudId") UUID cloudId,
            final @NonNull @JsonProperty("environmentId") UUID environmentId
    ) {
        this.product = product;
        this.cloudId = cloudId;
        this.environmentId = environmentId;
    }
}
