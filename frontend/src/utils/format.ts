import { format, parseISO } from 'date-fns';
import { ru } from 'date-fns/locale';

export function fmtDate(value?: string) {
  if (!value) return '-';
  try {
    return format(parseISO(value), 'dd.MM.yyyy HH:mm', { locale: ru });
  } catch {
    return value;
  }
}

export function n(value?: number | string | null, suffix = '') {
  if (value === undefined || value === null || value === '') return '-';
  return `${Number(value).toLocaleString('ru-RU')}${suffix}`;
}

export function isoDateTimeLocal(date: Date) {
  const pad = (v: number) => String(v).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function toBackendDateTime(localValue: string) {
  return localValue.length === 16 ? `${localValue}:00` : localValue;
}

export function fillPercent(current?: number, max?: number) {
  if (!current || !max || max <= 0) return 0;
  return Math.min(100, Math.round((current / max) * 100));
}
