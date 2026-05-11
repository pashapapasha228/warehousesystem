import { http } from './http';
import type { CurrentUser } from '../types/api';

export async function login(username: string, password: string) {
  const response = await http.post<{ token: string }>('/auth/login', { username, password });
  return response.data;
}

export async function logout() {
  return http.post('/auth/logout');
}

export async function me() {
  const response = await http.get<CurrentUser>('/auth/me');
  return response.data;
}
