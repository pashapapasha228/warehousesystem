import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createOne, deleteOne, getOne, list, updateOne } from './crud';
import { http } from './http';

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockedHttp = vi.mocked(http);

describe('crud api helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps known UI sort fields before requesting lists', async () => {
    mockedHttp.get.mockResolvedValueOnce({ data: { content: [] } });

    await list('/storage-cells', {
      page: 2,
      size: 20,
      sort: 'warehouseCode,asc',
      zone: 'A',
    });

    expect(mockedHttp.get).toHaveBeenCalledWith('/storage-cells', {
      params: {
        page: 2,
        size: 20,
        sort: 'warehouse.code,asc',
        zone: 'A',
      },
    });
  });

  it('keeps unknown sort fields unchanged', async () => {
    mockedHttp.get.mockResolvedValueOnce({ data: { content: [] } });

    await list('/products', { sort: 'sku,desc' });

    expect(mockedHttp.get).toHaveBeenCalledWith('/products', {
      params: { sort: 'sku,desc' },
    });
  });

  it('returns response data for item mutations', async () => {
    mockedHttp.get.mockResolvedValueOnce({ data: { id: 7 } });
    mockedHttp.post.mockResolvedValueOnce({ data: { id: 8 } });
    mockedHttp.put.mockResolvedValueOnce({ data: { id: 9 } });
    mockedHttp.delete.mockResolvedValueOnce({ data: { ok: true } });

    await expect(getOne('/products', 7)).resolves.toEqual({ id: 7 });
    await expect(createOne('/products', { sku: 'A-1' })).resolves.toEqual({ id: 8 });
    await expect(updateOne('/products', 9, { sku: 'A-2' })).resolves.toEqual({ id: 9 });
    await expect(deleteOne('/products', 10)).resolves.toEqual({ ok: true });

    expect(mockedHttp.get).toHaveBeenCalledWith('/products/7');
    expect(mockedHttp.post).toHaveBeenCalledWith('/products', { sku: 'A-1' });
    expect(mockedHttp.put).toHaveBeenCalledWith('/products/9', { sku: 'A-2' });
    expect(mockedHttp.delete).toHaveBeenCalledWith('/products/10');
  });
});
