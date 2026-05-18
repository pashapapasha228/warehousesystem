import { Add, Refresh } from '@mui/icons-material';
import { Box, Button, Chip, FormControl, InputLabel, MenuItem, Select, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { operationsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { operationStatusLabels, operationTypeLabels, type OperationStatus, type OperationType } from '../types/enums';
import { fmtDate } from '../utils/format';
import { useTableSort } from '../utils/sorting';

export function OperationsPage() {
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [type, setType] = useState(searchParams.get('type') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const tableSort = useTableSort('id', 'desc');
  const navigate = useNavigate();
  const { warehouseId } = useWarehouseContext();

  useEffect(() => {
    setType(searchParams.get('type') || '');
    setStatus(searchParams.get('status') || '');
    setPage(0);
  }, [searchParams]);

  const query = useQuery({
    queryKey: ['operations', page, size, type, status, warehouseId, tableSort.sort],
    queryFn: () => operationsApi.list({ page, size, sort: tableSort.sort, type: type || undefined, status: status || undefined, warehouseId: warehouseId || undefined }),
  });

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1, minWidth: 220 }}>
          <Typography variant="h4">Операции</Typography>
          <Typography color="text.secondary">Приемка, отгрузка и внутренние перемещения</Typography>
        </Box>
        <Button startIcon={<Refresh />} onClick={() => query.refetch()}>Обновить</Button>
        <Button variant="contained" startIcon={<Add />} onClick={() => navigate('/operations/new')}>Создать операцию</Button>
      </Box>
      <Box display="flex" gap={2} flexWrap="wrap">
        <FormControl sx={{ minWidth: 180 }}>
          <InputLabel>Тип</InputLabel>
          <Select label="Тип" value={type} onChange={(e) => setType(e.target.value)}>
            <MenuItem value="">Все</MenuItem>
            {Object.entries(operationTypeLabels).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}
          </Select>
        </FormControl>
        <FormControl sx={{ minWidth: 180 }}>
          <InputLabel>Статус</InputLabel>
          <Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}>
            <MenuItem value="">Все</MenuItem>
            {Object.entries(operationStatusLabels).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}
          </Select>
        </FormControl>
      </Box>
      {query.isLoading && <LoadingState />}
      {query.isError && <ErrorState message={getErrorMessage(query.error)} />}
      {query.data && (
        <ResourceTable
          rows={query.data.content}
          total={query.data.totalElements}
          page={page}
          size={size}
          onPageChange={setPage}
          onSizeChange={(next) => { setSize(next); setPage(0); }}
          {...tableSort.tableSortProps}
          onSortChange={(sortBy, sortDirection) => {
            tableSort.tableSortProps.onSortChange(sortBy, sortDirection);
            setPage(0);
          }}
          onView={(row) => navigate(`/operations/${row.id}`)}
          columns={[
            { key: 'operationNumber', label: 'Номер' },
            { key: 'type', label: 'Тип', render: (r) => operationTypeLabels[r.type as OperationType] },
            { key: 'status', label: 'Статус', render: (r) => <Chip size="small" label={operationStatusLabels[r.status as OperationStatus]} color={r.status === 'COMPLETED' ? 'success' : r.status === 'CANCELLED' ? 'default' : 'warning'} /> },
            { key: 'warehouseCode', label: 'Склад' },
            { key: 'source', label: 'Источник' },
            { key: 'documentDate', label: 'Дата документа' },
            { key: 'createdAt', label: 'Создана', render: (r) => fmtDate(r.createdAt) },
          ]}
        />
      )}
    </Stack>
  );
}
