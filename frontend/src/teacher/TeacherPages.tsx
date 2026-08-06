import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { AxiosError } from 'axios';
import { Bulletin, BulletinPeriod, frenchApiError, Registration, Student, Teacher, TeacherNote, TeacherNotePayload, Teaching, teacherPortal } from '../admin/api';

const today = new Date().toISOString().slice(0, 10);
const periods: Array<{ value: BulletinPeriod; label: string }> = [
  { value: 'TRIMESTRE_1', label: 'Trimestre 1' },
  { value: 'TRIMESTRE_2', label: 'Trimestre 2' },
  { value: 'TRIMESTRE_3', label: 'Trimestre 3' },
];

function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return <section className="notice error" role="alert"><p>{message}</p>{retry && <button type="button" onClick={retry}>Réessayer</button>}</section>;
}

function NotFoundState({ resource }: { resource: string }) {
  return <section><h1>Page introuvable</h1><p>{resource} est introuvable ou non accessible.</p></section>;
}

function isNotFound(error: unknown) {
  return (error as AxiosError).response?.status === 404;
}

function periodLabel(period: BulletinPeriod) {
  return periods.find((item) => item.value === period)?.label ?? period;
}

function bulletinStatusLabel(status: Bulletin['statut']) {
  return { BROUILLON: 'Brouillon', PUBLIE: 'Publié', REMPLACE: 'Remplacé' }[status];
}

function StudentTable({ data, actionLabel }: { data: Student[]; actionLabel: string }) {
  return <div className="table-wrap"><table><thead><tr><th>Dossier</th><th>Nom</th><th>Prénom</th><th>État</th><th>Action</th></tr></thead><tbody>{data.map((student) => <tr key={student.id}><td>{student.numeroDossier}</td><td>{student.nom}</td><td>{student.prenom}</td><td>{student.actif ? 'Actif' : 'Inactif'}</td><td><Link to={`/enseignant/eleves/${student.id}`}>{actionLabel}</Link></td></tr>)}</tbody></table></div>;
}

export function TeacherDashboard() {
  const [profile, setProfile] = useState<Teacher | null>(null); const [assignments, setAssignments] = useState<Teaching[]>([]); const [students, setStudents] = useState<Student[]>([]); const [error, setError] = useState(''); const [loading, setLoading] = useState(true);
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([teacherPortal.profile(), teacherPortal.assignments(), teacherPortal.students.list()]).then(([me, ownAssignments, visibleStudents]) => { setProfile(me); setAssignments(ownAssignments); setStudents(visibleStudents); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger votre espace.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  return <section><header className="page-header"><div><h1>Bonjour{profile ? `, ${profile.prenom}` : ''}</h1><p>Vos classes, évaluations et bulletins, dans le périmètre de vos affectations.</p></div></header>
    {loading ? <p aria-busy="true">Chargement de votre espace…</p> : error ? <ErrorState message={error} retry={load} /> : <><div className="metric-grid"><div><dt>Élèves accessibles</dt><dd>{students.length}</dd></div><div><dt>Affectations</dt><dd>{assignments.length}</dd></div><div><dt>Classes</dt><dd>{new Set(assignments.map((item) => item.classeId)).size}</dd></div></div>
    <section className="panel"><h2>Mes affectations</h2>{assignments.length === 0 ? <p>Aucune affectation ne vous est attribuée pour le moment.</p> : <div className="assignment-grid">{assignments.map((assignment) => <article key={assignment.id} className="assignment-card"><h3>{assignment.matiereNom}</h3><p>{assignment.classeNom} · {assignment.anneeScolaire}</p><p>Coefficient de matière : {assignment.coefficientMatiere}</p><Link className="secondary-link" to={`/enseignant/eleves?classe=${assignment.classeId}`}>Voir les élèves de cette classe</Link></article>)}</div>}</section></>}
    <div className="action-row"><Link className="button" to="/enseignant/eleves">Consulter les élèves</Link><Link className="secondary-link" to="/enseignant/notes">Saisir des évaluations</Link></div>
  </section>;
}

export function TeacherStudentList({ mode = 'students' }: { mode?: 'students' | 'notes' | 'bulletins' }) {
  const [data, setData] = useState<Student[]>([]); const [assignments, setAssignments] = useState<Teaching[]>([]); const [roster, setRoster] = useState<Registration[] | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [params, setParams] = useSearchParams();
  const selectedClass = Number(params.get('classe')) || 0;
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([teacherPortal.assignments(), teacherPortal.students.list()]).then(([ownAssignments, visibleStudents]) => { setAssignments(ownAssignments); setData(visibleStudents); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger les élèves accessibles.'))).finally(() => setLoading(false)); }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { if (!selectedClass || !assignments.some((item) => item.classeId === selectedClass)) { setRoster(null); return; } teacherPortal.classes.registrations(selectedClass).then(setRoster).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger cette classe.'))); }, [assignments, selectedClass]);
  const content = mode === 'notes' ? { title: 'Évaluations', text: 'Choisissez un élève puis une inscription accessible pour consulter et modifier vos évaluations.', action: 'Consulter les évaluations' } : mode === 'bulletins' ? { title: 'Bulletins', text: 'Choisissez un élève puis une inscription accessible pour consulter les bulletins en lecture seule.', action: 'Consulter les bulletins' } : { title: 'Élèves et classes accessibles', text: 'Les inscriptions affichées dans le détail sont limitées à vos classes et années d’enseignement.', action: 'Ouvrir le dossier' };
  const classes = Array.from(new Map(assignments.map((item) => [item.classeId, item])).values()); const shown = roster ? data.filter((student) => roster.some((registration) => registration.eleveId === student.id)) : data;
  return <section><header className="page-header"><div><h1>{content.title}</h1><p>{content.text}</p></div></header>{loading ? <p aria-busy="true">Chargement des élèves…</p> : error ? <ErrorState message={error} retry={load} /> : assignments.length === 0 ? <p>Aucune affectation ne vous permet d’afficher une classe.</p> : <><label className="class-filter">Classe de votre affectation<select aria-label="Classe de votre affectation" value={selectedClass} onChange={(event) => setParams(event.target.value ? { classe: event.target.value } : {})}><option value="">Toutes mes classes</option>{classes.map((assignment) => <option key={assignment.classeId} value={assignment.classeId}>{assignment.classeNom} — {assignment.anneeScolaire}</option>)}</select></label>{shown.length === 0 ? <p>Aucun élève accessible.</p> : <StudentTable data={shown} actionLabel={content.action} />}</>}</section>;
}

function TeacherPhoto({ student }: { student: Student }) {
  const [url, setUrl] = useState<string | null>(null); const [error, setError] = useState('');
  useEffect(() => {
    let cancelled = false;
    if (!student.photoDisponible) { setUrl(null); return () => { cancelled = true; }; }
    teacherPortal.students.photo(student.id).then((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      if (cancelled) URL.revokeObjectURL(objectUrl); else setUrl(objectUrl);
    }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger la photo.')));
    return () => { cancelled = true; };
  }, [student.id, student.photoDisponible]);
  useEffect(() => () => { if (url) URL.revokeObjectURL(url); }, [url]);
  return <section className="panel"><h2>Photo</h2>{error && <ErrorState message={error} />}{url ? <img className="student-photo" src={url} alt={`Photo de ${student.prenom} ${student.nom}`} /> : <p>Aucune photo disponible.</p>}</section>;
}

function NoteEditor({ note, registration, teachingOptions, onSaved }: { note?: TeacherNote; registration: Registration; teachingOptions: Teaching[]; onSaved: () => void }) {
  const [form, setForm] = useState({ enseignementId: String(note?.enseignementId ?? teachingOptions[0]?.id ?? ''), periode: note?.periode ?? 'TRIMESTRE_1' as BulletinPeriod, valeur: note ? String(note.valeur) : '', coefficient: note ? String(note.coefficient) : '1', dateEvaluation: note?.dateEvaluation ?? today, libelle: note?.libelle ?? '', commentaire: note?.commentaire ?? '' });
  const [error, setError] = useState(''); const [pending, setPending] = useState(false);
  const update = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) => setForm((current) => ({ ...current, [key]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault(); if (pending) return;
    const valeur = Number(form.valeur); const coefficient = Number(form.coefficient);
    if (!Number.isFinite(valeur) || valeur < 0 || valeur > 20 || !Number.isFinite(coefficient) || coefficient <= 0 || !form.dateEvaluation || form.dateEvaluation > today || form.libelle.length > 150 || form.commentaire.length > 500) { setError('Renseignez une note entre 0 et 20, un coefficient positif et une date non future.'); return; }
    const payload: TeacherNotePayload = { inscriptionId: registration.id, enseignementId: Number(form.enseignementId), periode: form.periode, valeur, bareme: 20, coefficient, dateEvaluation: form.dateEvaluation, libelle: form.libelle.trim() || null, commentaire: form.commentaire.trim() || null };
    setPending(true); setError('');
    try { if (note) await teacherPortal.notes.update(note.id, payload); else await teacherPortal.notes.create(payload); onSaved(); } catch (reason) { setError(frenchApiError(reason, 'Impossible d’enregistrer cette évaluation.')); } finally { setPending(false); }
  };
  return <form className="inline-form note-editor" onSubmit={submit} noValidate><h3>{note ? 'Modifier l’évaluation' : 'Nouvelle évaluation'}</h3>{error && <ErrorState message={error} />}{!note && <label>Matière accessible<select aria-label="Matière accessible" value={form.enseignementId} onChange={(event) => update('enseignementId', event.target.value)}>{teachingOptions.map((item) => <option key={item.id} value={item.id}>{item.matiereNom}</option>)}</select></label>}<label>Période<select aria-label="Période de l’évaluation" value={form.periode} onChange={(event) => update('periode', event.target.value as BulletinPeriod)}>{periods.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label><label>Note sur 20<input aria-label="Valeur de l’évaluation" required type="number" min="0" max="20" step="0.01" value={form.valeur} onChange={(event) => update('valeur', event.target.value)} /></label><label>Barème<input aria-label="Barème de l’évaluation" readOnly value="20" /></label><label>Coefficient<input aria-label="Coefficient de l’évaluation" required type="number" min="0.01" step="0.01" value={form.coefficient} onChange={(event) => update('coefficient', event.target.value)} /></label><label>Date<input aria-label="Date de l’évaluation" required type="date" max={today} value={form.dateEvaluation} onChange={(event) => update('dateEvaluation', event.target.value)} /></label><label>Libellé<input aria-label="Libellé de l’évaluation" maxLength={150} value={form.libelle} onChange={(event) => update('libelle', event.target.value)} /></label><label>Commentaire<textarea aria-label="Commentaire de l’évaluation" maxLength={500} value={form.commentaire} onChange={(event) => update('commentaire', event.target.value)} /></label><button disabled={pending}>{pending ? 'Enregistrement…' : 'Enregistrer'}</button></form>;
}

function TeacherNotes({ registration, assignments }: { registration: Registration; assignments: Teaching[] }) {
  const [data, setData] = useState<TeacherNote[]>([]); const [period, setPeriod] = useState<BulletinPeriod>('TRIMESTRE_1'); const [average, setAverage] = useState<number | null>(null); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [editing, setEditing] = useState<number | 'new' | null>(null); const [feedback, setFeedback] = useState('');
  const load = useCallback(() => { setLoading(true); setError(''); Promise.all([teacherPortal.notes.byRegistration(registration.id), teacherPortal.notes.averages(registration.id, period)]).then(([notes, averages]) => { setData(notes.filter((note) => note.inscriptionId === registration.id)); setAverage(averages.moyenneGenerale); }).catch((reason) => setError(frenchApiError(reason, 'Impossible de charger vos évaluations.'))).finally(() => setLoading(false)); }, [period, registration.id]);
  useEffect(() => { load(); }, [load]);
  const visible = useMemo(() => data.filter((note) => note.periode === period), [data, period]);
  const teachingOptions = useMemo(() => assignments.filter((item) => item.classeId === registration.classeId && item.anneeScolaire === registration.anneeScolaire), [assignments, registration.anneeScolaire, registration.classeId]);
  return <section className="panel"><h2>Évaluations</h2><p>Seules les évaluations que le serveur rattache à vos propres enseignements sont affichées.</p><label>Période affichée<select aria-label="Période affichée" value={period} onChange={(event) => setPeriod(event.target.value as BulletinPeriod)}>{periods.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</select></label>{feedback && <p className="notice success" role="status">{feedback}</p>}{loading ? <p aria-busy="true">Chargement des évaluations…</p> : error ? <ErrorState message={error} retry={load} /> : <><p>Moyenne calculée pour vos matières : {average ?? 'Non évaluée'}</p>{visible.length === 0 ? <p>Aucune évaluation pour cette période.</p> : <ul className="relationship-list">{visible.map((note) => <li key={note.id}><strong>{note.matiere}</strong> — {note.valeur}/{note.bareme}, coefficient {note.coefficient}<br />{periodLabel(note.periode)} — {note.dateEvaluation}{note.libelle && <> — {note.libelle}</>}{note.commentaire && <p>{note.commentaire}</p>}<div className="action-row"><button type="button" className="secondary-button" onClick={() => setEditing(note.id)}>Modifier</button></div>{editing === note.id && <NoteEditor note={note} registration={registration} teachingOptions={teachingOptions} onSaved={() => { setEditing(null); setFeedback('Évaluation modifiée.'); load(); }} />}</li>)}</ul>}{teachingOptions.length > 0 ? <><div className="action-row"><button type="button" onClick={() => setEditing('new')}>Nouvelle évaluation</button></div>{editing === 'new' && <NoteEditor registration={registration} teachingOptions={teachingOptions} onSaved={() => { setEditing(null); setFeedback('Évaluation créée.'); load(); }} />}</> : <p className="history-note">Aucune de vos affectations ne correspond à cette inscription.</p>}</>}</section>;
}

function TeacherBulletins({ registration }: { registration: Registration }) {
  const [data, setData] = useState<Bulletin[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [notFound, setNotFound] = useState(false); const [pending, setPending] = useState(false);
  const load = useCallback(() => { setLoading(true); setError(''); setNotFound(false); teacherPortal.bulletins.byRegistration(registration.id).then((items) => setData(items.filter((item) => item.inscriptionId === registration.id))).catch((reason) => { if (isNotFound(reason)) setNotFound(true); else setError(frenchApiError(reason, 'Impossible de charger les bulletins.')); }).finally(() => setLoading(false)); }, [registration.id]);
  useEffect(() => { load(); }, [load]);
  const download = async (bulletin: Bulletin) => { setPending(true); setError(''); try { const blob = await teacherPortal.bulletins.pdf(bulletin.id); const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = `bulletin-${bulletin.id}.pdf`; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 0); } catch (reason) { if (isNotFound(reason)) setNotFound(true); else setError(frenchApiError(reason, 'Impossible de télécharger ce bulletin.')); } finally { setPending(false); } };
  if (notFound) return <NotFoundState resource="Ce bulletin" />;
  return <section className="panel"><h2>Bulletins</h2><p>Les snapshots sont consultables en lecture seule.</p>{loading ? <p aria-busy="true">Chargement des bulletins…</p> : error ? <ErrorState message={error} retry={load} /> : data.length === 0 ? <p>Aucun bulletin accessible pour cette inscription.</p> : <ul className="relationship-list">{data.map((bulletin) => <li key={bulletin.id}><strong>{bulletinStatusLabel(bulletin.statut)}</strong>{bulletin.statut === 'PUBLIE' && <span className="marker">Version actuelle</span>} — {periodLabel(bulletin.periode)}<p>Moyenne générale du snapshot : {bulletin.moyenneGenerale ?? 'Non évaluée'}</p>{bulletin.appreciation && <p>Appréciation : {bulletin.appreciation}</p>}<ul>{bulletin.lignes.map((line) => <li key={`${bulletin.id}-${line.codeMatiere}`}>{line.nomMatiere} : {line.moyenne ?? 'Non évaluée'} (coef. {line.coefficient})</li>)}</ul>{(bulletin.statut === 'PUBLIE' || bulletin.statut === 'REMPLACE') && <button type="button" className="secondary-button" disabled={pending} onClick={() => void download(bulletin)}>Télécharger le PDF</button>}</li>)}</ul>}</section>;
}

export function TeacherStudentDetails() {
  const { id } = useParams(); const studentId = Number(id); const [student, setStudent] = useState<Student | null>(null); const [registrations, setRegistrations] = useState<Registration[]>([]); const [assignments, setAssignments] = useState<Teaching[]>([]); const [registrationId, setRegistrationId] = useState(''); const [error, setError] = useState(''); const [notFound, setNotFound] = useState(false);
  const load = useCallback(async () => { setError(''); setNotFound(false); try { const loadedStudent = await teacherPortal.students.get(studentId); const [loadedRegistrations, ownAssignments] = await Promise.all([teacherPortal.students.registrations(studentId), teacherPortal.assignments()]); setStudent(loadedStudent); setRegistrations(loadedRegistrations); setAssignments(ownAssignments); setRegistrationId((current) => current || String(loadedRegistrations[0]?.id ?? '')); } catch (reason) { if (isNotFound(reason)) setNotFound(true); else setError(frenchApiError(reason, 'Impossible de charger cet élève.')); } }, [studentId]);
  useEffect(() => { void load(); }, [load]);
  if (notFound) return <NotFoundState resource="Cet élève" />;
  if (error) return <ErrorState message={error} retry={() => void load()} />;
  if (!student) return <p aria-busy="true">Chargement de l’élève…</p>;
  const selectedRegistration = registrations.find((item) => item.id === Number(registrationId));
  return <section><header className="page-header"><div><h1>{student.prenom} {student.nom}</h1><p>Dossier : {student.numeroDossier}</p></div><Link className="secondary-link" to="/enseignant/eleves">Retour aux élèves</Link></header><dl className="student-details"><div><dt>État</dt><dd>{student.actif ? 'Actif' : 'Inactif'}</dd></div><div><dt>Informations visibles</dt><dd>Dossier, identité et inscriptions accessibles</dd></div></dl><TeacherPhoto student={student} /><section className="panel"><h2>Inscriptions accessibles</h2>{registrations.length === 0 ? <p>Aucune inscription accessible.</p> : <label>Inscription<select aria-label="Inscription accessible" value={registrationId} onChange={(event) => setRegistrationId(event.target.value)}>{registrations.map((item) => <option key={item.id} value={item.id}>{item.classeNom} — {item.anneeScolaire} — {item.statut}</option>)}</select></label>}</section>{selectedRegistration && <><TeacherNotes registration={selectedRegistration} assignments={assignments} /><TeacherBulletins registration={selectedRegistration} /></>}</section>;
}
