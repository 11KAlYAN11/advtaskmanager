import type { AuthResponse } from '../types/types';
import type { TaskFilterParams } from '../types/types';

// API Base URL — reads from env var in production, falls back to localhost for dev
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// ── Auth Headers (exported for reuse in aiApi.ts) ─────────────────────────────
export const authHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
};

// ── Handle 401 globally (exported for reuse) ──────────────────────────────────
export const handleResponse = async (res: Response) => {
  if (res.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.reload();
    throw new Error('Session expired — please log in again.');
  }
  return res;
};

// ── Auth API ──────────────────────────────────────────────────────────────────
export const authAPI = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!res.ok) {
      // Try to surface a real server message
      const body = await res.text().catch(() => res.statusText);
      throw new Error(`HTTP ${res.status}: ${body}`);
    }

    return res.json() as Promise<AuthResponse>;
  },
};

// ── User API ──────────────────────────────────────────────────────────────────
export const userAPI = {
  getAll: () =>
    fetch(`${API_BASE_URL}/users`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json()),

  getById: (id: number) =>
    fetch(`${API_BASE_URL}/users/${id}`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json()),

  create: (user: { name: string; email: string; password?: string; role: string }) =>
    fetch(`${API_BASE_URL}/users`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(user),
    }).then(handleResponse).then(res => res.json()),

  delete: (id: number) =>
    fetch(`${API_BASE_URL}/users/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.text()),

  deleteAll: () =>
    fetch(`${API_BASE_URL}/users`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.text()),
};

// ── Task API ──────────────────────────────────────────────────────────────────
export const taskAPI = {
  getAll: (filters?: TaskFilterParams) => {
    const params = new URLSearchParams();
    if (filters?.status)     params.set('status',     filters.status);
    if (filters?.priority)   params.set('priority',   filters.priority);
    if (filters?.assignedTo) params.set('assignedTo', String(filters.assignedTo));
    if (filters?.q)          params.set('q',          filters.q);
    const qs = params.toString();
    return fetch(`${API_BASE_URL}/tasks${qs ? `?${qs}` : ''}`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json());
  },

  getById: (id: number) =>
    fetch(`${API_BASE_URL}/tasks/${id}`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json()),

  getByUser: (userId: number) =>
    fetch(`${API_BASE_URL}/tasks/user/${userId}`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json()),

  getByStatus: (status: string) =>
    fetch(`${API_BASE_URL}/tasks/status?status=${status}`, { headers: authHeaders() })
      .then(handleResponse).then(res => res.json()),

  create: (task: { title: string; description: string; status: string; priority?: string; dueDate?: string }) =>
    fetch(`${API_BASE_URL}/tasks`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(task),
    }).then(handleResponse).then(res => res.json()),

  assignToUser: (taskId: number, userId: number) =>
    fetch(`${API_BASE_URL}/tasks/${taskId}/assign/${userId}`, {
      method: 'PUT',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.json()),

  updateStatus: (taskId: number, status: string) =>
    fetch(`${API_BASE_URL}/tasks/${taskId}/status?status=${status}`, {
      method: 'PUT',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.json()),

  delete: (id: number) =>
    fetch(`${API_BASE_URL}/tasks/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.text()),

  deleteAll: () =>
    fetch(`${API_BASE_URL}/tasks`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(handleResponse).then(res => res.text()),
};

// ── Import / Export API ────────────────────────────────────────────────────
export const dataAPI = {
  // ── JSON ──────────────────────────────────────────────────────────────────

  // Download full snapshot as a parsed JSON object
  export: (): Promise<object> =>
    fetch(`${API_BASE_URL}/data/export`, { headers: authHeaders() })
      .then(handleResponse)
      .then(res => res.json()),

  // Upload a JSON snapshot and restore all data
  import: (snapshot: object): Promise<string> =>
    fetch(`${API_BASE_URL}/data/import`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(snapshot),
    }).then(handleResponse).then(res => res.text()),

  // ── CSV ───────────────────────────────────────────────────────────────────

  // Download all data as a ZIP containing users.csv + tasks.csv
  exportCsv: (): Promise<Blob> =>
    fetch(`${API_BASE_URL}/data/export/csv`, { headers: authHeaders() })
      .then(handleResponse)
      .then(res => res.blob()),

  // Upload a ZIP file (users.csv + tasks.csv) to restore all data
  importCsv: (file: File): Promise<string> => {
    const form = new FormData();
    form.append('file', file);
    const token = localStorage.getItem('token');
    return fetch(`${API_BASE_URL}/data/import/csv`, {
      method: 'POST',
      // Don't set Content-Type — browser sets multipart boundary automatically
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    }).then(handleResponse).then(res => res.text());
  },
};

