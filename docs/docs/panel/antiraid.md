# Anti-Raid Settings

KirBot offers anti-raid protection for your server. View more information about anti-raid 
[here](../modules/antiraid.md).

## Master Switch
Enable/Disable the anti-raid settings. We recommend you keep this enabled and configured

## Detection Settings
Tweak various detection related settings

### Count
The amount of users that have to join to trigger a raid alert

### Period
The time period (in seconds) over which the users have to join

### Action
The action to apply to the user

* **Mute** - Applys the [Muted](general.md#muted-role) role to the user and locks them in for 
moderator action post-raid
* **Kick** - Immediately kicks the user
* **Ban** - Immediately bans the user

**Note:** Due to discord ratelimits, for large raids the action may be delayed.

### Quiet Period
The amount of time (in seconds) between user joins before the raid alert is terminated and a 
report generated.

## Alert Settings
Configure alert settings

## Alert Role
The role to ping when a raid is detected. If the role is not pingable and the bot has permisison, 
it will make it pingable.

In the case of `@everyone` or `@here`, the bot must have permission to mention everyone.

## Alert Channel
The channel where the alert will be sent