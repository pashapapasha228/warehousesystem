import { http } from './http';
import type { Page, PageParams } from '../types/api';

export function list<T>(url: string, params?: PageParams) {
  return http.get<Page<T>>(url, { params }).then((r) => r.data);
}

export function getOne<T>(url: string, id: number | string) {
  return http.get<T>(`${url}/${id}`).then((r) => r.data);
}

export function createOne<T, B>(url: string, body: B) {
  return http.post<T>(url, body).then((r) => r.data);
}

export function updateOne<T, B>(url: string, id: number | string, body: B) {
  return http.put<T>(`${url}/${id}`, body).then((r) => r.data);
}

export function deleteOne(url: string, id: number | string) {
  return http.delete(`${url}/${id}`).then((r) => r.data);
}
