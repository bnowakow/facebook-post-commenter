import com.restfb.Connection
import com.restfb.FacebookClient
import com.restfb.Parameter
import facebook4j.*
import facebook4j.internal.org.json.JSONObject
import mu.KotlinLogging
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

class FacebookReplies(private val facebook: Facebook, private val restfbClient: FacebookClient) {

    var commentedPosts = 0
    private val logger = KotlinLogging.logger {}

    private fun isCommentWrittenByOneOfAdmins(comment: com.restfb.types.Comment): Boolean {
        return comment.from?.id == "105161449087504" // Kuba
    }

    private fun isCommentRepliedByOneOfAdmins(comment: com.restfb.types.Comment): Boolean {

        val commentsOfComment: ArrayList<com.restfb.types.Comment> = fetchAllComments(comment.id)

        for (commentOfComment in commentsOfComment) {
            logger.trace("\t\t\t\twhich is commented by ${commentOfComment.from?.name}: ${commentOfComment.message.replace("\n", "")}")
            if (isCommentWrittenByOneOfAdmins(commentOfComment)) {
                return true
            }
        }
        return false
    }

    companion object {

        private fun shortenUrl(longUrl: String): String {
            val client: HttpClient = HttpClient.newHttpClient()

            val postBody = HttpRequest.BodyPublishers.ofString("url=" + URLEncoder.encode(longUrl, StandardCharsets.UTF_8))

            val request: HttpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://cleanuri.com/api/v1/shorten"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(postBody)
                .build()

            val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

            return if (response.statusCode() == 200) {

                JSONObject(response.body()).getString("result_url")
            } else {
                // TODO fail flow
//                logger.error("couldn't shorten url: $longUrl")
                longUrl
            }
        }

        fun randomizeThankYouReply(randomizeUrls: Boolean = true): String {
            val reply: StringBuilder = StringBuilder()
            reply.append(listOf("", "Bardzo Tobie", "Bardzo Ci").random())
            if (reply.isNotEmpty()) {
                reply.append(" ")
                reply.append(listOf("Dziękuję", "Dzięki", "Dziękujemy").random())
            } else {
                reply.append(
                    listOf(
                        "Dziękuję Ci",
                        "Dzięki Ci",
                        "Dziękujemy Ci",
                        "Dziękuję Tobie",
                        "Dzięki Tobie",
                        "Dziękujemy Tobie"
                    ).random()
                )
            }
            reply.append(listOf(".", "!", "!!").random())
            reply.append(" ")
            reply.append(listOf("<3", "(:", ":)", "").random())
            reply.append(listOf("\n", "\n\n", " ").random())
            reply.append("Link do mojej zbiórki: ")
            if (randomizeUrls) {
                reply.append(
                    shortenUrl("http://siepomaga.pl/raczka-kuby?" + Random().nextLong().toString())
                )
            } else {
                reply.append("http://siepomaga.pl/raczka-kuby")
            }

            reply.append(" ")

            if (java.time.LocalDate.now().month.value <= 4) {
                // tax deduction usually can be done until end of April
                reply.append(listOf("\n", "\n\n", " ").random())
                reply.append("Możesz również przekazać mi swoje 1.5% podatku przy rozliczeniu PIT: ")
                if (randomizeUrls) {
                    reply.append(
                        shortenUrl("https://www.siepomaga.pl/raczka-kuby/procent-podatku?" + Random().nextLong().toString())
                    )
                } else {
                    reply.append("https://www.siepomaga.pl/raczka-kuby/procent-podatku")
                }
            }

            return reply.toString()
        }
    }

    private fun checkIfAllCommentsContainAdminComment(comments: ArrayList<com.restfb.types.Comment>) {
        for ((i, comment) in comments.withIndex()) {

            logger.debug("\t\t\thas comment $i/${comments.size} [${comment.message.substring(0, min(comment.message.length, 30))}]")
            if (!isCommentWrittenByOneOfAdmins(comment)) {
                if (!isCommentRepliedByOneOfAdmins(comment)) {

                    facebook.likePost(comment.id)
                    val replyMessage: String = randomizeThankYouReply()
                    logger.info("\t\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")
                    try {
                        facebook.commentPost(comment.id, replyMessage)
                        commentedPosts++
                    } catch (e: Exception) {
                        logger.error(e.message)
                    }

                    val numberOfSeconds: Long = (30..120).random().toLong()
                    logger.info("\t\t\t\tsleeping for $numberOfSeconds seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }

            }
        }
    }
    private fun fetchAllComments(postId: String): ArrayList<com.restfb.types.Comment> {
        var commentConnection: Connection<com.restfb.types.Comment> = restfbClient.fetchConnection(
            "$postId/comments", com.restfb.types.Comment::class.java,
            Parameter.with("fields", "id,from{name,id},message")
        )

        val allComments: ArrayList<com.restfb.types.Comment> = ArrayList()
        while (true) {
            for (commentList in commentConnection) {
                allComments.addAll(commentList)
            }
            if (commentConnection.nextPageUrl != null) {
                commentConnection = restfbClient.fetchConnectionPage(commentConnection.nextPageUrl, com.restfb.types.Comment::class.java)
            } else {
                break
            }
        }

        return allComments
    }

    fun checkIfAllCommentsUnderPostContainAdminComment(postId: String) {

        val allComments = this.fetchAllComments(postId)
        logger.info("\t\tgot ${allComments.size} comments")

        checkIfAllCommentsContainAdminComment(allComments)
    }
}