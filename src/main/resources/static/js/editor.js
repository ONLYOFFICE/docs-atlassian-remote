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

document.addEventListener("DOMContentLoaded", function() {
    (function(DocsAPI, config, events, sessionExpires, settings) {
        if (!DocsAPI) {
            events.emit("DOCS_API_UNDEFINED");
            return;
        } else {
            events.emit("PAGE_IS_LOADED", {
                config
            });
        }

        let editor;
        let connector;
        let sessionTimer;

        const startEditor = () => {
            editor = new DocsAPI.DocEditor("documentEditor", config);
        };

        const stopEditor = () => {
            editor.destroyEditor();
        };

        const startSession = (endTime) => {
            const targetTime = new Date(endTime);
            const now = new Date();

            const delay = targetTime.getTime() - now.getTime();

            if (delay > 0) {
                sessionTimer = setTimeout(() => {
                    editor.denyEditingRights("Editing of this document has been stopped because the current session has expired. Please reload the editor.");
                }, delay);
            } else {
                editor.denyEditingRights("Editing of this document has been stopped because the current session has expired. Please reload the editor.");
            }
        };

        const stopSession = () => {
            clearTimeout(sessionTimer);
        };

        const getOrCreateConnector = () => {
            if (!connector) {
                connector = editor.createConnector();
            };

            return connector;
        };

        events.on("REFRESH_SESSION", (data) => {
            const { sessionExpires } = data;

            stopSession();
            startSession(sessionExpires);
        });

        events.on("STOP_EDITING", (data) => {
            const {message} = data;

            editor.denyEditingRights(message);
        });

        events.on("SHOW_MESSAGE", (data) => {
            const {message} = data;

            editor.showMessage(message);
        });

        events.on("SET_USERS", (data) => {
            const {c, users} = data;

            editor.setUsers({
              "c": c,
              "users": users,
            });
        });

        events.on("SET_REFERENCE_DATA", (data) => {
            editor.setReferenceData(data);
        });

        events.on("INIT_AI_PROVIDER", (data) => {
            console.log("[ONLYOFFICE] INIT_AI_PROVIDER", data);
            const { providerName, settings } = data;

            const connector = getOrCreateConnector();

            const initAIProvider = (providerName, settings) => {
                connector.sendEvent("ai_onCustomProviders", [{ name: providerName }]);
                connector.sendEvent("ai_onCustomInit", settings);
            }

            connector.executeMethod("AI", [{ type: "Actions" }], (data) => {
                if (data && typeof data === "object" && "error" in data && data.error) {
                    connector.attachEvent("ai_onInit", () => {
                        initAIProvider(providerName, settings);
                    });
                } else {
                    initAIProvider(providerName, settings);
                }
            });

            connector.attachEvent("ai_onExternalFetch", (e) => {
                events.emit("AI_FETCH", e);
            });
        });

        events.on("AI_FETCH_RESPONSE", (data) => {
            console.log("[ONLYOFFICE] AI_FETCH_RESPONSE", data);
            connector.sendEvent("ai_onExternalFetch", data);
        });

        const onDocumentReady = () => {
            events.emit("DOCUMENT_READY", {
                demo: settings.demo
            });
        }

        const onRequestClose = (event) => {
            events.emit("REQUEST_CLOSE", {
                url: config.editorConfig.customization.goback.url
            });
        }

        const onRequestOpen = (event) => {
            events.emit("REQUEST_OPEN", event.data);
        }

        const onRequestUsers = function(event) {
            switch (event.data.c) {
                case "info":
                    events.emit("REQUEST_USERS", {
                        c: event.data.c,
                        ids: event.data.id
                    });
                    break;
            }
        };

        const onRequestReferenceData = function(event) {
            events.emit("REQUEST_REFERENCE_DATA", event.data);
        }

        config.events = {
            onDocumentReady: onDocumentReady,
            onRequestClose: onRequestClose,
            onRequestOpen: onRequestOpen,
            onRequestUsers: onRequestUsers,
            onRequestReferenceData: onRequestReferenceData
        };

        startSession(sessionExpires);
        startEditor();

    })(window.DocsAPI, window.config, window.events, window.sessionExpires, window.settings);
});