import { Add, Delete } from '@mui/icons-material';
import { Alert, Button, Card, CardContent, FormControl, Grid2 as Grid, IconButton, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { ediApi, productsApi, warehousesApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';

type SimulatorLine = {
  source: 'mapping' | 'productId';
  mappingId?: number | '';
  productId?: number | '';
  quantity: number;
};

type SimulatorForm = {
  actor: 'supplier' | 'customer';
  partnerId: number | '';
  warehouseId: number | '';
  documentNumber?: string;
  items: SimulatorLine[];
};

type ReceiptForm = {
  messageId?: number | '';
  operationId?: number | '';
  documentNumber?: string;
};

export function EdiSimulatorPage() {
  const queryClient = useQueryClient();
  const { warehouseId } = useWarehouseContext();
  const [notice, setNotice] = useState('');
  const { control, register, handleSubmit, watch } = useForm<SimulatorForm>({
    defaultValues: {
      actor: 'supplier',
      warehouseId: warehouseId || '',
      items: [{ source: 'mapping', quantity: 1 }],
    },
  });
  const receiptForm = useForm<ReceiptForm>({ defaultValues: {} });
  const lines = useFieldArray({ control, name: 'items' });
  const actor = watch('actor');
  const partnerId = watch('partnerId');

  const partners = useQuery({ queryKey: ['edi-partners-simulator'], queryFn: () => ediApi.partners.list({ page: 0, size: 200, sort: 'code,asc' }) });
  const warehouses = useQuery({ queryKey: ['warehouses-simulator'], queryFn: () => warehousesApi.list({ page: 0, size: 200, sort: 'code,asc' }) });
  const products = useQuery({ queryKey: ['products-simulator'], queryFn: () => productsApi.list({ page: 0, size: 300, sort: 'sku,asc' }) });
  const mappings = useQuery({ queryKey: ['edi-mappings-simulator'], queryFn: () => ediApi.mappings.list({ page: 0, size: 500, sort: 'externalProductCode,asc' }) });

  const mutation = useMutation({
    mutationFn: (data: SimulatorForm) => {
      const body = {
        partnerId: Number(data.partnerId),
        warehouseId: Number(data.warehouseId),
        documentNumber: data.documentNumber || null,
        items: data.items.map((item) => ({
          mappingId: item.source === 'mapping' && item.mappingId ? Number(item.mappingId) : null,
          productId: item.source === 'productId' && item.productId ? Number(item.productId) : null,
          quantity: Number(item.quantity),
        })),
      };
      return data.actor === 'supplier' ? ediApi.simulateSupplier(body) : ediApi.simulateCustomer(body);
    },
    onSuccess: (message) => {
      setNotice(`EDI-сообщение создано и поставлено в очередь: #${message.id}`);
      queryClient.invalidateQueries({ queryKey: ['edi-messages'] });
      queryClient.invalidateQueries({ queryKey: ['edi-queue'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  const receiptMutation = useMutation({
    mutationFn: (data: ReceiptForm) => ediApi.simulateCustomerReceipt({
      messageId: data.messageId ? Number(data.messageId) : null,
      operationId: data.operationId ? Number(data.operationId) : null,
      documentNumber: data.documentNumber || null,
    }),
    onSuccess: (message) => {
      setNotice(`Подтверждение клиента принято, EDI-сообщение завершено: #${message.id}`);
      queryClient.invalidateQueries({ queryKey: ['edi-messages'] });
      queryClient.invalidateQueries({ queryKey: ['edi-queue'] });
      queryClient.invalidateQueries({ queryKey: ['operations'] });
    },
    onError: (error) => setNotice(getErrorMessage(error)),
  });

  const partnerMappings = mappings.data?.content.filter((mapping) => (
    !partnerId || mapping.partnerId === Number(partnerId)
  )) ?? [];

  return (
    <Stack spacing={2}>
      <Typography variant="h4">External messages</Typography>
      {notice && <Alert severity={notice.includes('создано') || notice.includes('принято') ? 'success' : 'error'} onClose={() => setNotice('')}>{notice}</Alert>}

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Имитация DESADV / ORDERS</Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="actor" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Кто отправляет</InputLabel><Select {...field} label="Кто отправляет"><MenuItem value="supplier">Поставщик: DESADV</MenuItem><MenuItem value="customer">Клиент: ORDERS</MenuItem></Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="partnerId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>EDI-партнер</InputLabel><Select {...field} label="EDI-партнер" value={field.value || ''}>{partners.data?.content.map((partner) => <MenuItem key={partner.id} value={partner.id}>{partner.code} - {partner.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="warehouseId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Склад</InputLabel><Select {...field} label="Склад" value={field.value || ''}>{warehouses.data?.content.map((warehouse) => <MenuItem key={warehouse.id} value={warehouse.id}>{warehouse.code} - {warehouse.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="Номер документа" {...register('documentNumber')} /></Grid>
            </Grid>

            <Stack spacing={1}>
              <Typography variant="h6">Товары</Typography>
              {lines.fields.map((line, index) => {
                const source = watch(`items.${index}.source`);
                return (
                  <Grid container spacing={2} key={line.id} alignItems="center">
                    <Grid size={{ xs: 12, md: 2 }}>
                      <Controller control={control} name={`items.${index}.source`} render={({ field }) => (
                        <FormControl fullWidth><InputLabel>Источник</InputLabel><Select {...field} label="Источник"><MenuItem value="mapping">Mapping</MenuItem><MenuItem value="productId">ID товара</MenuItem></Select></FormControl>
                      )} />
                    </Grid>
                    {source === 'mapping' ? (
                      <Grid size={{ xs: 12, md: 5 }}>
                        <Controller control={control} name={`items.${index}.mappingId`} render={({ field }) => (
                          <FormControl fullWidth><InputLabel>Mapping</InputLabel><Select {...field} label="Mapping" value={field.value || ''}>{partnerMappings.map((mapping) => <MenuItem key={mapping.id} value={mapping.id}>{mapping.partnerCode} / {mapping.externalProductCode} - {mapping.internalSku} · {mapping.internalProductName}</MenuItem>)}</Select></FormControl>
                        )} />
                      </Grid>
                    ) : (
                      <Grid size={{ xs: 12, md: 5 }}>
                        <Controller control={control} name={`items.${index}.productId`} render={({ field }) => (
                          <FormControl fullWidth><InputLabel>Складской товар</InputLabel><Select {...field} label="Складской товар" value={field.value || ''}>{products.data?.content.map((product) => <MenuItem key={product.id} value={product.id}>#{product.id} / {product.sku} - {product.name}</MenuItem>)}</Select></FormControl>
                        )} />
                      </Grid>
                    )}
                    <Grid size={{ xs: 6, md: 2 }}><TextField fullWidth label="Количество" type="number" {...register(`items.${index}.quantity`, { valueAsNumber: true })} /></Grid>
                    <Grid size={{ xs: 12, md: 1 }}><IconButton color="error" onClick={() => lines.remove(index)}><Delete /></IconButton></Grid>
                  </Grid>
                );
              })}
              <Button startIcon={<Add />} onClick={() => lines.append({ source: 'mapping', quantity: 1 })}>Добавить товар</Button>
            </Stack>

            <Button variant="contained" disabled={mutation.isPending} onClick={handleSubmit((data) => mutation.mutate(data))}>Отправить внешнее сообщение</Button>
          </Stack>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Stack spacing={2}>
            <Typography variant="h6">Подтверждение приема товара клиентом</Typography>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="ID EDI-сообщения ORDERS" type="number" {...receiptForm.register('messageId', { valueAsNumber: true })} /></Grid>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="ID операции OUTCOME" type="number" {...receiptForm.register('operationId', { valueAsNumber: true })} /></Grid>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="Номер подтверждения" {...receiptForm.register('documentNumber')} /></Grid>
            </Grid>
            <Button variant="contained" disabled={receiptMutation.isPending} onClick={receiptForm.handleSubmit((data) => receiptMutation.mutate(data))}>Подтвердить прием клиентом</Button>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
