import { Add, ArrowBack, Delete, Send } from '@mui/icons-material';
import { Alert, Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ediApi, operationsApi, productsApi, storageCellsApi, warehousesApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { ResourcePage } from '../components/ResourcePage';
import { ConfirmDialog } from '../components/feedback/ConfirmDialog';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { BoolChip } from '../components/tables/ResourceTable';
import { ResourceTable } from '../components/tables/ResourceTable';
import { canManageEdi } from '../utils/permissions';
import { fmtDate } from '../utils/format';
import { useTableSort } from '../utils/sorting';
import { ediMessageStatuses, ediMessageTypeLabels, ediMessageTypes, type EdiMessageStatus, type EdiMessageType } from '../types/enums';
import type { EdiMapping, EdiPartner, EdiQueueItem, Product, StockBalance, StorageCell } from '../types/api';

const inboundPayloadExamples: Record<EdiMessageType, string> = {
  DESADV: '{\n  "warehouseId": 1,\n  "documentDate": "2026-05-14",\n  "items": [\n    {\n      "externalProductCode": "SUPPLIER-SKU-001",\n      "quantity": 5,\n      "toCellId": 1,\n      "unitPrice": 10\n    }\n  ]\n}',
  ORDERS: '{\n  "warehouseId": 1,\n  "documentDate": "2026-05-14",\n  "items": [\n    {\n      "externalProductCode": "CUSTOMER-SKU-001",\n      "quantity": 2,\n      "fromCellId": 1,\n      "unitPrice": 10\n    }\n  ]\n}',
  ORDRSP: '{\n  "documentDate": "2026-05-14",\n  "items": []\n}',
};

export function EdiPartnersPage() {
  const navigate = useNavigate();
  const warehouses = useQuery({
    queryKey: ['edi-partner-form-warehouses'],
    queryFn: () => warehousesApi.list({ page: 0, size: 500, sort: 'code,asc' }),
  });

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
        { key: 'warehouses', label: 'Склады', render: (row) => row.warehouses?.map((warehouse) => warehouse.code).join(', ') || '—', sortKey: false },
        { key: 'inboundEnabled', label: 'Inbound', render: (r) => <BoolChip value={r.inboundEnabled} /> },
        { key: 'outboundEnabled', label: 'Outbound', render: (r) => <BoolChip value={r.outboundEnabled} /> },
      ]}
      fields={[
        { name: 'code', label: 'Код', required: true },
        { name: 'name', label: 'Название', required: true },
        { name: 'gln', label: 'GLN' },
        { name: 'counterpartyId', label: 'ID контрагента', type: 'number' },
        { name: 'warehouseIds', label: 'Склады', type: 'multiselect', options: warehouses.data?.content.map((warehouse) => ({ value: warehouse.id, label: `${warehouse.code} · ${warehouse.name}` })) ?? [] },
        { name: 'inboundEnabled', label: 'Входящие включены', type: 'checkbox' },
        { name: 'outboundEnabled', label: 'Исходящие включены', type: 'checkbox' },
        { name: 'isActive', label: 'Активно', type: 'checkbox' },
      ]}
      onView={(partner) => navigate(`/edi/partners/${partner.id}`)}
    />
  );
}

export function EdiPartnerCardPage() {
  const { id } = useParams();
  const partnerId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const allowed = canManageEdi(user?.role);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [editing, setEditing] = useState<EdiMapping | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<EdiMapping | null>(null);
  const [notice, setNotice] = useState('');
  const tableSort = useTableSort('externalProductCode', 'asc');

  const partner = useQuery({
    queryKey: ['edi-partner', partnerId],
    queryFn: () => ediApi.partners.get(partnerId),
    enabled: Number.isFinite(partnerId),
  });
  const mappings = useQuery({
    queryKey: ['edi-partner-mappings', partnerId, page, size, tableSort.sort, appliedSearch],
    queryFn: () => ediApi.mappings.list({ page, size, sort: tableSort.sort, partnerId, search: appliedSearch || undefined }),
    enabled: Number.isFinite(partnerId),
  });
  const products = useQuery({
    queryKey: ['edi-mapping-products'],
    queryFn: () => productsApi.list({ page: 0, size: 1000, sort: 'sku,asc' }),
    enabled: allowed,
  });

  const invalidateMappings = () => {
    queryClient.invalidateQueries({ queryKey: ['edi-partner-mappings', partnerId] });
    queryClient.invalidateQueries({ queryKey: ['edi-mappings'] });
    queryClient.invalidateQueries({ queryKey: ['edi-mappings-simulator'] });
    queryClient.invalidateQueries({ queryKey: ['edi-process-mappings'] });
  };

  const saveMapping = useMutation({
    mutationFn: (data: MappingFormData) => {
      const body = { partnerId, ...data };
      return editing?.id ? ediApi.mappings.update(editing.id, body) : ediApi.mappings.create(body);
    },
    onSuccess: () => {
      setNotice(editing ? 'Маппинг обновлен.' : 'Маппинг создан.');
      setFormOpen(false);
      setEditing(null);
      invalidateMappings();
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  const deleteMapping = useMutation({
    mutationFn: (mapping: EdiMapping) => ediApi.mappings.delete(mapping.id),
    onSuccess: () => {
      setNotice('Маппинг удален.');
      setDeleting(null);
      invalidateMappings();
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  if (!Number.isFinite(partnerId)) {
    return <ErrorState message="Некорректный ID EDI-партнера" />;
  }

  return (
    <Stack spacing={2}>
      <Box display="flex" alignItems="center" gap={2} flexWrap="wrap">
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/edi/partners')}>К партнерам</Button>
        <Box sx={{ flex: 1, minWidth: 240 }}>
          <Typography variant="h4">Карточка EDI-партнера</Typography>
          {partner.data && <Typography color="text.secondary">{partner.data.code} · {partner.data.name}</Typography>}
        </Box>
        {allowed && <Button variant="contained" startIcon={<Add />} onClick={() => { setEditing(null); setFormOpen(true); }}>Создать маппинг</Button>}
      </Box>

      {notice && <Alert severity={notice.includes('создан') || notice.includes('обновлен') || notice.includes('удален') ? 'success' : 'error'} onClose={() => setNotice('')}>{notice}</Alert>}
      {partner.isLoading && <LoadingState />}
      {partner.isError && <ErrorState message={getErrorMessage(partner.error)} />}
      {partner.data && <EdiPartnerSummary partner={partner.data} />}
      {!allowed && <Alert severity="info">Ваша роль разрешает просмотр, но не изменение маппингов партнера.</Alert>}

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Box display="flex" gap={1} flexWrap="wrap" alignItems="center">
              <TextField
                label="Поиск маппинга"
                placeholder="Внешний код, SKU, товар..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                size="small"
                sx={{ flex: '1 1 280px', maxWidth: 520 }}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    setAppliedSearch(search.trim());
                    setPage(0);
                  }
                }}
              />
              <Button variant="outlined" onClick={() => { setAppliedSearch(search.trim()); setPage(0); }}>Найти</Button>
              {appliedSearch && <Button onClick={() => { setSearch(''); setAppliedSearch(''); setPage(0); }}>Сбросить</Button>}
            </Box>
            {mappings.isLoading && <LoadingState />}
            {mappings.isError && <ErrorState message={getErrorMessage(mappings.error)} />}
            {mappings.data && (
              <ResourceTable
                rows={mappings.data.content}
                total={mappings.data.totalElements}
                page={page}
                size={size}
                onPageChange={setPage}
                onSizeChange={(next) => { setSize(next); setPage(0); }}
                {...tableSort.tableSortProps}
                onSortChange={(sortBy, sortDirection) => {
                  tableSort.tableSortProps.onSortChange(sortBy, sortDirection);
                  setPage(0);
                }}
                onEdit={allowed ? (mapping) => { setEditing(mapping); setFormOpen(true); } : undefined}
                onDelete={allowed ? setDeleting : undefined}
                columns={[
                  { key: 'externalProductCode', label: 'Внешний код товара' },
                  { key: 'internalSku', label: 'Внутренний SKU' },
                  { key: 'internalProductName', label: 'Название' },
                  { key: 'isActive', label: 'Активность', render: (row) => <BoolChip value={row.isActive} /> },
                ]}
              />
            )}
          </Stack>
        </CardContent>
      </Card>

      <MappingDialog
        open={formOpen}
        editing={editing}
        products={products.data?.content ?? []}
        onClose={() => { setFormOpen(false); setEditing(null); }}
        onSubmit={(data) => saveMapping.mutateAsync(data)}
      />
      <ConfirmDialog
        open={!!deleting}
        title="Удалить маппинг?"
        text="Маппинг будет отключен и перестанет использоваться при обработке EDI."
        onClose={() => setDeleting(null)}
        onConfirm={() => deleting && deleteMapping.mutate(deleting)}
      />
    </Stack>
  );
}

function EdiPartnerSummary({ partner }: { partner: EdiPartner }) {
  return (
    <Card>
      <CardContent>
        <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: 'repeat(4, 1fr)' }} gap={2}>
          <InfoItem label="Код" value={partner.code} />
          <InfoItem label="Название" value={partner.name} />
          <InfoItem label="GLN" value={partner.gln || '—'} />
          <InfoItem label="Контрагент" value={partner.counterpartyName || '—'} />
          <InfoItem label="Склады" value={partner.warehouses?.map((warehouse) => warehouse.code).join(', ') || '—'} />
          <InfoItem label="Входящие" value={<BoolChip value={partner.inboundEnabled} />} />
          <InfoItem label="Исходящие" value={<BoolChip value={partner.outboundEnabled} />} />
          <InfoItem label="Активность" value={<BoolChip value={partner.isActive} />} />
        </Box>
      </CardContent>
    </Card>
  );
}

function InfoItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography component="div" fontWeight={600}>{value}</Typography>
    </Box>
  );
}

type MappingFormData = {
  externalProductCode: string;
  internalProductId: number;
  isActive: boolean;
};

function MappingDialog({
  open,
  editing,
  products,
  onClose,
  onSubmit,
}: {
  open: boolean;
  editing: EdiMapping | null;
  products: Product[];
  onClose: () => void;
  onSubmit: (data: MappingFormData) => Promise<unknown>;
}) {
  const [externalProductCode, setExternalProductCode] = useState('');
  const [internalProductId, setInternalProductId] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setExternalProductCode(editing?.externalProductCode ?? '');
    setInternalProductId(editing?.internalProductId ? String(editing.internalProductId) : '');
    setIsActive(editing?.isActive !== false);
    setError('');
    setSubmitting(false);
  }, [editing, open]);

  const submit = async () => {
    const productId = Number(internalProductId);
    if (!externalProductCode.trim()) {
      setError('Укажите внешний код товара.');
      return;
    }
    if (!Number.isFinite(productId) || productId <= 0) {
      setError('Выберите внутренний товар.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await onSubmit({ externalProductCode: externalProductCode.trim(), internalProductId: productId, isActive });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{editing ? 'Редактирование маппинга' : 'Создание маппинга'}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}
          <TextField label="Внешний код товара" value={externalProductCode} onChange={(event) => setExternalProductCode(event.target.value)} required fullWidth />
          <FormControl fullWidth>
            <InputLabel>Внутренний товар</InputLabel>
            <Select label="Внутренний товар" value={internalProductId} onChange={(event) => setInternalProductId(String(event.target.value))}>
              {products.map((product) => <MenuItem key={product.id} value={product.id}>{product.sku} · {product.name}</MenuItem>)}
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel>Активность</InputLabel>
            <Select label="Активность" value={isActive ? 'true' : 'false'} onChange={(event) => setIsActive(event.target.value === 'true')}>
              <MenuItem value="true">Активно</MenuItem>
              <MenuItem value="false">Выключено</MenuItem>
            </Select>
          </FormControl>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Отмена</Button>
        <Button variant="contained" disabled={submitting} onClick={submit}>Сохранить</Button>
      </DialogActions>
    </Dialog>
  );
}

export function EdiMessagesPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [type, setType] = useState('');
  const [ediStatus, setEdiStatus] = useState('');
  const tableSort = useTableSort('id', 'desc');
  const query = useQuery({ queryKey: ['edi-messages', page, size, type, ediStatus, tableSort.sort], queryFn: () => ediApi.messages({ page, size, sort: tableSort.sort, type: type || undefined, ediStatus: ediStatus || undefined }) });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">EDI-сообщения</Typography>
      <Box display="flex" gap={2} flexWrap="wrap">
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Тип</InputLabel><Select label="Тип" value={type} onChange={(e) => setType(e.target.value)}><MenuItem value="">Все</MenuItem>{ediMessageTypes.map((v) => <MenuItem key={v} value={v}>{ediMessageTypeLabels[v]}</MenuItem>)}</Select></FormControl>
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Статус EDI</InputLabel><Select label="Статус EDI" value={ediStatus} onChange={(e) => setEdiStatus(e.target.value)}><MenuItem value="">Все</MenuItem>{ediMessageStatuses.map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      </Box>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} {...tableSort.tableSortProps} onSortChange={(sortBy, sortDirection) => { tableSort.tableSortProps.onSortChange(sortBy, sortDirection); setPage(0); }} columns={[
        { key: 'id', label: 'ID' },
        { key: 'messageType', label: 'Тип' },
        { key: 'status', label: 'Статус EDI' },
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
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [ediStatus, setEdiStatus] = useState<EdiMessageStatus | ''>((searchParams.get('ediStatus') as EdiMessageStatus | null) || '');
  const [selected, setSelected] = useState<EdiQueueItem | null>(null);
  const [allocations, setAllocations] = useState<AllocationState>({});
  const [notice, setNotice] = useState('');
  const tableSort = useTableSort('id', 'desc');

  useEffect(() => {
    setEdiStatus((searchParams.get('ediStatus') as EdiMessageStatus | null) || '');
    setPage(0);
  }, [searchParams]);

  const query = useQuery({ queryKey: ['edi-queue', page, size, ediStatus, tableSort.sort], queryFn: () => ediApi.queue({ page, size, sort: tableSort.sort, ediStatus: ediStatus || undefined }) });
  const selectedPayload = parsePayload(selected?.normalizedPayload);
  const cells = useQuery({
    queryKey: ['edi-process-cells', selectedPayload?.warehouseId],
    queryFn: () => storageCellsApi.list({ page: 0, size: 500, sort: 'code,asc', warehouseId: selectedPayload?.warehouseId }),
    enabled: !!selectedPayload?.warehouseId,
  });
  const products = useQuery({
    queryKey: ['edi-process-products'],
    queryFn: () => productsApi.list({ page: 0, size: 1000, sort: 'sku,asc' }),
    enabled: !!selected,
  });
  const mappings = useQuery({
    queryKey: ['edi-process-mappings'],
    queryFn: () => ediApi.mappings.list({ page: 0, size: 1000, sort: 'externalProductCode,asc' }),
    enabled: !!selected,
  });
  const stockBalances = useQuery({
    queryKey: ['edi-process-stock-balances', selectedPayload?.warehouseId],
    queryFn: () => operationsApi.stockBalances({ page: 0, size: 1000, warehouseId: selectedPayload?.warehouseId }),
    enabled: !!selectedPayload?.warehouseId,
  });
  const processingWarnings = useMemo(() => {
    if (!selectedPayload?.items) return [];
    return selectedPayload.items
      .map((item, index) => {
        const product = resolvePayloadProduct(item, selected, products.data?.content ?? [], mappings.data?.content ?? []);
        return product ? null : `Строка ${index + 1}: товар не найден в справочнике или маппинге`;
      })
      .filter(Boolean) as string[];
  }, [mappings.data?.content, products.data?.content, selected, selectedPayload?.items]);
  const canCreateDocument = !!selectedPayload?.items?.length
    && processingWarnings.length === 0
    && selectedPayload.items.every((item, index) => {
      const requiredQuantity = Math.max(1, Number(item.quantity) || 1);
      const rows = allocations[index] ?? [];
      const product = resolvePayloadProduct(item, selected, products.data?.content ?? [], mappings.data?.content ?? []);
      return rows.length > 0
        && rows.every((row) => row.cellId && Number(row.quantity) > 0)
        && rows.reduce((sum, row) => sum + (Number(row.quantity) || 0), 0) === requiredQuantity
        && rows.every((row) => {
          const quantity = Number(row.quantity) || 0;
          if (!product) return false;
          if (selected?.messageType === 'DESADV') {
            const cell = cells.data?.content.find((candidate) => String(candidate.id) === row.cellId);
            return !!cell && isIncomeCellSuitable(product, cell, stockBalances.data?.content ?? [], quantity);
          }
          return (stockBalances.data?.content ?? []).some((balance) => balance.productId === product.id
            && String(balance.cellId) === row.cellId
            && balance.availableQuantity >= quantity);
        });
    });
  const processMutation = useMutation({
    mutationFn: () => ediApi.process(selected!.id, {
      cellAssignments: Object.entries(allocations)
        .flatMap(([itemIndex, rows]) => rows
          .filter((row) => row.cellId && Number(row.quantity) > 0)
          .map((row) => ({ itemIndex: Number(itemIndex), cellId: Number(row.cellId), quantity: Number(row.quantity) }))),
    }),
    onSuccess: () => {
      setNotice('EDI-сообщение обработано, документ создан без изменения остатков.');
      setSelected(null);
      setAllocations({});
      queryClient.invalidateQueries({ queryKey: ['edi-queue'] });
      queryClient.invalidateQueries({ queryKey: ['edi-messages'] });
      queryClient.invalidateQueries({ queryKey: ['operations'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Очередь EDI</Typography>
      {notice && <Alert severity={notice.startsWith('EDI') ? 'success' : 'error'} onClose={() => setNotice('')}>{notice}</Alert>}
      <FormControl sx={{ maxWidth: 220 }}><InputLabel>Статус EDI</InputLabel><Select label="Статус EDI" value={ediStatus} onChange={(e) => setEdiStatus(e.target.value as EdiMessageStatus | '')}><MenuItem value="">Все</MenuItem>{ediMessageStatuses.map((v) => <MenuItem key={v} value={v}>{v}</MenuItem>)}</Select></FormControl>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && <ResourceTable rows={query.data.content} total={query.data.totalElements} page={page} size={size} onPageChange={setPage} onSizeChange={(n) => { setSize(n); setPage(0); }} {...tableSort.tableSortProps} onSortChange={(sortBy, sortDirection) => { tableSort.tableSortProps.onSortChange(sortBy, sortDirection); setPage(0); }} columns={[
        { key: 'id', label: 'ID' },
        { key: 'messageType', label: 'Тип' },
        { key: 'messageStatus', label: 'Статус EDI' },
        { key: 'messageRef', label: 'Ref' },
        { key: 'documentNumber', label: 'Документ' },
        { key: 'partnerCode', label: 'Партнер' },
        { key: 'attemptCount', label: 'Попытки' },
        { key: 'scheduledAt', label: 'Запланировано', render: (r) => fmtDate(r.scheduledAt) },
        { key: 'lastError', label: 'Ошибка' },
        { key: 'process', label: 'Обработка', sortKey: false, render: (row) => canProcessEdiMessage(row.messageStatus) ? <Button size="small" onClick={() => { setSelected(row); setAllocations({}); }}>Обработать</Button> : row.relatedOperationId ? <Button size="small" href={`/operations/${row.relatedOperationId}`}>Операция</Button> : '—' },
      ]} />}
      {selected && selectedPayload && (
        <Card>
          <CardContent>
            <Stack spacing={2}>
              <Typography variant="h6">Обработка сообщения #{selected.ediMessageId}</Typography>
              <Typography color="text.secondary">
                {selected.messageType === 'DESADV'
                  ? 'Выберите ячейки, куда положить товар. Остатки изменятся только после складской приемки операции.'
                  : 'Выберите ячейки списания с нужным товаром. Остатки спишутся только на шаге отправки товаров.'}
              </Typography>
              {processingWarnings.map((warning) => <Alert key={warning} severity="warning">{warning}</Alert>)}
              {(selectedPayload.items ?? []).map((item: EdiPayloadItem, index: number) => (
                <EdiAllocationRows
                  key={index}
                  index={index}
                  item={item}
                  queueItem={selected}
                  cells={cells.data?.content ?? []}
                  products={products.data?.content ?? []}
                  mappings={mappings.data?.content ?? []}
                  stockBalances={stockBalances.data?.content ?? []}
                  rows={allocations[index] ?? []}
                  setAllocations={setAllocations}
                />
              ))}
              <Box display="flex" gap={2}>
                <Button variant="contained" disabled={processMutation.isPending || !canCreateDocument} onClick={() => processMutation.mutate()}>Создать документ</Button>
                <Button onClick={() => { setSelected(null); setAllocations({}); }}>Отмена</Button>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}

function canProcessEdiMessage(status: EdiMessageStatus) {
  return status === 'RECEIVED' || status === 'NORMALIZED' || status === 'FAILED';
}

type EdiPayloadItem = {
  productId?: number | string | null;
  externalProductCode?: string | null;
  sku?: string | null;
  quantity?: number | string | null;
};

type EdiNormalizedPayload = {
  warehouseId?: number | string | null;
  items?: EdiPayloadItem[];
};

type AllocationRow = {
  cellId: string;
  quantity: number;
};

type AllocationState = Record<number, AllocationRow[]>;

function EdiAllocationRows({
  index,
  item,
  queueItem,
  cells,
  products,
  mappings,
  stockBalances,
  rows,
  setAllocations,
}: {
  index: number;
  item: EdiPayloadItem;
  queueItem: EdiQueueItem;
  cells: StorageCell[];
  products: Product[];
  mappings: EdiMapping[];
  stockBalances: StockBalance[];
  rows: AllocationRow[];
  setAllocations: (updater: (current: AllocationState) => AllocationState) => void;
}) {
  const product = resolvePayloadProduct(item, queueItem, products, mappings);
  const requiredQuantity = Math.max(1, Number(item.quantity) || 1);
  const displayedRows = rows.length ? rows : [{ cellId: '', quantity: requiredQuantity }];
  const allocatedQuantity = rows.reduce((sum, row) => sum + (Number(row.quantity) || 0), 0);
  const allocationOk = allocatedQuantity === requiredQuantity;

  const setRow = (rowIndex: number, patch: Partial<AllocationRow>) => {
    setAllocations((current) => {
      const nextRows = current[index]?.length ? [...current[index]] : [{ cellId: '', quantity: requiredQuantity }];
      nextRows[rowIndex] = { ...nextRows[rowIndex], ...patch };
      return { ...current, [index]: nextRows };
    });
  };

  const addRow = () => {
    setAllocations((current) => {
      const nextRows = current[index]?.length ? [...current[index]] : [];
      const alreadyAllocated = nextRows.reduce((sum, row) => sum + (Number(row.quantity) || 0), 0);
      nextRows.push({ cellId: '', quantity: Math.max(1, requiredQuantity - alreadyAllocated) });
      return { ...current, [index]: nextRows };
    });
  };

  const removeRow = (rowIndex: number) => {
    setAllocations((current) => {
      const nextRows = (current[index] ?? []).filter((_, currentIndex) => currentIndex !== rowIndex);
      return { ...current, [index]: nextRows };
    });
  };

  return (
    <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 2 }}>
      <Stack spacing={1.5}>
        <Box display="flex" justifyContent="space-between" gap={2} flexWrap="wrap">
          <Typography fontWeight={600}>
            {item.sku || item.externalProductCode || `productId ${item.productId}`} · {requiredQuantity} шт.
          </Typography>
          <Typography color={allocationOk ? 'success.main' : 'warning.main'}>
            Распределено: {allocatedQuantity || 0} / {requiredQuantity}
          </Typography>
        </Box>
        <Typography color="text.secondary">Строка {index + 1}{product ? ` · ${product.sku} · ${product.name}` : ''}</Typography>
        {displayedRows.map((row, rowIndex) => {
          const options = queueItem.messageType === 'DESADV'
            ? getIncomeCellOptions(product, cells, stockBalances, Number(row.quantity) || 1)
            : getOutcomeCellOptions(product, stockBalances);
          return (
            <Box key={rowIndex} display="grid" gridTemplateColumns={{ xs: '1fr', md: 'minmax(220px, 2fr) 140px 48px' }} gap={1.5} alignItems="center">
              <FormControl fullWidth>
                <InputLabel>{queueItem.messageType === 'DESADV' ? 'Куда положить' : 'Откуда взять'}</InputLabel>
                <Select
                  label={queueItem.messageType === 'DESADV' ? 'Куда положить' : 'Откуда взять'}
                  value={row.cellId}
                  onChange={(event) => setRow(rowIndex, { cellId: String(event.target.value) })}
                >
                  {options.map((option) => (
                    <MenuItem key={option.cellId} value={option.cellId}>{option.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <TextField
                label="Количество"
                type="number"
                inputProps={{ min: 1 }}
                value={row.quantity}
                onChange={(event) => setRow(rowIndex, { quantity: Number(event.target.value) })}
              />
              <IconButton aria-label="Удалить ячейку" onClick={() => removeRow(rowIndex)} disabled={!rows.length}>
                <Delete />
              </IconButton>
            </Box>
          );
        })}
        <Box>
          <Button size="small" variant="outlined" onClick={addRow}>Добавить ячейку</Button>
        </Box>
      </Stack>
    </Box>
  );
}

function parsePayload(payload?: string): EdiNormalizedPayload | null {
  if (!payload) return null;
  try {
    return JSON.parse(payload);
  } catch {
    return null;
  }
}

function resolvePayloadProduct(
  item: EdiPayloadItem,
  queueItem: EdiQueueItem | null,
  products: Product[],
  mappings: EdiMapping[],
) {
  const productId = Number(item.productId);
  if (Number.isFinite(productId) && productId > 0) {
    return products.find((product) => product.id === productId);
  }

  if (!item.externalProductCode || !queueItem) return undefined;
  const mapping = mappings.find((candidate) => candidate.isActive !== false
    && candidate.externalProductCode === item.externalProductCode
    && (!queueItem.partnerCode || candidate.partnerCode === queueItem.partnerCode));
  return mapping ? products.find((product) => product.id === mapping.internalProductId) : undefined;
}

function getIncomeCellOptions(
  product: Product | undefined,
  cells: StorageCell[],
  stockBalances: StockBalance[],
  quantity: number,
) {
  if (!product) return [];
  return cells
    .filter((cell) => isIncomeCellSuitable(product, cell, stockBalances, quantity))
    .map((cell) => ({
      cellId: String(cell.id),
      label: `${cell.warehouseCode}/${cell.code} · свободно ${remainingUnits(cell, stockBalances)} ед.`,
    }));
}

function getOutcomeCellOptions(
  product: Product | undefined,
  stockBalances: StockBalance[],
) {
  if (!product) return [];
  return stockBalances
    .filter((balance) => balance.productId === product.id && balance.availableQuantity > 0)
    .map((balance) => ({
      cellId: String(balance.cellId),
      label: `${balance.warehouseCode}/${balance.cellCode} · доступно ${balance.availableQuantity} ед.`,
    }));
}

function isIncomeCellSuitable(
  product: Product,
  cell: StorageCell,
  stockBalances: StockBalance[],
  quantity: number,
) {
  if (cell.isActive === false) return false;
  if (remainingUnits(cell, stockBalances) < quantity) return false;
  if (!dimensionFits(product.lengthCm, cell.lengthCm)) return false;
  if (!dimensionFits(product.widthCm, cell.widthCm)) return false;
  if (!dimensionFits(product.heightCm, cell.heightCm)) return false;
  if (!limitFits(cell.maxWeightKg, cell.currentWeightKg, product.weightPerUnitKg, quantity)) return false;
  return limitFits(cell.maxVolumeCm3, cell.currentVolumeCm3, product.volumePerUnitCm3, quantity);
}

function remainingUnits(cell: StorageCell, stockBalances: StockBalance[]) {
  const capacity = Number(cell.capacityUnits) || 0;
  const occupied = stockBalances
    .filter((balance) => balance.cellId === cell.id)
    .reduce((sum, balance) => sum + (Number(balance.quantity) || 0), 0);
  return Math.max(0, capacity - occupied);
}

function dimensionFits(productSize?: number, cellSize?: number) {
  const productValue = Number(productSize) || 0;
  const cellValue = Number(cellSize) || 0;
  return productValue <= 0 || cellValue <= 0 || productValue <= cellValue;
}

function limitFits(maxLimit?: number, currentValue?: number, perUnit?: number, quantity = 1) {
  const max = Number(maxLimit) || 0;
  const current = Number(currentValue) || 0;
  const increment = (Number(perUnit) || 0) * quantity;
  return max <= 0 ? increment <= 0 : current + increment <= max;
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
        { key: 'status', label: 'Статус аудита' },
        { key: 'details', label: 'Детали' },
        { key: 'createdAt', label: 'Дата', render: (r) => fmtDate(r.createdAt) },
      ]} />}
    </Stack>
  );
}
