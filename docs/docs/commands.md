# Admin
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `refresh ` | 100 | Updates the currently running configuration with the database |
| `clearance [user:user]` | 0 | Displays the user's clearance |
| `stats ` | 0 | Displays statistics about the bot |
| `clean ` | 50 | Cleans (deletes) messages |
| `clean bots [amount:int]` | 50 | Clean messages sent by bots in the current channel |
| `clean all [amount:int]` | 50 | Cleans messages from everyone in the current channel |
| `clean user <user:snowflake> [amount:int]` | 50 | Clean messages sent by a specific user in the current channel |

# Uncategorized
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `archive ` | 50 | Create an archive |
| `archive user <user:snowflake> [amount:int]` | 50 | Create an archive of messages that a user has sent |
| `archive channel <channel:snowflake> [amount:int]` | 50 | Creates an archive of messages sent in a channel |
| `temprole <user:snowflake> <role:string> <duration:string> [reason:string...]` | 100 | No description provided |
| `jumbo <emojis:string...>` | 0 | Sends a bigger version of the given emojis |
| `ping ` | 0 | Check the bot's ping |
| `charinfo <text:string...>` | 0 | Get information about a string of characters |
| `role ` | 50 | List all the roles and their IDs |
| `role remove/rem <user:snowflake> <role:string> [reason:string...]` | 50 |  |
| `role add <user:snowflake> <role:string> [reason:string...]` | 50 | Add a role to the given user |
| `urban <term:string...>` | 0 | Retrieve definitions of words from the Urban Dictionary |
| `permissions ` | 100 | Displays all the permissions the bot currently has in the current channel |
| `star ` | 50 | Starboard related commands |
| `star update <mid:snowflake>` | 50 | Forces an update of the starboard message |
| `star unhide <mid:snowflake>` | 50 | Unhides an entry from the starboard |
| `star hide <mid:snowflake>` | 50 | Hides an entry from the starboard |
| `star unblock <user:snowflake>` | 50 | Unblocks a user from the starboard |
| `star block <user:snowflake>` | 50 | Blocks a user from the starboard. They cannot star messages and their messages cannot be starred |
| `selfrole ` | 0 | Displays a list of self-assignable roles |
| `selfrole add <role:string...>` | 100 | Add a role to the list of self-assignable roles |
| `selfrole remove <role:string...>` | 100 | Removes a role from the list of self-assignable roles |
| `selfrole join <role:string...>` | 0 | Join a self-assignable role |
| `selfrole leave <role:string...>` | 0 | Leave a self-assignable role |
| `dumpsettings ` | 100 |  |

# Music
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `skip ` | 0 | Skips the currently playing song |
| `skip force ` | 0 | No description provided |
| `disconnect ` | 0 | Disconnects the bot from the current voice channel |
| `dequeue <position:int>` | 0 | Removes a previously queued song from the queue |
| `pause ` | 50 | Pause the music that is currently playing |
| `play [query/url:string...]` | 0 | Play music |
| `queue [option:string]` | 0 | Shows the current queue |
| `volume [volume:string]` | 50 | Sets the bot's volume |
| `move <from:int> [to:int]` | 0 | Move songs around in the queue |
| `connect [channel:string...]` | 0 | Connects the bot to the voice channel |

# Moderation
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `tempban <user:snowflake> <time:string> [reason:string...]` | 50 | Temporarily bans a user |
| `unban <user:snowflake> [reason:string...]` | 50 | Unbans a user |
| `tempmute <user:user> <time:string> [reason:string...]` | 0 | Temporarily mute the given user |
| `unhide ` | 100 | No description provided |
| `unmute <user:user> [reason:string...]` | 50 | Unmute a user (Remove the configured muted role) |
| `softban <user:snowflake> [reason:string...]` | 50 | Soft-bans (kicks and deletes the last 7 days) a user |
| `forceban <user:snowflake> [reason:string...]` | 50 | Force bans a user |
| `lock [msg:string...]` | 50 | No description provided |
| `modlog ` | 100 | Modlog related commands |
| `modlog unhush ` | 100 | Unhush the modlogs |
| `modlog hush ` | 100 | Hush the modlogs (Message deletes won't be logged) |
| `modlog unhide <user:user>` | 100 | Unhides a user from the modlogs |
| `modlog hide <user:user>` | 100 | Hides a user from the modlogs |
| `modlog hidden ` | 100 | List all the hidden users |
| `kick <user:user> [reason:string...]` | 50 | Kick a user |
| `mute <user:user> [reason:string...]` | 50 | Mute a user (Assign the set muted role) |
| `ban <user:user> [reason:string...]` | 50 | Bans a user |
| `infractions [user:snowflake]` | 50 | Infraction related commands |
| `infractions search [query:string...]` | 50 | Search for an infraction with the given query |
| `infractions clear <id:int> [reason:string...]` | 50 | Clears an infraction (Deletes it from the database) |
| `infractions import-banlist ` | 100 | Imports the banlist as infractions |
| `infractions info <id:int>` | 50 | Gets detailed information about an infraction |
| `infractions reason <id:number> <reason:string...>` | 50 | Sets the reason of an infraction |
| `infractions export ` | 50 | Exports a CSV of infractions |
| `warn <user:snowflake> <reason:string...>` | 0 | Register a warning infraction for the given user |
| `unlock ` | 50 | No description provided |
| `hide ` | 100 | No description provided |

# Miscellaneous
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `rss ` | 50 | Shows a list of RSS feeds currently being monitored |
| `rss refresh [id:string]` | 50 | Refresh a feed |
| `rss add <url:string>` | 50 | Adds a feed to be watched |
| `rss remove <id:string>` | 50 | Removes a feed from the watch list |
| `rss list ` | 50 | Show a list of RSS feeds being monitored |
| `help [command:string...]` | 0 | Display help for a command |

# Fun
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| `color <color:string>` | 0 | Sets a user's color in the member list |
| `remindme <time:string> <query:string...>` | 0 |  |
| `quotes ` | 0 | Show quote help |
| `quotes block <user:snowflake>` | 50 | Blocks a user from being quoted |
| `quotes unblock <user:snowflake>` | 50 | Unblocks a user that was previously blocked, allowing them to quote again |
| `poll <duration:string> <question:string> <options:string...>` | 0 | Create polls |
| `quote [id:int]` | 0 | Displays a quote previously taken |
| `info [user:user]` | 0 | Retrieves information about a user |

