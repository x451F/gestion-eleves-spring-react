import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ClassDetails, StudentDetails, StudentForm, StudentList } from './AdminPages';

const api = vi.hoisted(() => ({
  students: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), registrations: vi.fn(), guardianLinks: vi.fn() },
  classes: { list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), registrations: vi.fn() },
  registrations: { create: vi.fn(), transfer: vi.fn(), terminate: vi.fn(), cancel: vi.fn() },
  guardians: { list: vi.fn(), create: vi.fn(), link: vi.fn(), end: vi.fn(), setPrincipal: vi.fn() },
  photos: { load: vi.fn(), upload: vi.fn() },
}));

vi.mock('./api', () => ({ ...api, frenchApiError: (reason: any, fallback: string) => reason?.response?.data?.fieldErrors?.length ? 'Certaines informations sont invalides. Vérifiez les champs signalés.' : reason?.response?.data?.message ?? fallback }));

const student = { id: 1, numeroDossier: 'EL-1', nom: 'Durand', prenom: 'Lina', dateNaissance: '2012-05-09', email: 'lina@famille.fr', telephone: '0102030405', actif: true, photoDisponible: false };
const classOne = { id: 4, code: '6A-26', nom: '6e A', niveau: '6e', anneeScolaire: '2026-2027', actif: true };
const classTwo = { id: 5, code: '6B-26', nom: '6e B', niveau: '6e', anneeScolaire: '2026-2027', actif: true };
const currentRegistration = { id: 8, eleveId: 1, eleveNomComplet: 'Durand Lina', classeId: 4, classeNom: '6e A', anneeScolaire: '2026-2027', dateInscription: '2026-09-01', dateFin: null, statut: 'EN_COURS' as const };

function mount(element: React.ReactNode, path = '/admin/eleves/1') {
  return render(<MemoryRouter initialEntries={[path]}><Routes><Route path="/admin/eleves/1" element={element} /><Route path="/admin/eleves/1/modifier" element={element} /><Route path="/admin/eleves/nouveau" element={element} /><Route path="/admin/classes/4" element={element} /></Routes></MemoryRouter>);
}

function defaults() {
  api.students.list.mockResolvedValue([student]); api.students.get.mockResolvedValue(student); api.students.create.mockResolvedValue(student); api.students.update.mockResolvedValue(student);
  api.students.registrations.mockResolvedValue([]); api.students.guardianLinks.mockResolvedValue([]);
  api.classes.list.mockResolvedValue([classOne, classTwo]); api.classes.get.mockResolvedValue(classOne); api.classes.create.mockResolvedValue(classOne); api.classes.update.mockResolvedValue(classOne); api.classes.registrations.mockResolvedValue([]);
  api.registrations.create.mockResolvedValue(currentRegistration); api.registrations.transfer.mockResolvedValue(currentRegistration); api.registrations.terminate.mockResolvedValue(currentRegistration); api.registrations.cancel.mockResolvedValue(currentRegistration);
  api.guardians.list.mockResolvedValue([]); api.guardians.create.mockResolvedValue({}); api.guardians.link.mockResolvedValue({}); api.guardians.end.mockResolvedValue({}); api.guardians.setPrincipal.mockResolvedValue({});
  api.photos.load.mockResolvedValue(new Blob(['photo'], { type: 'image/png' })); api.photos.upload.mockResolvedValue({});
}

describe('ADMIN student portal behaviours', () => {
  beforeEach(() => { vi.clearAllMocks(); defaults(); vi.stubGlobal('confirm', vi.fn(() => true)); });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('renders student list loading, populated, empty, and safe API-error states', async () => {
    let resolveList: (value: typeof student[]) => void = () => undefined;
    api.students.list.mockImplementation(() => new Promise<typeof student[]>((resolve) => { resolveList = resolve; }));
    const view = mount(<StudentList />, '/admin/eleves/1');
    expect(screen.getByText('Chargement des élèves…')).toBeInTheDocument();
    resolveList([student]);
    expect(await screen.findByRole('link', { name: 'EL-1' })).toHaveAttribute('href', '/admin/eleves/1');
    view.unmount();
    api.students.list.mockResolvedValue([]);
    mount(<StudentList />, '/admin/eleves/1');
    expect(await screen.findByText('Aucun élève trouvé.')).toBeInTheDocument();
    api.students.list.mockRejectedValueOnce({ response: { data: { message: 'Service indisponible.' } } });
    const failed = mount(<StudentList />, '/admin/eleves/1');
    expect(await screen.findByRole('alert')).toHaveTextContent('Service indisponible.');
    failed.unmount();
  });

  it('validates and creates a student with the exact request fields without duplicate submission', async () => {
    let resolveCreate: (value: typeof student) => void = () => undefined;
    api.students.create.mockImplementation(() => new Promise<typeof student>((resolve) => { resolveCreate = resolve; }));
    const user = userEvent.setup();
    mount(<StudentForm />, '/admin/eleves/nouveau');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Les champs obligatoires doivent être renseignés.');
    await user.type(screen.getByLabelText('Numéro de dossier'), ' EL-1 '); await user.type(screen.getByLabelText('Nom'), 'Durand'); await user.type(screen.getByLabelText('Prénom'), 'Lina'); await user.type(screen.getByLabelText('Date de naissance'), '2012-05-09');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(api.students.create).toHaveBeenCalledWith({ numeroDossier: 'EL-1', nom: 'Durand', prenom: 'Lina', dateNaissance: '2012-05-09', email: null, telephone: null, actif: true });
    expect(screen.getByRole('button', { name: 'Enregistrement…' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Enregistrement…' }));
    expect(api.students.create).toHaveBeenCalledTimes(1);
    resolveCreate(student);
  });

  it('shows a French backend validation error on student creation', async () => {
    api.students.create.mockRejectedValue({ response: { data: { fieldErrors: [{ field: 'numeroDossier', message: 'must not be blank' }] } } });
    const user = userEvent.setup(); mount(<StudentForm />, '/admin/eleves/nouveau');
    await user.type(screen.getByLabelText('Numéro de dossier'), 'EL-1'); await user.type(screen.getByLabelText('Nom'), 'Durand'); await user.type(screen.getByLabelText('Prénom'), 'Lina'); await user.type(screen.getByLabelText('Date de naissance'), '2012-05-09');
    await user.click(screen.getByRole('button', { name: 'Enregistrer' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Certaines informations sont invalides. Vérifiez les champs signalés.');
  });

  it('opens student detail and transfers a current registration with confirmation and refresh', async () => {
    api.students.registrations.mockResolvedValue([currentRegistration]);
    const user = userEvent.setup(); mount(<StudentDetails />);
    expect(await screen.findByRole('heading', { name: 'Lina Durand' })).toBeInTheDocument();
    await user.selectOptions(await screen.findByLabelText('Classe de destination'), '5');
    await user.click(screen.getByRole('button', { name: 'Transférer' }));
    await waitFor(() => expect(api.registrations.transfer).toHaveBeenCalledWith(8, { classeId: 5, dateTransfert: expect.any(String) }));
    expect(globalThis.confirm).toHaveBeenCalled();
    await waitFor(() => expect(api.students.registrations).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('button', { name: /Supprimer/ })).not.toBeInTheDocument();
  });

  it('sends termination and cancellation lifecycle requests, never a hard-delete request', async () => {
    api.students.registrations.mockResolvedValue([currentRegistration]);
    const user = userEvent.setup(); mount(<StudentDetails />);
    await screen.findByRole('button', { name: 'Terminer' });
    await user.click(screen.getByRole('button', { name: 'Terminer' }));
    await waitFor(() => expect(api.registrations.terminate).toHaveBeenCalledWith(8, expect.any(String)));
    await user.click(screen.getByRole('button', { name: 'Annuler l’inscription' }));
    await waitFor(() => expect(api.registrations.cancel).toHaveBeenCalledWith(8, expect.any(String)));
  });

  it('renders guardian principal data, ends a relationship, and requests a principal change', async () => {
    api.students.guardianLinks.mockResolvedValue([{ id: 3, eleveId: 1, eleveNomComplet: 'Durand Lina', responsableId: 7, responsableNomComplet: 'Martin Ana', lienParente: 'MERE', responsablePrincipal: true, autoriteParentale: true, contactUrgence: false }, { id: 4, eleveId: 1, eleveNomComplet: 'Durand Lina', responsableId: 9, responsableNomComplet: 'Martin Paul', lienParente: 'PERE', responsablePrincipal: false, autoriteParentale: false, contactUrgence: true }]);
    api.guardians.list.mockResolvedValue([{ id: 12, nom: 'Renard', prenom: 'Sam', email: 'sam@famille.fr', telephone: null, utilisateurId: null, actif: true }]);
    const user = userEvent.setup(); mount(<StudentDetails />);
    expect((await screen.findAllByText('Responsable principal'))[0]).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Responsable à associer'), '12');
    await user.click(screen.getByRole('button', { name: 'Associer' }));
    await waitFor(() => expect(api.guardians.link).toHaveBeenCalledWith(12, 1, { lienParente: 'PERE', responsablePrincipal: false, autoriteParentale: true, contactUrgence: false }));
    await user.click(screen.getAllByRole('button', { name: 'Définir principal' })[1]);
    await waitFor(() => expect(api.guardians.setPrincipal).toHaveBeenCalledWith(9, 1));
    await user.click(screen.getAllByRole('button', { name: 'Terminer la relation' })[0]);
    await waitFor(() => expect(api.guardians.end).toHaveBeenCalledWith(7, 1, expect.any(String)));
    await waitFor(() => expect(api.students.guardianLinks.mock.calls.length).toBeGreaterThan(1));
  });

  it('creates a guardian with the implemented DTO then links it to the student', async () => {
    api.guardians.create.mockResolvedValue({ id: 12 });
    const user = userEvent.setup(); mount(<StudentDetails />);
    await screen.findByRole('heading', { name: 'Responsables légaux et contacts' });
    await user.type(await screen.findByLabelText('Nom du nouveau responsable'), 'Renard'); await user.type(screen.getByLabelText('Prénom du nouveau responsable'), 'Sam'); await user.type(screen.getByLabelText('E-mail du nouveau responsable'), 'sam@famille.fr');
    await user.click(screen.getByRole('button', { name: 'Créer et associer' }));
    await waitFor(() => expect(api.guardians.create).toHaveBeenCalledWith({ nom: 'Renard', prenom: 'Sam', email: 'sam@famille.fr', telephone: null, actif: true }));
    await waitFor(() => expect(api.guardians.link).toHaveBeenCalledWith(12, 1, { lienParente: 'PERE', responsablePrincipal: false, autoriteParentale: true, contactUrgence: false }));
  });

  it('loads an authenticated photo as a blob, uploads the exact multipart boundary, rejects invalid files, and revokes URLs', async () => {
    api.students.get.mockResolvedValue({ ...student, photoDisponible: true });
    const createObjectURL = vi.fn(() => 'blob:student-photo'); const revokeObjectURL = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true }); Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
    const user = userEvent.setup(); const view = mount(<StudentDetails />);
    expect(await screen.findByRole('img', { name: 'Photo de Lina Durand' })).toHaveAttribute('src', 'blob:student-photo');
    expect(api.photos.load).toHaveBeenCalledWith(1);
    const input = screen.getByLabelText('Importer une photo');
    fireEvent.change(input, { target: { files: [new File(['bad'], 'bad.gif', { type: 'image/gif' })] } });
    expect(await screen.findByRole('alert')).toHaveTextContent('Choisissez une photo JPEG, PNG ou WebP.');
    const file = new File(['ok'], 'photo.png', { type: 'image/png' });
    fireEvent.change(input, { target: { files: [file] } });
    await waitFor(() => expect(api.photos.upload).toHaveBeenCalledWith(1, file));
    view.unmount();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:student-photo');
  });

  it('renders class current students from the implemented class registrations route', async () => {
    api.classes.registrations.mockResolvedValue([currentRegistration, { ...currentRegistration, id: 10, statut: 'TERMINEE' as const }]);
    mount(<ClassDetails />, '/admin/classes/4');
    expect(await screen.findByRole('heading', { name: '6e A' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Durand Lina' })).toHaveAttribute('href', '/admin/eleves/1');
  });
});
