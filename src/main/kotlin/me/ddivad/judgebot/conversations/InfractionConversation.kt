package me.ddivad.judgebot.conversations

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import me.ddivad.judgebot.dataclasses.*
import me.ddivad.judgebot.embeds.createHistoryEmbed
import me.ddivad.judgebot.embeds.createInfractionRuleEmbed
import me.ddivad.judgebot.services.DatabaseService
import me.ddivad.judgebot.services.infractions.InfractionService
import me.jakejmattson.discordkt.api.conversations.conversation

@KordPreview
class InfractionConversation(
    private val databaseService: DatabaseService,
    private val configuration: Configuration,
    private val infractionService: InfractionService
) {
    fun createInfractionConversation(
        guild: Guild,
        targetUser: Member,
        weight: Int,
        infractionReason: String,
        type: InfractionType
    ) = conversation("cancel") {
        val guildConfiguration = configuration[guild.id.value] ?: return@conversation
        val user = databaseService.users.getOrCreateUser(targetUser, guild)
        val points = weight *
                if (type == InfractionType.Strike) guildConfiguration.infractionConfiguration.strikePoints
                else guildConfiguration.infractionConfiguration.warnPoints
        val rules = databaseService.guilds.getRules(guild)
        val ruleId = if (rules.isNotEmpty()) {

            val rule = promptButton<Int> {
                embed {
                    createInfractionRuleEmbed(guild, rules)
                }
                rules.chunked(5).forEach { list ->
                    buttons {
                        list.forEach { rule ->
                            button("${rule.number}", null, rule.number, ButtonStyle.Primary)
                        }
                    }
                }
                buttons {
                    button("Infract without rule", null, 0, ButtonStyle.Danger)
                }
            }

            if (rule > 0) rule else null
        } else null
        val infraction = Infraction(this.user.id.asString, infractionReason, type, points, ruleId)
        infractionService.infract(targetUser, guild, user, infraction)
        respondMenu { createHistoryEmbed(targetUser, user, guild, configuration, databaseService) }
    }
}
