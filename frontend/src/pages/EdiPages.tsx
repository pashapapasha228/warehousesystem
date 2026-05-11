import { Send } from '@mui/icons-material';
import { Alert, Box, Button, Card, CardContent, FormControl, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { ediApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { ResourcePage } from '../components/ResourcePage';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { BoolChip } from '../components/tables/ResourceTable';
import { ResourceTable } from '../components/tables/ResourceTable';
import { canManageEdi } from '../utils/permissions';
import { fmtDate } from '../utils/format';

export function EdiPartnersPage() {
  return (
    <ResourcePage
      title="EDI-партнеры"
      queryKey="edi-partners"
      api={ediApi.partners}
      canEdit={canManageEdi}
      columns={[
        { key: 'code', label: 'Код' },
        { key: 'name', label: 'Название' },
        { key: 'gln', label: 'GLN' },
        { key: 'counterpartyName', label: 'Контрагент' },
        { key: 'defaultWarehouseCode', label: 'Склад' },
        { key: 'inboundEnabled', label: 'Inbound', render: (r) => <BoolChip value={r.inboundEnabled} /> },
        { key: 'outboundEnabled', label: 'Outbound', render: (r) => <BoolChip value={r.outboundEnabled} /> },
      ]}
      fields={[
        { name: 'code', label: 'Код', required: true },
        { name: 'name', label: 'Название', required: true },
        { name: 'gln', label: 'GLN' },
        { name: 'counterpartyId', label: 'ID контрагента', type: 'number' },
        { name: 'defaultWarehouseId', label: 'ID склада по умолчанию', type: 'number' },
        { name: 'inboundEnabled', label: 'Входящие включены', type: 'checkbox' },
        { name: 'outboundEnabled', label: 'Исходящие включены', type: 'checkbox' },
        { name: 'isActive', label: 'Активно', type: 'checkbox' },
      ]}
    />
  );
}

export function EdiMappingsPage() {
  return (
    <ResourcePage
      title="EDI-маппинги товаров"
      queryKey="edi-mappings"
      api={ediApi.mappings}
      canEdit={canManageEdi}
      columns={[
        { key: 'partnerCode', label: 'Партнер' },
        { key: 'messageType', label: 'Тип' },
        { key: 'externalProductCode', label: 'Внешний код' },
        { key: 'externalUom', label: 'Внеш. ед.' },
        { key: 'internalSku', label: 'SKU' },
        { key: 'internalUom', label: 'Внутр. ед.' },
        { key: 'isActive', label: 'Статус', render: (r) => <BoolChip value={r.isActive} /> },
      ]}
      fields={[
        { name: 'partnerId', label: 'ID партнера', type: 'number', required: true },
        { name: 'messageType', label: 'Тип сообщения', type: 'select', required: true, options: ['ORDERS', 'DESADV', 'ORDRSP'].map((v) => ({ value: v, label: v })) },
        { name: 'externalProductCode', label: 'Внешний код товара', required: true },
        { name: 'externalUom', label: 'Внешняя ед.' },
        { name: 'internalProductId', label: 'ID внутреннего товара', type: 'number', required: true },
        { name: 'internalUom', label: 'Внутренняя ед.' },
        { name: 'isActive', label: 'Активно', type: 'checkbox' },
      ]}
    />
  );
}

export function EdiMessagesPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const query = useQuery({ queryKey: ['edi-messages', page, size, type, status], queryFn: () => ediApi.messages({ page, size, sort: 'id,desc', type: type || undefined, status: status || undefined }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">EDI-сообщения</Typography>
      <Box display="flex" gap={2} flexWrap="wrap">
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Тип</InputLabel><Select label="Тип" value={type} onChange={(e) => setType(e.target.value)}><MenuItem value="">Все</MenuItem>{['ORDERS', 'DESADV', 'ORDRSP'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Статус</InputLabel><Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}><MenuItem value="">Все</MenuItem>{['RECEIVED', 'NORMALIZED', 'PROCESSING', 'PROCESSED', 'FAILED'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      </Box>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} columns={[
        { key: 'id', label: 'ID' },
        { key: 'messageType', label: 'Тип' },
        { key: 'status', label: 'Статус' },
        { key: 'partnerCode', label: 'Партнер' },
        { key: 'documentNumber', label: 'Документ' },
        { key: 'relatedOperationNumber', label: 'Операция' },
        { key: 'receivedAt', label: 'Получено', render: (r) => fmtDate(r.receivedAt) },
        { key: 'errorMessage', label: 'Ошибка' },
      ]} />}
      <InboundEdiForm />
    </Stack>
  );
}

function InboundEdiForm() {
  const { user } = useAuth();
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const { register, handleSubmit, reset } = useForm({
    defaultValues: {
      partnerId: '',
      partnerCode: '',
      messageType: 'ORDERS',
      interchangeRef: '',
      messageRef: '',
      documentNumber: '',
      rawPayload: '',
      normalizedPayload: '{\n  "items": []\n}',
    },
  });
  const mutation = useMutation({ mutationFn: ediApi.inbound, onSuccess: (message) => { setOk(`Сообщение принято: #${message.id}`); reset(); }, onError: (err) => setError(getErrorMessage(err)) });
  if (!canManageEdi(user?.role)) return null;
  return (
    <Card>
      <CardContent>
        <Stack spacing={2}>
          <Typography variant="h6">Ручной прием inbound EDI</Typography>
          {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}
          {ok && <Alert severity="success" onClose={() => setOk('')}>{ok}</Alert>}
          <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: 'repeat(3, 1fr)' }} gap={2}>
            <TextField label="partnerId" {...register('partnerId')} />
            <TextField label="partnerCode" {...register('partnerCode')} />
            <TextField label="messageType" select defaultValue="ORDERS" {...register('messageType')}>{['ORDERS', 'DESADV', 'ORDRSP'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</TextField>
            <TextField label="interchangeRef" {...register('interchangeRef')} />
            <TextField label="messageRef" {...register('messageRef')} />
            <TextField label="documentNumber" {...register('documentNumber')} />
          </Box>
          <TextField label="rawPayload" multiline minRows={3} {...register('rawPayload')} />
          <TextField label="normalizedPayload JSON" multiline minRows={5} className="mono" {...register('normalizedPayload')} />
          <Button startIcon={<Send />} variant="contained" disabled={mutation.isPending} onClick={handleSubmit((data) => {
            setError('');
            try {
              mutation.mutate({ ...data, partnerId: data.partnerId ? Number(data.partnerId) : null, normalizedPayload: JSON.parse(data.normalizedPayload) });
            } catch {
              setError('normalizedPayload должен быть корректным JSON.');
            }
          })}>Отправить в backend</Button>
        </Stack>
      </CardContent>
    </Card>
  );
}

export function EdiQueuePage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [status, setStatus] = useState('');
  const query = useQuery({ queryKey: ['edi-queue', page, size, status], queryFn: () => ediApi.queue({ page, size, sort: 'id,desc', status: status || undefined }) });
  const process = useMutation({ mutationFn: ediApi.process, onSuccess: () => query.refetch() });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Очередь EDI</Typography>
      <FormControl sx={{ maxWidth: 220 }}><InputLabel>Статус</InputLabel><Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}><MenuItem value="">Все</MenuItem>{['PENDING', 'RUNNING', 'DONE', 'FAILED'].map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} onView={canManageEdi(user?.role) ? (row) => process.mutate(row.id) : undefined} columns={[
        { key: 'id', label: 'ID' },
        { key: 'ediMessageId', label: 'Сообщение' },
        { key: 'messageRef', label: 'Ref' },
        { key: 'partnerCode', label: 'Партнер' },
        { key: 'status', label: 'Статус' },
        { key: 'attemptCount', label: 'Попытки' },
        { key: 'scheduledAt', label: 'Запланировано', render: (r) => fmtDate(r.scheduledAt) },
        { key: 'lastError', label: 'Ошибка' },
      ]} />}
    </Stack>
  );
}

export function EdiAuditPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const query = useQuery({ queryKey: ['edi-audit', page, size], queryFn: () => ediApi.audit({ page, size, sort: 'id,desc' }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Аудит EDI</Typography>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} columns={[
        { key: 'ediMessageId', label: 'Сообщение' },
        { key: 'stage', label: 'Этап' },
        { key: 'status', label: 'Статус' },
        { key: 'details', label: 'Детали' },
        { key: 'createdAt', label: 'Дата', render: (r) => fmtDate(r.createdAt) },
      ]} />}
    </Stack>
  );
}
