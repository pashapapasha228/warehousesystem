import type { UserRole } from '../types/enums';

export function canManageCatalogs(role?: UserRole) {
  return role === 'ADMIN' || role === 'MANAGER';
}

export function canManageUsers(role?: UserRole) {
  return role === 'ADMIN';
}

export function canCompleteOperations(role?: UserRole) {
  return role === 'ADMIN' || role === 'MANAGER';
}

export function canUseReports(role?: UserRole) {
  return role === 'ADMIN' || role === 'MANAGER';
}

export function canManageEdi(role?: UserRole) {
  return role === 'ADMIN' || role === 'MANAGER';
}
