# Parcels Todo list

Commands
-
Basically all admin commands.
* ~~setowner~~
* ~~dispose~~
* ~~reset~~
* swap
* New admin commands that I can't think of right now.

Also
* ~~setbiome~~
* random

Modify home command:
* ~~Make `:` not be required if prior component cannot be parsed to an int~~
* Listen for command events that use plotme-style argument, and transform the command

~~Add permissions to commands (replace or fix `IContextFilter` from command lib
to allow inheriting permissions properly).~~

Parcel Options
-

Parcel options apply to any player with `DEFAULT` added status. 
They affect what their permissions might be within the parcel.

Apart from `/p option inputs`, `/p option inventory`, the following might be considered. 

Move existing options to "interact" namespace (`/p o interact`)

Then,
* Split `/p option interact inputs` into a list of interactible block types. 
The list could include container blocks, merging the existing inventory option.
* Players cannot launch projectiles in locations where they can't build. 
This could become optional.
* Option to control spreading and/or forming of blocks such as grass and ice within the parcel. 

Block Management
- 
~~Update the parcel corner with owner info when a player flies into the parcel (after migrations).
Parcels has a player head in that corner in addition to the sign that PlotMe uses.~~

Commands that modify parcel blocks must be kept track of to prevent multiple
from running simultaneously in the same parcel. `hasBlockVisitors` field must be updated.
In general, spamming the commands must be caught at all cost to avoid lots of lag.

Swap - schematic is in place, but proper placement order must be enforced to make sure that attachable 
blocks are placed properly. Alternatively, if a block change method can be found that doesn't
cause block updates, that would be preferred subject to having good performance.

~~Change `RegionTraversal` to allow traversing different parts of a region in a different order.
This could apply to clearing of plots, for example. It would be better if the bottom 64 (floor height) 
layers are done upwards, and the rest downwards.~~

Events
-
Prevent block spreading subject to conditions.

Scan through blocks that were added since original Parcels implementation,
that might introduce things that need to be checked or listened for.

~~WorldEdit Listener.~~

Limit number of beacons in a parcel and/or avoid potion effects being applied outside the parcel.

Database
-
Find and patch ways to add new useless entries (for regular players at least)

Prevent invalid player names from being saved to the database. 
Here, invalid player names mean names that contain invalid characters.

Use an atomic GET OR INSERT query so that parallel execution doesn't cause problems
(as is currently the case when migrating).

Implement a container that doesn't require loading all parcel data on startup (Complex).

~~Update player profiles in the database on join to account for name changes.~~

Store player status on parcel (allowed, default banned) as a number to allow for future additions to this set of possibilities


