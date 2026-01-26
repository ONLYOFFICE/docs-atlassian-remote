# ONLYOFFICE Docs Atlassian Remote

Service for integrating ONLYOFFICE Document Editors into Atlassian cloud products.
This is an intermediate service between Forge Atlassian Applications, Atlassian API and ONLYOFFICE Document Editors.

## Table of Contents
- [About](#about)
- [Technologies](#technologies)
- [Requirements](#requirements)
- [Run](#run)
- [Docker Run](#docker-run)
- [Environment Variables](#environment-variables)

## About
### Key features
  - Ability to open Atlassian documents for viewing or editing.
  - Creating new documents in Atlassian products.
  - Storing information about connections to the demo server of ONLYOFFICE editors.

### Supported formats

**For viewing:**
* **WORD**: DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, HWP, HWPX, MD, MHT, MHTML, ODT, OTT, PAGES, RTF, STW, SXW, TXT, WPS, WPT, XML
* **CELL**: CSV, ET, ETT, FODS, NUMBERS, ODS, OTS, SXC, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE**: DPS, DPT, FODP, KEY, ODG, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SXI
* **PDF**: DJVU, DOCXF, OFORM, OXPS, PDF, XPS
* **DIAGRAM**: VSDM, VSDX, VSSM, VSSX, VSTM, VSTX

**For editing:**
* **WORD**: DOCM, DOCX, DOTM, DOTX
* **CELL**: XLSB, XLSM, XLSX, XLTM, XLTX
* **SLIDE**: POTM, POTX, PPSM, PPSX, PPTM, PPTX
* **PDF**: PDF

**For editing with possible loss of information:**
* **WORD**: EPUB, FB2, HTML, ODT, OTT, RTF, TXT
* **CELL**: CSV, ODS, OTS
* **SLIDE**: ODP, OTP

## Technologies
List the main technologies and libraries used:
- Java 21
- Spring Boot 3
- Spring Security
- Spring Web
- Spring Data JPA
- Spring Data Redis

## Requirements
- Java 21+
- Maven 3.8+
- PostgresSQL 16+
- Redis 7+

## Run
1. Make sure PostgreSQL and Redis are activated and available.
2. Set environment variables (see [Environment Variables](#environment-variables) section).
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Docker Run
1. Rename the `.env.example` file to `.env` and set the environment variables (see [Environment Variables](#environment-variables) section).
2. Run the following command:
   ```bash
   docker compose up -d
   ```
3. To stop the application, run:
   ```bash
   docker compose down
   ```

## Environment Variables
| Name                         | Description                             |                            Default Value                            | Required |
|------------------------------|-----------------------------------------|:-------------------------------------------------------------------:|:--------:|
| `APP_BASE_URL`               | Application Base URL                    |                                  -                                  |    ✅     |
| `APP_SECRET_KEY`             | Application Secret Key (JWT/Encryption) |                                  -                                  |    ✅     |
| `FORGE_JIRA_APP_ID`          | ID Forge applications for JIRA          |                                  -                                  |    ✅     |
| `SPRING_DATASOURCE_URL`      | JDBC URL to the database                | `jdbc:postgresql://localhost:5432/onlyoffice_docs_atlassian_remote` |    ❌     |
| `SPRING_DATASOURCE_USERNAME` | Database user                           |                            `onlyoffice`                             |    ❌     |
| `SPRING_DATASOURCE_PASSWORD` | Database password                       |                            `onlyoffice`                             |    ❌     |
| `SPRING_REDIS_HOST`          | Redis Host                              |                             `localhost`                             |    ❌     |
| `SPRING_REDIS_PORT`          | Redis port                              |                               `6379`                                |    ❌     |
| `SPRING_REDIS_USER`          | Redis user                              |                                  -                                  |    ❌     |
| `SPRING_REDIS_PASSWORD`      | Redis password                          |                                  -                                  |    ❌     |
| `SPRING_REDIS_DATABASE`      | Redis database number                   |                                 `0`                                 |    ❌     |
