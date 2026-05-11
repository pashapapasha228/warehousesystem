import { Alert, Box, Button, Card, CardContent, Chip, Divider, Grid2 as Grid, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { operationsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { operationStatusLabels, operationTypeLabels } from '../types/enums';
import { fmtDate } from '../utils/format';
import { canCompleteOperations } from '../utils/permissions';

export function OperationDetailsPage() {
  const id = Number(useParams().id);
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState('');
  const query = useQuery({ queryKey: ['operation', id], queryFn: () => operationsApi.get(id), enabled: Number.isFinite(id) });
  const action = useMutation({
    mutationFn: (kind: 'complete' | 'cancel') => kind === 'complete' ? operationsApi.complete(id) : operationsApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operation', id] });
      queryClient.invalidateQueries({ queryKey: ['operations'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorState message={getErrorMessage(query.error)} />;
  const operation = query.data!;
  const canAct = canCompleteOperations(user?.role) && operation.status === 'DRAFT';

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4">Операция {operation.operationNumber}</Typography>
          <Typography color="text.secondary">{operationTypeLabels[operation.type]} · {fmtDate(operation.createdAt)}</Typography>
        </Box>
        <Chip label={operationStatusLabels[operation.status]} color={operation.status === 'COMPLETED' ? 'success' : operation.status === 'DRAFT' ? 'warning' : 'default'} />
        {canAct && <Button variant="contained" onClick={() => action.mutate('complete')}>Выполнить</Button>}
        {canAct && <Button color="error" onClick={() => action.mutate('cancel')}>Отменить</Button>}
      </Box>
      {notice && <Alert severity="error" onClose={() => setNotice('')}>{notice}</Alert>}
      <Card>
        <CardContent>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 3 }}><Typography color="text.secondary">Склад</Typography><Typography>{operation.warehouseCode || operation.warehouseId}</Typography></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Typography color="text.secondary">Контрагент ID</Typography><Typography>{operation.counterpartyId || '—'}</Typography></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Typography color="text.secondary">Источник</Typography><Typography>{operation.source || 'MANUAL'}</Typography></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Typography color="text.secondary">Документ</Typography><Typography>{operation.externalDocumentNumber || '—'}</Typography></Grid>
            <Grid size={{ xs: 12 }}><Divider /></Grid>
            <Grid size={{ xs: 12 }}><Typography color="text.secondary">Комментарий</Typography><Typography>{operation.comment || '—'}</Typography></Grid>
          </Grid>
        </CardContent>
      </Card>
      <Typography variant="h6">Позиции</Typography>
      <ResourceTable
        rows={operation.items}
        total={operation.items.length}
        page={0}
        size={operation.items.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'productSku', label: 'SKU' },
          { key: 'productName', label: 'Товар' },
          { key: 'quantity', label: 'Кол-во' },
          { key: 'unitPrice', label: 'Цена' },
          { key: 'unitOfMeasure', label: 'Ед.' },
          { key: 'fromCellCode', label: 'Из ячейки' },
          { key: 'toCellCode', label: 'В ячейку' },
        ]}
      />
    </Stack>
  );
}
