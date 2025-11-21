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

package com.onlyoffice.docs.atlassian.remote.web.controller;

import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/callback")
@RequiredArgsConstructor
public class CallbackController {
    private final SettingsManager settingsManager;
    private final CallbackService callbackService;

    @PostMapping({"jira", "confluence"})
    public ResponseEntity<Map<String, Object>> callback(
            final @RequestHeader Map<String, String> headers,
            final @RequestBody Callback callback
    ) throws Exception {
        String authorizationHeader = Optional.ofNullable(headers.get(settingsManager.getSecurityHeader()))
                        .orElse(headers.get(settingsManager.getSecurityHeader().toLowerCase()));

        Callback verifiedCallback;
        try {
            verifiedCallback = callbackService.verifyCallback(callback, authorizationHeader);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Access denied: " + e.getMessage()));
        }

        callbackService.processCallback(verifiedCallback, null);

        return ResponseEntity.ok(Map.of("error", 0));
    }
}
