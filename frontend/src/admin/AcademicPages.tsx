import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  Account,
  accounts,
  Bulletin,
  BulletinPayload,
  BulletinPeriod,
  bulletins,
  Classe,
  classes,
  frenchApiError,
  Guardian,
  guardians,
  Registration,
  registrations,
  Subject,
  SubjectPayload,
  subjects,
  Teacher,
  TeacherPayload,
  teachers,
  Teaching,
  TeachingPayload,
  teachings,
} from './api';

const periods: Array<{ value: BulletinPeriod; label: string }> = [
  { value: 'TRIMESTRE_1', label: 'Trimestre 1' },
  { value: 'TRIMESTRE_2', label: 'Trimestre 2' },
  { value: 'TRIMESTRE_3', label: 'Trimestre 3' },
];

function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return <section className="notice error" role="alert"><p>{message}</p>{retry && <button type="button" onClick={retry}>Réessayer</button>}</section>;
}

function validEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function periodLabel(period: BulletinPeriod) {
  return periods.find((item) => item.value === period)?.label ?? period;
}

function bulletinStatusLabel(status: Bulletin['statut']) {
  return { BROUILLON: 'Brouillon', PUBLIE: 'Publié', REMPLACE: 'Remplacé' }[status];
}

function accountState(account: Account | undefined) {
  if (!account) return 'Aucun compte associé';
  return account.actif ? 'Compte actif' : 'Compte non actif (détail indisponible)';
}

const emptySubject: SubjectPayload = { code: '', nom: '', coefficientDefaut: 1, actif: true };

export function SubjectList() {
  const [data, setData] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const location = useLocation();
  const load = useCallback(() => {
    setLoading(true); setError('');
    subjects.list().then(setData).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les matières.'))).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const state = location.state as { feedback?: string } | null; if (state?.feedback) setFeedback(state.feedback); }, [location.state]);

  return <section><header className="page-header"><div><h1>Matières</h1><p>Référentiel des matières et de leurs coefficients par défaut.</p></div><Link className="button" to="/admin/matieres/nouvelle">Nouvelle matière</Link></header>
    {feedback && <p className="notice success" role="status">{feedback}</p>}{loading ? <p aria-busy="true">Chargement des matières…</p> : error ? <ErrorState message={error} retry={load} /> : data.length === 0 ? <p>Aucune matière.</p> : <div className="table-wrap"><table><thead><tr><th>Code</th><th>Nom</th><th>Coefficient</th><th>État</th><th>Action</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td>{item.code}</td><td>{item.nom}</td><td>{item.coefficientDefaut}</td><td>{item.actif ? 'Active' : 'Inactive'}</td><td><Link to={`/admin/matieres/${item.id}/modifier`}>Modifier</Link></td></tr>)}</tbody></table></div>}
  </section>;
}

export function SubjectForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const editing = Boolean(id);
  const [form, setForm] = useState<SubjectPayload>(emptySubject);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  useEffect(() => {
    if (!id) return;
    subjects.get(Number(id)).then(({ id: ignored, ...item }) => { void ignored; setForm(item); })
      .catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cette matière.')));
  }, [id]);
  const update = <K extends keyof SubjectPayload>(key: K, value: SubjectPayload[K]) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending) return;
    if (!form.code.trim() || !form.nom.trim() || !Number.isFinite(form.coefficientDefaut) || form.coefficientDefaut <= 0) {
      setError('Renseignez le code, le nom et un coefficient strictement positif.'); return;
    }
    setPending(true); setError('');
    try {
      const payload = { ...form, code: form.code.trim(), nom: form.nom.trim() };
      if (editing) await subjects.update(Number(id), payload); else await subjects.create(payload);
      navigate('/admin/matieres', { state: { feedback: editing ? 'Matière modifiée.' : 'Matière créée.' } });
    } catch (reason) { setError(frenchApiError(reason, 'Impossible d’enregistrer cette matière.')); } finally { setPending(false); }
  };
  return <form className="form" onSubmit={submit} noValidate><h1>{editing ? 'Modifier la matière' : 'Nouvelle matière'}</h1>{error && <ErrorState message={error} />}
    <label>Code<input required maxLength={50} value={form.code} onChange={(event) => update('code', event.target.value)} /></label>
    <label>Nom<input required maxLength={100} value={form.nom} onChange={(event) => update('nom', event.target.value)} /></label>
    <label>Coefficient par défaut<input aria-label="Coefficient par défaut" required type="number" min="0.01" step="0.01" value={form.coefficientDefaut} onChange={(event) => update('coefficientDefaut', Number(event.target.value))} /></label>
    <label className="checkbox"><input type="checkbox" checked={form.actif} onChange={(event) => update('actif', event.target.checked)} /> Matière active</label>
    <div className="action-row"><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button><Link className="secondary-link" to="/admin/matieres">Annuler</Link></div>
  </form>;
}

const emptyTeacher: TeacherPayload = { matricule: '', nom: '', prenom: '', email: '', actif: true };

export function TeacherList() {
  const [data, setData] = useState<Teacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); teachers.list().then(setData).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les enseignants.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  return <section><header className="page-header"><div><h1>Enseignants</h1><p>Profils enseignants et leurs affectations.</p></div><Link className="button" to="/admin/enseignants/nouveau">Nouvel enseignant</Link></header>
    {loading ? <p aria-busy="true">Chargement des enseignants…</p> : error ? <ErrorState message={error} retry={load} /> : data.length === 0 ? <p>Aucun enseignant.</p> : <div className="table-wrap"><table><thead><tr><th>Matricule</th><th>Nom</th><th>E-mail</th><th>Profil</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td><Link to={`/admin/enseignants/${item.id}`}>{item.matricule}</Link></td><td>{item.prenom} {item.nom}</td><td>{item.email}</td><td>{item.actif ? 'Actif' : 'Inactif'}</td></tr>)}</tbody></table></div>}
  </section>;
}

export function TeacherForm() {
  const { id } = useParams(); const navigate = useNavigate(); const editing = Boolean(id);
  const [form, setForm] = useState<TeacherPayload>(emptyTeacher); const [error, setError] = useState(''); const [pending, setPending] = useState(false);
  useEffect(() => { if (id) teachers.get(Number(id)).then(({ id: ignored, utilisateurId, ...item }) => { void ignored; void utilisateurId; setForm(item); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cet enseignant.'))); }, [id]);
  const update = <K extends keyof TeacherPayload>(key: K, value: TeacherPayload[K]) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (pending) return;
    if (!form.matricule.trim() || !form.nom.trim() || !form.prenom.trim() || !validEmail(form.email)) { setError('Renseignez le matricule, le nom, le prénom et une adresse e-mail valide.'); return; }
    setPending(true); setError('');
    try {
      const payload = { ...form, matricule: form.matricule.trim(), nom: form.nom.trim(), prenom: form.prenom.trim(), email: form.email.trim() };
      const saved = editing ? await teachers.update(Number(id), payload) : await teachers.create(payload);
      navigate(`/admin/enseignants/${saved.id}`, { state: { feedback: editing ? 'Enseignant modifié.' : 'Enseignant créé.' } });
    } catch (reason) { setError(frenchApiError(reason, 'Impossible d’enregistrer cet enseignant.')); } finally { setPending(false); }
  };
  return <form className="form" onSubmit={submit} noValidate><h1>{editing ? 'Modifier l’enseignant' : 'Nouvel enseignant'}</h1>{error && <ErrorState message={error} />}
    <label>Matricule<input required maxLength={50} value={form.matricule} onChange={(event) => update('matricule', event.target.value)} /></label><label>Nom<input required maxLength={100} value={form.nom} onChange={(event) => update('nom', event.target.value)} /></label><label>Prénom<input required maxLength={100} value={form.prenom} onChange={(event) => update('prenom', event.target.value)} /></label><label>E-mail<input required type="email" maxLength={180} value={form.email} onChange={(event) => update('email', event.target.value)} /></label><label className="checkbox"><input type="checkbox" checked={form.actif} onChange={(event) => update('actif', event.target.checked)} /> Profil enseignant actif</label>
    <div className="action-row"><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button><Link className="secondary-link" to="/admin/enseignants">Annuler</Link></div>
  </form>;
}

export function TeacherDetails() {
  const { id } = useParams(); const teacherId = Number(id);
  const [teacher, setTeacher] = useState<Teacher | null>(null); const [account, setAccount] = useState<Account>(); const [assignmentList, setAssignmentList] = useState<Teaching[]>([]); const [error, setError] = useState(''); const [feedback, setFeedback] = useState('');
  const load = useCallback(() => {
    setError('');
    Promise.all([teachers.get(teacherId), teachers.teachings(teacherId), accounts.list()]).then(([item, assignments, accountList]) => { setTeacher(item); setAssignmentList(assignments); setAccount(accountList.find((candidate) => candidate.id === item.utilisateurId)); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cet enseignant.')));
  }, [teacherId]);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const state = window.history.state?.usr as { feedback?: string } | undefined; if (state?.feedback) setFeedback(state.feedback); }, []);
  if (error) return <ErrorState message={error} retry={load} />;
  if (!teacher) return <p aria-busy="true">Chargement de l’enseignant…</p>;
  return <section><header className="page-header"><div><h1>{teacher.prenom} {teacher.nom}</h1><p>Matricule : {teacher.matricule}</p></div><Link className="button" to={`/admin/enseignants/${teacher.id}/modifier`}>Modifier</Link></header>
    {feedback && <p className="notice success" role="status">{feedback}</p>}<dl className="student-details"><div><dt>E-mail</dt><dd>{teacher.email}</dd></div><div><dt>Profil</dt><dd>{teacher.actif ? 'Actif' : 'Inactif'}</dd></div><div><dt>Compte</dt><dd>{accountState(account)}</dd></div><div><dt>Rôle</dt><dd>{account?.role ?? '—'}</dd></div></dl>
    <section className="panel"><h2>Affectations d’enseignement</h2>{assignmentList.length === 0 ? <p>Aucune affectation.</p> : <ul>{assignmentList.map((item) => <li key={item.id}>{item.matiereNom} — {item.classeNom} ({item.anneeScolaire}), coefficient {item.coefficientMatiere}</li>)}</ul>}<Link className="secondary-link" to="/admin/enseignements/nouveau">Gérer les affectations</Link></section>
  </section>;
}

type TeachingFormState = { enseignantId: string; matiereId: string; classeId: string; coefficientMatiere: string };
const emptyTeaching: TeachingFormState = { enseignantId: '', matiereId: '', classeId: '', coefficientMatiere: '1' };

export function TeachingList() {
  const [data, setData] = useState<Teaching[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [feedback, setFeedback] = useState(''); const [pendingId, setPendingId] = useState<number | null>(null); const location = useLocation();
  const load = useCallback(() => { setLoading(true); setError(''); teachings.list().then(setData).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les affectations.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { const state = location.state as { feedback?: string } | null; if (state?.feedback) setFeedback(state.feedback); }, [location.state]);
  const remove = async (item: Teaching) => {
    if (!window.confirm(`Supprimer l’affectation de ${item.enseignantNomComplet} ? Cette action n’est possible que si elle n’est pas référencée.`)) return;
    setPendingId(item.id); setError('');
    try { await teachings.remove(item.id); setFeedback('Affectation supprimée.'); load(); } catch (reason) { setError(frenchApiError(reason, 'Cette affectation ne peut pas être supprimée.')); } finally { setPendingId(null); }
  };
  return <section><header className="page-header"><div><h1>Affectations d’enseignement</h1><p>Chaque affectation relie un enseignant, une matière, une classe et son année.</p></div><Link className="button" to="/admin/enseignements/nouveau">Nouvelle affectation</Link></header>
    {feedback && <p className="notice success" role="status">{feedback}</p>}{error && <ErrorState message={error} retry={load} />}{loading ? <p aria-busy="true">Chargement des affectations…</p> : data.length === 0 ? <p>Aucune affectation.</p> : <div className="table-wrap"><table><thead><tr><th>Enseignant</th><th>Matière</th><th>Classe</th><th>Année</th><th>Coefficient</th><th>Actions</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td>{item.enseignantNomComplet}</td><td>{item.matiereNom}</td><td>{item.classeNom}</td><td>{item.anneeScolaire}</td><td>{item.coefficientMatiere}</td><td><div className="action-row compact-actions"><Link to={`/admin/enseignements/${item.id}/modifier`}>Modifier</Link><button type="button" className="danger-button" disabled={pendingId === item.id} onClick={() => void remove(item)}>Supprimer</button></div></td></tr>)}</tbody></table></div>}
  </section>;
}

export function TeachingForm() {
  const { id } = useParams(); const navigate = useNavigate(); const editing = Boolean(id);
  const [form, setForm] = useState<TeachingFormState>(emptyTeaching); const [teacherList, setTeacherList] = useState<Teacher[]>([]); const [subjectList, setSubjectList] = useState<Subject[]>([]); const [classList, setClassList] = useState<Classe[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [pending, setPending] = useState(false);
  const load = useCallback(() => {
    setLoading(true); setError('');
    Promise.all([teachers.list(), subjects.list(), classes.list(), editing ? teachings.get(Number(id)) : Promise.resolve(null)]).then(([loadedTeachers, loadedSubjects, loadedClasses, current]) => {
      setTeacherList(loadedTeachers.filter((item) => item.actif)); setSubjectList(loadedSubjects.filter((item) => item.actif)); setClassList(loadedClasses.filter((item) => item.actif));
      if (current) setForm({ enseignantId: String(current.enseignantId), matiereId: String(current.matiereId), classeId: String(current.classeId), coefficientMatiere: String(current.coefficientMatiere) });
    }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les données de l’affectation.'))).finally(() => setLoading(false));
  }, [editing, id]);
  useEffect(() => { load(); }, [load]);
  const selectedClass = classList.find((item) => item.id === Number(form.classeId));
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (pending) return;
    const coefficient = Number(form.coefficientMatiere);
    if (!selectedClass || !Number(form.enseignantId) || !Number(form.matiereId) || !Number.isFinite(coefficient) || coefficient <= 0) { setError('Choisissez un enseignant, une matière, une classe et un coefficient strictement positif.'); return; }
    const payload: TeachingPayload = { enseignantId: Number(form.enseignantId), matiereId: Number(form.matiereId), classeId: selectedClass.id, anneeScolaire: selectedClass.anneeScolaire, coefficientMatiere: coefficient };
    setPending(true); setError('');
    try { if (editing) await teachings.update(Number(id), payload); else await teachings.create(payload); navigate('/admin/enseignements', { state: { feedback: editing ? 'Affectation modifiée.' : 'Affectation créée.' } }); } catch (reason) { setError(frenchApiError(reason, 'Impossible d’enregistrer cette affectation.')); } finally { setPending(false); }
  };
  if (loading) return <p aria-busy="true">Chargement de l’affectation…</p>;
  return <form className="form" onSubmit={submit} noValidate><h1>{editing ? 'Modifier l’affectation' : 'Nouvelle affectation'}</h1>{error && <ErrorState message={error} retry={load} />}
    <label>Enseignant<select aria-label="Enseignant" required value={form.enseignantId} onChange={(event) => setForm((current) => ({ ...current, enseignantId: event.target.value }))}><option value="">Choisir un enseignant</option>{teacherList.map((item) => <option key={item.id} value={item.id}>{item.prenom} {item.nom} — {item.matricule}</option>)}</select></label>
    <label>Matière<select aria-label="Matière" required value={form.matiereId} onChange={(event) => setForm((current) => ({ ...current, matiereId: event.target.value }))}><option value="">Choisir une matière</option>{subjectList.map((item) => <option key={item.id} value={item.id}>{item.code} — {item.nom}</option>)}</select></label>
    <label>Classe<select aria-label="Classe" required value={form.classeId} onChange={(event) => setForm((current) => ({ ...current, classeId: event.target.value }))}><option value="">Choisir une classe</option>{classList.map((item) => <option key={item.id} value={item.id}>{item.nom} — {item.anneeScolaire}</option>)}</select></label>
    <label>Année scolaire<input aria-label="Année scolaire" readOnly value={selectedClass?.anneeScolaire ?? ''} placeholder="Choisissez une classe" /></label><p>L’année est imposée par la classe choisie pour éviter une affectation incohérente.</p>
    <label>Coefficient de la matière<input aria-label="Coefficient de la matière" required type="number" min="0.01" step="0.01" value={form.coefficientMatiere} onChange={(event) => setForm((current) => ({ ...current, coefficientMatiere: event.target.value }))} /></label>
    <div className="action-row"><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button><Link className="secondary-link" to="/admin/enseignements">Annuler</Link></div>
  </form>;
}

type ProvisionForm = { profileId: string; email: string; matricule: string; nom: string; prenom: string; telephone: string };
const emptyProvision: ProvisionForm = { profileId: '', email: '', matricule: '', nom: '', prenom: '', telephone: '' };

export function AccountList() {
  const [data, setData] = useState<Account[]>([]); const [teacherList, setTeacherList] = useState<Teacher[]>([]); const [guardianList, setGuardianList] = useState<Guardian[]>([]); const [teacherForm, setTeacherForm] = useState<ProvisionForm>(emptyProvision); const [guardianForm, setGuardianForm] = useState<ProvisionForm>(emptyProvision); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [feedback, setFeedback] = useState(''); const [pending, setPending] = useState(false);
  const load = useCallback(() => {
    setLoading(true); setError('');
    Promise.all([accounts.list(), teachers.list(), guardians.list()]).then(([accountList, loadedTeachers, loadedGuardians]) => { setData(accountList); setTeacherList(loadedTeachers); setGuardianList(loadedGuardians); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les comptes.'))).finally(() => setLoading(false));
  }, []);
  useEffect(() => { load(); }, [load]);
  const profileLabel = (account: Account) => {
    if (account.role === 'ADMIN') return 'Aucun profil métier';
    if (account.role === 'ENSEIGNANT') { const profile = teacherList.find((item) => item.utilisateurId === account.id); return profile ? `Enseignant : ${profile.prenom} ${profile.nom}` : 'Profil enseignant non détaillé'; }
    const profile = guardianList.find((item) => item.utilisateurId === account.id); return profile ? `Responsable : ${profile.prenom} ${profile.nom}` : 'Profil responsable non détaillé';
  };
  const provisionTeacher = async (event: FormEvent) => {
    event.preventDefault(); if (pending) return;
    if (!validEmail(teacherForm.email) || (!teacherForm.profileId && (!teacherForm.matricule.trim() || !teacherForm.nom.trim() || !teacherForm.prenom.trim()))) { setError('Renseignez une adresse e-mail valide et les informations du nouvel enseignant.'); return; }
    setPending(true); setError('');
    try { const result = await accounts.provisionTeacher({ enseignantId: teacherForm.profileId ? Number(teacherForm.profileId) : null, email: teacherForm.email.trim(), matricule: teacherForm.profileId ? null : teacherForm.matricule.trim(), nom: teacherForm.profileId ? null : teacherForm.nom.trim(), prenom: teacherForm.profileId ? null : teacherForm.prenom.trim() }); setFeedback(result.mailDelivered ? 'Compte enseignant créé : le lien d’activation a été envoyé.' : 'Compte enseignant créé. L’envoi du lien d’activation n’a pas abouti.'); setTeacherForm(emptyProvision); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de provisionner ce compte enseignant.')); } finally { setPending(false); }
  };
  const provisionGuardian = async (event: FormEvent) => {
    event.preventDefault(); if (pending) return;
    if (!validEmail(guardianForm.email) || (!guardianForm.profileId && (!guardianForm.nom.trim() || !guardianForm.prenom.trim()))) { setError('Renseignez une adresse e-mail valide et les informations du nouveau responsable.'); return; }
    setPending(true); setError('');
    try { const result = await accounts.provisionGuardian({ responsableId: guardianForm.profileId ? Number(guardianForm.profileId) : null, email: guardianForm.email.trim(), nom: guardianForm.profileId ? null : guardianForm.nom.trim(), prenom: guardianForm.profileId ? null : guardianForm.prenom.trim(), telephone: guardianForm.profileId ? null : guardianForm.telephone.trim() || null }); setFeedback(result.mailDelivered ? 'Compte responsable créé : le lien d’activation a été envoyé.' : 'Compte responsable créé. L’envoi du lien d’activation n’a pas abouti.'); setGuardianForm(emptyProvision); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de provisionner ce compte responsable.')); } finally { setPending(false); }
  };
  const resend = async (account: Account) => { if (pending) return; setPending(true); setError(''); try { const result = await accounts.resendActivation(account.id); setFeedback(result.mailDelivered ? 'Le lien d’activation a été renvoyé.' : 'Le lien a été renouvelé, mais son envoi n’a pas abouti.'); load(); } catch (reason) { setError(frenchApiError(reason, 'Ce compte ne peut pas recevoir de nouveau lien d’activation.')); } finally { setPending(false); } };
  const deactivate = async (account: Account) => { if (!window.confirm(`Désactiver le compte ${account.email} ? Ses sessions actives seront révoquées.`)) return; setPending(true); setError(''); try { await accounts.deactivate(account.id); setFeedback('Compte désactivé. Les sessions actives ont été révoquées.'); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de désactiver ce compte.')); } finally { setPending(false); } };
  return <section><header className="page-header"><div><h1>Comptes</h1><p>Les rôles sont immuables. Il n’existe ni inscription publique ni suppression de compte.</p></div></header>{feedback && <p className="notice success" role="status">{feedback}</p>}{error && <ErrorState message={error} retry={load} />}
    {loading ? <p aria-busy="true">Chargement des comptes…</p> : data.length === 0 ? <p>Aucun compte.</p> : <div className="table-wrap"><table><thead><tr><th>E-mail</th><th>Rôle</th><th>État</th><th>Profil</th><th>Dernière connexion</th><th>Actions</th></tr></thead><tbody>{data.map((account) => <tr key={account.id}><td>{account.email}</td><td>{account.role}</td><td>{account.actif ? 'Actif' : 'Non actif (détail indisponible)'}</td><td>{profileLabel(account)}</td><td>{account.lastLoginAt ?? 'Aucune'}</td><td><div className="action-row compact-actions">{!account.actif && <button type="button" className="secondary-button" disabled={pending} onClick={() => void resend(account)}>Renvoyer l’activation</button>}{account.actif && <button type="button" className="danger-button" disabled={pending} onClick={() => void deactivate(account)}>Désactiver</button>}</div></td></tr>)}</tbody></table></div>}
    <section className="panel"><h2>Provisionner un compte enseignant</h2><form className="inline-form" onSubmit={provisionTeacher} noValidate><label>Profil existant<select aria-label="Profil enseignant existant" value={teacherForm.profileId} onChange={(event) => { const profile = teacherList.find((item) => item.id === Number(event.target.value)); setTeacherForm((current) => ({ ...current, profileId: event.target.value, email: profile?.email ?? current.email })); }}><option value="">Créer un profil avec le compte</option>{teacherList.filter((item) => !item.utilisateurId).map((item) => <option key={item.id} value={item.id}>{item.prenom} {item.nom} — {item.matricule}</option>)}</select></label><label>E-mail<input aria-label="E-mail du compte enseignant" required type="email" value={teacherForm.email} onChange={(event) => setTeacherForm((current) => ({ ...current, email: event.target.value }))} /></label>{!teacherForm.profileId && <><label>Matricule<input aria-label="Matricule du compte enseignant" value={teacherForm.matricule} onChange={(event) => setTeacherForm((current) => ({ ...current, matricule: event.target.value }))} /></label><label>Nom<input aria-label="Nom du compte enseignant" value={teacherForm.nom} onChange={(event) => setTeacherForm((current) => ({ ...current, nom: event.target.value }))} /></label><label>Prénom<input aria-label="Prénom du compte enseignant" value={teacherForm.prenom} onChange={(event) => setTeacherForm((current) => ({ ...current, prenom: event.target.value }))} /></label></>}<button disabled={pending}>Envoyer l’activation</button></form></section>
    <section className="panel"><h2>Provisionner un compte responsable</h2><form className="inline-form" onSubmit={provisionGuardian} noValidate><label>Profil existant<select aria-label="Profil responsable existant" value={guardianForm.profileId} onChange={(event) => { const profile = guardianList.find((item) => item.id === Number(event.target.value)); setGuardianForm((current) => ({ ...current, profileId: event.target.value, email: profile?.email ?? current.email })); }}><option value="">Créer un profil avec le compte</option>{guardianList.filter((item) => !item.utilisateurId).map((item) => <option key={item.id} value={item.id}>{item.prenom} {item.nom}</option>)}</select></label><label>E-mail<input aria-label="E-mail du compte responsable" required type="email" value={guardianForm.email} onChange={(event) => setGuardianForm((current) => ({ ...current, email: event.target.value }))} /></label>{!guardianForm.profileId && <><label>Nom<input aria-label="Nom du compte responsable" value={guardianForm.nom} onChange={(event) => setGuardianForm((current) => ({ ...current, nom: event.target.value }))} /></label><label>Prénom<input aria-label="Prénom du compte responsable" value={guardianForm.prenom} onChange={(event) => setGuardianForm((current) => ({ ...current, prenom: event.target.value }))} /></label><label>Téléphone<input aria-label="Téléphone du compte responsable" value={guardianForm.telephone} onChange={(event) => setGuardianForm((current) => ({ ...current, telephone: event.target.value }))} /></label></>}<button disabled={pending}>Envoyer l’activation</button></form></section>
  </section>;
}

function BulletinVersion({ bulletin, onPublish, onCorrect, onDownload, pending }: { bulletin: Bulletin; onPublish: (item: Bulletin) => void; onCorrect: (item: Bulletin) => void; onDownload: (item: Bulletin) => void; pending: boolean }) {
  const canDownload = bulletin.statut === 'PUBLIE' || bulletin.statut === 'REMPLACE';
  return <li className="bulletin-version"><strong>{bulletinStatusLabel(bulletin.statut)}</strong>{bulletin.statut === 'PUBLIE' && <span className="marker">Version actuelle</span>}<span> — généré le {new Date(bulletin.dateGeneration).toLocaleString('fr-FR')}</span><p>Moyenne générale : {bulletin.moyenneGenerale ?? 'Non évaluée'}</p>{bulletin.appreciation && <p>Appréciation : {bulletin.appreciation}</p>}<ul>{bulletin.lignes.map((line) => <li key={`${bulletin.id}-${line.codeMatiere}`}>{line.nomMatiere} : {line.moyenne ?? 'Non évaluée'} (coef. {line.coefficient})</li>)}</ul><div className="action-row compact-actions">{bulletin.statut === 'BROUILLON' && <button type="button" disabled={pending} onClick={() => onPublish(bulletin)}>Publier</button>}{bulletin.statut === 'PUBLIE' && <button type="button" className="secondary-button" disabled={pending} onClick={() => onCorrect(bulletin)}>Corriger par version</button>}{canDownload && <button type="button" className="secondary-button" disabled={pending} onClick={() => onDownload(bulletin)}>Télécharger le PDF</button>}</div></li>;
}

export function BulletinList() {
  const [data, setData] = useState<Bulletin[]>([]); const [registrationList, setRegistrationList] = useState<Registration[]>([]); const [query, setQuery] = useState(''); const [registrationId, setRegistrationId] = useState(''); const [period, setPeriod] = useState<BulletinPeriod | ''>(''); const [generate, setGenerate] = useState<BulletinPayload>({ inscriptionId: 0, periode: 'TRIMESTRE_1', appreciation: null }); const [correction, setCorrection] = useState<{ bulletinId: number; payload: BulletinPayload } | null>(null); const [loading, setLoading] = useState(true); const [pending, setPending] = useState(false); const [error, setError] = useState(''); const [feedback, setFeedback] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([bulletins.list(), registrations.list()]).then(([bulletinList, loadedRegistrations]) => { setData(bulletinList); setRegistrationList(loadedRegistrations); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les bulletins.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => data.filter((item) => (!query.trim() || item.eleveNomComplet.toLocaleLowerCase('fr').includes(query.trim().toLocaleLowerCase('fr'))) && (!registrationId || item.inscriptionId === Number(registrationId)) && (!period || item.periode === period)), [data, period, query, registrationId]);
  const groups = useMemo(() => {
    const byKey = new Map<string, { key: string; title: string; items: Bulletin[] }>();
    [...visible]
      .sort((left, right) => new Date(right.dateGeneration).getTime() - new Date(left.dateGeneration).getTime())
      .forEach((item) => {
        const key = `${item.inscriptionId}-${item.periode}`;
        const group = byKey.get(key);
        if (group) group.items.push(item);
        else byKey.set(key, { key, title: `${item.eleveNomComplet} — ${item.classeNom} — ${periodLabel(item.periode)}`, items: [item] });
      });
    return Array.from(byKey.values());
  }, [visible]);
  const perform = async (action: () => Promise<unknown>, message: string) => { if (pending) return; setPending(true); setError(''); try { await action(); setFeedback(message); setCorrection(null); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de modifier ce bulletin.')); } finally { setPending(false); } };
  const submitGenerate = (event: FormEvent) => { event.preventDefault(); if (!generate.inscriptionId) { setError('Choisissez une inscription et une période.'); return; } const existingDraft = data.find((item) => item.inscriptionId === generate.inscriptionId && item.periode === generate.periode && item.statut === 'BROUILLON'); if (existingDraft && !window.confirm('Régénérer ce brouillon ? Le brouillon précédent sera remplacé.')) return; void perform(() => bulletins.generate(generate), existingDraft ? 'Brouillon régénéré.' : 'Brouillon généré.'); };
  const download = async (item: Bulletin) => { if (pending) return; setPending(true); setError(''); try { const blob = await bulletins.pdf(item.id); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = `bulletin-${item.id}.pdf`; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 0); setFeedback('Téléchargement du PDF lancé.'); } catch (reason) { setError(frenchApiError(reason, 'Impossible de télécharger ce PDF.')); } finally { setPending(false); } };
  return <section><header className="page-header"><div><h1>Bulletins</h1><p>Les versions publiées sont immuables ; une correction crée un nouveau brouillon et conserve l’historique.</p></div></header>{feedback && <p className="notice success" role="status">{feedback}</p>}{error && <ErrorState message={error} retry={load} />}
    <section className="panel"><h2>Générer un brouillon</h2><form className="inline-form" onSubmit={submitGenerate} noValidate><label>Inscription<select aria-label="Inscription du bulletin" value={generate.inscriptionId || ''} onChange={(event) => setGenerate((current) => ({ ...current, inscriptionId: Number(event.target.value) }))}><option value="">Choisir une inscription</option>{registrationList.map((item) => <option key={item.id} value={item.id}>{item.eleveNomComplet} — {item.classeNom} ({item.anneeScolaire})</option>)}</select></label><label>Période<select aria-label="Période du bulletin" value={generate.periode} onChange={(event) => setGenerate((current) => ({ ...current, periode: event.target.value as BulletinPeriod }))}>{periods.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label><label>Appréciation<textarea aria-label="Appréciation du bulletin" maxLength={1000} value={generate.appreciation ?? ''} onChange={(event) => setGenerate((current) => ({ ...current, appreciation: event.target.value || null }))} /></label><button disabled={pending}>Générer le brouillon</button></form></section>
    {correction && <section className="panel"><h2>Corriger un bulletin publié</h2><p>La version publiée devient « Remplacé » et un nouveau brouillon est créé ; l’historique est conservé.</p><form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (window.confirm('Confirmer la correction et la création d’un nouveau brouillon ?')) void perform(() => bulletins.correct(correction.bulletinId, correction.payload), 'Nouveau brouillon de correction créé.'); }}><label>Appréciation corrigée<textarea aria-label="Appréciation corrigée" maxLength={1000} value={correction.payload.appreciation ?? ''} onChange={(event) => setCorrection((current) => current ? { ...current, payload: { ...current.payload, appreciation: event.target.value || null } } : current)} /></label><div className="action-row"><button disabled={pending}>Créer le brouillon de correction</button><button type="button" className="secondary-button" disabled={pending} onClick={() => setCorrection(null)}>Annuler</button></div></form></section>}
    <section className="panel"><h2>Historique des bulletins</h2><div className="filters"><label>Élève<input aria-label="Filtrer par élève" value={query} onChange={(event) => setQuery(event.target.value)} /></label><label>Inscription<select aria-label="Filtrer par inscription" value={registrationId} onChange={(event) => setRegistrationId(event.target.value)}><option value="">Toutes les inscriptions</option>{registrationList.map((item) => <option key={item.id} value={item.id}>{item.eleveNomComplet} — {item.classeNom}</option>)}</select></label><label>Période<select aria-label="Filtrer par période" value={period} onChange={(event) => setPeriod(event.target.value as BulletinPeriod | '')}><option value="">Toutes les périodes</option>{periods.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label></div>{loading ? <p aria-busy="true">Chargement des bulletins…</p> : groups.length === 0 ? <p>Aucun bulletin.</p> : <div className="bulletin-groups">{groups.map((group) => <section key={group.key} className="bulletin-group"><h3>{group.title}</h3><ul className="relationship-list">{group.items.map((item) => <BulletinVersion key={item.id} bulletin={item} pending={pending} onPublish={(candidate) => { if (window.confirm('Publier ce bulletin ? La version publiée devient un instantané immuable.')) void perform(() => bulletins.publish(candidate.id), 'Bulletin publié.'); }} onCorrect={(candidate) => setCorrection({ bulletinId: candidate.id, payload: { inscriptionId: candidate.inscriptionId, periode: candidate.periode, appreciation: candidate.appreciation } })} onDownload={(candidate) => void download(candidate)} />)}</ul></section>)}</div>}</section>
  </section>;
}
