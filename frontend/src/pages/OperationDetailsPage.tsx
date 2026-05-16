import { Alert, Box, Button, Card, CardContent, Chip, Divider, FormControl, Grid2 as Grid, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
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
  const [decision, setDecision] = useState<'ACCEPT' | 'REJECT' | 'ACCEPT_PARTIALLY'>('ACCEPT');
  const [actuals, setActuals] = useState<Record<number, number>>({});
  const query = useQuery({ queryKey: ['operation', id], queryFn: () => operationsApi.get(id), enabled: Number.isFinite(id) });
  const chain = useQuery({ queryKey: ['operation-chain', id], queryFn: () => operationsApi.executionChain(id), enabled: Number.isFinite(id) });
  const verifications = useQuery({ queryKey: ['operation-verifications', id], queryFn: () => operationsApi.verifications(id), enabled: Number.isFinite(id) });
  const action = useMutation({
    mutationFn: (kind: 'complete' | 'ship' | 'cancel') => kind === 'complete' ? operationsApi.complete(id) : kind === 'ship' ? operationsApi.ship(id) : operationsApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operation', id] });
      queryClient.invalidateQueries({ queryKey: ['operations'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  const operationData = query.data;
  const verify = useMutation({
    mutationFn: () => operationsApi.verify(id, {
      decision,
      items: operationData!.items.map((item) => ({
        operationItemId: item.id,
        actualQuantity: actuals[item.id!] ?? item.quantity,
      })),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operation', id] });
      queryClient.invalidateQueries({ queryKey: ['operation-chain', id] });
      queryClient.invalidateQueries({ queryKey: ['operation-verifications', id] });
      queryClient.invalidateQueries({ queryKey: ['operations'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });
  const canAct = canCompleteOperations(user?.role) && operationData?.status === 'DRAFT';
  const canFactCheck = canAct && operationData?.type !== 'OUTCOME';
  const itemRows = useMemo(() => (operationData?.items ?? []).map((item) => ({
    ...item,
    actualQuantity: actuals[item.id!] ?? item.quantity,
    discrepancy: (actuals[item.id!] ?? item.quantity) - item.quantity,
  })), [actuals, operationData?.items]);

  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorState message={getErrorMessage(query.error)} />;
  const operation = operationData!;

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1 }}>
          <Typography variant="h4">Операция {operation.operationNumber}</Typography>
          <Typography color="text.secondary">{operationTypeLabels[operation.type]} · {fmtDate(operation.createdAt)}</Typography>
        </Box>
        <Chip label={operationStatusLabels[operation.status]} color={operation.status === 'COMPLETED' ? 'success' : operation.status === 'DRAFT' ? 'warning' : 'default'} />
        {canAct && operation.type === 'OUTCOME' && <Button variant="contained" onClick={() => action.mutate('ship')}>Отправить товары</Button>}
        {canAct && operation.type !== 'OUTCOME' && <Button variant="contained" onClick={() => action.mutate('complete')}>Принять товар</Button>}
        {canAct && <Button color="error" onClick={() => action.mutate('cancel')}>Отменить</Button>}
        {operation.status === 'SHIPPED' && <Chip label="Ожидается подтверждение клиента" />}
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
        rows={itemRows}
        total={itemRows.length}
        page={0}
        size={itemRows.length || 10}
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
      {canFactCheck && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="h6">Fact check</Typography>
              <Box display="flex" gap={2} flexWrap="wrap">
                <FormControl sx={{ minWidth: 220 }}>
                  <InputLabel>Decision</InputLabel>
                  <Select label="Decision" value={decision} onChange={(event) => setDecision(event.target.value as any)}>
                    <MenuItem value="ACCEPT">Accept</MenuItem>
                    <MenuItem value="ACCEPT_PARTIALLY">Accept partially</MenuItem>
                    <MenuItem value="REJECT">Reject</MenuItem>
                  </Select>
                </FormControl>
                <Button variant="contained" disabled={verify.isPending} onClick={() => verify.mutate()}>Fix fact and close</Button>
              </Box>
              <Grid container spacing={2}>
                {operation.items.map((item) => (
                  <Grid key={item.id} size={{ xs: 12, md: 4 }}>
                    <TextField
                      fullWidth
                      type="number"
                      label={`${item.productSku} actual`}
                      value={actuals[item.id!] ?? item.quantity}
                      onChange={(event) => setActuals((current) => ({ ...current, [item.id!]: Number(event.target.value) }))}
                    />
                  </Grid>
                ))}
              </Grid>
            </Stack>
          </CardContent>
        </Card>
      )}
      <Typography variant="h6">Business execution chain</Typography>
      <ResourceTable
        rows={chain.data ?? []}
        total={chain.data?.length ?? 0}
        page={0}
        size={chain.data?.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'stage', label: 'Stage' },
          { key: 'status', label: 'Status' },
          { key: 'details', label: 'Details' },
          { key: 'createdBy', label: 'User' },
          { key: 'createdAt', label: 'Date', render: (row) => fmtDate(row.createdAt) },
        ]}
      />
      <Typography variant="h6">Fact history</Typography>
      <ResourceTable
        rows={verifications.data ?? []}
        total={verifications.data?.length ?? 0}
        page={0}
        size={verifications.data?.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'decision', label: 'Decision' },
          { key: 'verifiedBy', label: 'User' },
          { key: 'verifiedAt', label: 'Date', render: (row) => fmtDate(row.verifiedAt) },
          { key: 'items', label: 'Diffs', render: (row) => row.items.map((item) => `${item.productSku}: ${item.discrepancyQuantity}`).join(', ') },
        ]}
      />
    </Stack>
  );
}
