import type { User } from '../types/types';
import './UserList.css';

interface Props {
  users: User[];
  onDeleteUser: (id: number) => void;
}

export default function UserList({ users, onDeleteUser }: Props) {
  return (
    <div className="user-list">
      <h2>👥 All Users ({users.length})</h2>
      <div className="users-grid">
        {users.map((user) => (
          <div key={user.id} className={`user-card ${user.role.toLowerCase()}`}>
            <div className="user-header">
              <h3>{user.name}</h3>
              <span className={`role-badge ${user.role.toLowerCase()}`}>
                {user.role}
              </span>
            </div>
            <p className="email">📧 {user.email}</p>
            <p className="task-count">
              📋 {user.tasks?.length || 0} task(s)
            </p>
            <button
              className="delete-btn"
              onClick={() => user.id && onDeleteUser(user.id)}
            >
              🗑️ Delete
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}

