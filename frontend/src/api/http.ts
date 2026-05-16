import axios, { AxiosError } from 'axios';

const TOKEN_KEY = 'warehouse.jwt';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

http.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const storedWarehouseId = localStorage.getItem('warehouse.context.id');
  const warehouseId = storedWarehouseId ? Number(storedWarehouseId) : null;
  if (warehouseId) {
    config.headers['X-Warehouse-Id'] = String(warehouseId);
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      tokenStore.clear();
      window.dispatchEvent(new Event('auth:unauthorized'));
    }
    return Promise.reject(error);
  },
);

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 403) return 'Недостаточно прав для выполнения действия.';
    const data = error.response?.data as any;
    if (typeof data === 'string') return data;
    if (data?.message) return data.message;
    if (data?.error) return data.error;
    if (data?.details) return String(data.details);
    if (error.message) return error.message;
  }
  return error instanceof Error ? error.message : 'Неизвестная ошибка.';
}

export async function downloadBlob(url: string, filename: string, params?: Record<string, unknown>) {
  const response = await http.get(url, { params, responseType: 'blob' });
  const href = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = href;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(href);
}
