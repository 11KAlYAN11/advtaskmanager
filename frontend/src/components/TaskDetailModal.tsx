import { useEffect } from 'react';
import type { Task, TaskPriority, TaskStatus } from '../types/types';
import './TaskDetailModal.css';

interface Props {
  task: Task | null;
  onClose: () => void;
}

const STATUS_COLORS: Record<TaskStatus, string> = {
  TODO:        '#6c757d',
  IN_PROGRESS: '#0d6efd',
  REVIEW:      '#e67e00',
  DONE:        '#198754',
};
const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO:        '📝 TODO',
  IN_PROGRESS: '🔄 In Progress',
  REVIEW:      '👁️ Review',
  DONE:        '✅ Done',
};
const PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW:      '#198754',
  MEDIUM:   '#0d6efd',
  HIGH:     '#e67e00',
  CRITICAL: '#dc3545',
};
const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW:      '🟢 Low',
  MEDIUM:   '🔵 Medium',
  HIGH:     '🟠 High',
  CRITICAL: '🔴 Critical',
};

function formatDate(iso?: string) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function TaskDetailModal({ task, onClose }: Props) {
  // Close on Escape key
  useEffect(() => {
    if (!task) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [task, onClose]);

  // Prevent body scroll when open
  useEffect(() => {
    if (task) document.body.style.overflow = 'hidden';
    else      document.body.style.overflow = '';
    return () => { document.body.style.overflow = ''; };
  }, [task]);

  if (!task) return null;

  const today = new Date().toISOString().split('T')[0];
  const isOverdue = task.dueDate ? task.dueDate < today && task.status !== 'DONE' : false;

  return (
    <>
      {/* ── Backdrop ─────────────────────────────────────────────────────── */}
      <div className="modal-backdrop" onClick={onClose} aria-hidden="true" />

      {/* ── Drawer ───────────────────────────────────────────────────────── */}
      <div
        className="task-detail-modal"
        role="dialog"
        aria-modal="true"
        aria-label={`Task details: ${task.title}`}
      >
        {/* ── Modal header ─────────────────────────────────────────────── */}
        <div className="modal-header">
          <h2 className="modal-title">🗂️ Task Details</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close details">✕</button>
        </div>

        <div className="modal-body">
          {/* ── Task title ─────────────────────────────────────────────── */}
          <h3 className="detail-task-title">{task.title}</h3>

          {/* ── Badges row ─────────────────────────────────────────────── */}
          <div className="detail-badges">
            {task.status && (
              <span
                className="detail-badge"
                style={{ background: STATUS_COLORS[task.status] }}
              >
                {STATUS_LABELS[task.status]}
              </span>
            )}
            {task.priority && (
              <span
                className="detail-badge"
                style={{ background: PRIORITY_COLORS[task.priority] }}
              >
                {PRIORITY_LABELS[task.priority]}
              </span>
            )}
          </div>

          {/* ── Description ─────────────────────────────────────────────── */}
          <section className="detail-section">
            <h4 className="detail-label">📄 Description</h4>
            <p className="detail-text">
              {task.description?.trim() || <em className="detail-empty">No description provided.</em>}
            </p>
          </section>

          {/* ── Due date ────────────────────────────────────────────────── */}
          <section className="detail-section">
            <h4 className="detail-label">📅 Due Date</h4>
            {task.dueDate ? (
              <p className={`detail-text${isOverdue ? ' detail-overdue' : ''}`}>
                {isOverdue ? '⚠️ Overdue — ' : ''}{task.dueDate}
              </p>
            ) : (
              <p className="detail-text detail-empty">No due date set.</p>
            )}
          </section>

          {/* ── Assignee ────────────────────────────────────────────────── */}
          <section className="detail-section">
            <h4 className="detail-label">👤 Assigned To</h4>
            {task.assignedTo ? (
              <div className="detail-assignee-chip">
                <span className="assignee-avatar">
                  {task.assignedTo.name.charAt(0).toUpperCase()}
                </span>
                <span>
                  <strong>{task.assignedTo.name}</strong>
                  <span className="assignee-role"> · {task.assignedTo.role}</span>
                </span>
              </div>
            ) : (
              <p className="detail-text detail-empty">Unassigned</p>
            )}
          </section>

          {/* ── Timestamps ──────────────────────────────────────────────── */}
          <section className="detail-section detail-timestamps">
            <div className="timestamp-row">
              <span className="timestamp-label">🕐 Created</span>
              <span className="timestamp-value">{formatDate(task.createdAt)}</span>
            </div>
            <div className="timestamp-row">
              <span className="timestamp-label">✏️ Updated</span>
              <span className="timestamp-value">{formatDate(task.updatedAt)}</span>
            </div>
            <div className="timestamp-row">
              <span className="timestamp-label">🔑 Task ID</span>
              <span className="timestamp-value">#{task.id}</span>
            </div>
          </section>
        </div>

        {/* ── Footer ──────────────────────────────────────────────────────── */}
        <div className="modal-footer">
          <button className="modal-close-footer-btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </>
  );
}

