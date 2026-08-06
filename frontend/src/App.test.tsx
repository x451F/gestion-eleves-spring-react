import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

const auth = vi.hoisted(() => ({
  state: { status: 'anonymous', user: null as any, login: vi.fn(), logout: vi.fn() },
}));

vi.mock('./auth/AuthContext', () => ({ useAuth: () => auth.state }));
vi.mock('./admin/AdminPages', () => ({
  Dashboard: () => <h1>Tableau de bord ADMIN</h1>, StudentList: () => <h1>Élèves</h1>, StudentForm: () => <h1>Formulaire élève</h1>, StudentDetails: () => <h1>Détail élève</h1>,
  ClassList: () => <h1>Classes</h1>, ClassForm: () => <h1>Formulaire classe</h1>, ClassDetails: () => <h1>Détail classe</h1>,
}));

const mount = (path: string) => render(<MemoryRouter initialEntries={[path]}><App /></MemoryRouter>);

describe('routing and Phase 8 authentication UI', () => {
  beforeEach(() => {
    auth.state = { status: 'anonymous', user: null, login: vi.fn(), logout: vi.fn() };
  });

  it('allows ADMIN into the administration portal and exposes its supported sections', () => {
    auth.state = { status: 'authenticated', user: { id: 1, email: 'admin@ecole.fr', role: 'ADMIN' }, login: vi.fn(), logout: vi.fn() };
    mount('/admin');
    expect(screen.getByRole('heading', { name: 'Tableau de bord ADMIN' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Élèves' })).toHaveAttribute('href', '/admin/eleves');
    expect(screen.getByRole('link', { name: 'Classes' })).toHaveAttribute('href', '/admin/classes');
  });

  it.each(['ENSEIGNANT', 'RESPONSABLE'] as const)('rejects %s from ADMIN routes', (role) => {
    auth.state = { status: 'authenticated', user: { id: 2, email: 'u@ecole.fr', role }, login: vi.fn(), logout: vi.fn() };
    mount('/admin');
    expect(screen.getByRole('heading', { name: 'Accès refusé' })).toBeInTheDocument();
  });

  it('does not render protected content during bootstrap', () => {
    auth.state = { status: 'initializing', user: null, login: vi.fn(), logout: vi.fn() };
    mount('/admin');
    expect(screen.getByText('Chargement de la session…')).toBeInTheDocument();
    expect(screen.queryByText('Tableau de bord ADMIN')).not.toBeInTheDocument();
  });

  it('prevents duplicate login submission while a request is pending', async () => {
    let resolveLogin: (value: boolean) => void = () => undefined;
    auth.state.login = vi.fn(() => new Promise<boolean>((resolve) => { resolveLogin = resolve; }));
    const user = userEvent.setup();
    mount('/connexion');
    await user.type(screen.getByLabelText('E-mail'), 'admin@ecole.fr');
    await user.type(screen.getByLabelText('Mot de passe'), 'mot-de-passe');
    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    expect(screen.getByRole('button', { name: 'Connexion…' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Connexion…' }));
    expect(auth.state.login).toHaveBeenCalledTimes(1);
    resolveLogin(false);
  });

  it('renders a neutral French login error', async () => {
    auth.state.login = vi.fn().mockResolvedValue(false);
    const user = userEvent.setup();
    mount('/connexion');
    await user.type(screen.getByLabelText('E-mail'), 'admin@ecole.fr');
    await user.type(screen.getByLabelText('Mot de passe'), 'invalide');
    await user.click(screen.getByRole('button', { name: 'Se connecter' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de se connecter avec ces informations.');
  });
});
