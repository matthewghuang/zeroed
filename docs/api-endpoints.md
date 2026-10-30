# Zeroed API — Initial Endpoints

## Conventions

- Base path: `/api`.
- Request and response bodies use JSON except where Spring Security's configured login mechanism requires otherwise.
- Authentication uses a Spring Security server-side session. After login, the backend sets the session cookie; the React client sends it with subsequent requests.
- All group endpoints require an authenticated user. A user may access only groups they belong to, except where an explicitly agreed admin rule applies.
- Never return passwords, password hashes, session IDs, or other secrets in JSON.
- Suggested errors: `400 Bad Request` for invalid input, `401 Unauthorized` when not signed in, `403 Forbidden` when signed in but not permitted, `404 Not Found` when the requested resource does not exist or should not be disclosed, and `409 Conflict` for duplicates.

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
  "name": "Vancouver Trip"
}
```

Suggested response: `201 Created` with the new group, e.g. `{ "id": 7, "name": "Vancouver Trip", "createdAt": "2026-10-28T18:30:00Z" }`.

### `GET /api/groups/{groupId}`

Get details for one group. Require the caller to be a member of that group (or have a separately defined admin permission).

Suggested response: `200 OK` with group details. Return `404 Not Found` when the group does not exist or is not visible to the caller, to avoid leaking group existence.

## Group members

### `GET /api/groups/{groupId}/members`

List members of a group the caller is allowed to access.

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

Proposed request by email (a user ID could be used instead if the UI first selects from a member directory):

```json
{
  "email": "sam@example.com"
}
```

Suggested behavior and responses:

- `201 Created` when membership is added.
- `400 Bad Request` for malformed input.
- `404 Not Found` if the group or target account is not found/visible.
- `409 Conflict` if the person is already a member.

## Authorization summary

- Unauthenticated requests to protected routes: `401 Unauthorized`.
- Authenticated users can list only their own groups.
- Group detail/member routes require membership (or an explicitly defined elevated role).
- The server derives the acting user from Spring Security, not from a caller-supplied `userId` field.
