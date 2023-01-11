import facebook4j.*
import mu.KotlinLogging


class FacebookReplies(private val facebook: Facebook) {

    val commentedPosts: Int = 0
    private val logger = KotlinLogging.logger {}

    fun isCommentWrittenByOneOfAdmins(comment: Comment): Boolean {
        return comment?.from?.id == "105161449087504"; // Kuba
    }

    fun isCommentRepliedByOneOfAdmins(comment: Comment): Boolean {

        val commentsOfComment: ResponseList<Comment> = facebook.getCommentReplies(comment.id)

        logger.debug("\thas comment ${comment.message}")
        for (commentOfComment in commentsOfComment) {
            logger.debug("\t\twhich is commented by ${commentOfComment.from?.name}: ${commentOfComment.message}")
            if (isCommentWrittenByOneOfAdmins(commentOfComment)) {
                return true;
            }
        }
        return false;
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
            reply.append("Link do mojej zbiórki: http://siepomaga.pl/raczka-kuby")
            return reply.toString()
        }
    }

    fun checkIfAllCommentsContainAdminComment(comments: PagableList<Comment>) {
        for (comment in comments) {

            if (!isCommentWrittenByOneOfAdmins(comment)) {
                if (!isCommentRepliedByOneOfAdmins(comment)) {
//                    println("\t\t\tcomment ${comment}");
//                    println("comment ${comment} under post ${post.id}");

                    facebook.likePost(comment.id)
                    val replyMessage: String = randomizeThankYouReply()
                    logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")
                    facebook.commentPost(comment.id, replyMessage)
                    commentedPosts.inc()

                    val numberOfSeconds: Long = (10..120).random().toLong()
                    logger.info("\t\t\tsleeping for ${numberOfSeconds} seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }
            }
        }
    }

    fun checkIfAllCommentsUnderPostContainAdminComment(post: Post) {
        var commentLimitNumber: Int = 250
        var comments: PagableList<Comment> = facebook.getPostComments(post.id, Reading().limit(commentLimitNumber))

        var messagePreview: String = ""
        if (post.message !== null ) {
            if (post.message.length > 30) {
                messagePreview = post.message.substring(0, 30)
            } else {
                messagePreview = post.message
            }
        }

        logger.info("in post [${messagePreview}...] got ${comments.size} comments on first page with limit of ${commentLimitNumber}")
        check(commentLimitNumber > comments.size)

        // debug
        //        var paging: Paging<Comment> = comments.paging
//        var comments2: PagableList<Comment> = facebook.fetchNext(paging)
//        var paging2: Paging<Comment> = comments2.paging
//        var comments3: PagableList<Comment> = facebook.fetchNext(paging2)
//        var paging3: Paging<Comment> = comments3.paging
        // \debug

        checkIfAllCommentsContainAdminComment(comments)

//        while (comments.paging?.next !== null) {
//            var url: URL = comments.paging.next
//            comments = facebook.fetchNext(comments.paging)
//        }
    }
}