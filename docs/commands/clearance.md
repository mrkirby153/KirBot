# Clearance
Clearance is KirBot's permission heirarchy. This heirarchy is linear and a user's effective clearance is their highest level. For example, an action requiring `BOT_MANAGER` will be executable by those with `SERVER_ADMINISTRATOR`.

The permission heirarchy is as follows:
```
- BOT_OWNER
- SERVER_OWNER
- SERVER_ADMINISTRATOR
- BOT_MANAGER
- USER
- BOT
```
The criteria for determining clearance is as follows:

#### Bot Owner (BOT_OWNER)
Users listed in the `admins` file of KirBot's configuration.
#### Server Owner (SERVER_OWNER)
The user that is the owner of the server.
#### Server Administrator (SERVER_ADMINISTRATOR)
Users with the `Administrator` permission.
#### Bot Manager/Moderator (BOT_MANAGER)
Users with a role listed in the `Bot Manager` section on the web configuration panel.
#### Users (USER)
Any user that does not meet any of the above criteria and is not another bot.
#### Bot (BOT)
Any user that does not meet any of the above criteria.