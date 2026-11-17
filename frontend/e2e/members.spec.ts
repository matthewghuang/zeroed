import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import { createUser, postWithCsrf } from './api';

test('adds a registered member who can then access the group in a separate session', async ({ page, browser, baseURL }) => {
  const owner = await createUser(page.request);
  const name = `Shared group ${randomUUID()}`;
  const creation = await postWithCsrf(page.request, '/api/groups', { name });
  expect(creation.status()).toBe(201);
  const group: { id: number } = await creation.json();

  const invitedContext = await browser.newContext({ baseURL });
  try {
    const invited = await createUser(invitedContext.request);
    const invitedPage = await invitedContext.newPage();
    await invitedPage.goto('/groups');
    await expect(invitedPage.getByText('No groups yet', { exact: true })).toBeVisible();
    expect((await invitedContext.request.get(`/api/groups/${group.id}/members`)).status()).toBe(404);

    await page.goto('/groups');
    await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
    await page.getByPlaceholder('email', { exact: true }).fill(invited.email);
    await page.getByRole('button', { name: 'Add Member', exact: true }).click();
    await expect(page.getByText('Group Member Added Successfully!', { exact: true })).toBeVisible();
    await expect(page.getByRole('listitem').filter({ hasText: invited.email })).toBeVisible();

    await page.reload();
    await expect(page.getByRole('listitem').filter({ hasText: invited.email })).toBeVisible();
    await invitedPage.reload();
    await expect(invitedPage.getByRole('button', { name, exact: true })).toBeVisible();
    await expect(invitedPage.getByRole('heading', { name, exact: true })).toBeVisible();
    await expect(invitedPage.getByRole('listitem').filter({ hasText: owner.email })).toBeVisible();
    await expect(invitedPage.getByRole('listitem').filter({ hasText: invited.email })).toBeVisible();

    const members = await invitedContext.request.get(`/api/groups/${group.id}/members`);
    expect(members.status()).toBe(200);
    expect(await members.json()).toEqual(expect.arrayContaining([
      expect.objectContaining({ email: owner.email }),
      expect.objectContaining({ email: invited.email }),
    ]));
  } finally {
    await invitedContext.close();
  }
});
