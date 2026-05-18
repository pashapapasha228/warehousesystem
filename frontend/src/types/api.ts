import type {
  CounterpartyType,
  EdiAuditStatus,
  EdiDirection,
  EdiMessageStatus,
  EdiMessageType,
  VerificationDecision,
  DocumentExecutionStage,
  DocumentExecutionStatus,
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
  category?: ProductCategoryCode;
  weightPerUnitKg?: number;
  volumePerUnitCm3?: number;
  lengthCm?: number;
  widthCm?: number;
  heightCm?: number;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type ProductCategoryCode =
  | 'ELECTRONICS'
  | 'COMPONENTS'
  | 'CABLES'
  | 'TOOLS'
  | 'OFFICE'
  | 'CONSUMABLES'
  | 'NETWORK'
  | 'SERVER'
  | 'OTHER';

export type ProductCategory = {
  code: ProductCategoryCode;
  label: string;
};

export type ProductCard = {
  product: Product;
  totalQuantity: number;
  totalReservedQuantity: number;
  totalAvailableQuantity: number;
  warehouseAggregates: Array<{
    warehouseId: number;
    warehouseCode: string;
    quantity: number;
    reservedQuantity: number;
    availableQuantity: number;
    minStockLevel: number;
  }>;
  minStockLevels: Array<{
    productId: number;
    warehouseId: number;
    warehouseCode: string;
    warehouseName: string;
    minStockLevel: number;
  }>;
  placements: Array<{
    warehouseId: number;
    warehouseCode: string;
    cellId: number;
    cellCode: string;
    quantity: number;
    reservedQuantity: number;
    availableQuantity: number;
  }>;
  recentOperations: Operation[];
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

export type LowStockAlert = {
  productName: string;
  sku: string;
  warehouseId: number;
  warehouseCode: string;
  currentStock: number;
  minLevel: number;
};

export type EdiPartner = {
  id: number;
  code: string;
  name: string;
  gln?: string;
  counterpartyId?: number;
  counterpartyName?: string;
  counterpartyType?: CounterpartyType;
  warehouseIds?: number[];
  warehouses?: Array<{ id: number; code: string; name: string }>;
  inboundEnabled?: boolean;
  outboundEnabled?: boolean;
  isActive?: boolean;
};

export type EdiMapping = {
  id: number;
  partnerId: number;
  partnerCode?: string;
  externalProductCode: string;
  internalProductId: number;
  internalSku?: string;
  internalProductName?: string;
  isActive?: boolean;
};

export type EdiMessage = {
  id: number;
  messageType: EdiMessageType;
  direction: EdiDirection;
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
  messageType: EdiMessageType;
  messageStatus: EdiMessageStatus;
  messageRef?: string;
  documentNumber?: string;
  partnerCode?: string;
  relatedOperationId?: number;
  normalizedPayload?: string;
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

export type DocumentExecutionStep = {
  id: number;
  operationId?: number;
  ediMessageId?: number;
  stage: DocumentExecutionStage;
  status: DocumentExecutionStatus;
  details?: string;
  createdBy?: string;
  createdAt?: string;
};

export type OperationVerificationItem = {
  id: number;
  operationItemId: number;
  productId: number;
  productSku: string;
  productName: string;
  plannedQuantity: number;
  actualQuantity: number;
  discrepancyQuantity: number;
  reason?: string;
};

export type OperationVerification = {
  id: number;
  operationId: number;
  decision: VerificationDecision;
  comment?: string;
  verifiedBy: string;
  verifiedAt?: string;
  items: OperationVerificationItem[];
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
  kpi: DashboardKpi;
  attentionItems: AttentionItem[];
  receivingQueue: DashboardWorkQueueItem[];
  shippingQueue: DashboardWorkQueueItem[];
  ediSummary: DashboardEdiSummary;
  cellUtilization: DashboardCellUtilizationSummary;
  stockWarnings: DashboardStockWarning[];
  recentOperations: DashboardRecentOperation[];
};

export type DashboardKpi = {
  expectedReceiving: number;
  readyToShip: number;
  pendingEdi: number;
  failedEdi: number;
  zeroStockProducts: number;
  belowMinProducts: number;
  averageVolumeUtilization: number;
  averageWeightUtilization: number;
  completedOperations: number;
  draftOperations: number;
};

export type AttentionItem = {
  type: string;
  severity: 'success' | 'info' | 'warning' | 'error' | string;
  priority: number;
  title: string;
  detail: string;
  actionLabel: string;
  actionUrl: string;
};

export type DashboardWorkQueueItem = {
  id: number;
  source: 'EDI' | 'OPERATION' | string;
  partnerName?: string;
  documentNumber?: string;
  itemCount: number;
  status: string;
  messageType?: EdiMessageType;
  operationType?: OperationType;
  actionLabel: string;
  actionUrl: string;
};

export type DashboardEdiSummary = {
  receivedToday: number;
  queued: number;
  processed: number;
  failed: number;
  problemMessages: DashboardEdiProblem[];
};

export type DashboardEdiProblem = {
  id: number;
  messageType: EdiMessageType;
  status: EdiMessageStatus;
  partnerName?: string;
  errorMessage?: string;
  actionUrl: string;
};

export type DashboardCellUtilizationSummary = {
  averageVolumePercent: number;
  averageWeightPercent: number;
  cellsAbove80Percent: number;
  cellsAbove90Percent: number;
  topCells: DashboardCellLoad[];
};

export type DashboardCellLoad = {
  id?: number;
  warehouseCode?: string;
  cellCode: string;
  volumePercent?: number;
  weightPercent?: number;
  actionUrl: string;
};

export type DashboardStockWarning = {
  productName: string;
  sku: string;
  warehouseId: number;
  warehouseCode: string;
  currentStock: number;
  minLevel: number;
  status: 'Нет остатка' | 'Ниже минимума' | string;
  actionUrl: string;
};

export type DashboardRecentOperation = {
  id: number;
  occurredAt?: string;
  type: OperationType;
  status: OperationStatus;
  operationNumber: string;
  warehouseCode?: string;
  username?: string;
  actionUrl: string;
};
