# SimpleModerationPlus

## Why choose SimpleModerationPlus?
SimpleModerationPlus:
* Keeps logs of all moderator actions
* Has a simple votekick system
* Supports tab complete
* Allows use of vanilla player selectors (like @a)
* Has fully customizable messages
* Supports translations
* Lets moderators be exempt from commands
* Allows bypass of exempt
* Prevents you from kicking yourself
* Doesn't require any dependencies
* Is fully vanilla styled
* Is a small, manageable plugin
* Uses the most modern APIs

## Dependencies
All dependencies are soft dependencies, meaning the plugin will load without them but lack some features

* **Vault:** Removes requirement to be online to be `simplemoderationplus.exempt` (requires permissions backend)
* **WorldGuard:** Allows admins to define a region named `jail` that jailed players cannot leave

## Version
* **Requires Paper server**, the plugin will not load on Spigot or Bukkit
* **1.21.6 or later**, the plugin will not load on an earlier version

## Commands
### Votekick command
`/vk <player> <reason>`
Start a vote to kick player with reason
### Ban commands
`/ban <player> [<duration>|<reason>]`
Ban player(s) with optional reason and duration

`/tempban <player> <duration> [<reason>]`
Ban player(s) with duration and optional reason

`/permban <player> [<reason>]`
Ban player(s) permanently with optional reason

`/unban <player> [<reason>]`
Unban player(s) with optional reason

### Mute commands
`/mute <player> [<duration>|<reason>]`
Mute player(s) with optional reason and duration

`/tempmute <player> <duration> [<reason>]`
Mute player(s) with duration and optional reason

`/permmute <player> [<reason>]`
Mute player(s) permanently with optional reason

`/unmute <player> [<reason>]`
Unmute player(s) with optional reason

### Jail commands
`/setjail`
Set the location of the jail

`/jail <player> [<duration>|<reason>]`
Jail player(s) with optional reason and duration

`/tempjail <player> <duration> [<reason>]`
Jail player(s) with duration and optional reason

`/permjail <player> [<reason>]`
Jail player(s) permanently with optional reason

`/unjail <player> [<reason>]`
Unjail player(s) with optional reason

### Kick command
`/kick <player> [<reason>]`
Kick player(s) with optional reason

### Warn command
`/warn <player> [<reason>]`
Warn player(s) with optional reason

### Actions command
`/actions <player>`
View the moderator actions taken against player(s)

## Permissions
All permissions are granted by default with operator
(or with `minecraft.command.ban`... etc)

`simplemoderationplus.votekick`
Allowed to start a votekick

`simplemoderationplus.votekick.vote`
Allowed to vote in a votekick

`simplemoderationplus.votekick.cancel`
Allowed to cancel any votekick

`simplemoderationplus.ban`
Allowed to ban players

`simplemoderationplus.unban`
Allowed to unban players

`simplemoderationplus.mute`
Allowed to mute players

`simplemoderationplus.unmute`
Allowed to unmute players

`simplemoderationplus.warn`
Allowed to warn players

`simplemoderationplus.kick`
Allowed to kick players

`simplemoderationplus.jail`
Allowed to jail players

`simplemoderationplus.unjail`
Allowed to unjail players

`simplemoderationplus.setjail`
Allowed to set the location of the jail

`simplemoderationplus.actions`
Allowed to view actions regarding players

`simplemoderationplus.exempt`
Exempt from moderation

`simplemoderationplus.bypassexempt`
Bypass exempt
