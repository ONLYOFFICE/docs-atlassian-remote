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

package com.onlyoffice.docs.atlassian.remote.service;

import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnection;
import com.onlyoffice.docs.atlassian.remote.entity.DemoServerConnectionId;
import com.onlyoffice.docs.atlassian.remote.repository.DemoServerConnectionRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class DemoServerConnectionService {
    private final DemoServerConnectionRepository demoServerConnectionRepository;

    public DemoServerConnection findById(final DemoServerConnectionId id) {
        return demoServerConnectionRepository.findById(id).orElse(null);
    }

    @Transactional
    public DemoServerConnection create(final DemoServerConnectionId id, final String startDate) {
        if (demoServerConnectionRepository.existsById(id)) {
            throw new EntityExistsException();
        }

        return demoServerConnectionRepository.saveAndFlush(
            DemoServerConnection.builder()
                    .id(id)
                    .startDate(startDate)
                    .build()
        );
    }
}
