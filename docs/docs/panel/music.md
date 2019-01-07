# Music
KirBot supports playing music in voice channels. If you do not wish to use this feature, turn the `Master Switch` off.

## Master Switch

Globally controls if music is enabled or disabled. If the switch is off, the bot will not respond to any music commands and music commands will be removed from the help box.

## Channel Whitelist/Blacklist
Control which channels the bot can play music in.

There are three modes:

* **Whitelist** - Only allow music playback in the given channels
* **Blacklist** - Allow music playback in all channels except the given
* **Off** - Disable the whitelist/backlist and allow playback everywhere

## Queue Settings
Configure various queue-related settings

### Queue Length
The maximum length of the queue in minutes. Any song that is added to the queue that would put the queue over the max length will be rejected.

Set to `-1` to disable and allow the queue to grow to an unlimited length.

### Max Song Length
The maximum length of a song that can be queued in minutes.

Set to `-1` to disable and allow any song to be added to the queue regardless of length.

### Allow Playlists
If playlists can be queued. Playlists will be queued as individual songs subject to the **Queue Length** and **Max Song Length** settings.

## Skip Settings
KirBot supports vote skipping when there is more than one person in the channel.

### Skip Cooldown
The amount of time (in seconds) that a user must wait between starting vote skips.

### Skip Timer
The amount of time (in seconds) that KirBot will wait before tallying votes.