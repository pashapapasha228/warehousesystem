import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const { register, handleSubmit, formState: { isSubmitting } } = useForm({ defaultValues: { username: 'admin', password: 'admin123' } });

  if (user) return <Navigate to="/" replace />;

  return (
    <Box minHeight="100vh" display="grid" sx={{ placeItems: 'center', bgcolor: 'background.default', p: 2 }}>
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent>
          <Stack spacing={2}>
            <Box>
              <Typography variant="h4">Warehousesystem</Typography>
              <Typography color="text.secondary">Вход в локальную систему складского учета</Typography>
            </Box>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Логин" autoComplete="username" {...register('username', { required: true })} />
            <TextField label="Пароль" type="password" autoComplete="current-password" {...register('password', { required: true })} />
            <Button
              variant="contained"
              size="large"
              disabled={isSubmitting}
              onClick={handleSubmit(async (data) => {
                setError('');
                try {
                  await login(data.username, data.password);
                  navigate('/');
                } catch (err) {
                  setError(err instanceof Error ? err.message : 'Не удалось войти.');
                }
              })}
            >
              Войти
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
