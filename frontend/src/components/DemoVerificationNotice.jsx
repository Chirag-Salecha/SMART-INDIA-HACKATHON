import './DemoVerificationNotice.css'

/**
 * Mandatory, clearly visible disclaimer wherever government-portal-style
 * verification data is shown. This platform never connects to a real
 * GST/PAN/Udyam/EPFO/ESIC system - every such check is a deterministic
 * mock adapter for demo purposes. This banner must not be removed or
 * softened per the project's compliance requirements.
 */
export default function DemoVerificationNotice() {
  return (
    <div className="demo-notice">
      <span className="demo-notice-badge">DEMO / SIMULATED</span>
      <span className="demo-notice-text">
        Government Portal Verification: this build does not connect to any real
        GST, PAN, Udyam, EPFO or ESIC system. All portal checks shown here are
        deterministic mock results for demonstration purposes only.
      </span>
    </div>
  )
}
