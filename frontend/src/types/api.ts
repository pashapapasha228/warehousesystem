import type {
  CounterpartyType,
  EdiAuditStatus,
  EdiMessageStatus,
  EdiMessageType,
  EdiQueueStatus,
  OperationSource,
  OperationStatus,
  OperationType,
  UserRole,
} from './enums';

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

export type PageParams = {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: unknown;
};

export type CurrentUser = {
  id: number;
  username: string;
  fullName?: string;
  email?: string;
  role: UserRole;
  isActive: boolean;
};

export type Product = {
  id: number;
  sku: string;
  barcode?: string;
  name: string;
  category?: string;
  unitOfMeasure?: string;
  minStockLevel?: number;
  weightPerUnitKg?: number;
  volumePerUnitCm3?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type Warehouse = {
  id: number;
  code: string;
  name: string;
  address?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type StorageCell = {
  id: number;
  warehouseId: number;
  warehouseCode?: string;
  code: string;
  zone?: string;
  rack?: string;
  shelf?: string;
  level?: string;
  capacityUnits?: number;
  maxWeightKg?: number;
  maxVolumeCm3?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  currentWeightKg?: number;
  currentVolumeCm3?: number;
  isActive?: boolean;
};

export type Counterparty = {
  id: number;
  code: string;
  name: string;
  type: CounterpartyType;
  taxId?: string;
  gln?: string;
  email?: string;
  phone?: string;
  address?: string;
  contactInfo?: string;
  isActive?: boolean;
};

export type OperationItem = {
  id?: number;
  productId: number;
  productSku?: string;
  productName?: string;
  quantity: number;
  unitPrice?: number;
  unitOfMeasure?: string;
  fromCellId?: number;
  fromCellCode?: string;
  toCellId?: number;
  toCellCode?: string;
};

export type Operation = {
  id: number;
  operationNumber: string;
  type: OperationType;
  status: OperationStatus;
  warehouseId: number;
  warehouseCode?: string;
  createdByUserId?: number;
  completedByUserId?: number;
  counterpartyId?: number;
  source?: OperationSource;
  externalDocumentNumber?: string;
  documentDate?: string;
  comment?: string;
  createdAt?: string;
  completedAt?: string;
  items: OperationItem[];
};

export type StockBalance = {
  productId: number;
  productSku: string;
  productName: string;
  cellId: number;
  cellCode: string;
  warehouseId: number;
  warehouseCode: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  updatedAt?: string;
};

export type EdiPartner = {
  id: number;
  code: string;
  name: string;
  gln?: string;
  counterpartyId?: number;
  counterpartyName?: string;
  defaultWarehouseId?: number;
  defaultWarehouseCode?: string;
  inboundEnabled?: boolean;
  outboundEnabled?: boolean;
  isActive?: boolean;
};

export type EdiMapping = {
  id: number;
  partnerId: number;
  partnerCode?: string;
  messageType: EdiMessageType;
  externalProductCode: string;
  externalUom?: string;
  internalProductId: number;
  internalSku?: string;
  internalUom?: string;
  isActive?: boolean;
};

export type EdiMessage = {
  id: number;
  messageType: EdiMessageType;
  direction: string;
  status: EdiMessageStatus;
  interchangeRef?: string;
  messageRef?: string;
  documentNumber?: string;
  rawPayload?: string;
  normalizedPayload?: string;
  partnerId?: number;
  partnerCode?: string;
  relatedOperationId?: number;
  relatedOperationNumber?: string;
  receivedAt?: string;
  processedAt?: string;
  errorMessage?: string;
};

export type EdiQueueItem = {
  id: number;
  ediMessageId: number;
  messageRef?: string;
  partnerCode?: string;
  status: EdiQueueStatus;
  attemptCount: number;
  scheduledAt?: string;
  startedAt?: string;
  finishedAt?: string;
  lastError?: string;
};

export type EdiAuditLog = {
  id: number;
  ediMessageId: number;
  stage: string;
  status: EdiAuditStatus;
  details?: string;
  createdAt?: string;
};

export type User = {
  id: number;
  username: string;
  fullName?: string;
  email?: string;
  role: UserRole;
  isActive?: boolean;
};

export type DashboardReport = {
  activeProducts: number;
  activeWarehouses: number;
  activeStorageCells: number;
  completedOperations: number;
  draftOperations: number;
  totalStockQuantity: number;
  lowStockProducts: number;
  pendingEdiMessages: number;
  failedEdiMessages: number;
  mostUtilizedCells: Array<{
    cellCode: string;
    currentVolume?: number;
    maxVolume?: number;
    currentWeight?: number;
    maxWeight?: number;
  }>;
  lowStockAlerts: Array<{ productName: string; sku: string; currentStock: number; minLevel: number }>;
};
