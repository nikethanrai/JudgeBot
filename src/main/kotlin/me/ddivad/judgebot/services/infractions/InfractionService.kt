package me.ddivad.judgebot.services.infractions

import dev.kord.common.exception.RequestException
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.Job
import me.ddivad.judgebot.dataclasses.*
import me.ddivad.judgebot.embeds.createInfractionEmbed
import me.ddivad.judgebot.services.DatabaseService
import me.ddivad.judgebot.services.LoggingService
import me.jakejmattson.discordkt.api.annotations.Service
import me.jakejmattson.discordkt.api.extensions.sendPrivateMessage
import org.joda.time.DateTime

@Service
class InfractionService(private val configuration: Configuration,
                        private val databaseService: DatabaseService,
                        private val loggingService: LoggingService,
                        private val banService: BanService,
                        private val muteService: MuteService) {

    private val punishmentTimerMap = hashMapOf<Pair<Int, GuildID>, Job>()
    private fun toKey(punishment: Punishment, guild: Guild) = punishment.id to guild.id.value

    suspend fun infract(target: Member, guild: Guild, userRecord: GuildMember, infraction: Infraction): Infraction {
        var rule: Rule? = null
        if (infraction.ruleNumber != null) {
            rule = databaseService.guilds.getRule(guild, infraction.ruleNumber)
        }
        return databaseService.users.addInfraction(guild, userRecord, infraction).also {
            try {
                target.asUser().sendPrivateMessage {
                    createInfractionEmbed(guild, configuration[guild.id.value]!!, target, userRecord, it, rule)
                }
            } catch (ex: RequestException) {
                loggingService.dmDisabled(guild, target.asUser())
            }
            loggingService.infractionApplied(guild, target.asUser(), it)
            applyPunishment(guild, target, it)
        }
    }

    private suspend fun applyPunishment(guild: Guild, target: Member, infraction: Infraction) {
        when (infraction.punishment?.punishment) {
            PunishmentType.NONE -> return
            PunishmentType.MUTE -> muteService.applyInfractionMute(target, infraction.punishment?.duration!!, "Infraction mute. Please check corresponding infraction embed above.")
            PunishmentType.BAN -> {
                val clearTime = infraction.punishment!!.duration?.let { DateTime().millis.plus(it) }
                val punishment = Punishment(target.id.asString, InfractionType.Ban, infraction.reason, infraction.moderator, clearTime)
                banService.banUser(target, guild, punishment)
            }
        }
    }
}
