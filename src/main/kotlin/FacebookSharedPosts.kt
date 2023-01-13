import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.interactions.Actions


class FacebookSharedPosts {

//    val driver: WebDriver = ChromeDriver()
    val driver: WebDriver = FirefoxDriver()
//    val driver: WebDriver = SafariDriver()

    val facebookProperties: FacebookProperties = FacebookProperties()

    private val logger = KotlinLogging.logger {}
    fun loginToFacebook() {

        val facebookProperties: FacebookProperties = FacebookProperties()

//        System.setProperty("webdriver.chrome.driver", "Yourpath\\chromedriver.exe")

//        driver.manage().window().maximize()
        driver["https://www.facebook.com"]
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        Thread.sleep(500)
        // cookie form
        driver.findElement(By.className("_9xo6")).click()
        // login button
        driver.findElement(By.name("login")).click()
        Thread.sleep(5000)
    }

    fun switchProfileToFanPage() {
        // TODO fix notification popup from chrome
        if (facebookProperties.getProperty("username").contains("kuba")) {
            // account icon
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]"))
                .click()
            Thread.sleep(500)
            // switch profile to fan page
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div"))
                .click()
        } else {
            // account icon
            driver.findElement(By.xpath("/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[1]/span/div/div[1]"))
                .click()
            Thread.sleep(500)
            // switch profile
            driver.findElement(By.xpath("/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[2]/div/div[1]/div"))
                .click()
            Thread.sleep(500)
            // switch profile to fan page
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[2]/div/div[2]/div[1]/div[2]/div"))
                .click()
        }
        Thread.sleep(2000)
    }

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")
        // TODO get first words of post to log it alongside with id
        logger.info("lookig in shares of ${id} post")
        driver["https://www.facebook.com/shares/view?id=$id"]
        Thread.sleep(5000)

        // scroll down to bottom of page to load all posts (lazy loading)
        val js = driver as JavascriptExecutor
        // TODO figure out how to verify if all posts have been loaded
        var j = 200
//        j = 12 // TODO debug
        for (i in 1..j) {
            //Scroll down till the bottom of the page
            js.executeScript("window.scrollBy(0,document.body.scrollHeight)")
            Thread.sleep(1000)
            // TODO check if checking document height after scroll has changed, if not we reached bottom
            if (i % 100 == 0 || i === 0) {
                // should be debug but can't set netty to info then
                logger.info("\tscrolling for ${i}th time out of ${j.toString()} tries")
            }
        }

        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
        Thread.sleep(1000)

        if (facebookProperties.getProperty("username").contains("kuba")) {
            // search box
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[2]/div/div/div/div[3]")).click()
            // div containg all posts to move focus to some reachable element at the top
            // oryginal post (admin's one)
            Actions(driver).moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div")))
                .perform()
            // first shared post
            Actions(driver).moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]")))
                .perform()
        } else {
            // search box
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div[2]/div[2]/div")).click()
            // div containg all posts to move focus to some reachable element at the top
            // oryginal post (admin's one)
            Actions(driver).moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[1]")))
                .perform()
            // dif of all shared posts
            Actions(driver).moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]")))
                .perform()
        }

        val beginingOfNextPostLocation = driver.pageSource.indexOf("Shared with Public</title>")
        if (beginingOfNextPostLocation > -1) {
            var pageSource: String = driver.pageSource.removeRange(0, beginingOfNextPostLocation)

            var commentNumber = 1
            while (true) {
                // TODO fix edge case for last comment
                val secondCommentLinkPosition: Int = pageSource.indexOf("Shared with Public</title>", 1)
                if (secondCommentLinkPosition == -1) {
                    break
                }
                //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to acces post made by others
                var commentSource: String = pageSource.substring(0, secondCommentLinkPosition)
                // commentAuthor shows name of next post since we're spliting post by shared with public which occures after a post author
                val commentAuthor: String =
                    commentSource.removeRange(0, commentSource.indexOf("<a aria-label")).replace("<a aria-label=\"", "")
                        .replaceAfter("\"", "").replace("\"", "")
                var commentTextBoxPosition: Int = commentSource.indexOf("Write a comment")
                if (commentTextBoxPosition > -1) {
                    // can be commented
                    logger.debug("\t\tpost ${commentNumber}, next one is written by ${commentAuthor} can be commented")
                    val adminUsernamePosition = commentSource.indexOf("Kuba Dobrowolski-Nowakowski")
                    if (adminUsernamePosition == -1) {
                        // no comment from admin of fanpage
                        logger.debug("\t\t\tpost ${commentNumber}, next one is written by ${commentAuthor} doesn't contain admin response")
                        val replyMessage: String = FacebookReplies.Companion.randomizeThankYouReply()
                        logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                        try {
                            if (facebookProperties.getProperty("username").contains("kuba")) {
                                // chrome
//                              driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                                    .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    // TODO this breaks for some rare kind of post (maybe ones with commenting turned off text or smth other)
                                    // TODO check if that fails if post has already one commment
                                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                    .sendKeys(Keys.RETURN)
                            } else {
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    // TODO this breaks for some rare kind of post (maybe ones with commenting turned off text or smth other)
                                    // TODO check if that fails if post has already one commment
                                    driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
                                    .sendKeys(Keys.RETURN)
                            }
                        } catch (e: NoSuchElementException) {
                            logger.error(e.message)
                            logger.error("NoSuchElementException exception has been thrown during processing of ${id} post on ${commentNumber}th post, next one is written by ${commentAuthor}")
                        } catch (e: Exception) {
                            logger.error(e.message)
                            logger.error("Exception exception has been thrown during processing of ${id} post on ${commentNumber}th post , next one is written by ${commentAuthor}")
                        }

                        val numberOfSeconds: Long = (10..120).random().toLong()
                        logger.info("\t\t\tsleeping for ${numberOfSeconds} seconds\n")
                        Thread.sleep(1000 * numberOfSeconds)
                    } else {
                        logger.debug("\t\t\tpost ${commentNumber}, next one is written by ${commentAuthor} contains admin response")
                        // TODO this breaks if there's longer post (i.e. with comments)
                        // TODO example is birth post no. 85
                        for (i in 1..20) {
                            driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                            Thread.sleep(5)
                        }
                    }
                } else {
                    logger.debug("\t\tpost ${commentNumber}, next one is written by ${commentAuthor} can't be commented")
                    // TODO this breaks if there's longer post (i.e. with comments)
                    // TODO example is birth post no. 85
                    for (i in 1..8) {
                        driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                        Thread.sleep(5)
                    }
                }

                commentNumber++
                pageSource = pageSource.removeRange(0, secondCommentLinkPosition)
            }
        } else {
            logger.info("\tpost ${id} doesn't have any shared posts")
        }
    }
}