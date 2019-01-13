# Custom Commands

Configure command-related settings here.

## Command Prefix
Sets the command prefix. All commands on the server will then use the prefix. The bot also will always respond to commands executed via mention (i.e `@KirBot help`). If at any point, you forget the prefix, just mention KirBot without any command and it will reply with its currently configured prefix.

**Note:** Leading and trailing whitespace will be removed in the prefix.

### Silent Fail
By default, KirBot sends a message alerting when a user attempts to run a command they don't have permission to run. By enabling this toggle, that message will be suppressed and commands will fail silently if the user doesn't have permission to perform the command

## Custom Commands
KirBot supports custom commands and these can be configured using the table below.

### Creating a Command
To create a command, click the `New Command` button to bring up the new command dialog.

**Command Name** - The name of the command

**Respect Whitelist** - If the command should respect the [command whitelist](general.md#channel-whitelist)

**Command Response** - The response that will be sent when the command is executed.

**Clearance** - The required clearance to execute the command

## Command Aliases
Alias commands and override their clearance.

### Aliasing Commands
To alias a command to another, enter the new command in the **Command** box and the command it aliases to in the **Alias** box. If you wish to override its clearance, enter the clearance, or leave it set to `-1` to inherit the clearance of the aliased command

### Overriding Clearance
To override the clearance of a command, enter the command in the **Command** box followed by the **Clearance**. Leave the **Alias** box blank.

To override the default clearance for all commands, enter `*` in the **Command** box.