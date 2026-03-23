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
    await handleResponse(res);
    return res.json();
  },
};

