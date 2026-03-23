import { useState } from 'react';
import type { User } from '../types/types';
import './CreateUserForm.css';

interface Props {
  onCreateUser: (user: Omit<User, 'id'>) => void;
}

export default function CreateUserForm({ onCreateUser }: Props) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'ADMIN' | 'USER'>('USER');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onCreateUser({ name, email, password, role });
    setName('');
    setEmail('');
    setPassword('');
    setRole('USER');
  };

  return (
    <div className="create-user-form">
      <h2>➕ Create New User</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <select value={role} onChange={(e) => setRole(e.target.value as 'ADMIN' | 'USER')}>
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button type="submit">Create User</button>
      </form>
    </div>
  );
}

