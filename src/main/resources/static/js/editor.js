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
        let sessionTimer;

        const startEditor = (config) => {
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
                    editor.denyEditingRights("Editing of this file has been stopped as the current session has expired. Please reload the editor to continue.");
                }, delay);
            } else {
                if (editor) {
                    editor.denyEditingRights("Editing of this file has been stopped as the current session has expired. Please reload the editor to continue.");
                }
            }
        };

        const stopSession = () => {
            clearTimeout(sessionTimer);
        };

        events.on("REFRESH_SESSION", (data) => {
            const { sessionExpires } = data;

            stopSession();
            startSession(sessionExpires);
        });

        events.on("OPEN_EDITOR", (data) => {
            const { config, sessionExpires } = data;

            config.events = {
                onDocumentReady: onDocumentReady,
                onRequestClose: getOnRequestClose(config.editorConfig.customization.goback.url),
                onRequestOpen: onRequestOpen,
                onRequestUsers: onRequestUsers,
                onRequestReferenceData: onRequestReferenceData
            };

            startSession(sessionExpires);
            startEditor(config);
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

        const onDocumentReady = () => {
            events.emit("DOCUMENT_READY", {
                demo: settings.demo
            });
        }

        const getOnRequestClose = (url) => {
            return (event) => {
                events.emit("REQUEST_CLOSE", {
                    url
                });
            }
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

        if (config) {
            config.events = {
                onDocumentReady: onDocumentReady,
                onRequestClose: getOnRequestClose(config.editorConfig.customization.goback.url),
                onRequestOpen: onRequestOpen,
                onRequestUsers: onRequestUsers,
                onRequestReferenceData: onRequestReferenceData
            };

            startSession(sessionExpires);
            startEditor(config);
        }

    })(window.DocsAPI, window.config, window.events, window.sessionExpires, window.settings);
});