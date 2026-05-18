import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { useQuery } from '@tanstack/react-query';
import { warehousesApi } from '../api/resourcesApi';
import { useAuth } from '../auth/useAuth';
import type { Warehouse } from '../types/api';

const WAREHOUSE_CONTEXT_KEY = 'warehouse.context.id';

type WarehouseContextValue = {
  warehouseId: number | null;
  setWarehouseId: (warehouseId: number | null) => void;
  warehouses: Warehouse[];
  selectedWarehouse?: Warehouse;
  isLoading: boolean;
};

const WarehouseContext = createContext<WarehouseContextValue | null>(null);

export function WarehouseProvider({ children }: PropsWithChildren) {
  const { user, loading } = useAuth();
  const [warehouseId, setWarehouseIdState] = useState<number | null>(() => {
    const stored = localStorage.getItem(WAREHOUSE_CONTEXT_KEY);
    return stored ? Number(stored) : null;
  });

  const warehousesQuery = useQuery({
    queryKey: ['warehouse-context-options', user?.id],
    queryFn: () => warehousesApi.list({ page: 0, size: 200, sort: 'code,asc' }),
    enabled: !loading && !!user,
  });

  const warehouses = user ? warehousesQuery.data?.content ?? [] : [];
  const selectedWarehouse = warehouses.find((warehouse) => warehouse.id === warehouseId);

  const setWarehouseId = (nextWarehouseId: number | null) => {
    setWarehouseIdState(nextWarehouseId);
    if (nextWarehouseId == null) {
      localStorage.removeItem(WAREHOUSE_CONTEXT_KEY);
    } else {
      localStorage.setItem(WAREHOUSE_CONTEXT_KEY, String(nextWarehouseId));
    }
    window.dispatchEvent(new Event('warehouse:context-changed'));
  };

  useEffect(() => {
    if (warehouseId != null && warehouses.length > 0 && !selectedWarehouse) {
      setWarehouseId(null);
    }
  }, [selectedWarehouse, warehouseId, warehouses.length]);

  const value = useMemo<WarehouseContextValue>(() => ({
    warehouseId,
    setWarehouseId,
    warehouses,
    selectedWarehouse,
    isLoading: warehousesQuery.isLoading,
  }), [selectedWarehouse, warehouseId, warehouses, warehousesQuery.isLoading]);

  return <WarehouseContext.Provider value={value}>{children}</WarehouseContext.Provider>;
}

export function useWarehouseContext() {
  const value = useContext(WarehouseContext);
  if (!value) {
    throw new Error('useWarehouseContext must be used inside WarehouseProvider');
  }
  return value;
}

export function getStoredWarehouseId() {
  const stored = localStorage.getItem(WAREHOUSE_CONTEXT_KEY);
  return stored ? Number(stored) : null;
}
