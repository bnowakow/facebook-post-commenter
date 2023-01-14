package pl.bnowakowski.facebook_commenter

import facebook4j.Post

class FacebookPost {
    companion object {
        fun previewMessage(post: Post): String {
            var messagePreview = ""
            if (post.message !== null ) {
                messagePreview = if (post.message.length > 30) {
                    post.message.substring(0, 30)
                } else {
                    post.message
                }
            }

            return messagePreview
        }
    }
}