import { AxiosError } from 'axios';
import { api } from '../auth/http';

export type Student = {
  id: number;
  numeroDossier: string;
  nom: string;
  prenom: string;
  dateNaissance: string;
  email: string | null;
  telephone: string | null;
  actif: boolean;
  photoDisponible: boolean;
};

export type StudentPayload = Omit<Student, 'id' | 'photoDisponible'>;

export type Classe = {
  id: number;
  code: string;
  nom: string;
  niveau: string;
  anneeScolaire: string;
  actif: boolean;
};

export type ClassePayload = Omit<Classe, 'id'>;

export type RegistrationStatus = 'EN_COURS' | 'TERMINEE' | 'ANNULEE';

export type Registration = {
  id: number;
  eleveId: number;
  eleveNomComplet: string;
  classeId: number;
  classeNom: string;
  anneeScolaire: string;
  dateInscription: string;
  dateFin: string | null;
  statut: RegistrationStatus;
};

export type Guardian = {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  utilisateurId: number | null;
  actif: boolean;
};

export type GuardianLink = {
  id: number;
  eleveId: number;
  eleveNomComplet: string;
  responsableId: number;
  responsableNomComplet: string;
  lienParente: 'PERE' | 'MERE' | 'TUTEUR' | 'AUTRE';
  responsablePrincipal: boolean;
  autoriteParentale: boolean;
  contactUrgence: boolean;
};

export type ApiErrorBody = {
  message?: string;
  detail?: string;
  fieldErrors?: Array<{ field: string; message: string }>;
};

export type Subject = {
  id: number;
  code: string;
  nom: string;
  coefficientDefaut: number;
  actif: boolean;
};

export type SubjectPayload = Omit<Subject, 'id'>;

export type Teacher = {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  utilisateurId: number | null;
  actif: boolean;
};

export type TeacherPayload = Omit<Teacher, 'id' | 'utilisateurId'>;

export type Teaching = {
  id: number;
  enseignantId: number;
  enseignantNomComplet: string;
  matiereId: number;
  matiereNom: string;
  classeId: number;
  classeNom: string;
  anneeScolaire: string;
  coefficientMatiere: number;
};

export type TeachingPayload = Omit<Teaching, 'id' | 'enseignantNomComplet' | 'matiereNom' | 'classeNom'>;

export type Account = {
  id: number;
  email: string;
  role: 'ADMIN' | 'ENSEIGNANT' | 'RESPONSABLE';
  actif: boolean;
  createdAt: string;
  lastLoginAt: string | null;
};

export type ProvisionedAccount = {
  id: number;
  email: string;
  role: Account['role'];
  status: 'EN_ATTENTE_ACTIVATION' | 'ACTIF' | 'DESACTIVE';
  profileId: number | null;
  mailDelivered: boolean;
};

export type BulletinStatus = 'BROUILLON' | 'PUBLIE' | 'REMPLACE';
export type BulletinPeriod = 'TRIMESTRE_1' | 'TRIMESTRE_2' | 'TRIMESTRE_3';

export type Bulletin = {
  id: number;
  inscriptionId: number;
  eleveNomComplet: string;
  classeNom: string;
  anneeScolaire: string;
  periode: BulletinPeriod;
  dateGeneration: string;
  statut: BulletinStatus;
  moyenneGenerale: number | null;
  appreciation: string | null;
  lignes: Array<{
    codeMatiere: string;
    nomMatiere: string;
    moyenne: number | null;
    coefficient: number;
    nombreNotes: number;
  }>;
};

export type BulletinPayload = {
  inscriptionId: number;
  periode: BulletinPeriod;
  appreciation: string | null;
};

export function frenchApiError(error: unknown, fallback: string) {
  const body = (error as AxiosError<ApiErrorBody>).response?.data;
  if (body?.fieldErrors?.length) {
    return 'Certaines informations sont invalides. Vérifiez les champs signalés.';
  }
  const message = body?.message ?? body?.detail;
  return message && /[àâçéèêëîïôûùüÿœa-z]/i.test(message) ? message : fallback;
}

export const students = {
  list: () => api.get<Student[]>('/eleves').then((response) => response.data),
  get: (id: number) => api.get<Student>(`/eleves/${id}`).then((response) => response.data),
  create: (payload: StudentPayload) => api.post<Student>('/eleves', payload).then((response) => response.data),
  update: (id: number, payload: StudentPayload) =>
    api.put<Student>(`/eleves/${id}`, payload).then((response) => response.data),
  registrations: (id: number) =>
    api.get<Registration[]>(`/eleves/${id}/inscriptions`).then((response) => response.data),
  guardianLinks: (id: number) =>
    api.get<GuardianLink[]>(`/eleves/${id}/responsables`).then((response) => response.data),
};

export const classes = {
  list: () => api.get<Classe[]>('/classes').then((response) => response.data),
  get: (id: number) => api.get<Classe>(`/classes/${id}`).then((response) => response.data),
  create: (payload: ClassePayload) => api.post<Classe>('/classes', payload).then((response) => response.data),
  update: (id: number, payload: ClassePayload) =>
    api.put<Classe>(`/classes/${id}`, payload).then((response) => response.data),
  registrations: (id: number) =>
    api.get<Registration[]>(`/classes/${id}/inscriptions`).then((response) => response.data),
};

export const registrations = {
  list: () => api.get<Registration[]>('/inscriptions').then((response) => response.data),
  create: (payload: {
    eleveId: number;
    classeId: number;
    anneeScolaire: string;
    dateInscription: string;
    dateFin: null;
    statut: 'EN_COURS';
  }) => api.post<Registration>('/inscriptions', payload).then((response) => response.data),
  transfer: (id: number, payload: { classeId: number; dateTransfert: string }) =>
    api.post<Registration>(`/inscriptions/${id}/transfer`, payload).then((response) => response.data),
  terminate: (id: number, dateFin: string) =>
    api.post<Registration>(`/inscriptions/${id}/terminate`, { dateFin }).then((response) => response.data),
  cancel: (id: number, dateFin: string) =>
    api.post<Registration>(`/inscriptions/${id}/cancel`, { dateFin }).then((response) => response.data),
};

export const subjects = {
  list: () => api.get<Subject[]>('/matieres').then((response) => response.data),
  get: (id: number) => api.get<Subject>(`/matieres/${id}`).then((response) => response.data),
  create: (payload: SubjectPayload) => api.post<Subject>('/matieres', payload).then((response) => response.data),
  update: (id: number, payload: SubjectPayload) =>
    api.put<Subject>(`/matieres/${id}`, payload).then((response) => response.data),
};

export const teachers = {
  list: () => api.get<Teacher[]>('/enseignants').then((response) => response.data),
  get: (id: number) => api.get<Teacher>(`/enseignants/${id}`).then((response) => response.data),
  create: (payload: TeacherPayload) => api.post<Teacher>('/enseignants', payload).then((response) => response.data),
  update: (id: number, payload: TeacherPayload) =>
    api.put<Teacher>(`/enseignants/${id}`, payload).then((response) => response.data),
  teachings: (id: number) => api.get<Teaching[]>(`/enseignants/${id}/enseignements`).then((response) => response.data),
};

export const teachings = {
  list: () => api.get<Teaching[]>('/enseignements').then((response) => response.data),
  get: (id: number) => api.get<Teaching>(`/enseignements/${id}`).then((response) => response.data),
  create: (payload: TeachingPayload) => api.post<Teaching>('/enseignements', payload).then((response) => response.data),
  update: (id: number, payload: TeachingPayload) =>
    api.put<Teaching>(`/enseignements/${id}`, payload).then((response) => response.data),
  remove: (id: number) => api.delete(`/enseignements/${id}`),
};

export const accounts = {
  list: () => api.get<Account[]>('/utilisateurs').then((response) => response.data),
  provisionTeacher: (payload: {
    enseignantId: number | null;
    email: string;
    matricule: string | null;
    nom: string | null;
    prenom: string | null;
  }) => api.post<ProvisionedAccount>('/admin/accounts/teachers', payload).then((response) => response.data),
  provisionGuardian: (payload: {
    responsableId: number | null;
    email: string;
    nom: string | null;
    prenom: string | null;
    telephone: string | null;
  }) => api.post<ProvisionedAccount>('/admin/accounts/guardians', payload).then((response) => response.data),
  resendActivation: (id: number) =>
    api.post<ProvisionedAccount>(`/admin/accounts/${id}/resend-activation`).then((response) => response.data),
  deactivate: (id: number) => api.post(`/admin/accounts/${id}/deactivate`),
};

export const bulletins = {
  list: () => api.get<Bulletin[]>('/bulletins').then((response) => response.data),
  generate: (payload: BulletinPayload) => api.post<Bulletin>('/bulletins/generate', payload).then((response) => response.data),
  publish: (id: number) => api.post<Bulletin>(`/bulletins/${id}/publier`).then((response) => response.data),
  correct: (id: number, payload: BulletinPayload) =>
    api.post<Bulletin>(`/bulletins/${id}/corriger`, payload).then((response) => response.data),
  pdf: (id: number) => api.get<Blob>(`/bulletins/${id}/pdf`, { responseType: 'blob' }).then((response) => response.data),
};

export const guardians = {
  list: () => api.get<Guardian[]>('/responsables').then((response) => response.data),
  create: (payload: Omit<Guardian, 'id' | 'utilisateurId'>) =>
    api.post<Guardian>('/responsables', payload).then((response) => response.data),
  link: (
    guardianId: number,
    studentId: number,
    payload: Pick<GuardianLink, 'lienParente' | 'responsablePrincipal' | 'autoriteParentale' | 'contactUrgence'>,
  ) =>
    api
      .post<GuardianLink>(`/responsables/${guardianId}/eleves/${studentId}`, payload)
      .then((response) => response.data),
  end: (guardianId: number, studentId: number, dateFin: string) =>
    api
      .post<GuardianLink>(`/responsables/${guardianId}/eleves/${studentId}/end`, { dateFin })
      .then((response) => response.data),
  setPrincipal: (guardianId: number, studentId: number) =>
    api
      .post<GuardianLink>(`/responsables/${guardianId}/eleves/${studentId}/principal`)
      .then((response) => response.data),
};

export const photos = {
  load: (studentId: number) =>
    api.get<Blob>(`/eleves/${studentId}/photo`, { responseType: 'blob' }).then((response) => response.data),
  upload: (studentId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/eleves/${studentId}/photo`, formData).then((response) => response.data);
  },
};
