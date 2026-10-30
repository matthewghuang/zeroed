# Zeroed Data Model

## Purpose

This document explains the first-pass relational data model for Zeroed: user accounts, expense-sharing groups, and the relationship between users and groups. It also clarifies how login sessions fit into the design when authentication is managed by Spring Security.

## Overview

The application data model has three core tables:

- `users` stores account identity and the password hash.
- `groups` stores each expense-sharing group.
- `group_memberships` connects users to groups and records when each membership began.

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
- `created_at` — when the group was created; useful for sorting and auditing, but optional.
- `created_by_user_id` - user who created the group

### `group_memberships`

- `user_id` — foreign key to `users.id` and part of the composite primary key.
- `group_id` — foreign key to `groups.id` and part of the composite primary key.
- `joined_at` — when this particular user joined this particular group. It belongs to the relationship, not to the user or group, because each membership has its own date.

## Login sessions and Spring Security

Spring Security handles authentication and the HTTP session; after successful login, the backend issues a session cookie, and requests carrying that cookie are associated with the server-side session.
