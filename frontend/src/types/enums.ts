export type UserRole = 'ADMIN' | 'MANAGER' | 'STOREKEEPER';
export type CounterpartyType = 'SUPPLIER' | 'CUSTOMER' | 'BOTH';
export type OperationType = 'INCOME' | 'OUTCOME' | 'MOVE';
export type OperationStatus = 'DRAFT' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED';
export type OperationSource = 'MANUAL' | 'EDI';
export type EdiMessageType = 'ORDERS' | 'DESADV' | 'ORDRSP';
export type EdiDirection = 'INBOUND' | 'OUTBOUND';
export type EdiMessageStatus = 'RECEIVED' | 'NORMALIZED' | 'PROCESSING' | 'PROCESSED' | 'COMPLETED' | 'FAILED';
export type EdiAuditStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';
export type VerificationDecision = 'ACCEPT' | 'REJECT' | 'ACCEPT_PARTIALLY';
export type DocumentExecutionStage = 'EDI_RECEIVED' | 'DRAFT_CREATED' | 'FACT_CHECK' | 'STOCK_POSTED' | 'COMPLETED' | 'CANCELLED';
export type DocumentExecutionStatus = 'PENDING' | 'DONE' | 'FAILED';

export const ediMessageTypes: EdiMessageType[] = ['ORDERS', 'DESADV', 'ORDRSP'];
export const ediMessageStatuses: EdiMessageStatus[] = ['RECEIVED', 'NORMALIZED', 'PROCESSING', 'PROCESSED', 'COMPLETED', 'FAILED'];

export const ediMessageTypeLabels: Record<EdiMessageType, string> = {
  ORDERS: 'ORDERS: заказ клиента',
  DESADV: 'DESADV: уведомление поставщика',
  ORDRSP: 'ORDRSP: ответ на заказ',
};

export const roleLabels: Record<UserRole, string> = {
  ADMIN: 'Администратор',
  MANAGER: 'Менеджер',
  STOREKEEPER: 'Кладовщик',
};

export const operationTypeLabels: Record<OperationType, string> = {
  INCOME: 'Приемка',
  OUTCOME: 'Отгрузка',
  MOVE: 'Перемещение',
};

export const operationStatusLabels: Record<OperationStatus, string> = {
  DRAFT: 'Черновик',
  SHIPPED: 'Отправлено',
  COMPLETED: 'Завершена',
  CANCELLED: 'Отменена',
};

export const counterpartyTypeLabels: Record<CounterpartyType, string> = {
  SUPPLIER: 'Поставщик',
  CUSTOMER: 'Клиент',
  BOTH: 'Клиент и поставщик',
};
