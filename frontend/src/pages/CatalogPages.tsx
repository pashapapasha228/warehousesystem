import { Chip } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ResourcePage } from '../components/ResourcePage';
import { BoolChip, FillBar } from '../components/tables/ResourceTable';
import { counterpartiesApi, productsApi, storageCellsApi, warehousesApi } from '../api/resourcesApi';
import { useWarehouseContext } from '../app/WarehouseContext';
import { canManageCatalogs } from '../utils/permissions';
import { counterpartyTypeLabels } from '../types/enums';
import { fillPercent, n } from '../utils/format';

const activeField = { name: 'isActive', label: 'Активно', type: 'checkbox' as const };

export function ProductsPage() {
  const navigate = useNavigate();
  const categories = useQuery({ queryKey: ['product-categories'], queryFn: productsApi.categories });
  const categoryLabels = Object.fromEntries((categories.data ?? []).map((category) => [category.code, category.label]));
  return (
    <ResourcePage
      title="Товары"
      queryKey="products"
      api={productsApi}
      canEdit={canManageCatalogs}
      onView={(product) => navigate(`/products/${product.id}`)}
      columns={[
        { key: 'sku', label: 'SKU' },
        { key: 'name', label: 'Название' },
        { key: 'category', label: 'Категория', render: (row) => row.category ? categoryLabels[row.category] ?? row.category : '—' },
        { key: 'isActive', label: 'Статус', render: (r) => <BoolChip value={r.isActive} /> },
      ]}
      fields={[
        { name: 'sku', label: 'SKU', required: true },
        { name: 'barcode', label: 'Штрихкод' },
        { name: 'name', label: 'Название', required: true },
        { name: 'category', label: 'Категория', type: 'select', options: (categories.data ?? []).map((category) => ({ value: category.code, label: category.label })) },
        { name: 'weightPerUnitKg', label: 'Вес единицы, кг', type: 'number' },
        { name: 'lengthCm', label: 'Длина, см', type: 'number' },
        { name: 'widthCm', label: 'Ширина, см', type: 'number' },
        { name: 'heightCm', label: 'Высота, см', type: 'number' },
        activeField,
      ]}
    />
  );
}

export function WarehousesPage() {
  return (
    <ResourcePage
      title="Склады"
      queryKey="warehouses"
      api={warehousesApi}
      canEdit={canManageCatalogs}
      columns={[
        { key: 'code', label: 'Код' },
        { key: 'name', label: 'Название' },
        { key: 'address', label: 'Адрес' },
        { key: 'isActive', label: 'Статус', render: (r) => <BoolChip value={r.isActive} /> },
      ]}
      fields={[
        { name: 'code', label: 'Код', required: true },
        { name: 'name', label: 'Название', required: true },
        { name: 'address', label: 'Адрес', type: 'textarea' },
        activeField,
      ]}
    />
  );
}

export function StorageCellsPage() {
  const { warehouseId, selectedWarehouse } = useWarehouseContext();

  return (
    <ResourcePage
      title="Ячейки хранения"
      description={selectedWarehouse ? `Склад: ${selectedWarehouse.code} · ${selectedWarehouse.name}` : 'Все склады'}
      queryKey="storage-cells"
      api={storageCellsApi}
      listParams={{ warehouseId: warehouseId || undefined }}
      canEdit={canManageCatalogs}
      columns={[
        { key: 'warehouseCode', label: 'Склад' },
        { key: 'code', label: 'Код ячейки' },
        { key: 'place', label: 'Зона/стеллаж/полка/уровень', render: (r) => [r.zone, r.rack, r.shelf, r.level].filter(Boolean).join(' / ') || '—' },
        { key: 'dims', label: 'Габариты', render: (r) => `${n(r.lengthCm)} x ${n(r.widthCm)} x ${n(r.heightCm)} см` },
        { key: 'max', label: 'Макс. вес/объем', render: (r) => `${n(r.maxWeightKg, ' кг')} / ${n(r.maxVolumeCm3, ' см3')}` },
        { key: 'current', label: 'Текущий вес/объем', render: (r) => `${n(r.currentWeightKg, ' кг')} / ${n(r.currentVolumeCm3, ' см3')}` },
        { key: 'fill', label: 'Заполненность', render: (r) => <FillBar value={Math.max(fillPercent(r.currentWeightKg, r.maxWeightKg), fillPercent(r.currentVolumeCm3, r.maxVolumeCm3))} /> },
      ]}
      fields={[
        { name: 'warehouseId', label: 'ID склада', type: 'number', required: true },
        { name: 'code', label: 'Код ячейки', required: true },
        { name: 'zone', label: 'Зона' },
        { name: 'rack', label: 'Стеллаж' },
        { name: 'shelf', label: 'Полка' },
        { name: 'level', label: 'Уровень' },
        { name: 'capacityUnits', label: 'Вместимость, шт.', type: 'number' },
        { name: 'maxWeightKg', label: 'Макс. вес, кг', type: 'number' },
        { name: 'maxVolumeCm3', label: 'Макс. объем, см3', type: 'number' },
        { name: 'lengthCm', label: 'Длина, см', type: 'number' },
        { name: 'widthCm', label: 'Ширина, см', type: 'number' },
        { name: 'heightCm', label: 'Высота, см', type: 'number' },
        activeField,
      ]}
    />
  );
}

export function CounterpartiesPage() {
  return (
    <ResourcePage
      title="Контрагенты"
      queryKey="counterparties"
      api={counterpartiesApi}
      canEdit={canManageCatalogs}
      columns={[
        { key: 'code', label: 'Код' },
        { key: 'name', label: 'Название' },
        { key: 'type', label: 'Тип', render: (r) => <Chip size="small" label={counterpartyTypeLabels[r.type]} /> },
        { key: 'gln', label: 'GLN' },
        { key: 'email', label: 'Email' },
        { key: 'phone', label: 'Телефон' },
        { key: 'contactInfo', label: 'Контактная информация' },
        { key: 'isActive', label: 'Статус', render: (r) => <BoolChip value={r.isActive} /> },
      ]}
      fields={[
        { name: 'code', label: 'Код', required: true },
        { name: 'name', label: 'Название', required: true },
        { name: 'type', label: 'Тип', type: 'select', required: true, options: [{ value: 'SUPPLIER', label: 'Поставщик' }, { value: 'CUSTOMER', label: 'Клиент' }, { value: 'BOTH', label: 'Клиент и поставщик' }] },
        { name: 'taxId', label: 'УНП/ИНН' },
        { name: 'gln', label: 'GLN' },
        { name: 'email', label: 'Email' },
        { name: 'phone', label: 'Телефон' },
        { name: 'address', label: 'Адрес', type: 'textarea' },
        { name: 'contactInfo', label: 'Контактная информация', type: 'textarea' },
        activeField,
      ]}
    />
  );
}
