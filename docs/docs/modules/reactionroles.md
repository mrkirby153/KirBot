# Reaction Roles

To aid in the self-assignment of roles, KirBot can assign server members roles based off of reactions
that they can click.

When users react to a message that is configured with a reaction role, they will be given the role
configured. If they unreact from the message, that role will be taken from them.

## Configuration

To view a list of all reaction roles currently configured on the server, use the `!reaction-role list`
 command. This returns a list of all configured reaction roles on the server, including their message
ID, role, and channel.

### Adding a Role

To add a reaction role to a message, use the `!reaction-role add <message id> <emoji> <role>` command.
The bot must have access to the emoji that it is using for the reaction role, and will warn you if
it does not have the ability to use it. 

The message id given can be either a message id or a jump link. A jump link is preferred, but not
necessary.

The bot will then react to the message with the provided emote and will start giving/taking roles
based off of user's reactions.

### Removing a Role

To remove a role and have KirBot stop giving/taking it, get the ID of the reaction role using the
`!reaction-role list` command. The reaction role can then be removed with `!reaction-role remove <id>`.

**Note:** This will not remove the role from users that already have the role. It will only stop the
bot from giving/taking roles based off new reactions.

## Commands
* `!reaction-role list` -- Prints a list of reaction roles configured on the server
* `!reaction-role add` -- Configures a new reaction role
* `!reaction-role remove` -- Removes a reaction role that has been previously configured