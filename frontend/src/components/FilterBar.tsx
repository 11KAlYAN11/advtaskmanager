import { useState, useEffect, useRef } from 'react';
import type { User, TaskStatus, TaskPriority, TaskFilterParams } from '../types/types';
import './FilterBar.css';

interface Props {
  users: User[];
  onFilterChange: (params: TaskFilterParams) => void;
}

const STATUS_OPTIONS: { value: TaskStatus; label: string; emoji: string }[] = [
  { value: 'TODO',        label: 'TODO',        emoji: '📝' },
  { value: 'IN_PROGRESS', label: 'IN PROGRESS', emoji: '🔄' },
  { value: 'REVIEW',      label: 'REVIEW',      emoji: '👁️' },
  { value: 'DONE',        label: 'DONE',        emoji: '✅' },
];

const PRIORITY_OPTIONS: { value: TaskPriority; label: string; emoji: string }[] = [
  { value: 'LOW',      label: 'LOW',      emoji: '🟢' },
  { value: 'MEDIUM',   label: 'MEDIUM',   emoji: '🔵' },
  { value: 'HIGH',     label: 'HIGH',     emoji: '🟠' },
  { value: 'CRITICAL', label: 'CRITICAL', emoji: '🔴' },
];

export default function FilterBar({ users, onFilterChange }: Props) {
  const [q,          setQ]          = useState('');
  const [status,     setStatus]     = useState<TaskStatus | ''>('');
  const [priority,   setPriority]   = useState<TaskPriority | ''>('');
  const [assignedTo, setAssignedTo] = useState<number | ''>('');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef  = useRef(false);  // ← skip the first-mount fire

  // Only notify parent when the user actually changes a filter value
  useEffect(() => {
    if (!mountedRef.current) {
      mountedRef.current = true;
      return;   // skip initial mount — no API call on load
    }
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const params: TaskFilterParams = {};
      if (status)     params.status     = status;
      if (priority)   params.priority   = priority;
      if (assignedTo) params.assignedTo = assignedTo as number;
      if (q.trim())   params.q          = q.trim();
      onFilterChange(params);
    }, 350);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [q, status, priority, assignedTo]); // eslint-disable-line react-hooks/exhaustive-deps

  const hasFilters = q || status || priority || assignedTo;

  const clearAll = () => {
    setQ('');
    setStatus('');
    setPriority('');
    setAssignedTo('');
  };

  return (
    <div className="filter-bar">
      {/* ── Search ──────────────────────────────────────────────────────── */}
      <div className="filter-search-wrapper">
        <span className="filter-search-icon">🔍</span>
        <input
          type="text"
          className="filter-search"
          placeholder="Search tasks…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        {q && (
          <button className="filter-clear-x" onClick={() => setQ('')} title="Clear search">✕</button>
        )}
      </div>

      {/* ── Status filter ────────────────────────────────────────────────── */}
      <select
        className="filter-select"
        value={status}
        onChange={(e) => setStatus(e.target.value as TaskStatus | '')}
      >
        <option value="">📋 All Statuses</option>
        {STATUS_OPTIONS.map(({ value, label, emoji }) => (
          <option key={value} value={value}>{emoji} {label}</option>
        ))}
      </select>

      {/* ── Priority filter ──────────────────────────────────────────────── */}
      <select
        className="filter-select"
        value={priority}
        onChange={(e) => setPriority(e.target.value as TaskPriority | '')}
      >
        <option value="">⚡ All Priorities</option>
        {PRIORITY_OPTIONS.map(({ value, label, emoji }) => (
          <option key={value} value={value}>{emoji} {label}</option>
        ))}
      </select>

      {/* ── Assignee filter ──────────────────────────────────────────────── */}
      <select
        className="filter-select"
        value={assignedTo}
        onChange={(e) => setAssignedTo(e.target.value ? Number(e.target.value) : '')}
      >
        <option value="">👤 All Assignees</option>
        {users.map((u) => (
          <option key={u.id} value={u.id}>{u.name}</option>
        ))}
      </select>

      {/* ── Clear all ───────────────────────────────────────────────────── */}
      {hasFilters && (
        <button className="filter-clear-btn" onClick={clearAll} title="Clear all filters">
          ✕ Clear
        </button>
      )}
    </div>
  );
}

