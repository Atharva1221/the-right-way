# The Right Way deployment guide

This application is a React/Vite frontend and a monolithic Spring Boot 4.1.1
backend using Java 17, MySQL, and Flyway. This guide describes preparation for
deployment; no hosting resources have been created and no production deployment
has been performed.

## Local development on Windows

Prerequisites:

- Java 17.
- MySQL 8.0, with the existing `exam_platform` database available.
- Node.js and npm.
- Google OAuth credentials only if testing Google sign-in.

The backend does not automatically load `.env` files. Supply server environment
variables through the current PowerShell process or the IDE run configuration.
Keep local credentials out of source control.

```powershell
# Start MySQL using the normal Windows service or MySQL Workbench.

$env:DB_URL = "jdbc:mysql://localhost:3306/exam_platform"
$env:DB_USERNAME = "<local database user>"
$env:DB_PASSWORD = "<local database password>"
$env:JWT_SECRET = "<base64-encoded random key of at least 32 bytes>"
$env:GOOGLE_CLIENT_ID = "<Google OAuth client ID>"
$env:GOOGLE_CLIENT_SECRET = "<Google OAuth client secret>"
$env:FRONTEND_URL = "http://localhost:5173"
$env:REFRESH_COOKIE_SECURE = "false"

Set-Location "F:\Projects\Exam Platform\Backend"
.\mvnw.cmd spring-boot:run
```

In another PowerShell window:

```powershell
Set-Location "F:\Projects\Exam Platform\Frontend"
npm ci
npm test
npm run build
npm run dev
```

Vite proxies `/api`, `/oauth2`, and `/login/oauth2` to
`VITE_BACKEND_PROXY_TARGET`, which defaults to `http://localhost:8080`. The
proxy makes local browser calls same-origin; no local CORS allowlist is needed
for this setup. Copy `.env.example` to `.env` only when overriding the Vite
proxy target. Do not put server secrets in any `VITE_*` variable.

Verify backend/database availability with:

```powershell
curl.exe -i http://localhost:8080/health
```

The expected successful body is `{"status":"UP","database":"UP"}`. The endpoint
does not return connection details. To run backend checks:

```powershell
Set-Location "F:\Projects\Exam Platform\Backend"
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

Flyway runs at startup. Hibernate remains in `validate` mode; do not use
`ddl-auto=update` or reset the database.

## Environment-variable reference

Set `SPRING_PROFILES_ACTIVE=prod` on the hosted backend. The production profile
requires `DB_URL` and `FRONTEND_URL`; it defaults the refresh cookie to Secure.
`JWT_SECRET`, database credentials, and Google OAuth credentials are already
required by the normal Spring configuration.

| Variable | Service | Secret? | Required | Example placeholder | Purpose |
|---|---|---:|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | No | Production | `prod` | Loads production-only URL and cookie settings. |
| `DB_URL` | Backend | No | Production | `jdbc:mysql://db.example.net:3306/exam_platform?sslMode=VERIFY_IDENTITY` | JDBC URL for managed MySQL. Use the provider's TLS parameters and CA/identity requirements. |
| `DB_USERNAME` | Backend | Yes/sensitive | Yes | `<database-user>` | MySQL account name. |
| `DB_PASSWORD` | Backend | Yes | Yes | `<database-password>` | MySQL password. |
| `JWT_SECRET` | Backend | Yes | Yes | `<base64-random-key-at-least-32-bytes>` | Base64-encoded HMAC signing key. Keep it stable across backend instances and rotate only with a planned session invalidation. |
| `FRONTEND_URL` | Backend | No | Production | `https://app.example.com` | Exact HTTPS frontend origin used for Google OAuth redirects; production rejects non-HTTPS URLs and URL paths. |
| `REFRESH_COOKIE_SECURE` | Backend | No | Production | `true` | Enables Secure on the refresh cookie. Production profile defaults this to true; do not override to false on HTTPS. |
| `CORS_ALLOWED_ORIGINS` | Backend | No | Only for cross-origin frontend | `https://app.example.com` | Comma-separated exact HTTP(S) origins. Production requires HTTPS. Wildcards, paths, and credentials-bearing origins are rejected. Empty means no cross-origin access. |
| `GOOGLE_CLIENT_ID` | Backend | Not a password; keep server-configured | Yes for current app config | `<client-id>.apps.googleusercontent.com` | Google OIDC client identifier. |
| `GOOGLE_CLIENT_SECRET` | Backend | Yes | Yes for current app config | `<Google-client-secret>` | Server-side Google OIDC secret. |
| `PORT` | Backend | No | Optional | `8080` | Host-provided listening port; defaults to 8080. |
| `VITE_API_BASE_URL` | Frontend build | Public | Production | `https://api.example.com` | HTTPS backend API origin compiled into the browser bundle. It must be set for production builds; HTTP URLs and paths are rejected. |
| `VITE_BACKEND_PROXY_TARGET` | Frontend dev server | No | Optional/local | `http://localhost:8080` | Local Vite development proxy target only. |

The frontend API URL and CORS origin are not secrets. Never set
`DB_PASSWORD`, `JWT_SECRET`, or `GOOGLE_CLIENT_SECRET` as `VITE_*` variables.

## Recommended deployment topology

Prefer custom domains on the same registrable domain:

- Frontend: `https://app.example.com` (Vercel custom domain).
- Backend: `https://api.example.com` (Java host custom domain).
- `VITE_API_BASE_URL=https://api.example.com`.
- `FRONTEND_URL=https://app.example.com`.
- `CORS_ALLOWED_ORIGINS=https://app.example.com`.

These are cross-origin, so the exact CORS allowlist is needed, but sibling
subdomains are same-site. The refresh cookie remains `HttpOnly`, `Secure`,
`SameSite=Lax`, and scoped to `/api/auth`; it intentionally has no `Domain`
attribute. The CSRF endpoint returns its token in a JSON body, while the
HttpOnly `XSRF-TOKEN` cookie is sent automatically to the backend. The frontend
returns the token in `X-XSRF-TOKEN` for unsafe requests; it does not need to
read a cookie belonging to the API subdomain.

Do not assume `*.vercel.app` and an unrelated backend-provider domain can share
cookies: browsers may block the refresh cookie as third-party. The application
does not currently configure `SameSite=None`; do not work around this by
weakening cookie settings. Use custom sibling domains or a same-origin reverse
proxy. If the eventual host requires unrelated sites, design and verify a
separate secure cookie/CSRF strategy before deployment.

Vercel preview origins are not automatically trusted. Add a specific preview
origin only when it is stable and intentionally approved; do not use wildcard
subdomain patterns.

## Deploy the frontend to Vercel

1. Import the existing repository into Vercel, or configure the project root as
   `Frontend` when importing a monorepo.
2. Use the Vite framework preset, install command `npm ci`, build command
   `npm run build`, and output directory `dist`.
3. Set `VITE_API_BASE_URL` in Vercel's Production environment to the final
   backend origin, for example `https://api.example.com`. Set it for Preview
   only if that preview is permitted in the backend's exact CORS list and the
   cookie topology supports it.
4. Deploy and verify direct refreshes of `/login`, `/register`, `/admin`,
   `/admin/subjects`, `/student`, `/student/exams`, and a saved result URL.
   `Frontend/vercel.json` routes SPA paths to `index.html`; Vercel serves
   existing static assets before applying the fallback rewrite.
5. Verify that the browser's requests target the configured backend origin,
   not `localhost`. Vite environment values are embedded at build time, so
   changing them requires a new deployment.

## Deploy the backend and managed MySQL

1. Choose a Java 17-compatible host that supports a Spring Boot executable JAR
   and supplies a stable HTTPS custom domain. Configure managed MySQL with a
   dedicated least-privilege application user and database.
2. Configure the backend environment variables above, including
   `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `JWT_SECRET`, `FRONTEND_URL`, and Google OAuth credentials. Set the exact
   frontend value in `CORS_ALLOWED_ORIGINS`. Ensure the JDBC URL enforces the
   selected MySQL provider's TLS/hostname verification requirements.
3. Build from `Backend` with:

   ```powershell
   .\mvnw.cmd clean package
   ```

   Deploy the executable JAR under `Backend\target` using the selected host's
   Java application process. Let the host provide `PORT`; the app binds to it.
4. Configure the custom backend domain and HTTPS. Keep
   `REFRESH_COOKIE_SECURE=true`. If the host terminates TLS at a proxy, it must
   forward the original scheme and host using trusted forwarding headers; the
   app is configured to consume forwarded headers.
5. Inspect provider logs for Flyway migration validation, Hibernate schema
   validation, successful startup, and database connection failures. Never
   paste credentials or token/cookie values into logs or support tickets.
6. Configure the host's health probe as `GET /health`. It returns only `UP` or
   `DOWN` for process/database connectivity and responds 503 when the database
   connection check fails.

Flyway migrations are versioned under `Backend/src/main/resources/db/migration`
and remain enabled. Startup can apply pending forward migrations; review the
migration history and take the database provider's backup before deployment.
No migration is introduced by this deployment-preparation change.

## Google OAuth setup

The backend callback URL format is:

```text
https://<BACKEND_DOMAIN>/login/oauth2/code/google
```

For local development it is:

```text
http://localhost:8080/login/oauth2/code/google
```

In Google Cloud Console, configure the OAuth consent screen and add the exact
backend callback URL under **Authorized redirect URIs**. Add the frontend
origin under **Authorized JavaScript origins** only if required by the Google
client/application setup; this platform starts OAuth through Spring Security
on the backend. Set the resulting client ID and secret only on the backend.
Set `FRONTEND_URL` to the exact React origin. On success, the backend sets the
refresh cookie and redirects to `/auth/callback?oauth=success`; access and
refresh tokens are not placed in the redirect URL. OAuth provider roundtrip
must be tested after the real domains and credentials are configured.

## Post-deployment verification

- [ ] `/health` returns `UP` and only generic status fields.
- [ ] Untrusted CORS origins are rejected; the configured exact origin can make
      credentialed preflight and API requests.
- [ ] Student registration, login, refresh after page reload, and logout work.
- [ ] State-changing requests fail without CSRF and succeed with the expected
      `X-XSRF-TOKEN` header.
- [ ] Google sign-in completes using the deployed callback and restores the
      session without token values in the URL.
- [ ] Student dashboard, exam catalogue, exam details, submission, saved result
      retrieval, and refreshed dashboard work against the deployed backend.
- [ ] Admin routes and APIs reject unauthenticated and wrong-role requests;
      students cannot retrieve other students' result IDs.
- [ ] Admin subject/exam/question CRUD and publishing rules remain functional.
- [ ] Refresh direct frontend route URLs and confirm the Vercel SPA rewrite.
- [ ] Confirm an attempt remains available after reload and check that no
      individual answers are persisted in result history.

## Current verification limits

Local builds and tests do not constitute a production deployment. No Vercel,
backend-host, Google Cloud, or managed-database account has been configured by
this guide. Google OAuth, production cookie behavior, host proxy headers, and
browser behavior on the final custom domains must be verified after the domains
are assigned.
