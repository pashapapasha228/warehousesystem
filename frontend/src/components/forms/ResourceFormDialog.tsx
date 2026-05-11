import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { useEffect } from 'react';

export type FieldDef = {
  name: string;
  label: string;
  type?: 'text' | 'number' | 'checkbox' | 'select' | 'password' | 'textarea' | 'date';
  required?: boolean;
  options?: Array<{ value: string | number | boolean; label: string }>;
};

export function ResourceFormDialog({
  open,
  title,
  fields,
  initialValues,
  onClose,
  onSubmit,
}: {
  open: boolean;
  title: string;
  fields: FieldDef[];
  initialValues?: Record<string, any>;
  onClose: () => void;
  onSubmit: (data: Record<string, any>) => Promise<void> | void;
}) {
  const { control, handleSubmit, reset, formState: { isSubmitting } } = useForm<Record<string, any>>({ defaultValues: {} });

  useEffect(() => {
    const defaults = Object.fromEntries(fields.map((field) => [field.name, field.type === 'checkbox' ? true : '']));
    reset({ ...defaults, ...initialValues });
  }, [fields, initialValues, reset, open]);

  const submit = handleSubmit(async (data) => {
    const allowed = new Set(fields.map((field) => field.name));
    const clean = Object.fromEntries(
      Object.entries(data)
        .filter(([key]) => allowed.has(key))
        .map(([key, value]) => [key, value === '' ? null : value]),
    );
    await onSubmit(clean);
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {fields.map((field) => (
            <Controller
              key={field.name}
              control={control}
              name={field.name}
              rules={{ required: field.required ? 'Заполните поле' : false }}
              render={({ field: controllerField, fieldState }) => {
                if (field.type === 'checkbox') {
                  return <FormControlLabel control={<Checkbox checked={Boolean(controllerField.value)} onChange={(_, checked) => controllerField.onChange(checked)} />} label={field.label} />;
                }
                if (field.type === 'select') {
                  return (
                    <FormControl fullWidth error={!!fieldState.error}>
                      <InputLabel>{field.label}</InputLabel>
                      <Select {...controllerField} label={field.label} value={controllerField.value ?? ''}>
                        {field.options?.map((option) => <MenuItem key={String(option.value)} value={option.value as any}>{option.label}</MenuItem>)}
                      </Select>
                    </FormControl>
                  );
                }
                return (
                  <TextField
                    {...controllerField}
                    label={field.label}
                    type={field.type === 'number' ? 'number' : field.type === 'password' ? 'password' : field.type === 'date' ? 'date' : 'text'}
                    required={field.required}
                    error={!!fieldState.error}
                    helperText={fieldState.error?.message}
                    multiline={field.type === 'textarea'}
                    minRows={field.type === 'textarea' ? 3 : undefined}
                    InputLabelProps={field.type === 'date' ? { shrink: true } : undefined}
                    fullWidth
                  />
                );
              }}
            />
          ))}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Отмена</Button>
        <Button variant="contained" onClick={submit} disabled={isSubmitting}>Сохранить</Button>
      </DialogActions>
    </Dialog>
  );
}
