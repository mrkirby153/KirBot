# Starboard

The starboard module can be used to send messages that get a specific number of reactions to a
designated channel.

The Starboard must be configured from the [web admin panel](../panel/general.md#starboard)

## Starring a message

Users can star messages with the ⭐ reaction. Once a message has gotten a number of stars configured
by the server administrators, it is published to the starboard.

If a message falls below the required number of stars, it will be deleted from the starboard.

### Gilding

Gilding acts as a 2nd tier of star. If a message gets enough stars to gild, its icon on the starboard
will be replaced with 🌠 to indicate that it has been gilded.

## Moderating the Starboard

Moderation commands are available to help server moderators moderate the starboard.

### Blocking Users

Users can be blocked from the starboard. When a user is blocked from the starboard, their stars have
no effect, and their messages cannot be starred. To block a user from the starboard, use the
`!starboard block <user id>` command. To unblock a previously blocked user, use the `!starboard unblock <user id>`
command.

### Hiding Posts

If a post makes it to the starboard and needs to be hidden, moderators can use the
`!starboard hide <message id>` to hide the message from the starboard. It will immediately be deleted
from the starboard channel, but votes _will_ still count for it. To unhide a previously hidden
starboard message, use `!starboard unhide <message id>`

## Commands

* `!starboard block <user id>` -- Blocks a user from the starboard
* `!starboard unblock <user id>` -- Unblocks a user from the starboard
* `!starboard hide <message id>` -- Hides a message from the starboard
* `!starboard unhide <message id>` -- Unhides a message from the starboard
* `!starboard check <message id>` -- Forces a check of the provided message