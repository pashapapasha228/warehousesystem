import { describe, expect, it } from 'vitest';
import {
  canCompleteOperations,
  canManageCatalogs,
  canManageEdi,
  canManageUsers,
  canUseReports,
} from './permissions';
import type { UserRole } from '../types/enums';

describe('permission helpers', () => {
  const roles: Array<UserRole | undefined> = ['ADMIN', 'MANAGER', 'STOREKEEPER', undefined];

  it('allows only admins to manage users', () => {
    expect(roles.map((role) => canManageUsers(role))).toEqual([true, false, false, false]);
  });

  it('allows admins and managers to operate business sections', () => {
    expect(roles.map((role) => canManageCatalogs(role))).toEqual([true, true, false, false]);
    expect(roles.map((role) => canCompleteOperations(role))).toEqual([true, true, false, false]);
    expect(roles.map((role) => canUseReports(role))).toEqual([true, true, false, false]);
    expect(roles.map((role) => canManageEdi(role))).toEqual([true, true, false, false]);
  });
});
