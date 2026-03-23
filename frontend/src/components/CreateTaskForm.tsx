import { useState } from 'react';
import type { Task } from '../types/types';
import './CreateTaskForm.css';

interface Props {
  onCreateTask: (task: Omit<Task, 'id'>) => void;
}

export default function CreateTaskForm({ onCreateTask }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE'>('TODO');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onCreateTask({ title, description, status });
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
          placeholder="Task Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
        />
        <select value={status} onChange={(e) => setStatus(e.target.value as any)}>
          <option value="TODO">📝 TODO</option>
          <option value="IN_PROGRESS">🔄 IN PROGRESS</option>
          <option value="REVIEW">👁️ REVIEW</option>
          <option value="DONE">✅ DONE</option>
        </select>
        <button type="submit">Create Task</button>
      </form>
    </div>
  );
}

