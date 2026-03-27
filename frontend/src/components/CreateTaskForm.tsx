import { useState } from 'react';
import type { Task, TaskStatus, TaskPriority } from '../types/types';
import './CreateTaskForm.css';

interface Props {
  onCreateTask: (task: Omit<Task, 'id'>) => void;
}

const STATUS_OPTIONS: { value: TaskStatus; emoji: string; label: string }[] = [
  { value: 'TODO',        emoji: '📝', label: 'TODO' },
  { value: 'IN_PROGRESS', emoji: '🔄', label: 'IN PROG' },
  { value: 'REVIEW',      emoji: '👁️', label: 'REVIEW' },
  { value: 'DONE',        emoji: '✅', label: 'DONE' },
];

export default function CreateTaskForm({ onCreateTask }: Props) {
  const [title,       setTitle]       = useState('');
  const [description, setDescription] = useState('');
  const [status,      setStatus]      = useState<TaskStatus>('TODO');
  const [priority,    setPriority]    = useState<TaskPriority | ''>('');   // no default
  const [dueDate,     setDueDate]     = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    onCreateTask({
      title:       title.trim(),
      description: description.trim(),
      status,
      priority:    priority || undefined,   // omit if not chosen
      dueDate:     dueDate  || undefined,   // omit if not set
    });
    setTitle('');
    setDescription('');
    setStatus('TODO');
    setPriority('');
    setDueDate('');
  };

  return (
    <div className="create-task-form">
      <h2>➕ Create New Task</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Task Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
        <textarea
          placeholder="Task Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
        />

        {/* ── Status pill selector ─────────────────────────────────────────── */}
        <div>
          <span className="status-select-label">📌 Initial Status</span>
          <div className="status-pills">
            {STATUS_OPTIONS.map(({ value, emoji, label }) => (
              <button
                key={value}
                type="button"
                className={`status-pill${status === value ? ` active-${value}` : ''}`}
                onClick={() => setStatus(value)}
              >
                <span className="pill-emoji">{emoji}</span>
                <span className="pill-label">{label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* ── Priority dropdown ────────────────────────────────────────────── */}
        <div className="form-row-inline">
          <div className="form-field">
            <span className="status-select-label">⚡ Priority</span>
            <select
              className="form-select"
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority | '')}
            >
              <option value="">— No priority —</option>
              <option value="LOW">🟢 Low</option>
              <option value="MEDIUM">🔵 Medium</option>
              <option value="HIGH">🟠 High</option>
              <option value="CRITICAL">🔴 Critical</option>
            </select>
          </div>

          {/* ── Due Date ────────────────────────────────────────────────────── */}
          <div className="form-field">
            <span className="status-select-label">📅 Due Date <span className="optional-hint">(optional)</span></span>
            <input
              type="date"
              className="form-date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              min={new Date().toISOString().split('T')[0]}
            />
          </div>
        </div>

        <button type="submit">✚ Create Task</button>
      </form>
    </div>
  );
}
