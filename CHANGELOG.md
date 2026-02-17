# Changelog

## 1.1.0
## Added
- Session expiration tracking (`sessionExpires`) in authorization response
- Token expiration validation with refresh-threshold in XForgeTokenInterceptor
- Product resolution from JWT audience via ForgeProperties

## Changed
- Increased callback token TTL from 4 hours to 7 days
- Made JiraClient reactive with parallel resource preloading
- Refactored SecurityUtils into a Spring component, consolidated token handling

## Fixed
- Race conditions in JWT audience validation and demo settings creation
- Cross-tenant data leakage via shared in-memory settings map
- Memory leak from unreleased ClientResponse body in DownloadController
- Session expiration calculation and update logic

## 1.0.1
## Changed
- fixed bug link generation when the document server URL contains a context path

## 1.0.0
## Added
- Authorization endpoint for issuing JWT tokens (`/api/v1/remote/authorization`)
- Editor page for viewing and editing documents in Jira (`/editor/jira`)
- Callback endpoint for processing document save events (`/api/v1/callback/jira`)
- Download endpoint for streaming document content (`/api/v1/download/jira`)
- Create endpoint for creating new blank documents (`/api/v1/remote/create`)
- Formats endpoint for retrieving supported document formats (`/api/v1/remote/formats`)
- Settings endpoint for demo server management (`/api/v1/remote/settings`)
- Health check endpoint (`/api/v1/health`)
