import './DataTable.css'

/**
 * Generic table for lists of records (tenders, bidders, documents).
 * `columns`: [{ key, header, render?(row) }]
 * `rows`: array of records, each needs a unique `id`
 */
export default function DataTable({ columns, rows, onRowClick, emptyMessage = 'No records found.' }) {
  if (!rows || rows.length === 0) {
    return <div className="data-table-empty">{emptyMessage}</div>
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          {columns.map((col) => (
            <th key={col.key}>{col.header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr
            key={row.id}
            className={onRowClick ? 'data-table-row--clickable' : ''}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
          >
            {columns.map((col) => (
              <td key={col.key}>{col.render ? col.render(row) : row[col.key]}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
