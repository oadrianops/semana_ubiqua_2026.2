import { lazy } from 'react';
import { Route, Routes } from 'react-router';
import { RequireAuth } from '../shared/auth/RequireAuth';
import { HomePage } from '../features/search/pages/HomePage';
import { SearchPage } from '../features/search/pages/SearchPage';
import { MarketsPage } from '../features/markets/pages/MarketsPage';
import { MarketPage } from '../features/markets/pages/MarketPage';
import { StallPage } from '../features/stalls/pages/StallPage';
import { AppLayout } from './AppLayout';

// Rotas públicas entram de imediato; produto, categoria, páginas institucionais, conta, colaborador e
// administração carregam sob demanda (9.4, 9.14).
const ProductPage = lazy(() => import('../features/products/pages/ProductPage'));
const CategoryPage = lazy(() => import('../features/categories/pages/CategoryPage'));
const InfoPage = lazy(() => import('../features/info/pages/InfoPage'));
const NotFoundPage = lazy(() => import('../features/info/pages/NotFoundPage'));
const LoginPage = lazy(() => import('../features/auth/pages/LoginPage'));
const SignupPage = lazy(() => import('../features/auth/pages/SignupPage'));
const VerifyEmailPage = lazy(() => import('../features/auth/pages/VerifyEmailPage'));
const ForgotPasswordPage = lazy(() => import('../features/auth/pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('../features/auth/pages/ResetPasswordPage'));
const TasksPage = lazy(() => import('../features/tasks/pages/TasksPage'));
const TaskPage = lazy(() => import('../features/tasks/pages/TaskPage'));
const HistoryPage = lazy(() => import('../features/tasks/pages/HistoryPage'));
const EarningsPage = lazy(() => import('../features/earnings/pages/EarningsPage'));
const NotificationsPage = lazy(() => import('../features/notifications/pages/NotificationsPage'));
const ProfilePage = lazy(() => import('../features/profile/pages/ProfilePage'));
const AdminRoutes = lazy(() => import('../features/admin/AdminRoutes'));

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="markets" element={<MarketsPage />} />
        <Route path="markets/:slug" element={<MarketPage />} />
        <Route path="stalls/:publicId" element={<StallPage />} />
        <Route path="products/:slug" element={<ProductPage />} />
        <Route path="categories/:slug" element={<CategoryPage />} />
        <Route path="about" element={<InfoPage page="about" />} />
        <Route path="for-vendors" element={<InfoPage page="vendors" />} />
        <Route path="privacy" element={<InfoPage page="privacy" />} />
        <Route path="terms" element={<InfoPage page="terms" />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route element={<RequireAuth minRole="COLLABORATOR" />}>
          <Route path="collaborate" element={<TasksPage />} />
          <Route path="collaborate/tasks/:publicId" element={<TaskPage />} />
          <Route path="collaborate/history" element={<HistoryPage />} />
          <Route path="collaborate/earnings" element={<EarningsPage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
        <Route element={<RequireAuth minRole="MODERATOR" />}>
          <Route path="admin/*" element={<AdminRoutes />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
