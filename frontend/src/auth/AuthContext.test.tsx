import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';

const http = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn() }, setAccessToken: vi.fn(), registerClearAuth: vi.fn() }));
vi.mock('./http', () => http);

function Consumer() {
  const auth = useAuth();
  return <><p>{auth.status}</p><p>{auth.user?.email ?? 'aucun utilisateur'}</p><button type="button" onClick={() => void auth.logout()}>Déconnexion test</button></>;
}

describe('AuthProvider backend contract', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('bootstraps CSRF, refreshes once, then obtains the current user before rendering authenticated state', async () => {
    http.api.get.mockImplementation((url: string) => url === '/auth/csrf' ? Promise.resolve({ data: { headerName: 'X-XSRF-TOKEN', token: 'csrf' } }) : Promise.resolve({ data: { id: 1, email: 'admin@ecole.fr', role: 'ADMIN', status: 'ACTIF' } }));
    http.api.post.mockResolvedValue({ data: { accessToken: 'memory-token' } });
    render(<AuthProvider><Consumer /></AuthProvider>);
    expect(screen.getByText('initializing')).toBeInTheDocument();
    expect(await screen.findByText('authenticated')).toBeInTheDocument();
    expect(screen.getByText('admin@ecole.fr')).toBeInTheDocument();
    expect(http.api.get).toHaveBeenNthCalledWith(1, '/auth/csrf');
    expect(http.api.post).toHaveBeenNthCalledWith(1, '/auth/refresh');
    expect(http.api.get).toHaveBeenNthCalledWith(2, '/auth/me');
    expect(http.setAccessToken).toHaveBeenCalledWith('memory-token');
  });

  it('clears frontend state even when logout fails', async () => {
    http.api.get.mockImplementation((url: string) => url === '/auth/csrf' ? Promise.resolve({ data: {} }) : Promise.resolve({ data: { id: 1, email: 'admin@ecole.fr', role: 'ADMIN', status: 'ACTIF' } }));
    http.api.post.mockImplementation((url: string) => url === '/auth/refresh' ? Promise.resolve({ data: { accessToken: 'memory-token' } }) : Promise.reject(new Error('offline')));
    render(<AuthProvider><Consumer /></AuthProvider>);
    await screen.findByText('authenticated');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Déconnexion test' }));
    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument());
    expect(http.setAccessToken).toHaveBeenLastCalledWith(null);
  });
});
