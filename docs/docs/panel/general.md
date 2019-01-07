# General Settings
The general settings tab contains miscelaneous settings that do not fall into other channels.

## Logging
Configure modlog settings. Log events can be configured per channel and the bot supports an unlimited number of logging channels per server.

Log events are filtered into two modes: Include and Exclude. When events are included, _only_ those events will be included in the modlogs for that channel. When events are excluded every event _except_ those on the list will be included in the channel.

Set the timezone of the logs with the **Log Timezone** field. Most conventional timezones are supported. In the event an unrecognized timezone being entered, the bot will silently fall back to UTC.

### Log Events

| Event Name | Description |
| ---------- | ----------- |
| MEMBER_RESTORE | A user has been restored because of persistence |
| ROLE_DELETE | A role was deleted |
| ROLE_UPDATE | A role was modified |
| USER_NAME_CHANGE | A user changed their name |
| MESSAGE_DELETE | A message was deleted |
| USER_WARN | A user was warned |
| USER_KICK | A user was kicked from the server by the bot. This does not currently support manual kicks |
| USER_BAN | A user was banned from the server |
| ROLE_ADD | A role was added to a user |
| USER_JOIN | A user joined the server |
| USER_LEAVE | A user left the server |
| SPAM_VIOLATE | A user has violated any of the [spam](panel/spam.md) rules |
| MESSAGE_CENSOR | A user has violated any of the [censor](panel/censor.md) rules |
| MESSAGE_EDIT | A user has edited their message |
| ROLE_CREATE | A Role was created |
| ADMIN_COMMAND | A moderator or admin used an admin command (Not all commands are logged) |
| USER_UNBAN | A user has been unbanned |
| USER_NICKNAME_CHANGE | A user changed their nickname |
| ROLE_REMOVE | A role has been removed from a user |
| MESSAGE_BULKDELETE | A bot has purged more than one message from a channel. An archive will automatically be created |
| USER_MUTE | A user was muted via the `mute` command |
| USER_UNMUTE | A user was unmuted |
| VOICE_ACTION | A user joins, leaves, or moves in a voice channel |

## Bot Nickname
Set the nickname of the bot. There is a maximum of 32 characters for the nickanme (Discord enforced)

## Muted Role
Set the role that's applied to the user when they are muted via bot commands or as a spam violation punishment

## User Persistence
User persistence allows for roles, nicknames, and voice states to be preserved even if a user leaves the server. Upon rejoining, KirBot will attempt to restore the user's roles, nickanmes, and voice state.

### Settings
* **Persist Mute** - Persist the user's server mute in a voice channel
* **Persist Roles** - Restore a user's roles when they rejoin. A whitelist can be configured. If the whitelist is blank _all_ roles will be restored when rejoining.
* **Persist Deafen** - A user's server deafen state will be persisted
* **Persist Nickname** - A user's nickname will be automatically restored, assuming the bot has permission to interact with the user.

## Channel Whitelist
Configure channels that the bot can execute commands in. Most moderation commands ignore the command whitelist.

## Starboard/Quotes
KirBot supports both starboards and quotes. These two features are mutually exclusive and enabling the starboard will disable the quoting features.

### Quoting
When the starboard is disabled, reacting to _any_ message with 🗨 will create a quote. The quote ID will be displayed and can later be retrieved with the `quote <id>` command.

### Starboard
In addition to quoting, a starboard can be used instead.

**Starboard Channel** - The channel where starboard posts will be sent

**Star Count** - The amount of star reactions needed to add the post to the starboard

**Gild Count** - The amount of star reactions needed to "gild" a post

**Self Star** - If users are allowed to star their own post.