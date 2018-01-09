# Command Reference

Below is a quick reference of commands and their descriptions. Arguments surrounded in `< >` are required and `[ ]` are optional.

_Note:_ This reference assumes the command prefix `!`. The prefix may be different on your server.

### A Note on Arguments
By default, KirBot interprets each word separated by a space as an independent argument. To consider multiple words as a single argument, surround the string you wish to have considered with `"`. If you want KirBot to interpret `"` literally, place a backslash: `\\` in front of it, like so: `\"`

#### Example:
`!testCommand One Two Three` has three arguments: `["One", "Two", "Three"]` while `!testCommand "One Two" Three` only has two: `["One Two", "Three"]`

---

## Complete Command Reference

## Moderation
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!clean <count> [user]` | Cleans the last messages in the channel | BOT_MANAGER | `!clean 50` Or `!clean 10 @mrkirby153#7840` |
| `!kick <user> [reason]` | Kicks a user from the server | BOT_MANAGER | `!kick @TestAccount#5467 Going off-topic` |
| `!mute <user>` | Mutes a user in the current channel | BOT_MANAGER | `!mute @TestAccount#5467` |
| `!unmute <user>` | Unmutes a user in the current channel | BOT_MANAGER | `!unmute @TestAccount#5647` |

## Groups
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!createGroup <name>` | Creates a group | BOT_MANAGER | `!createGroup Minecraft` |
| `!deleteGroup <name>` | Deletes a group | BOT_MANAGER | `!deleteGroup Roblox` |
| `!leaveGroup <name>` | Removes you from the given group | USER | `!leaveGroup "Star Citizen"` |
| `!joinGroup <name>` | Adds you to the given group | USER | `!joinGroup Minecraft` |
| `!groups` | Lists all the groups that exist on this server | USER | `!groups` |

## Music
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!play <url/query>` | Queues up a song in the Auto DJ | USER | `!play Never gonna give you up` or `!play https://youtu.be/yPYZpwSpKmA` |
| `!disconnect` | Disconnects the Auto DJ from the current channel | USER | `!disconnect` |
| `!queue [action]` | Displays the current Auto DJ queue | USER | `!queue` or `!queue shuffle` or `!queue clear` |
| `!skip` | Skips the current song by means of majority vote | USER | `!skip` |
| `!dequeue <position>` | Removes a song from the queue at the given position | BOT_MANAGER | `!dequeue 2` |
| `!volume <volume>` | Changes the volume of the Auto DJ | BOT_MANAGER | `!volume 10` or `!volume -5` |
| `!move <from> [to]` | Moves a song in the queue. If `to` is not specified, it defaults to next in the queue | BOT_MANAGER | `!move 3 1` or `!move 6`` |
| `!pause` | Pauses the music | BOT_MANAGER | `!pause` |
| `!connect` | Summons KirBot to your currently connected voice channel | USER | `!connect` |

## Other Commands
| Command | Description | Clearance | Example Usage |
| ------- | ---------- | --------- |
| `!updateNames` | Forces an update of everyone's nicknames from the database | BOT_MANAGER | `!updateNames` |
| `!poll <duration> <question> <options>` | Creates a timed poll. Options are separated with a comma (,) | USER | `!poll 24h "What is your favorite color?" "Blue, Red, Green"` |
| `!hideChannel` | Hides the channel from everyone on the server (except Administrators) | BOT_MANAGER | `!hideChannel` |
| `!stats` | Displays statistics about the robot | USER | `!stats` |
| `!clearance` | Displays your current Clearance level | USER | `!clearance` |
| `!refresh <item>` | Refreshes data from the database | SERVER_ADMINISTRATOR | `!refresh all` |
| `!overwatch <battletag>` | Displays Overwatch stats for a Battle Tag | USER | `!overwatch Somebody#1337` |
| `!color` | Changes your name's color | USER | `!color #FAFAFA` |
| `!permissions` | Dumps a list of permissions KirBot has | BOT_MANAGER | `!permissions` |
| `!quote <id>` | Displays a quote taken by KirBot earlier | USER | `!quote 10` |
| `!seen <user>` | Displays the last activity of a user | USER | `!seen @mrkirby153#7840` |
| `!ping` | Checks KirBot's Ping to Discord | USER | `!ping`|
| `!remindMe <time> <query>` | Have KirBot remind you about things | USER | `!remindMe 45m Buy eggs at the store` |
