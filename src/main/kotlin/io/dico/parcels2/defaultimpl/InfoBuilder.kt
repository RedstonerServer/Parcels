package io.dico.parcels2.defaultimpl

import io.dico.dicore.Formatting
import io.dico.parcels2.Privilege
import io.dico.parcels2.PrivilegeKey
import io.dico.parcels2.RawPrivileges
import io.dico.parcels2.filterProfilesWithPrivilegeTo

object InfoBuilder {
    val infoStringColor1 = Formatting.GREEN
    val infoStringColor2 = Formatting.AQUA

    inline fun StringBuilder.appendField(field: StringBuilder.() -> Unit, value: StringBuilder.() -> Unit) {
        append(infoStringColor1)
        field()
        append(": ")
        append(infoStringColor2)
        value()
        append(' ')
    }

    inline fun StringBuilder.appendField(name: String, value: StringBuilder.() -> Unit) {
        appendField({ append(name) }, value)
    }

    inline fun StringBuilder.appendFieldWithCount(name: String, count: Int, value: StringBuilder.() -> Unit) {
        appendField({
            append(name)
            append('(')
            append(infoStringColor2)
            append(count)
            append(infoStringColor1)
            append(')')
        }, value)
    }

    fun StringBuilder.appendProfilesWithPrivilege(fieldName: String, local: RawPrivileges, global: RawPrivileges?, privilege: Privilege) {
        val map = linkedMapOf<PrivilegeKey, Privilege>()
        local.filterProfilesWithPrivilegeTo(map, privilege)
        val localCount = map.size
        global?.filterProfilesWithPrivilegeTo(map, privilege)
        appendPrivilegeProfiles(fieldName, map, localCount)
    }

    fun StringBuilder.appendPrivilegeProfiles(fieldName: String, map: LinkedHashMap<PrivilegeKey, Privilege>, localCount: Int) {
        if (map.isEmpty()) return

        appendFieldWithCount(fieldName, map.size) {
            // first [localCount] entries are local
            val separator = "$infoStringColor1, $infoStringColor2"
            val iterator = map.iterator()

            if (localCount != 0) {
                appendPrivilegeEntry(false, iterator.next().toPair())
                repeat(localCount - 1) {
                    append(separator)
                    appendPrivilegeEntry(false, iterator.next().toPair())
                }

            } else if (iterator.hasNext()) {
                // ensure there is never a leading or trailing separator
                appendPrivilegeEntry(true, iterator.next().toPair())
            }

            iterator.forEach { next ->
                append(separator)
                appendPrivilegeEntry(true, next.toPair())
            }
        }
    }

    fun StringBuilder.appendPrivilegeEntry(global: Boolean, pair: Pair<PrivilegeKey, Privilege>) {
        val (key, priv) = pair

        append(key.notNullName)

        // suffix. Maybe T should be M for mod or something. T means they have CAN_MANAGE privilege.
        append(
            when {
                global && priv == Privilege.CAN_MANAGE -> " (G) (T)"
                global -> " (G)"
                priv == Privilege.CAN_MANAGE -> " (T)"
                else -> ""
            }
        )
    }

}