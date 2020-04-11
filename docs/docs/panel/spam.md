# Anti-Spam

KirBot has highly configurable spam detection

## Configuring


### Rules
Rules match users whose clearance is equal to or lower than the rule's level.

For example, `101` will match all users who have clearance of `101` or less.

Each rule has two properties:

* **Count** -- The amount of the action that must be performed
* **Period** -- The period (in seconds) over which these actions must occurr

### Available Rules
The following rules are available

* **Max Links** -- Limits the amount of links users can send
* **Max Messages** -- Limits the amount of messages a user can send
* **Max Newlines** -- Limits the amount of newlines users can include in their messages
* **Max Mentions** -- Limits the amount of mentions
* **Max Duplicates** -- Limits the amount of duplicate messages users can send
* **Max Attachments** -- Limits the amount of attachments users can send

## Punishments

There are 6 punishments available that will be applied to users that violate the spam rules

* **No Action** -- Perform no moderation action, only clean messages.
* **Mute the user** -- Permanently mutes the user who violated the rule.
* **Kick the user** -- Kicks the user who violated the rule.
* **Ban the user** -- Permanently bans the user who violated the rule.
* **Tempban the user** -- Temporarily bans the user who violated the rule. Punishment duration must be set
* **Tempmute the user** -- Temporarily mutes the user who violated the rule. Punishment duration must be set

### Cleaning
In addition to punishing a user, KirBot can automatically clean their messages.

Two properties exist when configuring cleaning:

* **Clean Amount** -- Determines the number of messages to clean (i.e. Last 10 messages)
* **Clean Duration** -- Determines the duration of messages to clean (i.e. Last 30 seconds)
