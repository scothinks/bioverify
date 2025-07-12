// This interface defines the structure of a Tenant object in our frontend.
// It should match the structure of the Tenant entity in our Java backend.

export interface Tenant {
  id: string; // UUIDs are represented as strings in JSON
  name: string;
  subdomain: string;
  stateCode: string;
  optimaConfig: string;
  active: boolean;
  createdAt: string; // Timestamps are typically handled as strings or Date objects
  updatedAt: string;
}
