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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;


@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ProblemDetail> handleWebClientException(
            final WebClientResponseException exception,
            final HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getMessage()
        );

        problem.setTitle(exception.getStatusText());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("externalResponse", exception.getResponseBodyAsString());

        return ResponseEntity.status(exception.getStatusCode()).body(problem);
    }
}
