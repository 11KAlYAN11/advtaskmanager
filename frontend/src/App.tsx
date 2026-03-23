import { useState, useEffect } from 'react';
import './App.css';
import { userAPI, taskAPI } from './api/api';
import type { User, Task, TaskStatus } from './types/types';
import { AuthProvider, useAuth } from './context/AuthContext';
import UserList from './components/UserList';
import TaskBoard from './components/TaskBoard';
import CreateUserForm from './components/CreateUserForm';
import CreateTaskForm from './components/CreateTaskForm';
import LoginPage from './components/LoginPage';
import AIAssistant from './components/AIAssistant';

// ── Main app (only shown when authenticated) ──────────────────────────────────
function MainApp() {
  const { user, logout } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'board' | 'users'>('board');
  const [dragError, setDragError] = useState<string | null>(null);

  const showError = (msg: string) => {
    setDragError(msg);
    setTimeout(() => setDragError(null), 4000);
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const [usersData, tasksData] = await Promise.all([
        userAPI.getAll(),
        taskAPI.getAll(),
      ]);
      setUsers(usersData);
      setTasks(tasksData);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

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
    try { await taskAPI.create(taskData); await loadData(); }
    catch (error) { console.error('Error creating task:', error); }
  };

  const handleAssignTask = async (taskId: number, userId: number) => {
    try { await taskAPI.assignToUser(taskId, userId); await loadData(); }
    catch (error) { console.error('Error assigning task:', error); showError('Failed to assign task.'); }
  };

  const handleUpdateStatus = async (taskId: number, status: TaskStatus) => {
    try { await taskAPI.updateStatus(taskId, status); await loadData(); }
    catch (error) { console.error('Error updating status:', error); showError(`Failed to move task to ${status}. Check console.`); }
  };

  const handleDeleteTask = async (id: number) => {
    if (window.confirm('Delete this task?')) {
      try { await taskAPI.delete(id); await loadData(); }
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
          <button className="logout-btn" onClick={logout}>🚪 Logout</button>
        </div>
      </header>

      {/* Error toast — shows when drag/API call fails */}
      {dragError && (
        <div className="error-toast">⚠️ {dragError}</div>
      )}

      <div className="tabs">
        {/* Task Board — visible to everyone */}
        <button
          className={activeTab === 'board' ? 'tab active' : 'tab'}
          onClick={() => setActiveTab('board')}
        >
          📋 Task Board
        </button>

        {/* Users tab — ADMIN only */}
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
            <TaskBoard
              tasks={tasks}
              users={users}
              isAdmin={isAdmin}
              onAssignTask={handleAssignTask}
              onUpdateStatus={handleUpdateStatus}
              onDeleteTask={handleDeleteTask}
            />
          </>
        ) : isAdmin ? (
          /* Users tab — only reachable by ADMIN */
          <>
            <CreateUserForm onCreateUser={handleCreateUser} />
            <UserList users={users} onDeleteUser={handleDeleteUser} />
          </>
        ) : null}
      </div>

      {/* ── AI Assistant floating widget ─────────────────────────────────── */}
      <AIAssistant onRefresh={loadData} />
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
