import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import { LoadingState } from "../components/ui";

export function RequireAuth({ role, requireCompleteProfile = false }) {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <LoadingState />;
  }
  if (!user) {
    return <Navigate
      to="/login"
      replace
      state={{ from: `${location.pathname}${location.search}` }}
    />;
  }
  if (role && user.role !== role) {
    return <Navigate to={user.role === "ADMIN" ? "/admin" : "/student"} replace />;
  }
  if (
    requireCompleteProfile
    && user.role === "STUDENT"
    && !user.profileComplete
    && location.pathname !== "/student/complete-profile"
  ) {
    return <Navigate to="/student/complete-profile" replace />;
  }
  return <Outlet />;
}
