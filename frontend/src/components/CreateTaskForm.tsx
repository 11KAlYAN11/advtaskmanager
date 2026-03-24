import { useState } from 'react';
import type { Task, TaskStatus } from '../types/types';
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    onCreateTask({ title: title.trim(), description: description.trim(), status });
    setTitle('');
    setDescription('');
    setStatus('TODO');
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

        {/* ── Status pill selector — replaces plain <select> ──────────────── */}
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

        <button type="submit">✚ Create Task</button>
      </form>
    </div>
  );
}
