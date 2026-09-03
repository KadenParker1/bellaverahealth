import { Route, Routes } from 'react-router-dom'
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
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
