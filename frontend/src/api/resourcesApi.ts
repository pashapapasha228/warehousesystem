import type {
  Counterparty,
  DashboardReport,
  EdiAuditLog,
  EdiMapping,
  EdiMessage,
  EdiPartner,
  EdiQueueItem,
  DocumentExecutionStep,
  Operation,
  OperationVerification,
  PageParams,
  Product,
  ProductCard,
  ProductCategory,
  StockBalance,
  StorageCell,
  User,
  Warehouse,
} from '../types/api';
import { createOne, deleteOne, getOne, list, updateOne } from './crud';
import { downloadBlob, http } from './http';

export const productsApi = {
  list: (params?: PageParams) => list<Product>('/products', params),
  get: (id: number) => getOne<Product>('/products', id),
  card: (id: number, params?: PageParams) => http.get<ProductCard>(`/products/${id}/card`, { params }).then((r) => r.data),
  categories: () => http.get<ProductCategory[]>('/products/categories').then((r) => r.data),
  setWarehouseMinStock: (id: number, body: { warehouseId: number; minStockLevel: number }) => http.put(`/products/${id}/warehouse-min-stock`, body).then((r) => r.data),
  create: (body: Partial<Product>) => createOne<Product, Partial<Product>>('/products', body),
  update: (id: number, body: Partial<Product>) => updateOne<Product, Partial<Product>>('/products', id, body),
  delete: (id: number) => deleteOne('/products', id),
};

export const warehousesApi = {
  list: (params?: PageParams) => list<Warehouse>('/warehouses', params),
  create: (body: Partial<Warehouse>) => createOne<Warehouse, Partial<Warehouse>>('/warehouses', body),
  update: (id: number, body: Partial<Warehouse>) => updateOne<Warehouse, Partial<Warehouse>>('/warehouses', id, body),
  delete: (id: number) => deleteOne('/warehouses', id),
};

export const storageCellsApi = {
  list: (params?: PageParams) => list<StorageCell>('/storage-cells', params),
  create: (body: Partial<StorageCell>) => createOne<StorageCell, Partial<StorageCell>>('/storage-cells', body),
  update: (id: number, body: Partial<StorageCell>) => updateOne<StorageCell, Partial<StorageCell>>('/storage-cells', id, body),
  delete: (id: number) => deleteOne('/storage-cells', id),
};

export const counterpartiesApi = {
  list: (params?: PageParams) => list<Counterparty>('/counterparties', params),
  create: (body: Partial<Counterparty>) => createOne<Counterparty, Partial<Counterparty>>('/counterparties', body),
  update: (id: number, body: Partial<Counterparty>) => updateOne<Counterparty, Partial<Counterparty>>('/counterparties', id, body),
  delete: (id: number) => deleteOne('/counterparties', id),
};

export const operationsApi = {
  list: (params?: PageParams) => list<Operation>('/operations', params),
  get: (id: number) => getOne<Operation>('/operations', id),
  create: (body: any) => createOne<Operation, any>('/operations', body),
  complete: (id: number) => http.put<Operation>(`/operations/${id}/complete`).then((r) => r.data),
  ship: (id: number) => http.put<Operation>(`/operations/${id}/ship`).then((r) => r.data),
  cancel: (id: number) => http.put<Operation>(`/operations/${id}/cancel`).then((r) => r.data),
  verify: (id: number, body: any) => http.post<OperationVerification>(`/operations/${id}/verification`, body).then((r) => r.data),
  verifications: (id: number) => http.get<OperationVerification[]>(`/operations/${id}/verification`).then((r) => r.data),
  executionChain: (id: number) => http.get<DocumentExecutionStep[]>(`/operations/${id}/execution-chain`).then((r) => r.data),
  stockBalances: (params?: PageParams) => list<StockBalance>('/operations/stock-balances', params),
};

export const ediApi = {
  messages: (params?: PageParams) => list<EdiMessage>('/edi/messages', params),
  message: (id: number) => getOne<EdiMessage>('/edi/messages', id),
  inbound: (body: any) => createOne<EdiMessage, any>('/edi/messages/inbound', body),
  simulateSupplier: (body: any) => createOne<EdiMessage, any>('/edi/simulator/supplier', body),
  simulateCustomer: (body: any) => createOne<EdiMessage, any>('/edi/simulator/customer', body),
  simulateCustomerReceipt: (body: any) => createOne<EdiMessage, any>('/edi/simulator/customer-receipt', body),
  queue: (params?: PageParams) => list<EdiQueueItem>('/edi/queue', params),
  process: (id: number, body?: any) => http.post(`/edi/queue/${id}/process`, body ?? {}).then((r) => r.data),
  audit: (params?: PageParams) => list<EdiAuditLog>('/edi/audit', params),
  partners: {
    list: (params?: PageParams) => list<EdiPartner>('/edi/partners', params),
    get: (id: number) => getOne<EdiPartner>('/edi/partners', id),
    create: (body: Partial<EdiPartner>) => createOne<EdiPartner, Partial<EdiPartner>>('/edi/partners', body),
    update: (id: number, body: Partial<EdiPartner>) => updateOne<EdiPartner, Partial<EdiPartner>>('/edi/partners', id, body),
    delete: (id: number) => deleteOne('/edi/partners', id),
  },
  mappings: {
    list: (params?: PageParams) => list<EdiMapping>('/edi/mappings', params),
    create: (body: Partial<EdiMapping>) => createOne<EdiMapping, Partial<EdiMapping>>('/edi/mappings', body),
    update: (id: number, body: Partial<EdiMapping>) => updateOne<EdiMapping, Partial<EdiMapping>>('/edi/mappings', id, body),
    delete: (id: number) => deleteOne('/edi/mappings', id),
  },
};

export const usersApi = {
  list: (params?: PageParams) => list<User>('/users', params),
  create: (body: Partial<User> & { password?: string }) => createOne<User, Partial<User> & { password?: string }>('/users', body),
  update: (id: number, body: Partial<User> & { password?: string }) => updateOne<User, Partial<User> & { password?: string }>('/users', id, body),
  delete: (id: number) => deleteOne('/users', id),
};

export const reportsApi = {
  dashboard: (params?: { warehouseId?: number; periodDays?: number }) => http.get<DashboardReport>('/reports/dashboard', { params }).then((r) => r.data),
  lowStock: () => http.get('/reports/low-stock').then((r) => r.data),
  stockBalance: () => http.get('/reports/stock-balance').then((r) => r.data),
  turnover: (params: Record<string, string>) => http.get('/reports/turnover', { params }).then((r) => r.data),
  movement: (params: Record<string, string>) => http.get('/reports/movement', { params }).then((r) => r.data),
  topProducts: (params: Record<string, string | number>) => http.get('/reports/top-products', { params }).then((r) => r.data),
  supplierStats: (params: Record<string, string>) => http.get('/reports/supplier-stats', { params }).then((r) => r.data),
  cellUtilization: () => http.get('/reports/cell-utilization').then((r) => r.data),
  abc: (params: Record<string, string>) => http.get('/reports/abc-analysis', { params }).then((r) => r.data),
  ediStatistics: () => http.get('/reports/edi-statistics').then((r) => r.data),
  audit: (params: PageParams) => http.get('/reports/audit', { params }).then((r) => r.data),
  exportExcel: (report: string, params: Record<string, string>) => downloadBlob('/reports/export/excel', `${report}.xlsx`, { report, ...params }),
  exportPdf: (report: string, params: Record<string, string>) => downloadBlob('/reports/export/pdf', `${report}.pdf`, { report, ...params }),
};
