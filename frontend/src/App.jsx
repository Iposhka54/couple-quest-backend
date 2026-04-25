import { Navigate, Route, Routes } from 'react-router-dom';
import { MainLayout } from './components/MainLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { EmailVerificationGate } from './components/EmailVerificationGate';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { DashboardPage } from './pages/DashboardPage';
import { TasksPage } from './pages/TasksPage';
import { ShopPage } from './pages/ShopPage';
import { MenuPage } from './pages/MenuPage';
import { CalendarPage } from './pages/CalendarPage';

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<EmailVerificationGate />}>
            <Route path="/app" element={<DashboardPage />} />
            <Route path="/tasks" element={<TasksPage />} />
            <Route path="/shop" element={<ShopPage />} />
            <Route path="/menu" element={<MenuPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}