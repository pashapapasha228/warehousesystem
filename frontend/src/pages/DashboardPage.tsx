import { Alert, Card, CardContent, Grid2 as Grid, LinearProgress, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { reportsApi } from '../api/resourcesApi';
import { ErrorState, LoadingState } from '../components/feedback/StateViews';
import { getErrorMessage } from '../api/http';
import { fillPercent } from '../utils/format';

export function DashboardPage() {
  const query = useQuery({ queryKey: ['dashboard'], queryFn: reportsApi.dashboard });
  if (query.isLoading) return <LoadingState />;
  if (query.isError) return <ErrorState message={getErrorMessage(query.error)} />;
  const data = query.data!;
  const kpis = [
    ['Активные товары', data.activeProducts],
    ['Активные склады', data.activeWarehouses],
    ['Активные ячейки', data.activeStorageCells],
    ['Завершенные операции', data.completedOperations],
    ['Черновики', data.draftOperations],
    ['Общий остаток', data.totalStockQuantity],
    ['Ниже минимума', data.lowStockProducts],
    ['EDI pending/failed', `${data.pendingEdiMessages}/${data.failedEdiMessages}`],
  ];
  const chart = data.mostUtilizedCells.map((cell) => ({
    name: cell.cellCode,
    weight: fillPercent(cell.currentWeight, cell.maxWeight),
    volume: fillPercent(cell.currentVolume, cell.maxVolume),
  }));

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Панель управления</Typography>
      <Grid container spacing={2}>
        {kpis.map(([label, value]) => (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }} key={label}>
            <Card><CardContent><Typography color="text.secondary">{label}</Typography><Typography variant="h5">{value}</Typography></CardContent></Card>
          </Grid>
        ))}
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" mb={2}>Самые заполненные ячейки</Typography>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={chart}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="weight" name="Вес, %" fill="#1d5f8f" />
                  <Bar dataKey="volume" name="Объем, %" fill="#bf7f18" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" mb={2}>Предупреждения по остаткам</Typography>
              <Stack spacing={1}>
                {data.lowStockAlerts.length === 0 && <Alert severity="success">Критичных остатков нет.</Alert>}
                {data.lowStockAlerts.slice(0, 8).map((alert) => (
                  <Alert key={`${alert.sku}-${alert.warehouseCode}`} severity="warning">
                    {alert.productName} ({alert.sku}), склад {alert.warehouseCode}: {alert.currentStock} из минимума {alert.minLevel}
                    <LinearProgress sx={{ mt: 1 }} color="warning" variant="determinate" value={Math.min(100, (alert.currentStock / Math.max(alert.minLevel, 1)) * 100)} />
                  </Alert>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Stack>
  );
}
