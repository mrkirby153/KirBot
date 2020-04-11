# Censor

KirBot has a highly modular and effective censoring system which can be used to censor invites,
websites, words, and nicknames.

All censor rules have to be configured on the admin panel. See [here](../panel/censor.md) for
configuration instructions.

## Censor Levels

Each censor rule has a level associated with it. This level determines which users the rule will apply
to. Censor rules whose level is less than or equal to the user's clearance will be applied to messages
that users send as well as messages that they edit.

## Invite Censor

The invite censor has two modes: whitelist and blacklist. If the whitelist is left blank and guilds
are specified in the blacklist, all invites except those to the servers on the blacklist will be allowed.
If the blacklist is empty and whitelisted guilds are specified, all invites to servers not on the
whitelist will be deleted.

**Note:** The invite censor switch must be enabled for invites to be censored.

## Domain Censor

In addition to blocking invites, domains can also be blocked. Domains follow the same rules
as invite censors and are configured much the same. 

**Note:** The domain censor switch must be enabled for invites to be censored.

## Word Blacklist

The word blacklist has two configurations: tokens and words. Strings on the token blacklist can appear
anywhere in the message (including inside other words). For example, the token `ape` will match postive
against the word `grape`. Strings in the word blacklist must be delimited on either side by spaces, newlines,
or the start/end of the message.

### Regular Expressions

For more advanced configuration, regular expressions can be used to create advanced censor rules. To
use a regular expression, prefix the token or word with `r:`. The same rules apply for words and tokens
when using regular expressions.

### Name Blacklist

Additionally, specific phrases can be blocked from display names (both usernames and nicknames).
If the user has a nickname set, their nickname will be subject to the tokens in the nickname blacklist.
However, if the user does not have a nickname set, their username will be subject to the nickname
blacklist.

If a user's display name matches any token on the blacklist, they will be nicknamed to
`Censored Nickname XXXXX` where `X` is a random alphanumeric character.

## Zalgo

When the zalgo switch is enabled, most zalgo characters will cause the message to be censored.