export interface User {
  id: string;
  fullName: string;
  email: string;
  role: string;
  password?: string;
  assignedMinistryIds?: string[];
  assignedDepartmentIds?: string[];
}