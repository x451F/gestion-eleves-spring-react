import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  Classe,
  ClassePayload,
  classes,
  frenchApiError,
  Guardian,
  GuardianLink,
  guardians,
  photos,
  Registration,
  registrations,
  Student,
  StudentPayload,
  students,
} from './api';

const today = new Date().toISOString().slice(0, 10);

function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <section className="notice error" role="alert">
      <p>{message}</p>
      {retry && <button type="button" onClick={retry}>Réessayer</button>}
    </section>
  );
}

function Confirmation({ children }: { children: React.ReactNode }) {
  return <p className="history-note">{children}</p>;
}

function statusLabel(status: Registration['statut']) {
  return { EN_COURS: 'En cours', TERMINEE: 'Terminée', ANNULEE: 'Annulée' }[status];
}

function validSchoolYear(value: string) {
  return /^\d{4}-\d{4}$/.test(value) && Number(value.slice(5)) === Number(value.slice(0, 4)) + 1;
}

function studentPayload(form: StudentPayload): StudentPayload {
  return {
    ...form,
    numeroDossier: form.numeroDossier.trim(),
    nom: form.nom.trim(),
    prenom: form.prenom.trim(),
    email: form.email?.trim() || null,
    telephone: form.telephone?.trim() || null,
  };
}

export function Dashboard() {
  return (
    <section>
      <h1>Administration</h1>
      <p>Gérez les élèves, les classes, les matières, les enseignants, les comptes et les bulletins.</p>
      <div className="action-row">
        <Link className="button" to="/admin/eleves">Gérer les élèves</Link>
        <Link className="secondary-link" to="/admin/classes">Gérer les classes</Link>
        <Link className="secondary-link" to="/admin/matieres">Gérer les matières</Link>
      </div>
    </section>
  );
}

export function StudentList() {
  const [data, setData] = useState<Student[]>([]);
  const [query, setQuery] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    students.list()
      .then(setData)
      .catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les élèves.')))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  const visible = useMemo(() => {
    const needle = query.trim().toLocaleLowerCase('fr');
    if (!needle) return data;
    return data.filter((student) =>
      [student.numeroDossier, student.nom, student.prenom, student.email ?? '']
        .join(' ')
        .toLocaleLowerCase('fr')
        .includes(needle),
    );
  }, [data, query]);

  return (
    <section>
      <header className="page-header">
        <div><h1>Élèves</h1><p>Recherche parmi les élèves déjà chargés.</p></div>
        <Link className="button" to="/admin/eleves/nouveau">Nouvel élève</Link>
      </header>
      <label className="search-label">Rechercher
        <input aria-label="Rechercher un élève" value={query} onChange={(event) => setQuery(event.target.value)} />
      </label>
      {loading ? <p aria-busy="true">Chargement des élèves…</p> : error ? <ErrorState message={error} retry={load} /> : visible.length === 0 ? (
        <p>Aucun élève trouvé.</p>
      ) : (
        <div className="table-wrap"><table>
          <thead><tr><th>Dossier</th><th>Nom</th><th>Prénom</th><th>Statut</th></tr></thead>
          <tbody>{visible.map((student) => <tr key={student.id}>
            <td><Link to={`/admin/eleves/${student.id}`}>{student.numeroDossier}</Link></td>
            <td>{student.nom}</td><td>{student.prenom}</td><td>{student.actif ? 'Actif' : 'Inactif'}</td>
          </tr>)}</tbody>
        </table></div>
      )}
    </section>
  );
}

const emptyStudent: StudentPayload = {
  numeroDossier: '', nom: '', prenom: '', dateNaissance: '', email: null, telephone: null, actif: true,
};

export function StudentForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const editing = Boolean(id);
  const [form, setForm] = useState<StudentPayload>(emptyStudent);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!id) return;
    students.get(Number(id)).then((student) => {
      const { id: ignored, photoDisponible, ...payload } = student;
      void ignored; void photoDisponible;
      setForm(payload);
    }).catch((reason) => setLoadError(frenchApiError(reason, 'Impossible de charger cet élève.')));
  }, [id]);

  const change = <K extends keyof StudentPayload>(key: K, value: StudentPayload[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (pending) return;
    if (!form.numeroDossier.trim() || !form.nom.trim() || !form.prenom.trim() || !form.dateNaissance) {
      setError('Les champs obligatoires doivent être renseignés.');
      return;
    }
    if (form.dateNaissance >= today) {
      setError('La date de naissance doit être antérieure à aujourd’hui.');
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError('Saisissez une adresse e-mail valide.');
      return;
    }

    setPending(true);
    setError('');
    try {
      const saved = editing
        ? await students.update(Number(id), studentPayload(form))
        : await students.create(studentPayload(form));
      navigate(`/admin/eleves/${saved.id}`, { state: { feedback: editing ? 'Élève modifié.' : 'Élève créé.' } });
    } catch (reason) {
      setError(frenchApiError(reason, 'Impossible d’enregistrer cet élève.'));
    } finally {
      setPending(false);
    }
  };

  if (loadError) return <ErrorState message={loadError} />;
  return (
    <form className="form" onSubmit={submit} noValidate>
      <h1>{editing ? 'Modifier l’élève' : 'Nouvel élève'}</h1>
      {error && <ErrorState message={error} />}
      <label>Numéro de dossier<input required maxLength={50} value={form.numeroDossier} onChange={(event) => change('numeroDossier', event.target.value)} /></label>
      <label>Nom<input required maxLength={100} value={form.nom} onChange={(event) => change('nom', event.target.value)} /></label>
      <label>Prénom<input required maxLength={100} value={form.prenom} onChange={(event) => change('prenom', event.target.value)} /></label>
      <label>Date de naissance<input required type="date" max={today} value={form.dateNaissance} onChange={(event) => change('dateNaissance', event.target.value)} /></label>
      <label>E-mail<input type="email" maxLength={180} value={form.email ?? ''} onChange={(event) => change('email', event.target.value || null)} /></label>
      <label>Téléphone<input maxLength={30} value={form.telephone ?? ''} onChange={(event) => change('telephone', event.target.value || null)} /></label>
      <label className="checkbox"><input type="checkbox" checked={form.actif} onChange={(event) => change('actif', event.target.checked)} /> Élève actif</label>
      <div className="action-row"><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button><Link className="secondary-link" to={editing ? `/admin/eleves/${id}` : '/admin/eleves'}>Annuler</Link></div>
    </form>
  );
}

function PhotoPanel({ student, onChange }: { student: Student; onChange: () => void }) {
  const [url, setUrl] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    if (student.photoDisponible) {
      photos.load(student.id).then((blob) => {
        const objectUrl = URL.createObjectURL(blob);
        if (cancelled) {
          URL.revokeObjectURL(objectUrl);
        } else {
          setUrl(objectUrl);
        }
      }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger la photo.')));
    } else {
      setUrl(null);
    }
    return () => { cancelled = true; };
  }, [student.id, student.photoDisponible]);

  useEffect(() => {
    return () => { if (url) URL.revokeObjectURL(url); };
  }, [url]);

  const upload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Choisissez une photo JPEG, PNG ou WebP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('La photo ne doit pas dépasser 5 Mo.');
      return;
    }
    setUrl(URL.createObjectURL(file));
    setPending(true); setError('');
    try {
      await photos.upload(student.id, file);
      onChange();
    } catch (reason) {
      setError(frenchApiError(reason, 'Impossible d’enregistrer la photo.'));
    } finally {
      setPending(false);
      event.target.value = '';
    }
  };

  return <section className="panel"><h2>Photo</h2>
    {url ? <img className="student-photo" src={url} alt={`Photo de ${student.prenom} ${student.nom}`} /> : <p>Aucune photo disponible.</p>}
    {error && <ErrorState message={error} />}
    <label>Importer ou remplacer la photo (JPEG, PNG ou WebP, 5 Mo maximum)
      <input aria-label="Importer une photo" type="file" accept="image/jpeg,image/png,image/webp" disabled={pending} onChange={(event) => void upload(event)} />
    </label>
    {pending && <p aria-busy="true">Enregistrement de la photo…</p>}
  </section>;
}

function RegistrationPanel({ studentId }: { studentId: number }) {
  const [data, setData] = useState<Registration[]>([]);
  const [availableClasses, setAvailableClasses] = useState<Classe[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [pending, setPending] = useState(false);
  const [classId, setClassId] = useState('');
  const [date, setDate] = useState(today);

  const load = useCallback(() => {
    setLoading(true); setError('');
    Promise.all([students.registrations(studentId), classes.list()])
      .then(([history, classList]) => { setData(history); setAvailableClasses(classList.filter((item) => item.actif)); })
      .catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les inscriptions.')))
      .finally(() => setLoading(false));
  }, [studentId]);

  useEffect(() => { load(); }, [load]);
  const current = data.find((registration) => registration.statut === 'EN_COURS');
  const selectedClass = availableClasses.find((item) => item.id === Number(classId));

  const perform = async (message: string, action: () => Promise<unknown>) => {
    if (pending) return;
    setPending(true); setError('');
    try { await action(); setFeedback(message); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de modifier cette inscription.')); } finally { setPending(false); }
  };

  const create = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedClass || !date) { setError('Choisissez une classe et une date d’inscription.'); return; }
    void perform('Inscription créée. L’historique est conservé.', () => registrations.create({
      eleveId: studentId, classeId: selectedClass.id, anneeScolaire: selectedClass.anneeScolaire,
      dateInscription: date, dateFin: null, statut: 'EN_COURS',
    }));
  };

  return <section className="panel"><h2>Inscriptions</h2><Confirmation>Les inscriptions terminées ou annulées sont conservées dans l’historique.</Confirmation>
    {feedback && <p className="notice success" role="status">{feedback}</p>}{error && <ErrorState message={error} retry={load} />}
    {loading ? <p aria-busy="true">Chargement des inscriptions…</p> : <>
      {data.length === 0 ? <p>Aucune inscription enregistrée.</p> : <div className="table-wrap"><table><thead><tr><th>Classe</th><th>Année</th><th>Début</th><th>Fin</th><th>Statut</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td>{item.classeNom}</td><td>{item.anneeScolaire}</td><td>{item.dateInscription}</td><td>{item.dateFin ?? '—'}</td><td>{statusLabel(item.statut)}</td></tr>)}</tbody></table></div>}
      {!current ? <form className="inline-form" onSubmit={create}><h3>Créer une première inscription</h3>
        <label>Classe<select aria-label="Classe d’inscription" required value={classId} onChange={(event) => setClassId(event.target.value)}><option value="">Choisir une classe</option>{availableClasses.map((item) => <option value={item.id} key={item.id}>{item.nom} — {item.anneeScolaire}</option>)}</select></label>
        <label>Date d’inscription<input required type="date" value={date} onChange={(event) => setDate(event.target.value)} /></label><button disabled={pending}>Créer l’inscription</button>
      </form> : <div className="lifecycle-actions"><h3>Inscription en cours : {current.classeNom}</h3>
        <label>Classe de destination<select aria-label="Classe de destination" value={classId} onChange={(event) => setClassId(event.target.value)}><option value="">Choisir une classe</option>{availableClasses.filter((item) => item.id !== current.classeId).map((item) => <option value={item.id} key={item.id}>{item.nom} — {item.anneeScolaire}</option>)}</select></label>
        <label>Date de l’opération<input type="date" value={date} min={current.dateInscription} onChange={(event) => setDate(event.target.value)} /></label>
        <div className="action-row"><button type="button" disabled={pending || !classId} onClick={() => {
          if (window.confirm('Confirmer le transfert ? L’inscription actuelle sera terminée et son historique conservé.')) void perform('Transfert effectué. L’historique est conservé.', () => registrations.transfer(current.id, { classeId: Number(classId), dateTransfert: date }));
        }}>Transférer</button>
        <button type="button" className="secondary-button" disabled={pending} onClick={() => {
          if (window.confirm('Confirmer la terminaison ? Cette inscription sera conservée dans l’historique.')) void perform('Inscription terminée. L’historique est conservé.', () => registrations.terminate(current.id, date));
        }}>Terminer</button>
        <button type="button" className="danger-button" disabled={pending} onClick={() => {
          if (window.confirm('Confirmer l’annulation ? Cette inscription sera conservée dans l’historique.')) void perform('Inscription annulée. L’historique est conservé.', () => registrations.cancel(current.id, date));
        }}>Annuler l’inscription</button></div>
      </div>}
    </>}
  </section>;
}

function GuardianPanel({ studentId }: { studentId: number }) {
  const [links, setLinks] = useState<GuardianLink[]>([]);
  const [guardianList, setGuardianList] = useState<Guardian[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [pending, setPending] = useState(false);
  const [guardianId, setGuardianId] = useState('');
  const [relationship, setRelationship] = useState<GuardianLink['lienParente']>('PERE');
  const [principal, setPrincipal] = useState(false);
  const [authority, setAuthority] = useState(true);
  const [emergency, setEmergency] = useState(false);
  const [date, setDate] = useState(today);
  const [newGuardian, setNewGuardian] = useState({ nom: '', prenom: '', email: '', telephone: '' });

  const load = useCallback(() => {
    setLoading(true); setError('');
    Promise.all([students.guardianLinks(studentId), guardians.list()])
      .then(([studentLinks, allGuardians]) => { setLinks(studentLinks); setGuardianList(allGuardians.filter((item) => item.actif)); })
      .catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les responsables.')))
      .finally(() => setLoading(false));
  }, [studentId]);
  useEffect(() => { load(); }, [load]);

  const perform = async (message: string, action: () => Promise<unknown>) => {
    if (pending) return;
    setPending(true); setError('');
    try { await action(); setFeedback(message); load(); } catch (reason) { setError(frenchApiError(reason, 'Impossible de modifier cette relation.')); } finally { setPending(false); }
  };

  const linkedIds = new Set(links.map((link) => link.responsableId));
  return <section className="panel"><h2>Responsables légaux et contacts</h2>
    <Confirmation>Le serveur conserve les relations terminées, mais ne retourne pas leurs dates de validité : l’interface ne les qualifie donc pas à tort comme actives.</Confirmation>
    {feedback && <p className="notice success" role="status">{feedback}</p>}{error && <ErrorState message={error} retry={load} />}
    {loading ? <p aria-busy="true">Chargement des responsables…</p> : <>
      {links.length === 0 ? <p>Aucun responsable associé.</p> : <ul className="relationship-list">{links.map((link) => <li key={link.id}><strong>{link.responsableNomComplet}</strong> — {link.lienParente.toLowerCase()}
        {link.responsablePrincipal && <span className="marker">Responsable principal</span>}
        {link.autoriteParentale && <span className="marker">Autorité parentale</span>}
        {link.contactUrgence && <span className="marker">Contact d’urgence</span>}
        <div className="action-row"><button type="button" className="secondary-button" disabled={pending || link.responsablePrincipal} onClick={() => {
          if (window.confirm('Désigner ce responsable comme principal ?')) void perform('Responsable principal modifié.', () => guardians.setPrincipal(link.responsableId, studentId));
        }}>Définir principal</button>
        <button type="button" className="danger-button" disabled={pending} onClick={() => {
          if (window.confirm('Terminer cette relation ? Son historique sera conservé.')) void perform('Relation terminée. Son historique est conservé.', () => guardians.end(link.responsableId, studentId, date));
        }}>Terminer la relation</button></div>
      </li>)}</ul>}
      <form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (!guardianId) { setError('Choisissez un responsable.'); return; } void perform('Responsable associé.', () => guardians.link(Number(guardianId), studentId, { lienParente: relationship, responsablePrincipal: principal, autoriteParentale: authority, contactUrgence: emergency })); }}>
        <h3>Associer un responsable existant</h3><label>Responsable<select aria-label="Responsable à associer" required value={guardianId} onChange={(event) => setGuardianId(event.target.value)}><option value="">Choisir un responsable</option>{guardianList.filter((guardian) => !linkedIds.has(guardian.id)).map((guardian) => <option key={guardian.id} value={guardian.id}>{guardian.prenom} {guardian.nom}</option>)}</select></label>
        <label>Lien<select value={relationship} onChange={(event) => setRelationship(event.target.value as GuardianLink['lienParente'])}><option value="PERE">Père</option><option value="MERE">Mère</option><option value="TUTEUR">Tuteur</option><option value="AUTRE">Autre</option></select></label>
        <label className="checkbox"><input type="checkbox" checked={principal} onChange={(event) => setPrincipal(event.target.checked)} /> Responsable principal</label><label className="checkbox"><input type="checkbox" checked={authority} onChange={(event) => setAuthority(event.target.checked)} /> Autorité parentale</label><label className="checkbox"><input type="checkbox" checked={emergency} onChange={(event) => setEmergency(event.target.checked)} /> Contact d’urgence</label><button disabled={pending}>Associer</button>
      </form>
      <form className="inline-form" onSubmit={(event) => { event.preventDefault(); if (!newGuardian.nom.trim() || !newGuardian.prenom.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newGuardian.email)) { setError('Renseignez le nom, le prénom et une adresse e-mail valide du responsable.'); return; } void perform('Responsable créé et associé.', async () => { const created = await guardians.create({ nom: newGuardian.nom.trim(), prenom: newGuardian.prenom.trim(), email: newGuardian.email.trim(), telephone: newGuardian.telephone.trim() || null, actif: true }); await guardians.link(created.id, studentId, { lienParente: relationship, responsablePrincipal: principal, autoriteParentale: authority, contactUrgence: emergency }); }); }}>
        <h3>Créer et associer un responsable</h3><label>Nom<input aria-label="Nom du nouveau responsable" required maxLength={100} value={newGuardian.nom} onChange={(event) => setNewGuardian((current) => ({ ...current, nom: event.target.value }))} /></label><label>Prénom<input aria-label="Prénom du nouveau responsable" required maxLength={100} value={newGuardian.prenom} onChange={(event) => setNewGuardian((current) => ({ ...current, prenom: event.target.value }))} /></label><label>E-mail<input aria-label="E-mail du nouveau responsable" required type="email" maxLength={180} value={newGuardian.email} onChange={(event) => setNewGuardian((current) => ({ ...current, email: event.target.value }))} /></label><label>Téléphone<input aria-label="Téléphone du nouveau responsable" maxLength={30} value={newGuardian.telephone} onChange={(event) => setNewGuardian((current) => ({ ...current, telephone: event.target.value }))} /></label><button disabled={pending}>Créer et associer</button>
      </form>
    </>}
  </section>;
}

export function StudentDetails() {
  const { id } = useParams();
  const [student, setStudent] = useState<Student | null>(null);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const studentId = Number(id);
  const loadStudent = useCallback(() => {
    students.get(studentId).then(setStudent).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cet élève.')));
  }, [studentId]);
  useEffect(() => { loadStudent(); }, [loadStudent]);
  useEffect(() => {
    const state = window.history.state?.usr as { feedback?: string } | undefined;
    if (state?.feedback) setFeedback(state.feedback);
  }, []);
  if (error) return <ErrorState message={error} retry={loadStudent} />;
  if (!student) return <p aria-busy="true">Chargement de l’élève…</p>;
  return <section><header className="page-header"><div><h1>{student.prenom} {student.nom}</h1><p>Dossier : {student.numeroDossier}</p></div><Link className="button" to={`/admin/eleves/${student.id}/modifier`}>Modifier</Link></header>
    {feedback && <p className="notice success" role="status">{feedback}</p>}
    <dl className="student-details"><div><dt>Date de naissance</dt><dd>{student.dateNaissance}</dd></div><div><dt>E-mail</dt><dd>{student.email ?? 'Non renseigné'}</dd></div><div><dt>Téléphone</dt><dd>{student.telephone ?? 'Non renseigné'}</dd></div><div><dt>Statut</dt><dd>{student.actif ? 'Actif' : 'Inactif'}</dd></div></dl>
    <PhotoPanel student={student} onChange={loadStudent} /><RegistrationPanel studentId={student.id} /><GuardianPanel studentId={student.id} />
  </section>;
}

const emptyClass: ClassePayload = { code: '', nom: '', niveau: '', anneeScolaire: '', actif: true };

export function ClassList() {
  const [data, setData] = useState<Classe[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); classes.list().then(setData).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les classes.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  return <section><header className="page-header"><h1>Classes</h1><Link className="button" to="/admin/classes/nouvelle">Nouvelle classe</Link></header>{loading ? <p aria-busy="true">Chargement des classes…</p> : error ? <ErrorState message={error} retry={load} /> : data.length === 0 ? <p>Aucune classe.</p> : <div className="table-wrap"><table><thead><tr><th>Code</th><th>Classe</th><th>Niveau</th><th>Année scolaire</th></tr></thead><tbody>{data.map((item) => <tr key={item.id}><td><Link to={`/admin/classes/${item.id}`}>{item.code}</Link></td><td>{item.nom}</td><td>{item.niveau}</td><td>{item.anneeScolaire}</td></tr>)}</tbody></table></div>}</section>;
}

export function ClassForm() {
  const { id } = useParams(); const navigate = useNavigate(); const editing = Boolean(id);
  const [form, setForm] = useState<ClassePayload>(emptyClass); const [error, setError] = useState(''); const [pending, setPending] = useState(false);
  useEffect(() => { if (id) classes.get(Number(id)).then(({ id: ignored, ...payload }) => { void ignored; setForm(payload); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cette classe.'))); }, [id]);
  const update = <K extends keyof ClassePayload>(key: K, value: ClassePayload[K]) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => { event.preventDefault(); if (pending) return; if (!form.code.trim() || !form.nom.trim() || !form.niveau.trim() || !validSchoolYear(form.anneeScolaire)) { setError('Renseignez tous les champs et une année au format AAAA-AAAA (années consécutives).'); return; } setPending(true); setError(''); try { const saved = editing ? await classes.update(Number(id), form) : await classes.create(form); navigate(`/admin/classes/${saved.id}`); } catch (reason) { setError(frenchApiError(reason, 'Impossible d’enregistrer cette classe.')); } finally { setPending(false); } };
  return <form className="form" onSubmit={submit} noValidate><h1>{editing ? 'Modifier la classe' : 'Nouvelle classe'}</h1>{error && <ErrorState message={error} />}<label>Code<input required maxLength={50} value={form.code} onChange={(event) => update('code', event.target.value)} /></label><label>Nom<input required maxLength={100} value={form.nom} onChange={(event) => update('nom', event.target.value)} /></label><label>Niveau<input required maxLength={100} value={form.niveau} onChange={(event) => update('niveau', event.target.value)} /></label><label>Année scolaire<input aria-describedby="school-year-help" required pattern="\d{4}-\d{4}" value={form.anneeScolaire} onChange={(event) => update('anneeScolaire', event.target.value)} /></label><p id="school-year-help">Format attendu : 2026-2027.</p><label className="checkbox"><input type="checkbox" checked={form.actif} onChange={(event) => update('actif', event.target.checked)} /> Classe active</label><div className="action-row"><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button><Link className="secondary-link" to={editing ? `/admin/classes/${id}` : '/admin/classes'}>Annuler</Link></div></form>;
}

export function ClassDetails() {
  const { id } = useParams(); const classId = Number(id); const [item, setItem] = useState<Classe | null>(null); const [data, setData] = useState<Registration[]>([]); const [error, setError] = useState('');
  const load = useCallback(() => { Promise.all([classes.get(classId), classes.registrations(classId)]).then(([loadedClass, registrationsForClass]) => { setItem(loadedClass); setData(registrationsForClass); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cette classe.'))); }, [classId]);
  useEffect(() => { load(); }, [load]);
  if (error) return <ErrorState message={error} retry={load} />; if (!item) return <p aria-busy="true">Chargement de la classe…</p>;
  const current = data.filter((registration) => registration.statut === 'EN_COURS');
  return <section><header className="page-header"><div><h1>{item.nom}</h1><p>{item.code} — {item.niveau} — {item.anneeScolaire}</p></div><Link className="button" to={`/admin/classes/${item.id}/modifier`}>Modifier</Link></header><h2>Élèves actuellement inscrits</h2>{current.length === 0 ? <p>Aucun élève actuellement inscrit.</p> : <ul>{current.map((registration) => <li key={registration.id}><Link to={`/admin/eleves/${registration.eleveId}`}>{registration.eleveNomComplet}</Link></li>)}</ul>}</section>;
}
