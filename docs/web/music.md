# Music Settings
This panel controls the AutoDJ.

**NOTE:** Changes made here will take effect in up to 30 seconds.

## Master Switch
If it isn't obvious enough, this enables/disabled the Auto-DJ on the server.

## Channel Whitelist/Blacklist {#whitelist}
It may, at times, be useful to restrict KirBot from playing music in certain channels. This operates in two modes: Whitelist and Blacklist.

When in **Whitelist** mode, KirBot can only play music in channels on the list.

When in **Blacklist** mode, KirBot will be able to play music in all channels except those on the list.

## Queue Settings
Controlls various settings on the music queue

### Queue Length
The max length (in minutes) that the queue can be. This can be used to prevent the queue from becomming too long

### Max Song Length
The max length (in minutes) a song can be. Setting this to `-1` disables the max length

## Playlists
If users can queue playlists. Playlists that are queued do respect the [Max Song Length](#max-song-length) setting, and all songs that are longer will not be queued.

**NOTE:** Currently, playlists can bypass the Max Queue length, a fix is in progress.

## Skip Settings
Parameters for dealing with Skipping songs.

## Skip Cooldown
The amount of time (in seconds) a user has to wait after starting a skip to start a new one (this is added to the Skip Timer). This is in place to prevent skip spamming, where one user just spams skip commands. Setting this to `0` will disable it.

## Skip Timer
The time (in seconds) that the skip vote will wait before tallying votes and determining the result.
