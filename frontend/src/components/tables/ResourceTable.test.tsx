import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ResourceTable, type Column, type SortDirection } from './ResourceTable';

type ProductRow = {
  id: number;
  name: string;
  quantity: number;
  nested?: {
    code: string;
  };
};

const rows: ProductRow[] = [
  { id: 1, name: 'Cable 10', quantity: 4, nested: { code: 'B' } },
  { id: 2, name: 'Cable 2', quantity: 12, nested: { code: 'A' } },
  { id: 3, name: 'Adapter', quantity: 0 },
];

const columns: Column<ProductRow>[] = [
  { key: 'name', label: 'Name' },
  { key: 'quantity', label: 'Qty' },
  { key: 'nested.code', label: 'Code' },
];

function renderTable(overrides: Partial<React.ComponentProps<typeof ResourceTable<ProductRow>>> = {}) {
  return render(
    <ResourceTable
      rows={rows}
      columns={columns}
      total={rows.length}
      page={0}
      size={10}
      onPageChange={vi.fn()}
      onSizeChange={vi.fn()}
      {...overrides}
    />,
  );
}

function bodyRows() {
  return within(screen.getAllByRole('rowgroup')[1]).getAllByRole('row');
}

describe('ResourceTable', () => {
  it('renders rows and falls back for missing nested values', () => {
    renderTable();

    expect(screen.getByText('Cable 10')).toBeTruthy();
    expect(screen.getByText('Cable 2')).toBeTruthy();
    expect(screen.getByText('Adapter')).toBeTruthy();
    expect(screen.getAllByText('—')).toHaveLength(3);
  });

  it('sorts rows locally when sorting is uncontrolled', async () => {
    renderTable();

    await userEvent.click(screen.getByRole('button', { name: 'Name' }));
    expect(within(bodyRows()[0]).getByText('Adapter')).toBeTruthy();

    await userEvent.click(screen.getByRole('button', { name: 'Name' }));
    expect(within(bodyRows()[0]).getByText('Cable 10')).toBeTruthy();
  });

  it('delegates sorting when controlled by a parent component', async () => {
    const onSortChange = vi.fn<(sortBy: string, sortDirection: SortDirection) => void>();

    renderTable({
      sortBy: 'quantity',
      sortDirection: 'asc',
      onSortChange,
    });

    await userEvent.click(screen.getByRole('button', { name: 'Qty' }));

    expect(onSortChange).toHaveBeenCalledWith('quantity', 'desc');
    expect(within(bodyRows()[0]).getByText('Cable 10')).toBeTruthy();
  });

  it('calls row action handlers with the selected row', async () => {
    const onView = vi.fn();
    const onEdit = vi.fn();
    const onDelete = vi.fn();

    renderTable({ onView, onEdit, onDelete });

    const firstRow = bodyRows()[0];
    const buttons = within(firstRow).getAllByRole('button');

    await userEvent.click(buttons[0]);
    await userEvent.click(buttons[1]);
    await userEvent.click(buttons[2]);

    expect(onView).toHaveBeenCalledWith(rows[0]);
    expect(onEdit).toHaveBeenCalledWith(rows[0]);
    expect(onDelete).toHaveBeenCalledWith(rows[0]);
  });
});
