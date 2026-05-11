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
  Tooltip,
} from '@mui/material';
import { Delete, Edit, Visibility } from '@mui/icons-material';
import type { ReactNode } from 'react';
import { EmptyState } from '../feedback/StateViews';

export type Column<T> = {
  key: string;
  label: string;
  render?: (row: T) => ReactNode;
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

export function ResourceTable<T extends { id?: number }>({
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
}) {
  return (
    <Paper variant="outlined">
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((column) => <TableCell key={column.key}>{column.label}</TableCell>)}
              {(onEdit || onDelete || onView) && <TableCell align="right">Действия</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row, index) => (
              <TableRow key={row.id ?? index} hover>
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
