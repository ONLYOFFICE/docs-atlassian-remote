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
public class ConfluenceFileId {
    private String parentId;
    private String parentContentType;
    private String attachmentId;

    public static ConfluenceFileId parse(final String value) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return new ConfluenceFileId(null, null, null);
        }

        String[] parts = value.split(":", 3);

        String parentContentType = parts.length > 0 ? parts[0] : null;
        String parentId = parts.length > 1 ? parts[1] : null;
        String attachmentId = parts.length > 2 ? parts[2] : null;

        return new ConfluenceFileId(parentId, parentContentType, attachmentId);
    }

    public static ConfluenceFileId parse(final String parent, final String attachment) {
        if (Objects.isNull(parent) || parent.isEmpty()) {
            return new ConfluenceFileId(null, null, attachment);
        }

        String[] parts = parent.split(":", 2);

        String parentContentType = parts.length > 0 ? parts[0] : null;
        String parentId = parts.length > 1 ? parts[1] : null;

        return new ConfluenceFileId(parentId, parentContentType, attachment);
    }

    @Override
    public String toString() {
        return parentContentType + ":" + parentId + ":" + attachmentId;
    }
}
