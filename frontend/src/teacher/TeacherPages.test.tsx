import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { TeacherDashboard, TeacherStudentDetails, TeacherStudentList } from './TeacherPages';

const api = vi.hoisted(() => ({
  teacherPortal: {
    profile: vi.fn(), assignments: vi.fn(),
    students: { list: vi.fn(), get: vi.fn(), registrations: vi.fn(), photo: vi.fn() },
    classes: { registrations: vi.fn() },
    notes: { byRegistration: vi.fn(), create: vi.fn(), update: vi.fn(), averages: vi.fn() },
    bulletins: { byRegistration: vi.fn(), pdf: vi.fn() },
  },
}));

vi.mock('../admin/api', () => ({
  ...api,
  frenchApiError: (reason: any, fallback: string) => reason?.response?.data?.fieldErrors?.length
    ? 'Certaines informations sont invalides. Vérifiez les champs signalés.'
    : reason?.response?.data?.message ?? reason?.response?.data?.detail ?? fallback,
}));

const student = { id: 1, numeroDossier: 'EL-1', nom: 'Durand', prenom: 'Lina', dateNaissance: '2012-05-09', email: 'lina@famille.fr', telephone: '0102030405', actif: true, photoDisponible: true };
const registration = { id: 8, eleveId: 1, eleveNomComplet: 'Durand Lina', classeId: 4, classeNom: '6e A', anneeScolaire: '2026-2027', dateInscription: '2026-09-01', dateFin: null, statut: 'EN_COURS' as const };
const ownNote = { id: 9, inscriptionId: 8, enseignementId: 12, matiere: 'Mathématiques', periode: 'TRIMESTRE_1' as const, valeur: 14, bareme: 20, coefficient: 2, dateEvaluation: '2026-08-02', libelle: 'Contrôle', commentaire: 'Bon travail' };
const otherRegistrationNote = { ...ownNote, id: 10, inscriptionId: 99, enseignementId: 67, matiere: 'Histoire interdite' };
const ownAssignment = { id: 42, enseignantId: 2, enseignantNomComplet: 'Professeur Test', matiereId: 2, matiereNom: 'Mathématiques', classeId: 4, classeNom: '6e A', anneeScolaire: '2026-2027', coefficientMatiere: 2 };
const published = { id: 20, inscriptionId: 8, eleveNomComplet: 'Durand Lina', classeNom: '6e A', anneeScolaire: '2026-2027', periode: 'TRIMESTRE_1' as const, dateGeneration: '2026-10-05T10:00:00', statut: 'PUBLIE' as const, moyenneGenerale: 14, appreciation: 'Très bien', lignes: [{ codeMatiere: 'MATH', nomMatiere: 'Mathématiques', moyenne: 14, coefficient: 2, nombreNotes: 2 }] };
const replaced = { ...published, id: 19, statut: 'REMPLACE' as const };

function mount(element: React.ReactNode, path = '/enseignant/eleves/1') {
  return render(<MemoryRouter initialEntries={[path]}><Routes><Route path="/enseignant" element={element} /><Route path="/enseignant/eleves" element={element} /><Route path="/enseignant/notes" element={element} /><Route path="/enseignant/bulletins" element={element} /><Route path="/enseignant/eleves/:id" element={element} /></Routes></MemoryRouter>);
}

function defaults() {
  api.teacherPortal.profile.mockResolvedValue({ id: 2, matricule: 'T-2', nom: 'Test', prenom: 'Professeur', email: 'prof@ecole.fr', utilisateurId: 2, actif: true }); api.teacherPortal.assignments.mockResolvedValue([ownAssignment]); api.teacherPortal.classes.registrations.mockResolvedValue([registration]);
  api.teacherPortal.students.list.mockResolvedValue([student]); api.teacherPortal.students.get.mockResolvedValue(student); api.teacherPortal.students.registrations.mockResolvedValue([registration]); api.teacherPortal.students.photo.mockResolvedValue(new Blob(['image'], { type: 'image/png' }));
  api.teacherPortal.notes.byRegistration.mockResolvedValue([ownNote, otherRegistrationNote]); api.teacherPortal.notes.create.mockResolvedValue(ownNote); api.teacherPortal.notes.update.mockResolvedValue(ownNote); api.teacherPortal.notes.averages.mockResolvedValue({ inscriptionId: 8, periode: 'TRIMESTRE_1', moyenneGenerale: 14, matieres: [{ matiereId: 2, nom: 'Mathématiques', moyenne: 14, coefficient: 2, nombreNotes: 1 }] });
  api.teacherPortal.bulletins.byRegistration.mockResolvedValue([published, replaced]); api.teacherPortal.bulletins.pdf.mockResolvedValue(new Blob(['pdf'], { type: 'application/pdf' }));
}

describe('teacher-visible portal behaviour', () => {
  beforeEach(() => { vi.clearAllMocks(); defaults(); });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('uses authenticated profile and assignment routes, renders no foreign assignment, and supports an empty assignment state', async () => {
    mount(<TeacherDashboard />, '/enseignant');
    expect(await screen.findByRole('heading', { name: 'Bonjour, Professeur' })).toBeInTheDocument();
    expect(api.teacherPortal.profile).toHaveBeenCalledTimes(1); expect(api.teacherPortal.assignments).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Mathématiques')).toBeInTheDocument(); expect(screen.queryByText('Histoire interdite')).not.toBeInTheDocument();
    api.teacherPortal.assignments.mockResolvedValueOnce([]); const empty = mount(<TeacherDashboard />, '/enseignant'); expect(await screen.findByText('Aucune affectation ne vous est attribuée pour le moment.')).toBeInTheDocument(); empty.unmount();
  });

  it('renders teacher student loading, populated, empty and safe-error states without management actions', async () => {
    let resolveList: (value: typeof student[]) => void = () => undefined;
    api.teacherPortal.students.list.mockImplementation(() => new Promise<typeof student[]>((resolve) => { resolveList = resolve; }));
    const view = mount(<TeacherStudentList />, '/enseignant/eleves');
    expect(screen.getByText('Chargement des élèves…')).toBeInTheDocument(); resolveList([student]);
    expect(await screen.findByText('EL-1')).toBeInTheDocument(); expect(screen.queryByRole('button', { name: /Nouvel|Supprimer|Modifier/i })).not.toBeInTheDocument(); view.unmount();
    api.teacherPortal.students.list.mockResolvedValue([]); mount(<TeacherStudentList />, '/enseignant/eleves'); expect(await screen.findByText('Aucun élève accessible.')).toBeInTheDocument();
    api.teacherPortal.students.list.mockRejectedValueOnce({ response: { data: { message: 'Service indisponible.' } } }); const failure = mount(<TeacherStudentList />, '/enseignant/eleves'); expect(await screen.findByRole('alert')).toHaveTextContent('Service indisponible.'); failure.unmount();
  });

  it('derives a class roster only from an authenticated teacher assignment', async () => {
    const user = userEvent.setup(); mount(<TeacherStudentList />, '/enseignant/eleves'); await screen.findByText('EL-1');
    await user.selectOptions(screen.getByLabelText('Classe de votre affectation'), '4');
    await waitFor(() => expect(api.teacherPortal.classes.registrations).toHaveBeenCalledWith(4));
  });

  it('loads only server-returned student registrations and the authenticated photo blob', async () => {
    const createObjectURL = vi.fn(() => 'blob:student-photo'); const revokeObjectURL = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true }); Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
    const view = mount(<TeacherStudentDetails />);
    expect(await screen.findByRole('heading', { name: 'Lina Durand' })).toBeInTheDocument();
    expect(api.teacherPortal.students.registrations).toHaveBeenCalledWith(1); expect(api.teacherPortal.students.photo).toHaveBeenCalledWith(1);
    expect(screen.getByLabelText('Inscription accessible')).toHaveDisplayValue('6e A — 2026-2027 — EN_COURS'); expect(await screen.findByRole('img', { name: 'Photo de Lina Durand' })).toHaveAttribute('src', 'blob:student-photo');
    view.unmount(); expect(revokeObjectURL).toHaveBeenCalledWith('blob:student-photo');
  });

  it('renders a safe not-found page for a foreign or invisible student', async () => {
    api.teacherPortal.students.get.mockRejectedValueOnce({ response: { status: 404 } });
    mount(<TeacherStudentDetails />);
    expect(await screen.findByRole('heading', { name: 'Page introuvable' })).toBeInTheDocument(); expect(screen.getByText('Cet élève est introuvable ou non accessible.')).toBeInTheDocument();
  });

  it('shows only notes scoped to the selected registration and sends the exact guarded update DTO once', async () => {
    let resolveUpdate: (value: typeof ownNote) => void = () => undefined;
    api.teacherPortal.notes.update.mockImplementation(() => new Promise<typeof ownNote>((resolve) => { resolveUpdate = resolve; }));
    const user = userEvent.setup(); mount(<TeacherStudentDetails />);
    await screen.findByText('Mathématiques'); expect(screen.queryByText('Histoire interdite')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Modifier' })); await user.clear(screen.getByLabelText('Valeur de l’évaluation')); await user.type(screen.getByLabelText('Valeur de l’évaluation'), '16');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(api.teacherPortal.notes.update).toHaveBeenCalledWith(9, { inscriptionId: 8, enseignementId: 12, periode: 'TRIMESTRE_1', valeur: 16, bareme: 20, coefficient: 2, dateEvaluation: '2026-08-02', libelle: 'Contrôle', commentaire: 'Bon travail' });
    await user.click(screen.getByRole('button', { name: 'Enregistrement…' })); expect(api.teacherPortal.notes.update).toHaveBeenCalledTimes(1);
    resolveUpdate(ownNote); await waitFor(() => expect(api.teacherPortal.notes.byRegistration.mock.calls.length).toBeGreaterThan(1));
  });

  it('validates note fields and presents backend validation errors safely', async () => {
    const user = userEvent.setup(); mount(<TeacherStudentDetails />); await screen.findByRole('button', { name: 'Modifier' }); await user.click(screen.getByRole('button', { name: 'Modifier' }));
    await user.clear(screen.getByLabelText('Valeur de l’évaluation')); await user.type(screen.getByLabelText('Valeur de l’évaluation'), '21'); await user.click(screen.getByRole('button', { name: 'Enregistrer' })); expect(screen.getByRole('alert')).toHaveTextContent('Renseignez une note entre 0 et 20');
    await user.clear(screen.getByLabelText('Valeur de l’évaluation')); await user.type(screen.getByLabelText('Valeur de l’évaluation'), '15'); api.teacherPortal.notes.update.mockRejectedValueOnce({ response: { data: { fieldErrors: [{ field: 'valeur', message: 'invalid' }] } } }); await user.click(screen.getByRole('button', { name: 'Enregistrer' })); expect(await screen.findByRole('alert')).toHaveTextContent('Certaines informations sont invalides. Vérifiez les champs signalés.');
  });

  it('creates a first note from the selected authenticated teacher assignment', async () => {
    const user = userEvent.setup(); mount(<TeacherStudentDetails />); await screen.findByRole('button', { name: 'Nouvelle évaluation' }); await user.click(screen.getByRole('button', { name: 'Nouvelle évaluation' }));
    expect(screen.getByLabelText('Matière accessible')).toHaveDisplayValue('Mathématiques'); await user.type(screen.getByLabelText('Valeur de l’évaluation'), '12'); await user.type(screen.getByLabelText('Libellé de l’évaluation'), 'Oral'); await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(api.teacherPortal.notes.create).toHaveBeenCalledWith({ inscriptionId: 8, enseignementId: 42, periode: 'TRIMESTRE_1', valeur: 12, bareme: 20, coefficient: 1, dateEvaluation: expect.any(String), libelle: 'Oral', commentaire: null }));
  });

  it('renders read-only bulletin snapshots and downloads a PDF through the blob boundary', async () => {
    const createObjectURL = vi.fn(() => 'blob:bulletin'); const revokeObjectURL = vi.fn(); const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true }); Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
    const user = userEvent.setup(); mount(<TeacherStudentDetails />); expect(await screen.findByText('Version actuelle')).toBeInTheDocument(); expect(screen.getByText('Remplacé')).toBeInTheDocument(); expect(screen.queryByRole('button', { name: /Publier|Corriger|Supprimer/i })).not.toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Télécharger le PDF' })[0]); await waitFor(() => expect(api.teacherPortal.bulletins.pdf).toHaveBeenCalledWith(20)); await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:bulletin')); expect(click).toHaveBeenCalled(); click.mockRestore();
  });

  it('renders a safe not-found state when a selected registration has no teacher-visible bulletins', async () => {
    api.teacherPortal.bulletins.byRegistration.mockRejectedValueOnce({ response: { status: 404 } }); mount(<TeacherStudentDetails />); expect(await screen.findAllByRole('heading', { name: 'Page introuvable' })).not.toHaveLength(0);
  });
});
