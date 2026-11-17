import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import { createUser } from './api';

test('creates a group with the creator as a member and persists it after reload', async ({ page }) => {
  const owner = await createUser(page.request);
  const name = `Group ${randomUUID()}`;

  await page.goto('/groups');
  await expect(page.getByText('No groups yet', { exact: true })).toBeVisible();
  await page.getByPlaceholder('group name...', { exact: true }).fill(name);
  const creation = page.waitForResponse(response =>
    new URL(response.url()).pathname === '/api/groups' && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Create Group', exact: true }).click();
  const response = await creation;
  expect(response.status()).toBe(201);
  const group: { id: number } = await response.json();

  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: owner.email })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('button', { name, exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: owner.email })).toBeVisible();

  const groups = await page.request.get('/api/groups');
  expect(groups.status()).toBe(200);
  expect(await groups.json()).toEqual([expect.objectContaining({ id: group.id, name })]);
  const members = await page.request.get(`/api/groups/${group.id}/members`);
  expect(members.status()).toBe(200);
  expect(await members.json()).toEqual([expect.objectContaining({ email: owner.email })]);
});
