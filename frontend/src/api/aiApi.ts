import { authHeaders, handleResponse } from './api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export interface AIChatResponse {
  reply: string;
  refreshData: boolean;
  error: boolean;
}

export const aiAPI = {
  chat: async (message: string): Promise<AIChatResponse> => {
    const res = await fetch(`${API_BASE_URL}/ai/chat`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ message }),
    });

    await handleResponse(res); // handles 401 → auto-logout

    if (!res.ok) {
      // Server returned 4xx/5xx — surface a proper error object
      return {
        reply: `❌ Server error (HTTP ${res.status}). Please try again.`,
        refreshData: false,
        error: true,
      };
    }

    const data: AIChatResponse = await res.json();

    // Guard: ensure reply is always a string (never undefined / null)
    if (!data.reply) {
      return { reply: '⚠️ Received an empty response from the AI.', refreshData: false, error: true };
    }

    return data;
  },
};



