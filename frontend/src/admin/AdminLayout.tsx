import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const navigation = [
  { to: '/admin', label: 'Tableau de bord', end: true },
  { to: '/admin/eleves', label: 'Élèves' },
  { to: '/admin/classes', label: 'Classes' },
];

export default function AdminLayout() {
  const { logout, user } = useAuth();

  return (
    <div className="admin-layout">
      <header className="admin-header">
        <nav aria-label="Navigation administration">
          <strong>Gestion des élèves</strong>
          <div className="admin-navigation">
            {navigation.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end}>
                {item.label}
              </NavLink>
            ))}
          </div>
          <div className="nav-user">
            <span>{user?.email}</span>
            <button type="button" className="secondary-button" onClick={() => void logout()}>
              Se déconnecter
            </button>
          </div>
        </nav>
      </header>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
