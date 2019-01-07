# Anti-Spam

KirBot has highly configurable spam detection. Due to its high configurability, these settigs must be implemented via JSON configuration.

## Configuring


### Rules
Each json object has a key which represents the clearance level that the rule applies to. Rules match users whose clearance is equal to or lower than the block.

For example, `101` will match all users who have clearance of `101` or less.

Each rule has two properties:

| Property | Description |
| -------- | ----------- |
| count | The amount of the action that must be performed |
| period | The period (in secinds) over wich these actions must occur |

### Available Rules

| Rule | Description |
| ---- | ----------- |
| max_messages | Limits the amount of messages |
| max_newlines | Limits the amount of newlines |
| max_mentions | Limits the amount of mentions |
| max_links | Limits the amount of links |
| max_emoji | Limits the amount of custom emoji. Unicode emoji is currently not supported |
| max_uppercase | Limits the amount of uppercase letters |
| max_attachments | Limits the amount of attachments |
| max_duplicates | Limits the amount of duplicate messages |

### Punishments
A punishment can be attached to a rule. The `punishment` property has 6 values: `NONE, MUTE, KICK, BAN, TEMPBAN, TEMPMUTE`.

For temporary punishments, provide a `punishment_duration`, which is the amount of time (in seconds) that the punishment should be effective for.

### Cleaning
In addition to punishing a user, KirBot can automatically clean their messages.

Two properties exist: `clean_amount` which determines the number of messages to clean, and `clean_duration` which determines the duration of messages to clean.

## Example Configuration

Configuring anti-spam may be a bit tricky, so an example configuration has been provided below.
```json
{
  "0": {
    "punishment": "KICK",
    "clean_duration": 300,
    "max_links": {
      "count": 10,
      "period": 60
    },
    "max_messages": {
      "count": 7,
      "period": 10
    },
    "max_newlines": {
      "count": 30,
      "period": 120
    },
    "max_mentions": {
      "count": 10,
      "period": 10
    },
    "max_duplicates": {
      "count": 6,
      "period": 30
    },
    "max_attachments": {
      "count": 5,
      "period": 120
    }
  }
}
```