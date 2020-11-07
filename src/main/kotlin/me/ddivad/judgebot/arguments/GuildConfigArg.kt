package me.ddivad.judgebot.arguments

import me.jakejmattson.discordkt.api.arguments.ArgumentResult
import me.jakejmattson.discordkt.api.arguments.ArgumentType
import me.jakejmattson.discordkt.api.arguments.Success
import me.jakejmattson.discordkt.api.arguments.Error
import me.jakejmattson.discordkt.api.dsl.CommandEvent

val validConfigParameters = mutableListOf(
        "setPrefix",
        "setAdminRole",
        "setStaffRole",
        "setMuteRole",
        "setLogChannel",
        "setAlertChannel",
        "view",
        "options"
)

open class GuildConfigArg(override val name: String = "GuildConfig") : ArgumentType<String>() {
    override fun generateExamples(event: CommandEvent<*>): MutableList<String> = validConfigParameters

    companion object : GuildConfigArg()

    override suspend fun convert(arg: String, args: List<String>, event: CommandEvent<*>): ArgumentResult<String> {
        val parameters = validConfigParameters.map { it.toLowerCase() }
        val parameter = arg.toLowerCase()
        return if (parameters.contains(parameter)) Success(parameter) else Error("$arg is not a valid configuration parameter")
    }
}