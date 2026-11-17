import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import { createUser, postWithCsrf } from './api';

test('adds an expense with a different payer and persists its details and equal splits', async ({ page, request }) => {
  const owner = await createUser(page.request);
  const payer = await createUser(request);
  const name = `Expense group ${randomUUID()}`;
  const creation = await postWithCsrf(page.request, '/api/groups', { name });
  expect(creation.status()).toBe(201);
  const group: { id: number } = await creation.json();
  const addition = await postWithCsrf(page.request, `/api/groups/${group.id}/members`, { email: payer.email });
  expect(addition.status()).toBe(201);
  const paidBy: { id: number; email: string } = await addition.json();
  const memberResponse = await page.request.get(`/api/groups/${group.id}/members`);
  expect(memberResponse.status()).toBe(200);
  const members: { id: number; email: string }[] = await memberResponse.json();
  const createdBy = members.find(member => member.email === owner.email);
  expect(createdBy).toBeDefined();

  await page.goto('/groups');
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: payer.email })).toBeVisible();
  await page.getByRole('button', { name: 'Add Expense', exact: true }).click();
  await page.getByLabel('Description:', { exact: true }).fill('Museum tickets');
  await page.getByLabel('amount:', { exact: true }).fill('42.50');
  await page.getByLabel('currency:', { exact: true }).selectOption('EUR');
  await page.getByLabel('date:', { exact: true }).fill('2026-11-13');
  await page.getByLabel('Paid By:', { exact: true }).selectOption({ label: payer.email });

  const submission = page.waitForResponse(response =>
    new URL(response.url()).pathname === `/api/groups/${group.id}/expenses` && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Create Expense', exact: true }).click();
  const response = await submission;
  expect(response.status()).toBe(201);
  const expense: { id: number } = await response.json();
  await expect(page.getByText('Expense added successfully', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();

  // There is no expense list in the UI yet; read persisted data as the other member.
  const expenses = await request.get(`/api/groups/${group.id}/expenses`);
  expect(expenses.status()).toBe(200);
  expect(await expenses.json()).toEqual([expect.objectContaining({
    id: expense.id,
    groupId: group.id,
    description: 'Museum tickets',
    amount: '42.50',
    currency: 'EUR',
    date: '2026-11-13',
    paidBy,
    createdBy,
    splitType: 'equal',
    splits: [
      { userId: createdBy!.id, amount: '21.25' },
      { userId: paidBy.id, amount: '21.25' },
    ].sort((left, right) => left.userId - right.userId),
  })]);
});
