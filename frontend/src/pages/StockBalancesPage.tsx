import {
  AddTask,
  ErrorOutline,
  History,
  Inventory2,
  LocalShipping,
  MoveDown,
  OpenInNew,
  Refresh,
  RestartAlt,
  Search,
  ShoppingCartCheckout,
  Warehouse,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  FormControl,
  Grid2 as Grid,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState, type ReactNode } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { operationsApi, reportsApi } from '../api/resourcesApi';
import { getErrorMessage } from '../api/http';
import { useWarehouseContext } from '../app/WarehouseContext';
import type { LowStockAlert, StockBalance, Warehouse as WarehouseDto } from '../types/api';
import type { SortDirection } from '../components/tables/ResourceTable';
import { fmtDate, n } from '../utils/format';

type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'NO_STOCK' | 'RESERVED' | 'AVAILABLE' | 'INVALID';
type StockFilterStatus = 'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'NO_STOCK' | 'RESERVED' | 'AVAILABLE';

type EnrichedStockBalance = StockBalance & {
  calculatedAvailable: number;
  minLevel?: number;
  status: StockStatus;
};

const STOCK_FETCH_SIZE = 5000;

const statusLabels: Record<StockFilterStatus | StockStatus, string> = {
  ALL: 'Все',
  IN_STOCK: 'В наличии',
  LOW_STOCK: 'Ниже минимума',
  NO_STOCK: 'Нет остатка',
  RESERVED: 'Есть резерв',
  AVAILABLE: 'Доступно к отгрузке',
  INVALID: 'Некорректно',
};

const statusColor: Record<StockStatus, 'default' | 'success' | 'warning' | 'error' | 'info' | 'secondary'> = {
  IN_STOCK: 'success',
  LOW_STOCK: 'warning',
  NO_STOCK: 'error',
  RESERVED: 'info',
  AVAILABLE: 'success',
  INVALID: 'error',
};

const filterStatuses: StockFilterStatus[] = ['ALL', 'IN_STOCK', 'LOW_STOCK', 'NO_STOCK', 'RESERVED', 'AVAILABLE'];

export function StockBalancesPage() {
  const navigate = useNavigate();
  const { warehouseId, setWarehouseId, warehouses } = useWarehouseContext();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [sortBy, setSortBy] = useState<keyof EnrichedStockBalance>('productSku');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [search, setSearch] = useState('');
  const [cellCode, setCellCode] = useState('');
  const [status, setStatus] = useState<StockFilterStatus>('ALL');

  const balancesQuery = useQuery({
    queryKey: ['stock-balances-workspace', warehouseId],
    queryFn: () => operationsApi.stockBalances({
      page: 0,
      size: STOCK_FETCH_SIZE,
      sort: 'productSku,asc',
      warehouseId: warehouseId || undefined,
    }),
  });

  const lowStockQuery = useQuery({
    queryKey: ['stock-low-alerts', warehouseId],
    queryFn: reportsApi.lowStock,
  });

  const rows = balancesQuery.data?.content ?? [];
  const lowStockAlerts = useMemo(
    () => (lowStockQuery.data ?? []).filter((item) => !warehouseId || item.warehouseId === warehouseId),
    [lowStockQuery.data, warehouseId],
  );
  const lowStockIndex = useMemo(() => buildLowStockIndex(lowStockAlerts), [lowStockAlerts]);
  const enrichedRows = useMemo(() => rows.map((row) => enrichStockBalance(row, lowStockIndex)), [lowStockIndex, rows]);
  const cellOptions = useMemo(() => [...new Set(enrichedRows.map((row) => row.cellCode).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'ru')), [enrichedRows]);

  const filteredRows = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return enrichedRows.filter((row) => {
      const matchesSearch = !normalizedSearch
        || row.productSku.toLowerCase().includes(normalizedSearch)
        || row.productName.toLowerCase().includes(normalizedSearch);
      const matchesCell = !cellCode || row.cellCode === cellCode;
      const matchesStatus = status === 'ALL' || matchesFilterStatus(row, status);
      return matchesSearch && matchesCell && matchesStatus;
    });
  }, [cellCode, enrichedRows, search, status]);

  const sortedRows = useMemo(() => sortRows(filteredRows, sortBy, sortDirection), [filteredRows, sortBy, sortDirection]);
  const pagedRows = useMemo(() => sortedRows.slice(page * size, page * size + size), [page, size, sortedRows]);
  const metrics = useMemo(() => calculateMetrics(enrichedRows, lowStockAlerts), [enrichedRows, lowStockAlerts]);
  const problemItems = useMemo(() => buildProblemItems(enrichedRows, lowStockAlerts), [enrichedRows, lowStockAlerts]);
  const loading = balancesQuery.isLoading || lowStockQuery.isLoading;
  const error = balancesQuery.error ?? lowStockQuery.error;

  const resetFilters = () => {
    setSearch('');
    setWarehouseId(null);
    setCellCode('');
    setStatus('ALL');
    setPage(0);
  };

  const handleSort = (nextSortBy: keyof EnrichedStockBalance) => {
    setSortDirection((current) => sortBy === nextSortBy && current === 'asc' ? 'desc' : 'asc');
    setSortBy(nextSortBy);
    setPage(0);
  };

  return (
    <Stack spacing={3}>
      <Box display="flex" alignItems="flex-start" justifyContent="space-between" gap={2} flexWrap="wrap">
        <Box>
          <Typography variant="h4" fontWeight={800}>Остатки</Typography>
          <Typography color="text.secondary">Фактические, зарезервированные и доступные количества по складам и ячейкам</Typography>
        </Box>
        <Button startIcon={<Refresh />} onClick={() => { balancesQuery.refetch(); lowStockQuery.refetch(); }}>
          Обновить
        </Button>
      </Box>

      {error && (
        <Alert
          severity="error"
          action={<Button color="inherit" size="small" onClick={() => { balancesQuery.refetch(); lowStockQuery.refetch(); }}>Повторить</Button>}
        >
          Не удалось загрузить остатки: {getErrorMessage(error)}
        </Alert>
      )}

      <Grid container spacing={2}>
        {loading ? Array.from({ length: 6 }).map((_, index) => (
          <Grid key={index} size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}>
            <Skeleton variant="rounded" height={118} />
          </Grid>
        )) : (
          <>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="Товарных позиций" value={metrics.uniqueProducts} caption="Уникальные SKU в остатках" icon={<Inventory2 />} tone="primary" /></Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="Фактический остаток" value={metrics.quantity} caption="Базовые единицы товара" icon={<Warehouse />} tone="primary" /></Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="В резерве" value={metrics.reserved} caption="Занято под отгрузки" icon={<ShoppingCartCheckout />} tone="warning" /></Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="Доступно" value={metrics.available} caption="Факт минус резерв" icon={<LocalShipping />} tone="success" /></Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="Ниже минимума" value={metrics.lowStock} caption="По настроенным минимумам" icon={<ErrorOutline />} tone={metrics.lowStock > 0 ? 'warning' : 'neutral'} /></Grid>
            <Grid size={{ xs: 12, sm: 6, lg: 4, xl: 2 }}><KpiCard title="Нет остатка" value={metrics.noStock} caption="Факт или доступно равны 0" icon={<ErrorOutline />} tone={metrics.noStock > 0 ? 'error' : 'neutral'} /></Grid>
          </>
        )}
      </Grid>

      <Card sx={{ borderRadius: 1.5 }}>
        <CardContent>
          <Stack spacing={2}>
            <Box display="flex" justifyContent="space-between" alignItems="center" gap={2} flexWrap="wrap">
              <Box>
                <Typography variant="h6" fontWeight={800}>Фильтры</Typography>
                <Typography variant="body2" color="text.secondary">Поиск, склад, ячейка и состояние запаса</Typography>
              </Box>
              <Button startIcon={<RestartAlt />} onClick={resetFilters}>Сбросить фильтры</Button>
            </Box>
            <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: 'minmax(240px, 2fr) repeat(3, minmax(180px, 1fr))' }} gap={2}>
              <TextField
                label="Поиск"
                placeholder="SKU или название товара"
                value={search}
                onChange={(event) => { setSearch(event.target.value); setPage(0); }}
                InputProps={{ startAdornment: <InputAdornment position="start"><Search /></InputAdornment> }}
              />
              <WarehouseSelect warehouses={warehouses} warehouseId={warehouseId} setWarehouseId={(value) => { setWarehouseId(value); setPage(0); setCellCode(''); }} />
              <FormControl>
                <InputLabel>Ячейка</InputLabel>
                <Select label="Ячейка" value={cellCode} onChange={(event) => { setCellCode(event.target.value); setPage(0); }}>
                  <MenuItem value="">Все ячейки</MenuItem>
                  {cellOptions.map((cell) => <MenuItem key={cell} value={cell}>{cell}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl>
                <InputLabel>Статус</InputLabel>
                <Select label="Статус" value={status} onChange={(event) => { setStatus(event.target.value as StockFilterStatus); setPage(0); }}>
                  {filterStatuses.map((item) => <MenuItem key={item} value={item}>{statusLabels[item]}</MenuItem>)}
                </Select>
              </FormControl>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, xl: 8 }}>
          <StockTable
            rows={pagedRows}
            total={sortedRows.length}
            page={page}
            size={size}
            sortBy={sortBy}
            sortDirection={sortDirection}
            loading={loading}
            onSort={handleSort}
            onPageChange={setPage}
            onSizeChange={(next) => { setSize(next); setPage(0); }}
            onOpenProduct={(row) => navigate(`/products/${row.productId}`)}
          />
        </Grid>
        <Grid size={{ xs: 12, xl: 4 }}>
          <ProblemStockPanel items={problemItems} loading={loading} />
        </Grid>
      </Grid>
    </Stack>
  );
}

function WarehouseSelect({
  warehouses,
  warehouseId,
  setWarehouseId,
}: {
  warehouses: WarehouseDto[];
  warehouseId: number | null;
  setWarehouseId: (warehouseId: number | null) => void;
}) {
  return (
    <FormControl>
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
  value: number;
  caption: string;
  icon: ReactNode;
  tone: 'primary' | 'success' | 'warning' | 'error' | 'neutral';
}) {
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardContent>
        <Stack spacing={1.25}>
          <Box display="flex" justifyContent="space-between" gap={2} alignItems="flex-start">
            <Typography color="text.secondary" fontWeight={700}>{title}</Typography>
            <Box sx={{ color: tone === 'neutral' ? 'text.secondary' : `${tone}.main`, display: 'flex' }}>{icon}</Box>
          </Box>
          <Typography variant="h4" fontWeight={900}>{n(value)}</Typography>
          <Typography variant="body2" color="text.secondary">{caption}</Typography>
        </Stack>
      </CardContent>
    </Card>
  );
}

function StockTable({
  rows,
  total,
  page,
  size,
  sortBy,
  sortDirection,
  loading,
  onSort,
  onPageChange,
  onSizeChange,
  onOpenProduct,
}: {
  rows: EnrichedStockBalance[];
  total: number;
  page: number;
  size: number;
  sortBy: keyof EnrichedStockBalance;
  sortDirection: SortDirection;
  loading: boolean;
  onSort: (sortBy: keyof EnrichedStockBalance) => void;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  onOpenProduct: (row: EnrichedStockBalance) => void;
}) {
  const columns: Array<{ key: keyof EnrichedStockBalance; label: string; align?: 'right' }> = [
    { key: 'productSku', label: 'SKU' },
    { key: 'productName', label: 'Товар' },
    { key: 'warehouseCode', label: 'Склад' },
    { key: 'cellCode', label: 'Ячейка' },
    { key: 'quantity', label: 'Факт', align: 'right' },
    { key: 'reservedQuantity', label: 'Резерв', align: 'right' },
    { key: 'availableQuantity', label: 'Доступно', align: 'right' },
    { key: 'updatedAt', label: 'Обновлено' },
  ];

  return (
    <Paper variant="outlined" sx={{ borderRadius: 1.5, overflow: 'hidden' }}>
      <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2 }}>
        <Box>
          <Typography variant="h6" fontWeight={800}>Остатки по ячейкам</Typography>
          <Typography variant="body2" color="text.secondary">Факт = физический остаток, резерв = занято под заказы, доступно = факт - резерв</Typography>
        </Box>
        <Chip label={`${n(total)} строк`} />
      </Box>
      <Divider />
      <TableContainer sx={{ maxHeight: 640 }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell key={column.key} align={column.align} sortDirection={sortBy === column.key ? sortDirection : false}>
                  <TableSortLabel active={sortBy === column.key} direction={sortBy === column.key ? sortDirection : 'asc'} onClick={() => onSort(column.key)}>
                    {column.label}
                  </TableSortLabel>
                </TableCell>
              ))}
              <TableCell>Статус</TableCell>
              <TableCell align="right">Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading && Array.from({ length: 8 }).map((_, index) => (
              <TableRow key={index}>
                <TableCell colSpan={10}><Skeleton height={32} /></TableCell>
              </TableRow>
            ))}
            {!loading && rows.map((row) => (
              <TableRow
                key={`${row.productId}-${row.warehouseId}-${row.cellId}`}
                hover
                sx={{
                  bgcolor: row.status === 'NO_STOCK' ? 'error.50' : row.status === 'LOW_STOCK' ? 'warning.50' : undefined,
                  '& td:first-of-type': {
                    borderLeft: 3,
                    borderLeftColor: row.status === 'NO_STOCK' ? 'error.main' : row.status === 'LOW_STOCK' ? 'warning.main' : 'transparent',
                  },
                }}
              >
                <TableCell>
                  <Typography fontWeight={800}>{row.productSku}</Typography>
                  {row.minLevel != null && <Typography variant="caption" color="text.secondary">Мин.: {row.minLevel}</Typography>}
                </TableCell>
                <TableCell>{row.productName}</TableCell>
                <TableCell>{row.warehouseCode}</TableCell>
                <TableCell>{row.cellCode}</TableCell>
                <TableCell align="right">{n(row.quantity)}</TableCell>
                <TableCell align="right">{n(row.reservedQuantity)}</TableCell>
                <TableCell align="right">{n(row.availableQuantity)}</TableCell>
                <TableCell>{fmtDate(row.updatedAt)}</TableCell>
                <TableCell><StatusChip status={row.status} /></TableCell>
                <TableCell align="right">
                  <RowActions row={row} onOpenProduct={onOpenProduct} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {!loading && rows.length === 0 && (
        <Box py={6} textAlign="center">
          <Typography fontWeight={800}>По фильтрам ничего не найдено</Typography>
          <Typography color="text.secondary">Измените поиск, склад, ячейку или статус</Typography>
        </Box>
      )}
      <TablePagination
        component="div"
        count={total}
        page={page}
        rowsPerPage={size}
        rowsPerPageOptions={[10, 20, 50, 100]}
        labelRowsPerPage="Строк"
        labelDisplayedRows={({ from, to, count }) => `${from}-${to} из ${count}`}
        onPageChange={(_, next) => onPageChange(next)}
        onRowsPerPageChange={(event) => onSizeChange(Number(event.target.value))}
      />
    </Paper>
  );
}

function RowActions({ row, onOpenProduct }: { row: EnrichedStockBalance; onOpenProduct: (row: EnrichedStockBalance) => void }) {
  return (
    <Stack direction="row" spacing={0.25} justifyContent="flex-end">
      <Tooltip title="Открыть товар">
        <IconButton size="small" onClick={() => onOpenProduct(row)}><OpenInNew fontSize="small" /></IconButton>
      </Tooltip>
      <Tooltip title="История движения">
        <IconButton component={RouterLink} to={`/products/${row.productId}`} size="small"><History fontSize="small" /></IconButton>
      </Tooltip>
      <Tooltip title="Создать приемку">
        <IconButton component={RouterLink} to="/operations/new?type=INCOME" size="small" color="primary"><AddTask fontSize="small" /></IconButton>
      </Tooltip>
      <Tooltip title="Создать отгрузку">
        <IconButton component={RouterLink} to="/operations/new?type=OUTCOME" size="small" color="success"><LocalShipping fontSize="small" /></IconButton>
      </Tooltip>
      <Tooltip title="Переместить">
        <IconButton component={RouterLink} to="/operations/new?type=MOVE" size="small"><MoveDown fontSize="small" /></IconButton>
      </Tooltip>
    </Stack>
  );
}

function ProblemStockPanel({ items, loading }: { items: EnrichedStockBalance[]; loading: boolean }) {
  return (
    <Card sx={{ height: '100%', borderRadius: 1.5 }}>
      <CardContent>
        <Stack spacing={2}>
          <Box>
            <Typography variant="h6" fontWeight={800}>Проблемные остатки</Typography>
            <Typography variant="body2" color="text.secondary">Нулевые остатки, минимум, большой резерв и некорректные значения</Typography>
          </Box>
          {loading && Array.from({ length: 6 }).map((_, index) => <Skeleton key={index} variant="rounded" height={48} />)}
          {!loading && items.length === 0 && <Alert severity="success">Проблемных остатков нет</Alert>}
          {!loading && items.map((item) => (
            <Box key={`${item.productId}-${item.warehouseId}-${item.cellId}-${item.status}`} sx={{ py: 1, borderBottom: 1, borderColor: 'divider' }}>
              <Box display="flex" justifyContent="space-between" gap={1} alignItems="flex-start">
                <Box sx={{ minWidth: 0 }}>
                  <Typography fontWeight={800}>{item.productName}</Typography>
                  <Typography variant="body2" color="text.secondary">{item.productSku} · {item.warehouseCode}/{item.cellCode}</Typography>
                </Box>
                <StatusChip status={item.status} />
              </Box>
              <Typography variant="caption" color="text.secondary">
                Факт {n(item.quantity)} · резерв {n(item.reservedQuantity)} · доступно {n(item.availableQuantity)}
              </Typography>
            </Box>
          ))}
        </Stack>
      </CardContent>
    </Card>
  );
}

function StatusChip({ status }: { status: StockStatus }) {
  return <Chip size="small" color={statusColor[status]} label={statusLabels[status]} />;
}

function buildLowStockIndex(alerts: LowStockAlert[]) {
  const index = new Map<string, LowStockAlert>();
  alerts.forEach((alert) => index.set(lowStockKey(alert.sku, alert.warehouseId), alert));
  return index;
}

function enrichStockBalance(row: StockBalance, lowStockIndex: Map<string, LowStockAlert>): EnrichedStockBalance {
  const calculatedAvailable = row.quantity - row.reservedQuantity;
  const alert = lowStockIndex.get(lowStockKey(row.productSku, row.warehouseId));
  const invalid = calculatedAvailable < 0 || row.availableQuantity < 0 || row.availableQuantity !== calculatedAvailable;
  const noStock = row.quantity <= 0 || row.availableQuantity <= 0;
  const hasReserve = row.reservedQuantity > 0;
  const lowStock = !!alert && alert.currentStock > 0;
  const status: StockStatus = invalid
    ? 'INVALID'
    : noStock
      ? 'NO_STOCK'
      : lowStock
        ? 'LOW_STOCK'
        : hasReserve
          ? 'RESERVED'
          : row.availableQuantity > 0
            ? 'AVAILABLE'
            : 'IN_STOCK';

  return {
    ...row,
    calculatedAvailable,
    minLevel: alert?.minLevel,
    status,
  };
}

function lowStockKey(sku: string, warehouseId: number) {
  return `${sku}::${warehouseId}`;
}

function matchesFilterStatus(row: EnrichedStockBalance, status: StockFilterStatus) {
  if (status === 'NO_STOCK') return row.status === 'NO_STOCK' || row.status === 'INVALID';
  if (status === 'LOW_STOCK') return row.status === 'LOW_STOCK';
  if (status === 'RESERVED') return row.reservedQuantity > 0;
  if (status === 'AVAILABLE') return row.availableQuantity > 0;
  if (status === 'IN_STOCK') return row.quantity > 0;
  return true;
}

function sortRows(rows: EnrichedStockBalance[], sortBy: keyof EnrichedStockBalance, sortDirection: SortDirection) {
  return [...rows].sort((left, right) => compareValues(left[sortBy], right[sortBy], sortDirection));
}

function compareValues(left: unknown, right: unknown, direction: SortDirection) {
  const multiplier = direction === 'asc' ? 1 : -1;
  if (left == null && right == null) return 0;
  if (left == null) return 1 * multiplier;
  if (right == null) return -1 * multiplier;

  const leftNumber = typeof left === 'number' ? left : Number(left);
  const rightNumber = typeof right === 'number' ? right : Number(right);
  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
    return (leftNumber - rightNumber) * multiplier;
  }
  return String(left).localeCompare(String(right), 'ru', { numeric: true, sensitivity: 'base' }) * multiplier;
}

function calculateMetrics(rows: EnrichedStockBalance[], lowStockAlerts: LowStockAlert[]) {
  const rowNoStockKeys = new Set(rows
    .filter((row) => row.quantity <= 0 || row.availableQuantity <= 0)
    .map((row) => lowStockKey(row.productSku, row.warehouseId)));
  const alertNoStockKeys = lowStockAlerts
    .filter((alert) => alert.currentStock <= 0)
    .map((alert) => lowStockKey(alert.sku, alert.warehouseId));

  return {
    uniqueProducts: new Set(rows.map((row) => row.productSku)).size,
    quantity: rows.reduce((sum, row) => sum + row.quantity, 0),
    reserved: rows.reduce((sum, row) => sum + row.reservedQuantity, 0),
    available: rows.reduce((sum, row) => sum + row.availableQuantity, 0),
    lowStock: lowStockAlerts.filter((alert) => alert.currentStock > 0).length,
    noStock: new Set([...rowNoStockKeys, ...alertNoStockKeys]).size,
  };
}

function buildProblemItems(rows: EnrichedStockBalance[], lowStockAlerts: LowStockAlert[]) {
  const lowStockKeys = new Set(lowStockAlerts.map((alert) => lowStockKey(alert.sku, alert.warehouseId)));
  return rows
    .filter((row) => row.status === 'NO_STOCK'
      || row.status === 'LOW_STOCK'
      || row.status === 'INVALID'
      || row.availableQuantity < row.reservedQuantity
      || row.reservedQuantity > Math.max(10, row.quantity * 0.6)
      || lowStockKeys.has(lowStockKey(row.productSku, row.warehouseId)))
    .sort((left, right) => problemPriority(left) - problemPriority(right) || left.productSku.localeCompare(right.productSku, 'ru'))
    .slice(0, 10);
}

function problemPriority(row: EnrichedStockBalance) {
  if (row.status === 'INVALID') return 0;
  if (row.status === 'NO_STOCK') return 1;
  if (row.status === 'LOW_STOCK') return 2;
  if (row.availableQuantity < row.reservedQuantity || row.reservedQuantity > Math.max(10, row.quantity * 0.6)) return 3;
  return 4;
}
