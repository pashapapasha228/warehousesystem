import { Download } from '@mui/icons-material';
import { Alert, Box, Button, Card, CardContent, Grid2 as Grid, Stack, TextField, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useState } from 'react';
import { reportsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useAuth } from '../auth/useAuth';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { ResourceTable } from '../components/tables/ResourceTable';
import { canUseReports } from '../utils/permissions';
import { isoDateTimeLocal, toBackendDateTime } from '../utils/format';

export function ReportsPage() {
  const { user } = useAuth();
  const now = new Date();
  const [start, setStart] = useState(isoDateTimeLocal(new Date(now.getTime() - 30 * 86400_000)));
  const [end, setEnd] = useState(isoDateTimeLocal(now));
  const params = { start: toBackendDateTime(start), end: toBackendDateTime(end) };

  const stock = useQuery({ queryKey: ['report-stock'], queryFn: reportsApi.stockBalance });
  const lowStock = useQuery({ queryKey: ['report-low-stock'], queryFn: reportsApi.lowStock });
  const enabled = canUseReports(user?.role);
  const top = useQuery({ queryKey: ['report-top', params], queryFn: () => reportsApi.topProducts({ ...params, topN: 10 }), enabled });
  const movement = useQuery({ queryKey: ['report-movement', params], queryFn: () => reportsApi.movement(params), enabled });
  const suppliers = useQuery({ queryKey: ['report-suppliers', params], queryFn: () => reportsApi.supplierStats(params), enabled });
  const cells = useQuery({ queryKey: ['report-cells'], queryFn: reportsApi.cellUtilization, enabled });
  const abc = useQuery({ queryKey: ['report-abc', params], queryFn: () => reportsApi.abc(params), enabled });
  const edi = useQuery({ queryKey: ['report-edi'], queryFn: reportsApi.ediStatistics, enabled });
  const audit = useQuery({ queryKey: ['report-audit', params], queryFn: () => reportsApi.audit({ ...params, page: 0, size: 20 }), enabled });

  if (stock.isLoading || lowStock.isLoading) return <LoadingState />;
  if (stock.isError) return <ErrorState message={getErrorMessage(stock.error)} />;

  const topRows = top.data?.topProducts ?? [];
  const cellRows = cells.data?.utilizations ?? [];
  const abcRows = abc.data?.analysis ?? [];

  return (
    <Stack spacing={2}>
      <Box display="flex" gap={2} alignItems="center" flexWrap="wrap">
        <Box sx={{ flex: 1, minWidth: 220 }}>
          <Typography variant="h4">Отчеты</Typography>
          <Typography color="text.secondary">Остатки доступны всем ролям, расширенная аналитика ADMIN/MANAGER.</Typography>
        </Box>
        {enabled && <Button startIcon={<Download />} onClick={() => reportsApi.exportExcel('stock-balance', params)}>XLSX</Button>}
        {enabled && <Button startIcon={<Download />} onClick={() => reportsApi.exportPdf('stock-balance', params)}>PDF</Button>}
      </Box>
      <Box display="flex" gap={2} flexWrap="wrap">
        <TextField label="Начало" type="datetime-local" value={start} onChange={(e) => setStart(e.target.value)} InputLabelProps={{ shrink: true }} />
        <TextField label="Окончание" type="datetime-local" value={end} onChange={(e) => setEnd(e.target.value)} InputLabelProps={{ shrink: true }} />
      </Box>
      {!enabled && <Alert severity="info">Для STOREKEEPER backend закрывает расширенные отчеты. Показаны доступные остатки и низкие остатки.</Alert>}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card><CardContent><Typography variant="h6" mb={2}>Низкие остатки</Typography>
            <ResourceTable rows={lowStock.data ?? []} total={(lowStock.data ?? []).length} page={0} size={10} onPageChange={() => undefined} onSizeChange={() => undefined} columns={[
              { key: 'sku', label: 'SKU' },
              { key: 'productName', label: 'Товар' },
              { key: 'currentStock', label: 'Остаток' },
              { key: 'minLevel', label: 'Минимум' },
            ]} />
          </CardContent></Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <Card><CardContent><Typography variant="h6" mb={2}>Остатки по ячейкам</Typography>
            <ResourceTable rows={stock.data?.balances ?? []} total={(stock.data?.balances ?? []).length} page={0} size={10} onPageChange={() => undefined} onSizeChange={() => undefined} columns={[
              { key: 'productSku', label: 'SKU' },
              { key: 'productName', label: 'Товар' },
              { key: 'quantity', label: 'Кол-во' },
              { key: 'cellCode', label: 'Ячейка' },
            ]} />
          </CardContent></Card>
        </Grid>
      </Grid>
      {enabled && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card><CardContent><Typography variant="h6" mb={2}>Топ товаров</Typography>
              {top.isError && <Alert severity="error">{getErrorMessage(top.error)}</Alert>}
              <ResponsiveContainer width="100%" height={260}><BarChart data={topRows}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="productSku" /><YAxis /><Tooltip /><Bar dataKey="totalQuantity" fill="#1d5f8f" /></BarChart></ResponsiveContainer>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}>
            <Card><CardContent><Typography variant="h6" mb={2}>ABC-анализ</Typography>
              <ResponsiveContainer width="100%" height={260}><PieChart><Pie data={abcRows} dataKey="totalQuantity" nameKey="productSku" fill="#bf7f18" label /></PieChart></ResponsiveContainer>
            </CardContent></Card>
          </Grid>
          <Grid size={{ xs: 12, lg: 6 }}><ReportJson title="Движение товаров" data={movement.data} error={movement.error} /></Grid>
          <Grid size={{ xs: 12, lg: 6 }}><ReportJson title="Статистика поставщиков" data={suppliers.data} error={suppliers.error} /></Grid>
          <Grid size={{ xs: 12, lg: 6 }}><ReportJson title="Заполненность ячеек" data={cellRows} error={cells.error} /></Grid>
          <Grid size={{ xs: 12, lg: 6 }}><ReportJson title="EDI-статистика" data={edi.data} error={edi.error} /></Grid>
          <Grid size={{ xs: 12 }}><ReportJson title="Аудит" data={audit.data} error={audit.error} /></Grid>
        </Grid>
      )}
    </Stack>
  );
}

function ReportJson({ title, data, error }: { title: string; data: unknown; error: unknown }) {
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" mb={2}>{title}</Typography>
        {error ? <Alert severity="error">{getErrorMessage(error)}</Alert> : <Box component="pre" className="mono" sx={{ whiteSpace: 'pre-wrap', bgcolor: 'background.default', p: 2, borderRadius: 1, maxHeight: 300, overflow: 'auto' }}>{JSON.stringify(data ?? {}, null, 2)}</Box>}
      </CardContent>
    </Card>
  );
}
