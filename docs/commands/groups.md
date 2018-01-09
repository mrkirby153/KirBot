# Groups
The group system enhances Discord's current role system by creating open roles that anyone on the server can join, no need to have someone with "Manage Roles" assign the role for you. An example use case of this system is splitting channels up by topic. On servers with a large number of channels, the sidebar can become quite cluttered. By restricting channels to groups, the sidebar can be cleaned up.

## Creating groups
To create a group, you must be a Bot Manager. Groups are created with the `!creaateGroup` command. This command will create a role with the group's name, and register it with the grouping system.

Coming in the near future: importing groups by their roles

## Joining Groups
Anyone can join groups. To join a group, use the `!joinGroup` command. After joining the group, you will be given the group's role. Also, in the event your role gets removed, it will be added back automatically.

## Leaving Groups
In the event one doesn't wish to be a member of a group, they can leave the group with `!leaveGroup`. This will immediately remove the role. **Note:** Removing the role is _not_ enough to remove a user from the group.

## Deleting Groups
In the event a group is no longer required, Bot Managers can delete the group and its associated role with `!deleteGroup`.
