import { useState, useEffect } from 'react';
import type { Task, TaskPriority, TaskStatus } from '../types/types';
import { taskAPI } from '../api/api';
import './TaskDetailModal.css';

interface Props {
  task: Task | null;
  onClose: () => void;
  onTaskUpdated?: (updatedTask: Task) => void;
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

export default function TaskDetailModal({ task, onClose, onTaskUpdated }: Props) {
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [formData, setFormData] = useState<Partial<Task>>({});

  // Initialize form data when task changes
  useEffect(() => {
    if (task) {
      setFormData({
        title: task.title,
        description: task.description,
        priority: task.priority,
        dueDate: task.dueDate,
        status: task.status,
      });
    }
  }, [task]);

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

  const handleFieldChange = (field: keyof Task, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSaveChanges = async () => {
    if (!task?.id) return;
    
    setIsSaving(true);
    try {
      const updatedTask = await taskAPI.update(task.id, formData);
      if (onTaskUpdated) {
        onTaskUpdated(updatedTask);
      }
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update task:', error);
      alert('Failed to save task changes. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancel = () => {
    // Reset form data to original task values
    if (task) {
      setFormData({
        title: task.title,
        description: task.description,
        priority: task.priority,
        dueDate: task.dueDate,
        status: task.status,
      });
    }
    setIsEditing(false);
  };

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
          <div className="modal-header-buttons">
            {!isEditing ? (
              <button 
                className="modal-edit-btn" 
                onClick={() => setIsEditing(true)}
                aria-label="Edit task"
                title="Edit task details"
              >
                ✏️ Edit
              </button>
            ) : null}
            <button className="modal-close-btn" onClick={onClose} aria-label="Close details">✕</button>
          </div>
        </div>

        <div className="modal-body">
          {isEditing ? (
            // ── EDIT MODE ───────────────────────────────────────────
            <>
              {/* ── Title Field ─────────────────────────────────────── */}
              <div className="edit-field">
                <label className="edit-label">Task Name</label>
                <input
                  type="text"
                  className="edit-input"
                  value={formData.title || ''}
                  onChange={(e) => handleFieldChange('title', e.target.value)}
                  placeholder="Enter task name"
                />
              </div>

              {/* ── Description Field ───────────────────────────────── */}
              <div className="edit-field">
                <label className="edit-label">Description</label>
                <textarea
                  className="edit-textarea"
                  value={formData.description || ''}
                  onChange={(e) => handleFieldChange('description', e.target.value)}
                  placeholder="Enter task description"
                  rows={4}
                />
              </div>

              {/* ── Priority Field ──────────────────────────────────── */}
              <div className="edit-field">
                <label className="edit-label">Priority</label>
                <select
                  className="edit-select"
                  value={formData.priority || 'MEDIUM'}
                  onChange={(e) => handleFieldChange('priority', e.target.value)}
                >
                  <option value="LOW">🟢 Low</option>
                  <option value="MEDIUM">🔵 Medium</option>
                  <option value="HIGH">🟠 High</option>
                  <option value="CRITICAL">🔴 Critical</option>
                </select>
              </div>

              {/* ── Due Date Field ──────────────────────────────────── */}
              <div className="edit-field">
                <label className="edit-label">Due Date</label>
                <input
                  type="date"
                  className="edit-input"
                  value={formData.dueDate || ''}
                  onChange={(e) => handleFieldChange('dueDate', e.target.value)}
                />
              </div>

              {/* ── Status Field ────────────────────────────────────── */}
              <div className="edit-field">
                <label className="edit-label">Status</label>
                <select
                  className="edit-select"
                  value={formData.status || 'TODO'}
                  onChange={(e) => handleFieldChange('status', e.target.value)}
                >
                  <option value="TODO">📝 TODO</option>
                  <option value="IN_PROGRESS">🔄 In Progress</option>
                  <option value="REVIEW">👁️ Review</option>
                  <option value="DONE">✅ Done</option>
                </select>
              </div>

              {/* ── Edit Action Buttons ─────────────────────────────── */}
              <div className="edit-actions">
                <button
                  className="btn-save"
                  onClick={handleSaveChanges}
                  disabled={isSaving}
                >
                  {isSaving ? '💾 Saving...' : '💾 Save Changes'}
                </button>
                <button
                  className="btn-cancel"
                  onClick={handleCancel}
                  disabled={isSaving}
                >
                  ✕ Cancel
                </button>
              </div>
            </>
          ) : (
            // ── VIEW MODE ────────────────────────────────────────────
            <>
              {/* ── Task title ─────────────────────────────────────── */}
              <h3 className="detail-task-title">{task.title}</h3>

              {/* ── Badges row ─────────────────────────────────────── */}
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

              {/* ── Description ─────────────────────────────────────– */}
              <section className="detail-section">
                <h4 className="detail-label">📄 Description</h4>
                <p className="detail-text">
                  {task.description?.trim() || <em className="detail-empty">No description provided.</em>}
                </p>
              </section>

              {/* ── Due date ────────────────────────────────────────– */}
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

              {/* ── Assignee ────────────────────────────────────────– */}
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

              {/* ── Timestamps ──────────────────────────────────────– */}
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
            </>
          )}
        </div>

        {/* ── Footer ──────────────────────────────────────────────────────– */}
        <div className="modal-footer">
          <button className="modal-close-footer-btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </>
  );
}

