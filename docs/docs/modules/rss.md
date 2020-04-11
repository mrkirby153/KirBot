# RSS

KirBot can be configured to subscribe to RSS feeds and post new updates to a given channel.

An example use case of this would be subscribing to a Youtuber's RSS feed and being notified of new
uploads.

KirBot's RSS feeds refresh every 15 minutes.

## Configuring

View a list of all currently configured RSS feeds in the current channel with the `!rss` command.
This will send a message with all the currently monitored feeds as well as its id to the channel.

### Subscribing

To subscribe to an RSS feed, use the `!rss subscribe <url>` command. After a successful invocation,
new RSS feed items will be posted in the channel every time the feed is refreshed (defaults to every
15 minutes).

### Unsubscribing

To unsubscribe from an RSS feed, grab teh feed's ID (the 10 digit alphanumberic string) with `!rss`.
Afterwards, run `!rss unsubscribe <id>`. This will immediately unsubscribe KirBot from the feed.

### Manual Refresh

Manually refreshing the RSS feed is accomplished with `!rss refresh <feed id>`. This will immediately
refresh the feed and post new entries to the channel.

#### Feed Errors

If KirBot fails to refresh a feed during one of its periodic refreshes, it will enter a failed state.
Feeds that have failed will not be automatically refreshed, but will need to be refreshed manually.
If the refresh is successful, KirBot will continue automatically refreshing the feed.

## Commands
* `!rss` -- Lists all RSS feeds configured in the channel
* `!rss add <url>` -- Subscribes to an RSS feed
* `!rss remove <id>` -- Unsubscribes from an RSS feed