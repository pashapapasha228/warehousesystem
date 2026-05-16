import { http } from './http';
import type { Page, PageParams } from '../types/api';

export function list<T>(url: string, params?: PageParams) {
  return http.get<Page<T>>(url, { params: mapSortParams(url, params) }).then((r) => r.data);
}

export function getOne<T>(url: string, id: number | string) {
  return http.get<T>(`${url}/${id}`).then((r) => r.data);
}

export function createOne<T, B>(url: string, body: B) {
  return http.post<T>(url, body).then((r) => r.data);
}

export function updateOne<T, B>(url: string, id: number | string, body: B) {
  return http.put<T>(`${url}/${id}`, body).then((r) => r.data);
}

export function deleteOne(url: string, id: number | string) {
  return http.delete(`${url}/${id}`).then((r) => r.data);
}

const sortAliases: Record<string, Record<string, string>> = {
  '/storage-cells': {
    warehouseCode: 'warehouse.code',
    place: 'zone',
    dims: 'lengthCm',
    max: 'maxWeightKg',
    current: 'currentWeightKg',
    fill: 'currentVolumeCm3',
  },
  '/operations': {
    warehouseCode: 'warehouse.code',
  },
  '/operations/stock-balances': {
    productSku: 'product.sku',
    productName: 'product.name',
    warehouseCode: 'cell.warehouse.code',
    cellCode: 'cell.code',
    availableQuantity: 'quantity',
  },
  '/edi/messages': {
    partnerCode: 'partner.code',
    relatedOperationNumber: 'relatedOperation.operationNumber',
  },
  '/edi/queue': {
    ediMessageId: 'ediMessage.id',
    messageRef: 'ediMessage.messageRef',
    partnerCode: 'ediMessage.partner.code',
  },
  '/edi/partners': {
    counterpartyName: 'counterparty.name',
    warehouses: 'code',
  },
  '/edi/mappings': {
    partnerCode: 'partner.code',
    internalSku: 'internalProduct.sku',
  },
};

function mapSortParams(url: string, params?: PageParams) {
  if (!params?.sort || typeof params.sort !== 'string') {
    return params;
  }

  const [field, direction] = params.sort.split(',');
  const sortField = sortAliases[url]?.[field] ?? field;
  return { ...params, sort: direction ? `${sortField},${direction}` : sortField };
}
