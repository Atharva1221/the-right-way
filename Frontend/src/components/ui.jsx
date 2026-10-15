import { Link } from "react-router-dom";

export function Brand({ compact = false }) {
  return (
    <Link className={`brand${compact ? " brand--compact" : ""}`} to="/" aria-label="The Right Way home">
      <span className="brand__mark" aria-hidden="true">
        <svg viewBox="0 0 28 28" fill="none">
          <path d="M4.5 7.4c3.5-.9 6.8-.2 9.5 2.1v13c-2.7-2.3-6-3-9.5-2.1v-13Z" />
          <path d="M23.5 7.4c-3.5-.9-6.8-.2-9.5 2.1v13c2.7-2.3 6-3 9.5-2.1v-13Z" />
          <path d="M14 9.5v13" />
        </svg>
      </span>
      {!compact && <span className="brand__name">The Right Way</span>}
    </Link>
  );
}

export function Button({
  children,
  variant = "primary",
  className = "",
  type = "button",
  ...props
}) {
  return (
    <button
      className={`button button--${variant} ${className}`.trim()}
      type={type}
      {...props}
    >
      {children}
    </button>
  );
}

export function ButtonLink({ children, to, variant = "primary", className = "", ...props }) {
  return (
    <Link
      className={`button button--${variant} ${className}`.trim()}
      to={to}
      {...props}
    >
      {children}
    </Link>
  );
}

export function TextField({ label, hint, error, id, ...props }) {
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className="field">
      <label className="field__label" htmlFor={id}>{label}</label>
      <input
        className={`field__input${error ? " field__input--error" : ""}`}
        id={id}
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy}
        {...props}
      />
      {hint && <p className="field__hint" id={hintId}>{hint}</p>}
      {error && <p className="field__error" id={errorId}>{error}</p>}
    </div>
  );
}

export function Alert({ children, variant = "error", onDismiss }) {
  return (
    <div className={`alert alert--${variant}`} role={variant === "error" ? "alert" : "status"}>
      <span>{children}</span>
      {onDismiss && (
        <button
          aria-label="Dismiss message"
          className="alert__dismiss"
          onClick={onDismiss}
          type="button"
        >
          <span aria-hidden="true">×</span>
        </button>
      )}
    </div>
  );
}

export function LoadingState({ label = "Loading your workspace" }) {
  return (
    <div className="loading-state" role="status" aria-live="polite">
      <span className="loading-state__spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <section className="empty-state">
      <span className="empty-state__rule" aria-hidden="true" />
      <h2>{title}</h2>
      <p>{children}</p>
    </section>
  );
}
