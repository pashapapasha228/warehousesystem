import { Add, Delete } from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  FormControl,
  Grid2 as Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Controller, useFieldArray, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { counterpartiesApi, operationsApi, productsApi, storageCellsApi, warehousesApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { operationTypeLabels } from '../types/enums';

const optionalNumber = z.preprocess((value) => value === '' || value === undefined ? null : value, z.coerce.number().optional().nullable());

const schema = z.object({
  type: z.enum(['INCOME', 'OUTCOME', 'MOVE']),
  warehouseId: z.coerce.number().min(1),
  counterpartyId: optionalNumber,
  source: z.enum(['MANUAL', 'EDI']).default('MANUAL'),
  externalDocumentNumber: z.string().optional().nullable(),
  documentDate: z.string().optional().nullable(),
  comment: z.string().optional().nullable(),
  items: z.array(z.object({
    productId: z.coerce.number().min(1),
    quantity: z.coerce.number().min(1),
    unitPrice: optionalNumber,
    fromCellId: optionalNumber,
    toCellId: optionalNumber,
  })).min(1),
}).superRefine((data, ctx) => {
  data.items.forEach((item, index) => {
    if (data.type === 'MOVE' && !item.fromCellId) ctx.addIssue({ code: 'custom', path: ['items', index, 'fromCellId'], message: 'Выберите ячейку списания' });
    if ((data.type === 'INCOME' || data.type === 'MOVE') && !item.toCellId) ctx.addIssue({ code: 'custom', path: ['items', index, 'toCellId'], message: 'Выберите ячейку поступления' });
    if (data.type === 'MOVE' && item.fromCellId && item.toCellId && item.fromCellId === item.toCellId) ctx.addIssue({ code: 'custom', path: ['items', index, 'toCellId'], message: 'Ячейки должны отличаться' });
  });
});

type FormData = z.infer<typeof schema>;

export function OperationCreatePage() {
  const navigate = useNavigate();
  const { warehouseId } = useWarehouseContext();
  const { control, register, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema) as any,
    defaultValues: { type: 'INCOME', source: 'MANUAL', warehouseId: warehouseId || undefined, items: [{ quantity: 1 }] as any },
  });
  const items = useFieldArray({ control, name: 'items' });
  const type = watch('type');
  const products = useQuery({ queryKey: ['products-select'], queryFn: () => productsApi.list({ page: 0, size: 200, sort: 'name,asc' }) });
  const warehouses = useQuery({ queryKey: ['warehouses-select'], queryFn: () => warehousesApi.list({ page: 0, size: 200, sort: 'code,asc' }) });
  const counterparties = useQuery({ queryKey: ['counterparties-select'], queryFn: () => counterpartiesApi.list({ page: 0, size: 200, sort: 'name,asc' }) });
  const cells = useQuery({ queryKey: ['cells-select', warehouseId], queryFn: () => storageCellsApi.list({ page: 0, size: 500, sort: 'code,asc', warehouseId: warehouseId || undefined }) });

  const mutation = useMutation({
    mutationFn: operationsApi.create,
    onSuccess: (operation) => navigate(`/operations/${operation.id}`),
  });

  return (
    <Stack spacing={2}>
      <Typography variant="h4">Создание операции</Typography>
      <Stepper activeStep={0} alternativeLabel>
        {['Тип', 'Склад и контрагент', 'Товары', 'Ячейки', 'Проверка'].map((label) => <Step key={label}><StepLabel>{label}</StepLabel></Step>)}
      </Stepper>
      {mutation.isError && <Alert severity="error">{getErrorMessage(mutation.error)}</Alert>}
      <Card>
        <CardContent>
          <Stack spacing={3}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="type" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Тип операции</InputLabel><Select {...field} label="Тип операции">{Object.entries(operationTypeLabels).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="warehouseId" render={({ field }) => (
                  <FormControl fullWidth error={!!errors.warehouseId}><InputLabel>Склад</InputLabel><Select {...field} label="Склад" value={field.value || ''}>{warehouses.data?.content.map((w) => <MenuItem key={w.id} value={w.id}>{w.code} — {w.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="counterpartyId" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Контрагент</InputLabel><Select {...field} label="Контрагент" value={field.value || ''}><MenuItem value="">Без контрагента</MenuItem>{counterparties.data?.content.map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}</Select></FormControl>
                )} />
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="Номер внешнего документа" {...register('externalDocumentNumber')} /></Grid>
              <Grid size={{ xs: 12, md: 4 }}><TextField fullWidth label="Дата документа" type="date" InputLabelProps={{ shrink: true }} {...register('documentDate')} /></Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <Controller control={control} name="source" render={({ field }) => (
                  <FormControl fullWidth><InputLabel>Источник</InputLabel><Select {...field} label="Источник"><MenuItem value="MANUAL">Вручную</MenuItem><MenuItem value="EDI">EDI</MenuItem></Select></FormControl>
                )} />
              </Grid>
            </Grid>
            <TextField fullWidth label="Комментарий" multiline minRows={2} {...register('comment')} />
            <Divider />
            <Box display="flex" justifyContent="space-between" alignItems="center">
              <Typography variant="h6">Позиции</Typography>
              <Button startIcon={<Add />} onClick={() => items.append({ quantity: 1 } as any)}>Добавить товар</Button>
            </Box>
            {items.fields.map((item, index) => (
              <Grid container spacing={2} key={item.id} alignItems="center">
                <Grid size={{ xs: 12, md: 3 }}>
                  <Controller control={control} name={`items.${index}.productId`} render={({ field }) => (
                    <FormControl fullWidth error={!!errors.items?.[index]?.productId}><InputLabel>Товар</InputLabel><Select {...field} label="Товар" value={field.value || ''}>{products.data?.content.map((p) => <MenuItem key={p.id} value={p.id}>{p.sku} — {p.name}</MenuItem>)}</Select></FormControl>
                  )} />
                </Grid>
                <Grid size={{ xs: 6, md: 1.5 }}><TextField fullWidth label="Кол-во" type="number" {...register(`items.${index}.quantity`)} error={!!errors.items?.[index]?.quantity} /></Grid>
                <Grid size={{ xs: 6, md: 1.5 }}><TextField fullWidth label="Цена" type="number" {...register(`items.${index}.unitPrice`)} /></Grid>
                {(type === 'OUTCOME' || type === 'MOVE') && (
                  <Grid size={{ xs: 12, md: 2 }}>
                    <Controller control={control} name={`items.${index}.fromCellId`} render={({ field }) => (
                      <FormControl fullWidth error={!!errors.items?.[index]?.fromCellId}><InputLabel>Из ячейки</InputLabel><Select {...field} label="Из ячейки" value={field.value || ''}><MenuItem value="">Auto pick</MenuItem>{cells.data?.content.map((c) => <MenuItem key={c.id} value={c.id}>{c.warehouseCode}/{c.code}</MenuItem>)}</Select></FormControl>
                    )} />
                  </Grid>
                )}
                {(type === 'INCOME' || type === 'MOVE') && (
                  <Grid size={{ xs: 12, md: 2 }}>
                    <Controller control={control} name={`items.${index}.toCellId`} render={({ field }) => (
                      <FormControl fullWidth error={!!errors.items?.[index]?.toCellId}><InputLabel>В ячейку</InputLabel><Select {...field} label="В ячейку" value={field.value || ''}>{cells.data?.content.map((c) => <MenuItem key={c.id} value={c.id}>{c.warehouseCode}/{c.code}</MenuItem>)}</Select></FormControl>
                    )} />
                  </Grid>
                )}
                <Grid size={{ xs: 12, md: 0.5 }}><IconButton color="error" onClick={() => items.remove(index)}><Delete /></IconButton></Grid>
              </Grid>
            ))}
            <Box display="flex" gap={2} justifyContent="flex-end">
              <Button onClick={() => navigate('/operations')}>Отмена</Button>
              <Button variant="contained" disabled={mutation.isPending} onClick={handleSubmit((data) => mutation.mutate(data))}>Создать черновик</Button>
            </Box>
          </Stack>
        </CardContent>
      </Card>
    </Stack>
  );
}
