# Music

KirBot provides a music system that server members can use to play YouTube, Soundcloud, Twitch, and
other audio streams in a voide channel.

**Note:** This feature may be removed without warning in the near future.

## The DJ Role

Users with a role named "DJ" have enhanced control over the music features of the bot. They are able
to disconnect the bot, switch the channel the bot is in, and other capabilities described later in
this section.

## Connecting

The command `!connect` or `!summon` will have KirBot join the current channel that you are currently
in. Optionally, if a channel name is provided, KirBot will attempt to join the channel specified.

If the bot is currently playing music in another channel, only a DJ can change the channel that it
is currently playing in.

## Playing Music

To play music, use the `!play` command, followed by the URL of the song to play, or search query to
search YouTube. If KirBot is not currently in a voice channel, it will be summoned automatically to
 the channel that you are currently in. If music is already playing, the song will be added to the
song queue and played in order.

## The Queue

To promote fairness, a queue system is used to play songs. Server administrators can set the maximum
length of the queue on the admin panel as well as enforcing a maximum length for songs.

Once a song ends, the bot will play the next song in the queue, removing it from the queue. This
process repeats until the queue is empty. After the queue is empty, the bot will automatically
disconnect from the voice channel.

### Skipping Songs

If someone queues up a song that you want to skip, you can start a vote to skip the song with the
`!skip` command. This will start a vote and wait a configurable amount of time (Configured by the
server administrators, defaults to 30 seconds) for the users in voice to vote yes or no to skipping.
Once the voting period concludes, the votes are tallied and the appropriate action is taken.

To prevent abuse of this feature, server administrators can enforce a cooldown (configurable on the
panel) between invocations of the skip command.
