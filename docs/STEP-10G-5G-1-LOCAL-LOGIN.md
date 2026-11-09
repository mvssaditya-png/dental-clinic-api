# Step 10G.5G.1 - Explicit local Web startup

## Verified diagnosis
The running backend on port 8080 returned 404 WEB_SESSION_DISABLED through both the direct API and Metro on 8081. WebSessionFilter returns this when WebSessionSettings.enabled is false; its default is false. Restarting without selecting local-web does not change that configuration. The running Java command had no local-web argument; no profile override appeared in JVM system properties. The effective disabled flag is confirmed by the endpoint, rather than an assumption based solely on startup arguments.

Read-only PostgreSQL verification found dental_clinic_test_db at ::1:5432, a JDBC application connection to that database, and Flyway V23 "web auth sessions" success=true. No migration or data change was needed.

## Fix
Added scripts/Start-LocalWeb.ps1. It explicitly selects local-web, the local test datasource, loopback API binding, localhost:8081 origin and local HTTP cookie mode. Default/production configuration remains disabled and Secure by default. CSRF keys are generated only in the user's terminal environment when absent; never printed or written to files. Keep that terminal/key for backend restarts. Losing the key requires a fresh login; existing cookies may need clearing.

1. Stop the existing backend in the IDE/terminal.
2. In your own interactive PowerShell terminal:
   cd D:developmentDentalapidental-clinic-api
   .scriptsStart-LocalWeb.ps1 -LocalOtpConsole
3. Open http://localhost:8081 (use localhost, not LAN IP or 127.0.0.1).
4. Request OTP and enter it yourself in the browser. Do not send it to an assistant, redirect the terminal or enable transcription.
5. Keep the backend terminal open. Subsequent IDE launches without this explicit configuration still use the secure disabled default.

The optional console flag is necessary for this repository's current local login because there is no external OTP delivery provider. It is guarded by exclusive local-web profile, loopback binding and an actual interactive Java console. Launching in an IDE output panel or redirected terminal fails safely.

This launcher binds the API to loopback; physical-device LAN access is unavailable in this opt-in local console mode. Native JWT endpoint/DTO/SecureStore behavior is unchanged. Production OTP delivery and deployment are outside this fix.

## Acceptance
After startup: verify CSRF bootstrap and HttpOnly preauth cookie; OTP request and manual verification; session cookie; /auth/me; protected URL, refresh and independent tab; logout in both tabs; reject missing CSRF and hostile Origin. Do not capture real OTP/session values in reports.

No production/shared database changes, migration edits, application security bypasses or deployment.
