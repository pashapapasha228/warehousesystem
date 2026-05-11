import { Add, Refresh } from '@mui/icons-material';
import { Alert, Box, Button, Snackbar, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { getErrorMessage } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { ConfirmDialog } from './feedback/ConfirmDialog';
import { ErrorState, LoadingState } from './feedback/StateViews';
import { FieldDef, ResourceFormDialog } from './forms/ResourceFormDialog';
import { Column, ResourceTable } from './tables/ResourceTable';
import type { UserRole } from '../types/enums';

type Api<T> = {
  list: (params: any) => Promise<{ content: T[]; totalElements: number; number: number; size: number }>;
  create?: (body: any) => Promise<T>;
  update?: (id: number, body: any) => Promise<T>;
  delete?: (id: number) => Promise<unknown>;
};

export function ResourcePage<T extends { id?: number }>({
  title,
  queryKey,
  api,
  columns,
  fields,
  canEdit,
  description,
}: {
  title: string;
  queryKey: string;
  api: Api<T>;
  columns: Column<T>[];
  fields: FieldDef[];
  canEdit: (role?: UserRole) => boolean;
  description?: string;
}) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [editing, setEditing] = useState<T | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<T | null>(null);
  const [notice, setNotice] = useState('');
  const allowed = canEdit(user?.role);

  const query = useQuery({
    queryKey: [queryKey, page, size],
    queryFn: () => api.list({ page, size, sort: 'id,desc' }),
  });

  const saveMutation = useMutation({
    mutationFn: (data: any) => editing?.id ? api.update!(editing.id, data) : api.create!(data),
    onSuccess: () => {
      setFormOpen(false);
      setEditing(null);
      setNotice('Данные сохранены.');
      queryClient.invalidateQueries({ queryKey: [queryKey] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  const deleteMutation = useMutation({
    mutationFn: (row: T) => api.delete!(row.id!),
    onSuccess: () => {
      setDeleting(null);
      setNotice('Запись удалена.');
      queryClient.invalidateQueries({ queryKey: [queryKey] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  return (
    <Stack spacing={2}>
      <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
        <Box sx={{ flex: 1, minWidth: 220 }}>
          <Typography variant="h4">{title}</Typography>
          {description && <Typography color="text.secondary">{description}</Typography>}
        </Box>
        <Button startIcon={<Refresh />} onClick={() => query.refetch()}>Обновить</Button>
        {allowed && <Button variant="contained" startIcon={<Add />} onClick={() => { setEditing(null); setFormOpen(true); }}>Создать</Button>}
      </Box>
      {!allowed && <Alert severity="info">Ваша роль разрешает просмотр, но не изменение этого раздела.</Alert>}
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && (
        <ResourceTable
          rows={query.data.content}
          columns={columns}
          total={query.data.totalElements}
          page={page}
          size={size}
          onPageChange={setPage}
          onSizeChange={(next) => { setSize(next); setPage(0); }}
          onEdit={allowed ? (row) => { setEditing(row); setFormOpen(true); } : undefined}
          onDelete={allowed ? setDeleting : undefined}
        />
      )}
      <ResourceFormDialog
        open={formOpen}
        title={editing ? 'Редактирование' : 'Создание'}
        fields={fields}
        initialValues={editing as any}
        onClose={() => setFormOpen(false)}
        onSubmit={(data) => saveMutation.mutateAsync(data)}
      />
      <ConfirmDialog
        open={!!deleting}
        title="Удалить запись?"
        text="Действие нельзя отменить. Backend дополнительно проверит ограничения целостности."
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && deleteMutation.mutate(deleting)}
      />
      <Snackbar open={!!notice} autoHideDuration={5000} onClose={() => setNotice('')} message={notice} />
    </Stack>
  );
}
