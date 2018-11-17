package io.dico.parcels2.blockvisitor

import io.dico.parcels2.util.math.Vec3d
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Minecart

/*
open class EntityCopy<T : Entity>(entity: T) {
    val type = entity.type

    @Suppress("UNCHECKED_CAST")
    fun spawn(world: World, position: Vec3d): T {
        val entity = world.spawnEntity(Location(null, position.x, position.y, position.z), type) as T
        setAttributes(entity)
        return entity
    }

    open fun setAttributes(entity: T) {}
}

open class MinecartCopy<T : Minecart>(entity: T) : EntityCopy<T>(entity) {
    val damage = entity.damage
    val maxSpeed = entity.maxSpeed
    val isSlowWhenEmpty = entity.isSlowWhenEmpty
    val flyingVelocityMod = entity.flyingVelocityMod
    val derailedVelocityMod = entity.derailedVelocityMod
    val displayBlockData = entity.displayBlockData
    val displayBlockOffset = entity.displayBlockOffset

    override fun setAttributes(entity: T) {
        super.setAttributes(entity)
        entity.damage = damage
        entity.displayBlockData = displayBlockData
        entity.displayBlockOffset = displayBlockOffset
    }
}*/