# Command Reference

Below is a quick reference of commands and their descriptions. Arguments surrounded in `< >` are required and `[ ]` are optional.

_Note:_ This reference assumes the command prefix `!`. The prefix may be different on your server.

### Clearance
Each command has an assigned clearance to it. The invoker's clearance must be greater than or equal to the comamnd's clearance. Clearances can be set through the web panel.

#### Default Clearances
0 - Default (all users without any roles with clearance)

50 - Moderator

100 - Admin

### A Note on Arguments
By default, KirBot interprets each word separated by a space as an independent argument. To consider multiple words as a single argument, surround the string you wish to have considered with `"`. If you want KirBot to interpret `"` literally, place a backslash: `\\` in front of it, like so: `\"`

#### Example:
`!testCommand One Two Three` has three arguments: `["One", "Two", "Three"]` while `!testCommand "One Two" Three` only has two: `["One Two", "Three"]`

---

## Complete Command Reference

## Moderation
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!clean <count> [user]` | Cleans the last messages in the channel | 50 | `!clean 50` Or `!clean 10 @mrkirby153#7840` |
| `!kick <user> [reason]` | Kicks a user from the server | 50 | `!kick @TestAccount#5467 Going off-topic` |
| `!mute <user> <reason>` | Mutes a user | 50 | `!mute @TestAccount#5467` |
| `!unmute <user> <reason>` | Unmutes a user | 50 | `!unmute @TestAccount#5647` |
| `!tempmute <user> <time> <reason>` | Temporarily mutes a user | 50 | `!tempmute @TestAccount#567 30m Spam` |
| `!chanmute <user> <time>` | Temporarily mutes the user in the current channel | 50 | `!chanmute @TestAccuont#567 30m` |
| `!chanunmute <user>` | Unmutes the user in the current channel | 50 | `!chanunmute @TestAccount#567`|

## Groups
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!createGroup <name>` | Creates a group | 50 | `!createGroup Minecraft` |
| `!deleteGroup <name>` | Deletes a group | 50 | `!deleteGroup Roblox` |
| `!leaveGroup <name>` | Removes you from the given group | 0 | `!leaveGroup "Star Citizen"` |
| `!joinGroup <name>` | Adds you to the given group | 0 | `!joinGroup Minecraft` |
| `!groups` | Lists all the groups that exist on this server | 0 | `!groups` |

## Music
| Command | Description | Clearance | Usage |
| --- | --- | --- | --- |
| `!play <url/query>` | Queues up a song in the Auto DJ | 0 | `!play Never gonna give you up` or `!play https://youtu.be/yPYZpwSpKmA` |
| `!disconnect` | Disconnects the Auto DJ from the current channel | 0 | `!disconnect` |
| `!queue [action]` | Displays the current Auto DJ queue | 0 | `!queue` or `!queue shuffle` or `!queue clear` |
| `!skip` | Skips the current song by means of majority vote | 0 | `!skip` |
| `!dequeue <position>` | Removes a song from the queue at the given position | 50 | `!dequeue 2` |
| `!volume <volume>` | Changes the volume of the Auto DJ | 50 | `!volume 10` or `!volume -5` |
| `!move <from> [to]` | Moves a song in the queue. If `to` is not specified, it defaults to next in the queue | 50 | `!move 3 1` or `!move 6`` |
| `!pause` | Pauses the music | 50 | `!pause` |
| `!connect` | Summons KirBot to your currently connected voice channel | 0 | `!connect` |

## Other Commands
| Command | Description | Clearance | Example Usage |
| ------- | ---------- | --------- |
| `!updateNames` | Forces an update of everyone's nicknames from the database | 50 | `!updateNames` |
| `!poll <duration> <question> <options>` | Creates a timed poll. Options are separated with a comma (,) | 0 | `!poll 24h "What is your favorite color?" "Blue, Red, Green"` |
| `!hideChannel` | Hides the channel from everyone on the server (except Administrators) | 50 | `!hideChannel` |
| `!stats` | Displays statistics about the robot | 0 | `!stats` |
| `!clearance` | Displays your current Clearance level | 0 | `!clearance` |
| `!refresh <item>` | Refreshes data from the database | 100 | `!refresh all` |
| `!color` | Changes your name's color | 0 | `!color #FAFAFA` |
| `!permissions` | Dumps a list of permissions KirBot has | 50 | `!permissions` |
| `!quote <id>` | Displays a quote taken by KirBot earlier | 0 | `!quote 10` |
| `!seen <user>` | Displays the last activity of a user | 0 | `!seen @mrkirby153#7840` |
| `!ping` | Checks KirBot's Ping to Discord | 0 | `!ping`|
| `!remindMe <time> <query>` | Have KirBot remind you about things | 0 | `!remindMe 45m Buy eggs at the store` |
