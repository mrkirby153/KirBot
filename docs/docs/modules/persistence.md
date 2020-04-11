# Persistence

Sometimes, server administrators want to keep various aspects of a user's state on the server, such
as their roles and nickname. Persistence enables this by creating a backup of the user if they leave
the server.

In order to protect user privacy, backups are created if the following two criteria are met:

1. The server has persistence enabled
2. The user leaves the server

Additionally, once a user has rejoined the server, their backup is deleted upon restoration.

## Role Persistence

KirBot will only restore roles if this setting is enabled. The bot must additionally have permission
to manage roles on the server so that it can give user's roles back. By default, all roles are
persisted, but administrators can restrict this to certain roles on the [admin panel](../panel/general.md#user-persistence).

## Voice Mute/Deafen Persistence

A user's voice state can be persisted. If a user rejoins the server and voice mute/deafen persistence
is enabled, their backup is kept until they join voice, at which the backup is fully applied and
deleted from the database.

## Nickname Persistence

A user's nickname can be persisted. KirBot must have permission to Manage Nicknames in order to 
succesfully restore a user's nickname.