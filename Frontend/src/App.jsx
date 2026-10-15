import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import { useAuth } from "./auth/AuthProvider";
import { AppShell } from "./components/AppShell";
import {
  AdminDashboardPage,
  AdminExamDetailPage,
  AdminExamsPage,
  AdminSubjectsPage,
} from "./pages/AdminPages";
import { LoginPage } from "./pages/LoginPage";
import { RegistrationPage } from "./pages/RegistrationPage";
import { OAuthCallbackPage } from "./pages/OAuthCallbackPage";
import { CompleteProfilePage } from "./pages/CompleteProfilePage";
import { StudentDashboardPage } from "./pages/StudentDashboardPage";
import { AvailableExamsPage } from "./pages/AvailableExamsPage";
import { ExamInstructionsPage, ActiveExamPage } from "./pages/StudentExamPages";
import { ExamResultPage } from "./pages/ExamResultPage";

const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/register", element: <RegistrationPage /> },
  { path: "/auth/callback", element: <OAuthCallbackPage /> },
  {
    element: <RequireAuth role="ADMIN" />,
    children: [
      {
        path: "/admin",
        element: <AppShell role="ADMIN" />,
        children: [
          { index: true, element: <AdminDashboardPage /> },
          { path: "subjects", element: <AdminSubjectsPage /> },
          { path: "exams", element: <AdminExamsPage /> },
          { path: "exams/:examId", element: <AdminExamDetailPage /> },
          { path: "*", element: <Navigate to="/admin" replace /> },
        ],
      },
    ],
  },
  {
    element: <RequireAuth role="STUDENT" />,
    children: [
      { path: "/student/complete-profile", element: <CompleteProfilePage /> },
    ],
  },
  {
    element: <RequireAuth role="STUDENT" requireCompleteProfile />,
    children: [
      {
        path: "/student",
        element: <AppShell role="STUDENT" />,
        children: [
          { index: true, element: <Navigate to="/student/dashboard" replace /> },
          { path: "dashboard", element: <StudentDashboardPage /> },
          { path: "exams", element: <AvailableExamsPage /> },
          { path: "exams/:examId", element: <ExamInstructionsPage /> },
          { path: "exams/:examId/take", element: <ActiveExamPage /> },
          { path: "results/:examId", element: <ExamResultPage /> },
          { path: "*", element: <Navigate to="/student" replace /> },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <RootRedirect />,
  },
]);

function RootRedirect() {
  const { user } = useAuth();
  const destination = user?.role === "ADMIN"
    ? "/admin"
    : user
      ? user.profileComplete ? "/student/dashboard" : "/student/complete-profile"
      : "/login";
  return <Navigate to={destination} replace />;
}

export default function App() {
  return <RouterProvider router={router} />;
}
