export interface User {
  id?: number;
  name: string;
  email: string;
  password?: string;
  role: 'ADMIN' | 'USER';
  tasks?: Task[];
}

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Task {
  id?: number;
  title: string;
  description: string;
  status: 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
  priority?: TaskPriority;
  dueDate?: string;          // ISO date string: "2026-04-15"
  assignedTo?: User;
  createdAt?: string;
  updatedAt?: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type UserRole = 'ADMIN' | 'USER';

// ── Auth Types ────────────────────────────────────────────────────────────────
export interface AuthUser {
  name: string;
  email: string;
  role: 'ADMIN' | 'USER';
}

export interface AuthResponse {
  token: string;
  name: string;
  email: string;
  role: string;
}

// ── Filter Params ─────────────────────────────────────────────────────────────
export interface TaskFilterParams {
  status?: TaskStatus;
  priority?: TaskPriority;
  assignedTo?: number;
  q?: string;
}



