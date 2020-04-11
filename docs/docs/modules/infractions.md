# Infractions

KirBot offers an infraction tracking system to track information about a user such as kicks, bans,
and warnings.

Even if a user leaves the server, their infraction history is preserved.

## Types of Infractions

There are 8 different types of infractions.

1. Kick - Created when a user is kicked.
2. Mute - Created when a user is muted with the `!mute` command.
3. Warning - Created when a user is warned with the `!warn` command.
4. Ban - Created when users are banned.
5. Unban - Created when a user is unbanned (Useful for tracking the unban reason)
6. Tempban - Created when a user is temporarily banned
7. Tempmute - Created when a user is temporarily muted

## Infraction Logging

Infractions are created automatically when using the moderation commands built in to KirBot. Additionally, 
infractions are created when automatic actions are performed by KirBot (such as banning a user for spam). 

KirBot does not by default track manual actions like banning a user via the built in Discord menu. This
behavior can be changed by enabling the "Log Manual Actions" toggle on the 
[Infraction](../panel/infractions.md) page on the panel.

### Infraction History

A user's infraction history can be searched with the `!infraction search <query>` command. This command returns
a table of infractions that the user has in the current server.

More details about an infraction can be viewed with the `!infraction info <id>` command.

### Deleting Infractions

While it's not recommended to delete infractions, Administrators (or anyone with clearance 100+ 
by default) can delete infractions if it was created mistakenly with the `!infraction clear <id>`
command.

### Infraction Reasons

All infractions have a reason field attached. When kicking/banning/muting/warning a user, the reason
is automatically set to the provided reason. If the moderation team wishes to modify an infraction
reason, they can do so with the `!infraction reason <id> <new reason>` command.

## Commands
* `!infraction search <id>` -- Search the infraction database for the given query
* `!infraction info <id>` -- Retrieves more details about an infraction
* `!infraction delete <id>` -- Deletes an infraction
* `!infraction reason <id> <new reason>` -- Updates the reason for an infraction

### Moderation Commands
* `!kick <user> [reason]` -- Kicks a user
* `!ban <user> [reason]` -- Bans a user
* `!tempban <user> <time> [reason]` -- Temporarily bans a user
* `!mute <user> [reason]` -- Mutes a user
* `!tempmute <user> [reason]` -- Temporarily mutes a user
* `!warn <user> <reason>` -- Warns the user

When performing a moderation command, KirBot can send a DM with the reason to the user. To send a DM
to the user, prefix the infraction with `[DM]` to send a normal DM (You have been warned in **Kirby's
test server** by mrkirby153#7840 for: `Example`) or an anonymous DM (You have been warned in **Kirby's
test server** for: `Example`).