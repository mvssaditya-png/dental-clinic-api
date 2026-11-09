# Step 10G.5F — Secure cross-tab Web authentication design

Status: DESIGN ONLY, 2026-11-09. Approval required before implementation. No application, schema, permission or deployment changes are authorized by this document.

## 1. Verified current architecture

Backend is Spring Boot 4.1.1, Java 17, Spring Security, JPA, PostgreSQL and Flyway (pom.xml). Latest migration is V22__odontogram_condition_configuration.sql.

Source paths relative to backend:
- src/main/java/com/dentalclinic/security/config/SecurityConfig.java: STATELESS, CSRF disabled, bearer filter, only send-otp/verify-otp public.
- src/main/java/com/dentalclinic/security/filter/JwtAuthenticationFilter.java: Authorization: Bearer validation, current active user, clinic policy and runtime role/permission lookup.
- src/main/java/com/dentalclinic/auth/controller/AuthController.java: POST /api/auth/send-otp, POST /api/auth/verify-otp, GET /api/auth/me. No session creation/restoration/logout API.
- src/main/java/com/dentalclinic/auth/service/OtpService.java: validates OTP and login eligibility, consumes OTP, updates login metadata, issues JWT. Reuse this validation transaction; do not duplicate OTP rules.
- src/main/java/com/dentalclinic/auth/service/JwtService.java: signed expiring bearer JWT. No server-side logout revocation found in inspected auth/security code.
- src/main/resources/application.properties: API port 8080. Secret configuration values are intentionally excluded from this report.

Frontend paths:
- src/providers/AuthProvider.tsx: restoring state precedes routing; validates expiry and /auth/me; generation guards protect logout/identity changes. Sign-out clears local storage only.
- src/lib/auth/session-storage.ts: browser sessionStorage; session-storage.native.ts: SecureStore.
- src/lib/auth/authenticated-request.ts: bearer/expiry checks, stale-response protection.
- src/lib/api/client.ts: Authorization bearer, credentials: omit, no-store.
- app/_layout.tsx: restoration/error screen before protected routes; no premature-login redirect found.
- src/components/layout/NavigationLinks.tsx: ordinary route-only Expo Router links.
- src/providers/SelectedClinicProvider.tsx and src/lib/clinics/selected-clinic.ts: per-identity, in-memory selection; platform selection validated; no shared selected-clinic storage.
- src/lib/auth/authorization.ts and capabilities.ts: runtime authorities, not inferred role grants.

Independent tabs have no sessionStorage token. Same-tab reload can restore it and revalidate /me. Copying tokens between tabs or moving them to localStorage is not the proposed solution. Native authentication remains unchanged.

## 2. Recommended architecture

Add an explicit Web-only opaque-cookie session alongside native bearer authentication. Keep Spring Security's request context stateless: the new application session repository is authoritative; do not accidentally enable a second JSESSIONID/HttpSession mechanism.

Generate 32 cryptographically random bytes with SecureRandom, encode base64url without padding (43 characters). The browser receives this opaque value only in an HttpOnly cookie. Persist only SHA-256 of the decoded random value, never the raw value. SHA-256 is appropriate for a high-entropy random secret; it is not a password hashing recommendation. Reject malformed/oversized/duplicate session cookies. Generate fresh randomness after successful OTP login; never accept a client-chosen session or promote an anonymous session.

Recommended initial policy, subject to approval: 30-minute inactivity timeout and 12-hour absolute lifetime; no remember-me. Use database UTC instants. Cookie is a browser-session cookie (no persistent Max-Age on creation); server limits remain authoritative even if the browser restores tabs after restart. Do not rotate the session ID on every request because parallel tabs would race. Rotate on reauthentication, atomically revoking the prior presented Web session. Separate browsers/devices may have separate sessions.

Every authenticated request validates the session against the shared database and loads current user/clinic eligibility and authorities using the existing bearer-equivalent policy. No positive authentication cache in the first implementation. Database failure fails closed with 503, not anonymous fallback or fake logout success. Browser identity is origin-wide; simultaneous different-account sessions in different tabs are not supported. Different clinic selections may exist per tab only when independently authorized.

## 3. Proposed persistence and additive migration

Reserve V23__web_auth_sessions.sql only if V23 remains unused at implementation time. Never modify V21/V22. This design creates no migration file.

web_auth_session:
- id UUID primary key (internal audit identifier, never a credential)
- token_hash BYTEA NOT NULL UNIQUE, CHECK octet_length=32
- user_id UUID NOT NULL FK app_user(id), ON DELETE RESTRICT
- created_at TIMESTAMPTZ NOT NULL
- last_seen_at TIMESTAMPTZ NOT NULL
- expires_at TIMESTAMPTZ NOT NULL (absolute)
- revoked_at TIMESTAMPTZ NULL
- revocation_reason VARCHAR(40) NULL, bounded codes: LOGOUT, REAUTHENTICATED, SECURITY
- csrf_key_version SMALLINT NOT NULL
- constraints expires_at > created_at; last_seen_at >= created_at; revocation timestamp/reason both null or both present
- index user_id; index expires_at for cleanup. Unique hash serves lookup. No roles, clinical data or selected clinic persisted.

Authentication uses a conditional UPDATE ... RETURNING by token_hash: revoked_at IS NULL, expires_at > now, last_seen_at greater than now minus idle policy. Set last_seen_at=greatest(last_seen_at, database current time). This serializes touches against revocation without reviving revoked sessions; reject boundary equality as expired. Retain the row ID for request audit. Small first-version per-request DB write cost is explicit; optimize only after measuring without weakening expiry/revocation.

web_auth_session_event (recommended same migration): UUID id, nullable session_id UUID FK ON DELETE SET NULL, user_id UUID FK ON DELETE RESTRICT, event_type bounded string, occurred_at TIMESTAMPTZ, outcome_code bounded string. Events: CREATED, REVOKED, EXPIRED. Internal identifiers only, no cookies, JWTs, OTPs, CSRF values, request bodies or clinical data. Insert creation/revocation audit atomically with state change; no generic event update/delete API. Retention cleanup is controlled maintenance, not clinical audit deletion. Do not promise database-enforced immutable audit without separately specifying triggers and retention exceptions.

Daily bounded cleanup: record EXPIRED once before removing expired sessions older than proposed 7-day retention; retain auth events proposed 90 days. Use unique (session_id,event_type) where appropriate for once-only terminal events, row locking and batches safe across instances. Never extend expired sessions. Retention durations require approval. Only isolated test DBs during development.

## 4. CSRF and request authentication rules

Use Spring Security CSRF integration with a dedicated repository/request handler compatible with the resolved Spring Security version. All cookie-authenticated POST/PUT/PATCH/DELETE require X-CSRF-Token. No mutation through GET. Native bearer-only requests retain their existing CSRF exemption.

Authenticated CSRF token: derive a per-session value as base64url HMAC-SHA-256 of domain-separated text containing the internal session UUID, using a dedicated server CSRF key (not the JWT key). Persist only key version. Endpoint returns token in JSON; browser holds it in memory. Validate exact encoding and constant-time comparison. Never derive a usable token from session token_hash alone. Rotation supports current/previous key versions until associated sessions expire or are revoked. All API nodes share keys from secret management. Refreshing CSRF in one tab must not invalidate other tabs' tokens. No readable auth cookie.

Pre-login CSRF: GET /api/auth/web/csrf creates a short-lived, HttpOnly host-only preauth cookie containing 32 random bytes. Return a signed HMAC token binding this nonce and a 10-minute expiry (domain-separated preauth purpose). Validate on Web OTP-send and verify. Reuse a valid existing nonce so opening tabs does not rotate it unnecessarily. No authenticated privileges or database session from this bootstrap. Successful login clears preauth cookie and provides an authenticated CSRF token through the next GET. Race between first-ever anonymous tabs may require refreshing CSRF; never automatically replay OTP verification.

Additional defense for Web unsafe endpoints and cookie-authenticated writes: exact Origin allowlist, or validated Referer origin when Origin is absent; reject neither-present, null or unexpected origins. Reject cross-site Fetch Metadata where supplied. CORS is not CSRF protection. Trusted proxies must preserve browser Origin/Referer. Production requests must use JSON content type for JSON writes. Error code CSRF_INVALID is 403; it is distinct from missing capability and 401. Refreshing a CSRF token is safe, but clinical mutations must never be automatically replayed after uncertain responses.

Precedence is explicit: if a session cookie and any Authorization header coexist on a protected API, reject 400 AMBIGUOUS_CREDENTIALS; no fallback from an invalid bearer to a cookie. Bearer-only keeps existing JWT validation. Cookie-only invokes the new session filter. Web auth endpoints reject Authorization headers and use Web-specific handling. Do not decide based on User-Agent or trust a client platform flag. Native fetch remains credentials: omit.

Filter ordering: CORS/preflight first; resolve authentication mode and Web session validity; CSRF validation before controllers; existing authorization/method guards afterward. Keep existing exception JSON conventions. Anonymous login bootstrap and Web OTP paths are explicitly permitted but still receive preauth CSRF/origin/rate-limit checks. Never exempt all /auth/** from CSRF.

## 5. Proposed contracts (additive)

All Web auth responses use Cache-Control: no-store; do not log request/response secrets.

| Endpoint | Request | Success | Security |
|---|---|---|---|
| GET /api/auth/web/csrf | Cookie if present | 200 {csrfToken, headerName:"X-CSRF-Token"} | Valid session produces bound token; otherwise anonymous preauth bootstrap only; no identity assertion |
| POST /api/auth/web/send-otp | Existing SendOtpRequest + CSRF header | Existing SendOtpResponse | Preauth/session CSRF and allowed origin; same OTP throttling/validation |
| POST /api/auth/web/verify-otp | Existing VerifyOtpRequest + CSRF header | 200 {success:true, expiresAt:UTC timestamp}; Set-Cookie | OTP verification and session/audit insert one transaction; no accessToken in JSON |
| GET /api/auth/me | Web cookie OR existing native bearer | Existing AuthMeResponse unchanged | Current identity/authorities/clinic policy; invalid session 401 |
| POST /api/auth/web/logout | No body, session cookie + CSRF | 204 after committed revocation and cookie expiry | Current browser session only; invalid/absent session may return 204 clearing cookie after origin check; valid session always requires CSRF |

Pre-login OTP consumed but response lost: do not retry verify automatically. Reconcile with GET /auth/me; if no session cookie was received, user requests a new OTP. A newly created but undelivered session expires naturally. Logout response lost: GET /auth/me should return 401 if revocation committed; no client should claim server logout succeeded merely because it cleared UI state.

401: invalid/expired/revoked session; 403: permission or typed CSRF rejection; 400: malformed/ambiguous credentials; existing OTP business error conventions preserved; 503: session persistence unavailable. Do not expose token validity details to unauthorized callers.

## 6. Frontend implementation plan

Introduce platform-specific auth transport/session strategy while preserving public feature authenticatedRequest options. Web uses credentials:same-origin for recommended topology (include only for an explicitly approved cross-origin topology), no Authorization token, and in-memory CSRF header for unsafe methods. Native retains existing JWT/SecureStore behavior, expiry and headers. Do not alter feature If-Match headers or uncertainty-recovery behavior.

Web initialization: INITIALIZING/restoring -> GET /auth/me -> validated signedIn, or 401 signedOut, or transport failure restoreError with retry. Root guards already support this. Once authenticated, fetch session CSRF before allowing writes; safe reads can proceed. If /csrf races session loss, /me and subsequent protected calls still enforce authentication. Never publish partial identity or reuse previous user's clinic store.

Login: bootstrap CSRF, use Web OTP endpoints, reconcile /me, obtain fresh authenticated CSRF, publish identity. Never save session credential in Web sessionStorage/localStorage. Native paths remain existing endpoints. New tabs and direct URLs run the same independent /me initialization. Preserve requested internal route through restoration; if login is needed, retain only a validated internal path, no credential and no external redirect target.

Logout: block new local requests immediately, invalidate generation/abort pending UI reads, POST logout, clear identity/clinic/CSRF after confirmed revocation or confirmed 401. On uncertain failure retain explicit logout-pending recovery; no claim all tabs are signed out. Broadcast only a nonsecret invalidation event after success (BroadcastChannel where available); recipients clear stale UI and revalidate, never accept identity or clinic from a message. Also revalidate on focus/visibility and protected 401. Broadcast is a UX optimization; server revocation is the authority. A request authenticated before logout may already be in flight and can complete: no promise of retroactive cancellation. Requests authenticated after committed logout must fail, across all nodes.

Different account login in another tab: broadcast session-changed invalidation and revalidate on focus. For unsafe requests include a nonsecret expected user ID from current /me context in X-Expected-User-Id; server compares with cookie principal and rejects stale-account requests before mutation. This is an identity consistency assertion, never an authority or clinic override. Treat mismatch as reinitialization-required, never retry. This prevents stale forms submitting under a replaced shared cookie account. It requires tests and approval as part of the design.

SelectedClinicProvider remains per-tab memory. Bound user derives clinic from /me; platform user must reselect and validate an active clinic in each tab. Never sync selected clinic or authority lists across tabs. Refresh /me must preserve selection only for same validated identity and revalidate access, otherwise discard. Keep backend TenantClinicResolver and capabilities unchanged.

## 7. Actual deployment findings and required configuration

Observed local configuration: frontend EXPO_PUBLIC_API_BASE_URL_WEB=/api; native API http://192.168.1.12:8080/api; backend server.port=8080. metro.config.js proxies /api to DEV_API_PROXY_TARGET or http://127.0.0.1:8080. Browser localhost:8081 is the reported development entry point, not a production domain. Metro rewrites Host and currently deletes Origin/Referer. No production ingress, domain, TLS or credentialed CORS configuration was found in inspected repositories; deployed infrastructure may exist outside them and must be supplied before rollout.

Preferred production topology: serve Web and /api under one HTTPS public origin through a trusted reverse proxy. Cookie __Host-dental-web-session; HttpOnly; Secure; SameSite=Lax; Path=/; no Domain. Separate preauth cookie __Host-dental-web-preauth with same attributes and 10-minute Max-Age. Expire with matching name/path and Max-Age=0. No auth cookie in JS. Host prefix intentionally requires Path=/; proxy access logs must redact Cookie/Set-Cookie and Authorization.

Production proxy: forward all Set-Cookie headers without coalescing; preserve cookies/CSRF/Origin/Referer; disable auth caching; trust forwarded scheme/host only from known proxy hops. Strip forged incoming forwarded headers at ingress. Do not derive origin allowlists from arbitrary Host headers. Enable TLS/HSTS at approved edge. SPA deep routes fall back to frontend index; /api errors must never fall back to index.

Development: keep same-origin /api proxy; preserve Origin/Referer and explicitly allow actual loopback frontend origins. Local HTTPS is preferred. If HTTP loopback is approved, use distinct dental-web-session-dev/dental-web-preauth-dev names without __Host prefix and Secure=false ONLY in explicit local-development profile; host-only, HttpOnly, SameSite=Lax, Path=/ remain. Fail startup for insecure cookie production config. Do not extend HTTP exception to LAN/shared hosts. Ports do not isolate cookies: use separate local hostnames/profiles when running multiple apps. Never send production cookies to development.

Cross-origin deployment, if unavoidable: exact scheme/host/port allowlist, credentials include, Access-Control-Allow-Credentials true, Vary: Origin, no wildcard; allow X-CSRF-Token, X-Expected-User-Id, Content-Type and existing If-Match/If-None-Match headers/methods, unauthenticated OPTIONS. Same-site HTTPS subdomains may use Lax host-only API cookies; genuinely cross-site needs SameSite=None; Secure and faces third-party-cookie restrictions. Prefer same-origin proxy rather than relying on cross-site cookies. CORS still applies to GET /csrf response readability; deny untrusted origins.

## 8. Security review

CSRF: session-bound token plus origin checks, including login/logout. XSS: HttpOnly reduces credential extraction but cannot stop malicious same-origin requests; maintain escaping, CSP and dependency hygiene. Never render notes as HTML. Fixation: rotate on login and revoke prior session atomically. Replay: stolen raw cookie remains usable until revocation/expiry; TLS, short limits and server checks reduce exposure. Persistence read compromise: only high-entropy hashes, not usable cookie values; CSRF signing secret stored outside DB. DB write compromise remains severe and is not solved by hashing. No secret logs, URLs, telemetry or broadcast payloads.

Runtime permissions/user/clinic policy are recomputed, not cached in session claims. No role-grant changes. Account disable/clinic disable immediately affect subsequent checks. Session event rows are security metadata, not clinical audit. Logout does not revoke unchanged native JWTs or other browser sessions; a future logout-all feature is separate. In-flight writes at logout are a documented boundary. Shared-cookie cross-account races require expected-user consistency checks described above.

## 9. Deployment, migration and rollback

1. Approve topology, timeouts, storage/retention, precedence and identity-consistency rules.
2. Add/test schema and Web auth backend behind disabled-by-default rollout flag. No existing tables or clinical rows changed. Preserve native and legacy bearer endpoints.
3. Deploy additive migration, server keys, CSRF/origin/proxy configuration and backend support first. Verify isolated integration and HTTPS staging tests.
4. Deploy Web client using cookie-only auth. Ignore/remove legacy environment-specific sessionStorage bearer record once new flow is enabled. Users sign in again; do not silently exchange stored JWTs. Native keeps current endpoints/storage. Legacy issued JWTs still expire under existing rules; do not claim cookie logout revokes them.
5. Enable feature in controlled environment and verify multi-tab, server-node, expiry, logout and native tests before wider rollout. No deployment performed in this step.

Rollback: disable new Web login and revert frontend to prior approved client (fresh login needed), revoke newly issued Web sessions, expire cookies through an available endpoint, keep native bearer operational. Leave additive tables/migration in place; never roll Flyway backward or edit checksums. Retain security records under approved retention. Cookie mode must fail closed if unavailable, not transparently fall back to stale stored bearer tokens. Rollback temporarily restores the known new-tab limitation.

## 10. Test and implementation sequence

A. Backend foundation: isolated PostgreSQL migration, random/hash-only storage, uniqueness, timeout boundaries, revocation races, audit atomicity, cleanup and multi-node behavior.
B. Web auth endpoints/filter/CSRF: OTP success/failure/replay/transaction rollback, fresh-ID fixation prevention, cookie flags, ambiguous credentials (including invalid bearer), missing/invalid CSRF, hostile/null/missing Origin, preauth expiry and parallel tabs, CORS, no-store, secret-log redaction, DB failure.
C. Frontend platform transport: unchanged native SecureStore/bearer tests; Web no bearer persistence, initialization, /me validation, network retry, direct URL/refresh/new tab, CSRF acquisition, no mutation auto-replay, typed failures, stale-response rejection and generation cancellation.
D. Real browsers at approved HTTPS origin: two independently opened tabs, copied URL, logout/revocation on another API node, suspended-tab resume, expired session, offline logout recovery, account replacement with stale form, unauthorized routes, bound/platform clinic selection isolation, untrusted-origin CSRF and browser cookie settings. Test actual iOS/Android native clients separately.
E. Existing clinical regression suite, TypeScript, Web export, backend verify/package, diff check. No tests/builds required or run for this design-only document; prior results are not new validation.

Likely backend additions: WebSession entity/repository/service/filter/controller/DTOs, WebCsrf repository/handler, security-event/cleanup support, V23 migration and PostgreSQL/security tests. Modify SecurityConfig, extract/reuse OTP verification transaction without changing native contract, share current-authority loading with JwtAuthenticationFilter. Frontend: AuthProvider, auth.api/types/validation, platform storage/authenticated-request/client strategy, metro.config.js, and focused auth tests. Selected-clinic behavior preserved. No Odontogram changes.

## 11. Decisions requiring approval

- Cookie-session architecture, additive session/audit migration and scoped shared OTP/auth extraction.
- 30-minute idle / 12-hour absolute limits; browser-session cookies; 7-day session and 90-day event retention.
- Production public origin/proxy/TLS owners (unknown), allowed local origins, local HTTPS versus explicit loopback HTTP profile.
- Both-credentials rejection and X-Expected-User-Id guard for shared-cookie account changes.
- CSRF signing key provision/rotation and retention maintenance ownership.
- Current-browser-session logout only; legacy/native JWT expiry unchanged; no logout-all guarantee.

Implementation should begin only after approval, with backend session/CSRF foundation first. Do not implement a frontend persistence shortcut.

## References

Design references, not substitutes for matching dependency-version tests:
- Spring Security CSRF and SPA token lifecycle: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Security logout: https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html
- OWASP session management guidance: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
