import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

test('rejects invalid credentials, persists a valid login, and invalidates logout', async ({ page }) => {
  const email = `login-${randomUUID()}@example.com`;
  const password = 'Login-e2e-Password!';

  // Seed through the real API so this test does not depend on the registration UI.
  const csrf = await page.request.get('/api/auth/csrf');
  expect(csrf.status()).toBe(200);
  const csrfCookie = (await page.context().cookies()).find(cookie => cookie.name === 'XSRF-TOKEN');
  expect(csrfCookie).toBeDefined();
  const registration = await page.request.post('/api/auth/register', {
    headers: { 'X-XSRF-TOKEN': decodeURIComponent(csrfCookie!.value) },
    data: { email, password },
  });
  expect(registration.status()).toBe(201);

  await page.goto('/login');
  await page.getByLabel('Email:', { exact: true }).fill(email);
  await page.getByLabel('Password:', { exact: true }).fill('Incorrect-password!');
  await page.getByRole('button', { name: 'Log In', exact: true }).click();
  await expect(page.getByText('Login failed. Check your email and password.', { exact: true })).toBeVisible();
  await expect(page).toHaveURL('/login');
  expect((await page.request.get('/api/auth/me')).status()).toBe(401);

  await page.getByLabel('Password:', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Log In', exact: true }).click();
  await expect(page).toHaveURL('/groups');
  await expect(page.getByText('No groups yet', { exact: true })).toBeVisible();

  await page.reload();
  await expect(page.getByRole('button', { name: 'Logout', exact: true })).toBeVisible();
  const identity = await page.request.get('/api/auth/me');
  expect(identity.status()).toBe(200);
  expect(await identity.text()).toBe(email);

  await page.getByRole('button', { name: 'Logout', exact: true }).click();
  await expect(page).toHaveURL('/login');
  await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();
  expect((await page.request.get('/api/auth/me')).status()).toBe(401);
});
