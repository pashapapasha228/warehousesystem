import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { RoleGuard } from '../auth/RoleGuard';
import { AppLayout } from '../components/layout/AppLayout';
import { CounterpartiesPage, ProductsPage, StorageCellsPage, WarehousesPage } from '../pages/CatalogPages';
import { DashboardPage } from '../pages/DashboardPage';
import { EdiAuditPage, EdiMessagesPage, EdiPartnerCardPage, EdiPartnersPage, EdiQueuePage } from '../pages/EdiPages';
import { EdiSimulatorPage } from '../pages/EdiSimulatorPage';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { OperationCreatePage } from '../pages/OperationCreatePage';
import { OperationDetailsPage } from '../pages/OperationDetailsPage';
import { OperationsPage } from '../pages/OperationsPage';
import { ProductCardPage } from '../pages/ProductCardPage';
import { ReportsPage } from '../pages/ReportsPage';
import { StockBalancesPage } from '../pages/StockBalancesPage';
import { UsersPage } from '../pages/UsersPage';

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'products', element: <ProductsPage /> },
          { path: 'products/:id', element: <ProductCardPage /> },
          { path: 'warehouses', element: <WarehousesPage /> },
          { path: 'storage-cells', element: <StorageCellsPage /> },
          { path: 'counterparties', element: <CounterpartiesPage /> },
          { path: 'operations', element: <OperationsPage /> },
          { path: 'operations/new', element: <OperationCreatePage /> },
          { path: 'operations/:id', element: <OperationDetailsPage /> },
          { path: 'documents/:id', element: <OperationDetailsPage /> },
          { path: 'stock-balances', element: <StockBalancesPage /> },
          { path: 'edi', element: <Navigate to="/edi/messages" replace /> },
          { path: 'edi/messages', element: <EdiMessagesPage /> },
          { path: 'edi/simulator', element: <RoleGuard roles={['ADMIN', 'MANAGER']}><EdiSimulatorPage /></RoleGuard> },
          { path: 'edi/queue', element: <EdiQueuePage /> },
          { path: 'edi/partners', element: <EdiPartnersPage /> },
          { path: 'edi/partners/:id', element: <EdiPartnerCardPage /> },
          { path: 'edi/mappings', element: <Navigate to="/edi/partners" replace /> },
          { path: 'edi/audit', element: <RoleGuard roles={['ADMIN', 'MANAGER']}><EdiAuditPage /></RoleGuard> },
          { path: 'reports', element: <ReportsPage /> },
          { path: 'users', element: <RoleGuard roles={['ADMIN']}><UsersPage /></RoleGuard> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]);
