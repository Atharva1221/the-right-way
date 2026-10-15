import { useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { Brand, Button } from "./ui";

const roleLabels = {
  ADMIN: "Administrator",
  STUDENT: "Student",
};

export function AppShell({ role }) {
  const { user, signOut } = useAuth();
  const [isSigningOut, setIsSigningOut] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const isAdmin = role === "ADMIN";
  const pageTitle = isAdmin
    ? location.pathname.startsWith("/admin/subjects")
      ? "Subjects"
      : location.pathname.startsWith("/admin/exams")
        ? "Exams"
        : "Overview"
    : location.pathname.startsWith("/student/exams")
      ? "Available exams"
      : location.pathname.startsWith("/student/results")
        ? "Exam result"
        : "Dashboard";

  async function handleSignOut() {
    setIsSigningOut(true);
    try {
      await signOut();
      navigate("/login", { replace: true });
    } finally {
      setIsSigningOut(false);
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Brand />
        <div className="sidebar__section-label">Workspace</div>
        <nav aria-label="Main navigation" className="sidebar__nav">
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
            end
            to={isAdmin ? "/admin" : "/student"}
          >
            <span className="nav-link__indicator" aria-hidden="true" />
            {isAdmin ? "Overview" : "Dashboard"}
          </NavLink>
          {isAdmin ? (
            <>
              <NavLink
                className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
                to="/admin/subjects"
              >
                <span className="nav-link__indicator" aria-hidden="true" />
                Subjects
              </NavLink>
              <NavLink
                className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
                to="/admin/exams"
              >
                <span className="nav-link__indicator" aria-hidden="true" />
                Exams
              </NavLink>
            </>
          ) : (
            <NavLink
              className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
              to="/student/exams"
            >
              <span className="nav-link__indicator" aria-hidden="true" />
              Available exams
            </NavLink>
          )}
        </nav>
        <div className="sidebar__footer">
          <span className="sidebar__footer-mark" aria-hidden="true">RW</span>
          <span>Learn. Practice. Improve.</span>
        </div>
      </aside>

      <div className="app-shell__body">
        <header className="topbar">
          <div className="topbar__context">
            <span className="topbar__eyebrow">{roleLabels[role]} workspace</span>
            <span className="topbar__title">{pageTitle}</span>
          </div>
          <div className="topbar__account">
            <div className="account-chip">
              <span className="account-chip__avatar" aria-hidden="true">
                {(user?.name?.trim()?.[0] || user?.email?.[0] || "?").toUpperCase()}
              </span>
              <span className="account-chip__details">
                <strong>{user?.name}</strong>
                <span>{roleLabels[role]}</span>
              </span>
            </div>
            <Button
              className="topbar__signout"
              disabled={isSigningOut}
              onClick={handleSignOut}
              variant="quiet"
            >
              {isSigningOut ? "Signing out…" : "Sign out"}
            </Button>
          </div>
        </header>

        <main className="page-content">
          <Outlet />
        </main>
        <footer className="page-footer">
          <span>The Right Way</span>
          <span>Learn. Practice. Improve.</span>
        </footer>
      </div>
    </div>
  );
}
