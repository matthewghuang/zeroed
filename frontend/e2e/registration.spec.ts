import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

test('registers a new account whose credentials can be used to log in', async ({ page }) => {
  const email = `registration-${randomUUID()}@example.com`;
  const password = 'Registration-e2e-Password!';

  await page.goto('/login');
  await page.getByRole('button', { name: 'Sign up', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Register', exact: true })).toBeVisible();
  await page.getByLabel('Username:', { exact: true }).fill('E2E registration');
  await page.getByLabel('Email:', { exact: true }).fill(email);
  await page.getByLabel('Password:', { exact: true }).fill(password);

  const registration = page.waitForResponse(response =>
    new URL(response.url()).pathname === '/api/auth/register' && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Sign Up', exact: true }).click();
  expect((await registration).status()).toBe(201);
  await expect(page.getByText('Registration Successful!', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Login', exact: true })).toBeVisible();

  await page.getByLabel('Email:', { exact: true }).fill(email);
  await page.getByLabel('Password:', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Log In', exact: true }).click();
  await expect(page).toHaveURL('/groups');

  const identity = await page.request.get('/api/auth/me');
  expect(identity.status()).toBe(200);
  expect(await identity.text()).toBe(email);
});
