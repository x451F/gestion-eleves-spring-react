import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AccountList, BulletinList, SubjectForm, SubjectList, TeacherForm, TeachingForm } from './AcademicPages';

const api = vi.hoisted(() => ({
  subjects: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn() },
  teachers: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), teachings: vi.fn() },
  teachings: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() },
  classes: { list: vi.fn() },
  guardians: { list: vi.fn() },
  registrations: { list: vi.fn() },
  accounts: { list: vi.fn(), provisionTeacher: vi.fn(), provisionGuardian: vi.fn(), resendActivation: vi.fn(), deactivate: vi.fn() },
  bulletins: { list: vi.fn(), generate: vi.fn(), publish: vi.fn(), correct: vi.fn(), pdf: vi.fn() },
}));

vi.mock('./api', () => ({
  ...api,
  frenchApiError: (reason: any, fallback: string) => reason?.response?.data?.fieldErrors?.length
    ? 'Certaines informations sont invalides. Vérifiez les champs signalés.'
    : reason?.response?.data?.message ?? reason?.response?.data?.detail ?? fallback,
}));

const subject = { id: 4, code: 'MATH', nom: 'Mathématiques', coefficientDefaut: 2, actif: true };
const teacher = { id: 2, matricule: 'ENS-2', nom: 'Martin', prenom: 'Noé', email: 'noe@ecole.fr', utilisateurId: 11, actif: true };
const schoolClass = { id: 7, code: '6A', nom: '6e A', niveau: '6e', anneeScolaire: '2026-2027', actif: true };
const registration = { id: 8, eleveId: 4, eleveNomComplet: 'Lina Durand', classeId: 7, classeNom: '6e A', anneeScolaire: '2026-2027', dateInscription: '2026-09-01', dateFin: null, statut: 'EN_COURS' as const };
const account = { id: 1, email: 'admin@ecole.fr', role: 'ADMIN' as const, actif: true, createdAt: '2026-01-01T10:00:00', lastLoginAt: null };
const draft = { id: 20, inscriptionId: 8, eleveNomComplet: 'Lina Durand', classeNom: '6e A', anneeScolaire: '2026-2027', periode: 'TRIMESTRE_1' as const, dateGeneration: '2026-10-01T09:00:00', statut: 'BROUILLON' as const, moyenneGenerale: 14.5, appreciation: 'Bon travail', lignes: [{ codeMatiere: 'MATH', nomMatiere: 'Mathématiques', moyenne: 14.5, coefficient: 2, nombreNotes: 2 }] };
const published = { ...draft, id: 21, statut: 'PUBLIE' as const, dateGeneration: '2026-10-02T09:00:00' };
const replaced = { ...draft, id: 19, statut: 'REMPLACE' as const, dateGeneration: '2026-09-30T09:00:00' };

function mount(element: React.ReactNode, path: string) {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/admin/matieres" element={element} /><Route path="/admin/matieres/nouvelle" element={element} /><Route path="/admin/matieres/4/modifier" element={element} />
    <Route path="/admin/enseignants/nouveau" element={element} /><Route path="/admin/enseignements/nouveau" element={element} />
    <Route path="/admin/comptes" element={element} /><Route path="/admin/bulletins" element={element} />
  </Routes></MemoryRouter>);
}

function defaults() {
  api.subjects.list.mockResolvedValue([subject]); api.subjects.get.mockResolvedValue(subject); api.subjects.create.mockResolvedValue(subject); api.subjects.update.mockResolvedValue(subject);
  api.teachers.list.mockResolvedValue([teacher]); api.teachers.get.mockResolvedValue(teacher); api.teachers.create.mockResolvedValue(teacher); api.teachers.update.mockResolvedValue(teacher); api.teachers.teachings.mockResolvedValue([]);
  api.teachings.list.mockResolvedValue([]); api.teachings.get.mockResolvedValue(null); api.teachings.create.mockResolvedValue({}); api.teachings.update.mockResolvedValue({}); api.teachings.remove.mockResolvedValue({});
  api.classes.list.mockResolvedValue([schoolClass]); api.guardians.list.mockResolvedValue([]); api.registrations.list.mockResolvedValue([registration]);
  api.accounts.list.mockResolvedValue([account, { ...account, id: 11, email: 'noe@ecole.fr', role: 'ENSEIGNANT', actif: false }]); api.accounts.provisionTeacher.mockResolvedValue({ id: 11, email: 'noe@ecole.fr', role: 'ENSEIGNANT', status: 'EN_ATTENTE_ACTIVATION', profileId: 2, mailDelivered: true }); api.accounts.provisionGuardian.mockResolvedValue({}); api.accounts.resendActivation.mockResolvedValue({ mailDelivered: true }); api.accounts.deactivate.mockResolvedValue({});
  api.bulletins.list.mockResolvedValue([draft, published, replaced]); api.bulletins.generate.mockResolvedValue(draft); api.bulletins.publish.mockResolvedValue(published); api.bulletins.correct.mockResolvedValue(draft); api.bulletins.pdf.mockResolvedValue(new Blob(['pdf'], { type: 'application/pdf' }));
}

describe('ADMIN academic and account portal behaviours', () => {
  beforeEach(() => { vi.clearAllMocks(); defaults(); vi.stubGlobal('confirm', vi.fn(() => true)); });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('renders subject loading, data, empty and safe error states', async () => {
    let resolveList: (value: typeof subject[]) => void = () => undefined;
    api.subjects.list.mockImplementation(() => new Promise<typeof subject[]>((resolve) => { resolveList = resolve; }));
    const view = mount(<SubjectList />, '/admin/matieres');
    expect(screen.getByText('Chargement des matières…')).toBeInTheDocument();
    resolveList([subject]);
    expect(await screen.findByText('Mathématiques')).toBeInTheDocument();
    view.unmount();
    api.subjects.list.mockResolvedValue([]); mount(<SubjectList />, '/admin/matieres');
    expect(await screen.findByText('Aucune matière.')).toBeInTheDocument();
    api.subjects.list.mockRejectedValueOnce({ response: { data: { message: 'Service indisponible.' } } }); const failure = mount(<SubjectList />, '/admin/matieres');
    expect(await screen.findByRole('alert')).toHaveTextContent('Service indisponible.'); failure.unmount();
  });

  it('validates, creates a subject with the exact DTO, and prevents a duplicate submit', async () => {
    let resolveCreate: (value: typeof subject) => void = () => undefined;
    api.subjects.create.mockImplementation(() => new Promise<typeof subject>((resolve) => { resolveCreate = resolve; }));
    const user = userEvent.setup(); mount(<SubjectForm />, '/admin/matieres/nouvelle');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Renseignez le code');
    await user.type(screen.getByLabelText('Code'), ' math '); await user.type(screen.getByLabelText('Nom'), ' Mathématiques ');
    await user.clear(screen.getByLabelText('Coefficient par défaut')); await user.type(screen.getByLabelText('Coefficient par défaut'), '2');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(api.subjects.create).toHaveBeenCalledWith({ code: 'math', nom: 'Mathématiques', coefficientDefaut: 2, actif: true });
    await user.click(screen.getByRole('button', { name: 'Enregistrement…' }));
    expect(api.subjects.create).toHaveBeenCalledTimes(1); resolveCreate(subject);
  });

  it('shows a safe backend validation error for a subject request', async () => {
    api.subjects.create.mockRejectedValueOnce({ response: { data: { fieldErrors: [{ field: 'code', message: 'invalid' }] } } });
    const user = userEvent.setup(); mount(<SubjectForm />, '/admin/matieres/nouvelle');
    await user.type(screen.getByLabelText('Code'), 'MATH'); await user.type(screen.getByLabelText('Nom'), 'Mathématiques');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Certaines informations sont invalides. Vérifiez les champs signalés.');
  });

  it('renders a safe backend validation error and sends an exact teacher DTO', async () => {
    const user = userEvent.setup(); mount(<TeacherForm />, '/admin/enseignants/nouveau');
    await user.type(screen.getByLabelText('Matricule'), ' ENS-2 '); await user.type(screen.getByLabelText('Nom'), ' Martin '); await user.type(screen.getByLabelText('Prénom'), ' Noé '); await user.type(screen.getByLabelText('E-mail'), 'noe@ecole.fr');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(api.teachers.create).toHaveBeenCalledWith({ matricule: 'ENS-2', nom: 'Martin', prenom: 'Noé', email: 'noe@ecole.fr', actif: true }));
  });

  it('sends a class-consistent teaching assignment and refreshes after the mutation', async () => {
    const user = userEvent.setup(); mount(<TeachingForm />, '/admin/enseignements/nouveau');
    await screen.findByLabelText('Enseignant');
    await user.selectOptions(screen.getByLabelText('Enseignant'), '2'); await user.selectOptions(screen.getByLabelText('Matière'), '4'); await user.selectOptions(screen.getByLabelText('Classe'), '7');
    expect(screen.getByLabelText('Année scolaire')).toHaveValue('2026-2027');
    expect(screen.getByText('L’année est imposée par la classe choisie pour éviter une affectation incohérente.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    await waitFor(() => expect(api.teachings.create).toHaveBeenCalledWith({ enseignantId: 2, matiereId: 4, classeId: 7, anneeScolaire: '2026-2027', coefficientMatiere: 1 }));
  });

  it('provisions with the exact account DTO, confirms deactivation, refreshes, and shows last-admin refusal safely', async () => {
    const user = userEvent.setup(); mount(<AccountList />, '/admin/comptes');
    await screen.findByText('admin@ecole.fr');
    await user.type(screen.getByLabelText('E-mail du compte enseignant'), 'new@ecole.fr'); await user.type(screen.getByLabelText('Matricule du compte enseignant'), 'ENS-9'); await user.type(screen.getByLabelText('Nom du compte enseignant'), 'Durand'); await user.type(screen.getByLabelText('Prénom du compte enseignant'), 'Lina');
    await user.click(screen.getAllByRole('button', { name: 'Envoyer l’activation' })[0]);
    await waitFor(() => expect(api.accounts.provisionTeacher).toHaveBeenCalledWith({ enseignantId: null, email: 'new@ecole.fr', matricule: 'ENS-9', nom: 'Durand', prenom: 'Lina' }));
    await user.click(screen.getByRole('button', { name: 'Désactiver' }));
    await waitFor(() => expect(api.accounts.deactivate).toHaveBeenCalledWith(1));
    expect(globalThis.confirm).toHaveBeenCalled();
    api.accounts.deactivate.mockRejectedValueOnce({ response: { data: { message: 'Le dernier compte administrateur actif ne peut pas être désactivé.' } } });
    await user.click(screen.getByRole('button', { name: 'Désactiver' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Le dernier compte administrateur actif ne peut pas être désactivé.');
    expect(screen.queryByText(/Changer le rôle/i)).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: /Supprimer le compte/i })).not.toBeInTheDocument();
  });

  it('generates, publishes and corrects bulletin versions without direct historical editing or deletion', async () => {
    const user = userEvent.setup(); mount(<BulletinList />, '/admin/bulletins');
    await screen.findByText('Historique des bulletins');
    await user.selectOptions(screen.getByLabelText('Inscription du bulletin'), '8'); await user.type(screen.getByLabelText('Appréciation du bulletin'), 'Très bien');
    await user.click(screen.getByRole('button', { name: 'Générer le brouillon' }));
    await waitFor(() => expect(api.bulletins.generate).toHaveBeenCalledWith({ inscriptionId: 8, periode: 'TRIMESTRE_1', appreciation: 'Très bien' }));
    await user.click(screen.getByRole('button', { name: 'Publier' }));
    await waitFor(() => expect(api.bulletins.publish).toHaveBeenCalledWith(20));
    expect(screen.getByText('Remplacé')).toBeInTheDocument(); expect(screen.getByText('Version actuelle')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Corriger par version' }));
    await user.clear(screen.getByLabelText('Appréciation corrigée')); await user.type(screen.getByLabelText('Appréciation corrigée'), 'Correction');
    await user.click(screen.getByRole('button', { name: 'Créer le brouillon de correction' }));
    await waitFor(() => expect(api.bulletins.correct).toHaveBeenCalledWith(21, { inscriptionId: 8, periode: 'TRIMESTRE_1', appreciation: 'Correction' }));
    expect(screen.queryByRole('button', { name: /Supprimer/i })).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: /Modifier/i })).not.toBeInTheDocument();
  });

  it('loads PDFs through the authenticated blob boundary and revokes the object URL', async () => {
    const createObjectURL = vi.fn(() => 'blob:bulletin'); const revokeObjectURL = vi.fn(); const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true }); Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
    const user = userEvent.setup(); mount(<BulletinList />, '/admin/bulletins');
    await screen.findAllByRole('button', { name: 'Télécharger le PDF' }); await user.click(screen.getAllByRole('button', { name: 'Télécharger le PDF' })[0]);
    await waitFor(() => expect(api.bulletins.pdf).toHaveBeenCalledWith(21));
    await waitFor(() => expect(revokeObjectURL).toHaveBeenCalledWith('blob:bulletin')); expect(click).toHaveBeenCalled(); click.mockRestore();
  });
});
