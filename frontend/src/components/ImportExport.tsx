import { useRef, useState } from 'react';
import { dataAPI } from '../api/api';
import './ImportExport.css';

interface Props {
  onImportComplete: () => void;   // called after import so the parent refreshes data
}

export default function ImportExport({ onImportComplete }: Props) {
  const [status, setStatus] = useState<{ type: 'success' | 'error' | 'info'; msg: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [showPanel, setShowPanel] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const clearStatus = () => setTimeout(() => setStatus(null), 5000);

  // ── EXPORT ──────────────────────────────────────────────────────────────
  const handleExport = async () => {
    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Preparing your backup…' });
    try {
      const snapshot = await dataAPI.export();

      // Build a downloadable .json file
      const blob = new Blob([JSON.stringify(snapshot, null, 2)], { type: 'application/json' });
      const url  = URL.createObjectURL(blob);
      const link = document.createElement('a');

      const now  = new Date();
      const ts   = now.toISOString().replace(/[:.]/g, '-').slice(0, 19);
      link.href  = url;
      link.download = `taskmanager-backup-${ts}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      setStatus({ type: 'success', msg: '✅ Backup downloaded successfully!' });
    } catch (err) {
      setStatus({ type: 'error', msg: '❌ Export failed. Please try again.' });
      console.error('Export error:', err);
    } finally {
      setLoading(false);
      clearStatus();
    }
  };

  // ── IMPORT ──────────────────────────────────────────────────────────────
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.name.endsWith('.json')) {
      setStatus({ type: 'error', msg: '❌ Please select a valid .json backup file.' });
      clearStatus();
      return;
    }

    const confirmed = window.confirm(
      `⚠️ IMPORT WARNING\n\n` +
      `This will REPLACE all current users and tasks with the data from:\n"${file.name}"\n\n` +
      `This action cannot be undone. Make sure you have a backup first.\n\n` +
      `Continue with import?`
    );

    if (!confirmed) {
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Importing your data…' });

    try {
      const text     = await file.text();
      const snapshot = JSON.parse(text);

      if (!snapshot.users || !snapshot.tasks) {
        throw new Error('Invalid backup file: missing users or tasks fields.');
      }

      const result = await dataAPI.import(snapshot);
      setStatus({ type: 'success', msg: result });
      onImportComplete();   // reload data in parent
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Unknown error';
      setStatus({ type: 'error', msg: `❌ Import failed: ${message}` });
      console.error('Import error:', err);
    } finally {
      setLoading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      clearStatus();
    }
  };

  return (
    <div className="import-export-wrapper">
      {/* Toggle button */}
      <button
        className="ie-toggle-btn"
        onClick={() => setShowPanel(p => !p)}
        title="Import / Export Data"
      >
        💾 Backup
      </button>

      {/* Slide-down panel */}
      {showPanel && (
        <div className="ie-panel">
          <div className="ie-panel-header">
            <h3>📦 Data Backup &amp; Restore</h3>
            <button className="ie-close-btn" onClick={() => setShowPanel(false)}>✕</button>
          </div>

          <p className="ie-description">
            Export a full snapshot of all users &amp; tasks as a <strong>.json</strong> file.
            Import that file later to restore the app to exactly that state.
          </p>

          {/* Status banner */}
          {status && (
            <div className={`ie-status ie-status--${status.type}`}>
              {status.msg}
            </div>
          )}

          <div className="ie-actions">
            {/* EXPORT */}
            <div className="ie-card ie-card--export">
              <div className="ie-card-icon">📤</div>
              <div className="ie-card-body">
                <h4>Export</h4>
                <p>Download a JSON backup of all users and tasks.</p>
              </div>
              <button
                className="ie-btn ie-btn--export"
                onClick={handleExport}
                disabled={loading}
              >
                {loading ? '⏳ Working…' : '⬇ Export JSON'}
              </button>
            </div>

            {/* IMPORT */}
            <div className="ie-card ie-card--import">
              <div className="ie-card-icon">📥</div>
              <div className="ie-card-body">
                <h4>Import</h4>
                <p>Restore app from a previously exported JSON backup.</p>
              </div>
              <button
                className="ie-btn ie-btn--import"
                onClick={() => fileInputRef.current?.click()}
                disabled={loading}
              >
                {loading ? '⏳ Working…' : '⬆ Import JSON'}
              </button>
              {/* Hidden file input */}
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,application/json"
                style={{ display: 'none' }}
                onChange={handleFileChange}
              />
            </div>
          </div>

          <p className="ie-warning">
            ⚠️ Import will <strong>replace</strong> all existing data. Always export first.
          </p>
        </div>
      )}
    </div>
  );
}

