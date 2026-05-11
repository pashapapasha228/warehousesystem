import { Chip } from '@mui/material';
import { usersApi } from '../api/resourcesApi';
import { ResourcePage } from '../components/ResourcePage';
import { BoolChip } from '../components/tables/ResourceTable';
import { roleLabels } from '../types/enums';
import { canManageUsers } from '../utils/permissions';

export function UsersPage() {
  return (
    <ResourcePage
      title="Пользователи"
      description="Доступно только администраторам"
      queryKey="users"
      api={usersApi}
      canEdit={canManageUsers}
      columns={[
        { key: 'username', label: 'Логин' },
        { key: 'fullName', label: 'ФИО' },
        { key: 'email', label: 'Email' },
        { key: 'role', label: 'Роль', render: (r) => <Chip size="small" label={roleLabels[r.role]} /> },
        { key: 'isActive', label: 'Статус', render: (r) => <BoolChip value={r.isActive} /> },
      ]}
      fields={[
        { name: 'username', label: 'Логин', required: true },
        { name: 'password', label: 'Пароль', type: 'password' },
        { name: 'fullName', label: 'ФИО' },
        { name: 'email', label: 'Email' },
        { name: 'role', label: 'Роль', type: 'select', required: true, options: Object.entries(roleLabels).map(([value, label]) => ({ value, label })) },
        { name: 'isActive', label: 'Активен', type: 'checkbox' },
      ]}
    />
  );
}
