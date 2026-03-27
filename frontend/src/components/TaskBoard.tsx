import { useState, useRef, useEffect } from 'react';
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
  onTaskClick?: (task: Task) => void;
}

export default function TaskBoard({ tasks, users, isAdmin, onAssignTask, onUpdateStatus, onDeleteTask, onTaskClick }: Props) {
  const columns: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
  const [dragOverColumn, setDragOverColumn]   = useState<TaskStatus | null>(null);
  const [activeColumn,   setActiveColumn]     = useState<TaskStatus>('TODO');

  const boardRef   = useRef<HTMLDivElement>(null);
  const dragCounters = useRef<Partial<Record<TaskStatus, number>>>({});

  // ── IntersectionObserver — tracks which column is visible on mobile ──────
  useEffect(() => {
    const board = boardRef.current;
    if (!board) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const status = entry.target.getAttribute('data-status') as TaskStatus;
            if (status) setActiveColumn(status);
          }
        });
      },
      { root: board, threshold: 0.55 }
    );

    const colEls = board.querySelectorAll('[data-status]');
    colEls.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  // ── Scroll board to column when tab tapped ───────────────────────────────
  const scrollToColumn = (status: TaskStatus) => {
    setActiveColumn(status);
    const board = boardRef.current;
    if (!board) return;
    const idx   = columns.indexOf(status);
    const colEl = board.querySelectorAll('[data-status]')[idx] as HTMLElement;
    colEl?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' });
  };

  // ── Drag & Drop (desktop) ────────────────────────────────────────────────
  const handleDragEnter = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    dragCounters.current[status] = (dragCounters.current[status] ?? 0) + 1;
    setDragOverColumn(status);
  };
  const handleDragOver  = (e: React.DragEvent) => { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; };
  const handleDragLeave = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    dragCounters.current[status] = Math.max((dragCounters.current[status] ?? 0) - 1, 0);
    if (dragCounters.current[status] === 0) setDragOverColumn((p) => (p === status ? null : p));
  };
  const handleDrop = (e: React.DragEvent, status: TaskStatus) => {
    e.preventDefault();
    dragCounters.current[status] = 0;
    setDragOverColumn(null);
    const taskId = parseInt(e.dataTransfer.getData('taskId'));
    if (!isNaN(taskId) && taskId > 0) onUpdateStatus(taskId, status);
  };

  // ── Helpers ──────────────────────────────────────────────────────────────
  const getEmoji = (status: TaskStatus) => ({ TODO: '📝', IN_PROGRESS: '🔄', REVIEW: '👁️', DONE: '✅' }[status]);
  const getColor = (status: TaskStatus) => ({ TODO: '#6c757d', IN_PROGRESS: '#0d6efd', REVIEW: '#e67e00', DONE: '#198754' }[status]);

  return (
    <>
      {/* ── Mobile column tab bar ─────────────────────────────────────── */}
      <div className="mobile-col-tabs">
        {columns.map((status) => (
          <button
            key={status}
            className={`mobile-col-tab${activeColumn === status ? ' active' : ''}`}
            style={activeColumn === status ? { background: getColor(status), borderColor: getColor(status) } : {}}
            onClick={() => scrollToColumn(status)}
          >
            <span className="col-tab-emoji">{getEmoji(status)}</span>
            <span className="col-tab-label">{status.replace('_', ' ')}</span>
            <span className="col-tab-count">{tasks.filter((t) => t.status === status).length}</span>
          </button>
        ))}
      </div>

      {/* ── Swipe hint (mobile only) ──────────────────────────────────── */}
      <div className="swipe-hint">
        <span>←</span> swipe between columns <span>→</span>
      </div>

      {/* ── Board ─────────────────────────────────────────────────────── */}
      <div className="task-board" ref={boardRef}>
        {columns.map((status) => {
          const columnTasks = tasks.filter((t) => t.status === status);
          const isOver      = dragOverColumn === status;

          return (
            <div
              key={status}
              data-status={status}
              className={`column${isOver ? ' drag-over' : ''}`}
              onDragEnter={(e) => handleDragEnter(e, status)}
              onDragOver={handleDragOver}
              onDragLeave={(e) => handleDragLeave(e, status)}
              onDrop={(e) => handleDrop(e, status)}
            >
              <div className="column-header" style={{ backgroundColor: getColor(status) }}>
                <h3>{getEmoji(status)} {status.replace('_', ' ')}</h3>
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
                    onUpdateStatus={onUpdateStatus}
                    onDeleteTask={onDeleteTask}
                    onTaskClick={onTaskClick}
                  />
                ))}
                {isOver && columnTasks.length === 0 && <div className="drop-hint">⬇ Drop here</div>}
              </div>

              {isOver && columnTasks.length > 0 && <div className="drop-hint drop-hint-bottom">⬇ Drop here</div>}
            </div>
          );
        })}
      </div>
    </>
  );
}
