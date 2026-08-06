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
