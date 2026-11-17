# Zeroed API — Initial Endpoints

## Conventions

- Base path: `/api`.
- Request and response bodies use JSON except where Spring Security's configured login mechanism requires otherwise.
- Authentication uses a Spring Security server-side session. After login, the backend sets the session cookie; the React client sends it with subsequent requests.
- All group endpoints require an authenticated user. A user may access only groups they belong to, except where an explicitly agreed admin rule applies.
- Never return passwords, password hashes, session IDs, or other secrets in JSON.
- Suggested errors: `400 Bad Request` for invalid input, `401 Unauthorized` when not signed in, `403 Forbidden` when signed in but not permitted, `404 Not Found` when the requested resource does not exist or should not be disclosed, and `409 Conflict` for duplicates.
- Money amounts are exact decimal strings, never JSON numbers, and their precision is currency-specific: an amount denominated in a currency whose ISO 4217 minor-unit exponent is E carries exactly E fractional digits, and carries no decimal point at all when E is 0. Examples: "100.00" (CAD, E = 2), "0.05" (EUR), "2500" (JPY, E = 0), "1.250" (KWD, E = 3). The integer part always has 1–9 digits. The server parses each amount into an integer count of that currency's minor units (cents, yen, fils, …), stores the integer alongside the ISO code, and performs all arithmetic on integers: binary floating point never touches a money value, 0.1 + 0.2 style drift is impossible, and no amount is silently rounded.
- Every money figure carries an explicit ISO 4217 code, and currencies are never mixed inside one figure.

## Authentication

### `POST /api/auth/register`

Create a user account.

Example request:

```json
{
  "email": "alex@example.com",
  "password": "user-supplied-password"
}
```

Expected behavior:

- Validate and normalize email consistently (for example, trim whitespace and use a consistent case policy).
- Hash the password with a Spring Security `PasswordEncoder`; never persist the raw password.
- Reject an email already in use.

Suggested responses:

- `201 Created` with a safe user representation, e.g. `{ "id": 42, "email": "alex@example.com" }`.
- `400 Bad Request` for invalid email/password input.
- `409 Conflict` if the email is already registered.

### `POST /api/auth/login`

Authenticate the user and establish a server-side session. On success, Spring Security sets the session cookie; do not return the session ID in the JSON body.

Example JSON request:

```json
{
  "email": "alex@example.com",
  "password": "user-supplied-password"
}
```

Suggested responses:

- `200 OK` with a safe user representation.
- `401 Unauthorized` for invalid credentials; avoid revealing whether the email exists.

**Implementation note:** Spring Security's built-in form-login flow is not automatically a JSON API at this path. If the frontend sends JSON to `/api/auth/login`, configure a JSON-compatible authentication flow and ensure the authenticated security context is saved in the session.

### `POST /api/auth/logout`

Invalidate the current session and clear/invalidate its cookie.

Suggested responses:

- `204 No Content` when logout succeeds.
- `401 Unauthorized` if the API requires an active session for logout; alternatively, make logout idempotent and return `204` even when already logged out. Choose one behavior and document it.

## Groups

### `GET /api/groups`

List groups the current user belongs to. If the user has the ADMIN role they can view all groups.

Suggested response: `200 OK` with an array. An empty list is `[]`.

```json
[
  { "id": 7, "name": "Vancouver Trip", "createdAt": "2026-10-28T18:30:00Z" }
]
```

The backend derives the user from the authenticated session; the client must not choose a different user's identity in the request.

### `POST /api/groups`

Create a group. The authenticated user should be added as a member automatically so the creator can immediately access it.

Example request:

```json
{
  "name": "Vancouver Trip",
}
```

Suggested response: `201 Created` with the new group, e.g. `{ "id": 7, "name": "Vancouver Trip", "createdAt": "2026-10-28T18:30:00Z" }`.

### `GET /api/groups/{groupId}`

Get details for one group. Require the caller to be a member of that group (or have a separately defined admin permission).

Suggested response: `200 OK` with group details. Return `404 Not Found` when the group does not exist or is not visible to the caller, to avoid leaking group existence.

## Group members

### `GET /api/groups/{groupId}/members`

List members of a group the caller is allowed to access.

The caller must be a member or have the ADMIN role. Admins may view members of any existing group. Return `401 Unauthorized` when not signed in and `404 Not Found` when the group does not exist or is not visible to the caller.

Suggested response: `200 OK` with safe profile fields only, such as:

```json
[
  { "id": 42, "email": "alex@example.com" },
  { "id": 57, "email": "sam@example.com" }
]
```

Do not include password hashes or authentication/session data.

### `POST /api/groups/{groupId}/members`

Add an already-registered user to the group.

The caller must already be a member, including admins. The server derives the caller from the session and normalizes the target email by trimming whitespace and converting it to lowercase.

Proposed request by email (a user ID could be used instead if the UI first selects from a member directory):

```json
{
  "email": "sam@example.com"
}
```

Suggested behavior and responses:

- `201 Created` with `{ "id": 57, "email": "sam@example.com" }` when membership is added.
- `401 Unauthorized` when not signed in.
- `400 Bad Request` for malformed input.
- `404 Not Found` if the group or target account is not found/visible.
- `409 Conflict` if the person is already a member.

## Expenses

An expense records money that one group member paid on behalf of selected members of the same group. It carries two distinct roles: the member who recorded it (createdBy, always derived from the authenticated session) and the member who actually paid (paidBy, chosen by the creator from the group's current members). The same person often fills both roles, but the API never assumes it.

Each expense also carries its own ISO 4217 currency, supplied explicitly when the expense is created or edited.

### Expense Representation/Object

```json
{
  "id": 301,
  "groupId": 7,
  "description": "Dinner",
  "amount": "100.00",
  "currency": "CAD",
  "paidBy": { "id": 57, "email": "sam@example.com" },
  "createdBy": { "id": 42, "email": "alex@example.com" },
  "splitType": "equal",
  "splits": [
    { "userId": 42, "amount": "25.00" },
    { "userId": 57, "amount": "25.00" },
    { "userId": 61, "amount": "25.00" },
    { "userId": 88, "amount": "25.00" }
  ],
  "date": "2026-11-14",
  "createdAt": "2026-11-14T18:30:00Z",
  "updatedAt": "2026-11-14T18:30:00Z"
}
```

### `POST /api/groups/{groupId}/expenses`

NOTE: Custom splitting is currently not supported. It will come in a later iteration.

```json
{
  "description": "Museum tickets",
  "amount": "60.00",
  "currency": "EUR",
  "date": "2026-11-13",
  "paidByUserId": 61,
  "participantIds": [42, 61, 88]
}
```

| Field            | Required | Rules                                                                                                                                                                                |
| ---------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `description`    | yes      | Trimmed; 1–200 characters after trimming; not blank.                                                                                                                                 |
| `amount`         | yes      | Exact decimal string with 1–9 integer digits and exactly the currency's ISO 4217 minor-unit exponent in fractional digits (no decimal point for exponent 0); strictly positive. |
| `currency`       | yes      | Uppercase ISO 4217 code recognized by Java's `Currency` with a minor-unit exponent from 0 through 4. Codes without a defined minor unit, including `XXX` and `XTS`, are unsupported. |
| `date`           | no       | Valid `YYYY-MM-DD`; defaults to the current UTC date. Future dates are allowed (pre-logging a planned expense).                                                                      |
| `paidByUserId`   | no       | Integer user ID; must be a current member of the group. Defaults to the authenticated creator when omitted. Independent of `participantIds`: the payer need not appear in the split. |
| `participantIds` | no       | Non-empty array of unique integers; each must be a current member of the group; at most the current member count. When omitted, all current group members participate. Explicit `null` and empty arrays are invalid. The payer may appear or not appear. |

Equal splits use integer minor units. Participants are sorted by ascending user ID; each receives `amountMinor / participantCount`, and the first `amountMinor % participantCount` participants receive one additional minor unit. Split amounts always sum exactly to the expense amount; zero shares are allowed. Responses list splits in this same order.

Only omitted optional fields receive defaults. Explicit `null` values are invalid. Money must be a JSON string; IDs must be JSON integers, not strings or fractional numbers. Expense creation requires group membership even for admins.

Suggested response: `201 Created` with the expense representation, and a `Location: /api/expenses/{id}` header.

Validation failures return `400 Bad Request`:

```json
{
  "error": "validation_failed",
  "message": "Request body is invalid.",
  "fields": {
    "amount": "must have 1–9 integer digits and exactly 2 fractional digits",
    "paidByUserId": "must reference a current member of the group",
    "participantIds": "must contain only current group members"
  }
}
```

Error behavior:

- `400 Bad Request` — malformed JSON, unknown/immutable field (`unsupported_field`), or any field-rule violation (`validation_failed`). Participant IDs that do not exist or are not members of this group, and a `paidByUserId` that is not a current member, are request errors, not `404`.
- `401 Unauthorized` — not signed in.
- `404 Not Found` — group does not exist or the caller is not a member. Group existence is not disclosed to non-members.
- `500 Internal Server Error` — persistence failure.
 
### `GET /api/groups/{groupId}/expenses`

List the expenses of a group the caller is a member of, newest first (by `date`, then creation time).

Suggested response: `200 OK` with an array of the expense representation above. A group with no expenses returns `[]`.

Errors:
`401 Unauthorized`: not signed in.
`404 Not Found`: group does not exist or the caller is not a member. Group existence is not disclosed to non-members.

### `GET /api/expenses/{expenseId}`

Return one expense for a detail view or for refreshing after an edit. Any current group member may read it; the ADMIN role does not bypass membership.

Response: `200 OK` with the expense representation.

Errors: `401` when not signed in; `404` when the expense does not exist, is deleted, or belongs to a group the caller is not a member of. A non-member always receives `404`, never `403`, so the endpoint does not disclose whether the expense exists.

### `PATCH /api/expenses/{expenseId}`

Edit an existing expense. **Only the creator (`createdBy`) may edit**; any other group member receives `403 Forbidden`.

Editable fields: `description`, `amount`, `date`, `paidByUserId`, `participantIds`, `currency`. The body must contain at least one of them. Non-object or empty bodies, explicit `null` values, unknown or immutable fields, and invalid values are rejected with `400` using the same structured error representation as expense creation. Unknown or immutable fields use `unsupported_field`; other validation errors use `validation_failed`. The payer is changed by sending `paidByUserId`.

Request:

```json
{
  "description": "Fancy dinner",
  "amount": "120.00",
  "paidByUserId": 61,
  "participantIds": [42, 57, 61],
  "currency": "CAD"
}
```

Semantics:

- Provided fields replace the stored values; omitted fields keep their current values.
- `participantIds`, when present, replaces the entire participant set (not a merge) and must contain only current group members. When omitted, stored participants remain unchanged, including former members.
- `paidByUserId`, when present, replaces the payer and is validated the same way as on creation: it must reference a current member of the group, including the creator's own ID when they paid. When it is omitted, the stored `paidBy` is kept as-is and is not revalidated against current membership, so an expense whose payer later left the group still credits that user in balances.
- Supplied `amount` is validated against the effective currency (supplied `currency`, or the stored currency when omitted). A currency-only change retains the numeric amount and rescales minor units exactly: CAD `"10.00"` becomes JPY `"10"`, but CAD `"10.01"` cannot become JPY without a replacement amount. Inexact conversion returns `400` on `amount`; no rounding or exchange-rate conversion occurs.
- Changes to amount, currency, or participants recalculate equal shares with the same ascending-user-ID remainder rule as creation. Parent fields and split rows are persisted in one transaction; a failure rolls back both.
- `id`, `groupId`, `createdBy`, and `createdAt` never change. Every successful patch advances `updatedAt`; expense timestamps use database-compatible microsecond precision.
- Concurrent edits and deletions serialize on the expense row. Non-members receive `404` before creator checks; another member, including an admin or the payer, receives `403`.

Response: `200 OK` with the updated expense representation.

Error behavior:

- `400 Bad Request` — empty body, unknown/immutable field, or field-rule violation, including a `paidByUserId` or `participantIds` entry that is not a current group member.
- `401 Unauthorized` — not signed in.
- `403 Forbidden` — caller is a member of the expense's group but is not its creator.
- `404 Not Found` — expense does not exist, is already deleted, or the caller is not a member of its group.
- `500 Internal Server Error` — persistence failure.
 
### `DELETE /api/expenses/{expenseId}`

Delete an expense. **Only the creator (`createdBy`) may delete**; any other group member receives `403 Forbidden`.

Response: `204 No Content` with an empty body.

Semantics:

- The expense is removed from all history responses and from balance calculations immediately.
- Deletion is terminal in this version: there is no restore endpoint, and a second `DELETE` on the same ID returns `404` (it is not idempotent).
- Editing after deletion returns `404`.
- The client asks for confirmation before sending `DELETE`. The backend deletes immediately after authorization; no confirmation flag or additional endpoint is required.
- The expense and all its split rows are removed in one transaction. Other expenses and their contributions remain unchanged. Non-members, including admins, receive `404`; another current member receives `403`.

Error behavior:

- `401 Unauthorized` — not signed in.
- `403 Forbidden` — caller is a group member but not the creator.
- `404 Not Found` — expense does not exist, is already deleted, or the caller is not a member of its group.
- `500 Internal Server Error` — persistence failure.

## Balances

Balances are derived on demand from a group's non-deleted expenses. They are not stored, so an expense edit or deletion is reflected in the next balance read. Payments between members are outside the scope of iteration 2; balances here account for expenses only.

The balance endpoint below is not implemented yet. The expense mutation endpoints update the authoritative expense and split rows atomically; future balance reads must derive their values from those surviving rows.

### Balance semantics

All arithmetic is performed in integer cents of the group currency, so results are exact and the identity `sum(net) == 0` holds by construction.

For each non-deleted expense in the group:

- The payer (`paidBy`) is credited the full `amount`.
- Each entry in `splits` debits that participant by its `amount`. A payer who is also a participant therefore nets the rest of the expense, and a payer who is not a participant nets the full amount.

Per member:

- `paid` = sum of `amount` over expenses where the member is the payer.
- `owed` = sum of split amounts over expenses where the member is a participant.
- `net` = `paid - owed`. Positive means the group owes this member money; negative means the member owes the group. Zero means settled.

Suggested transfers are produced deterministically:

1. Calculate net balances for all members involved in any expense, whether as a payer or participant. Include former group members, as their outstanding balances remain valid.

2. Sort members by balance:

- Creditors (net > 0): Descending by balance, then ascending by userId.
- Debtors (net < 0): Descending by absolute balance, then ascending by userId.

3. Settle balances greedily: Match the largest debtor with the largest creditor and create a transfer for min(debt, credit). Update both balances, remove any that reach zero, and repeat until all balances are settled.

### `GET /api/groups/{groupId}/balances`

Return group-wide and per-member balances plus suggested settlement transfers.

Suggested response: `200 OK`

```json
{
  "groupId": 7,
  "currency": "USD",
  "members": [
    { "userId": 42, "email": "alex@example.com", "paid": "0.00", "owed": "25.00", "net": "-25.00" },
    { "userId": 57, "email": "sam@example.com", "paid": "100.00", "owed": "25.00", "net": "75.00" },
    { "userId": 61, "email": "jo@example.com", "paid": "0.00", "owed": "25.00", "net": "-25.00" },
    { "userId": 88, "email": "lee@example.com", "paid": "0.00", "owed": "25.00", "net": "-25.00" }
  ],
  "transfers": [
    { "from": { "id": 42, "email": "alex@example.com" }, "to": { "id": 57, "email": "sam@example.com" }, "amount": "25.00" },
    { "from": { "id": 61, "email": "jo@example.com" }, "to": { "id": 57, "email": "sam@example.com" }, "amount": "25.00" },
    { "from": { "id": 88, "email": "lee@example.com" }, "to": { "id": 57, "email": "sam@example.com" }, "amount": "25.00" }
  ],
  "generatedAt": "2026-11-15T09:00:00Z"
}
```

The numbers above follow the example expense used throughout this document: Alex (42) recorded it, Sam (57) paid it, and it was split equally across four participants. Sam is therefore the creditor because he is the payer, even though Alex created the record; editing `paidByUserId` to another current member would move that credit.

Semantics:

- `members` lists every current group member, plus any user who appears as a payer or split participant in a surviving expense, sorted by `userId` ascending. Current members with no activity appear with `"0.00"` in all three amount fields.
- `paid`, `owed`, and `net` use the sign convention above; `net` is the only field that can be negative.
- `transfers` is `[]` when everyone's `net` is `"0.00"`; a group with no expenses returns all members at `"0.00"` and no transfers.
- Transfer `from`/`to` are user references containing `id` and `email` only; no other profile data is exposed.
- The endpoint is not paginated: a group has at most one row per member, and the response size is bounded by membership.
- Balances are scoped to a single group and a single currency; there is no cross-group netting endpoint, and the ADMIN role gets no additional access.

Error behavior:

- `401 Unauthorized` — not signed in.
- `404 Not Found` — group does not exist or the caller is not a member (existence is not disclosed).
- `500 Internal Server Error` — persistence failure.

## Authorization summary

- Unauthenticated requests to protected routes: `401 Unauthorized`.
- Authenticated users can list only their own groups.
- Group detail/member routes require membership (or an explicitly defined elevated role).
- The server derives the acting user from Spring Security, not from a caller-supplied `userId` field.
