package me.ddivad.judgebot.services.infractions

import dev.kord.common.exception.RequestException
import dev.kord.core.behavior.ban
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ddivad.judgebot.embeds.createBadPfpEmbed
import me.ddivad.judgebot.services.LoggingService
import me.jakejmattson.discordkt.api.Discord
import me.jakejmattson.discordkt.api.annotations.Service
import me.jakejmattson.discordkt.api.extensions.sendPrivateMessage

@Service
class BadPfpService(private val muteService: MuteService,
                    private val discord: Discord,
                    private val loggingService: LoggingService) {
    private val badPfpTracker = hashMapOf<Pair<GuildID, UserId>, Job>()
    private suspend fun toKey(member: Member): Pair<GuildID, UserId> = member.guild.id.asString to member.asUser().id.asString

    suspend fun applyBadPfp(target: Member, guild: Guild, timeLimit: Long) {
        try {
            target.sendPrivateMessage {
                createBadPfpEmbed(guild, target)
            }
        } catch (ex: RequestException) {
            loggingService.dmDisabled(guild, target.asUser())
        }
        muteService.applyMuteAndSendReason(target, timeLimit, "Mute for BadPfp.")
        loggingService.badBfpApplied(guild, target)
        badPfpTracker[toKey((target))] = GlobalScope.launch {
            delay(timeLimit)
            if (target.avatar == discord.kord.getUser(target.id)?.avatar) {
                GlobalScope.launch {
                    delay(1000)
                    guild.ban(target.id) {
                        reason = "BadPfp - Having a bad pfp and refusing to change it."
                        deleteMessagesDays = 1
                    }
                    loggingService.badPfpBan(guild, target)
                }
            } else {
                target.asUser().sendPrivateMessage("Thanks for changing you avatar. You will not be banned.")
            }
        }
    }

    suspend fun hasActiveBapPfp(target: Member): Boolean {
        return badPfpTracker.containsKey(toKey(target))
    }

    suspend fun cancelBadPfp(guild: Guild, target: Member) {
        val key = toKey(target)
        if (hasActiveBapPfp(target)) {
            badPfpTracker[key]?.cancel()
            badPfpTracker.remove(key)
            loggingService.badPfpCancelled(guild, target)
            muteService.removeMute(guild, target.asUser())
        }
    }
}