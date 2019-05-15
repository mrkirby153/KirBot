# Uncategorized
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| ` rss list ` | 50 | Show a list of RSS feeds being monitored |
| ` rss remove <id:string>` | 50 | Removes a feed from the watch list |
| ` rss refresh [id:string]` | 50 | Refresh a feed |
| ` rss add <url:string>` | 50 | Adds a feed to be watched |
| ` starboard update <mid:snowflake>` | 50 | Forces an update of the starboard message |
| ` starboard unhide <mid:snowflake>` | 50 | Unhides an entry from the starboard |
| ` starboard hide <mid:snowflake>` | 50 | Hides an entry from the starboard |
| ` starboard block <user:snowflake>` | 50 | Blocks a user from the starboard. They cannot star messages and their messages cannot be starred |
| ` starboard unblock <user:snowflake>` | 50 | Unblocks a user from the starboard |
| ` selfrole add <role:string...>` | 100 | Add a role to the list of self-assignable roles |
| ` selfrole remove <role:string...>` | 100 | Removes a role from the list of self-assignable roles |
| ` selfrole join <role:string...>` | 0 | Join a self-assignable role |
| ` selfrole leave <role:string...>` | 0 | Leave a self-assignable role |
| ` jumbo <emojis:string...>` | 0 | Sends a bigger version of the given emojis |
| ` hide ` | 100 | No description provided |
| ` temprole <user:snowflake> <role:string> <duration:string> [reason:string...]` | 100 | No description provided |
| ` markov delete <user:snowflake>` | 0 | No description provided |
| ` pause ` | 50 | Pause the music that is currently playing |
| ` move <from:int> [to:int]` | 0 | Move songs around in the queue |
| ` dumpSettings ` | 100 | Dump the raw guild settings |
| ` unhide ` | 100 | No description provided |
| ` skip,next ` | 0 | Skips the currently playing song |
| ` force ` | 0 | No description provided |
| ` dequeue <position:int>` | 0 | Removes a previously queued song from the queue |
| ` setstatus <status:string> <type:string> <game:string...>` | 0 | No description provided |
| ` permissions ` | 100 | Displays all the permissions the bot currently has in the current channel |
| ` clearance [user:user]` | 0 | Displays the user's clearance |
| ` help [command:string...]` | 0 | Display help for a command |
| ` quotes block <user:snowflake>` | 50 | Blocks a user from being quoted |
| ` quotes unblock <user:snowflake>` | 50 | Unblocks a user that was previously blocked, allowing them to quote again |
| ` info [user:user]` | 0 | Retrieves information about a user |
| ` queue,np,nowplaying [option:string]` | 0 | Shows the current queue |
| ` volume [volume:string]` | 50 | Sets the bot's volume |
| ` play [query/url:string...]` | 0 | Play music |
| ` role remove <user:snowflake> <role:string> [reason:string...]` | 50 | Remove a role from the given user |
| ` role add <user:snowflake> <role:string> [reason:string...]` | 50 | Add a role to the given user |
| ` ping ` | 0 | Check the bot's ping |
| ` db-backup ` | 0 | No description provided |
| ` connect,summon [channel:string...]` | 0 | Connects the bot to the voice channel |
| ` charinfo <text:string...>` | 0 | Get information about a string of characters |
| ` whitelistGuild <guild:string>` | 0 | No description provided |

# Moderation
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| ` modlog unhide <user:user>` | 100 | Unhides a user from the modlogs |
| ` modlog hide <user:user>` | 100 | Hides a user from the modlogs |
| ` modlog hidden ` | 100 | List all the hidden users |
| ` modlog hush ` | 100 | Hush the modlogs (Message deletes won't be logged) |
| ` modlog unhush ` | 100 | Unhush the modlogs |
| ` unmute,unquiet <user:user> [reason:string...]` | 50 | Unmute a user (Remove the configured muted role) |
| ` raid kick <id:string>` | 50 | No description provided |
| ` raid info <id:string>` | 50 | No description provided |
| ` raid ban <id:string>` | 50 | No description provided |
| ` raid unmute <id:string>` | 50 | No description provided |
| ` raid dismiss ` | 50 | No description provided |
| ` lock [msg:string...]` | 50 | No description provided |
| ` infraction search [query:string...]` | 50 | Search for an infraction with the given query |
| ` infraction info <id:int>` | 50 | Gets detailed information about an infraction |
| ` infraction clear <id:int> [reason:string...]` | 50 | Clears an infraction (Deletes it from the database) |
| ` infraction import-banlist ` | 100 | Imports the banlist as infractions |
| ` infraction export ` | 50 | Exports a CSV of infractions |
| ` infraction reason <id:number> <reason:string...>` | 50 | Sets the reason of an infraction |
| ` ban <user:user> [reason:string...]` | 50 | Bans a user |
| ` unban <user:snowflake> [reason:string...]` | 50 | Unbans a user |
| ` mute,shutup,quiet <user:user> [reason:string...]` | 50 | Mute a user (Assign the set muted role) |
| ` softban <user:snowflake> [reason:string...]` | 50 | Soft-bans (kicks and deletes the last 7 days) a user |
| ` archive channel <channel:snowflake> [amount:int]` | 50 | Creates an archive of messages sent in a channel |
| ` archive user <user:snowflake> [amount:int]` | 50 | Create an archive of messages that a user has sent |
| ` kick <user:user> [reason:string...]` | 50 | Kick a user |
| ` forceban <user:snowflake> [reason:string...]` | 50 | Force bans a user |
| ` tempban <user:snowflake> <time:string> [reason:string...]` | 50 | Temporarily bans a user |
| ` warn <user:snowflake> <reason:string...>` | 50 | Register a warning infraction for the given user |
| ` unlock ` | 50 | No description provided |
| ` tempmute <user:user> <time:string> [reason:string...]` | 50 | Temporarily mute the given user |

# Music
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| ` disconnect ` | 0 | Disconnects the bot from the current voice channel |

# Admin
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| ` refresh ` | 100 | Updates the currently running configuration with the database |
| ` all [amount:int]` | 50 | Cleans messages from everyone in the current channel |
| ` user <user:snowflake> [amount:int]` | 50 | Clean messages sent by a specific user in the current channel |
| ` bots [amount:int]` | 50 | Clean messages sent by bots in the current channel |
| ` stats ` | 0 | Displays statistics about the bot |

# Fun
| Command | Clearance Level | Description |
|---------|-----------------|-------------|
| ` remind <time:string> <query:string...>` | 0 | Set reminders |
| ` server [server:snowflake]` | 50 | No description provided |
| ` quote [id:int]` | 0 | Displays a quote previously taken |
| ` poll <duration:string> <question:string> <options:string...>` | 0 | Create polls |
| ` color <color:string>` | 0 | Sets a user's color in the member list |
| ` urban <term:string...>` | 0 | Retrieve definitions of words from the Urban Dictionary |
