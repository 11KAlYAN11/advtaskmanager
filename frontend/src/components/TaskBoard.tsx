import { useState, useRef } from 'react';
import type { Task, User, TaskStatus } from '../types/types';
import TaskCard from './TaskCard';
import './TaskBoard.css';

interface Props {
  tasks: Task[];
  users: User[];
  isAdmin: boolean;
  onAssignTask: (taskId: number, userId: number) => void;
  onUpdateStatus: (taskId: number, status: TaskStatus) => void;
  onDeleteTask: (id: number) => void;
}

export default function TaskBoard({ tasks, users, isAdmin, onAssignTask, onUpdateStatus, onDeleteTask }: Props) {
  const columns: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
  const [dragOverColumn, setDragOverColumn] = useState<TaskStatus | null>(null);

  // Counter ref: tracks how many drag-enter events fired per column
  // Needed because dragLeave fires when entering a child element too
  const dragCounters = useRef<Partial<Record<TaskStatus, number>>>({});

  // ── Drag & Drop ──────────────────────────────────────────────────────────
  const handleDragEnter = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    dragCounters.current[status] = (dragCounters.current[status] ?? 0) + 1;
    setDragOverColumn(status);
  };

  const handleDragOver = (e: React.DragEvent) => {
    // Must call preventDefault on every dragOver to allow drop
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDragLeave = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    dragCounters.current[status] = Math.max((dragCounters.current[status] ?? 0) - 1, 0);
    if (dragCounters.current[status] === 0) {
      setDragOverColumn((prev) => (prev === status ? null : prev));
    }
  };

  const handleDrop = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    // Reset counter for this column
    dragCounters.current[status] = 0;
    setDragOverColumn(null);

    const taskId = parseInt(e.dataTransfer.getData('taskId'));
    if (!isNaN(taskId) && taskId > 0) {
      onUpdateStatus(taskId, status);
    }
  };

  // ── Helpers ──────────────────────────────────────────────────────────────
  const getColumnEmoji = (status: TaskStatus) => {
    switch (status) {
      case 'TODO':        return '📝';
      case 'IN_PROGRESS': return '🔄';
      case 'REVIEW':      return '👁️';
      case 'DONE':        return '✅';
    }
  };

  const getColumnColor = (status: TaskStatus) => {
    switch (status) {
      case 'TODO':        return '#6c757d';
      case 'IN_PROGRESS': return '#0d6efd';
      case 'REVIEW':      return '#e67e00';
      case 'DONE':        return '#198754';
    }
  };

  return (
    <div className="task-board">
      {columns.map((status) => {
        const columnTasks = tasks.filter((t) => t.status === status);
        const isOver = dragOverColumn === status;

        return (
          <div
            key={status}
            className={`column${isOver ? ' drag-over' : ''}`}
            onDragEnter={(e) => handleDragEnter(e, status)}
            onDragOver={handleDragOver}
            onDragLeave={(e) => handleDragLeave(e, status)}
            onDrop={(e) => handleDrop(e, status)}
          >
            <div className="column-header" style={{ backgroundColor: getColumnColor(status) }}>
              <h3>{getColumnEmoji(status)} {status.replace('_', ' ')}</h3>
              <span className="count">{columnTasks.length}</span>
            </div>

            <div className="tasks">
              {columnTasks.map((task) => (
                <TaskCard
                  key={task.id}
                  task={task}
                  users={users}
                  isAdmin={isAdmin}
                  onAssignTask={onAssignTask}
                  onDeleteTask={onDeleteTask}
                />
              ))}

              {isOver && columnTasks.length === 0 && (
                <div className="drop-hint">⬇ Drop here</div>
              )}
            </div>

            {isOver && columnTasks.length > 0 && (
              <div className="drop-hint drop-hint-bottom">⬇ Drop here</div>
            )}
          </div>
        );
      })}
    </div>
  );
}
