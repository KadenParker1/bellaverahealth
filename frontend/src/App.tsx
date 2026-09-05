import { Navigate, Route, Routes } from 'react-router-dom'
import { RedirectIfAuthed } from './auth/RedirectIfAuthed'
import { RequireAuth } from './auth/RequireAuth'
import { LoginPage } from './auth/pages/LoginPage'
import { SignupPage } from './auth/pages/SignupPage'
import { OnboardingPage } from './surveys/pages/OnboardingPage'
import { SurveyTakePage } from './surveys/pages/SurveyTakePage'
import { HomePage } from './themes/pages/HomePage'
import { ThemeDetailPage } from './themes/pages/ThemeDetailPage'
import { LearnMorePage } from './themes/pages/LearnMorePage'
import { ChatPage } from './chat/pages/ChatPage'
import { AboutPage, BlogPage, ContactPage } from './content/pages/ContentPlaceholderPage'
import { StorePage } from './store/pages/StorePage'
import { CartPage } from './store/pages/CartPage'
import { OrderPage } from './store/pages/OrderPage'
import { MyAccountPage } from './account/pages/MyAccountPage'
import { RequireAdmin } from './admin/RequireAdmin'
import { AdminLayout } from './admin/components/AdminLayout'
import { AdminSurveysPage } from './admin/pages/AdminSurveysPage'
import { AdminSurveyEditorPage } from './admin/pages/AdminSurveyEditorPage'
import { AdminProductsPage } from './admin/pages/AdminProductsPage'
import { AdminOrdersPage } from './admin/pages/AdminOrdersPage'
import { AdminUsersPage } from './admin/pages/AdminUsersPage'
import { ProtectedLayout } from './components/ui/ProtectedLayout'
import { NotFoundPage } from './components/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <RedirectIfAuthed>
            <LoginPage />
          </RedirectIfAuthed>
        }
      />
      <Route
        path="/signup"
        element={
          <RedirectIfAuthed>
            <SignupPage />
          </RedirectIfAuthed>
        }
      />
      <Route
        path="/onboarding"
        element={
          <RequireAuth>
            <OnboardingPage />
          </RequireAuth>
        }
      />

      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/themes/:slug" element={<ThemeDetailPage />} />
        <Route path="/themes/:slug/survey" element={<SurveyTakePage />} />
        <Route path="/themes/:slug/learn-more" element={<LearnMorePage />} />
        <Route path="/chat" element={<ChatPage />} />

        <Route path="/about" element={<AboutPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/blog" element={<BlogPage />} />

        {/* The catalog API allows anonymous reads; the SPA keeps the shop inside the
            authenticated shell for now, so the nav is the same everywhere. */}
        <Route path="/store" element={<StorePage />} />
        <Route path="/store/cart" element={<CartPage />} />
        <Route path="/store/order" element={<OrderPage />} />

        <Route path="/account" element={<MyAccountPage />} />

        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminLayout />
            </RequireAdmin>
          }
        >
          <Route index element={<Navigate to="/admin/surveys" replace />} />
          <Route path="surveys" element={<AdminSurveysPage />} />
          <Route path="surveys/:surveyId/versions/:versionId" element={<AdminSurveyEditorPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="users" element={<AdminUsersPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
