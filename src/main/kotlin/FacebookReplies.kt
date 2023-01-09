import facebook4j.*


class FacebookReplies(private val facebook: Facebook) {

    val commentedPosts: Int = 0

    fun isCommentWrittenByOneOfAdmins(comment: Comment): Boolean {
        return comment?.from?.id == "105161449087504"; // Kuba
    }

    fun isCommentRepliedByOneOfAdmins(comment: Comment): Boolean {

        val commentsOfComment: ResponseList<Comment> = facebook.getCommentReplies(comment.id)

        println("\thas comment ${comment.message}")
        for (commentOfComment in commentsOfComment) {
            println("\t\twhich is commented by ${commentOfComment.from?.name}: ${commentOfComment.message}")
            if (isCommentWrittenByOneOfAdmins(commentOfComment)) {
                return true;
            }
        }
        return false;
    }

    fun randomizeThankYouReply(): String {
        val reply: StringBuilder = StringBuilder()
        reply.append(listOf("", "Bardzo Tobie", "Bardzo Ci").random())
        if (reply.isNotEmpty()) {
            reply.append(" ")
            reply.append(listOf("Dziękuję", "Dzięki", "Dziękujemy").random())
        } else {
            reply.append(listOf("Dziękuję Ci", "Dzięki Ci", "Dziękujemy Ci", "Dziękuję Tobie", "Dzięki Tobie", "Dziękujemy Tobie").random())
        }
        reply.append(listOf(".", "!", "!!").random())
        reply.append(" ")
        reply.append(listOf("<3", "(:", ":)", "").random())
        reply.append(listOf("\n", "\n\n", " ").random())
        reply.append("Link do mojej zbiórki: http://siepomaga.pl/raczka-kuby")
        return reply.toString()
    }

    fun checkIfAllCommentsContainAdminComment(comments: PagableList<Comment>) {
        for (comment in comments) {

            if (!isCommentWrittenByOneOfAdmins(comment)) {
                if (!isCommentRepliedByOneOfAdmins(comment)) {
//                    println("\t\t\tcomment ${comment}");
//                    println("comment ${comment} under post ${post.id}");

                    facebook.likePost(comment.id)
                    val replyMessage: String = randomizeThankYouReply()
                    println("\t\t\ttrying replying with '${replyMessage}'")
                    facebook.commentPost(comment.id, replyMessage)
                    commentedPosts.inc()

                    val numberOfSeconds: Long = (10..120).random().toLong()
                    println("\t\t\tsleeping for ${numberOfSeconds} seconds\n")
                    Thread.sleep(1000 * numberOfSeconds)
                }
            }
        }
    }

    fun checkIfAllCommentsUnderPostContainAdminComment(post: Post) {
        var comments: PagableList<Comment> = facebook.getPostComments(post.id, Reading().limit(250))
        var paging: Paging<Comment>
        println("in post [${post.message?.substring(0, 30)}...] got ${comments.size} comments on first page")

        checkIfAllCommentsContainAdminComment(comments)

//        while (comments.paging?.next !== null) {
//            var url: URL = comments.paging.next
//            comments = facebook.fetchNext(comments.paging)
//        }
    }
}