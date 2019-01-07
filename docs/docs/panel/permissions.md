# Permissions

The permissions panel controls permissions related to the panel itself as well as role and clearance mapping.

## Panel Permissions
Configure access permissions to the panel here.

There are three levels of permissions: `View`, `Edit`, and `Admin`.
* **View** - The user can only view the panel
* **Edit** - The user can modify settings on the panel, but cannot add new users
* **Admin** - The user has full control over the panel, and can add new users.

Note: The server owner always has `Admin` permissions regardless of what is configured.

To add a user's permissions, click the green `Add` button and fill in the user's ID (For information regarding retrieving user ids, see [this](https://support.discordapp.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) support article).

To modify a user's permissions, select their new permission in the dropdown. Changes will be applied immediately and users cannot change their own permission.

## Role Permissions
Configure role and clearance mappings.

To map a clearance value to a role, click the `Add` button, select the role, then enter its clearance (0 to 100). After clicking save, the role's clearance will be reflected immediately. For more information about clearance see [Clearance](../clearance.md)