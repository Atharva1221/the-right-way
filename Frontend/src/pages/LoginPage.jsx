import { useEffect, useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { getGoogleAuthorizationUrl } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { Alert, Brand, Button, LoadingState, TextField } from "../components/ui";

export function LoginPage() {
  const {
    user,
    isLoading,
    sessionError,
    signIn,
    retrySession,
    dismissSessionError,
  } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const registrationSuccess = location.state?.registrationSuccess;
  const googleAuthorizationUrl = getGoogleAuthorizationUrl();

  useEffect(() => {
    if (user) {
      navigate(user.role === "ADMIN"
        ? "/admin"
        : user.profileComplete
          ? "/student/dashboard"
          : "/student/complete-profile", { replace: true });
    }
  }, [user, navigate]);

  if (isLoading) {
    return <LoadingState label="Checking your session" />;
  }
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

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      const signedInUser = await signIn(email.trim(), password);
      const next = location.state?.from;
      const root = signedInUser.role === "ADMIN" ? "/admin" : "/student";
      const destination = signedInUser.role === "ADMIN"
        ? root
        : signedInUser.profileComplete
          ? `${root}/dashboard`
          : `${root}/complete-profile`;
      const mayReturn = next === root || next?.startsWith(`${root}/`);
      navigate(mayReturn ? next : destination, { replace: true });
    } catch (signInError) {
      setError(signInError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-layout">
      <section className="login-intro" aria-labelledby="intro-heading">
        <div className="login-intro__top">
          <Brand />
          <span className="login-intro__caption">Learn. Practice. Improve.</span>
        </div>
        <div className="login-intro__content">
          <p className="eyebrow eyebrow--light">Learn with intention</p>
          <h1 id="intro-heading">Make room for<br />what you know.</h1>
          <p className="login-intro__copy">
            A focused place for exam practice, thoughtful review, and steady progress.
          </p>
          <div className="study-sheet" aria-hidden="true">
            <div className="study-sheet__header">
              <span>STUDY NOTES</span>
              <span className="study-sheet__page">01</span>
            </div>
            <span className="study-sheet__rule study-sheet__rule--wide" />
            <span className="study-sheet__rule" />
            <span className="study-sheet__rule study-sheet__rule--short" />
            <div className="study-sheet__check">
              <span />
              <span className="study-sheet__rule study-sheet__rule--check" />
            </div>
            <div className="study-sheet__check">
              <span />
              <span className="study-sheet__rule study-sheet__rule--check study-sheet__rule--medium" />
            </div>
            <div className="study-sheet__stamp">ONE STEP<br />AT A TIME</div>
          </div>
        </div>
        <div className="login-intro__bottom">
          <span>Practice</span>
          <span aria-hidden="true">/</span>
          <span>Review</span>
          <span aria-hidden="true">/</span>
          <span>Progress</span>
        </div>
      </section>

      <section className="login-panel" aria-labelledby="login-heading">
        <div className="login-panel__mobile-brand"><Brand /></div>
        <div className="login-form-wrap">
          <p className="eyebrow">Your learning, in focus</p>
          <h2 id="login-heading">Welcome back</h2>
          <p className="login-panel__description">
            Sign in to continue to your workspace.
          </p>

          {registrationSuccess && (
            <Alert variant="success">
              Your student account is ready. Sign in with your email and password.
            </Alert>
          )}
          {sessionError && (
            <Alert variant="warning" onDismiss={dismissSessionError}>
              {sessionError}
            </Alert>
          )}
          {sessionError && (
            <Button
              className="login-retry"
              onClick={() => void retrySession()}
              variant="quiet"
            >
              Try checking again
            </Button>
          )}
          {error && <Alert>{error}</Alert>}

          {googleAuthorizationUrl ? (
            <a
              className="button button--quiet auth-google"
              href={googleAuthorizationUrl}
            >
              <GoogleMark />
              Continue with Google
            </a>
          ) : (
            <Alert variant="warning">
              Sign-in is unavailable until the backend API URL is configured.
            </Alert>
          )}
          <div className="auth-divider" aria-hidden="true">
            <span>or sign in with email</span>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            <TextField
              autoComplete="username"
              id="email"
              label="Email address"
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@example.com"
              required
              type="email"
              value={email}
            />
            <TextField
              autoComplete="current-password"
              id="password"
              label="Password"
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
            />
            <Button
              className="login-submit"
              disabled={isSubmitting}
              type="submit"
            >
              {isSubmitting ? (
                <>
                  <span className="button-spinner" aria-hidden="true" />
                  Signing in…
                </>
              ) : (
                "Sign in"
              )}
            </Button>
          </form>
          <p className="login-panel__privacy">
            Your account is protected by secure sign-in.
          </p>
          <p className="auth-switch">
            Don’t have an account? <Link to="/register">Sign up</Link>
          </p>
        </div>
        <footer className="login-panel__footer">The Right Way <span>·</span> Learn. Practice. Improve.</footer>
      </section>
    </main>
  );
}

function GoogleMark() {
  return (
    <svg aria-hidden="true" className="google-mark" viewBox="0 0 48 48">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.91c-.58 2.96-2.25 5.48-4.72 7.18l7.62 5.91c4.45-4.11 7.17-10.16 7.17-17.56Z" />
      <path fill="#FBBC05" d="M10.53 28.59A14.4 14.4 0 0 1 9.75 24c0-1.59.27-3.13.76-4.59l-7.98-6.19A23.9 23.9 0 0 0 0 24c0 3.87.93 7.53 2.56 10.78l7.97-6.19Z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.9-5.8l-7.62-5.91c-2.12 1.42-4.84 2.27-8.28 2.27-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z" />
    </svg>
  );
}
