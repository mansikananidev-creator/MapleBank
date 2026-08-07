import { Routes, Route, Navigate } from 'react-router-dom'
import AuthPage from '@/pages/AuthPage'
import ForgotPasswordPage from '@/pages/ForgotPasswordPage.tsx'
import ResetPasswordPage from '@/pages/ResetPasswordPage.tsx'
import ProtectedRoute from "@/components/ProtectedRoute.tsx";
import Dashboard from "@/pages/Dashboard.tsx";
import AccountsPage from "@/pages/AccountsPage.tsx";
import AccountDetailsPage from "@/pages/AccountDetailsPage.tsx";
import Layout from "@/components/Layout.tsx";
import TransactionsPage from "@/pages/TransactionsPage.tsx";
import LoansPage from "@/pages/LoansPage.tsx";
import AdminRoute from "@/components/AdminRoute.tsx";
import AdminLoansPage from "@/pages/AdminLoansPage.tsx";
import AdminCompliancePage from "@/pages/AdminCompliancePage.tsx";
import RecurringPaymentsPage from "@/pages/RecurringPaymentsPage.tsx";
import ProfilePage from "@/pages/ProfilePage.tsx";

function App() {
  return (
      <Routes>
          <Route path="/login" element={<AuthPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route element={<Layout />}>
              <Route
                  path="/dashboard"
                  element={
                      <ProtectedRoute>
                          <Dashboard />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/accounts"
                  element={
                      <ProtectedRoute>
                          <AccountsPage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/accounts/:id"
                  element={
                      <ProtectedRoute>
                          <AccountDetailsPage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/transactions"
                  element={
                      <ProtectedRoute>
                          <TransactionsPage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/loans"
                  element={
                      <ProtectedRoute>
                          <LoansPage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/recurring-payments"
                  element={
                      <ProtectedRoute>
                          <RecurringPaymentsPage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/profile"
                  element={
                      <ProtectedRoute>
                          <ProfilePage />
                      </ProtectedRoute>
                  }
              />
              <Route
                  path="/admin/loans"
                  element={
                      <AdminRoute>
                          <AdminLoansPage />
                      </AdminRoute>
                  }
              />
              <Route
                  path="/admin/compliance"
                  element={
                      <AdminRoute>
                          <AdminCompliancePage />
                      </AdminRoute>
                  }
              />
          </Route>
          <Route path="*" element={<Navigate to="/login" />} />
      </Routes>
  )
}

export default App