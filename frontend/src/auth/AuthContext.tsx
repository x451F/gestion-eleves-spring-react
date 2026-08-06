import { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { api, registerClearAuth, setAccessToken } from './http';
import { User } from './models';

type AuthStatus = 'initializing' | 'authenticated' | 'anonymous';

type AuthState = {
  status: AuthStatus;
  user: User | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('initializing');
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    registerClearAuth(() => {
      setAccessToken(null);
      setUser(null);
      setStatus('anonymous');
    });

    const bootstrap = async () => {
      try {
        await api.get('/auth/csrf');
        const refresh = await api.post('/auth/refresh');
        setAccessToken(refresh.data.accessToken);
        const currentUser = await api.get<User>('/auth/me');
        setUser(currentUser.data);
        setStatus('authenticated');
      } catch {
        setAccessToken(null);
        setStatus('anonymous');
      }
    };

    void bootstrap();
  }, []);

  const login = async (email: string, password: string) => {
    try {
      await api.get('/auth/csrf');
      const response = await api.post('/auth/login', { email, password });
      setAccessToken(response.data.accessToken);
      const currentUser = await api.get<User>('/auth/me');
      setUser(currentUser.data);
      setStatus('authenticated');
      return true;
    } catch {
      setAccessToken(null);
      setUser(null);
      setStatus('anonymous');
      return false;
    }
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // Local logout must remain reliable even when the network request fails.
    } finally {
      setAccessToken(null);
      setUser(null);
      setStatus('anonymous');
    }
  };

  return <AuthContext.Provider value={{ status, user, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('AuthProvider manquant');
  }
  return context;
}
