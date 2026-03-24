import { useRef, useState } from 'react';
import { dataAPI } from '../api/api';
import './ImportExport.css';

interface Props {
  onImportComplete: () => void;
}

type Format = 'json' | 'csv';

export default function ImportExport({ onImportComplete }: Props) {
  const [status, setStatus]       = useState<{ type: 'success' | 'error' | 'info'; msg: string } | null>(null);
  const [loading, setLoading]     = useState(false);
  const [showPanel, setShowPanel] = useState(false);
  const [format, setFormat]       = useState<Format>('json');

  const jsonFileRef = useRef<HTMLInputElement>(null);
  const csvFileRef  = useRef<HTMLInputElement>(null);

  const clearStatus = () => setTimeout(() => setStatus(null), 5000);

  // ── JSON EXPORT ─────────────────────────────────────────────────────────────
  const handleJsonExport = async () => {
    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Preparing your JSON backup…' });
    try {
      const snapshot = await dataAPI.export();
      const blob  = new Blob([JSON.stringify(snapshot, null, 2)], { type: 'application/json' });
      const ts    = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      triggerDownload(blob, `taskmanager-backup-${ts}.json`);
      setStatus({ type: 'success', msg: '✅ JSON backup downloaded!' });
    } catch {
      setStatus({ type: 'error', msg: '❌ JSON export failed. Please try again.' });
    } finally { setLoading(false); clearStatus(); }
  };

  // ── JSON IMPORT ─────────────────────────────────────────────────────────────
  const handleJsonFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.name.endsWith('.json')) {
      setStatus({ type: 'error', msg: '❌ Please select a valid .json backup file.' });
      clearStatus(); return;
    }
    if (!confirmImport(file.name)) { resetInput(jsonFileRef); return; }
    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Importing JSON data…' });
    try {
      const snapshot = JSON.parse(await file.text());
      if (!snapshot.users || !snapshot.tasks) throw new Error('Missing users or tasks fields.');
      const result = await dataAPI.import(snapshot);
      setStatus({ type: 'success', msg: result });
      onImportComplete();
    } catch (err: unknown) {
      setStatus({ type: 'error', msg: `❌ Import failed: ${err instanceof Error ? err.message : 'Unknown error'}` });
    } finally { setLoading(false); resetInput(jsonFileRef); clearStatus(); }
  };

  // ── CSV EXPORT ─────────────────────────────────────────────────────────────
  const handleCsvExport = async () => {
    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Preparing your CSV backup…' });
    try {
      const blob = await dataAPI.exportCsv();
      const ts   = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      triggerDownload(blob, `taskmanager-backup-${ts}.zip`);
      setStatus({ type: 'success', msg: '✅ CSV backup downloaded! (ZIP with users.csv + tasks.csv)' });
    } catch {
      setStatus({ type: 'error', msg: '❌ CSV export failed. Please try again.' });
    } finally { setLoading(false); clearStatus(); }
  };

  // ── CSV IMPORT ─────────────────────────────────────────────────────────────
  const handleCsvFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.name.endsWith('.zip')) {
      setStatus({ type: 'error', msg: '❌ Please select a valid .zip backup file.' });
      clearStatus(); return;
    }
    if (!confirmImport(file.name)) { resetInput(csvFileRef); return; }
    setLoading(true);
    setStatus({ type: 'info', msg: '⏳ Importing CSV data…' });
    try {
      const result = await dataAPI.importCsv(file);
      setStatus({ type: 'success', msg: result });
      onImportComplete();
    } catch (err: unknown) {
      setStatus({ type: 'error', msg: `❌ CSV import failed: ${err instanceof Error ? err.message : 'Unknown error'}` });
    } finally { setLoading(false); resetInput(csvFileRef); clearStatus(); }
  };

  // ── helpers ─────────────────────────────────────────────────────────────────
  const triggerDownload = (blob: Blob, filename: string) => {
    const url  = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.download = filename;
    document.body.appendChild(link); link.click();
    document.body.removeChild(link); URL.revokeObjectURL(url);
  };

  const confirmImport = (filename: string) =>
    window.confirm(
      `⚠️ IMPORT WARNING\n\nThis will REPLACE all current users and tasks with data from:\n"${filename}"\n\nThis cannot be undone. Make sure you have a backup first.\n\nContinue?`
    );

  const resetInput = (ref: React.RefObject<HTMLInputElement | null>) => {
    if (ref.current) ref.current.value = '';
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

      {showPanel && (
        <div className="ie-panel">
          <div className="ie-panel-header">
            <h3>📦 Data Backup &amp; Restore</h3>
            <button className="ie-close-btn" onClick={() => setShowPanel(false)}>✕</button>
          </div>

          {/* Format tabs */}
          <div className="ie-tabs">
            <button
              className={`ie-tab ${format === 'json' ? 'ie-tab--active' : ''}`}
              onClick={() => setFormat('json')}
            >
              { } JSON
            </button>
            <button
              className={`ie-tab ${format === 'csv' ? 'ie-tab--active' : ''}`}
              onClick={() => setFormat('csv')}
            >
              📊 CSV
            </button>
          </div>

          <p className="ie-description">
            {format === 'json'
              ? <>Export a full <strong>.json</strong> snapshot — import it later to restore exactly that state.</>
              : <>Export a <strong>.zip</strong> with <code>users.csv</code> + <code>tasks.csv</code> — import the same ZIP to restore.</>
            }
          </p>

          {/* Status banner */}
          {status && (
            <div className={`ie-status ie-status--${status.type}`}>{status.msg}</div>
          )}

          {/* JSON cards */}
          {format === 'json' && (
            <div className="ie-actions">
              <div className="ie-card ie-card--export">
                <div className="ie-card-icon">📤</div>
                <div className="ie-card-body">
                  <h4>Export JSON</h4>
                  <p>Download a single <strong>.json</strong> backup of all users and tasks.</p>
                </div>
                <button className="ie-btn ie-btn--export" onClick={handleJsonExport} disabled={loading}>
                  {loading ? '⏳ Working…' : '⬇ Export .json'}
                </button>
              </div>

              <div className="ie-card ie-card--import">
                <div className="ie-card-icon">📥</div>
                <div className="ie-card-body">
                  <h4>Import JSON</h4>
                  <p>Restore the app from a previously exported <strong>.json</strong> backup.</p>
                </div>
                <button className="ie-btn ie-btn--import" onClick={() => jsonFileRef.current?.click()} disabled={loading}>
                  {loading ? '⏳ Working…' : '⬆ Import .json'}
                </button>
                <input ref={jsonFileRef} type="file" accept=".json,application/json"
                  style={{ display: 'none' }} onChange={handleJsonFileChange} />
              </div>
            </div>
          )}

          {/* CSV cards */}
          {format === 'csv' && (
            <div className="ie-actions">
              <div className="ie-card ie-card--export">
                <div className="ie-card-icon">📤</div>
                <div className="ie-card-body">
                  <h4>Export CSV</h4>
                  <p>Download a <strong>.zip</strong> containing <code>users.csv</code> + <code>tasks.csv</code>.</p>
                </div>
                <button className="ie-btn ie-btn--export" onClick={handleCsvExport} disabled={loading}>
                  {loading ? '⏳ Working…' : '⬇ Export .zip'}
                </button>
              </div>

              <div className="ie-card ie-card--import">
                <div className="ie-card-icon">📥</div>
                <div className="ie-card-body">
                  <h4>Import CSV</h4>
                  <p>Restore the app from a previously exported <strong>.zip</strong> backup.</p>
                </div>
                <button className="ie-btn ie-btn--import" onClick={() => csvFileRef.current?.click()} disabled={loading}>
                  {loading ? '⏳ Working…' : '⬆ Import .zip'}
                </button>
                <input ref={csvFileRef} type="file" accept=".zip,application/zip"
                  style={{ display: 'none' }} onChange={handleCsvFileChange} />
              </div>
            </div>
          )}

          <p className="ie-warning">
            ⚠️ Import will <strong>replace</strong> all existing data. Always export first.
          </p>
        </div>
      )}
    </div>
  );
}
