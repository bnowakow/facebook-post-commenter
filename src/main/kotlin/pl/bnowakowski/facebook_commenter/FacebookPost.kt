package pl.bnowakowski.facebook_commenter

import facebook4j.Facebook
import facebook4j.Post

class FacebookPost() {
    companion object {
        fun previewMessage(post: Post): String {
            var messagePreview: String = ""
            if (post.message !== null ) {
                if (post.message.length > 30) {
                    messagePreview = post.message.substring(0, 30)
                } else {
                    messagePreview = post.message
                }
            }

            return messagePreview
        }
    }
}