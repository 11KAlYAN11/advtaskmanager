import { useMemo, useState, useRef, useEffect } from 'react';
import { aiAPI } from '../api/aiApi';
import './AIAssistant.css';
import type { Task, User } from '../types/types';

interface Message {
  role: 'user' | 'ai';
  text: string;
  isError?: boolean;
}

// ── OpenAI-style icon (Tabler "brand-openai"), tinted light-blue ────────────
const OpenAIIcon = ({ size = 22, color = '#93c5fd' }: { size?: number; color?: string }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path stroke="none" d="M0 0h24v24H0z" fill="none" />
    <path
      d="M11.217 19.384a3.501 3.501 0 0 0 6.783 -1.217v-5.167l-6 -3.35"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M5.214 15.014a3.501 3.501 0 0 0 4.446 5.266l4.34 -2.534v-6.946"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M6 7.63c-1.391 -.236 -2.787 .395 -3.534 1.689a3.474 3.474 0 0 0 1.271 4.745l4.263 2.514l6 -3.348"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M12.783 4.616a3.501 3.501 0 0 0 -6.783 1.217v5.067l6 3.45"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M18.786 8.986a3.501 3.501 0 0 0 -4.446 -5.266l-4.34 2.534v6.946"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
    <path
      d="M18 16.302c1.391 .236 2.787 -.395 3.534 -1.689a3.474 3.474 0 0 0 -1.271 -4.745l-4.308 -2.514l-5.955 3.42"
      stroke={color}
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

// Close (X) icon for when panel is open
const CloseIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M18 6L6 18M6 6L18 18" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

function buildSuggestions(tasks: Task[], users: User[]) {
  const taskTitles = tasks.map(t => t.title).filter((t): t is string => typeof t === 'string' && t.trim().length > 0);
  const userNames = users.map(u => u.name).filter((n): n is string => typeof n === 'string' && n.trim().length > 0);

  const firstTask = taskTitles[0];
  const secondTask = taskTitles[1];
  const anyUser = userNames[0];

  const base = [
    '📝 Create a task "Fix login bug"',
    '📋 Show me all tasks summary',
    '🔎 Search tasks with "login"',
  ];

  const action = [
    ...(firstTask ? [`🔄 Move task "${firstTask}" to IN_PROGRESS`] : []),
    ...(secondTask ? [`👁️ Move task "${secondTask}" to REVIEW`] : []),
    ...(firstTask && anyUser ? [`👤 Assign task "${firstTask}" to "${anyUser}"`] : []),
    ...(firstTask ? [`✅ Mark task "${firstTask}" as DONE`] : []),
  ];

  // If there's nothing to act on yet, keep prompts generic (won't error).
  if (action.length === 0) {
    return [
      ...base,
      '➡️ Move task "ABC" to DONE',
      '👤 Assign task "ABC" to user "XYZ"',
      '🧾 Export all data as JSON',
    ];
  }

  return [...base, ...action].slice(0, 6);
}

interface Props {
  onRefresh: () => void;  // called when AI performs a CRUD action
  tasks?: Task[];
  users?: User[];
}

export default function AIAssistant({ onRefresh, tasks = [], users = [] }: Props) {
  const [open, setOpen]         = useState(false);
  const [input, setInput]       = useState('');
  const [loading, setLoading]   = useState(false);
  const suggestions = useMemo(() => buildSuggestions(tasks, users), [tasks, users]);
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'ai',
      text:
        "Hi! I'm your AI Task Assistant.\n\nTry prompts like:\n• \"Create a task called Fix login bug\"\n• \"Move task ABC to REVIEW\"\n• \"Assign task ABC to user XYZ\"\n\nTip: use the suggestions below — they adapt to your current tasks/users.",
    },
  ]);

  const bottomRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new message arrives
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const send = async (text?: string) => {
    const msg = (text ?? input).trim();
    if (!msg || loading) return;

    setInput('');
    setMessages(prev => [...prev, { role: 'user', text: msg }]);
    setLoading(true);

    try {
      const response = await aiAPI.chat(msg);
      setMessages(prev => [
        ...prev,
        { role: 'ai', text: response.reply, isError: response.error },
      ]);
      if (response.refreshData) {
        onRefresh(); // reload tasks + users from backend
      }
    } catch {
      setMessages(prev => [
        ...prev,
        { role: 'ai', text: '❌ Could not reach the server. Is the backend running?', isError: true },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKey = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <>
      {/* ── Floating action button ─────────────────────────────────────── */}
      <button
        className={`ai-fab${open ? ' ai-fab--open' : ''}`}
        onClick={() => setOpen(o => !o)}
        title="AI Task Assistant"
        aria-label={open ? 'Close AI Assistant' : 'Open AI Assistant'}
      >
        {open ? <CloseIcon /> : <OpenAIIcon />}
      </button>

      {/* ── Chat panel ─────────────────────────────────────────────────── */}
      {open && (
        <div className="ai-panel" role="dialog" aria-label="AI Task Assistant">
          {/* Header */}
          <div className="ai-header">
            <span className="ai-header-title">
              <OpenAIIcon size={17} color="white" />
              AI Task Assistant
            </span>
            <span className="ai-badge">Groq · llama-3.3</span>
          </div>

          {/* Messages */}
          <div className="ai-messages">
            {messages.map((m, i) => (
              <div key={i} className={`ai-msg ai-msg--${m.role}${m.isError ? ' ai-msg--error' : ''}`}>
                <span className="ai-msg-avatar">
                  {m.role === 'ai' ? <OpenAIIcon size={18} color="#60a5fa" /> : '👤'}
                </span>
                <div className="ai-msg-text">{m.text}</div>
              </div>
            ))}

            {loading && (
              <div className="ai-msg ai-msg--ai">
                <span className="ai-msg-avatar"><OpenAIIcon size={18} color="#60a5fa" /></span>
                <div className="ai-msg-text ai-thinking">
                  <span /><span /><span />
                </div>
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          {/* Quick suggestions */}
          {messages.length <= 1 && (
            <div className="ai-suggestions">
              {suggestions.map((s, i) => (
                <button key={i} className="ai-suggestion" onClick={() => send(s)}>
                  {s}
                </button>
              ))}
            </div>
          )}

          {/* Input */}
          <div className="ai-input-row">
            <textarea
              className="ai-input"
              placeholder="Ask anything… (Enter to send)"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKey}
              rows={2}
              disabled={loading}
            />
            <button
              className="ai-send"
              onClick={() => send()}
              disabled={loading || !input.trim()}
              title="Send"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M22 2L11 13M22 2L15 22L11 13M22 2L2 9L11 13" stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      )}
    </>
  );
}

