import './ActivityLog.css'

const STATUS_STYLES = {
  Verified: 'activity-status--verified',
  Pending: 'activity-status--pending',
  'High Risk': 'activity-status--risk',
}

export default function ActivityLog({ entries }) {
  return (
    <div className="activity-log">
      <div className="activity-log-header">
        <h2>Recent Verification Activity</h2>
        <p>Latest bidder submissions reviewed by the compliance engine</p>
      </div>

      <table className="activity-table">
        <thead>
          <tr>
            <th>Bidder</th>
            <th>Tender ID</th>
            <th>Status</th>
            <th>Reviewed</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.id}>
              <td>{entry.bidder}</td>
              <td className="activity-tender-id">{entry.tenderId}</td>
              <td>
                <span className={`activity-status ${STATUS_STYLES[entry.status]}`}>
                  {entry.status}
                </span>
              </td>
              <td className="activity-time">{entry.reviewedAt}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
