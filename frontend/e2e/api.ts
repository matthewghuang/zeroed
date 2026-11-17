import { randomUUID } from 'node:crypto';
import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';

export async function postWithCsrf(api: APIRequestContext, path: string, data: unknown) {
  const csrf = await api.get('/api/auth/csrf');
  expect(csrf.status()).toBe(200);
  const cookie = (await api.storageState()).cookies.find(cookie => cookie.name === 'XSRF-TOKEN');
  expect(cookie).toBeDefined();
  return api.post(path, {
    headers: { 'X-XSRF-TOKEN': decodeURIComponent(cookie!.value) },
    data,
  });
}

export async function createUser(api: APIRequestContext) {
  const email = `e2e-${randomUUID()}@example.com`;
  const password = `E2E-${randomUUID()}!`;
  const registration = await postWithCsrf(api, '/api/auth/register', { email, password });
  expect(registration.status()).toBe(201);
  const login = await postWithCsrf(api, '/api/auth/login', { email, password });
  expect(login.status()).toBe(204);
  return { email };
}
