# Adding KirBot
Adding KirBot to your server can be done via two ways:

## Direct Link
Click [here](https://kirbot.mrkirby153.com/add) to add the bot to your server.

## Server Setup
After adding the bot a few setup tasks must be performed.

1. Ensure the bot has the necessary permissions to function (this should automatically be completed
when adding the bot)
2. Positioning KirBot's role such that it can interact with the most users possible
    * KirBot can only interact with roles lower than its highest role in the role hierarchy, 
    so ensure KirBot's highest role is as high as possible.
3. Log into the [admin panel](https://kirbot.mrkirby153.com/servers), select your server 
and configure the bot to your heart's content. If this is your first time working with KirBot,
take a look at the [Quick Start Guide](quickstart.md).

## Permission Breakdown
KirBot requires these permissions by default. 

Permissions marked with a * are essential for correct operation

* Manage Roles: Support for Mutes, `!color` and Temprole commands
* Manage Channels: (Un)hide channels and lock channels
* Kick Members: Kick members as part of infraction tracking
* Ban Members: Ban/Unban members as part of infraction tracking
* Manage Nicknames: Nickname Censoring
* Change Nickname: Changing the bot's nickname through the web admin panel
* View Audit Log: Correctly attributing reasons to manual infraction recording
* Read Messages*: Receive messages, Command invocation, modlogs
* Send Messages*: Respond to commands, 
* Manage Messages: Censoring messages
* Embed Links: Respond using embeds to specific commands
* Attach Files: Upload files in certain circumstances (i.e. output is too long)
* Read Message History: Recover mod log messages if the bot goes offline
* Add Reactions: React to certain commands for confirmation, pagination, etc.
* Use External Emojis*: Use the bot's custom emojis
* Connect: Connect to voice to play Music
* Speak: Play Music