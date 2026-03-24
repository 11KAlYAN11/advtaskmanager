import { useRef, useState } from 'react';
import type { Task, User, TaskStatus } from '../types/types';
import './TaskCard.css';

interface Props {
  task: Task;
  users: User[];
  isAdmin: boolean;
  onAssignTask: (taskId: number, userId: number) => void;
  onUpdateStatus: (taskId: number, status: TaskStatus) => void;
  onDeleteTask: (id: number) => void;
}

const ALL_STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO:        '📝 TODO',
  IN_PROGRESS: '🔄 IN PROGRESS',
  REVIEW:      '👁️ REVIEW',
  DONE:        '✅ DONE',
};
const STATUS_COLORS: Record<TaskStatus, string> = {
  TODO:        '#6c757d',
  IN_PROGRESS: '#0d6efd',
  REVIEW:      '#e67e00',
  DONE:        '#198754',
};

export default function TaskCard({ task, users, isAdmin, onAssignTask, onUpdateStatus, onDeleteTask }: Props) {
  const cardRef = useRef<HTMLDivElement>(null);
  const [showMoveMenu, setShowMoveMenu] = useState(false);

  // ── Desktop drag handlers ─────────────────────────────────────────────────
  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.setData('taskId', String(task.id));
    e.dataTransfer.effectAllowed = 'move';
    setTimeout(() => cardRef.current?.classList.add('dragging'), 0);
  };
  const handleDragEnd = () => cardRef.current?.classList.remove('dragging');

  // ── Assign handler ────────────────────────────────────────────────────────
  const handleAssign = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const userId = parseInt(e.target.value);
    if (userId && task.id) onAssignTask(task.id, userId);
  };

  // ── Mobile move handler ───────────────────────────────────────────────────
  const handleMoveToStatus = (status: TaskStatus) => {
    if (task.id) onUpdateStatus(task.id, status);
    setShowMoveMenu(false);
  };

  const otherStatuses = ALL_STATUSES.filter((s) => s !== task.status);

  return (
    <div
      ref={cardRef}
      className="task-card"
      draggable
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      {/* ── Header ──────────────────────────────────────────────────── */}
      <div className="task-header">
        <span className="drag-handle" title="Drag to move">⠿</span>
        <h4 className="task-title">{task.title}</h4>
        {isAdmin && (
          <button className="delete-btn-small" onClick={() => task.id && onDeleteTask(task.id)} title="Delete task">
            🗑️
          </button>
        )}
      </div>

      {task.description && <p className="description">{task.description}</p>}

      {/* ── Assign dropdown ─────────────────────────────────────────── */}
      <div className="task-meta">
        <div className="assign-row">
          <label>👤 Assign:</label>
          <select
            draggable={false}
            value={task.assignedTo?.id || ''}
            onChange={handleAssign}
            onDragStart={(e) => e.stopPropagation()}
            onClick={(e) => e.stopPropagation()}
          >
            <option value="">Unassigned</option>
            {users.map((user) => (
              <option key={user.id} value={user.id}>
                {user.name} ({user.role})
              </option>
            ))}
          </select>
        </div>

        {task.assignedTo && (
          <div className="assigned-user">✅ {task.assignedTo.name}</div>
        )}
      </div>

      {/* ── Mobile "Move to" button ──────────────────────────────────── */}
      <div className="move-btn-wrapper">
        <button
          className="move-btn"
          onClick={(e) => { e.stopPropagation(); setShowMoveMenu((m) => !m); }}
        >
          ⟶ Move to…
        </button>
        {showMoveMenu && (
          <div className="move-menu">
            {otherStatuses.map((status) => (
              <button
                key={status}
                className="move-option"
                style={{ borderLeft: `4px solid ${STATUS_COLORS[status]}` }}
                onClick={() => handleMoveToStatus(status)}
              >
                {STATUS_LABELS[status]}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
