import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const navigation = [
  { to: '/enseignant', label: 'Tableau de bord', end: true },
  { to: '/enseignant/eleves', label: 'Élèves et classes' },
  { to: '/enseignant/notes', label: 'Évaluations' },
  { to: '/enseignant/bulletins', label: 'Bulletins' },
];

export default function TeacherLayout() {
  const { logout, user } = useAuth();
  const [open, setOpen] = useState(false);
  return <div className="app-layout">
    <header className="app-topbar"><button className="menu-button" type="button" aria-label="Ouvrir la navigation" aria-expanded={open} onClick={() => setOpen((value) => !value)}>☰</button><strong>Gestion des élèves</strong><div className="nav-user"><span>{user?.email}</span><button type="button" className="secondary-button" onClick={() => void logout()}>Se déconnecter</button></div></header>
    <aside className={`app-sidebar ${open ? 'open' : ''}`}><nav aria-label="Navigation enseignant"><p className="nav-heading">Espace enseignant</p><div className="admin-navigation">
          {navigation.map((item) => <NavLink key={item.to} to={item.to} end={item.end} onClick={() => setOpen(false)}>{item.label}</NavLink>)}
        </div></nav></aside>
    {open && <button type="button" aria-label="Fermer la navigation" className="nav-scrim" onClick={() => setOpen(false)} />}
    <main className="admin-content"><Outlet /></main>
  </div>;
}
