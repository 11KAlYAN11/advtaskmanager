import { useState, useRef, useEffect } from 'react';
import { aiAPI } from '../api/aiApi';
import './AIAssistant.css';

interface Message {
  role: 'user' | 'ai';
  text: string;
  isError?: boolean;
}

// ── Gemini-style 4-pointed sparkle SVG ───────────────────────────────────────
const SparkleIcon = ({ size = 22, color = 'white' }: { size?: number; color?: string }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Large center diamond-star */}
    <path
      d="M12 2C12 2 13.2 8.8 19 12C13.2 15.2 12 22 12 22C12 22 10.8 15.2 5 12C10.8 8.8 12 2 12 2Z"
      fill={color}
    />
    {/* Small accent star — top right */}
    <path
      d="M20 3C20 3 20.7 5.8 22.5 6.5C20.7 7.2 20 10 20 10C20 10 19.3 7.2 17.5 6.5C19.3 5.8 20 3 20 3Z"
      fill={color}
      opacity="0.72"
    />
    {/* Tiny accent dot — bottom left */}
    <path
      d="M4 14C4 14 4.5 16 6 16.5C4.5 17 4 19 4 19C4 19 3.5 17 2 16.5C3.5 16 4 14 4 14Z"
      fill={color}
      opacity="0.5"
    />
  </svg>
);

// Close (X) icon for when panel is open
const CloseIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M18 6L6 18M6 6L18 18" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

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
  const [open, setOpen]         = useState(false);
  const [input, setInput]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'ai',
      text: "✦ Hi! I'm your AI Task Assistant.\n\nYou can ask me things like:\n• \"Create a task called Fix login bug\"\n• \"Move task 2 to REVIEW\"\n• \"Assign task 1 to John\"\n\nJust type naturally — I'll handle the rest! 🚀",
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
        {open ? <CloseIcon /> : <SparkleIcon />}
      </button>

      {/* ── Chat panel ─────────────────────────────────────────────────── */}
      {open && (
        <div className="ai-panel" role="dialog" aria-label="AI Task Assistant">
          {/* Header */}
          <div className="ai-header">
            <span className="ai-header-title">
              <SparkleIcon size={17} />
              AI Task Assistant
            </span>
            <span className="ai-badge">Groq · llama-3.3</span>
          </div>

          {/* Messages */}
          <div className="ai-messages">
            {messages.map((m, i) => (
              <div key={i} className={`ai-msg ai-msg--${m.role}${m.isError ? ' ai-msg--error' : ''}`}>
                <span className="ai-msg-avatar">
                  {m.role === 'ai' ? <SparkleIcon size={18} color="#7c6fcd" /> : '👤'}
                </span>
                <div className="ai-msg-text">{m.text}</div>
              </div>
            ))}

            {loading && (
              <div className="ai-msg ai-msg--ai">
                <span className="ai-msg-avatar"><SparkleIcon size={18} color="#7c6fcd" /></span>
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

