import {
  Assessment,
  BarChart as BarChartIcon,
  Download,
  FilterAltOff,
  Inventory2,
  LocalShipping,
  Scale,
  SyncAlt,
  Warehouse,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid2 as Grid,
  LinearProgress,
  Paper,
  Skeleton,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState, type ReactNode } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { counterpartiesApi, ediApi, operationsApi, reportsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import { useAuth } from '../auth/useAuth';
import type {
  Counterparty,
  DashboardReport,
  EdiMessage,
  EdiQueueItem,
  LowStockAlert,
  Operation,
  OperationItem,
  StockBalance,
} from '../types/api';
import type { EdiMessageStatus, OperationSource, OperationStatus, OperationType } from '../types/enums';
import { canUseReports } from '../utils/permissions';
import { fillPercent, fmtDate, isoDateTimeLocal, n, toBackendDateTime } from '../utils/format';

type ReportType =
  | 'summary'
  | 'movement'
  | 'cells'
  | 'operations'
  | 'edi'
  | 'suppliers'
  | 'abc';

type Filters = {
  start: string;
  end: string;
  report: ReportType;
};

type KpiTone = 'primary' | 'success' | 'warning' | 'error' | 'neutral';

type ChartDatum = Record<string, string | number>;

const reportOptions: Array<{ value: ReportType; label: string; description: string }> = [
  { value: 'summary', label: 'Сводка по складу', description: 'SKU, остатки, резервы и загрузка' },
  { value: 'movement', label: 'Движение товаров', description: 'Приход, расход и перемещения за период' },
  { value: 'cells', label: 'Загрузка ячеек', description: 'Топ загруженных ячеек и средняя загрузка' },
  { value: 'operations', label: 'Операции', description: 'Складские документы и статусы' },
  { value: 'edi', label: 'EDI-обмен', description: 'Сообщения, очередь, ошибки и партнеры' },
  { value: 'suppliers', label: 'Поставщики', description: 'Активность поставок и EDI DESADV' },
  { value: 'abc', label: 'ABC-анализ', description: 'Значимость товаров по движению или стоимости' },
];

const operationTypeLabels: Record<OperationType, string> = {
  INCOME: 'Приход',
  OUTCOME: 'Расход',
  MOVE: 'Перемещение',
};

const operationStatusLabels: Record<OperationStatus, string> = {
  DRAFT: 'Черновик',
  SHIPPED: 'Отгружено',
  COMPLETED: 'Завершена',
  CANCELLED: 'Отменена',
};

const operationSourceLabels: Record<OperationSource | 'UNKNOWN', string> = {
  MANUAL: 'Вручную',
  EDI: 'Через EDI',
  UNKNOWN: 'Не указан',
};

const ediStatusLabels: Record<EdiMessageStatus, string> = {
  RECEIVED: 'Получено',
  NORMALIZED: 'Нормализовано',
  PROCESSING: 'Обрабатывается',
  PROCESSED: 'EDI обработано',
  COMPLETED: 'Завершено',
  FAILED: 'Ошибка',
};

const operationStatusColors: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error'> = {
  DRAFT: 'warning',
  SHIPPED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
  FAILED: 'error',
};

const abcColors: Record<string, string> = {
  A: '#1976d2',
  B: '#2e7d32',
  C: '#ed6c02',
};

const initialNow = new Date();
const initialStart = new Date(initialNow.getTime() - 30 * 86400_000);

export function ReportsPage() {
  const { user } = useAuth();
  const { warehouseId: contextWarehouseId } = useWarehouseContext();
  const [filters, setFilters] = useState<Filters>({
    start: isoDateTimeLocal(initialStart),
    end: isoDateTimeLocal(initialNow),
    report: 'summary',
  });
  const [appliedFilters, setAppliedFilters] = useState<Filters>(filters);
  const enabled = canUseReports(user?.role);
  const periodDays = getPeriodDays(appliedFilters.start, appliedFilters.end);
  const queryParams = {
    start: toBackendDateTime(appliedFilters.start),
    end: toBackendDateTime(appliedFilters.end),
  };
  const selectedWarehouseId = contextWarehouseId ?? undefined;

  const dashboardQuery = useQuery({
    queryKey: ['reports-dashboard', selectedWarehouseId, periodDays],
    queryFn: () => reportsApi.dashboard({ warehouseId: selectedWarehouseId, periodDays }),
  });
  const stockQuery = useQuery({
    queryKey: ['reports-stock-balances', selectedWarehouseId],
    queryFn: () => operationsApi.stockBalances({ page: 0, size: 500, warehouseId: selectedWarehouseId, sort: 'productSku,asc' }),
  });
  const lowStockQuery = useQuery({ queryKey: ['reports-low-stock'], queryFn: reportsApi.lowStock });
  const operationsQuery = useQuery({
    queryKey: ['reports-operations', selectedWarehouseId],
    queryFn: () => operationsApi.list({ page: 0, size: 500, warehouseId: selectedWarehouseId, sort: 'createdAt,desc' }),
    enabled,
  });
  const ediMessagesQuery = useQuery({
    queryKey: ['reports-edi-messages'],
    queryFn: () => ediApi.messages({ page: 0, size: 500, sort: 'receivedAt,desc' }),
    enabled,
  });
  const ediQueueQuery = useQuery({
    queryKey: ['reports-edi-queue'],
    queryFn: () => ediApi.queue({ page: 0, size: 500, sort: 'scheduledAt,desc' }),
    enabled,
  });
  const counterpartiesQuery = useQuery({
    queryKey: ['reports-counterparties'],
    queryFn: () => counterpartiesApi.list({ page: 0, size: 500, sort: 'name,asc' }),
    enabled,
  });
  const cellUtilizationQuery = useQuery({
    queryKey: ['reports-cell-utilization'],
    queryFn: reportsApi.cellUtilization,
    enabled,
  });
  const backendAbcQuery = useQuery({
    queryKey: ['reports-abc-backend', queryParams],
    queryFn: () => reportsApi.abc(queryParams),
    enabled,
  });
  const backendSuppliersQuery = useQuery({
    queryKey: ['reports-suppliers-backend', queryParams],
    queryFn: () => reportsApi.supplierStats(queryParams),
    enabled,
  });

  const operations = useMemo(
    () => filterOperationsByPeriod(operationsQuery.data?.content ?? [], appliedFilters.start, appliedFilters.end),
    [appliedFilters.end, appliedFilters.start, operationsQuery.data?.content],
  );
  const stockRows = useMemo(
    () => stockQuery.data?.content ?? [],
    [stockQuery.data?.content],
  );
  const lowStockRows = useMemo(
    () => (lowStockQuery.data ?? []).filter((row) => !selectedWarehouseId || row.warehouseId === selectedWarehouseId),
    [lowStockQuery.data, selectedWarehouseId],
  );
  const ediMessages = useMemo(
    () => filterEdiMessagesByPeriod(ediMessagesQuery.data?.content ?? [], appliedFilters.start, appliedFilters.end),
    [appliedFilters.end, appliedFilters.start, ediMessagesQuery.data?.content],
  );
  const ediQueue = ediQueueQuery.data?.content ?? [];
  const counterparties = counterpartiesQuery.data?.content ?? [];
  const cellRows = cellUtilizationQuery.data?.utilizations ?? [];
  const reportLabel = reportOptions.find((item) => item.value === appliedFilters.report)?.label ?? 'Отчет';
  const reportLoading = isReportLoading(appliedFilters.report, {
    dashboard: dashboardQuery.isLoading,
    stock: stockQuery.isLoading,
    lowStock: lowStockQuery.isLoading,
    operations: operationsQuery.isLoading,
    edi: ediMessagesQuery.isLoading || ediQueueQuery.isLoading,
    counterparties: counterpartiesQuery.isLoading,
    cells: cellUtilizationQuery.isLoading,
    backendAbc: backendAbcQuery.isLoading,
    backendSuppliers: backendSuppliersQuery.isLoading,
  });
  const reportError = getReportError(appliedFilters.report, {
    dashboard: dashboardQuery.error,
    stock: stockQuery.error,
    lowStock: lowStockQuery.error,
    operations: operationsQuery.error,
    edi: ediMessagesQuery.error ?? ediQueueQuery.error,
    counterparties: counterpartiesQuery.error,
    cells: cellUtilizationQuery.error,
    backendAbc: backendAbcQuery.error,
    backendSuppliers: backendSuppliersQuery.error,
  });
  const exportRows = useMemo(
    () => getExportRows(appliedFilters.report, {
      dashboard: dashboardQuery.data,
      stockRows,
      lowStockRows,
      operations,
      ediMessages,
      ediQueue,
      counterparties,
      cellRows,
      backendAbc: backendAbcQuery.data,
      backendSuppliers: backendSuppliersQuery.data,
    }),
    [
      appliedFilters.report,
      backendAbcQuery.data,
      backendSuppliersQuery.data,
      cellRows,
      counterparties,
      dashboardQuery.data,
      ediMessages,
      ediQueue,
      lowStockRows,
      operations,
      stockRows,
    ],
  );

  const handleReportChange = (_: React.SyntheticEvent, value: ReportType) => {
    setFilters((current) => ({ ...current, report: value }));
    setAppliedFilters((current) => ({ ...current, report: value }));
  };

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 1fr) auto' },
          gap: 2,
          alignItems: 'end',
        }}
      >
        <Box>
          <Typography variant="h4" fontWeight={800}>Отчеты</Typography>
          <Typography color="text.secondary">Аналитика по остаткам, операциям, ячейкам и EDI-обмену</Typography>
        </Box>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.25}>
          <Button
            variant="outlined"
            startIcon={<Download />}
            disabled={exportRows.length === 0}
            onClick={() => exportCsv(reportLabel, exportRows)}
          >
            Экспорт CSV
          </Button>
        </Stack>
      </Box>

      <Card sx={{ borderRadius: 1.5 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                size="small"
                label="Начало периода"
                type="datetime-local"
                value={filters.start}
                onChange={(event) => {
                  const start = event.target.value;
                  setFilters((current) => ({ ...current, start }));
                  setAppliedFilters((current) => ({ ...current, start }));
                }}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                fullWidth
                size="small"
                label="Конец периода"
                type="datetime-local"
                value={filters.end}
                onChange={(event) => {
                  const end = event.target.value;
                  setFilters((current) => ({ ...current, end }));
                  setAppliedFilters((current) => ({ ...current, end }));
                }}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {!enabled && appliedFilters.report !== 'summary' && (
        <Alert severity="info">Расширенные аналитические отчеты доступны ролям ADMIN и MANAGER.</Alert>
      )}

      <Card sx={{ borderRadius: 1.5 }}>
        <Tabs
          value={appliedFilters.report}
          onChange={handleReportChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ px: 1, borderBottom: 1, borderColor: 'divider' }}
        >
          {reportOptions.map((option) => (
            <Tab key={option.value} value={option.value} label={option.label} />
          ))}
        </Tabs>
      </Card>

      {Boolean(reportError) && <Alert severity="error">Не удалось загрузить отчет: {getErrorMessage(reportError)}</Alert>}
      {reportLoading && <ReportSkeleton />}
      {!reportLoading && !reportError && (
        <ReportContent
          report={appliedFilters.report}
          enabled={enabled}
          hasWarehouseFilter={!!selectedWarehouseId}
          dashboard={dashboardQuery.data}
          stockRows={stockRows}
          lowStockRows={lowStockRows}
          operations={operations}
          ediMessages={ediMessages}
          ediQueue={ediQueue}
          counterparties={counterparties}
          cellRows={cellRows}
          backendAbc={backendAbcQuery.data}
          backendSuppliers={backendSuppliersQuery.data}
        />
      )}
    </Stack>
  );
}

function ReportContent({
  report,
  enabled,
  hasWarehouseFilter,
  dashboard,
  stockRows,
  lowStockRows,
  operations,
  ediMessages,
  ediQueue,
  counterparties,
  cellRows,
  backendAbc,
  backendSuppliers,
}: {
  report: ReportType;
  enabled: boolean;
  hasWarehouseFilter: boolean;
  dashboard?: DashboardReport;
  stockRows: StockBalance[];
  lowStockRows: LowStockAlert[];
  operations: Operation[];
  ediMessages: EdiMessage[];
  ediQueue: EdiQueueItem[];
  counterparties: Counterparty[];
  cellRows: CellUtilizationItem[];
  backendAbc?: ABCAnalysisReport;
  backendSuppliers?: SupplierStatsReport;
}) {
  if (!enabled && report !== 'summary') {
    return <Alert severity="warning">Недостаточно прав для просмотра выбранного отчета.</Alert>;
  }

  if (report === 'summary') return <SummaryReport dashboard={dashboard} stockRows={stockRows} lowStockRows={lowStockRows} cellRows={cellRows} preferDashboardCells={hasWarehouseFilter} />;
  if (report === 'movement') return <MovementReportView operations={operations} />;
  if (report === 'cells') return <CellsReport dashboard={dashboard} cellRows={cellRows} preferDashboardCells={hasWarehouseFilter} />;
  if (report === 'operations') return <OperationsReport operations={operations} counterparties={counterparties} />;
  if (report === 'edi') return <EdiReport messages={ediMessages} queue={ediQueue} counterparties={counterparties} />;
  if (report === 'suppliers') return <SuppliersReport operations={operations} messages={ediMessages} counterparties={counterparties} backendSuppliers={backendSuppliers} />;
  return <ABCReport operations={operations} backendAbc={backendAbc} />;
}

function SummaryReport({
  dashboard,
  stockRows,
  lowStockRows,
  cellRows,
  preferDashboardCells,
}: {
  dashboard?: DashboardReport;
  stockRows: StockBalance[];
  lowStockRows: LowStockAlert[];
  cellRows: CellUtilizationItem[];
  preferDashboardCells: boolean;
}) {
  const totalQuantity = sum(stockRows, (row) => row.quantity);
  const totalReserved = sum(stockRows, (row) => row.reservedQuantity);
  const totalAvailable = sum(stockRows, (row) => row.availableQuantity);
  const uniqueSku = new Set(stockRows.map((row) => row.productSku)).size;
  const cellAnalytics = buildCellAnalytics(cellRows, dashboard, preferDashboardCells);
  const stockByWarehouse = groupStockByWarehouse(stockRows);
  const loadData = [
    { name: 'Средняя загрузка по объему', value: cellAnalytics.averageVolume },
    { name: 'Средняя загрузка по весу', value: cellAnalytics.averageWeight },
  ];

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="SKU" value={uniqueSku} caption="Уникальные товары в остатках" icon={<Inventory2 />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Фактический остаток" value={n(totalQuantity)} caption="Всего единиц на складе" icon={<Warehouse />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Резерв" value={n(totalReserved)} caption="Зарезервировано под операции" icon={<Assessment />} tone="warning" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Доступно" value={n(totalAvailable)} caption="Факт минус резерв" icon={<LocalShipping />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Ниже минимума" value={lowStockRows.length} caption="Позиции с дефицитом" icon={<BarChartIcon />} tone={lowStockRows.length ? 'warning' : 'neutral'} /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Ячейки > 80%" value={cellAnalytics.above80} caption="Высокая загрузка" icon={<Scale />} tone={cellAnalytics.above80 ? 'warning' : 'neutral'} /></Grid>
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <SectionCard title="Распределение остатков по складам">
            <BarPanel
              data={stockByWarehouse}
              bars={[{ key: 'quantity', name: 'Фактический остаток', color: '#1976d2' }, { key: 'reserved', name: 'Резерв', color: '#ed6c02' }]}
              emptyText="Нет остатков для выбранного склада или периода."
            />
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 5 }}>
          <SectionCard title="Загрузка по объему и весу" subtitle="Столбцы показывают средний процент использования лимитов ячеек">
            <BarPanel
              data={loadData}
              valueSuffix="%"
              yDomain={[0, 100]}
              bars={[{ key: 'value', name: 'Загрузка, %', color: '#2e7d32' }]}
              emptyText="Нет данных по загрузке ячеек."
            />
          </SectionCard>
        </Grid>
      </Grid>
    </Stack>
  );
}

function MovementReportView({ operations }: { operations: Operation[] }) {
  const completed = operations.filter((operation) => operation.status === 'COMPLETED');
  const movementRows = completed.flatMap(operationToMovementRows);
  const byDay = buildMovementByDay(completed);
  const incomeQty = quantityByType(completed, 'INCOME');
  const outcomeQty = quantityByType(completed, 'OUTCOME');
  const moveQty = quantityByType(completed, 'MOVE');
  const manualOperations = completed.filter((operation) => operationSource(operation) === 'MANUAL').length;
  const ediOperations = completed.filter((operation) => operationSource(operation) === 'EDI').length;

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Приход" value={n(incomeQty)} caption="Единиц товара" icon={<Inventory2 />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Расход" value={n(outcomeQty)} caption="Единиц товара" icon={<LocalShipping />} tone="warning" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Перемещения" value={n(moveQty)} caption="Единиц перемещено" icon={<SyncAlt />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Операций" value={completed.length} caption="Завершенные за период" icon={<Assessment />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Вручную" value={manualOperations} caption="Завершенные операции вручную" icon={<BarChartIcon />} tone="neutral" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Через EDI" value={ediOperations} caption="Завершенные операции EDI" icon={<SyncAlt />} tone="primary" /></Grid>
      </Grid>
      <SectionCard title="Приемки / отгрузки / перемещения по дням">
        <BarPanel
          data={byDay}
          bars={[
            { key: 'INCOME', name: 'Приход', color: '#2e7d32' },
            { key: 'OUTCOME', name: 'Расход', color: '#ed6c02' },
            { key: 'MOVE', name: 'Перемещение', color: '#1976d2' },
          ]}
          emptyText="За выбранный период нет завершенных движений."
        />
      </SectionCard>
      <SectionCard title="Детализация движения">
        <LimitedTable
          rows={movementRows}
          getKey={(row, index) => `${row.operationNumber}-${row.productSku}-${index}`}
          emptyText="Нет движений товаров за выбранный период."
          columns={[
            { label: 'Дата', render: (row) => fmtDate(row.date) },
            { label: 'Тип операции', render: (row) => operationTypeLabels[row.type] },
            { label: 'Товар', render: (row) => <ProductCell sku={row.productSku} name={row.productName} /> },
            { label: 'Склад', render: (row) => row.warehouseCode || '-' },
            { label: 'Количество', align: 'right', render: (row) => n(row.quantity) },
            { label: 'Источник', render: (row) => <SourceChip source={row.source} /> },
            { label: 'Пользователь', render: (row) => row.user },
            { label: 'Статус', render: (row) => <StatusChip status={row.status} /> },
          ]}
        />
      </SectionCard>
    </Stack>
  );
}

function LowStockReport({ rows, stockRows }: { rows: LowStockAlert[]; stockRows: StockBalance[] }) {
  const stockIndex = buildStockAggregateIndex(stockRows);
  const enriched = rows.map((row) => {
    const stock = stockIndex.get(stockKey(row.sku, row.warehouseId));
    const actual = stock?.quantity ?? row.currentStock;
    const reserved = stock?.reservedQuantity ?? 0;
    const available = stock?.availableQuantity ?? Math.max(actual - reserved, 0);
    const deficit = Math.max(row.minLevel - actual, 0);
    const status = actual <= 0 ? 'Нет остатка' : actual < row.minLevel ? 'Ниже минимума' : 'Норма';
    return { ...row, actual, reserved, available, deficit, status };
  });

  return (
    <SectionCard title="Остатки ниже минимума" subtitle="Это аналитический срез по дефицитам, не полный список остатков.">
      <LimitedTable
        rows={enriched}
        getKey={(row) => `${row.sku}-${row.warehouseId}`}
        emptyText="Позиции ниже минимума не найдены."
        columns={[
          { label: 'SKU', render: (row) => row.sku },
          { label: 'Товар', render: (row) => row.productName },
          { label: 'Склад', render: (row) => row.warehouseCode },
          { label: 'Фактический остаток', align: 'right', render: (row) => n(row.actual) },
          { label: 'Резерв', align: 'right', render: (row) => n(row.reserved) },
          { label: 'Доступно', align: 'right', render: (row) => n(row.available) },
          { label: 'Минимум', align: 'right', render: (row) => n(row.minLevel) },
          { label: 'Дефицит', align: 'right', render: (row) => n(row.deficit) },
          { label: 'Статус', render: (row) => <Chip size="small" color={row.status === 'Нет остатка' ? 'error' : row.status === 'Ниже минимума' ? 'warning' : 'success'} label={row.status} /> },
        ]}
      />
    </SectionCard>
  );
}

function CellsReport({ dashboard, cellRows, preferDashboardCells }: { dashboard?: DashboardReport; cellRows: CellUtilizationItem[]; preferDashboardCells: boolean }) {
  const analytics = buildCellAnalytics(cellRows, dashboard, preferDashboardCells);
  const topCells = analytics.cells.slice(0, 10);

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Средняя загрузка по объему" value={`${n(analytics.averageVolume)}%`} caption="Использование объема ячеек" icon={<Warehouse />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Средняя загрузка по весу" value={`${n(analytics.averageWeight)}%`} caption="Использование лимита веса" icon={<Scale />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Ячейки выше 80%" value={analytics.above80} caption="Требуют внимания" icon={<BarChartIcon />} tone={analytics.above80 ? 'warning' : 'neutral'} /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Ячейки выше 90%" value={analytics.above90} caption="Критическая загрузка" icon={<Assessment />} tone={analytics.above90 ? 'error' : 'neutral'} /></Grid>
      </Grid>
      <SectionCard title="Топ-10 наиболее загруженных ячеек" subtitle="Каждый столбец показывает процент загрузки по объему и по весу.">
        <BarPanel
          data={topCells}
          valueSuffix="%"
          yDomain={[0, 100]}
          bars={[
            { key: 'volumePercent', name: 'Загрузка по объему, %', color: '#1976d2' },
            { key: 'weightPercent', name: 'Загрузка по весу, %', color: '#ed6c02' },
          ]}
          emptyText="Нет данных по загрузке ячеек."
        />
      </SectionCard>
      <SectionCard title="Ячейки с высокой загрузкой">
        <LimitedTable
          rows={topCells}
          getKey={(row) => row.name}
          emptyText="Загруженных ячеек нет."
          columns={[
            { label: 'Ячейка', render: (row) => row.name },
            { label: 'Объем', render: (row) => <PercentBar value={row.volumePercent} /> },
            { label: 'Вес', render: (row) => <PercentBar value={row.weightPercent} /> },
            { label: 'Максимальная загрузка', render: (row) => <Chip size="small" color={row.maxPercent >= 90 ? 'error' : row.maxPercent >= 80 ? 'warning' : 'success'} label={`${n(row.maxPercent)}%`} /> },
          ]}
          initialLimit={10}
        />
      </SectionCard>
    </Stack>
  );
}

function OperationsReport({ operations, counterparties }: { operations: Operation[]; counterparties: Counterparty[] }) {
  const counterpartyMap = buildCounterpartyMap(counterparties);
  const typeCounts = countBy(operations, (operation) => operation.type);
  const statusCounts = countBy(operations, (operation) => operation.status);
  const sourceCounts = countBy(operations, (operation) => operationSource(operation));

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Приемки" value={typeCounts.INCOME ?? 0} caption="Вручную и через EDI" icon={<Inventory2 />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Отгрузки" value={typeCounts.OUTCOME ?? 0} caption="Вручную и через EDI" icon={<LocalShipping />} tone="warning" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Перемещения" value={typeCounts.MOVE ?? 0} caption="Внутренние операции" icon={<SyncAlt />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Через EDI" value={sourceCounts.EDI ?? 0} caption="Операции, оформленные EDI" icon={<SyncAlt />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Вручную" value={sourceCounts.MANUAL ?? 0} caption="Операции, созданные вручную" icon={<BarChartIcon />} tone="neutral" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Завершенные" value={statusCounts.COMPLETED ?? 0} caption="Завершенный статус" icon={<Assessment />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Черновики" value={statusCounts.DRAFT ?? 0} caption="Ожидают действия" icon={<BarChartIcon />} tone="warning" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}><KpiCard title="Отмененные" value={statusCounts.CANCELLED ?? 0} caption="Ошибочные или отмененные" icon={<FilterAltOff />} tone={statusCounts.CANCELLED ? 'error' : 'neutral'} /></Grid>
      </Grid>
      <SectionCard title="Складские операции">
        <LimitedTable
          rows={operations}
          getKey={(row) => row.id}
          emptyText="За выбранный период операций нет."
          columns={[
            { label: 'Номер операции', render: (row) => row.operationNumber },
            { label: 'Тип', render: (row) => operationTypeLabels[row.type] },
            { label: 'Склад', render: (row) => row.warehouseCode || '-' },
            { label: 'Контрагент', render: (row) => row.counterpartyId ? counterpartyMap.get(row.counterpartyId)?.name ?? `#${row.counterpartyId}` : '-' },
            { label: 'Источник', render: (row) => <SourceChip source={operationSource(row)} /> },
            { label: 'Статус', render: (row) => <StatusChip status={row.status} /> },
            { label: 'Позиций', align: 'right', render: (row) => row.items.length },
            { label: 'Дата создания', render: (row) => fmtDate(row.createdAt) },
            { label: 'Дата завершения', render: (row) => fmtDate(row.completedAt) },
          ]}
        />
      </SectionCard>
    </Stack>
  );
}

function EdiReport({ messages, queue, counterparties }: { messages: EdiMessage[]; queue: EdiQueueItem[]; counterparties: Counterparty[] }) {
  const partners = buildPartnerNameMap(counterparties);
  const statusCounts = countBy(messages, (message) => message.status);
  const typeCounts = {
    DESADV: messages.filter((message) => message.messageType === 'DESADV').length,
    ORDERS: messages.filter((message) => message.messageType === 'ORDERS').length,
    ORDRSP: messages.filter((message) => message.messageType === 'ORDRSP').length,
    RECADV: 0,
  };
  const byType = Object.entries(typeCounts).map(([name, value]) => ({ name, value }));
  const byPartner = topN(
    Object.entries(countBy(messages, (message) => message.partnerCode || 'Без партнера')).map(([name, value]) => ({ name, value })),
    (row) => row.value,
    10,
  );
  const problemMessages = messages
    .filter((message) => message.status === 'FAILED' || !!message.errorMessage)
    .sort((left, right) => dateValue(right.receivedAt) - dateValue(left.receivedAt));

  return (
    <Stack spacing={2}>
      <Alert severity="info">EDI-статус PROCESSED означает только успешную обработку EDI-сообщения. Он не подтверждает приемку или отгрузку товара на складе.</Alert>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Получено сообщений" value={messages.length} caption="За выбранный период" icon={<Assessment />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Обработано" value={(statusCounts.PROCESSED ?? 0) + (statusCounts.COMPLETED ?? 0)} caption="EDI успешно обработаны" icon={<BarChartIcon />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="В очереди" value={queue.length} caption="Текущая EDI-очередь" icon={<SyncAlt />} tone={queue.length ? 'warning' : 'neutral'} /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="С ошибкой" value={statusCounts.FAILED ?? 0} caption="FAILED сообщения" icon={<FilterAltOff />} tone={statusCounts.FAILED ? 'error' : 'neutral'} /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Партнеры" value={byPartner.length} caption="С EDI-активностью" icon={<Warehouse />} tone="neutral" /></Grid>
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 6 }}>
          <SectionCard title="Сообщения по типам">
            <BarPanel data={byType} bars={[{ key: 'value', name: 'Сообщений', color: '#1976d2' }]} emptyText="EDI-сообщений за период нет." />
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 6 }}>
          <SectionCard title="EDI по партнерам">
            <BarPanel data={byPartner} bars={[{ key: 'value', name: 'Сообщений', color: '#2e7d32' }]} emptyText="Нет активности по партнерам." />
          </SectionCard>
        </Grid>
      </Grid>
      <SectionCard title="Проблемные EDI">
        <LimitedTable
          rows={problemMessages}
          getKey={(row) => row.id}
          emptyText="Проблемных EDI-сообщений нет."
          columns={[
            { label: 'Тип', render: (row) => row.messageType },
            { label: 'Партнер', render: (row) => row.partnerCode ? partners.get(row.partnerCode) ?? row.partnerCode : '-' },
            { label: 'Направление', render: (row) => row.direction === 'INBOUND' ? 'Входящее' : 'Исходящее' },
            { label: 'Статус', render: (row) => <EdiStatusChip status={row.status} /> },
            { label: 'Ошибка', render: (row) => row.errorMessage || '-' },
            { label: 'Дата', render: (row) => fmtDate(row.receivedAt) },
          ]}
        />
      </SectionCard>
    </Stack>
  );
}

function SuppliersReport({
  operations,
  messages,
  counterparties,
  backendSuppliers,
}: {
  operations: Operation[];
  messages: EdiMessage[];
  counterparties: Counterparty[];
  backendSuppliers?: SupplierStatsReport;
}) {
  const supplierRows = buildSupplierRows(operations, messages, counterparties, backendSuppliers);
  const activeSuppliers = supplierRows.filter((row) => row.deliveryCount > 0 || row.desadvCount > 0);
  const topSuppliers = topN(supplierRows.map((row) => ({ name: row.supplierName, value: row.acceptedQuantity })), (row) => row.value, 10);

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Всего поставщиков" value={supplierRows.length} caption="Контрагенты типа SUPPLIER/BOTH" icon={<Warehouse />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Активных" value={activeSuppliers.length} caption="Были поставки или DESADV" icon={<Assessment />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Поставок" value={sum(supplierRows, (row) => row.deliveryCount)} caption="Завершенные INCOME" icon={<Inventory2 />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Вручную" value={sum(supplierRows, (row) => row.manualDeliveries)} caption="Приемки, созданные вручную" icon={<BarChartIcon />} tone="neutral" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Через EDI" value={sum(supplierRows, (row) => row.ediDeliveries)} caption="Приемки EDI" icon={<SyncAlt />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="Принято товаров" value={n(sum(supplierRows, (row) => row.acceptedQuantity))} caption="Количество по приемкам" icon={<LocalShipping />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="DESADV" value={sum(supplierRows, (row) => row.desadvCount)} caption="Входящие от поставщиков" icon={<SyncAlt />} tone="neutral" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2 }}><KpiCard title="EDI-ошибки" value={sum(supplierRows, (row) => row.ediErrors)} caption="Ошибки по поставщикам" icon={<FilterAltOff />} tone={sum(supplierRows, (row) => row.ediErrors) ? 'error' : 'neutral'} /></Grid>
      </Grid>
      <SectionCard title="Топ-10 поставщиков по принятому количеству">
        <BarPanel data={topSuppliers} bars={[{ key: 'value', name: 'Принятое количество', color: '#1976d2' }]} emptyText="Нет поставок за выбранный период." />
      </SectionCard>
      <SectionCard title="Статистика поставщиков" subtitle="Отчет строится по операциям INCOME, DESADV и контрагентам поставщиков. Расхождения не показываются, если их нельзя надежно посчитать из доступных данных.">
        <LimitedTable
          rows={supplierRows}
          getKey={(row) => row.supplierName}
          emptyText="Нет данных по поставщикам за выбранный период."
          columns={[
            { label: 'Поставщик', render: (row) => row.supplierName },
            { label: 'Поставок', align: 'right', render: (row) => n(row.deliveryCount) },
            { label: 'Вручную', align: 'right', render: (row) => n(row.manualDeliveries) },
            { label: 'Через EDI', align: 'right', render: (row) => n(row.ediDeliveries) },
            { label: 'Позиций', align: 'right', render: (row) => n(row.itemCount) },
            { label: 'Принятое количество', align: 'right', render: (row) => n(row.acceptedQuantity) },
            { label: 'DESADV', align: 'right', render: (row) => n(row.desadvCount) },
            { label: 'Ошибок EDI', align: 'right', render: (row) => n(row.ediErrors) },
            { label: 'Последняя поставка', render: (row) => fmtDate(row.lastDelivery) },
            { label: 'Статус', render: (row) => <Chip size="small" color={row.status === 'Активен' ? 'success' : 'default'} label={row.status} /> },
          ]}
        />
      </SectionCard>
    </Stack>
  );
}

function ABCReport({ operations, backendAbc }: { operations: Operation[]; backendAbc?: ABCAnalysisReport }) {
  const abcRows = buildAbcRows(operations, backendAbc);
  const totalValue = sum(abcRows, (row) => row.value);
  const classCounts = countBy(abcRows, (row) => row.abcClass);
  const classDistribution = ['A', 'B', 'C'].map((name) => ({ name, value: classCounts[name] ?? 0 }));
  const hasReliableCost = abcRows.some((row) => row.hasReliableCost);
  const abcValueLabel = hasReliableCost ? 'Стоимость движения' : 'Количество движения';
  const topProducts = topN(
    abcRows.map((row) => ({ name: row.productSku, productName: row.productName, value: row.value })),
    (row) => row.value,
    10,
  );

  if (abcRows.length === 0) {
    return <Alert severity="info">Недостаточно данных для ABC-анализа за выбранный период.</Alert>;
  }

  return (
    <Stack spacing={2}>
      <Alert severity="info">ABC-анализ распределяет товары по значимости на основе их вклада в общий объем или стоимость движения за выбранный период.</Alert>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Класс A" value={classCounts.A ?? 0} caption="Наиболее значимые товары" icon={<Assessment />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Класс B" value={classCounts.B ?? 0} caption="Средняя значимость" icon={<BarChartIcon />} tone="success" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Класс C" value={classCounts.C ?? 0} caption="Оставшийся хвост" icon={<Inventory2 />} tone="warning" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Доля класса A" value={`${n(round(share(sum(abcRows.filter((row) => row.abcClass === 'A'), (row) => row.value), totalValue)))}%`} caption="В общем движении" icon={<Scale />} tone="primary" /></Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 2.4 }}><KpiCard title="Товаров в анализе" value={abcRows.length} caption="SKU с движением" icon={<Warehouse />} tone="neutral" /></Grid>
      </Grid>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 5 }}>
          <SectionCard title="Распределение товаров по классам">
            <ClassDistribution data={classDistribution} />
          </SectionCard>
        </Grid>
        <Grid size={{ xs: 12, lg: 7 }}>
          <SectionCard title="Топ-10 товаров по движению" subtitle={`Цифровое значение на графике: ${abcValueLabel.toLowerCase()} за выбранный период.`}>
            <BarPanel data={topProducts} bars={[{ key: 'value', name: abcValueLabel, color: '#1976d2' }]} emptyText="Нет данных для графика." />
          </SectionCard>
        </Grid>
      </Grid>
      <SectionCard title="Таблица ABC-анализа" subtitle="A - первые примерно 80% накопленной доли, B - следующие 15%, C - оставшиеся позиции.">
        <SortableLimitedTable
          rows={abcRows}
          getKey={(row) => row.productSku}
          emptyText="Недостаточно данных для ABC-анализа за выбранный период."
          initialSort={{ key: 'value', direction: 'desc' }}
          columns={[
            { key: 'productSku', label: 'SKU', render: (row) => row.productSku, sortValue: (row) => row.productSku },
            { key: 'productName', label: 'Товар', render: (row) => row.productName, sortValue: (row) => row.productName },
            { key: 'movementQuantity', label: 'Количество движения', align: 'right', render: (row) => n(row.movementQuantity), sortValue: (row) => row.movementQuantity },
            { key: 'value', label: 'Стоимость движения', align: 'right', render: (row) => row.hasReliableCost ? n(row.value) : '-', sortValue: (row) => row.value },
            { key: 'sharePercent', label: 'Доля, %', align: 'right', render: (row) => `${n(row.sharePercent)}%`, sortValue: (row) => row.sharePercent },
            { key: 'cumulativePercent', label: 'Накопленная доля, %', align: 'right', render: (row) => `${n(row.cumulativePercent)}%`, sortValue: (row) => row.cumulativePercent },
            { key: 'abcClass', label: 'Класс ABC', render: (row) => <Chip size="small" label={row.abcClass} sx={{ bgcolor: abcColors[row.abcClass], color: 'common.white', fontWeight: 800 }} />, sortValue: (row) => row.abcClass },
          ]}
        />
      </SectionCard>
    </Stack>
  );
}

function KpiCard({
  title,
  value,
  caption,
  icon,
  tone,
}: {
  title: string;
  value: number | string;
  caption: string;
  icon: ReactNode;
  tone: KpiTone;
}) {
  const color = tone === 'neutral' ? 'text.secondary' : `${tone}.main`;
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardContent>
        <Stack spacing={1.25}>
          <Box display="flex" justifyContent="space-between" alignItems="flex-start" gap={1}>
            <Typography color="text.secondary" fontWeight={700}>{title}</Typography>
            <Box sx={{ color, display: 'flex' }}>{icon}</Box>
          </Box>
          <Typography variant="h4" fontWeight={900}>{value}</Typography>
          <Typography variant="body2" color="text.secondary">{caption}</Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function SectionCard({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardContent>
        <Stack spacing={2}>
          <Box>
            <Typography variant="h6" fontWeight={800}>{title}</Typography>
            {subtitle && <Typography variant="body2" color="text.secondary">{subtitle}</Typography>}
          </Box>
          {children}
        </Stack>
      </CardContent>
    </Card>
  );
}

function BarPanel({
  data,
  bars,
  valueSuffix = '',
  yDomain,
  emptyText,
}: {
  data: ChartDatum[];
  bars: Array<{ key: string; name: string; color: string }>;
  valueSuffix?: string;
  yDomain?: [number, number];
  emptyText: string;
}) {
  const visibleData = data.filter((row) => bars.some((bar) => Number(row[bar.key] ?? 0) > 0));

  if (visibleData.length === 0) {
    return <Alert severity="info">{emptyText}</Alert>;
  }

  return (
    <Box sx={{ width: '100%', height: 340 }}>
      <ResponsiveContainer>
        <BarChart data={visibleData} margin={{ top: 8, right: 16, bottom: 64, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="name" interval={0} angle={-35} textAnchor="end" height={84} tick={{ fontSize: 11 }} />
          <YAxis domain={yDomain} tickFormatter={(value) => `${value}${valueSuffix}`} />
          <Tooltip content={<ChartTooltip valueSuffix={valueSuffix} />} />
          <Legend />
          {bars.map((bar) => (
            <Bar key={bar.key} dataKey={bar.key} name={bar.name} fill={bar.color} radius={[3, 3, 0, 0]} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </Box>
  );
}

function ChartTooltip({
  active,
  payload,
  label,
  valueSuffix,
}: {
  active?: boolean;
  payload?: Array<{ name?: string; value?: number | string; payload?: ChartDatum }>;
  label?: string | number;
  valueSuffix: string;
}) {
  if (!active || !payload?.length) {
    return null;
  }

  const row = payload[0]?.payload;
  const productName = typeof row?.productName === 'string' ? row.productName : undefined;

  return (
    <Paper variant="outlined" sx={{ p: 1.25, maxWidth: 300 }}>
      {productName ? (
        <>
          <Typography fontWeight={800}>{productName}</Typography>
          <Typography variant="caption" color="text.secondary">{label}</Typography>
        </>
      ) : (
        <Typography fontWeight={800}>{label}</Typography>
      )}
      <Stack spacing={0.5} mt={0.75}>
        {payload.map((item) => (
          <Typography key={item.name} variant="body2">
            {item.name}: {n(item.value)}{valueSuffix}
          </Typography>
        ))}
      </Stack>
    </Paper>
  );
}

function ClassDistribution({ data }: { data: Array<{ name: string; value: number }> }) {
  return (
    <Box sx={{ width: '100%', height: 320 }}>
      <ResponsiveContainer>
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 24, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="name" />
          <YAxis allowDecimals={false} />
          <Tooltip formatter={(value) => n(Number(value))} />
          <Bar dataKey="value" name="Товаров">
            {data.map((entry) => (
              <Cell key={entry.name} fill={abcColors[entry.name]} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </Box>
  );
}

function LimitedTable<T>({
  rows,
  columns,
  getKey,
  emptyText,
  initialLimit = 10,
}: {
  rows: T[];
  columns: Array<{ label: string; align?: 'right'; render: (row: T) => ReactNode }>;
  getKey: (row: T, index: number) => React.Key;
  emptyText: string;
  initialLimit?: number;
}) {
  const [showAll, setShowAll] = useState(false);
  const visibleRows = showAll ? rows : rows.slice(0, initialLimit);

  return (
    <Stack spacing={1.5}>
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell key={column.label} align={column.align}>{column.label}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleRows.map((row, index) => (
              <TableRow key={getKey(row, index)} hover>
                {columns.map((column) => (
                  <TableCell key={column.label} align={column.align}>{column.render(row)}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {rows.length === 0 && (
          <Box py={4} textAlign="center">
            <Typography color="text.secondary">{emptyText}</Typography>
          </Box>
        )}
      </TableContainer>
      {rows.length > initialLimit && (
        <Box>
          <Button size="small" variant="outlined" onClick={() => setShowAll((value) => !value)}>
            {showAll ? 'Скрыть' : `Показать все (${rows.length})`}
          </Button>
        </Box>
      )}
    </Stack>
  );
}

type SortDirection = 'asc' | 'desc';

type SortableColumn<T> = {
  key: string;
  label: string;
  align?: 'right';
  render: (row: T) => ReactNode;
  sortValue: (row: T) => string | number;
};

function SortableLimitedTable<T>({
  rows,
  columns,
  getKey,
  emptyText,
  initialLimit = 10,
  initialSort,
}: {
  rows: T[];
  columns: Array<SortableColumn<T>>;
  getKey: (row: T, index: number) => React.Key;
  emptyText: string;
  initialLimit?: number;
  initialSort: { key: string; direction: SortDirection };
}) {
  const [showAll, setShowAll] = useState(false);
  const [sort, setSort] = useState(initialSort);
  const sortedRows = useMemo(() => {
    const column = columns.find((item) => item.key === sort.key);
    if (!column) return rows;
    return [...rows].sort((left, right) => compareSortValues(column.sortValue(left), column.sortValue(right), sort.direction));
  }, [columns, rows, sort.direction, sort.key]);
  const visibleRows = showAll ? sortedRows : sortedRows.slice(0, initialLimit);

  const changeSort = (key: string) => {
    setSort((current) => ({
      key,
      direction: current.key === key && current.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  return (
    <Stack spacing={1.5}>
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell key={column.key} align={column.align} sortDirection={sort.key === column.key ? sort.direction : false}>
                  <TableSortLabel
                    active={sort.key === column.key}
                    direction={sort.key === column.key ? sort.direction : 'asc'}
                    onClick={() => changeSort(column.key)}
                  >
                    {column.label}
                  </TableSortLabel>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {visibleRows.map((row, index) => (
              <TableRow key={getKey(row, index)} hover>
                {columns.map((column) => (
                  <TableCell key={column.key} align={column.align}>{column.render(row)}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {rows.length === 0 && (
          <Box py={4} textAlign="center">
            <Typography color="text.secondary">{emptyText}</Typography>
          </Box>
        )}
      </TableContainer>
      {rows.length > initialLimit && (
        <Box>
          <Button size="small" variant="outlined" onClick={() => setShowAll((value) => !value)}>
            {showAll ? 'Скрыть' : `Показать все (${rows.length})`}
          </Button>
        </Box>
      )}
    </Stack>
  );
}

function compareSortValues(left: string | number, right: string | number, direction: SortDirection) {
  const multiplier = direction === 'asc' ? 1 : -1;
  if (typeof left === 'number' && typeof right === 'number') {
    return (left - right) * multiplier;
  }
  return String(left).localeCompare(String(right), 'ru', { numeric: true, sensitivity: 'base' }) * multiplier;
}

function ProductCell({ sku, name }: { sku: string; name: string }) {
  return (
    <Box>
      <Typography fontWeight={700}>{name}</Typography>
      <Typography variant="caption" color="text.secondary">{sku}</Typography>
    </Box>
  );
}

function StatusChip({ status }: { status: string }) {
  const label = status in operationStatusLabels ? operationStatusLabels[status as OperationStatus] : status;
  return <Chip size="small" color={operationStatusColors[status] ?? 'default'} label={label} />;
}

function SourceChip({ source }: { source: OperationSource | 'UNKNOWN' }) {
  return (
    <Chip
      size="small"
      color={source === 'EDI' ? 'info' : source === 'MANUAL' ? 'default' : 'warning'}
      label={operationSourceLabels[source]}
      variant={source === 'EDI' ? 'filled' : 'outlined'}
    />
  );
}

function EdiStatusChip({ status }: { status: EdiMessageStatus }) {
  return <Chip size="small" color={status === 'FAILED' ? 'error' : status === 'PROCESSED' || status === 'COMPLETED' ? 'success' : 'warning'} label={ediStatusLabels[status]} />;
}

function PercentBar({ value }: { value: number }) {
  return (
    <Box sx={{ minWidth: 150 }}>
      <LinearProgress
        variant="determinate"
        value={Math.min(100, value)}
        color={value >= 90 ? 'error' : value >= 80 ? 'warning' : 'success'}
        sx={{ height: 8, borderRadius: 1, mb: 0.5 }}
      />
      <Typography variant="caption" color="text.secondary">{n(value)}%</Typography>
    </Box>
  );
}

function ReportSkeleton() {
  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        {Array.from({ length: 6 }).map((_, index) => (
          <Grid key={index} size={{ xs: 12, sm: 6, lg: 2 }}>
            <Skeleton variant="rounded" height={138} />
          </Grid>
        ))}
      </Grid>
      <Skeleton variant="rounded" height={380} />
    </Stack>
  );
}

type CellUtilizationItem = {
  cellCode: string;
  currentVolume?: number;
  maxVolume?: number;
  currentWeight?: number;
  maxWeight?: number;
};

type ABCAnalysisReport = {
  analysis: Array<{
    productSku: string;
    productName: string;
    totalQuantity: number;
    percentage: number;
    category: 'A' | 'B' | 'C' | string;
  }>;
};

type SupplierStatsReport = {
  supplierStats: Array<{
    supplierName: string;
    totalIncomingOps: number;
    totalIncomingQty: number;
  }>;
};

function filterOperationsByPeriod(rows: Operation[], start: string, end: string) {
  const startTime = dateValue(toBackendDateTime(start));
  const endTime = dateValue(toBackendDateTime(end));
  return rows.filter((row) => {
    const value = dateValue(row.completedAt ?? row.createdAt);
    return value >= startTime && value <= endTime;
  });
}

function filterEdiMessagesByPeriod(rows: EdiMessage[], start: string, end: string) {
  const startTime = dateValue(toBackendDateTime(start));
  const endTime = dateValue(toBackendDateTime(end));
  return rows.filter((row) => {
    const value = dateValue(row.receivedAt ?? row.processedAt);
    return value >= startTime && value <= endTime;
  });
}

function operationToMovementRows(operation: Operation) {
  return operation.items.map((item) => ({
    operationNumber: operation.operationNumber,
    type: operation.type,
    productSku: item.productSku ?? String(item.productId),
    productName: item.productName ?? 'Товар без названия',
    warehouseCode: operation.warehouseCode,
    quantity: item.quantity,
    source: operationSource(operation),
    user: operation.completedByUserId ? `#${operation.completedByUserId}` : operation.createdByUserId ? `#${operation.createdByUserId}` : '-',
    status: operation.status,
    date: operation.completedAt ?? operation.createdAt,
  }));
}

function buildMovementByDay(operations: Operation[]) {
  const grouped = new Map<string, { name: string; INCOME: number; OUTCOME: number; MOVE: number }>();
  operations.forEach((operation) => {
    const key = (operation.completedAt ?? operation.createdAt ?? '').slice(0, 10);
    if (!key) return;
    const row = grouped.get(key) ?? { name: key, INCOME: 0, OUTCOME: 0, MOVE: 0 };
    row[operation.type] += operationQuantity(operation);
    grouped.set(key, row);
  });
  return [...grouped.values()].sort((left, right) => left.name.localeCompare(right.name));
}

function buildStockAggregateIndex(rows: StockBalance[]) {
  const index = new Map<string, { quantity: number; reservedQuantity: number; availableQuantity: number }>();
  rows.forEach((row) => {
    const key = stockKey(row.productSku, row.warehouseId);
    const current = index.get(key) ?? { quantity: 0, reservedQuantity: 0, availableQuantity: 0 };
    current.quantity += row.quantity;
    current.reservedQuantity += row.reservedQuantity;
    current.availableQuantity += row.availableQuantity;
    index.set(key, current);
  });
  return index;
}

function buildCellAnalytics(cellRows: CellUtilizationItem[], dashboard?: DashboardReport, preferDashboardCells = false) {
  const fromReport = cellRows.map((row) => {
    const volumePercent = fillPercent(row.currentVolume, row.maxVolume);
    const weightPercent = fillPercent(row.currentWeight, row.maxWeight);
    return {
      name: row.cellCode,
      volumePercent,
      weightPercent,
      maxPercent: Math.max(volumePercent, weightPercent),
    };
  });
  const fromDashboard = dashboard?.cellUtilization.topCells.map((row) => ({
    name: row.warehouseCode ? `${row.warehouseCode}/${row.cellCode}` : row.cellCode,
    volumePercent: row.volumePercent ?? 0,
    weightPercent: row.weightPercent ?? 0,
    maxPercent: Math.max(row.volumePercent ?? 0, row.weightPercent ?? 0),
  })) ?? [];
  const cells = (preferDashboardCells && fromDashboard.length ? fromDashboard : fromReport.length ? fromReport : fromDashboard)
    .sort((left, right) => right.maxPercent - left.maxPercent);
  return {
    cells,
    averageVolume: dashboard?.cellUtilization.averageVolumePercent ?? average(cells.map((row) => row.volumePercent)),
    averageWeight: dashboard?.cellUtilization.averageWeightPercent ?? average(cells.map((row) => row.weightPercent)),
    above80: dashboard?.cellUtilization.cellsAbove80Percent ?? cells.filter((row) => row.maxPercent >= 80).length,
    above90: dashboard?.cellUtilization.cellsAbove90Percent ?? cells.filter((row) => row.maxPercent >= 90).length,
  };
}

function groupStockByWarehouse(rows: StockBalance[]) {
  const grouped = new Map<string, { name: string; quantity: number; reserved: number }>();
  rows.forEach((row) => {
    const item = grouped.get(row.warehouseCode) ?? { name: row.warehouseCode, quantity: 0, reserved: 0 };
    item.quantity += row.quantity;
    item.reserved += row.reservedQuantity;
    grouped.set(row.warehouseCode, item);
  });
  return [...grouped.values()].sort((left, right) => right.quantity - left.quantity).slice(0, 10);
}

function buildCounterpartyMap(counterparties: Counterparty[]) {
  return new Map(counterparties.map((row) => [row.id, row]));
}

function buildPartnerNameMap(counterparties: Counterparty[]) {
  return new Map(counterparties.flatMap((row) => [
    [row.code, row.name],
    [row.gln ?? row.code, row.name],
  ]));
}

function buildSupplierRows(
  operations: Operation[],
  messages: EdiMessage[],
  counterparties: Counterparty[],
  backendSuppliers?: SupplierStatsReport,
) {
  const supplierParties = counterparties.filter((row) => row.type === 'SUPPLIER' || row.type === 'BOTH');
  const byId = buildCounterpartyMap(supplierParties);
  const rows = new Map<string, SupplierReportRow>();
  supplierParties.forEach((supplier) => {
    rows.set(supplier.name, {
      supplierName: supplier.name,
      deliveryCount: 0,
      manualDeliveries: 0,
      ediDeliveries: 0,
      itemCount: 0,
      acceptedQuantity: 0,
      desadvCount: 0,
      ediErrors: 0,
      lastDelivery: undefined,
      status: supplier.isActive === false ? 'Неактивен' : 'Нет поставок',
    });
  });

  operations
    .filter((operation) => operation.type === 'INCOME' && operation.status === 'COMPLETED')
    .forEach((operation) => {
      const name = operation.counterpartyId ? byId.get(operation.counterpartyId)?.name ?? `Поставщик #${operation.counterpartyId}` : 'Без поставщика';
      const row = rows.get(name) ?? createSupplierRow(name);
      row.deliveryCount += 1;
      if (operationSource(operation) === 'EDI') {
        row.ediDeliveries += 1;
      } else {
        row.manualDeliveries += 1;
      }
      row.itemCount += operation.items.length;
      row.acceptedQuantity += operationQuantity(operation);
      row.lastDelivery = latestDate(row.lastDelivery, operation.completedAt ?? operation.createdAt);
      row.status = 'Активен';
      rows.set(name, row);
    });

  const nameByCode = buildPartnerNameMap(supplierParties);
  messages
    .filter((message) => message.messageType === 'DESADV')
    .forEach((message) => {
      const name = message.partnerCode ? nameByCode.get(message.partnerCode) ?? message.partnerCode : 'Без партнера';
      const row = rows.get(name) ?? createSupplierRow(name);
      row.desadvCount += 1;
      row.ediErrors += message.status === 'FAILED' ? 1 : 0;
      if (row.desadvCount > 0 && row.status === 'Нет поставок') row.status = 'Активен';
      rows.set(name, row);
    });

  backendSuppliers?.supplierStats.forEach((supplier) => {
    const row = rows.get(supplier.supplierName) ?? createSupplierRow(supplier.supplierName);
    row.deliveryCount = Math.max(row.deliveryCount, supplier.totalIncomingOps);
    row.acceptedQuantity = Math.max(row.acceptedQuantity, supplier.totalIncomingQty);
    if (row.deliveryCount > 0) row.status = 'Активен';
    rows.set(supplier.supplierName, row);
  });

  return [...rows.values()].sort((left, right) => right.acceptedQuantity - left.acceptedQuantity || left.supplierName.localeCompare(right.supplierName, 'ru'));
}

type SupplierReportRow = {
  supplierName: string;
  deliveryCount: number;
  manualDeliveries: number;
  ediDeliveries: number;
  itemCount: number;
  acceptedQuantity: number;
  desadvCount: number;
  ediErrors: number;
  lastDelivery?: string;
  status: string;
};

function createSupplierRow(supplierName: string): SupplierReportRow {
  return {
    supplierName,
    deliveryCount: 0,
    manualDeliveries: 0,
    ediDeliveries: 0,
    itemCount: 0,
    acceptedQuantity: 0,
    desadvCount: 0,
    ediErrors: 0,
    lastDelivery: undefined,
    status: 'Нет поставок',
  };
}

function buildAbcRows(operations: Operation[], backendAbc?: ABCAnalysisReport) {
  const movementOps = operations.filter((operation) => operation.status === 'COMPLETED' && (operation.type === 'INCOME' || operation.type === 'OUTCOME'));
  const rows = new Map<string, { productSku: string; productName: string; movementQuantity: number; costValue: number; hasCost: boolean }>();

  movementOps.flatMap((operation) => operation.items).forEach((item) => {
    const productSku = item.productSku ?? String(item.productId);
    const row = rows.get(productSku) ?? {
      productSku,
      productName: item.productName ?? 'Товар без названия',
      movementQuantity: 0,
      costValue: 0,
      hasCost: false,
    };
    row.movementQuantity += item.quantity;
    if (isReliablePrice(item)) {
      row.costValue += item.quantity * Number(item.unitPrice);
      row.hasCost = true;
    }
    rows.set(productSku, row);
  });

  if (rows.size === 0 && backendAbc?.analysis.length) {
    let previous = 0;
    return backendAbc.analysis.map((row) => {
      const sharePercent = Math.max(row.percentage - previous, 0);
      previous = row.percentage;
      return {
        productSku: row.productSku,
        productName: row.productName,
        movementQuantity: row.totalQuantity,
        value: row.totalQuantity,
        hasReliableCost: false,
        sharePercent: round(sharePercent),
        cumulativePercent: row.percentage,
        abcClass: normalizeAbcClass(row.category),
      };
    });
  }

  const hasReliableCost = [...rows.values()].some((row) => row.hasCost);
  const sorted = [...rows.values()]
    .map((row) => ({
      ...row,
      value: hasReliableCost ? row.costValue : row.movementQuantity,
    }))
    .filter((row) => row.value > 0)
    .sort((left, right) => right.value - left.value);
  const total = sum(sorted, (row) => row.value);
  let cumulative = 0;

  return sorted.map((row) => {
    const sharePercent = share(row.value, total);
    cumulative += sharePercent;
    return {
      productSku: row.productSku,
      productName: row.productName,
      movementQuantity: row.movementQuantity,
      value: round(row.value),
      hasReliableCost,
      sharePercent: round(sharePercent),
      cumulativePercent: round(Math.min(cumulative, 100)),
      abcClass: cumulative <= 80 ? 'A' : cumulative <= 95 ? 'B' : 'C',
    };
  });
}

function isReliablePrice(item: OperationItem) {
  return item.unitPrice !== undefined && item.unitPrice !== null && Number(item.unitPrice) > 0;
}

function normalizeAbcClass(value: string) {
  return value === 'A' || value === 'B' || value === 'C' ? value : 'C';
}

function getExportRows(
  report: ReportType,
  data: {
    dashboard?: DashboardReport;
    stockRows: StockBalance[];
    lowStockRows: LowStockAlert[];
    operations: Operation[];
    ediMessages: EdiMessage[];
    ediQueue: EdiQueueItem[];
    counterparties: Counterparty[];
    cellRows: CellUtilizationItem[];
    backendAbc?: ABCAnalysisReport;
    backendSuppliers?: SupplierStatsReport;
  },
) {
  if (report === 'summary') {
    return groupStockByWarehouse(data.stockRows);
  }
  if (report === 'movement') {
    return data.operations.filter((operation) => operation.status === 'COMPLETED').flatMap(operationToMovementRows);
  }
  if (report === 'cells') {
    return buildCellAnalytics(data.cellRows, data.dashboard).cells;
  }
  if (report === 'operations') {
    return data.operations.map((operation) => ({ ...operation, itemsCount: operation.items.length, quantity: operationQuantity(operation) }));
  }
  if (report === 'edi') {
    return data.ediMessages;
  }
  if (report === 'suppliers') {
    return buildSupplierRows(data.operations, data.ediMessages, data.counterparties, data.backendSuppliers);
  }
  return buildAbcRows(data.operations, data.backendAbc);
}

function exportCsv(reportLabel: string, rows: Array<Record<string, unknown>>) {
  const columns = Array.from(rows.reduce((set, row) => {
    Object.keys(row).forEach((key) => set.add(key));
    return set;
  }, new Set<string>()));
  const csv = [
    columns.join(';'),
    ...rows.map((row) => columns.map((column) => csvCell(row[column])).join(';')),
  ].join('\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${reportLabel.toLowerCase().replace(/\s+/g, '-')}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

function csvCell(value: unknown) {
  if (value === undefined || value === null) return '';
  const text = typeof value === 'object' ? JSON.stringify(value) : String(value);
  return `"${text.replace(/"/g, '""')}"`;
}

function isReportLoading(report: ReportType, loading: Record<string, boolean>) {
  if (report === 'summary') return loading.dashboard || loading.stock || loading.lowStock;
  if (report === 'movement') return loading.operations;
  if (report === 'cells') return loading.dashboard || loading.cells;
  if (report === 'operations') return loading.operations || loading.counterparties;
  if (report === 'edi') return loading.edi || loading.counterparties;
  if (report === 'suppliers') return loading.operations || loading.edi || loading.counterparties || loading.backendSuppliers;
  return loading.operations || loading.backendAbc;
}

function getReportError(report: ReportType, errors: Record<string, unknown>) {
  if (report === 'summary') return errors.dashboard ?? errors.stock ?? errors.lowStock;
  if (report === 'movement') return errors.operations;
  if (report === 'cells') return errors.dashboard ?? errors.cells;
  if (report === 'operations') return errors.operations ?? errors.counterparties;
  if (report === 'edi') return errors.edi ?? errors.counterparties;
  if (report === 'suppliers') return errors.operations ?? errors.edi ?? errors.counterparties ?? errors.backendSuppliers;
  return errors.operations ?? errors.backendAbc;
}

function getPeriodDays(start: string, end: string) {
  const diff = dateValue(toBackendDateTime(end)) - dateValue(toBackendDateTime(start));
  const days = Math.ceil(diff / 86400_000);
  if (days <= 1) return 1;
  if (days <= 7) return 7;
  return 30;
}

function quantityByType(operations: Operation[], type: OperationType) {
  return sum(operations.filter((operation) => operation.type === type), operationQuantity);
}

function operationQuantity(operation: Operation) {
  return sum(operation.items, (item) => item.quantity);
}

function operationSource(operation: Operation): OperationSource | 'UNKNOWN' {
  return operation.source === 'EDI' || operation.source === 'MANUAL' ? operation.source : 'UNKNOWN';
}

function countBy<T>(rows: T[], getKey: (row: T) => string) {
  return rows.reduce<Record<string, number>>((acc, row) => {
    const key = getKey(row);
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
}

function topN<T>(rows: T[], getValue: (row: T) => number, limit: number) {
  return [...rows].sort((left, right) => getValue(right) - getValue(left)).slice(0, limit);
}

function sum<T>(rows: T[], getValue: (row: T) => number | undefined) {
  return rows.reduce((total, row) => total + (getValue(row) ?? 0), 0);
}

function average(values: number[]) {
  const valid = values.filter((value) => Number.isFinite(value));
  return valid.length ? round(sum(valid, (value) => value) / valid.length) : 0;
}

function share(value: number, total: number) {
  return total > 0 ? (value * 100) / total : 0;
}

function round(value: number) {
  return Math.round(value * 100) / 100;
}

function stockKey(sku: string, warehouseId: number) {
  return `${sku}:${warehouseId}`;
}

function latestDate(left?: string, right?: string) {
  if (!left) return right;
  if (!right) return left;
  return dateValue(left) >= dateValue(right) ? left : right;
}

function dateValue(value?: string) {
  if (!value) return 0;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}
