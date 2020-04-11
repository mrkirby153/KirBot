# General Settings
The general settings tab contains miscelaneous settings that do not fall into other channels.

## Logging
Configure modlog settings. Log events can be configured per channel and the bot supports an unlimited number of logging channels per server.

Log events are filtered into two modes: Include and Exclude. When events are included, _only_ those events will be included in the modlogs for that channel. When events are excluded every event _except_ those on the list will be included in the channel.

Set the timezone of the logs with the **Log Timezone** field. Most conventional timezones are supported. In the event an unrecognized timezone being entered, the bot will silently fall back to UTC.

For more information regarding logs, see [Modlogs](../modules/logging.md).
### Log Events

| Event Name | Description |
| ---------- | ----------- |
| ROLE_DELETE | A role was deleted |
| ROLE_UPDATE | A role was modified (color, name, hoisted) |
| USER_NAME_CHANGE | A user changed their username |
| MESSAGE_DELETE | A message was deleted |
| CHANNEL_DELETE | A channel was deleted |
| USER_JOIN | A user joined the server |
| USER_LEAVE | A user left the server |
| MESSAGE_EDIT | A user's message was edited |
| ADMIN_COMMAND | A moderator or administrator performed a command |
| USER_NICKNAME_CHANGE | A user's nickname was changed |
| ROLE_REMOVE | A role was removed from a user |
| USER_MUTE | A user was muted with the `!mute` command |
| USER_UNMUTE | A user was unmuted with the `!unmute` command |
| CHANNEL_CREATE | A channel was created |
| MEMBER_RESTORE | A member was restored due to [Persistence](../modules/persistence.md) |
| USER_WARN | A user was warned |
| USER_KICK | A user was kicked from the server |
| USER_BAN | A user was banned from the server |
| ROLE_ADD | A role was added to a user |
| SPAM_VIOLATE | A user has violated a configured [Spam](../modules/spam.md) rule |
| MESSAGE_CENSOR | A user sent a message that violates a configured [Censor](../modules/censor.md) rule |
| NAME_CENSOR | A user's nickname/username violated a configured [Censor](../modules/censor.md) rule
| ROLE_CREATE | A role was created |
| USER_UNBAN | A user was unbanned |
| MESSAGE_BULKDELETE | More than 1 message was deleted (via a bot) at a time |
| VOICE_ACTION | A user has joined or left a voice channel, muted, unmuted, deafened or undeafened |
| CHANNEL_MODIFY | A channel was modified |



## Bot Nickname
Set the nickname of the bot. There is a maximum of 32 characters for the nickanme (Discord enforced)

## Muted Role
Set the role that's applied to the user when they are muted via bot commands or as a spam violation punishment

## User Persistence
User persistence allows for roles, nicknames, and voice states to be preserved even if a user leaves the server. Upon rejoining, KirBot will attempt to restore the user's roles, nickanmes, and voice state.

For more information, see [User Persistence](../modules/persistence.md).

### Settings
* **Persist Mute** - Persist the user's server mute in a voice channel
* **Persist Roles** - Restore a user's roles when they rejoin. A whitelist can be configured. If the whitelist is blank _all_ roles will be restored when rejoining.
* **Persist Deafen** - A user's server deafen state will be persisted
* **Persist Nickname** - A user's nickname will be automatically restored, assuming the bot has permission to interact with the user.

## Channel Whitelist
Configure channels that the bot can execute commands in. Most moderation commands ignore the command whitelist.


## Starboard
A starboard can be configured. For more information about the starboard, see [Starboard](../modules/starboard.md).

**Starboard Channel** - The channel where starboard posts will be sent

**Star Count** - The amount of star reactions needed to add the post to the starboard

**Gild Count** - The amount of star reactions needed to "gild" a post

**Self Star** - If users are allowed to star their own post.