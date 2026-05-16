import { Box, Button, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { operationsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { fmtDate } from '../utils/format';
import { useTableSort } from '../utils/sorting';

export function StockBalancesPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const tableSort = useTableSort('productSku', 'asc');
  const { warehouseId } = useWarehouseContext();
  const query = useQuery({ queryKey: ['stock-balances', page, size, warehouseId, tableSort.sort], queryFn: () => operationsApi.stockBalances({ page, size, sort: tableSort.sort, warehouseId: warehouseId || undefined }) });
  return (
    <Stack spacing={2}>
      <Box display="flex" alignItems="center" gap={2}>
        <Box sx={{ flex: 1 }}><Typography variant="h4">Остатки</Typography><Typography color="text.secondary">Фактические, зарезервированные и доступные количества по ячейкам</Typography></Box>
        <Button onClick={() => query.refetch()}>Обновить</Button>
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
          columns={[
            { key: 'productSku', label: 'SKU' },
            { key: 'productName', label: 'Товар' },
            { key: 'warehouseCode', label: 'Склад' },
            { key: 'cellCode', label: 'Ячейка' },
            { key: 'quantity', label: 'Факт' },
            { key: 'reservedQuantity', label: 'Резерв' },
            { key: 'availableQuantity', label: 'Доступно' },
            { key: 'updatedAt', label: 'Обновлено', render: (r) => fmtDate(r.updatedAt) },
          ]}
        />
      )}
    </Stack>
  );
}
