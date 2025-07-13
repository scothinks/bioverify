export interface Tenant {
  id: string;
  name: string;
  subdomain: string;
  stateCode: string;
  description?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
  createdBy?: string;
  updatedBy?: string;
  identitySourceConfig?: string;
}

export interface CreateTenantRequest {
  name: string;
  subdomain: string;
  stateCode: string;
  description?: string;
  identitySourceConfig?: string;
}

export interface UpdateTenantRequest {
  name?: string;
  stateCode?: string;
  description?: string;
  isActive?: boolean;
  identitySourceConfig?: string;
}
