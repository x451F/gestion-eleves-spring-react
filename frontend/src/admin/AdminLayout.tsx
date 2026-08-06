import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const navigation = [
  { to: '/admin', label: 'Tableau de bord', end: true },
  { to: '/admin/eleves', label: 'Élèves' },
  { to: '/admin/classes', label: 'Classes' },
  { to: '/admin/matieres', label: 'Matières' },
  { to: '/admin/enseignants', label: 'Enseignants' },
  { to: '/admin/enseignements', label: 'Affectations' },
  { to: '/admin/comptes', label: 'Comptes' },
  { to: '/admin/bulletins', label: 'Bulletins' },
];

export default function AdminLayout() {
  const { logout, user } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <div className="app-layout">
      <header className="app-topbar"><button className="menu-button" type="button" aria-label="Ouvrir la navigation" aria-expanded={open} onClick={() => setOpen((value) => !value)}>☰</button><strong>Gestion des élèves</strong><div className="nav-user"><span>{user?.email}</span><button type="button" className="secondary-button" onClick={() => void logout()}>Se déconnecter</button></div></header>
      <aside className={`app-sidebar ${open ? 'open' : ''}`}><nav aria-label="Navigation administration">
          <p className="nav-heading">Administration</p><div className="admin-navigation">
            {navigation.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} onClick={() => setOpen(false)}>
                {item.label}
              </NavLink>
            ))}
          </div></nav></aside>
      {open && <button type="button" aria-label="Fermer la navigation" className="nav-scrim" onClick={() => setOpen(false)} />}
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
