package me.mrkirby153.KirBot.command.control

import com.mrkirby153.bfs.query.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.resolveMentions
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.Hashtable
import java.util.Random
import java.util.Vector
import javax.inject.Inject
import kotlin.system.measureTimeMillis


class MarkovCommand @Inject constructor(private val shardManager: ShardManager){

    private val chains = mutableMapOf<String, MarkovChain>()

    @Command(name = "markov", arguments = ["<user:snowflake>", "[amount:int]"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val userChain = chains[cmdContext.get<String>("user")!!] ?: throw CommandException(
                "No chain found. Did you generate one?")
        val user = shardManager.getUserById(cmdContext.get<String>("user")!!)?.nameAndDiscrim
                ?: cmdContext.get<String>("user")!!
        val chain = StringBuilder()
        for (i in 0..(cmdContext.get<Int>("amount") ?: 1)) {
            chain.append(userChain.makeSentence(maxDepth = 50))
        }
        context.channel.sendMessage("```$chain\n - $user```").queue()
    }

    @Command(name = "generate", arguments = ["<user:snowflake>", "[limit:int]"], parent = "markov")
    @AdminCommand
    fun generateChain(context: Context, cmdContext: CommandContext) {
        val limit = cmdContext.get<Int>("limit") ?: -1
        val msg = context.channel.sendMessage(
                "Generating chain with " + (if (limit == -1) "all" else limit) + " messages").complete()
        Bot.scheduler.submit {
            var count = 0
            val time = measureTimeMillis {
                val params = mutableListOf<Any>(cmdContext.get<String>("user")!!)
                if (limit != -1)
                    params.add(limit)
                val encrypted = DB.getFirstColumnValues<String>(
                        "SELECT `message` FROM server_messages WHERE author = ?" + (if (limit != -1) " LIMIT ?" else ""),
                        *params.toTypedArray())
                val chain = MarkovChain()
                encrypted.forEach {
                    val decrypted = LogManager.decrypt(it)
                    val resolved = decrypted.resolveMentions()
                    resolved.split(Regex("[.?!]")).forEach { sentence ->
                        count++
                        chain.addWords(sentence.trim() + ".")
                    }
                }
                this.chains[cmdContext.get<String>("user")!!] = chain
            }
            msg.editMessage("Done in ${Time.formatLong(time = time, short = true,
                    smallest = Time.TimeUnit.MILLISECONDS)}. With $count messages").queue()
        }
    }

    @Command(name = "delete", arguments = ["<user:snowflake>"], parent = "markov")
    @AdminCommand
    fun deleteChain(context: Context, cmdContext: CommandContext) {
        chains.remove(cmdContext.get<String>("user")!!)
        context.success()
    }


    private class MarkovChain {
        val chain: Hashtable<String, Vector<String>> = Hashtable()
        val random = Random()
        val endChars = arrayOf('.', '!', '?')

        init {
            chain["_start"] = Vector()
            chain["_end"] = Vector()
        }

        fun addWords(phrase: String) {
            val words = phrase.split(" ")
            if (words.size < 2)
                return
            for (i in words.indices) {
                if (i == 0) {
                    val startWords = this.chain["_start"]!!
                    startWords.add(words[i])

                    var suffix = this.chain[words[i]]
                    if (suffix == null) {
                        suffix = Vector()
                        suffix.add(words[i + 1])
                        this.chain[words[i]] = suffix
                    }
                } else if (i == words.size - 1) {
                    this.chain["_end"]?.add(words[i])
                } else {
                    var suffix = this.chain[words[i]]
                    if (suffix == null) {
                        suffix = Vector()
                    }
                    suffix.add(words[i + 1])
                    this.chain[words[i]] = suffix
                }
            }
        }

        tailrec fun makeSentence(builder: StringBuilder = StringBuilder(),
                                 nextWord: String = "$$$$", currDepth: Int = 0,
                                 maxDepth: Int = 1000): String {
            val next = if (nextWord == "$$$$") {
                this.chain["_start"]!!.elementAt(random.nextInt(this.chain["_start"]!!.size))
            } else {
                nextWord
            }
            builder.append("$next ")
            return if (currDepth >= maxDepth || endChars.contains(next[next.length - 1])) {
                builder.toString()
            } else {
                val selection = this.chain[next] ?: return builder.toString()
                val n = selection.elementAt(random.nextInt(selection.size))
                makeSentence(builder, n, currDepth + 1, maxDepth)
            }
        }
    }
}