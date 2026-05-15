import {
  Box,
  Chip,
  IconButton,
  LinearProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  Tooltip,
} from '@mui/material';
import { Delete, Edit, Visibility } from '@mui/icons-material';
import { useMemo, useState, type ReactNode } from 'react';
import { EmptyState } from '../feedback/StateViews';

export type SortDirection = 'asc' | 'desc';

export type Column<T> = {
  key: string;
  label: string;
  render?: (row: T) => ReactNode;
  sortKey?: string | false;
  sortValue?: (row: T) => unknown;
};

export function BoolChip({ value }: { value?: boolean }) {
  return <Chip size="small" label={value === false ? 'Выключено' : 'Активно'} color={value === false ? 'default' : 'success'} />;
}

export function FillBar({ value }: { value: number }) {
  return (
    <Box sx={{ minWidth: 130 }}>
      <LinearProgress
        variant="determinate"
        value={value}
        color={value > 85 ? 'error' : value > 65 ? 'warning' : 'success'}
        sx={{ height: 8, borderRadius: 1, mb: 0.5 }}
      />
      <Box component="span" sx={{ fontSize: 12, color: 'text.secondary' }}>{value}%</Box>
    </Box>
  );
}

export function ResourceTable<T>({
  rows,
  columns,
  total,
  page,
  size,
  onPageChange,
  onSizeChange,
  onEdit,
  onDelete,
  onView,
  sortBy,
  sortDirection,
  onSortChange,
}: {
  rows: T[];
  columns: Column<T>[];
  total: number;
  page: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  onEdit?: (row: T) => void;
  onDelete?: (row: T) => void;
  onView?: (row: T) => void;
  sortBy?: string;
  sortDirection?: SortDirection;
  onSortChange?: (sortBy: string, sortDirection: SortDirection) => void;
}) {
  const [localSort, setLocalSort] = useState<{ sortBy: string; sortDirection: SortDirection } | null>(null);
  const activeSortBy = sortBy ?? localSort?.sortBy;
  const activeSortDirection = sortDirection ?? localSort?.sortDirection ?? 'asc';
  const isControlledSort = !!onSortChange;

  const sortedRows = useMemo(() => {
    if (isControlledSort || !activeSortBy) {
      return rows;
    }

    const column = columns.find((item) => (item.sortKey ?? item.key) === activeSortBy);
    return [...rows].sort((left, right) => compareValues(getSortValue(left, column, activeSortBy), getSortValue(right, column, activeSortBy), activeSortDirection));
  }, [activeSortBy, activeSortDirection, columns, isControlledSort, rows]);

  const handleSort = (column: Column<T>) => {
    const nextSortBy = column.sortKey ?? column.key;
    if (!nextSortBy) {
      return;
    }

    const nextDirection: SortDirection = activeSortBy === nextSortBy && activeSortDirection === 'asc' ? 'desc' : 'asc';
    if (onSortChange) {
      onSortChange(nextSortBy, nextDirection);
    } else {
      setLocalSort({ sortBy: nextSortBy, sortDirection: nextDirection });
    }
  };

  return (
    <Paper variant="outlined">
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((column) => {
                const columnSortBy = column.sortKey ?? column.key;
                const sortable = !!columnSortBy;
                const active = activeSortBy === columnSortBy;
                return (
                  <TableCell key={column.key} sortDirection={active ? activeSortDirection : false}>
                    {sortable ? (
                      <TableSortLabel active={active} direction={active ? activeSortDirection : 'asc'} onClick={() => handleSort(column)}>
                        {column.label}
                      </TableSortLabel>
                    ) : column.label}
                  </TableCell>
                );
              })}
              {(onEdit || onDelete || onView) && <TableCell align="right">Действия</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {sortedRows.map((row, index) => (
              <TableRow key={getRowKey(row, index)} hover>
                {columns.map((column) => (
                  <TableCell key={column.key}>{column.render ? column.render(row) : (row as any)[column.key] ?? '—'}</TableCell>
                ))}
                {(onEdit || onDelete || onView) && (
                  <TableCell align="right">
                    {onView && <Tooltip title="Просмотр"><IconButton size="small" onClick={() => onView(row)}><Visibility fontSize="small" /></IconButton></Tooltip>}
                    {onEdit && <Tooltip title="Редактировать"><IconButton size="small" onClick={() => onEdit(row)}><Edit fontSize="small" /></IconButton></Tooltip>}
                    {onDelete && <Tooltip title="Удалить"><IconButton size="small" color="error" onClick={() => onDelete(row)}><Delete fontSize="small" /></IconButton></Tooltip>}
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {!rows.length && <EmptyState />}
      <TablePagination
        component="div"
        count={total}
        page={page}
        rowsPerPage={size}
        rowsPerPageOptions={[10, 20, 50]}
        labelRowsPerPage="Строк"
        onPageChange={(_, next) => onPageChange(next)}
        onRowsPerPageChange={(event) => onSizeChange(Number(event.target.value))}
      />
    </Paper>
  );
}

function getSortValue<T>(row: T, column: Column<T> | undefined, sortBy: string) {
  if (column?.sortValue) {
    return column.sortValue(row);
  }

  return getByPath(row, column?.key ?? sortBy);
}

function getByPath(row: unknown, path: string) {
  return path.split('.').reduce<unknown>((value, key) => {
    if (value == null || typeof value !== 'object') {
      return undefined;
    }

    return (value as Record<string, unknown>)[key];
  }, row);
}

function compareValues(left: unknown, right: unknown, direction: SortDirection) {
  const multiplier = direction === 'asc' ? 1 : -1;

  if (left == null && right == null) {
    return 0;
  }

  if (left == null) {
    return 1 * multiplier;
  }

  if (right == null) {
    return -1 * multiplier;
  }

  const leftNumber = typeof left === 'number' ? left : Number(left);
  const rightNumber = typeof right === 'number' ? right : Number(right);

  if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) {
    return (leftNumber - rightNumber) * multiplier;
  }

  return String(left).localeCompare(String(right), 'ru', { numeric: true, sensitivity: 'base' }) * multiplier;
}

function getRowKey(row: unknown, index: number) {
  if (row != null && typeof row === 'object' && 'id' in row) {
    return String((row as { id?: unknown }).id ?? index);
  }

  return index;
}
