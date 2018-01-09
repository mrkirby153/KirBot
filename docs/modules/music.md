# Auto DJ
The Auto DJ enables the playing of audio for various supported services in a Voice Channel on your discord server.

## List of Supported Services
* YouTube
* Vimeo
* SoundCloud

## The Music Queue
KirBot uses a queueing system to determine the order of songs that are played. Anyone can add their song to the queue with the `!play` command. The play command either takes a URL from a supported service, or a search query to search YouTube with. Searching YouTube will take the number one result, which can lead to some unforeseen circumstances.

Server administrators can restrict which channels KirBot can play music in from the [Web Panel](../web/general.md).

Server administrators can also restrict the length of the queue as well as the maximum length of songs that can be queued.

## Skipping
KirBot employs a voting method for skipping songs. To start a vote to skip the currently playing song, use the `!skip` command. This will start a vote to determine if the majority of the users listening wish to skip the currently playing song. By default, the timeout between the start of the skip and the tallying of votes is `30 seconds` but can be changed through the Web panel.

In the event that the song is changed before the skip timer expires, the vote will be canceled and the song will not change.
