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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;


@AllArgsConstructor
@Getter
public class BitbucketFileId {
    private String repositoryId;
    private String commit;
    private String filePath;
    private String locale;

    public static BitbucketFileId parse(final String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return new BitbucketFileId(null, null, null, null);
        }

        String[] parts = value.split(":", 4);

        String repositoryId = parts.length > 0 ? parts[0] : null;
        String commit = parts.length > 1 ? parts[1] : null;
        String filePath = parts.length > 2 ? parts[2] : null;
        String locale = parts.length > 3 ? parts[3] : null;

        return new BitbucketFileId(repositoryId, locale, commit, filePath);
    }

    public static BitbucketFileId parse(final String repositoryId, final String fileId, final String locale) {
        if (Objects.isNull(fileId) || fileId.isEmpty()) {
            return new BitbucketFileId(repositoryId, null, null, locale);
        }

        String[] parts = fileId.split(":", 2);

        String commit = parts.length > 0 ? parts[0] : null;
        String filePath = parts.length > 1 ? parts[1] : null;

        return new BitbucketFileId(repositoryId, commit, filePath, locale);
    }

    @Override
    public String toString() {
        return repositoryId + ":" + commit + ":" + filePath + ":" + locale;
    }
}
