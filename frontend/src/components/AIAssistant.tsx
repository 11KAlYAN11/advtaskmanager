import { useState, useRef, useEffect } from 'react';
import { aiAPI } from '../api/aiApi';
import './AIAssistant.css';

interface Message {
  role: 'user' | 'ai';
  text: string;
  isError?: boolean;
}

const SUGGESTIONS = [
  '📝 Create a task "Fix login bug"',
  '🔄 Move task 1 to IN_PROGRESS',
  '👁️ Move task 2 to REVIEW',
  '👤 Assign task 1 to user 1',
  '✅ Mark task 3 as DONE',
  '📋 Show me all tasks summary',
];

interface Props {
  onRefresh: () => void;  // called when AI performs a CRUD action
}

export default function AIAssistant({ onRefresh }: Props) {
  const [open, setOpen]       = useState(false);
  const [input, setInput]     = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'ai',
      text: "👋 Hi! I'm your AI Task Assistant.\n\nYou can ask me things like:\n• \"Create a task called Fix login bug\"\n• \"Move task 2 to REVIEW\"\n• \"Assign task 1 to John\"\n\nJust type naturally — I'll handle the rest! 🚀",
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
      {/* ── Floating button ──────────────────────────────────────────────── */}
      <button
        className={`ai-fab${open ? ' ai-fab--open' : ''}`}
        onClick={() => setOpen(o => !o)}
        title="AI Task Assistant"
      >
        {open ? '✕' : '🤖'}
      </button>

      {/* ── Chat panel ───────────────────────────────────────────────────── */}
      {open && (
        <div className="ai-panel">
          {/* Header */}
          <div className="ai-header">
            <span>🤖 AI Task Assistant</span>
            <span className="ai-badge">GPT-4o-mini</span>
          </div>

          {/* Messages */}
          <div className="ai-messages">
            {messages.map((m, i) => (
              <div key={i} className={`ai-msg ai-msg--${m.role}${m.isError ? ' ai-msg--error' : ''}`}>
                <span className="ai-msg-avatar">{m.role === 'ai' ? '🤖' : '👤'}</span>
                <div className="ai-msg-text">{m.text}</div>
              </div>
            ))}

            {loading && (
              <div className="ai-msg ai-msg--ai">
                <span className="ai-msg-avatar">🤖</span>
                <div className="ai-msg-text ai-thinking">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          {/* Quick suggestions (shown when chat is fresh) */}
          {messages.length <= 1 && (
            <div className="ai-suggestions">
              {SUGGESTIONS.map((s, i) => (
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
              placeholder="Ask me anything… (Enter to send)"
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
            >
              ➤
            </button>
          </div>
        </div>
      )}
    </>
  );
}

