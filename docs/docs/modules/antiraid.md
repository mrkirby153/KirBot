# Anti Raid

KirBot is equipped with an anti-raid system that can be used to quickly and effectively detect a
large amount of users joining the server (Most common feature of a raid) and perform a configured
action automatically to users that are classified as raiders.

## Detection

Before KirBot will start detecting raids, it needs to be configured first. Configure the anti raid
settings on the [Anti-Raid](../panel/antiraid.md) tab on the admin panel.

After enabling the raid master switch, KirBot will begin monitoring the time between user joins. If
the amount of users joined exceeds the rate specified in the configuration, a raid alarm will be
triggered.

### False Alarms
Since KirBot only monitors join rate, false alarms can happen if a large amount of people join from
legitimate sources (i.e. invite posted on social media). In this case, the raid can be dismissed and
the users unmuted. After dismissing the alarm, the bot will enter a quiet period where raid alarms
will not be triggered until the quiet period (configured in the panel) expires. Once the period expires,
a message is sent in the raid alerts channel notifying admins that the bot is once again watching
for raids.

## During a Raid

Once the raid alarm has been tripped, KirBot will send a message in the configured channel alerting
the server admin team about the detection of the raid as well as giving them options on how to deal
with the raid. 

KirBot can optionally ping a role (or @everyone or @here) when detecting a raid. If the role is not
mentionable, it is made mentionable, pinged, then no longer made mentionable. If KirBot intends to
ping @everyone or @here, it must have the permission enabled on the channel. It's advised that server
admins only enable mention @everyone or @here on the raid alert channel and not server-wide.

If KirBot is configured to Mute users, the Muted role configured on the panel will be given to the
user to lock them in for further actions.

**Note:** If an action is configured (Kick, Ban, Mute), it's possible that KirBot may fall behind
during particularly large raids and the ratelimits imposed by Discord.

## After a Raid

After the joining of users have slowed, KirBot will send a summary of the raid including how many
users were classified as raiders to the raid alert channel.

If the raiders were muted, admins are given the options to kick, ban, or unmute the raiders. This
option is not available if the raider punishment is set to Kick or Ban.

Additionally, a report of user ids (for quick reporting to Discord's Trust and Safety team) will be
made available on the panel for a minimum of 30 days after the raid.

## Commands

The following commands are a part of the module

* `!raid dismiss <id>` -- Dismisses the raid as a false alarm
* `!raid kick/ban/unmute <id>` -- Kicks/Bans/Unmutes the raiders
* `!raid info <id>` -- Provides information about a previous raid: number of raiders and their usernames/ids