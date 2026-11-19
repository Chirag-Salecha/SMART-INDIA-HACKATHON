import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Tenders from './pages/Tenders.jsx'
import CreateTender from './pages/CreateTender.jsx'
import TenderDetails from './pages/TenderDetails.jsx'
import BidderDetails from './pages/BidderDetails.jsx'
import VerificationQueue from './pages/VerificationQueue.jsx'
import ComplianceReports from './pages/ComplianceReports.jsx'

function Protected({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />

          <Route path="/tenders" element={<Protected><Tenders /></Protected>} />
          <Route path="/tenders/new" element={<Protected><CreateTender /></Protected>} />
          <Route path="/tenders/:id" element={<Protected><TenderDetails /></Protected>} />
          <Route path="/tenders/:id/edit" element={<Protected><CreateTender /></Protected>} />

          <Route path="/bidders/:id" element={<Protected><BidderDetails /></Protected>} />

          <Route path="/verification-queue" element={<Protected><VerificationQueue /></Protected>} />
          <Route path="/compliance-reports" element={<Protected><ComplianceReports /></Protected>} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
