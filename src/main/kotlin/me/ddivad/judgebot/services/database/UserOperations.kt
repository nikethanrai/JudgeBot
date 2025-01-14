package me.ddivad.judgebot.services.database

import dev.kord.core.entity.Guild

import me.ddivad.judgebot.dataclasses.GuildMemberDetails
import me.ddivad.judgebot.dataclasses.GuildMember
import me.ddivad.judgebot.dataclasses.Infraction
import dev.kord.core.entity.User
import me.ddivad.judgebot.dataclasses.*
import me.jakejmattson.discordkt.api.annotations.Service
import org.litote.kmongo.eq

@Service
class UserOperations(
    connection: ConnectionService,
    private val configuration: Configuration,
    private val joinLeaveService: JoinLeaveOperations
) {
    private val userCollection = connection.db.getCollection<GuildMember>("Users")

    suspend fun getOrCreateUser(target: User, guild: Guild): GuildMember {
        val userRecord = userCollection.findOne(GuildMember::userId eq target.id.asString)
        return if (userRecord != null) {
            userRecord.ensureGuildDetailsPresent(guild.id.asString)
            userRecord.checkPointDecay(guild, configuration[guild.id.value]!!)
            target.asMemberOrNull(guild.id)?.let {
                joinLeaveService.createJoinLeaveRecordIfNotRecorded(guild.id.asString, it)
            }
            userRecord
        } else {
            val guildMember = GuildMember(target.id.asString)
            guildMember.guilds.add(GuildMemberDetails(guild.id.asString))
            userCollection.insertOne(guildMember)
            guildMember
        }
    }

    suspend fun addNote(guild: Guild, user: GuildMember, note: String, moderator: String): GuildMember {
        user.addNote(note, moderator, guild)
        return this.updateUser(user)
    }

    suspend fun editNote(
        guild: Guild,
        user: GuildMember,
        noteId: Int,
        newContent: String,
        moderator: String
    ): GuildMember {
        user.editNote(guild, noteId, newContent, moderator)
        return this.updateUser(user)
    }

    suspend fun deleteNote(guild: Guild, user: GuildMember, noteId: Int): GuildMember {
        user.deleteNote(noteId, guild)
        return this.updateUser(user)
    }

    suspend fun addInfo(guild: Guild, user: GuildMember, information: Info): GuildMember {
        user.addInfo(information, guild)
        return this.updateUser(user)
    }

    suspend fun removeInfo(guild: Guild, user: GuildMember, noteId: Int): GuildMember {
        user.removeInfo(noteId, guild)
        return this.updateUser(user)
    }

    suspend fun addLinkedAccount(guild: Guild, user: GuildMember, userId: String): GuildMember {
        user.addLinkedAccount(guild, userId)
        return this.updateUser(user)
    }

    suspend fun removeLinkedAccount(guild: Guild, user: GuildMember, userId: String): GuildMember {
        user.removeLinkedAccount(guild, userId)
        return this.updateUser(user)
    }

    suspend fun cleanseNotes(guild: Guild, user: GuildMember): GuildMember {
        user.cleanseNotes(guild)
        return this.updateUser(user)
    }

    suspend fun addInfraction(guild: Guild, user: GuildMember, infraction: Infraction): Infraction {
        user.addInfraction(infraction, guild)
        infraction.punishment = getPunishmentForPoints(guild, user)
        user.updatePointDecayDate(guild, infraction.punishment?.duration ?: 0)
        this.updateUser(user)
        return infraction
    }

    suspend fun addMessageDelete(guild: Guild, user: GuildMember, deleteReaction: Boolean): GuildMember {
        user.addMessageDeleted(guild, deleteReaction)
        return this.updateUser(user)
    }

    suspend fun cleanseInfractions(guild: Guild, user: GuildMember): GuildMember {
        user.cleanseInfractions(guild)
        return this.updateUser(user)
    }

    suspend fun removeInfraction(guild: Guild, user: GuildMember, infractionId: Int): GuildMember {
        user.deleteInfraction(guild, infractionId)
        return this.updateUser(user)
    }

    suspend fun incrementUserHistory(user: GuildMember, guild: Guild): GuildMember {
        user.incrementHistoryCount(guild.id.asString)
        return this.updateUser(user)
    }

    suspend fun resetUserRecord(guild: Guild, user: GuildMember): GuildMember {
        user.reset(guild)
        return this.updateUser(user)
    }

    private suspend fun updateUser(user: GuildMember): GuildMember {
        userCollection.updateOne(GuildMember::userId eq user.userId, user)
        return user
    }

    private fun getPunishmentForPoints(guild: Guild, guildMember: GuildMember): PunishmentLevel {
        val punishmentLevels = configuration[guild.id.value]?.punishments
        return punishmentLevels!!.filter {
            it.points <= guildMember.getGuildInfo(guild.id.asString).points
        }.maxByOrNull { it.points }!!
    }
}