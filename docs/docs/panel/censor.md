# Censor

Much like the [Anti-Spam](spam.md) settings, KirBot offers a granular censor module.

## Rules
Simiar to Anti-Spam, rules are matched with a clearance less than or equal to the given rule.

### Invites
The invite block controls invite related settings

| Property | Description |
| -------- | ----------- |
| enabled | Enable censoring of invites |
| whitelist | A list of invite codes that are whitelisted |
| guild_whitelist | A list of guild ids who are whitelisted |
| blacklist | A list of invite codes that are blacklisted |
| guild_blacklist | A list of guild ids who are blacklisted |

### Domains
The domain block controls domain-related censoring.

| Property | Description |
| -------- | ----------- |
| enabled | Enable censoring of domains |
| whitelist | Domains that are whitelisted |
| blacklist | Domains that are blacklisted |


| Property | Description |
| -------- | ----------- |
| blocked_tokens | A list of tokens (can appear anywhere in the message) that are blocked |
| blocked_words | A list of words (must be separated by a space) that are blocked |


## Example Configuration
An example configuration has been provided for your convenience
```json
{
  "0": {
    "invites": {
      "enabled": true,
      "whitelist": [],
      "guild_whitelist": []
    },
    "domains": {
      "enabled": false,
      "blacklist": []
    },
    "blocked_tokens": [],
    "blocked_words": []
  }
}
```