import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

let accessToken: string | null = null;
let refreshPromise: Promise<boolean> | null = null;
let clearAuth: () => void = () => undefined;

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  withCredentials: true,
});

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export const getAccessToken = () => accessToken;

export const registerClearAuth = (callback: () => void) => {
  clearAuth = callback;
};

function csrfToken() {
  const value = document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length);
  return value ? decodeURIComponent(value) : undefined;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  if (['post', 'put', 'patch', 'delete'].includes((config.method ?? '').toLowerCase())) {
    const token = csrfToken();
    if (token) {
      config.headers['X-XSRF-TOKEN'] = token;
    }
  }

  return config;
});

async function refresh() {
  if (!refreshPromise) {
    refreshPromise = api.post('/auth/refresh')
      .then((response) => {
        accessToken = response.data.accessToken;
        return true;
      })
      .catch(() => {
        accessToken = null;
        clearAuth();
        return false;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const url = config?.url ?? '';
    const authenticationRoute = ['/auth/login', '/auth/refresh', '/auth/logout', '/auth/logout-all'].some(
      (route) => url.includes(route),
    );

    if (error.response?.status !== 401 || !config || config._retried || authenticationRoute) {
      throw error;
    }

    config._retried = true;
    if (await refresh()) {
      return api(config);
    }
    throw error;
  },
);
