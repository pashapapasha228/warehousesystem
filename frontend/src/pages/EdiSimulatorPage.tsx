import { Alert, Button, Card, CardContent, FormControl, Grid2 as Grid, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ediApi, productsApi, storageCellsApi, warehousesApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';

type SimulatorForm = {
  actor: 'supplier' | 'customer';
  partnerId: number | '';
  warehouseId: number | '';
  productId: number | '';
  quantity: number;
  cellId?: number | '';
  documentNumber?: string;
};

export function EdiSimulatorPage() {
  const queryClient = useQueryClient();
  const { warehouseId } = useWarehouseContext();
  const [notice, setNotice] = useState('');
  const { control, register, handleSubmit, watch } = useForm<SimulatorForm>({
    defaultValues: { actor: 'supplier', warehouseId: warehouseId || '', quantity: 1 },
  });
  const currentWarehouseId = watch('warehouseId') || warehouseId;
  const partners = useQuery({ queryKey: ['edi-partners-simulator'], queryFn: () => ediApi.partners.list({ page: 0, size: 200, sort: 'code,asc' }) });
  const warehouses = useQuery({ queryKey: ['warehouses-simulator'], queryFn: () => warehousesApi.list({ page: 0, size: 200, sort: 'code,asc' }) });
  const products = useQuery({ queryKey: ['products-simulator'], queryFn: () => productsApi.list({ page: 0, size: 300, sort: 'sku,asc' }) });
  const cells = useQuery({ queryKey: ['cells-simulator', currentWarehouseId], queryFn: () => storageCellsApi.list({ page: 0, size: 500, sort: 'code,asc', warehouseId: currentWarehouseId || undefined }) });
  const mutation = useMutation({
    mutationFn: (data: SimulatorForm) => {
      const body = {
        partnerId: Number(data.partnerId),
        warehouseId: Number(data.warehouseId),
        productId: Number(data.productId),
        quantity: Number(data.quantity),
        cellId: data.cellId ? Number(data.cellId) : null,
        documentNumber: data.documentNumber || null,
      };
      return data.actor === 'supplier' ? ediApi.simulateSupplier(body) : ediApi.simulateCustomer(body);
    },
    onSuccess: (message) => {
      setNotice(`Created EDI message #${message.id}`);
      queryClient.invalidateQueries({ queryKey: ['edi-messages'] });
      queryClient.invalidateQueries({ queryKey: ['edi-queue'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">External messages</Typography>
      {notice && <Alert severity={notice.startsWith('Created') ? 'success' : 'error'} onClose={() => setNotice('')}>{notice}</Alert>}
      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="actor" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Actor</InputLabel><Select {...field} label="Actor"><MenuItem value="supplier">Supplier DESADV</MenuItem><MenuItem value="customer">Customer ORDERS</MenuItem></Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="partnerId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>EDI partner</InputLabel><Select {...field} label="EDI partner" value={field.value || ''}>{partners.data?.content.map((partner) => <MenuItem key={partner.id} value={partner.id}>{partner.code} - {partner.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="warehouseId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Warehouse</InputLabel><Select {...field} label="Warehouse" value={field.value || ''}>{warehouses.data?.content.map((warehouse) => <MenuItem key={warehouse.id} value={warehouse.id}>{warehouse.code} - {warehouse.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="productId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Product</InputLabel><Select {...field} label="Product" value={field.value || ''}>{products.data?.content.map((product) => <MenuItem key={product.id} value={product.id}>{product.sku} - {product.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 2 }}><TextField fullWidth label="Quantity" type="number" {...register('quantity', { valueAsNumber: true })} /></Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <Controller control={control} name="cellId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Cell</InputLabel><Select {...field} label="Cell" value={field.value || ''}><MenuItem value="">Auto/default</MenuItem>{cells.data?.content.map((cell) => <MenuItem key={cell.id} value={cell.id}>{cell.warehouseCode}/{cell.code}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}><TextField fullWidth label="Document number" {...register('documentNumber')} /></Grid>
            </Grid>
            <Button variant="contained" disabled={mutation.isPending} onClick={handleSubmit((data) => mutation.mutate(data))}>Create simulated message</Button>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
