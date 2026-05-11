export type UserRole = 'ADMIN' | 'MANAGER' | 'STOREKEEPER';
export type CounterpartyType = 'SUPPLIER' | 'CUSTOMER';
export type OperationType = 'INCOME' | 'OUTCOME' | 'MOVE';
export type OperationStatus = 'DRAFT' | 'COMPLETED' | 'CANCELLED';
export type OperationSource = 'MANUAL' | 'EDI';
export type EdiMessageType = 'ORDERS' | 'DESADV' | 'ORDRSP';
export type EdiMessageStatus = 'RECEIVED' | 'NORMALIZED' | 'PROCESSING' | 'PROCESSED' | 'FAILED';
export type EdiQueueStatus = 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED';
export type EdiAuditStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';

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
  COMPLETED: 'Завершена',
  CANCELLED: 'Отменена',
};

export const counterpartyTypeLabels: Record<CounterpartyType, string> = {
  SUPPLIER: 'Поставщик',
  CUSTOMER: 'Клиент',
};
