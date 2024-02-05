import com.restfb.Connection
import com.restfb.FacebookClient
import com.restfb.Parameter
import facebook4j.*
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


class FacebookReplies(private val facebook: Facebook, private val restfbClient: FacebookClient) {

    var commentedPosts = 0
    private val logger = KotlinLogging.logger {}

    private fun isCommentWrittenByOneOfAdminsRestfb(comment: com.restfb.types.Comment): Boolean {
        return comment.from?.id == "105161449087504" // Kuba
    }

    private fun isCommentWrittenByOneOfAdmins(comment: Comment): Boolean {
        return comment.from?.id == "105161449087504" // Kuba
    }

    private fun isCommentRepliedByOneOfAdminsRestfb(comment: com.restfb.types.Comment): Boolean {

        val commentsOfComment: ArrayList<com.restfb.types.Comment> = fetchAllComments(comment.id)

        logger.debug("\t\t\thas comment ${comment.message}")
        for (commentOfComment in commentsOfComment) {
            logger.trace("\t\t\t\twhich is commented by ${commentOfComment.from?.name}: ${commentOfComment.message.replace("\n", "")}")
            if (isCommentWrittenByOneOfAdminsRestfb(commentOfComment)) {
                return true
            }
        }
        return false
    }

    private fun isCommentRepliedByOneOfAdmins(comment: Comment): Boolean {

        val commentsOfComment: ResponseList<Comment> = facebook.getCommentReplies(comment.id)

        logger.debug("\t\t\thas comment ${comment.message}")
        for (commentOfComment in commentsOfComment) {
            logger.trace("\t\t\t\twhich is commented by ${commentOfComment.from?.name}: ${commentOfComment.message.replace("\n", "")}")
            if (isCommentWrittenByOneOfAdmins(commentOfComment)) {
                return true
            }
        }
        return false
    }

    // debug public
//    public fun shortenUrl(longUrl: String): String {
//        var bodyValues: MultiValueMap<String, String> = LinkedMultiValueMap()
//        bodyValues["url"] = URLEncoder.encode(longUrl, StandardCharsets.UTF_8)
//
//        val client = WebClient.create()
//
//        val result: String = client.post()
//            .uri(URI("https://cleanuri.com/api/v1/shorten"))
//            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//            .accept(MediaType.APPLICATION_JSON)
//            .body(BodyInserters.fromFormData(bodyValues))
//            .retrieve()
//            .bodyToMono(String::class.java)
//            .block();
//
//        return ""
//    }

    companion object {
        fun randomizeThankYouReply(): String {
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
            reply.append(
                listOf(
                    "http://siepomaga.pl/raczka-kuby",
                    "https://cleanuri.com/8w37mQ",
                    "https://cleanuri.com/BGN7lp",
                    "https://cleanuri.com/rP52Yl",
                    "https://cleanuri.com/w9A129",
                    "https://cleanuri.com/bNpJrr",
                ).random()
            )
            reply.append(" ")

            if (java.time.LocalDate.now().month.value <= 4) {
                // tax deduction usually can be done until end of April
                reply.append(listOf("\n", "\n\n", " ").random())
                reply.append("Możesz również przekazać mi swoje 1.5% podatku przy rozliczeniu PIT: ")
                reply.append(
                    listOf(
                        "https://www.siepomaga.pl/raczka-kuby/procent-podatku",
                        "https://cleanuri.com/dA9zr6",
                        "https://cleanuri.com/VYoWeL",
                        "https://cleanuri.com/dA9zQY",
                        "https://cleanuri.com/jzpvdA",
                        "https://cleanuri.com/Q2N6jR",
                    ).random()
                )
            }

            return reply.toString()
        }
    }

    private fun checkIfAllCommentsContainAdminCommentRestfb(comments: ArrayList<com.restfb.types.Comment>) {
        for (comment in comments) {

            if (!isCommentWrittenByOneOfAdminsRestfb(comment)) {
                if (!isCommentRepliedByOneOfAdminsRestfb(comment)) {

                    facebook.likePost(comment.id)
                    val replyMessage: String = randomizeThankYouReply()
                    logger.info("\t\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")
                    try {
                        facebook.commentPost(comment.id, replyMessage)
                        commentedPosts++
                    } catch (e: Exception) {
                        logger.error(e.message)
                    }

                    val numberOfSeconds: Long = (120..360).random().toLong()
                    logger.info("\t\t\t\tsleeping for $numberOfSeconds seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }

            }
        }
    }

    private fun checkIfAllCommentsContainAdminComment(comments: PagableList<Comment>) {
        for (comment in comments) {

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

    fun checkIfAllCommentsUnderPostContainAdminComment(post: Post) {

        val allComments = this.fetchAllComments(post.id)
        logger.info("\t\tgot ${allComments.size} comments")

        checkIfAllCommentsContainAdminCommentRestfb(allComments)
    }
}