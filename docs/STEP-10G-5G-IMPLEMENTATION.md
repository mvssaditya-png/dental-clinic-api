# Step 10G.5G � Secure Web sessions and shared OTP hardening

Implementation report, 2026-11-09. No deployment or application-database migration performed. Earlier uncommitted Odontogram work preserved.

## OTP safeguards
- Verification locks the newest OTP row with PESSIMISTIC_WRITE and checks used state after locking. It does not fall back to an older unused code. Concurrent success consumes once; concurrent failures serialize.
- FailedOtpException is the narrow noRollbackFor exception: its attempt-count update commits. Other errors, including a later failure in the enclosing session-creation transaction, roll back successful consumption. The Web login transaction uses the same exception rule.
- Unrestricted plaintext System.out logging removed. Native endpoints, DTOs, JWT issuance, SecureStore and runtime authorities remain unchanged.
- LocalOtpDelivery is OFF by default. Optional interactive console delivery requires the exclusive local-web profile, explicit loopback server.address and app.local-otp-console.enabled=true. It writes only to System.console(), never a redirected application log; no OTP-returning API exists. It fails if no interactive console is available. No delivery provider was added; with this option disabled, the existing code has no external SMS delivery integration.

## Session contracts
- GET /api/auth/web/csrf: returns csrfToken/headerName. Anonymous requests receive short-lived HttpOnly preauth cookie; authenticated requests receive stable session-bound HMAC CSRF token.
- POST /api/auth/web/send-otp: existing SendOtpRequest/Response, CSRF and origin required.
- POST /api/auth/web/verify-otp: existing VerifyOtpRequest, returns success/expiresAt and HttpOnly session cookie, never a bearer token in the response. Existing OTP validation reused transactionally.
- GET /api/auth/me: existing DTO, Web cookie OR native bearer authentication.
- POST /api/auth/web/logout: committed revocation then 204 and cookie clearing. Valid sessions require CSRF; absent/invalid sessions require trusted origin and clear cookies idempotently.
- Cookie plus Authorization is rejected with 400 AMBIGUOUS_CREDENTIALS. Cookie mutations require CSRF and expected user identity outside Web auth endpoints. Stale-account writes return 409 SESSION_IDENTITY_CHANGED without mutation. Cookie expiry/revocation returns 401; CSRF/origin failures return 403; session-store validation failure returns 503.
- CSRF/origin checks are enforced in the WebSessionFilter registered inside Spring Security before bearer authentication; existing bearer-only CSRF behavior remains unchanged. This is an explicit security-chain filter, not an HttpSession/CookieCsrfTokenRepository migration.

## V23
V23__web_auth_sessions.sql is additive. It creates web_auth_session and web_auth_session_event only. Session secrets are 32 random bytes; persistence contains SHA-256 hash only. Session creation/revocation and events are transactional. Absolute lifetime 24 hours; idle 8 hours. No selected clinic or cached authority grants are stored. V21/V22 unchanged.

Daily maintenance processes bounded expiry batches and purges records older than 30 days. It is disabled with Web sessions. Recent events remain; security-event retention is not clinical-history retention. No audit history UI is added.

## Local configuration and activation (manual, not executed)
Observed Web API base is /api; Metro forwards to http://127.0.0.1:8080 by default. Allowlisted local browser origin is http://localhost:8081. Use that hostname consistently. Metro now preserves Origin/Referer; no arbitrary-origin CORS is enabled.

Web sessions are disabled by default outside explicit configuration. application-local-web.properties enables only host-only HttpOnly SameSite=Lax Path=/ development cookies with Secure=false and localhost:8081 origin. Insecure cookies are rejected unless local-web is the only active profile and every origin is loopback. Secure defaults use __Host-dental-web-session and __Host-dental-web-preauth; no Domain attribute. Production enablement requires separately verified HTTPS/origins and a dedicated secret.

Before starting the modified backend, confirm the datasource is the intended disposable local DB and approve V23 application there. Verification used only dental_clinic_auth_verification_20261109163230. Do not point these instructions at shared/production data.

In your trusted terminal, provision a dedicated random Base64 key without displaying it, e.g. PowerShell:

    $csrfBytes = New-Object byte[] 32
    $csrfRng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $csrfRng.GetBytes($csrfBytes)
    $env:DENTAL_WEB_CSRF_KEY = [Convert]::ToBase64String($csrfBytes)
    $csrfRng.Dispose()
    $env:SPRING_PROFILES_ACTIVE = 'local-web'

Use normal approved backend startup with the verified local datasource. Restart Metro so proxy changes take effect. Do not put the key in source control or logs. Keep one stable dedicated key across API instances/restarts. Rotation: increment app.web-session.csrf-key-version and provide previous-csrf-key/previous-csrf-key-version until old sessions expire/revoke; preauth handshakes may need restarting during rotation. Never remove a needed previous key while active sessions reference it.

Optional local OTP visibility requires an interactive java terminal plus -Dapp.local-otp-console.enabled=true and -Dserver.address=127.0.0.1 (Java options before -jar). Loopback binding prevents direct native LAN access in that optional mode; default native server configuration is unchanged. No OTPs or credentials were entered/captured for browser verification in this implementation.

## Frontend
AuthProvider.web.tsx restores through /auth/me, keeps CSRF in memory and never persists Web bearer/session secrets. It clears the legacy environment-scoped browser session entry. Native AuthProvider.tsx and session-storage.native.ts are unchanged. Cookie-aware transport is explicit; native requests still omit cookies and send bearer tokens.

Independent tabs initialize from the shared host cookie. Background validation preserves the existing user object and per-tab clinic state when validated identity/authorities are unchanged. Changed identity invalidates old request closures. Platform clinic selection remains independently validated in each tab.

Logout blocks local calls, revokes on server and broadcasts only an invalidation signal. Other tabs revalidate on signal/focus/visibility or 401. Expected-user headers protect every authenticated frontend read and write under a newly shared account cookie. Mismatches return 409 SESSION_IDENTITY_CHANGED before CSRF evaluation or application handlers. Bootstrap /auth/me may omit this header. Failed background revalidation clears identity and CSRF state. Account changes key-remount the authenticated descendants, discarding clinical component caches; the selected-clinic store is recreated for the new identity. Unmounted providers cannot start deferred initialization. No clinical mutation is automatically replayed. Requests already authorized before revocation may finish; logout cannot retroactively cancel committed/in-flight writes. Old native/legacy JWTs are not revoked by Web logout.

## Files
Backend modified:
- auth/service/OtpService.java
- auth/repository/OtpVerificationRepository.java
- security/config/SecurityConfig.java
- tests: OtpClinicActivationTest, SecurityHttpErrorHandlingTest, ClinicControllerSecurityTest (test-context wiring only for the latter two)
Backend added (under src/main/java/com/dentalclinic):
- auth/service/FailedOtpException.java, LocalOtpDelivery.java
- auth/web/WebSessionSettings.java, WebSessionService.java, WebSessionFilter.java, WebSessionController.java, WebSessionMaintenance.java
- src/main/resources/application-local-web.properties
- src/main/resources/db/migration/V23__web_auth_sessions.sql
- tests: OtpSecurityDatabaseTest, WebSessionDatabaseTest, WebSessionSettingsTest, LocalOtpDeliveryTest
Frontend modified:
- metro.config.js
- src/features/auth/auth.api.ts
- src/lib/api/client.ts
- src/lib/auth/authenticated-request.ts (feature-facing option type excludes auth-owned fields)
Frontend added:
- src/lib/auth/web-session-api.ts
- src/providers/AuthProvider.web.tsx
- tests/web-authentication.test.ts

## Validation and limitations
Final verification: 133 backend tests passed, 0 failures/errors/skips, including 20 PostgreSQL OTP/session tests; Maven package succeeded. Focused frontend: 135 passed. Full frontend: 1,303 passed, 0 failures/skips. TypeScript, Expo Web export and both git diff --check checks passed. Lint was attempted but no lint script/configuration is provided; no dependencies were added. PostgreSQL tests use the isolated database named above; no application DB was changed. Tests cover OTP persistence/concurrency/rollback, session hashing/expiry/revocation, CSRF and origin enforcement, ambiguous credentials, current clinic eligibility, key rotation configuration, native JWT/method-security regressions, frontend restoration/logout/stale-account handling and permission/clinic stores.

Real independent-browser-tab verification remains pending activation of the backend profile and V23 in the approved local application database. No physical-device claim. No production CORS/HTTPS deployment configured. Cross-origin hosting is intentionally not enabled; same-origin /api is required for this frontend. Browser session restoration is now implemented, but the currently running old backend must be restarted/configured before manual use. No deployment performed.

## Manual browser acceptance checklist (not executed)
After intentional local activation using the setup above:
1. Sign in manually; open an independent tab and a direct protected URL, then refresh. Confirm restoration and intended per-tab clinic context.
2. Leave Tab A on patient data as User A. In Tab B sign out and sign in as User B. Tab A must discard the old screen/clinic context and revalidate. Exercise stale GET, POST, PUT, PATCH and DELETE requests: no application handler may run under the wrong expected identity.
3. Repeat with delayed responses, offline revalidation, closing/unmounting screens and unsupported BroadcastChannel. Focus/visibility and identity-header rejection provide fallback detection; an unfocused tab may retain previously rendered content until detection, but stale requests cannot retrieve another account's data.
4. Confirm logout revocation from another tab, idle/absolute expiry, CSRF rejection, untrusted-origin rejection and cookie HttpOnly/SameSite/Path attributes. Test Secure/__Host- attributes in HTTPS configuration, not insecure local development.
5. Check native bearer login and clinic selection on a device; native persistence implementation was not changed.

## Completion boundary
Implementation and automated verification are complete. Manual browser/device acceptance and intentional environment activation remain pending. No shared/production database was touched and no deployment occurred. Already-authorized in-flight requests cannot be retroactively cancelled. A browser profile shares one Web session identity; simultaneous independent accounts require separate browser profiles. Existing external OTP delivery integration is absent: default mode never prints OTP values; optional explicitly gated interactive local console delivery is documented above.