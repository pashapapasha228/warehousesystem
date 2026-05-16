import { Alert, Box, Button, Card, CardContent, Chip, Grid2 as Grid, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { useAuth } from '../auth/useAuth';
import { operationStatusLabels, operationTypeLabels } from '../types/enums';
import { fmtDate } from '../utils/format';
import { canManageCatalogs } from '../utils/permissions';

export function ProductCardPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { warehouseId, selectedWarehouse } = useWarehouseContext();
  const [minimums, setMinimums] = useState<Record<number, number>>({});
  const [notice, setNotice] = useState('');
  const query = useQuery({
    queryKey: ['product-card', id, warehouseId],
    queryFn: () => productsApi.card(id, { warehouseId: warehouseId || undefined }),
    enabled: Number.isFinite(id),
  });
  const categories = useQuery({ queryKey: ['product-categories'], queryFn: productsApi.categories });
  const saveMinimum = useMutation({
    mutationFn: ({ targetWarehouseId, minStockLevel }: { targetWarehouseId: number; minStockLevel: number }) => productsApi.setWarehouseMinStock(id, { warehouseId: targetWarehouseId, minStockLevel }),
    onSuccess: () => {
      setNotice('Минимальный остаток сохранен.');
      queryClient.invalidateQueries({ queryKey: ['product-card', id] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['report-low-stock'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  useEffect(() => {
    if (!query.data) return;
    setMinimums(Object.fromEntries(query.data.minStockLevels.map((row) => [row.warehouseId, row.minStockLevel])));
  }, [query.data]);

  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorState message={getErrorMessage(query.error)} />;

  const card = query.data!;
  const product = card.product;
  const categoryLabel = categories.data?.find((category) => category.code === product.category)?.label ?? product.category;
  const canEditMinimums = canManageCatalogs(user?.role);

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1, minWidth: 260 }}>
          <Typography variant="h4">{product.sku}</Typography>
          <Typography color="text.secondary">{product.name}</Typography>
        </Box>
        <Chip label={selectedWarehouse ? selectedWarehouse.code : 'Все склады'} />
        <Button onClick={() => navigate('/products')}>Назад</Button>
      </Box>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Всего" value={card.totalQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Доступно" value={card.totalAvailableQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="В резерве" value={card.totalReservedQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Порогов задано" value={card.minStockLevels.filter((row) => row.minStockLevel > 0).length} /></Grid>
      </Grid>
      {notice && <Alert severity={notice.includes('сохранен') ? 'success' : 'error'} onClose={() => setNotice('')}>{notice}</Alert>}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>Основная информация</Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Категория" value={categoryLabel} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Штрихкод" value={product.barcode} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Габариты" value={`${product.lengthCm ?? 0} x ${product.widthCm ?? 0} x ${product.heightCm ?? 0} см`} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Расчетный объем" value={`${product.volumePerUnitCm3 ?? 0} см3`} /></Grid>
          </Grid>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>Минимальный остаток по складам</Typography>
          <Stack spacing={1.5}>
            {card.minStockLevels.map((row) => (
              <Box key={row.warehouseId} display="grid" gridTemplateColumns={{ xs: '1fr', md: '1fr 180px 120px' }} gap={1.5} alignItems="center">
                <Typography>{row.warehouseCode} · {row.warehouseName}</Typography>
                <TextField
                  label="Минимум, шт."
                  type="number"
                  size="small"
                  disabled={!canEditMinimums}
                  inputProps={{ min: 0 }}
                  value={minimums[row.warehouseId] ?? 0}
                  onChange={(event) => setMinimums((current) => ({ ...current, [row.warehouseId]: Number(event.target.value) }))}
                />
                <Button
                  variant="outlined"
                  disabled={!canEditMinimums || saveMinimum.isPending}
                  onClick={() => saveMinimum.mutate({ targetWarehouseId: row.warehouseId, minStockLevel: Math.max(0, Number(minimums[row.warehouseId]) || 0) })}
                >
                  Сохранить
                </Button>
              </Box>
            ))}
          </Stack>
        </CardContent>
      </Card>

      <Typography variant="h6">Агрегат по складам</Typography>
      <ResourceTable
        rows={card.warehouseAggregates}
        total={card.warehouseAggregates.length}
        page={0}
        size={card.warehouseAggregates.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'warehouseCode', label: 'Склад' },
          { key: 'quantity', label: 'Всего' },
          { key: 'reservedQuantity', label: 'В резерве' },
          { key: 'availableQuantity', label: 'Доступно' },
          { key: 'minStockLevel', label: 'Минимум' },
        ]}
      />

      <Typography variant="h6">Размещение по ячейкам</Typography>
      <ResourceTable
        rows={card.placements}
        total={card.placements.length}
        page={0}
        size={card.placements.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'warehouseCode', label: 'Склад' },
          { key: 'cellCode', label: 'Ячейка' },
          { key: 'quantity', label: 'Всего' },
          { key: 'reservedQuantity', label: 'В резерве' },
          { key: 'availableQuantity', label: 'Доступно' },
        ]}
      />

      <Typography variant="h6">Последние операции</Typography>
      <ResourceTable
        rows={card.recentOperations}
        total={card.recentOperations.length}
        page={0}
        size={card.recentOperations.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        onView={(operation) => navigate(`/operations/${operation.id}`)}
        columns={[
          { key: 'operationNumber', label: 'Операция' },
          { key: 'type', label: 'Тип', render: (row) => operationTypeLabels[row.type] },
          { key: 'status', label: 'Статус', render: (row) => operationStatusLabels[row.status] },
          { key: 'warehouseCode', label: 'Склад' },
          { key: 'createdAt', label: 'Создана', render: (row) => fmtDate(row.createdAt) },
        ]}
      />
    </Stack>
  );
}

function Metric({ title, value }: { title: string; value: number }) {
  return (
    <Card>
      <CardContent>
        <Typography color="text.secondary">{title}</Typography>
        <Typography variant="h4">{value}</Typography>
      </CardContent>
    </Card>
  );
}

function Info({ title, value }: { title: string; value?: string | number | null }) {
  return (
    <Box>
      <Typography color="text.secondary">{title}</Typography>
      <Typography>{value || '-'}</Typography>
    </Box>
  );
}
