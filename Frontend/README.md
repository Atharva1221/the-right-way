# The Right Way

React and Vite foundation for The Right Way student and administrator experiences.

## Run locally

1. Start the Spring Boot backend on `http://localhost:8080`.
2. From this directory, install dependencies and start Vite:

   ```powershell
   npm install
   npm run dev
   ```

3. Open the local URL printed by Vite (normally `http://localhost:5173`).

The Vite development server proxies `/api`, `/oauth2`, and `/login/oauth2` to the backend. The default target is `http://localhost:8080`; override it with `VITE_BACKEND_PROXY_TARGET` in a local `.env` file when needed.

## Environment

Copy `.env.example` to `.env` to configure local overrides:

- `VITE_BACKEND_PROXY_TARGET` — Spring Boot origin used by the Vite development proxy; defaults to `http://localhost:8080`.
- `VITE_API_BASE_URL` — optional absolute API origin for development; required as an HTTPS origin for production builds.

The backend accepts credentialed CORS requests only from exact origins configured through `CORS_ALLOWED_ORIGINS`; wildcard origins are rejected. The refresh cookie is `HttpOnly`, `Secure` in the production profile, scoped to `/api/auth`, and uses `SameSite=Lax`. Use custom frontend/backend domains on the same registrable domain, or a same-origin proxy; unrelated frontend/backend sites may have refresh cookies blocked by browsers.

See [the deployment guide](../DEPLOYMENT.md) for production setup, environment-variable reference, Vercel routing, backend hosting, MySQL, CORS/CSRF/cookie behavior, and Google OAuth callback configuration.

## Authentication behavior

- Login uses `POST /api/auth/login`; `GET /api/auth/csrf` returns a CSRF token in JSON and the frontend sends it in `X-XSRF-TOKEN` for unsafe requests. The CSRF cookie is HttpOnly and scoped to the backend host.
- Access tokens are kept in application memory only. The refresh token remains in the backend-managed HttpOnly cookie.
- Startup attempts `POST /api/auth/refresh`, then confirms identity through `GET /api/auth/me`. The `/me` response is used for identity and role; it is not expected to return an access token.
- Authenticated API requests can refresh once after a `401`; a failed refresh clears the in-memory session.
- Logout uses `POST /api/auth/logout` and clears the local in-memory session.
- Student registration uses `POST /api/auth/register` with name, email, phone number and password; registration does not authenticate the new account, so sign in after successful registration.
- Google sign-in starts at `/oauth2/authorization/google`. On success, the backend sets the normal HttpOnly refresh cookie and redirects to the configured frontend callback; the callback restores the access token in memory through refresh and `/api/auth/me`. No tokens are placed in the redirect URL. New Google student accounts are prompted to supply the phone number required by the existing profile-completion contract.
- Set backend `FRONTEND_URL` to the frontend origin used for OAuth callback redirects. Configure the Google OAuth client in Google Cloud Console with the backend callback URI `http://localhost:8080/login/oauth2/code/google` for local development, or the deployed backend equivalent. Keep client credentials only on the backend.

## Available scripts

- `npm run dev` — local Vite server.
- `npm run build` — production build to `dist`.
- `npm run preview` — serve the production build locally.

## Student exam flow

- Student pages use the backend dashboard, available-exams, exam-detail, submission, and saved-result APIs. Student authorization remains enforced by Spring Security.
- The active exam stores its start timestamp and selected option IDs in `sessionStorage`, scoped to the signed-in student and exam. This allows same-tab reload recovery without storing credentials or sending answers to result history. Session storage is browser-local and can be cleared or unavailable; reload recovery is therefore best-effort.
- Remaining time is recalculated from the stored start timestamp and exam duration, including when the tab becomes active again. Timing is client-side only and is not tamper-proof or enforced by the backend.
- Submission results include a result ID. The result URL includes this ID and reloads the summary from the authenticated result endpoint. Only score, maximum marks, pass status, exam and subject names, and submission time are persisted; individual answers and the detailed correct/incorrect/unanswered breakdown are not available after the immediate result view.
- The dashboard fetches current summary metrics when opened, so returning after a successful submission loads updated data.

The administrator workspace provides subject, exam, and question management through the authenticated admin routes.
