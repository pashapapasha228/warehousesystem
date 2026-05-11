import { Alert } from '@mui/material';
import type { PropsWithChildren } from 'react';
import type { UserRole } from '../types/enums';
import { useAuth } from './useAuth';

export function RoleGuard({ roles, children }: PropsWithChildren<{ roles: UserRole[] }>) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) {
    return <Alert severity="warning">Недостаточно прав для просмотра раздела.</Alert>;
  }
  return children;
}
