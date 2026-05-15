import { useState } from 'react';
import type { SortDirection } from '../components/tables/ResourceTable';

export function useTableSort(defaultSortBy = 'id', defaultSortDirection: SortDirection = 'desc') {
  const [sortBy, setSortBy] = useState(defaultSortBy);
  const [sortDirection, setSortDirection] = useState<SortDirection>(defaultSortDirection);

  return {
    sortBy,
    sortDirection,
    sort: `${sortBy},${sortDirection}`,
    tableSortProps: {
      sortBy,
      sortDirection,
      onSortChange: (nextSortBy: string, nextSortDirection: SortDirection) => {
        setSortBy(nextSortBy);
        setSortDirection(nextSortDirection);
      },
    },
  };
}
