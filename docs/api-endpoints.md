# Zeroed API — Initial Endpoints

## Conventions

- Base path: `/api`.
- Request and response bodies use JSON except where Spring Security's configured login mechanism requires otherwise.
- Authentication uses a Spring Security server-side session. After login, the backend sets the session cookie; the React client sends it with subsequent requests.
- All group endpoints require an authenticated user. A user may access only groups they belong to, except where an explicitly agreed admin rule applies.
- Never return passwords, password hashes, session IDs, or other secrets in JSON.
- Suggested errors: `400 Bad Request` for invalid input, `401 Unauthorized` when not signed in, `403 Forbidden` when signed in but not permitted, `404 Not Found` when the requested resource does not exist or should not be disclosed, and `409 Conflict` for duplicates.
- Money amounts are exact decimal strings, never JSON numbers. Requests accept 1–9 integer digits and up to the currency's ISO 4217 exponent in fractional digits (no decimal point for exponent 0): `"50"`, `"50.0"` and `"50.00"` all represent 50 BWP. Responses always format amounts at the full currency precision. Converted totals may be larger, must fit signed 64-bit minor units, and explicitly use decimal `HALF_UP` rounding to base precision. Balances aggregate integer minor units without an individual-expense limit. Binary floating point never touches a monetary value.
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
  { "id": 7, "name": "Vancouver Trip", "createdAt": "2026-10-28T18:30:00Z", "baseCurrency": "CAD" }
]
```

The backend derives the user from the authenticated session; the client must not choose a different user's identity in the request.

### `POST /api/groups`

Create a group. The authenticated user should be added as a member automatically so the creator can immediately access it.

Example request:

```json
{
  "name": "Vancouver Trip",
  "baseCurrency": "CAD"
}
```

Response: `201 Created` with `{ "id": 7, "name": "Vancouver Trip", "createdAt": "2026-10-28T18:30:00Z", "baseCurrency": "CAD" }`. `baseCurrency` is required, uppercase ISO 4217 with a defined minor-unit exponent from 0 through 4. It is immutable from creation; no update endpoint is exposed. Group listing also includes `baseCurrency`.

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

Each expense carries its original ISO 4217 currency and amount, plus its group's immutable base currency, locked exchange rate, rate date, and rounded converted total. Rates are latest daily reference rates at recording time, not at the expense's calendar date. Reads never revalue expenses.

### Expense Representation/Object

```json
{
  "id": 301,
  "groupId": 7,
  "description": "Dinner",
  "amount": "100.00",
  "currency": "CAD",
  "baseCurrency": "CAD",
  "convertedAmount": "100.00",
  "exchangeRate": "1",
  "rateDate": "2026-11-14",
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

### `POST /api/groups/{groupId}/expenses/quote`

Authenticated group members (no admin membership bypass) may preview `{ "amount": "100.00", "currency": "USD" }`. Only these two fields are accepted; amount validation matches expense creation.

Response `200`: `{ "baseCurrency": "CAD", "convertedAmount": "140.00", "exchangeRate": "1.4", "rateDate": "2026-11-13" }`.

The server calls [Frankfurter](https://frankfurter.dev/) and caches rates by ordered currency pair and UTC request date, preserving the provider's actual observation date. Same-currency quotes use `1` without an external request. Preview does not persist anything; creation authoritatively resolves the rate again, so crossing UTC dates may change it. Conversion rounds the total once using decimal `HALF_UP` at base precision. Converted totals must fit a signed 64-bit minor-unit count; overflow is a validation error.

Provider failures/unsupported pairs return `503` with `{ "error": "exchange_rate_unavailable", "message": "...", "fields": {} }`; no expense is saved and the form preserves input for retry. Invalid quote input returns `400`; anonymous callers `401`; non-members `404`.

### `POST /api/expenses/{expenseId}/quote`

Same request/response as creation preview, restricted to the expense creator while still a group member. If currency is unchanged, uses the stored exchange rate and rate date; otherwise obtains the latest quote. This preview does not modify the expense. Non-members receive `404`, other members `403`.


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
| `amount`         | yes      | Exact decimal string with 1–9 integer digits and up to the currency's ISO 4217 minor-unit exponent in fractional digits (no decimal point for exponent 0); strictly positive. Missing fractional digits are zero-padded, never rounded; excess fractional digits are rejected. |
| `currency`       | yes      | Uppercase ISO 4217 code recognized by Java's `Currency` with a minor-unit exponent from 0 through 4. Codes without a defined minor unit, including `XXX` and `XTS`, are unsupported. |
| `date`           | no       | Valid `YYYY-MM-DD`; defaults to the current UTC date. Future dates are allowed (pre-logging a planned expense).                                                                      |
| `paidByUserId`   | no       | Integer user ID; must be a current member of the group. Defaults to the authenticated creator when omitted. Independent of `participantIds`: the payer need not appear in the split. |
| `participantIds` | no       | Non-empty array of unique integers; each must be a current member of the group; at most the current member count. When omitted, all current group members participate. Explicit `null` and empty arrays are invalid. The payer may appear or not appear. |

Equal splits use **base-currency** minor units after rounding the converted total. Participants are sorted by ascending user ID; each receives `convertedAmountMinor / participantCount`, and the first `convertedAmountMinor % participantCount` participants receive one additional minor unit. Split amounts always sum exactly to `convertedAmount`, not the original amount; zero shares and a zero converted total are allowed. Responses list splits in this same order.

Only omitted optional fields receive defaults. Explicit `null` values are invalid. Money must be a JSON string; IDs must be JSON integers, not strings or fractional numbers. Expense creation requires group membership even for admins.

Suggested response: `201 Created` with the expense representation, and a `Location: /api/expenses/{id}` header.

Validation failures return `400 Bad Request`:

```json
{
  "error": "validation_failed",
  "message": "Request body is invalid.",
  "fields": {
    "amount": "must have 1–9 integer digits and at most 2 fractional digits",
    "paidByUserId": "must reference a current member of the group",
    "participantIds": "must contain only current group members"
  }
}
```

Error behavior:

- `400 Bad Request` — malformed JSON, unknown/immutable field (`unsupported_field`), or any field-rule violation (`validation_failed`). Participant IDs that do not exist or are not members of this group, and a `paidByUserId` that is not a current member, are request errors, not `404`.
- `401 Unauthorized` — not signed in.
- `404 Not Found` — group does not exist or the caller is not a member. Group existence is not disclosed to non-members.
- `503 Service Unavailable` — the exchange-rate provider failed or has no usable rate for the currency pair. Nothing is persisted; preserve form input and retry.
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
- Supplied `amount` is validated against the effective original currency (supplied `currency`, or stored currency when omitted). A currency-only change retains the numeric original amount and rescales minor units exactly: CAD `"10.00"` becomes JPY `"10"`, but `"10.01"` cannot become JPY without a replacement amount.
- Amount-only changes reuse the stored rate/date. Changing the original currency obtains a new latest rate and converted total; changing it to the base currency uses `1` without an API call. Other edits do not refresh rates. Amount, currency, or participant changes recalculate equal shares from the converted total with the ascending-user-ID remainder rule. Provider failures return `503`; all parent/conversion/split changes roll back together.
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

Balances are derived on demand from surviving expenses. Edits and deletions are reflected on the next read; balances and suggested transfers are not stored.

For each expense, the payer (`paidBy`) is credited the stored converted total in the group's base currency and each stored split debits its participant in that same currency. The creator is not credited unless they are also the payer. Stored shares are authoritative; neither conversion nor equal shares are recalculated during balance reads.

`paid`, `owed`, and `net` are exact decimal strings at the **group base currency's** ISO 4217 exponent. Arithmetic uses integer minor units and aggregate totals are not limited by an individual expense's amount limit. `net = paid - owed`: positive is receivable, negative is payable. Foreign expenses are consolidated using their stored converted totals; rate fluctuations cannot change this ledger.

Suggested transfers are deterministic and advisory, not recorded payments, and operate exclusively in the group base currency. Initialize creditors with positive net and debtors with absolute negative net. Repeatedly select the largest remaining creditor and debtor (ties by ascending user ID), transfer the smaller remainder, then reinsert any nonzero remainder. Zero-net members are excluded.

### `GET /api/groups/{groupId}/balances`

Returns exactly one `currencies` section, denominated in the group's base currency, including for an empty group. It includes the full roster and monetary fields formatted at base precision.

```json
{
  "groupId": 7,
  "members": [
    { "id": 42, "email": "alex@example.com" },
    { "id": 57, "email": "sam@example.com" }
  ],
  "currencies": [
    {
      "currency": "CAD",
      "members": [
        { "userId": 42, "paid": "0.00", "owed": "25.00", "net": "-25.00" },
        { "userId": 57, "paid": "50.00", "owed": "25.00", "net": "25.00" }
      ],
      "transfers": [
        { "from": { "id": 42, "email": "alex@example.com" }, "to": { "id": 57, "email": "sam@example.com" }, "amount": "25.00" }
      ]
    }
  ],
  "generatedAt": "2026-11-15T09:00:00Z"
}
```

Semantics:

- Top-level `members` contains every current group member plus each payer and split participant in a surviving expense, sorted by ID. Former participants remain included, including participants whose stored share is zero. Expense creators are not included solely because they authored an expense. Profiles expose only `id` and `email`.
- The base-currency section repeats that roster sorted by `userId`; inactive members have zero `paid`, `owed`, and `net`.
- With no expenses or no outstanding balances, the base-currency section remains present with `transfers: []`.
- `generatedAt` is the response assembly time. It is not an edit timestamp or cache key.
- The endpoint is group-scoped and requires membership, including for admins. It does not paginate.

Error behavior:

- `401 Unauthorized` — not signed in.
- `404 Not Found` — group does not exist or the caller is not a member (existence is not disclosed).
- `500 Internal Server Error` — persistence failure.

## Authorization summary

- Unauthenticated requests to protected routes: `401 Unauthorized`.
- Authenticated users can list only their own groups.
- Group detail/member routes require membership (or an explicitly defined elevated role).
- The server derives the acting user from Spring Security, not from a caller-supplied `userId` field.
