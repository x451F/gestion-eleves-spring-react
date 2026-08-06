import { FormEvent, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import AdminLayout from './admin/AdminLayout';
import { ClassDetails, ClassForm, ClassList, Dashboard, StudentDetails, StudentForm, StudentList } from './admin/AdminPages';
import { AccountList, BulletinList, SubjectForm, SubjectList, TeacherDetails, TeacherForm, TeacherList, TeachingForm, TeachingList } from './admin/AcademicPages';
import { useAuth } from './auth/AuthContext';
import { Role } from './auth/models';

function Guard({ children, roles }: { children: JSX.Element; roles?: Role[] }) {
  const auth = useAuth();
  const location = useLocation();
  if (auth.status === 'initializing') return <main aria-busy="true">Chargement de la session…</main>;
  if (auth.status === 'anonymous') return <Navigate to="/connexion" state={{ from: location }} replace />;
  if (roles && (!auth.user || !roles.includes(auth.user.role))) return <Navigate to="/acces-refuse" replace />;
  return children;
}

function Login() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending) return;
    setPending(true); setError('');
    const loggedIn = await auth.login(email, password);
    if (!loggedIn) {
      setError('Impossible de se connecter avec ces informations.');
    } else {
      const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;
      navigate(from ?? '/');
    }
    setPending(false);
  };

  return <main className="login"><form onSubmit={submit} aria-label="Connexion" noValidate><h1>Gestion des élèves</h1><p>Connectez-vous pour accéder à votre espace.</p><label>E-mail<input type="email" required autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} /></label><label>Mot de passe<input type="password" required autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} /></label>{error && <p role="alert">{error}</p>}<button disabled={pending}>{pending ? 'Connexion…' : 'Se connecter'}</button></form></main>;
}

function Home() {
  const { logout, user } = useAuth();
  return <main><h1>Bienvenue</h1><p>Session active : {user?.email}</p><button type="button" onClick={() => void logout()}>Se déconnecter</button></main>;
}

function Unauthorized() {
  return <main><h1>Accès refusé</h1><p>Vous n’êtes pas autorisé à consulter cette page.</p></main>;
}

function NotFound() {
  return <main><h1>Page introuvable</h1><p>Cette page n’existe pas.</p></main>;
}

function RoleHome({ role }: { role: Role }) {
  return <Guard roles={[role]}><Home /></Guard>;
}

export default function App() {
  return <Routes>
    <Route path="/connexion" element={<Login />} />
    <Route path="/login" element={<Navigate to="/connexion" replace />} />
    <Route path="/admin" element={<Guard roles={['ADMIN']}><AdminLayout /></Guard>}>
      <Route index element={<Dashboard />} />
      <Route path="eleves" element={<StudentList />} />
      <Route path="eleves/nouveau" element={<StudentForm />} />
      <Route path="eleves/:id" element={<StudentDetails />} />
      <Route path="eleves/:id/modifier" element={<StudentForm />} />
      <Route path="classes" element={<ClassList />} />
      <Route path="classes/nouvelle" element={<ClassForm />} />
      <Route path="classes/:id" element={<ClassDetails />} />
      <Route path="classes/:id/modifier" element={<ClassForm />} />
      <Route path="matieres" element={<SubjectList />} />
      <Route path="matieres/nouvelle" element={<SubjectForm />} />
      <Route path="matieres/:id/modifier" element={<SubjectForm />} />
      <Route path="enseignants" element={<TeacherList />} />
      <Route path="enseignants/nouveau" element={<TeacherForm />} />
      <Route path="enseignants/:id" element={<TeacherDetails />} />
      <Route path="enseignants/:id/modifier" element={<TeacherForm />} />
      <Route path="enseignements" element={<TeachingList />} />
      <Route path="enseignements/nouveau" element={<TeachingForm />} />
      <Route path="enseignements/:id/modifier" element={<TeachingForm />} />
      <Route path="comptes" element={<AccountList />} />
      <Route path="bulletins" element={<BulletinList />} />
    </Route>
    <Route path="/enseignant" element={<RoleHome role="ENSEIGNANT" />} />
    <Route path="/responsable" element={<RoleHome role="RESPONSABLE" />} />
    <Route path="/teacher" element={<Navigate to="/enseignant" replace />} />
    <Route path="/guardian" element={<Navigate to="/responsable" replace />} />
    <Route path="/acces-refuse" element={<Unauthorized />} />
    <Route path="/" element={<Guard><Home /></Guard>} />
    <Route path="*" element={<NotFound />} />
  </Routes>;
}
