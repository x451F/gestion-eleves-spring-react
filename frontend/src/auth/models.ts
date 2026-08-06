export type Role = 'ADMIN' | 'ENSEIGNANT' | 'RESPONSABLE';

export type User = {
  id: number;
  email: string;
  role: Role;
  status: 'EN_ATTENTE_ACTIVATION' | 'ACTIF' | 'DESACTIVE';
};
