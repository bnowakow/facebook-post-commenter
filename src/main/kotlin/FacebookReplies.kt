import facebook4j.*
import mu.KotlinLogging


class FacebookReplies(private val facebook: Facebook) {

    var commentedPosts = 0
    private val logger = KotlinLogging.logger {}

    private fun isCommentWrittenByOneOfAdmins(comment: Comment): Boolean {
        return comment.from?.id == "105161449087504" // Kuba
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
            reply.append("Link do mojej zbiórki: http://siepomaga.pl/raczka-kuby ")

            if (java.time.LocalDate.now().month.value <= 4) {
                // tax deduction usually can be done until end of April
                reply.append(listOf("\n", "\n\n", " ").random())
                reply.append("Możesz również przekazać mi swoje 1.5% podatku przy rozliczeniu PIT: https://www.siepomaga.pl/raczka-kuby/procent-podatku")
            }

            return reply.toString()
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

                    val numberOfSeconds: Long = (120..360).random().toLong()
                    logger.info("\t\t\t\tsleeping for $numberOfSeconds seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }
            }
        }
    }

    fun checkIfAllCommentsUnderPostContainAdminComment(post: Post) {
        val commentLimitNumber = 250
        val comments: PagableList<Comment> = facebook.getPostComments(post.id, Reading().limit(commentLimitNumber))

        logger.info("\t\tgot ${comments.size} comments on first page with limit of $commentLimitNumber")
        check(commentLimitNumber > comments.size)

        // debug
        //        var paging: Paging<Comment> = comments.paging
//        var comments2: PagableList<Comment> = facebook.fetchNext(paging)
//        var paging2: Paging<Comment> = comments2.paging
//        var comments3: PagableList<Comment> = facebook.fetchNext(paging2)
//        var paging3: Paging<Comment> = comments3.paging
        // \debug

//        while (comments.paging?.next !== null) {
//            var url: URL = comments.paging.next
//            comments = facebook.fetchNext(comments.paging)
//        }

        checkIfAllCommentsContainAdminComment(comments)
    }
}