# Commands

This page handles the management of custom commands on the server.

## Command Discriminator
The sequence of characters that must prefix all commands to be run by KirBot.

Generally, this is set to one character such as ! or &, but it can support multiple characters, such as `kb$`

## Custom Commands
A list of all custom commands that exist on the server. Custom commands are executed the same way as regular commands, so a custom command `testing` would be executed on the server like so: `!testing`.

**NOTE:** Commands listed in the [Command Reference](../commands/commands.md) take precedence over custom commands, so a custom command `play` will not override the Auto DJ's `play` command.

### Adding a Command
Commands can be added by clicking the `Add Command` button at the bottom of the command list. A modal will open which will allow you to configure the command.

**Command Name** - The name of the command

**Respect Whitelist** - If this command should respect the [Command Whitelist](general.md#channel-whitelist)

**Clearance** - The clearance requried to execute this command.