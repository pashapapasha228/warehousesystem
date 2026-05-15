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
import { useTableSort } from '../utils/sorting';
import { ediMessageStatuses, ediMessageTypeLabels, ediMessageTypes, ediQueueStatuses, type EdiMessageType } from '../types/enums';

const inboundPayloadExamples: Record<EdiMessageType, string> = {
  DESADV: '{\n  "warehouseId": 1,\n  "documentDate": "2026-05-14",\n  "items": [\n    {\n      "externalProductCode": "SUPPLIER-SKU-001",\n      "quantity": 5,\n      "toCellId": 1,\n      "unitPrice": 10,\n      "unitOfMeasure": "pcs"\n    }\n  ]\n}',
  ORDERS: '{\n  "warehouseId": 1,\n  "documentDate": "2026-05-14",\n  "items": [\n    {\n      "externalProductCode": "CUSTOMER-SKU-001",\n      "quantity": 2,\n      "fromCellId": 1,\n      "unitPrice": 10,\n      "unitOfMeasure": "pcs"\n    }\n  ]\n}',
  ORDRSP: '{\n  "documentDate": "2026-05-14",\n  "items": []\n}',
};

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
        { name: 'messageType', label: 'Тип сообщения', type: 'select', required: true, options: ediMessageTypes.map((v) => ({ value: v, label: ediMessageTypeLabels[v] })) },
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
  const tableSort = useTableSort('id', 'desc');
  const query = useQuery({ queryKey: ['edi-messages', page, size, type, status, tableSort.sort], queryFn: () => ediApi.messages({ page, size, sort: tableSort.sort, type: type || undefined, status: status || undefined }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">EDI-сообщения</Typography>
      <Box display="flex" gap={2} flexWrap="wrap">
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Тип</InputLabel><Select label="Тип" value={type} onChange={(e) => setType(e.target.value)}><MenuItem value="">Все</MenuItem>{ediMessageTypes.map((v) => <MenuItem key={v} value={v}>{ediMessageTypeLabels[v]}</MenuItem>)}</Select></FormControl>
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Статус</InputLabel><Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}><MenuItem value="">Все</MenuItem>{ediMessageStatuses.map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      </Box>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} {...tableSort.tableSortProps} onSortChange={(sortBy, sortDirection) => { tableSort.tableSortProps.onSortChange(sortBy, sortDirection); setPage(0); }} columns={[
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
  const { register, handleSubmit, reset, setValue, watch } = useForm({
    defaultValues: {
      partnerId: '',
      partnerCode: '',
      messageType: 'DESADV' as EdiMessageType,
      interchangeRef: '',
      messageRef: '',
      documentNumber: '',
      rawPayload: '',
      normalizedPayload: inboundPayloadExamples.DESADV,
    },
  });
  const selectedMessageType = watch('messageType') as EdiMessageType;
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
            <TextField label="messageType" select defaultValue="DESADV" {...register('messageType')}>{ediMessageTypes.map((v) => <MenuItem key={v} value={v}>{ediMessageTypeLabels[v]}</MenuItem>)}</TextField>
            <TextField label="interchangeRef" {...register('interchangeRef')} />
            <TextField label="messageRef" {...register('messageRef')} />
            <TextField label="documentNumber" {...register('documentNumber')} />
          </Box>
          <TextField label="rawPayload" multiline minRows={3} {...register('rawPayload')} />
          <Button variant="outlined" onClick={() => setValue('normalizedPayload', inboundPayloadExamples[selectedMessageType])}>Подставить пример payload</Button>
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
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [status, setStatus] = useState('');
  const tableSort = useTableSort('id', 'desc');
  const query = useQuery({ queryKey: ['edi-queue', page, size, status, tableSort.sort], queryFn: () => ediApi.queue({ page, size, sort: tableSort.sort, status: status || undefined }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Очередь EDI</Typography>
      <FormControl sx={{ maxWidth: 220 }}><InputLabel>Статус</InputLabel><Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}><MenuItem value="">Все</MenuItem>{ediQueueStatuses.map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} {...tableSort.tableSortProps} onSortChange={(sortBy, sortDirection) => { tableSort.tableSortProps.onSortChange(sortBy, sortDirection); setPage(0); }} columns={[
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
  const tableSort = useTableSort('id', 'desc');
  const query = useQuery({ queryKey: ['edi-audit', page, size, tableSort.sort], queryFn: () => ediApi.audit({ page, size, sort: tableSort.sort }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Аудит EDI</Typography>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} {...tableSort.tableSortProps} onSortChange={(sortBy, sortDirection) => { tableSort.tableSortProps.onSortChange(sortBy, sortDirection); setPage(0); }} columns={[
        { key: 'ediMessageId', label: 'Сообщение' },
        { key: 'stage', label: 'Этап' },
        { key: 'status', label: 'Статус' },
        { key: 'details', label: 'Детали' },
        { key: 'createdAt', label: 'Дата', render: (r) => fmtDate(r.createdAt) },
      ]} />}
    </Stack>
  );
}
