package me.ddivad.judgebot.commands

import me.ddivad.judgebot.conversations.InfractionConveration
import me.ddivad.judgebot.dataclasses.Configuration
import me.ddivad.judgebot.dataclasses.Infraction
import me.ddivad.judgebot.dataclasses.InfractionType
import me.ddivad.judgebot.embeds.createHistoryEmbed
import me.ddivad.judgebot.services.*
import me.ddivad.judgebot.services.infractions.InfractionService
import me.jakejmattson.discordkt.api.arguments.EveryArg
import me.jakejmattson.discordkt.api.arguments.IntegerArg
import me.jakejmattson.discordkt.api.arguments.MemberArg
import me.jakejmattson.discordkt.api.dsl.commands
import me.jakejmattson.discordkt.api.services.ConversationService

fun createInfractonCommands(databaseService: DatabaseService,
                            config: Configuration,
                            infractionService: InfractionService,
                            conversationService: ConversationService) = commands("Infraction") {
    command("strike", "s") {
        description = "Strike a user."
        requiresGuild = true
        requiredPermissionLevel = PermissionLevel.Staff
        execute(MemberArg, IntegerArg.makeOptional(1), EveryArg) {
            val (user, weight, reason) = args
            conversationService.startPublicConversation<InfractionConveration>(author, channel.asChannel(), guild!!, user, weight, reason)
        }
    }

    command("warn", "w") {
        description = "Warn a user."
        requiresGuild = true
        requiredPermissionLevel = PermissionLevel.Staff
        execute(MemberArg, EveryArg) {
            val guildConfiguration = config[guild!!.id.longValue] ?: return@execute
            val user = databaseService.users.getOrCreateUser(args.first, guild!!.id.value)
            val infraction = Infraction(this.author.id.value, args.second, InfractionType.Warn, guildConfiguration.infractionConfiguration.warnPoints)
            infractionService.infract(args.first, guild!!, user, infraction)
            respondMenu {
                createHistoryEmbed(args.first, user, guild!!, config, true)
            }
        }
    }

    command("cleanse") {
        description = "Use this to delete (permanently) as user's infractions."
        requiresGuild = true
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(MemberArg) {
            val user = databaseService.users.getOrCreateUser(args.first, guild!!.id.value)
            if (user.getGuildInfo(guild!!.id.value)!!.infractions.isEmpty()) return@execute respond("User has no infractions.")
            databaseService.users.cleanseInfractions(guild!!, user)
            respond("Infractions cleansed.")
        }
    }
}