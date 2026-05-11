import { Button, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Stack spacing={2}>
      <Typography variant="h4">Страница не найдена</Typography>
      <Button component={Link} to="/" variant="contained">На панель управления</Button>
    </Stack>
  );
}
