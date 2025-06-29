# ModerationPlus

A Paper plugin providing enhanced staff moderation commands.

## Commands

### `/ban`<br>`/ban <name> [<duration>] [<reason>]`

Ban a player with optional duration and/or reason.

* If the second argument matches a duration pattern (`10s`, `10m`, `10h`, `10d`, `10w`, `10y`), it is treated as the length of the ban.
* Otherwise, all text after the player name is treated as the reason.

### `/unban` (alias: `/pardon`)<br>`/unban <name>`

Lift a ban on the specified player.

### `/jail`<br>`/jail <name> [<duration>] [<reason>]`

Jail a player with optional duration and/or reason. Behaves like `/ban` for parsing the duration and reason.

### `/unjail`<br>`/unjail <name>`

Release a player from jail and teleport them to spawn.

### `/kick`<br>`/kick <name> [<reason>]`

Kick a player with an optional reason.

### `/mute`<br>`/mute <name> [<duration>] [<reason>]`

Mute a player in chat with optional duration and/or reason. Duration parsing follows the same rules as `/ban`.

### `/unmute`<br>`/unmute <name>`

Remove a chat mute from the specified player.

### `/warn`<br>`/warn <name> [<reason>]`

Warn a player with an optional reason. Reason text is displayed to the player and logged.

### `/actions` (alias: `/logs`)<br>`/actions <name>`

View the last 5 moderation actions performed on the specified player.

---

**Note:** All actions are recorded in the moderation log and can be retrieved with the `/actions <name>` command. Make sure each command’s permission node (`moderationplus.<command>`) is granted to the appropriate staff roles.
