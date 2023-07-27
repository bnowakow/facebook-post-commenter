import facebook4j.*
import mu.KotlinLogging
import pl.bnowakowski.facebook_commenter.FacebookPost


fun main() {

    val facebook: Facebook = FacebookFactory().instance
    val facebookReplies = FacebookReplies(facebook)
    val logger = KotlinLogging.logger {}
    val facebookProperties = FacebookProperties()
    val facebook4jProperties = Facebook4jProperties()

    // TODO automate inviting people liking page to join grou
    // TODO find notification with people sharing fanpage (orange flag icon in notification) and comment it

    facebook.extendTokenExpiration()

    /************************
     * Ad Posts
     ***********************/

    var postNumber = 1
    // TODO figure out can we get id's of ad posts from API
    val adPostIds: List<String> = listOf(
        "105161449087504_pfbid02Qnx3ctSvN2Z2JJDEzp25kcdsLgNVSNtHV1bF57psQTR5zWHY6NgEExRnSxMBw6A9l",         // 1
//        "105161449087504_pfbid02QvbZbFUeYnwnktYc1Ryfi617mAMaJC6r655NxWENXF3VoqVRkE6DjhmdCrKZhoLQl",       // duplicate of 1
//        "105161449087504_pfbid0MAHt4ALYF5u5rmwRYNAjVq1ZHLQr7H9Cnvm52CL1Hp7mScWjRmNgKAMQn5TngvrJl",        // duplicate of 1
        "105161449087504_pfbid0gywSSeZKvCFomR5dELyr2ULFpk35SLHAaE5USdiMeyWw4H6bi5yLBVrHnnVN4tuEl",          // 2
//        "105161449087504_pfbid02kGQeR7Rvg73KXmW7PGRhWZWuTTHa9Vz3btLiDoZKg4qkvjGPUuD44RxAjTE4Uwjql",       // duplicate of 2
        "105161449087504_pfbid0VSV5LB9fjJNpas2RyCRgfEjYdV8hbyZ24pnxCNtWKuJFCdHuAoSs2DfgLDheCrDtl",          // 3
//        "105161449087504_pfbid02Z7mnDhokm7qsKJKNHdE6xWxtJJSx2CaVRrGsxa9kLLFWiGBnnVHCFf9QX6eVQ54Ml",       // duplicate of 3
//        "105161449087504_pfbid0V6B4jtcC67k8KtrL1UuFFexNVn7uNWr5psXiXnJSfxdqbLfuEs6Es3EragggRiVXl",        // duplicate of 3
//        "105161449087504_pfbid02YmTmdRGH82g4ftA6GnNYRuuVxRU58qVnZYMHnqmybHuRS8Lm53v3AsEfvTsf8Wz4l",       // duplicate of 3
        "105161449087504_1172046396767560",                                                                 // 4
        "105161449087504_pfbid0L8t5oum9D78tfeFybVqr8yDgU2WLAvUdMYznMa5n5cKHH4bvTKLNbkKYCxKJgNtHl",          // 5
        "105161449087504_pfbid02PYSh1VNnQ88X6U7mBTfQHvUqBW5ZbRiXZ9Di5WfzwLmVPEoAe7gPrjDmZ5YCATMHl",         // 6
//        "105161449087504_pfbid02PrFDGzgFQ7iiMJ8tpEAtW56weVYahCBTZQeZfHPzfJo3ssJo5AoaGYSbtPaEDcyZl"        // duplicate of 6
//        "10516144908750x4_pfbid02Pg6CyrQWakevfJ71eQbaLCc5Lpi2g4spGd9kGvdvzWZZTJ3SadBZgU1M7zQYTM7nl"       // duplicate of 6
//        "105161449087504_pfbid0L8T24gn9jPXEjFw98pEjTbnf846HprSVeRQNCf3RyXXYToMWv9BtU7rvuWYQCc1Yl",        // duplicate of 6
//        "105161449087504_pfbid0KxJ1mYWQuyoquoxxT78DCKqshatPaR3uCkhYsA6w4KFEqSjKShXEDWExAgtsGwdml",        // duplicate of 6
//        "105161449087504_pfbid02PojixDSEmRUoPtS5j4CwTSdH3tJi5GHiJNGxDdHMnZyzCoxps7BDcBtYqRmdp21Ml",       // duplicate of 6
//        "105161449087504_pfbid02Pdaif5AVwjqhWMUpNLnewRsXcq2TqdopJxjV79jMhNZRxXN35SYK5RrrbmCB5858l",       // duplicate of 6
        "105161449087504_pfbid0oyzqNdh1peTiGK2bsTLBNRReWDQ29iZT6JjmiSSoXrBuzuq7jD8czBZ2i1LaE5Val",          // 7
//        "105161449087504_pfbid0ooqq5VRGzyVkPoY2QdeAfEu8LUkrx1pD8sGVTjRbqWZnZqre9xDjD9Nx3G9HHsh1l",        // duplicate of 7
//        "105161449087504_pfbid0p7eMLzik1926nemiV44RG9QYHEuVFCYQwqyhFpb4Wqn2tuLFzxsozJuK5F9YEK3n9l",       // duplicate of 7
//        "105161449087504_pfbid02sV8Xy25N2mq6oK3CbbZj5bXyJvn6DQ1zBGwkgi9rcYnZ83DjPZJyhYEwgaJtWuyWl",       // duplicate of 7
//        "105161449087504_pfbid0owVM3rT1Bkww4QjdTLDbmZz6ZcCx5KskkvGbuR15q2pq3PS5sJwTuTKCj8AN7Npvl",        // duplicate of 7
//        "105161449087504_pfbid02snw4EXNq3AgYv72ai7NwxJocbsfCjGz3wwzzk9xq4ErRuP7BUQ1sNAfGT93r2MYdl",       // duplicate of 7
//        "105161449087504_pfbid02scn3wP76DSTLLaqSntN3MBTnNS7PDFbZcDEfh94eRPj9agx1nhcWSuiQ3P3kCp5Yl",       // duplicate of 7
        "105161449087504_pfbid02PmtVH17t6Vcu811B4KvrBodB4ZdJ5PhwD9VdsrF69kpakZnaPeKzp4WRqUTR3q92l",         // 8
//        "105161449087504_pfbid02PbjUyrr9GtXAvVnDzvYJ26s41eBpqfnn3mFcdPNWBdkZcMPDPH6F2vU5anmwYJEBl",       // duplicate of 8
//        "105161449087504_pfbid0L46J4hDnRkHUVkzn2ZUoatFFfM8KE63SJegAkXv84UDiz9d6fKHH7P2XniENTSN8l",        // duplicate of 8
//        "105161449087504_pfbid02PuY1FN9cH8aaPsaSyZ4LLx2WXEjT9DtrCUkVm2WKE2p2hf3EnDK6RK1Ts8ESwcWfl",       // duplicate of 8
//        "105161449087504_pfbid0KswHmYx3c4tWxmKjSPNu6yi9q9VDaceFNsWCxHT4NTiCaQATMhT8BjzSbB8C9Jgjl",        // duplicate of 8
//        "105161449087504_pfbid02PjNzxDssTZMT97qidc73xFeF2R5zTPaRFSbwnLg87q6LWHTrSZWYKJkoBMQckREil",       // duplicate of 8
//        "105161449087504_pfbid02PZDzf5c8e3hSt4PvDgPxRatxukGQyCu5ZzHCqz8Gyz6FFfwkeeqwFsuTJYLnUeyBl",       // duplicate of 8
        "105161449087504_pfbid0jDpKaCbboTVsmT7yKWDMsPcsGbrEkMHJGUD2CyUVv31QcbdhZv9HiZsoBJJGJAVtl",          // 9
//        "105161449087504_pfbid02o5G2ksXResxMN3cjBD7xubYiMPTYWfjrRSmxUYaoPspbApvUi1X1zHPcDHkq5xinl",       // duplicate of 9
//        "105161449087504_pfbid0j3fKH4Krz4UMJMD5cM3DrvZbBtAqddCUANf4AnJfsVEU9BKKRVvzyrXh7oKvakxrl",        // duplicate of 9
//        "105161449087504_pfbid02nu72TjFgqQeRcwiiKpNGsGVpgJocTxAcWxKv66bmBykAgMWFaAUgy6DtjzM661aul",       // duplicate of 9
//        "105161449087504_pfbid0jMTqYZdKzCKcNW7jJf6TDxcs5o9KXz8ZfUD7nrKwk2ioNz9x4ijFpdz15udVNHNJl",        // duplicate of 9
//        "105161449087504_pfbid02niqgni5wHfEdrTm9696tFFYA8x7GZwcSa1Bfd4RSquaA1XZ9TpGDD1V3CKJ8S1KDl",       // duplicate of 9
//        "105161449087504_pfbid0jBCVsYTaSQRdM5KtigdFSBwGkZ7EeZ3DevzAAFy42fHzhSu79fBcciC3GRFoCUpNl",        // duplicate of 9
//        "105161449087504_pfbid02o2eD4DPQHtio1Gja2cyQ7as7JrZeZrtE8SC1yPa8Ly4C1nSuF5ctUJzzQgLLKAdPl",       // duplicate of 9
        "105161449087504_pfbid036aUsfYo8NpiZR5MhSyLcB16HuxxCG5QrpRWJStWBkNgSQKmjfRJ9E3hG62Pr94Hsl",         // 10
//        "105161449087504_pfbid0232qgkPAmXVF1dNoFCa4VuqWMKqY5f2qRm4GPeirhGP6PrFDkwkXJZ9xuGqA9dZ2kl",       // duplicate of 10
//        "105161449087504_pfbid036tHPw46bNrcPH7WxeWQ473QLSwioMw1N8TaRp6A8pU9b7TJSmc8KGpTKEReEn7rdl",       // duplicate of 10
//        "105161449087504_pfbid022rggTEu2hz13wH7tfi6md682ewFuhTTgKztKLyxPEPzZmZMfoSzL42CsW7oprxxal",       // duplicate of 10
//        "105161449087504_pfbid036i8PduprZUuVGrpJ2Jn3ohh1A8knEpWCViwGoKMHGZLYpSzWM94hC9cDLudX9ddel",       // duplicate of 10
//        "105161449087504_pfbid023AVCikCViHdR1iPR8usMySacGHr5kke2on8M4d8U7naB9co7iKxqMz7EoubpASf3l",       // duplicate of 10
//        "105161449087504_pfbid036XyPLmZ7jnHxNNXiBXckT82gC9JieF9E2eho9aRUFtWNfHbK237nNnSpq548ELyul",       // duplicate of 10
        "105161449087504_pfbid02JgVMb7AhbxAgLzb7YgEMVr9A8eTz5h5ytepAVPeNSZTkctLk84zycGnSeWfa212kl",         // 11
//        "105161449087504_pfbid0F8rAfwYLkdEhTZzhnKwzS5CgAoxBigJfyFoxeqtfDZSePyMBQESCrmN5rMcydz8fl",        // duplicate of 11
//        "105161449087504_pfbid02JzHsrcUAc1sFPynMyYYvE6a1mR1LeMPhshNRWHsix3bt6Shc6G1dg8W62VQ8tdkil",       // duplicate of 11
//        "105161449087504_pfbid0FSegwSqokpeCPQ4z9k3Mgt6TiiCihgcMzUqQBoFMz8BT19ms4XMGJVAh6VbNhK5ml",        // duplicate of 11
//        "105161449087504_pfbid02Jp8sZUCRneCf7ACPqSkgGpMC7yiq1XowhQrVR2szNE3qncfcWPMYxsCzPDtAr9QLl",       // duplicate of 11
//        "105161449087504_pfbid02K7wPpyVtnkPQYYnBdBx9NceZK48C6ZdCezC5BF7uv8LHdP1oyaH4Z9DwqXCVXPdSl",       // duplicate of 11
//        "105161449087504_pfbid0FQ9Ccfbo83xQSnAJR4brUsFqPacZ9tTot92fShrf8MMkyKteb61pzMQk3mDrjvEhl",        // duplicate of 11
//        "105161449087504_pfbid02JmdPEgxR9dD2V4HK5hLQkWRBC4PN8tmFc8ieaSkuBdECF3qnWNcoFGUpzzL4vjbvl",       // duplicate of 11
        "105161449087504_pfbid02i3af3dUCAKpyNX8WVYW9HSyme3QfsQ4gFvfBK7apFPW2bZUdNPBjwcSb4tLEKhZTl",         // 12
        "105161449087504_pfbid02hyeu3vLQyL5uPy7YDF8HWpAsJggibkQ1BhZUHDJEABANTdypHSskUq25mrNSpCiFl",         // 13
        "105161449087504_pfbid01TEkQjff5D7Wg7BgaPPQcFVoonErBHVAPC4HYAfYTmboxd3zid5x9nKCwxZ5cJtBl",          // 14
        "105161449087504_pfbid028TLQmc2eTrxkbrWbNvHcSXRiCzidtk1AbW6zESN8KTwwrheDv5XRuWgQpFyudj7Ql",         // 15
        "105161449087504_pfbid02T7cDoH6JSo1nsNhAN5DxBkrt1zWWCXGYbhrFTiJAiZskW9PWMjA97vptFNjMSN3nl",         // 16
        "105161449087504_pfbid0zmkxMxMGZ6LD89PJfAzWHPaYBfgW9wzsV4txipv4a1f4FFHcJBupbS3eNUTfHmbil",          // 17

        // todo: 781862799854716 https://www.facebook.com/Kuba.Dobrowolski.Nowakowski/videos/781862799854716
        // todo: https://www.facebook.com/Kuba.Dobrowolski.Nowakowski/posts/pfbid0qTTvsByqd1tfpFrqR8iYN8XX1ufVpAStAyPnsCVhgXKxsRuQDXGfy8RDkHjYYexnl?notif_id=1685549767027847&notif_t=feedback_reaction_generic&ref=notif

    )

    logger.info("will be processing ${adPostIds.size} ad posts:")

    for (adPostId in adPostIds) {
        val post = facebook.getPost(adPostId)
        logger.info("in ${postNumber}/${adPostIds.size} ad post [${FacebookPost.previewMessage(post)}...], ${post.id}")

        // comments under ad posts via API
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(
                facebook.getPost(adPostId)
            )
        }

        // shared ad posts using workaround
        // TODO check how to get shared posts of ads
//        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
//            facebookSharedPosts !== null) {
//            facebookSharedPosts.openSharedPosts(adPost)
//        }
        postNumber++
    }

    /************************
     * Fanpage Posts
     ***********************/

    val posts: ResponseList<Post> = facebook.getPosts("105161449087504") // Kuba
    logger.info("will be processing ${posts.size} fan page posts:")

    var facebookSharedPosts: FacebookSharedPosts? = null
    if (facebookProperties.getProperty("workaround-enabled") == "true") {
        facebookSharedPosts = FacebookSharedPosts()
        facebookSharedPosts.loginToFacebook()
        facebookSharedPosts.inviteToLikeFanpagePeopleWhoInteractedWithPosts()
        facebookSharedPosts.switchProfileToFanPage()
    }

    postNumber = 1
    for (post in posts) {
        logger.info("in ${postNumber}/${posts.size} post [${FacebookPost.previewMessage(post)}...], ${post.id}")

        // comments under posts via API
        // TODO this will fail with property absent in file
        if (facebook4jProperties.getProperty("enabled") == "true") {
            logger.info("\tlooking into comments under post")
            facebookReplies.checkIfAllCommentsUnderPostContainAdminComment(post)
        }

        // shared posts via API
        //  val sharedPosts = facebook.getSharedPosts(post.id) // API

        // shared posts using workaround
        // TODO checking if value is not null is not elegant (it's done because when object is created browser window is spawned which is not necessary when only API is used)
        if (facebookProperties.getProperty("workaround-enabled") == "true" &&
            facebookSharedPosts !== null) {
            logger.info("\tlooking into shared posts using workaround")
            facebookSharedPosts.openSharedPosts(post.id)
        }
        postNumber++
    }

    if (facebook4jProperties.getProperty("enabled") == "true") {
        logger.info("added comment to ${facebookReplies.commentedPosts} comments via API")
    }
    if (facebookProperties.getProperty("workaround-enabled") == "true" &&
        facebookSharedPosts !== null) {
        logger.info("added comment to ${facebookSharedPosts.commentedPosts} comments via shared posts workaround")
    }

}
