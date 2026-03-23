import { useRef } from 'react';
import type { Task, User } from '../types/types';
import './TaskCard.css';

interface Props {
  task: Task;
  users: User[];
  isAdmin: boolean;
  onAssignTask: (taskId: number, userId: number) => void;
  onDeleteTask: (id: number) => void;
}

export default function TaskCard({ task, users, isAdmin, onAssignTask, onDeleteTask }: Props) {
  const cardRef = useRef<HTMLDivElement>(null);

  // ── Drag handlers ─────────────────────────────────────────────────────────
  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.setData('taskId', String(task.id));
    e.dataTransfer.effectAllowed = 'move';
    // ⚠️ CRITICAL: defer DOM mutation — setting state during dragstart
    //    causes React to re-render which cancels the drag in Chrome/Edge
    setTimeout(() => cardRef.current?.classList.add('dragging'), 0);
  };

  const handleDragEnd = () => {
    cardRef.current?.classList.remove('dragging');
  };

  // ── Assign handler ────────────────────────────────────────────────────────
  const handleAssign = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const userId = parseInt(e.target.value);
    if (userId && task.id) onAssignTask(task.id, userId);
  };

  return (
    <div
      ref={cardRef}
      className="task-card"
      draggable
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      {/* Header */}
      <div className="task-header">
        <span className="drag-handle" title="Drag to move column">⠿</span>
        <h4>{task.title}</h4>
        {isAdmin && (
          <button
            className="delete-btn-small"
            onClick={() => task.id && onDeleteTask(task.id)}
          >
            🗑️
          </button>
        )}
      </div>

      <p className="description">{task.description}</p>

      {/* Assign to user — draggable={false} stops it interfering with card drag */}
      <div className="assigned-to">
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
  );
}
