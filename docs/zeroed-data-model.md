# Zeroed Data Model

## Purpose

This document explains the first-pass relational data model for Zeroed: user accounts, expense-sharing groups, and the relationship between users and groups. It also clarifies how login sessions fit into the design when authentication is managed by Spring Security.

## Overview

The application data model has five core tables:

- `users` stores account identity and the password hash.
- `groups` stores each expense-sharing group.
- `group_memberships` connects users to groups and records when each membership began.
- `expenses` stores details about expenses
- `expense_splits` stores relational information between users and expenses

A user can belong to many groups, and a group can contain many users. This is a **many-to-many relationship**, represented by the `group_memberships` join table.

```text
USERS (1) -> (0..*) GROUP_MEMBERSHIPS (0..*) -> (1) GROUPS
```

Each membership row points to exactly one user and one group. A `(user_id, group_id)` composite primary key prevents duplicate memberships for the same user and group.

## Tables and fields

### `users`

- `id` — primary key; stable internal identifier for the account.
- `email` — unique login identifier. Enforce uniqueness in the database, not only in application code.
- `password_hash` — output from a password-hashing algorithm. Spring Security has **PasswordEncoder**
- `role` - Enum specifying REGULAR or ADMIN
- `created_at` — when the account was created.

### `groups`

- `id` — primary key for the group.
- `name` — user-visible group name.
- `base_currency` — immutable uppercase ISO 4217 code with exponent `0..4`; required at group creation. No currency-change API or model setter exists, including before expenses.
- `created_at` — when the group was created; useful for sorting and auditing, but optional.
- `created_by_user_id` - user who created the group

### `group_memberships`

- `user_id` — foreign key to `users.id` and part of the composite primary key.
- `group_id` — foreign key to `groups.id` and part of the composite primary key.
- `joined_at` — when this particular user joined this particular group. It belongs to the relationship, not to the user or group, because each membership has its own date.

### `expenses`

- `id` — primary key; the value in `/api/expenses/{expenseId}`.
- `group_id` — the owning group; every read is scoped through it.
- `description` — trimmed, 1–200 characters, not blank.
- `amount_minor` — total amount in minor units of `currency`; strictly greater than zero.
- `currency` — ISO 4217 alpha-3, uppercase, validated against the application's supported-currency set.
- `currency_exponent` — ISO 4217 exponent for `currency` at the time of writing; `0..4`. The writer must assert `currency_exponent == ISO4217[currency]`, so a bad pair cannot be persisted through the application.
- `converted_amount_minor` — required rounded total in the group's base currency; nonnegative (tiny foreign amounts can round to zero).
- `exchange_rate` — required `numeric(38,18)`, base-currency major units per one original-currency major unit; locked at recording time, `1` for matching currencies.
- `rate_date` — required provider observation date; can precede recording date on weekends/holidays. Same-currency records use the recording UTC date.
- `paid_by_user_id` — the member credited with paying. Must be a current member of `group_id` **at creation time** and whenever `paidByUserId` is supplied on edit; once stored it is never re-validated, so an expense whose payer later left the group keeps crediting them.
- `created_by_user_id` — the member who recorded it, derived from the session. Sole owner of edit and delete rights.
- `split_type` — `'EQUAL'`. Custom splitting is explicitly out of scope for this iteration. The column exists so existing rows stay interpretable when a second split type is added.
- `expense_date` — calendar date of the expense; defaults to the current UTC date; future dates are allowed.
- `created_at` / `updated_at` — row timestamps stored and returned at microsecond precision; `created_at` is immutable and `updated_at` advances on every successful `PATCH`.

### `expense_splits`

One row per participant of one expense — the debit side of the balance.

- `expense_id` — foreign key to `expenses.id`, part of the composite primary key, `ON DELETE CASCADE` (deleting an expense deletes its split rows in the same statement).
- `user_id` — foreign key to `users.id`, part of the composite primary key, `ON DELETE RESTRICT`.
- `amount_minor` — this participant's share in the group's **base currency** and its ISO exponent; `>= 0`. Shares sum exactly to `expenses.converted_amount_minor`, including when conversion rounds the total to zero.
- **No currency columns** — the base currency is inherited through the parent expense's group, not the expense's original currency.

Creation converts the original total once using decimal `HALF_UP` rounding to base minor units, then persists the expense, locked rate/date, converted total and all splits in one transaction. Equal splitting assigns remainder base minor units to participants in ascending user-ID order. Balance reads credit `converted_amount_minor` and debit stored splits; they never fetch or recalculate rates.

Editing is restricted to the creator while they remain a group member. It updates the expense and recalculates equal splits in one transaction. Overlapping participants keep their composite-key rows; removed participants' split rows are deleted, and new participants receive new rows. Omitted historical payers and participants are retained even if they have left the group; explicitly supplied user references must be current members.

Amount-only edits reuse the locked rate and date; changing the original currency obtains a new latest reference rate. Description, date, payer, and participant edits do not refresh rates. Provider failures roll back the entire mutation.

Deletion is creator-only and permanent: the expense and its splits are removed atomically, without modifying other expenses. Concurrent edits/deletions lock the expense row. Balances are not stored; the surviving expense credits and split debits are their authoritative inputs.

### Explanation of ISO 4217

ISO 4217 is an international standard that defines currency codes and how many decimal places each currency uses.

Each currency has:
- Code: A three-letter identifier, like CAD, USD, or JPY.
- Exponent: The number of decimal places used to represent the currency's smallest standard unit.

Think of the exponent as how many digits go after the decimal point.
- Exponent 0 -> No decimal places (100)
- Exponent 2 -> Two decimal places (100.00)
- Exponent 3 -> Three decimal places (100.000)

## Login sessions and Spring Security

Spring Security handles authentication and the HTTP session; after successful login, the backend issues a session cookie, and requests carrying that cookie are associated with the server-side session.
