import {
  AddTask,
  AssignmentTurnedIn,
  ErrorOutline,
  Inbox,
  Inventory2,
  LocalShipping,
  MoveDown,
  OpenInNew,
  PlaylistAddCheck,
  Scale,
  Speed,
  Warehouse,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  Divider,
  FormControl,
  Grid2 as Grid,
  InputLabel,
  LinearProgress,
  MenuItem,
  Select,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { reportsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { operationStatusLabels, operationTypeLabels } from '../types/enums';
import type {
  AttentionItem,
  DashboardCellLoad,
  DashboardEdiProblem,
  DashboardRecentOperation,
  DashboardReport,
  DashboardStockWarning,
  DashboardWorkQueueItem,
} from '../types/api';
import { fmtDate, n } from '../utils/format';

type PeriodDays = 1 | 7 | 30;
type KpiTone = 'primary' | 'success' | 'warning' | 'error' | 'neutral';

const periodOptions: Array<{ label: string; value: PeriodDays }> = [
  { label: 'Сегодня', value: 1 },
  { label: '7 дней', value: 7 },
  { label: '30 дней', value: 30 },
];

const statusColor: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error'> = {
  DRAFT: 'warning',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
  FAILED: 'error',
  RECEIVED: 'warning',
  NORMALIZED: 'warning',
  PROCESSING: 'primary',
  PROCESSED: 'success',
};

export function DashboardPage() {
  const { warehouseId, setWarehouseId, warehouses, selectedWarehouse } = useWarehouseContext();
  const [periodDays, setPeriodDays] = useState<PeriodDays>(1);
  const query = useQuery({
    queryKey: ['dashboard', warehouseId, periodDays],
    queryFn: () => reportsApi.dashboard({ warehouseId: warehouseId || undefined, periodDays }),
  });

  const data = query.data;
  const periodLabel = periodOptions.find((option) => option.value === periodDays)?.label.toLowerCase() ?? 'сегодня';

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) auto' },
          gap: 2,
          alignItems: 'end',
        }}
      >
        <Box>
          <Typography variant="h4" fontWeight={800}>Панель управления</Typography>
          <Typography color="text.secondary">Операционное состояние склада, EDI и остатков</Typography>
        </Box>
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems={{ xs: 'stretch', md: 'center' }}>
          <FormControl size="small" sx={{ minWidth: { xs: '100%', md: 240 } }}>
            <InputLabel>Склад</InputLabel>
            <Select
              label="Склад"
              value={warehouseId ?? ''}
              onChange={(event) => setWarehouseId(event.target.value ? Number(event.target.value) : null)}
            >
              <MenuItem value="">Все склады</MenuItem>
              {warehouses.map((warehouse) => (
                <MenuItem key={warehouse.id} value={warehouse.id}>{warehouse.code} - {warehouse.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <ToggleButtonGroup
            exclusive
            size="small"
            value={periodDays}
            onChange={(_, value: PeriodDays | null) => value && setPeriodDays(value)}
            sx={{ '& .MuiToggleButton-root': { px: 1.5, whiteSpace: 'nowrap' } }}
          >
            {periodOptions.map((option) => (
              <ToggleButton key={option.value} value={option.value}>{option.label}</ToggleButton>
            ))}
          </ToggleButtonGroup>
        </Stack>
      </Box>

      <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
        <Button component={RouterLink} to="/operations/new?type=INCOME" variant="contained" startIcon={<AssignmentTurnedIn />}>Создать приемку</Button>
        <Button component={RouterLink} to="/operations/new?type=OUTCOME" variant="outlined" startIcon={<LocalShipping />}>Создать отгрузку</Button>
        <Button component={RouterLink} to="/operations/new?type=MOVE" variant="outlined" startIcon={<MoveDown />}>Создать перемещение</Button>
        <Button component={RouterLink} to="/edi/queue" variant="outlined" startIcon={<Inbox />}>Открыть EDI-очередь</Button>
      </Stack>

      {query.isError && (
        <Alert severity="error">
          Не удалось загрузить данные панели: {getErrorMessage(query.error)}
        </Alert>
      )}

      <Grid container spacing={2}>
        {data ? (
          buildKpis(data, periodLabel).map((item) => (
            <Grid key={item.title} size={{ xs: 12, sm: 6, lg: 3 }}>
              <KpiCard {...item} />
            </Grid>
          ))
        ) : (
          Array.from({ length: 8 }).map((_, index) => (
            <Grid key={index} size={{ xs: 12, sm: 6, lg: 3 }}>
              <Skeleton variant="rounded" height={142} />
            </Grid>
          ))
        )}
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 5 }}>
          <AttentionPanel items={data?.attentionItems} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, xl: 7 }}>
          <EdiPanel data={data} loading={query.isLoading} />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <QueuePanel
            title="Ожидаемая приемка"
            emptyText="Нет ожидаемых приемок"
            partnerLabel="Поставщик"
            documentLabel="Документ / сообщение"
            items={data?.receivingQueue}
            loading={query.isLoading}
          />
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <QueuePanel
            title="Отгрузка клиентам"
            emptyText="Нет заказов на отгрузку"
            partnerLabel="Клиент"
            documentLabel="Заказ / сообщение"
            items={data?.shippingQueue}
            loading={query.isLoading}
          />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 7 }}>
          <CellUtilizationPanel cells={data?.cellUtilization.topCells} report={data} loading={query.isLoading} />
        </Grid>
        <Grid size={{ xs: 12, xl: 5 }}>
          <StockWarningsPanel items={data?.stockWarnings} loading={query.isLoading} />
        </Grid>
      </Grid>

      <RecentOperationsPanel
        items={data?.recentOperations}
        loading={query.isLoading}
        subtitle={`${selectedWarehouse ? selectedWarehouse.code : 'Все склады'} · ${periodLabel}`}
      />
    </Stack>
  );
}

function buildKpis(data: DashboardReport, periodLabel: string) {
  const ediProblems = data.kpi.pendingEdi + data.kpi.failedEdi;
  const criticalStock = data.kpi.zeroStockProducts + data.kpi.belowMinProducts;
  return [
    {
      title: 'Ожидается приемка',
      value: data.kpi.expectedReceiving,
      caption: 'DESADV и черновики приемки',
      icon: <AssignmentTurnedIn />,
      tone: 'primary' as KpiTone,
      to: '/operations?type=INCOME&status=DRAFT',
    },
    {
      title: 'Готово к отгрузке',
      value: data.kpi.readyToShip,
      caption: 'ORDERS и черновики отгрузки',
      icon: <LocalShipping />,
      tone: 'success' as KpiTone,
      to: '/operations?type=OUTCOME&status=DRAFT',
    },
    {
      title: 'Проблемы EDI',
      value: ediProblems,
      caption: `${data.kpi.pendingEdi} в очереди · ${data.kpi.failedEdi} ошибок`,
      icon: <ErrorOutline />,
      tone: ediProblems > 0 ? 'error' as KpiTone : 'neutral' as KpiTone,
      to: '/edi/queue',
    },
    {
      title: 'Критические остатки',
      value: criticalStock,
      caption: `${data.kpi.zeroStockProducts} без остатка · ${data.kpi.belowMinProducts} ниже минимума`,
      icon: <Inventory2 />,
      tone: criticalStock > 0 ? 'warning' as KpiTone : 'neutral' as KpiTone,
      to: '/stock-balances',
    },
    {
      title: 'Загрузка по объему',
      value: `${n(data.kpi.averageVolumeUtilization)}%`,
      caption: 'Средняя загрузка ячеек',
      icon: <Speed />,
      tone: 'primary' as KpiTone,
      to: '/storage-cells',
    },
    {
      title: 'Загрузка по весу',
      value: `${n(data.kpi.averageWeightUtilization)}%`,
      caption: 'Средняя загрузка ячеек',
      icon: <Scale />,
      tone: 'primary' as KpiTone,
      to: '/storage-cells',
    },
    {
      title: 'Операции сегодня',
      value: data.kpi.completedOperations,
      caption: `Завершено за период: ${periodLabel}`,
      icon: <AddTask />,
      tone: 'success' as KpiTone,
      to: '/operations?status=COMPLETED',
    },
    {
      title: 'Черновики операций',
      value: data.kpi.draftOperations,
      caption: 'Незавершенные складские документы',
      icon: <PlaylistAddCheck />,
      tone: data.kpi.draftOperations > 0 ? 'warning' as KpiTone : 'neutral' as KpiTone,
      to: '/operations?status=DRAFT',
    },
  ];
}

function KpiCard({
  title,
  value,
  caption,
  icon,
  tone,
  to,
}: {
  title: string;
  value: number | string;
  caption: string;
  icon: ReactNode;
  tone: KpiTone;
  to: string;
}) {
  const paletteColor = tone === 'neutral' ? 'text.secondary' : `${tone}.main`;
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardActionArea component={RouterLink} to={to} sx={{ height: '100%' }}>
        <CardContent>
          <Stack spacing={1.25}>
            <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={2}>
              <Typography color="text.secondary" fontWeight={700}>{title}</Typography>
              <Box sx={{ color: paletteColor, display: 'flex' }}>{icon}</Box>
            </Box>
            <Typography variant="h4" fontWeight={900}>{value}</Typography>
            <Typography variant="body2" color="text.secondary">{caption}</Typography>
          </Stack>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}

function AttentionPanel({ items, loading }: { items?: AttentionItem[]; loading: boolean }) {
  return (
    <SectionCard title="Требует внимания" action={<Button component={RouterLink} to="/edi/queue" size="small">EDI-очередь</Button>}>
      {loading && <PanelSkeleton rows={5} />}
      {!loading && (!items || items.length === 0) && <Alert severity="success">Критических предупреждений нет</Alert>}
      {!loading && items?.map((item, index) => (
        <Box key={`${item.priority}-${item.type}-${item.title}-${index}`} sx={{ py: 1.25, borderBottom: 1, borderColor: 'divider' }}>
          <Stack direction="row" spacing={1.5} alignItems="flex-start">
            <Chip size="small" color={severityToColor(item.severity)} label={item.priority} sx={{ mt: 0.25, width: 32 }} />
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Typography fontWeight={800}>{item.title}</Typography>
              <Typography variant="body2" color="text.secondary">{item.detail}</Typography>
            </Box>
            <Button component={RouterLink} to={item.actionUrl} size="small" endIcon={<OpenInNew />}>{item.actionLabel}</Button>
          </Stack>
        </Box>
      ))}
    </SectionCard>
  );
}

function QueuePanel({
  title,
  emptyText,
  partnerLabel,
  documentLabel,
  items,
  loading,
}: {
  title: string;
  emptyText: string;
  partnerLabel: string;
  documentLabel: string;
  items?: DashboardWorkQueueItem[];
  loading: boolean;
}) {
  return (
    <SectionCard title={title}>
      {loading && <PanelSkeleton rows={4} />}
      {!loading && (!items || items.length === 0) && <Alert severity="success">{emptyText}</Alert>}
      {!loading && !!items?.length && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{partnerLabel}</TableCell>
              <TableCell>{documentLabel}</TableCell>
              <TableCell align="right">Позиций</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell align="right">Действие</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((item) => (
              <TableRow key={`${item.source}-${item.id}`} hover>
                <TableCell>{item.partnerName || '-'}</TableCell>
                <TableCell>
                  <Typography fontWeight={700}>{item.documentNumber || `#${item.id}`}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {item.messageType || (item.operationType ? operationTypeLabels[item.operationType] : item.source)}
                  </Typography>
                </TableCell>
                <TableCell align="right">{item.itemCount}</TableCell>
                <TableCell><StatusChip status={item.status} /></TableCell>
                <TableCell align="right">
                  <Button component={RouterLink} to={item.actionUrl} size="small">{item.actionLabel}</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </SectionCard>
  );
}

function EdiPanel({ data, loading }: { data?: DashboardReport; loading: boolean }) {
  const summary = data?.ediSummary;
  return (
    <SectionCard title="EDI-обмен" action={<Button component={RouterLink} to="/edi/messages" size="small">Внешние сообщения</Button>}>
      {loading && <PanelSkeleton rows={5} />}
      {!loading && summary && (
        <Stack spacing={2}>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr 1fr', md: 'repeat(4, 1fr)' }} gap={1.5}>
            <MiniMetric label="Получено сегодня" value={summary.receivedToday} />
            <MiniMetric label="В очереди" value={summary.queued} />
            <MiniMetric label="Обработано" value={summary.processed} />
            <MiniMetric label="С ошибкой" value={summary.failed} tone={summary.failed > 0 ? 'error' : 'success'} />
          </Box>
          <Divider />
          <Typography variant="subtitle2" color="text.secondary">Последние проблемные сообщения</Typography>
          {summary.problemMessages.length === 0 && <Alert severity="success">Ошибок EDI нет</Alert>}
          {summary.problemMessages.map((message) => (
            <EdiProblemRow key={message.id} message={message} />
          ))}
        </Stack>
      )}
    </SectionCard>
  );
}

function EdiProblemRow({ message }: { message: DashboardEdiProblem }) {
  return (
    <Box display="grid" gridTemplateColumns={{ xs: '1fr', sm: '1fr auto' }} gap={1} alignItems="center" sx={{ py: 1, borderBottom: 1, borderColor: 'divider' }}>
      <Box sx={{ minWidth: 0 }}>
        <Typography fontWeight={800}>{message.messageType} · {message.partnerName || '-'}</Typography>
        <Typography variant="body2" color="error.main" noWrap>{message.errorMessage || 'Ошибка не детализирована'}</Typography>
      </Box>
      <Button component={RouterLink} to={message.actionUrl} size="small" variant="outlined">Открыть</Button>
    </Box>
  );
}

function CellUtilizationPanel({
  cells,
  report,
  loading,
}: {
  cells?: DashboardCellLoad[];
  report?: DashboardReport;
  loading: boolean;
}) {
  const chartData = (cells ?? []).map((cell) => ({
    name: cell.warehouseCode ? `${cell.warehouseCode}/${cell.cellCode}` : cell.cellCode,
    volume: cell.volumePercent ?? 0,
    weight: cell.weightPercent ?? 0,
    maxLoad: Math.max(cell.volumePercent ?? 0, cell.weightPercent ?? 0),
  }));

  return (
    <SectionCard title="Загрузка ячеек" action={<Button component={RouterLink} to="/storage-cells" size="small">Все ячейки</Button>}>
      {loading && <Skeleton variant="rounded" height={360} />}
      {!loading && report && (
        <Stack spacing={2}>
          <Box display="grid" gridTemplateColumns={{ xs: '1fr 1fr', md: 'repeat(4, 1fr)' }} gap={1.5}>
            <MiniMetric label="Средняя загрузка по объему" value={`${n(report.cellUtilization.averageVolumePercent)}%`} />
            <MiniMetric label="Средняя загрузка по весу" value={`${n(report.cellUtilization.averageWeightPercent)}%`} />
            <MiniMetric label="Ячеек выше 80%" value={report.cellUtilization.cellsAbove80Percent} tone="warning" />
            <MiniMetric label="Ячеек выше 90%" value={report.cellUtilization.cellsAbove90Percent} tone={report.cellUtilization.cellsAbove90Percent ? 'error' : 'success'} />
          </Box>
          {chartData.length === 0 ? (
            <Alert severity="info">Нет данных по загрузке ячеек</Alert>
          ) : (
            <Box sx={{ height: 340 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 8, right: 16, left: -16, bottom: 48 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" angle={-35} textAnchor="end" interval={0} height={72} tick={{ fontSize: 11 }} />
                  <YAxis domain={[0, 100]} tickFormatter={(value) => `${value}%`} />
                  <Tooltip formatter={(value: number) => `${n(value)}%`} />
                  <Legend />
                  <Bar dataKey="volume" name="Загрузка по объему, %" fill="#1976d2" radius={[3, 3, 0, 0]} />
                  <Bar dataKey="weight" name="Загрузка по весу, %" fill="#ed6c02" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Box>
          )}
          <Stack spacing={1}>
            {(cells ?? []).slice(0, 5).map((cell) => {
              const value = Math.max(cell.volumePercent ?? 0, cell.weightPercent ?? 0);
              return (
                <Box key={`${cell.warehouseCode}-${cell.cellCode}`}>
                  <Box display="flex" justifyContent="space-between" gap={1}>
                    <Typography variant="body2" fontWeight={700}>{cell.warehouseCode ? `${cell.warehouseCode}/` : ''}{cell.cellCode}</Typography>
                    <Chip size="small" color={value >= 90 ? 'error' : value >= 80 ? 'warning' : 'default'} label={`${n(value)}%`} />
                  </Box>
                  <LinearProgress variant="determinate" color={value >= 90 ? 'error' : value >= 80 ? 'warning' : 'primary'} value={Math.min(100, value)} sx={{ mt: 0.75 }} />
                </Box>
              );
            })}
          </Stack>
        </Stack>
      )}
    </SectionCard>
  );
}

function StockWarningsPanel({ items, loading }: { items?: DashboardStockWarning[]; loading: boolean }) {
  const visibleItems = (items ?? []).slice(0, 8);
  return (
    <SectionCard title="Предупреждения по остаткам" action={<Button component={RouterLink} to="/stock-balances" size="small">Показать все</Button>}>
      {loading && <PanelSkeleton rows={5} />}
      {!loading && visibleItems.length === 0 && <Alert severity="success">Критических остатков нет</Alert>}
      {!loading && visibleItems.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Товар</TableCell>
              <TableCell>Склад</TableCell>
              <TableCell align="right">Остаток</TableCell>
              <TableCell align="right">Минимум</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell align="right">Действие</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleItems.map((item) => (
              <TableRow key={`${item.sku}-${item.warehouseId}`} hover>
                <TableCell>
                  <Typography fontWeight={700}>{item.productName}</Typography>
                  <Typography variant="caption" color="text.secondary">{item.sku}</Typography>
                </TableCell>
                <TableCell>{item.warehouseCode}</TableCell>
                <TableCell align="right">{item.currentStock}</TableCell>
                <TableCell align="right">{item.minLevel}</TableCell>
                <TableCell><Chip size="small" color={item.currentStock === 0 ? 'error' : 'warning'} label={item.status} /></TableCell>
                <TableCell align="right">
                  <Button component={RouterLink} to="/operations/new?type=INCOME" size="small">Создать приемку</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </SectionCard>
  );
}

function RecentOperationsPanel({
  items,
  loading,
  subtitle,
}: {
  items?: DashboardRecentOperation[];
  loading: boolean;
  subtitle: string;
}) {
  return (
    <SectionCard title="Последние операции" subtitle={subtitle} action={<Button component={RouterLink} to="/operations" size="small">Все операции</Button>}>
      {loading && <PanelSkeleton rows={5} />}
      {!loading && (!items || items.length === 0) && <Alert severity="info">За выбранный период операций нет</Alert>}
      {!loading && !!items?.length && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Время</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Номер</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Пользователь</TableCell>
              <TableCell align="right">Действие</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((item) => (
              <TableRow key={item.id} hover>
                <TableCell>{fmtDate(item.occurredAt)}</TableCell>
                <TableCell>{operationTypeLabels[item.type]}</TableCell>
                <TableCell>
                  <Typography fontWeight={700}>{item.operationNumber}</Typography>
                  <Typography variant="caption" color="text.secondary">{item.warehouseCode || '-'}</Typography>
                </TableCell>
                <TableCell><StatusChip status={item.status} /></TableCell>
                <TableCell>{item.username || '-'}</TableCell>
                <TableCell align="right">
                  <Button component={RouterLink} to={item.actionUrl} size="small">Открыть</Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </SectionCard>
  );
}

function SectionCard({
  title,
  subtitle,
  action,
  children,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardContent>
        <Stack spacing={2}>
          <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={2}>
            <Box>
              <Typography variant="h6" fontWeight={800}>{title}</Typography>
              {subtitle && <Typography variant="body2" color="text.secondary">{subtitle}</Typography>}
            </Box>
            {action}
          </Box>
          {children}
        </Stack>
      </CardContent>
    </Card>
  );
}

function MiniMetric({ label, value, tone = 'primary' }: { label: string; value: number | string; tone?: 'primary' | 'success' | 'warning' | 'error' }) {
  return (
    <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 1.25, minHeight: 76 }}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="h6" fontWeight={900} color={`${tone}.main`}>{value}</Typography>
    </Box>
  );
}

function StatusChip({ status }: { status: string }) {
  const label = status in operationStatusLabels ? operationStatusLabels[status as keyof typeof operationStatusLabels] : status;
  return <Chip size="small" color={statusColor[status] ?? 'default'} label={label} />;
}

function PanelSkeleton({ rows }: { rows: number }) {
  return (
    <Stack spacing={1}>
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} variant="rounded" height={42} />
      ))}
    </Stack>
  );
}

function severityToColor(severity: string): 'default' | 'success' | 'info' | 'warning' | 'error' {
  if (severity === 'success' || severity === 'info' || severity === 'warning' || severity === 'error') {
    return severity;
  }
  return 'default';
}
