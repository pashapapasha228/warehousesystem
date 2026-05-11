import { Alert, Box, CircularProgress, Typography } from '@mui/material';

export function LoadingState() {
  return (
    <Box py={6} display="grid" sx={{ placeItems: 'center' }}>
      <CircularProgress />
    </Box>
  );
}

export function ErrorState({ message }: { message: string }) {
  return <Alert severity="error">{message}</Alert>;
}

export function EmptyState({ title = 'Нет данных' }: { title?: string }) {
  return (
    <Box py={6} textAlign="center">
      <Typography color="text.secondary">{title}</Typography>
    </Box>
  );
}
