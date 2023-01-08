import facebook4j.Comment
import facebook4j.Facebook
import facebook4j.ResponseList
import java.lang.StringBuilder

class FacebookReplies(private val facebook: Facebook) {

    fun isCommentWrittenByOneOfAdmins(comment: Comment): Boolean {
        return comment.from !== null && // non-Page comments are null
            comment.from.id in arrayOf("105161449087504"); // Kuba
    }

    fun isCommentRepliedByOneOfAdmins(comment: Comment): Boolean {

        val commentsOfComment: ResponseList<Comment> = facebook.getCommentReplies(comment.id)

        for (commentOfComment in commentsOfComment) {
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
}