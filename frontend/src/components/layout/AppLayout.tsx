import {
  Assessment,
  Dashboard,
  Group,
  Inventory2,
  LocalShipping,
  Logout,
  Menu as MenuIcon,
  MoveDown,
  PeopleAlt,
  QrCode2,
  TableRows,
  Warehouse,
} from '@mui/icons-material';
import {
  AppBar,
  Avatar,
  Box,
  Divider,
  Drawer,
  FormControl,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  MenuItem,
  Select,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useWarehouseContext } from '../../app/WarehouseContext';
import { useAuth } from '../../auth/useAuth';
import { canManageUsers } from '../../utils/permissions';
import { roleLabels } from '../../types/enums';

const drawerWidth = 268;

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuth();
  const { warehouseId, setWarehouseId, warehouses } = useWarehouseContext();
  const navigate = useNavigate();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const items = [
    { to: '/', label: 'Панель управления', icon: <Dashboard /> },
    { to: '/products', label: 'Товары', icon: <Inventory2 /> },
    { to: '/warehouses', label: 'Склады', icon: <Warehouse /> },
    { to: '/storage-cells', label: 'Ячейки хранения', icon: <TableRows /> },
    { to: '/counterparties', label: 'Контрагенты', icon: <PeopleAlt /> },
    { to: '/operations', label: 'Операции', icon: <MoveDown /> },
    { to: '/stock-balances', label: 'Остатки', icon: <QrCode2 /> },
    { to: '/edi/messages', label: 'EDI сообщения', icon: <LocalShipping /> },
    { to: '/edi/simulator', label: 'External messages', icon: <LocalShipping /> },
    { to: '/edi/queue', label: 'EDI очередь', icon: <LocalShipping /> },
    { to: '/edi/partners', label: 'EDI партнеры', icon: <LocalShipping /> },
    { to: '/edi/mappings', label: 'EDI маппинги', icon: <LocalShipping /> },
    ...(user?.role === 'ADMIN' || user?.role === 'MANAGER' ? [{ to: '/edi/audit', label: 'EDI аудит', icon: <LocalShipping /> }] : []),
    { to: '/reports', label: 'Отчеты', icon: <Assessment /> },
    ...(canManageUsers(user?.role) ? [{ to: '/users', label: 'Пользователи', icon: <Group /> }] : []),
  ];

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar sx={{ px: 2 }}>
        <Box>
          <Typography variant="h6">Warehouse ERP</Typography>
          <Typography variant="caption" color="text.secondary">Локальная складская панель</Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List sx={{ px: 1, py: 1 }}>
        {items.map((item) => (
          <ListItemButton
            key={item.to}
            component={NavLink}
            to={item.to}
            onClick={() => setMobileOpen(false)}
            sx={{
              borderRadius: 1,
              mb: 0.5,
              '&.active': {
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                '& .MuiListItemIcon-root': { color: 'inherit' },
              },
            }}
          >
            <ListItemIcon>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Box sx={{ flex: 1 }} />
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="body2" fontWeight={700}>{user?.fullName || user?.username}</Typography>
        <Typography variant="caption" color="text.secondary">{user?.role ? roleLabels[user.role] : ''}</Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" color="inherit" sx={{ ml: { md: `${drawerWidth}px` }, width: { md: `calc(100% - ${drawerWidth}px)` }, borderBottom: 1, borderColor: 'divider' }}>
        <Toolbar>
          <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ display: { md: 'none' }, mr: 1 }}>
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flex: 1 }}>Складской учет</Typography>
          <FormControl size="small" sx={{ minWidth: { xs: 150, sm: 240 }, mr: 2 }}>
            <Select
              displayEmpty
              value={warehouseId ?? ''}
              onChange={(event) => setWarehouseId(event.target.value ? Number(event.target.value) : null)}
            >
              <MenuItem value="">All warehouses</MenuItem>
              {warehouses.map((warehouse) => (
                <MenuItem key={warehouse.id} value={warehouse.id}>{warehouse.code} - {warehouse.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <Avatar sx={{ width: 32, height: 32, mr: 1 }}>{user?.username?.[0]?.toUpperCase()}</Avatar>
          <Box sx={{ display: { xs: 'none', sm: 'block' }, mr: 1 }}>
            <Typography variant="body2">{user?.username}</Typography>
            <Typography variant="caption" color="text.secondary">{user?.role && roleLabels[user.role]}</Typography>
          </Box>
          <Tooltip title="Выйти">
            <IconButton onClick={async () => { await logout(); navigate('/login'); }}>
              <Logout />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer variant={isMobile ? 'temporary' : 'permanent'} open={isMobile ? mobileOpen : true} onClose={() => setMobileOpen(false)} ModalProps={{ keepMounted: true }} sx={{ '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }}>
          {drawer}
        </Drawer>
      </Box>
      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, mt: 8, minWidth: 0 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
