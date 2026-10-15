import { Link, Navigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { Alert, Brand, Button, LoadingState } from "../components/ui";

export function OAuthCallbackPage() {
  const { user, isLoading, sessionError, retrySession } = useAuth();
  const [searchParams] = useSearchParams();
  const oauthStatus = searchParams.get("oauth");
  const errorCode = searchParams.get("error");

  if (user) {
    return <Navigate
      to={user.role === "ADMIN"
        ? "/admin"
        : user.profileComplete
          ? "/student/dashboard"
          : "/student/complete-profile"}
      replace
    />;
  }
  if (oauthStatus === "error" || errorCode) {
    const message = errorCode === "account-conflict"
      ? "An account already uses this email. Sign in with that account instead; automatic Google account linking is unavailable."
      : "Google sign-in was cancelled or could not be completed. You can try again or use your email and password.";
    return <OAuthMessage message={message} />;
  }
  if (isLoading) {
    return <LoadingState label="Completing Google sign-in" />;
  }
  if (sessionError) {
    return <OAuthMessage message={`Google sign-in could not restore your session: ${sessionError}`} onRetry={() => void retrySession()} />;
  }
  return (
    <OAuthMessage
      message="Google sign-in did not provide a session. Please try again or sign in with email."
      onRetry={() => void retrySession()}
    />
  );
}

function OAuthMessage({ message, onRetry }) {
  return (
    <main className="auth-page">
      <section className="auth-card auth-card--message">
        <Brand />
        <p className="eyebrow">Secure sign-in</p>
        <h1>Sign-in status</h1>
        <Alert>{message}</Alert>
        {onRetry && <Button onClick={onRetry} variant="quiet">Try restoring session</Button>}
        <p className="auth-switch"><Link to="/login">Return to sign in</Link></p>
      </section>
    </main>
  );
}
