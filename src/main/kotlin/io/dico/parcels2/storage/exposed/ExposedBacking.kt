@file:Suppress("NOTHING_TO_INLINE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName", "UNUSED_EXPRESSION")

package io.dico.parcels2.storage.exposed

import com.zaxxer.hikari.HikariDataSource
import io.dico.parcels2.*
import io.dico.parcels2.PlayerProfile.Star.name
import io.dico.parcels2.storage.*
import io.dico.parcels2.util.math.clampMax
import io.dico.parcels2.util.ext.synchronized
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ArrayChannel
import kotlinx.coroutines.channels.LinkedListChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.DatabaseDialect
import org.joda.time.DateTime
import java.util.UUID
import javax.sql.DataSource

class ExposedDatabaseException(message: String? = null) : Exception(message)

class ExposedBacking(private val dataSourceFactory: () -> DataSource, val poolSize: Int) : Backing, CoroutineScope {
    override val name get() = "Exposed"
    override val coroutineContext = Job() + newFixedThreadPoolContext(poolSize, "Parcels StorageThread")
    private var dataSource: DataSource? = null
    private var database: Database? = null
    private var isShutdown: Boolean = false
    override val isConnected get() = database != null

    override fun launchJob(job: Backing.() -> Unit): Job = launch { transaction { job() } }
    override fun <T> launchFuture(future: Backing.() -> T): Deferred<T> = async { transaction { future() } }

    override fun <T> openChannel(future: Backing.(SendChannel<T>) -> Unit): ReceiveChannel<T> {
        val channel = LinkedListChannel<T>()
        launchJob { future(channel) }
        return channel
    }

    override fun <T> openChannelForWriting(action: Backing.(T) -> Unit): SendChannel<T> {
        val channel = ArrayChannel<T>(poolSize * 2)

        repeat(poolSize.clampMax(3)) {
            launch {
                try {
                    while (true) {
                        action(channel.receive())
                    }
                } catch (ex: Exception) {
                    // channel closed
                }
            }
        }

        return channel
    }

    private fun <T> transaction(statement: Transaction.() -> T) = transaction(database!!, statement)

    companion object {
        init {
            Database.registerDialect("mariadb") {
                Class.forName("org.jetbrains.exposed.sql.vendors.MysqlDialect").newInstance() as DatabaseDialect
            }
        }
    }

    override fun init() {
        synchronized {
            if (isShutdown || isConnected) throw IllegalStateException()
            dataSource = dataSourceFactory()
            database = Database.connect(dataSource!!)
            transaction(database!!) {
                create(WorldsT, ProfilesT, ParcelsT, ParcelOptionsT, PrivilegesLocalT, PrivilegesGlobalT)
            }
        }
    }

    override fun shutdown() {
        synchronized {
            if (isShutdown) throw IllegalStateException()
            isShutdown = true
            coroutineContext[Job]!!.cancel(CancellationException("ExposedBacking shutdown"))
            dataSource?.let {
                (it as? HikariDataSource)?.close()
            }
            database = null
        }
    }

    @Suppress("RedundantObjectTypeCheck")
    private fun PlayerProfile.toOwnerProfile(): PlayerProfile {
        if (this is PlayerProfile.Star) return PlayerProfile.Fake(name)
        return this
    }

    private fun PlayerProfile.Unresolved.toResolvedProfile(): PlayerProfile.Real {
        return resolve(getPlayerUuidForName(name) ?: throwException())
    }

    private fun PlayerProfile.toResolvedProfile(): PlayerProfile {
        if (this is PlayerProfile.Unresolved) return toResolvedProfile()
        return this
    }

    private fun PlayerProfile.toRealProfile(): PlayerProfile.Real = when (this) {
        is PlayerProfile.Real -> this
        is PlayerProfile.Fake -> throw IllegalArgumentException("Fake profiles are not accepted")
        is PlayerProfile.Unresolved -> toResolvedProfile()
        else -> throw InternalError("Case should not be reached")
    }


    override fun getWorldCreationTime(worldId: ParcelWorldId): DateTime? {
        return WorldsT.getWorldCreationTime(worldId)
    }

    override fun setWorldCreationTime(worldId: ParcelWorldId, time: DateTime) {
        WorldsT.setWorldCreationTime(worldId, time)
    }

    override fun getPlayerUuidForName(name: String): UUID? {
        return ProfilesT.slice(ProfilesT.uuid).select { ProfilesT.name.upperCase() eq name.toUpperCase() }
            .firstOrNull()?.let { it[ProfilesT.uuid]?.toUUID() }
    }

    override fun updatePlayerName(uuid: UUID, name: String) {
        val binaryUuid = uuid.toByteArray()
        ProfilesT.upsert(ProfilesT.uuid) {
            it[ProfilesT.uuid] = binaryUuid
            it[ProfilesT.name] = name
        }
    }

    override fun transmitParcelData(channel: SendChannel<DataPair>, parcels: Sequence<ParcelId>) {
        for (parcel in parcels) {
            val data = readParcelData(parcel)
            channel.offer(parcel to data)
        }
        channel.close()
    }

    override fun transmitAllParcelData(channel: SendChannel<DataPair>) {
        ParcelsT.selectAll().forEach { row ->
            val parcel = ParcelsT.getItem(row) ?: return@forEach
            val data = rowToParcelData(row)
            channel.offer(parcel to data)
        }
        channel.close()
    }

    override fun readParcelData(parcel: ParcelId): ParcelDataHolder? {
        val row = ParcelsT.getRow(parcel) ?: return null
        return rowToParcelData(row)
    }

    override fun getOwnedParcels(user: PlayerProfile): List<ParcelId> {
        val user_id = ProfilesT.getId(user.toOwnerProfile()) ?: return emptyList()
        return ParcelsT.select { ParcelsT.owner_id eq user_id }
            .orderBy(ParcelsT.claim_time, isAsc = true)
            .mapNotNull(ParcelsT::getItem)
            .toList()
    }

    override fun setParcelData(parcel: ParcelId, data: ParcelDataHolder?) {
        if (data == null) {
            transaction {
                ParcelsT.getId(parcel)?.let { id ->
                    ParcelsT.deleteIgnoreWhere { ParcelsT.id eq id }

                    // Below should cascade automatically
                    /*
                    PrivilegesLocalT.deleteIgnoreWhere { PrivilegesLocalT.parcel_id eq id }
                    ParcelOptionsT.deleteIgnoreWhere(limit = 1) { ParcelOptionsT.parcel_id eq id }
                    */
                }

            }
            return
        }

        transaction {
            val id = ParcelsT.getOrInitId(parcel)
            PrivilegesLocalT.deleteIgnoreWhere { PrivilegesLocalT.attach_id eq id }
        }

        setParcelOwner(parcel, data.owner)

        for ((profile, privilege) in data.privilegeMap) {
            PrivilegesLocalT.setPrivilege(parcel, profile, privilege)
        }

        data.privilegeOfStar.takeIf { it != Privilege.DEFAULT }?.let { privilege ->
            PrivilegesLocalT.setPrivilege(parcel, PlayerProfile.Star, privilege)
        }

        setParcelOptionsInteractConfig(parcel, data.interactableConfig)
    }

    override fun setParcelOwner(parcel: ParcelId, owner: PlayerProfile?) {
        val id = if (owner == null)
            ParcelsT.getId(parcel) ?: return
        else
            ParcelsT.getOrInitId(parcel)

        val owner_id = owner?.let { ProfilesT.getOrInitId(it.toOwnerProfile()) }
        val time = owner?.let { DateTime.now() }

        ParcelsT.update({ ParcelsT.id eq id }) {
            it[ParcelsT.owner_id] = owner_id
            it[claim_time] = time
            it[sign_oudated] = false
        }
    }

    override fun setParcelOwnerSignOutdated(parcel: ParcelId, outdated: Boolean) {
        val id = ParcelsT.getId(parcel) ?: return
        ParcelsT.update({ ParcelsT.id eq id }) {
            it[sign_oudated] = outdated
        }
    }

    override fun setLocalPrivilege(parcel: ParcelId, player: PlayerProfile, privilege: Privilege) {
        PrivilegesLocalT.setPrivilege(parcel, player.toRealProfile(), privilege)
    }

    override fun setParcelOptionsInteractConfig(parcel: ParcelId, config: InteractableConfiguration) {
        val bitmaskArray = (config as? BitmaskInteractableConfiguration ?: return).bitmaskArray
        val isAllZero = !bitmaskArray.fold(false) { cur, elem -> cur || elem != 0 }

        if (isAllZero) {
            val id = ParcelsT.getId(parcel) ?: return
            ParcelOptionsT.deleteWhere { ParcelOptionsT.parcel_id eq id }
            return
        }

        if (bitmaskArray.size != 1) throw IllegalArgumentException()
        val array = bitmaskArray.toByteArray()
        val id = ParcelsT.getOrInitId(parcel)
        ParcelOptionsT.upsert(ParcelOptionsT.parcel_id) {
            it[parcel_id] = id
            it[interact_bitmask] = array
        }
    }

    override fun transmitAllGlobalPrivileges(channel: SendChannel<PrivilegePair<PlayerProfile>>) {
        PrivilegesGlobalT.sendAllPrivilegesH(channel)
        channel.close()
    }

    override fun readGlobalPrivileges(owner: PlayerProfile): PrivilegesHolder? {
        return PrivilegesGlobalT.readPrivileges(ProfilesT.getId(owner.toOwnerProfile()) ?: return null)
    }

    override fun setGlobalPrivilege(owner: PlayerProfile, player: PlayerProfile, privilege: Privilege) {
        PrivilegesGlobalT.setPrivilege(owner, player.toRealProfile(), privilege)
    }

    private fun rowToParcelData(row: ResultRow) = ParcelDataHolder().apply {
        owner = row[ParcelsT.owner_id]?.let { ProfilesT.getItem(it) }
        lastClaimTime = row[ParcelsT.claim_time]
        isOwnerSignOutdated = row[ParcelsT.sign_oudated]

        val id = row[ParcelsT.id]
        ParcelOptionsT.select { ParcelOptionsT.parcel_id eq id }.firstOrNull()?.let { optrow ->
            val source = optrow[ParcelOptionsT.interact_bitmask].toIntArray()
            val target = (interactableConfig as? BitmaskInteractableConfiguration ?: return@let).bitmaskArray
            System.arraycopy(source, 0, target, 0, source.size.clampMax(target.size))
        }

        val privileges = PrivilegesLocalT.readPrivileges(id)
        if (privileges != null) {
            copyPrivilegesFrom(privileges)
        }
    }

}

