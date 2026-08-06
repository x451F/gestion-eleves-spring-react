import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, getAccessToken, registerClearAuth, setAccessToken } from './http';

describe('auth HTTP foundation', () => {
  beforeEach(() => {
    setAccessToken(null);
    registerClearAuth(() => undefined);
    api.defaults.adapter = undefined as never;
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/';
  });

  it('keeps an access token in module memory and never persists it in browser storage', () => {
    setAccessToken('secret');
    expect(getAccessToken()).toBe('secret');
    if (typeof localStorage !== 'undefined') expect(localStorage.getItem('accessToken')).toBeNull();
    if (typeof sessionStorage !== 'undefined') expect(sessionStorage.getItem('accessToken')).toBeNull();
  });

  it('adds credentials-compatible bearer and CSRF headers to a state-changing request', async () => {
    setAccessToken('token');
    document.cookie = 'XSRF-TOKEN=csrf-value; Path=/';
    let seen: any;
    api.defaults.adapter = async (config) => {
      seen = config;
      return { data: {}, status: 204, statusText: 'No Content', headers: {}, config };
    };

    await api.post('/auth/logout');
    expect(seen.withCredentials).toBe(true);
    expect(seen.headers.Authorization).toBe('Bearer token');
    expect(seen.headers['X-XSRF-TOKEN']).toBe('csrf-value');
  });

  it('uses one shared refresh and retries each failed protected request only once', async () => {
    setAccessToken('old');
    let protectedAttempts = 0;
    let refreshes = 0;
    api.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') {
        refreshes += 1;
        return { data: { accessToken: 'new' }, status: 200, statusText: 'OK', headers: {}, config };
      }
      protectedAttempts += 1;
      if (protectedAttempts <= 2) return Promise.reject({ config, response: { status: 401 } });
      return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config };
    };

    const [one, two] = await Promise.all([api.get('/eleves'), api.get('/classes')]);
    expect(one.data.ok).toBe(true);
    expect(two.data.ok).toBe(true);
    expect(refreshes).toBe(1);
    expect(protectedAttempts).toBe(4);
  });

  it('does not try a refresh after a rejected login', async () => {
    let refreshes = 0;
    api.defaults.adapter = async (config) => {
      if (config.url === '/auth/refresh') refreshes += 1;
      return Promise.reject({ config, response: { status: 401 } });
    };

    await expect(api.post('/auth/login', { email: 'x@y.fr', password: 'x' })).rejects.toBeTruthy();
    expect(refreshes).toBe(0);
  });

  it('clears local auth state when refresh fails without entering a refresh loop', async () => {
    const cleared = vi.fn();
    registerClearAuth(cleared);
    api.defaults.adapter = async (config) => Promise.reject({ config, response: { status: 401 } });

    await expect(api.get('/eleves')).rejects.toBeTruthy();
    expect(cleared).toHaveBeenCalledTimes(1);
    expect(getAccessToken()).toBeNull();
  });
});
