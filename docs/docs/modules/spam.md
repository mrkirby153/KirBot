# Anti-Spam

KirBot is equipped with a customizable anti-spam system to help keep servers free from spam.

Due to its complexity, all spam rules must be configured on the admin panel. See [here](../panel/spam.md)
for configuration instructions.

## Spam Levels

Each spam rule has a level associated with it. This level is used to determine which users the rule
will apply to. If a spam rule's level is less than or equal to the user's clearance, the rule will
be applied to the user's messages.

**Note:** Bots are not excluded from anti-spam. Assuming the default anti-spam level is 0, giving the
bot's role a clearance of 1 in the [Permission Settings](../panel/permissions.md) will prevent spam
rules from affecting the bot.

## Anti-Spam Rules

A wide range of rules are available and not every rule needs to be enabled at the same time.

Each rule has two configuration values: count and period. Count is the number of times the rule's
action needs to be performed before the user will be punished. Period is the period of time (in
seconds) over which the count needs to be done. For example, the Max Links rule configured with a
count of 5 and a period of 30 will only let users send messages with 5 links in 30 seconds.

### Max Links

Limits the maximum amount of links that a user can send.

### Max Messages

Limits the maximum amount of individual messages that a user can send. This does not check the
length of the message.

### Max Newlines

Limits the amount of newlines users can send in their messages.

### Max Mentions

Limits the amount of user or role mentions users can send. This does _not_ apply to channel mentions.

### Max Duplicates

Limits the amount of duplicate messages a user can send. It's recommended to keep this low, as most
users will not send identical messages.

**Note:** This rule only applies to messages that are identical. Variations in capitalization, spacing,
or punctuation will not be considered a duplicate message.

### Max Attachments

Limits the amount of attachments users can upload.

## Punishments

If a user violates any spam rule, there are 6 actions that can be taken:
* No action
* Kick the user
* Ban the user
* Mute the user
* Temporarily ban the user
* Temporarily mute the user

If a temporary action is chosen, the Punishment Duration must be configured on the panel.

## Cleaning

In addition to punishing the user, KirBot can also clean up the user's messages. KirBot has two modes
for cleaning: Clean Amount and Clean Duration.

Clean Amount will clean the user's last messages. For example, if the clean amount is set to 50, users
who violate any censor rule will have their last 50 messages deleted.

Clean Duration will clean the user's last messages over time. For example, if the clean duration is set to
300 seconds, users who violate any censor rule will have all messages sent in the last 5 minutes deleted.