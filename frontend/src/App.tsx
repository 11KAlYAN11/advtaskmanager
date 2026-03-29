import { useState, useEffect, useRef, useCallback } from 'react';
import './App.css';
import { userAPI, taskAPI } from './api/api';
import type { User, Task, TaskStatus, TaskFilterParams } from './types/types';
import { AuthProvider, useAuth } from './context/AuthContext';
import UserList from './components/UserList';
import TaskBoard from './components/TaskBoard';
import CreateUserForm from './components/CreateUserForm';
import CreateTaskForm from './components/CreateTaskForm';
import LoginPage from './components/LoginPage';
import AIAssistant from './components/AIAssistant';
import ImportExport from './components/ImportExport';
import FilterBar from './components/FilterBar';
import TaskDetailModal from './components/TaskDetailModal';

// Derive backend root URL from the API base (strip "/api" suffix)
const BACKEND_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/api$/, '');
const SWAGGER_URL = `${BACKEND_URL}/swagger-ui/index.html`;

// ── Main app (only shown when authenticated) ──────────────────────────────────
function MainApp() {
  const { user, logout } = useAuth();
  const [users, setUsers]               = useState<User[]>([]);
  const [tasks, setTasks]               = useState<Task[]>([]);
  const [loading, setLoading]           = useState(true);   // only true on first load
  const [activeTab, setActiveTab]       = useState<'board' | 'users'>('board');
  const [dragError, setDragError]       = useState<string | null>(null);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);

  // Store filter params in a ref — updating it never triggers a re-render
  const filterParamsRef  = useRef<TaskFilterParams>({});
  const initialLoadDone  = useRef(false);

  const showError = (msg: string) => {
    setDragError(msg);
    setTimeout(() => setDragError(null), 4000);
  };

  // ── Fetch only tasks (used after task mutations) ──────────────────────────
  const refreshTasks = useCallback(async (filters?: TaskFilterParams) => {
    try {
      const tasksData = await taskAPI.getAll(filters ?? filterParamsRef.current);
      setTasks(tasksData);
    } catch (error) {
      console.error('Error refreshing tasks:', error);
    }
  }, []);

  // ── Full load — fetches users + tasks (used on mount and after user mutations) ──
  const loadData = useCallback(async (filters?: TaskFilterParams) => {
    const isFirst = !initialLoadDone.current;
    if (isFirst) setLoading(true);
    try {
      const [usersData, tasksData] = await Promise.all([
        userAPI.getAll(),
        taskAPI.getAll(filters ?? filterParamsRef.current),
      ]);
      setUsers(usersData);
      setTasks(tasksData);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      if (isFirst) {
        setLoading(false);
        initialLoadDone.current = true;
      }
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  // Filter changed by user — update ref (no re-render) then silently refresh tasks
  const handleFilterChange = useCallback((params: TaskFilterParams) => {
    filterParamsRef.current = params;
    refreshTasks(params);
  }, [refreshTasks]);

  // ── Handlers — only refresh tasks (not users) after task mutations ────────
  const handleCreateUser = async (userData: Omit<User, 'id'>) => {
    try { await userAPI.create(userData); await loadData(); }
    catch (error) { console.error('Error creating user:', error); }
  };

  const handleDeleteUser = async (id: number) => {
    if (window.confirm('Delete this user?')) {
      try { await userAPI.delete(id); await loadData(); }
      catch (error) { console.error('Error deleting user:', error); }
    }
  };

  const handleCreateTask = async (taskData: Omit<Task, 'id'>) => {
    try { await taskAPI.create(taskData); await refreshTasks(); }
    catch (error) { console.error('Error creating task:', error); }
  };

  const handleAssignTask = async (taskId: number, userId: number) => {
    try { await taskAPI.assignToUser(taskId, userId); await refreshTasks(); }
    catch (error) { console.error('Error assigning task:', error); showError('Failed to assign task.'); }
  };

  const handleUpdateStatus = async (taskId: number, status: TaskStatus) => {
    try { await taskAPI.updateStatus(taskId, status); await refreshTasks(); }
    catch (error) { console.error('Error updating status:', error); showError(`Failed to move task to ${status}.`); }
  };

  const handleDeleteTask = async (id: number) => {
    if (window.confirm('Delete this task?')) {
      try { await taskAPI.delete(id); await refreshTasks(); }
      catch (error) { console.error('Error deleting task:', error); }
    }
  };

  if (loading) {
    return <div className="loading">⏳ Loading Task Manager…</div>;
  }

  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="App">
      <header className="app-header">
        <div className="header-left">
          <h1>🎯 Advanced Task Manager</h1>
          <p>Jira-style Task Management System</p>
        </div>
        <div className="header-right">
          <div className="user-badge">
            <span className={`role-pill ${user?.role.toLowerCase()}`}>{user?.role}</span>
            <span className="user-name">👤 {user?.name}</span>
          </div>
          {isAdmin && <ImportExport onImportComplete={loadData} />}
          <a
            className="api-docs-btn"
            href={SWAGGER_URL}
            target="_blank"
            rel="noopener noreferrer"
            title="Open Swagger API documentation"
          >
            📖 API Docs
          </a>
          <button className="logout-btn" onClick={logout}>🚪 Logout</button>
        </div>
      </header>

      {dragError && <div className="error-toast">⚠️ {dragError}</div>}

      <div className="tabs">
        <button
          className={activeTab === 'board' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('board')}
        >
          📋 Task Board
        </button>
        {isAdmin && (
          <button
            className={activeTab === 'users' ? 'tab active' : 'tab'}
            onClick={() => setActiveTab('users')}
          >
            👥 Users
          </button>
        )}
      </div>

      <div className="content">
        {activeTab === 'board' ? (
          <>
            <CreateTaskForm onCreateTask={handleCreateTask} />
            <FilterBar users={users} onFilterChange={handleFilterChange} />
            <TaskBoard
              tasks={tasks}
              users={users}
              isAdmin={isAdmin}
              onAssignTask={handleAssignTask}
              onUpdateStatus={handleUpdateStatus}
              onDeleteTask={handleDeleteTask}
              onTaskClick={(task) => setSelectedTask(task)}
            />
          </>
        ) : isAdmin ? (
          <>
            <CreateUserForm onCreateUser={handleCreateUser} />
            <UserList users={users} onDeleteUser={handleDeleteUser} />
          </>
        ) : null}
      </div>

      {/* ── AI Assistant floating widget ─────────────────────────────────── */}
      <AIAssistant onRefresh={loadData} />

      {/* ── Task Detail slide-over modal ─────────────────────────────────── */}
      <TaskDetailModal task={selectedTask} onClose={() => setSelectedTask(null)} />
    </div>
  );
}

// ── Root — switches between Login and MainApp ─────────────────────────────────
function AppContent() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <MainApp /> : <LoginPage />;
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
