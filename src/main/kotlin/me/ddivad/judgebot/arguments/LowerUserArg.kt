package me.ddivad.judgebot.arguments

import dev.kord.core.entity.User
import me.jakejmattson.discordkt.api.arguments.*
import me.jakejmattson.discordkt.api.commands.CommandEvent
import me.jakejmattson.discordkt.api.extensions.isSelf
import me.jakejmattson.discordkt.api.extensions.toSnowflakeOrNull

open class LowerUserArg(override val name: String = "LowerUserArg") : Argument<User> {
    companion object : LowerUserArg()

    override val description = "A user with a lower rank"

    override suspend fun generateExamples(event: CommandEvent<*>) = mutableListOf("@User", "197780697866305536", "302134543639511050")

    override suspend fun convert(arg: String, args: List<String>, event: CommandEvent<*>): ArgumentResult<User> {
        val guild = event.guild ?: return Error("No guild found")

        val user = arg.toSnowflakeOrNull()?.let { guild.kord.getUser(it) } ?: return Error("User Not Found")
        val member = guild.getMemberOrNull(user.id) ?: return Success(user)
        val author = event.author.asMember(event.guild!!.id)

        return when {
            event.discord.permissions.isHigherLevel(event.discord, member, author) || event.author.isSelf() ->
                Error("You don't have the permission to use this command on the target user.")
            event.author == member -> Error("You can't use this command on yourself!")
            else -> Success(member.asUser())
        }
    }

    override fun formatData(data: User) = "@${data.tag}"
}