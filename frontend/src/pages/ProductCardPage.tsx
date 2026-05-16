import { Box, Button, Card, CardContent, Chip, Grid2 as Grid, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { productsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { operationStatusLabels, operationTypeLabels } from '../types/enums';
import { fmtDate } from '../utils/format';

export function ProductCardPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const { warehouseId, selectedWarehouse } = useWarehouseContext();
  const query = useQuery({
    queryKey: ['product-card', id, warehouseId],
    queryFn: () => productsApi.card(id, { warehouseId: warehouseId || undefined }),
    enabled: Number.isFinite(id),
  });

  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorState message={getErrorMessage(query.error)} />;

  const card = query.data!;
  const product = card.product;

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1, minWidth: 260 }}>
          <Typography variant="h4">{product.sku}</Typography>
          <Typography color="text.secondary">{product.name}</Typography>
        </Box>
        <Chip label={selectedWarehouse ? selectedWarehouse.code : 'All warehouses'} />
        <Button onClick={() => navigate('/products')}>Back</Button>
      </Box>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Total" value={card.totalQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Available" value={card.totalAvailableQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Reserved" value={card.totalReservedQuantity} /></Grid>
        <Grid size={{ xs: 12, md: 3 }}><Metric title="Min level" value={product.minStockLevel ?? 0} /></Grid>
      </Grid>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>Main information</Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Category" value={product.category} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Barcode" value={product.barcode} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Unit" value={product.unitOfMeasure} /></Grid>
            <Grid size={{ xs: 12, md: 3 }}><Info title="Dimensions" value={`${product.lengthCm ?? 0} x ${product.widthCm ?? 0} x ${product.heightCm ?? 0} cm`} /></Grid>
          </Grid>
        </CardContent>
      </Card>

      <Typography variant="h6">Warehouse aggregate</Typography>
      <ResourceTable
        rows={card.warehouseAggregates}
        total={card.warehouseAggregates.length}
        page={0}
        size={card.warehouseAggregates.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'warehouseCode', label: 'Warehouse' },
          { key: 'quantity', label: 'Total' },
          { key: 'reservedQuantity', label: 'Reserved' },
          { key: 'availableQuantity', label: 'Available' },
        ]}
      />

      <Typography variant="h6">Placements</Typography>
      <ResourceTable
        rows={card.placements}
        total={card.placements.length}
        page={0}
        size={card.placements.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        columns={[
          { key: 'warehouseCode', label: 'Warehouse' },
          { key: 'cellCode', label: 'Cell' },
          { key: 'quantity', label: 'Total' },
          { key: 'reservedQuantity', label: 'Reserved' },
          { key: 'availableQuantity', label: 'Available' },
        ]}
      />

      <Typography variant="h6">Recent operations</Typography>
      <ResourceTable
        rows={card.recentOperations}
        total={card.recentOperations.length}
        page={0}
        size={card.recentOperations.length || 10}
        onPageChange={() => undefined}
        onSizeChange={() => undefined}
        onView={(operation) => navigate(`/operations/${operation.id}`)}
        columns={[
          { key: 'operationNumber', label: 'Operation' },
          { key: 'type', label: 'Type', render: (row) => operationTypeLabels[row.type] },
          { key: 'status', label: 'Status', render: (row) => operationStatusLabels[row.status] },
          { key: 'warehouseCode', label: 'Warehouse' },
          { key: 'createdAt', label: 'Created', render: (row) => fmtDate(row.createdAt) },
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
