# About Clearance

Clearance is the bot's internal permission system. Users' clearance is assigned based on the roles that they have.

A user's clearance is determined by the highest clearance attached to the roles they have.

#### Example
If a user has two roles: `Trusted` and `Administrator`, with a clearance of `50` and `100` respectively, their clearance will be `100`.

## Assigning Clearance
Roles can be assigned clearance from the [permissions](panel/permissions.md) page in the admin panel.

## Default Clearances

There are 3 "levels" of clearance used by the bot

| Level | Clearance |
| ----- | --------- |
| Everyone | 0 |
| Moderator | 50 |
| Administrator | 100 |