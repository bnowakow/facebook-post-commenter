import facebook4j.Facebook
import facebook4j.Post
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.interactions.Actions
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStream
import java.lang.Long.max
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException
import kotlin.math.ln
import kotlin.math.min


class FacebookSharedPosts (
    private val facebook: Facebook,
    private val adPostsProcessor: AdPostsProcessor,
    private val facebookProperties: FacebookProperties,
    private val facebook4jProperties: Facebook4jProperties) {

    private var driver: WebDriver
    private val js: JavascriptExecutor

    var commentedPosts = 0

    private val logger = KotlinLogging.logger {}

    init {
        // Firefox
        // https://www.browserstack.com/docs/automate/selenium/firefox-profile
        val firefoxProfile = FirefoxProfile()
//        firefoxProfile.setPreference("layout.css.devPixelsPerPx", "2.0")

        // https://stackoverflow.com/questions/15397483/how-do-i-set-browser-width-and-height-in-selenium-webdriver
        val firefoxOptions = FirefoxOptions()
        // https://github.com/mdn/headless-examples/blob/master/headlessfirefox-gradle/src/main/java/com/mozilla/example/HeadlessFirefoxSeleniumExample.java
        if (facebookProperties.getProperty("browser.headless").toBoolean()) {
            logger.debug("running browser in headless mode")
            firefoxOptions.addArguments("--headless")
        } else {
            logger.debug("running browser in non-headless mode")
        }
        // https://stackoverflow.com/questions/15397483/how-do-i-set-browser-width-and-height-in-selenium-webdriver
        firefoxOptions.addArguments("--width=1000")
        firefoxOptions.addArguments("--height=3440")
        firefoxOptions.profile = firefoxProfile

        driver = FirefoxDriver(firefoxOptions)
        if (!facebookProperties.getProperty("browser.headless").toBoolean()) {
            // laptop screen
//        driver.manage().window().position = Point(800, 0)
            // desktop screen
            driver.manage().window().position = Point(1490, 0)
        }

        driver["https://www.facebook.com"]
        js = driver as JavascriptExecutor
//        driver.findElement(By.cssSelector("body")).sendKeys(Keys.chord(Keys.COMMAND, "-"))

        // Chrome
        //driver = ChromeDriver()

        // Safari
        //driver = SafariDriver()
    }

    fun takeScreenshot(comment: String = "") {
        val scrFile: File= (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        val fileName = "screenshot" + "-" + java.time.LocalDateTime.now() + "-" + comment + ".png"
        logger.debug("trying to take screenshot of name=$fileName")
        // Now you can do whatever you need to do with it, for example copy somewhere
        FileUtils.copyFile(scrFile, File(fileName))
    }

    private fun tabUntilGivenLabelIsFocussed(attributeName: String, expectedAttributeValue: String, maximumAmountOfTabPresses: Int = 30) {
        for (i in 1..maximumAmountOfTabPresses) {
            if (driver.switchTo().activeElement().getAttribute(attributeName)?.equals(expectedAttributeValue) == true) {
                driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
                break
            }
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
            Thread.sleep(500)  // was 5 but slowing down to not get banned
        }
    }

    fun loginToFacebook() {

        val facebookProperties = FacebookProperties()

        logger.debug("trying to open facebook page")
        driver["https://www.facebook.com"]
        logger.debug("trying to fill user and password")
        driver.findElement(By.id("email")).sendKeys(facebookProperties.getProperty("username"))
        driver.findElement(By.id("pass")).sendKeys(facebookProperties.getProperty("password"))
        // cookie form
        // for some reason can't find alternative after they change code, as a workaround I put breakpoint on login button below and dismiss cookie modal manually
//        driver.findElement(By.className("_42ft")).click()
//        // workaround for above
        var i = 0
        while (true) {
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.TAB)
            Thread.sleep(100)
            val elementText = driver.switchTo().activeElement().text
            logger.trace("tab i=$i element_txt=$elementText")
            if (elementText.equals("Decline optional cookies")) {
                break
            }
            i++
        }
        logger.debug("trying to click on cookie consent form")
        try {
            driver.findElement(By.cssSelector("body")).sendKeys(Keys.RETURN)
        } catch (e: Exception) {
            logger.info("exception while pressing RETURN on Tabbed button. Trying to click button By.className")
            driver.findElement(By.className("_42ft")).click()
        }
//        // \workaround
        Thread.sleep(500)
        // login button
        logger.debug("trying to click on login button")
//        takeScreenshot("trying_to_click_on_login_button")
        driver.findElement(By.name("login")).click()
        Thread.sleep(6000)
    }

    fun inviteToLikeFanpagePeopleWhoInteractedWithPosts() {
        driver["https://business.facebook.com/latest/home?asset_id=${facebook4jProperties.getProperty("fanpage.id")}&nav_ref=aymt_reaction_inviter_tip&notif_id=1679842962374398&notif_t=aymt_bizapp_invite_reactors_to_like_page_notif&ref=notif"]
        Thread.sleep(6000)
        if (driver.pageSource.split("You've reached your limit").size > 1) {
            logger.info("Can't invite people who interacted with page because daily limit of invites was reached")
            return
        }

        // TODO check if modal has been shown, sometimes page doesn't show modal with user list
        scalePage(50)
        val numberOfUsers = driver.pageSource.split("name=\"select user\"").size - 1
        var numberOfInvitedUsers = 0
        try {
            for (i in 1..numberOfUsers) {
                try {
                    if (clickElementIfOneInListExists(listOf(
                            "/html/body/div[2]/div[1]/div[1]/div/div/div/div/div[2]/div[1]/div[2]/div/div[2]/div[1]/div/div[$i]/label/div/input",
                            "/html/body/div[4]/div[1]/div[1]/div/div/div/div/div[2]/div[1]/div[2]/div/div[2]/div[1]/div/div[$i]/label/div/input",
                            "/html/body/div[5]/div[1]/div[1]/div/div/div/div/div[2]/div[1]/div[2]/div/div[2]/div[1]/div/div[$i]/label/div/input",
                    )).found) {
                        numberOfInvitedUsers++
                    }
                } catch (e: Exception) {
                    // ignoring since rest of program can run
                    logger.error("couldn't select $i'th user on invite to fan page screen")
                }
                Thread.sleep(100)
            }
            if (numberOfInvitedUsers > 0) {
                clickElementIfOneInListExists(listOf(
                    "/html/body/div[4]/div[1]/div[1]/div/div/div/div/div[3]/div/div[2]/div/span/div/div/div",
                    "/html/body/div[5]/div[1]/div[1]/div/div/div/div/div[3]/div/div[2]/div"
                ))
            }
        } catch (e: NoSuchElementException) {
            logger.error(e.message)
            logger.error("NoSuchElementException in inviteToLikeFanpagePeopleWhoInteractedWithPosts")
        } catch (e: Exception) {
            logger.error(e.message)
            logger.error("Exception in inviteToLikeFanpagePeopleWhoInteractedWithPosts")
        }
        Thread.sleep(6000)
        logger.info("invited $numberOfInvitedUsers users who interacted with our posts to like our fan page")
    }

    fun switchProfileToFanPage() {
        driver["https://www.facebook.com"]
        // TODO fix notification popup from chrome
        Thread.sleep(10000)
        if (facebookProperties.getProperty("username").contains("kuba")) {
            // account icon
            logger.info("trying to click on account icon")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[1]/span/div/div[1]",
            ))
            // switch profile to fan page
            logger.info("trying to click on switch profile to fan page")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div[3]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/div[1]/div/span/div/div/div/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/div[1]/div/div[2]/span/div/div[1]/div[1]/span/div",
            ))
        } else {
            // account icon
            logger.info("trying to click on account icon")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[1]/span/div/div[1]",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[1]/span/div/div[1]",
            ))
            // switch profile to fan page
            logger.info("trying to click on switch profile to fan page")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/div[1]/div/div[2]/span/div/div[1]/div[1]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/div[1]/div/span[1]/div/div/div/div",
                "/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
            ))
        }
    }

    private fun canElementBeReachedAndPressTabOnIt(xpath: String): Boolean {
        logger.trace("\t\t\tcanElementBeReachedAndPressTabOnIt xpath=$xpath")
        return try {
            driver.findElement(By.xpath(xpath)).sendKeys(Keys.TAB)
    //            driver.findElement(By.xpath(xpath)).sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB))
            true
        } catch (e: Exception) {
            false
        }
    }

    data class XpathElementFound(val found: Boolean, val xpath: String? = null)
    private fun clickElementIfOneInListExists(possibleXpaths : List<String>, clickOnElement: Boolean = true, throwException: Boolean = true): XpathElementFound {

        var xpath: String
        val iter: Iterator<String> = possibleXpaths.iterator()
        var elementFoundOrClicked: Boolean
        while (iter.hasNext()) {
            xpath = iter.next()
            elementFoundOrClicked = false
            if (!clickOnElement) {
                // if clickOnElement == false there's some type of elements that could be clicked but you can't press TAB on them
                if (this.canElementBeReachedAndPressTabOnIt(xpath)) {
                    elementFoundOrClicked = true
                }
            } else {
                try {
                    driver.findElement(By.xpath(xpath))
                        .click()
                    elementFoundOrClicked = true
                    Thread.sleep(10000)
                } catch (e: Exception) {
                }
            }
            if (elementFoundOrClicked) {
                return XpathElementFound(true, xpath)
            } else {
                if (!iter.hasNext()) {
                    // last item
                    throw Exception("couldn't find any element in list of possible xpaths")
                }
            }
        }
        return XpathElementFound(false)
    }

    public enum class SharedPostStrategy {
        CLICK_ON_SHARED_POSTS, USE_SHARED_ENDPOINT, COMMENTS_OF_POSTS
    }

    // TODO stupid workaround until I figure out how to query for a single post using restFb
    fun openPost(restFbPost: com.restfb.types.Post, strategies: List<SharedPostStrategy>) {
        val facebook4jPost: Post = facebook.getPost(restFbPost.id)
        openPost(facebook4jPost, strategies)
    }
    fun openPost(post: Post, strategies: List<SharedPostStrategy>) {

        run breaking@ {
            strategies.forEach {

                val id = post.id.substringAfter("_")
                when (it) {
                    SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                        SharedPostStrategy.COMMENTS_OF_POSTS    -> driver["https://www.facebook.com/Kuba.Dobrowolski.Nowakowski/posts/$id"]
                    SharedPostStrategy.USE_SHARED_ENDPOINT      -> driver["https://www.facebook.com/shares/view?id=$id"]
                }
                Thread.sleep(5000)

//        scalePage(55)

                if (it == SharedPostStrategy.CLICK_ON_SHARED_POSTS) {
                    logger.info("\t\ttrying to click link to shared posts using ${it.name} strategy")
                    try {
                        clickElementIfOneInListExists(
                            listOf(
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[5]/div/div/div[1]/div/div[1]/div/div[2]/div[3]/span/div",
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[5]/div/div/div[1]/div/div[1]/div/div[3]/div[2]/span/div",
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[1]/div/div[1]/div/div[3]/div[2]/span/div",
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[1]/div/div[1]/div/div[2]/div[3]/span/div",
                            ), true
                        )
                    } catch (exception: Exception) {
                        logger.info("\t\tcouldn't click link to shared posts using ${it.name} strategy")
                        return@forEach
                    }
                }

                if (it == SharedPostStrategy.COMMENTS_OF_POSTS) {
                    logger.info("\t\ttrying to click Most relevant")
                    try {
                        clickElementIfOneInListExists(
                            listOf(
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[2]/div/div",
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[2]/div/div",
                            ), true
                        )
                        logger.info("\t\ttrying to click and switch to All comments")
                        clickElementIfOneInListExists(
                            listOf(
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div/div[3]",
                            ), true
                        )
                        Thread.sleep(5000)
                    } catch (e: Exception) {
                        logger.info("\t\tdidn't find switch between Most relevant and All coments: $e")
                    }
                }

                val numberOfDaysSincePost: Long = TimeUnit.DAYS.convert(java.util.Date().time - post.createdTime.time, TimeUnit.MILLISECONDS)
                val postAgeCoefficient: Double = (1/((ln((numberOfDaysSincePost).toDouble())).coerceAtLeast(1.0)))
                val minimumAmountOfScrolls: Long = 2
                val maximumAmountOfScrolls = when (it) {
                    SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                        SharedPostStrategy.COMMENTS_OF_POSTS    -> 30 // was 100 but produced too many temporary block of /shares endpoint
                    SharedPostStrategy.USE_SHARED_ENDPOINT      -> 5 // was 150 but produced too many temporary block of /shares endpoint
                }
                // scroll down to bottom of page to load all posts (lazy loading)
                var scrollTimeout: Long = max((maximumAmountOfScrolls *  postAgeCoefficient).toLong(), minimumAmountOfScrolls)
                logger.info("\t\tpost is $numberOfDaysSincePost days old, will scroll maximum of $scrollTimeout times using ${it.name} strategy")
                if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
                    scrollTimeout = 2
                    logger.info("\t\tdeveloper-mode is enabled will only scroll maxium of $scrollTimeout times")
                }
                var previousScrollHeight: Long = -1
                var previousNumberOfSegments: Int = -1
                var currentNumberOfSegments: Int
                var numberOfConfirmations: Long = 0
                var chosenXpathElementFound: XpathElementFound = XpathElementFound(found = false)

                if (it == SharedPostStrategy.CLICK_ON_SHARED_POSTS) {
                    logger.info("\t\ttrying to find element top of modal in ${it.name} strategy")
                    try {
                        chosenXpathElementFound = clickElementIfOneInListExists(
                            listOf(
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]",
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[1]",
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[1]/div",
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div",
                            ), false
                        )
                    } catch (exception: Exception) {
                        logger.info("\t\tcouldn't scroll to load all posts using ${it.name} strategy")
                        return@forEach
                    }
                }
                for (scrollNumber in 1..scrollTimeout) {
                    // TODO after every page_down or tab check if there's modal about temporarily blocked feature and then switch to next strategy
                    // for /shares/ strategy this decetcs it: driver.pageSource.contains("You’re Temporarily Blocked")
                    when (it) {
                        SharedPostStrategy.CLICK_ON_SHARED_POSTS    -> driver.findElement(By.xpath(chosenXpathElementFound.xpath))
                            .sendKeys(Keys.PAGE_DOWN)

                        SharedPostStrategy.USE_SHARED_ENDPOINT,
                            SharedPostStrategy.COMMENTS_OF_POSTS    -> driver.findElement(By.cssSelector("body"))
                            .sendKeys(Keys.PAGE_DOWN)
                    }

                    Thread.sleep(5000) // was 500 but on rare occasion wasn't enough time to load ajax response with new posts. now much longer to also try avoid temporary block of /shares endpoint
                    // TODO below works for shared enpoint strategy but for click on shared posts return document.body.scrollHeight isn't probably lenth of modal, check if xpath lenght will be enough
                    val currentScrollHeight: Long = js.executeScript("return document.body.scrollHeight") as Long
                    if (currentScrollHeight <= previousScrollHeight) {
                        currentNumberOfSegments = driver.pageSource.split("<a aria-label=\"").size
                        if (currentNumberOfSegments <= previousNumberOfSegments) {
                            numberOfConfirmations++
                            if (numberOfConfirmations > 2) {
                                logger.info("\t\treached bottom of the page after ${scrollNumber}th time out of $scrollTimeout tries using ${it.name} strategy")
                                break
                            }
                        } else {
                            numberOfConfirmations = 0
                        }
                        previousNumberOfSegments = currentNumberOfSegments
                    }
                    previousScrollHeight = currentScrollHeight
                    if (scrollNumber % 20 == 0L) {
                        // should be debugged but can't set netty to info then
                        logger.debug("\t\tscrolling for ${scrollNumber}th time out of $scrollTimeout tries using ${it.name} strategy")
                    }
                }

                // scroll to the top of page (focus is still at the bottom)
                when (it) {
                    SharedPostStrategy.CLICK_ON_SHARED_POSTS -> for (scrollNumber in 1..scrollTimeout) {
                        driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]"))
                            .sendKeys(Keys.PAGE_UP)
                        Thread.sleep(5000) // was 500 but slowing down to not get banned
                    }

                    SharedPostStrategy.USE_SHARED_ENDPOINT,
                        SharedPostStrategy.COMMENTS_OF_POSTS    -> js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
                }
                Thread.sleep(5000) // was 500 but slowing down to not get banned

                var pageSource: String
                val indexOfSharedPostsHeading = when (it) {
                    SharedPostStrategy.CLICK_ON_SHARED_POSTS    -> driver.pageSource.indexOf("People who shared this")
                    SharedPostStrategy.USE_SHARED_ENDPOINT      -> driver.pageSource.indexOf("Shared with Public</title>")
                    SharedPostStrategy.COMMENTS_OF_POSTS        -> 0
                }
                if (indexOfSharedPostsHeading > -1) {
                    if (facebookProperties.getProperty("username").contains("kuba")) {
                        // send tab from like of first post should bring back focus to the top
                        logger.info("\t\ttrying to press Tab on like in first post")
                        try {
                            when (it) {
                                SharedPostStrategy.CLICK_ON_SHARED_POSTS -> clickElementIfOneInListExists(
                                    listOf(
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                                    ), false
                                )

                                SharedPostStrategy.USE_SHARED_ENDPOINT -> clickElementIfOneInListExists(
                                    listOf(
                                        "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",
                                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",
                                    ), false
                                )

                                SharedPostStrategy.COMMENTS_OF_POSTS -> null
                            }
                        } catch (exception: Exception) {
                            logger.error("\t\tcouldn't press Tab on like in first post using ${it.name} strategy")
                            return@forEach
                        }


                        // TODO check if locale of accounts are different and this causes below
                        pageSource = when(it) {
                            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                                SharedPostStrategy.USE_SHARED_ENDPOINT  -> driver.pageSource.substringAfter("People who shared this")
                            SharedPostStrategy.COMMENTS_OF_POSTS        -> driver.pageSource.substringAfter("aria-label=\"Comment by ")
                        }

                    } else {
                        // send tab from like of first post should bring back focus to the top
                        logger.info("\t\ttrying to press Tab on like in first post")
                        clickElementIfOneInListExists(
                            listOf(
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",        // like button
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",           // like button
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[3]/div",               // share button
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",                                     // like button
                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[1]/div/div[2]/div/div[3]/div",                                        // share button
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",                                                   // like button
                                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div/div/div[2]/div",                                                      // share button
                            )
                        )

                        // TODO check if locale of accounts are different and this causes below - UK?
                        pageSource = when(it) {
                            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                            SharedPostStrategy.USE_SHARED_ENDPOINT      -> driver.pageSource.substringAfter("People Who Shared This")
                            SharedPostStrategy.COMMENTS_OF_POSTS        -> driver.pageSource.substringAfter("aria-label=\"Comment by ")
                        }
                    }

                    if (it == SharedPostStrategy.CLICK_ON_SHARED_POSTS ||
                        it == SharedPostStrategy.USE_SHARED_ENDPOINT) {
                        pageSource = pageSource
                            .substringAfter("Enlarge")  // for video
                            .replace("<a aria-label=\"May be an image of", "")
                            .replace("<a aria-label=\"Home", "")
                            .replace("<a aria-label=\"Watch", "")
                            .replace("<a aria-label=\"Groups", "")
                            .replace("<a aria-label=\"Gaming", "")
                            .replace("<a aria-label=\"Events", "")
                            .replace("<a aria-label=\"More", "")
                            .replace("<a aria-label=\"Notifications", "")
                            .replace("<a aria-label=\"Messenger", "")
                            .replace("<a aria-label=\"Messenger", "")
                            .replace("<a aria-label=\"Click to view attachment", "")
                            .replace("<a aria-label=\"Open reel in Reels Viewer", "")
                            .replace("<a aria-label=\"See owner profile", "")
                            .replace("<a aria-label=\"https://", "")
                            .replace("<a aria-label=\"Boost post", "")
                            .substringAfter("<a aria-label=\"")
                    }

                    val totalPostNumber = when(it) {
                        SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                            SharedPostStrategy.USE_SHARED_ENDPOINT  -> pageSource.split("<a aria-label=\"").size
                        SharedPostStrategy.COMMENTS_OF_POSTS        -> pageSource.split("aria-label=\"Comment by ").size
                    }

                    var postNumber = 1
                    while (true) {
                        // TODO fix edge case for last comment
                        val nextPostStartPosition: Int = when(it) {
                            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                                SharedPostStrategy.USE_SHARED_ENDPOINT  -> pageSource.indexOf("<a aria-label=\"")
                            SharedPostStrategy.COMMENTS_OF_POSTS        -> pageSource.indexOf("aria-label=\"Comment by ")
                        }
                        if (nextPostStartPosition == -1) {
                            return@breaking
                        }
                        //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to accessed post made by others
                        val postSource: String = when(it) {
                            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                                SharedPostStrategy.USE_SHARED_ENDPOINT  -> pageSource.substringBefore("<a aria-label=\"")
                            SharedPostStrategy.COMMENTS_OF_POSTS        -> pageSource.substringBefore("aria-label=\"Comment by ")
                        }
                        val postAuthor: String = when(it) {
                            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
                                SharedPostStrategy.USE_SHARED_ENDPOINT  -> postSource.substringBefore("\"")
                            SharedPostStrategy.COMMENTS_OF_POSTS        -> postSource.substringBefore(" ago\"")
                        }
                        if (doesStringContainAnySubstringInList(
                                postSource, listOf(
                                    "Write a comment",
                                    "Write a public comment",
                                    "Submit your first comment",
                                )
                            ) || it == SharedPostStrategy.COMMENTS_OF_POSTS
                        ) {
                            // can be commented
                            logger.debug("\t\tshared post $postNumber/$totalPostNumber written by $postAuthor can be commented")
                            val adminUsernamePosition = postSource.indexOf("Dobrowolski-Nowakowski")
                            if (adminUsernamePosition == -1) {
                                // no comment from admin of fan page
                                logger.debug("\t\t\tpost doesn't contain admin response")
                                val replyMessage: String = FacebookReplies.randomizeThankYouReply(false)
                                logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                                if (it == SharedPostStrategy.COMMENTS_OF_POSTS) {
                                    logger.info("\t\t\ttrying to click Reply button to reveal text box")
                                    clickElementIfOneInListExists(
                                        listOf(
                                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div[2]/div[2]/div[2]/ul/li[3]/div/div",
                                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[2]/ul/li[3]/div/div",
                                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[2]/ul/li[3]/div/div",
                                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[3]/ul/li[3]/div/div",
                                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[3]/ul/li[3]/div/div"
                                        ), true
                                    )
                                }

                                logger.info("\t\t\ttrying to press Tab comment text box")
                                var commentTextFieldPossibleXpaths: XpathElementFound = XpathElementFound(found = false)
                                try {
                                    commentTextFieldPossibleXpaths = when (it) {
                                        SharedPostStrategy.CLICK_ON_SHARED_POSTS -> clickElementIfOneInListExists(
                                                listOf(
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div[2]/div/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                ), false
                                            )

                                        SharedPostStrategy.USE_SHARED_ENDPOINT -> clickElementIfOneInListExists(
                                                listOf(
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[4]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[4]/div[2]/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[2]/div/div[2]/div[1]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[3]/div/div[2]/div[1]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[4]/div/div[2]/div[1]/form/div/div[1]/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]",
                                                    "/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$postNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div[1]/div[1]/div/div[1]",
                                                ), false
                                            )

                                        SharedPostStrategy.COMMENTS_OF_POSTS -> clickElementIfOneInListExists(
                                            listOf(
                                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div[2]/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div",
                                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div",
                                                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div[2]/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div",
                                            ), false
                                        )
                                    }

                                } catch (exception: Exception) {
                                    // TODO add counter of failed comments, add some id's so they could be identified for debug later
                                    logger.error("\t\t\tcouldn't click on comment text box using ${it.name} strategy")
                                    // TODO repeat with below, move to function
                                    postNumber++
                                    pageSource = removeTopPostSourceCode(it, pageSource)
                                    continue
                                }

                                try {
                                    if (facebookProperties.getProperty("username").contains("kuba")) {
                                        // chrome
//                              driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                                    .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                                        // firefox
                                        for (letter in replyMessage.replace("\n", " ")) {
                                            driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                                .sendKeys(letter.toString())
                                            Thread.sleep(500) // was 50 but slowing down to not get banned
                                        }
                                        Thread.sleep(500)
                                        driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                            .sendKeys(Keys.RETURN)

                                        // TODO check source for "Unable to post comment." because it's there when there is a ban

                                        commentedPosts++
                                    } else {
                                        // firefox
                                        for (letter in replyMessage.replace("\n", " ")) {
                                            driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                                .sendKeys(letter.toString())
                                            Thread.sleep(500) // was 50 but slowing down to not get banned
                                        }
                                        Thread.sleep(500)
                                        driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                            .sendKeys(Keys.RETURN)

                                        commentedPosts++
                                    }
                                } catch (e: NoSuchElementException) {
                                    logger.error(e.message)
                                    logger.error("NoSuchElementException exception has been thrown during processing of $id post on $postNumber post written by $postAuthor")
                                } catch (e: Exception) {
                                    logger.error(e.message)
                                    logger.error("Exception exception has been thrown during processing of $id post on $postNumber post written by $postAuthor")
                                }

                                val numberOfSeconds: Long = (10..120).random().toLong()
                                logger.info("\t\t\tsleeping for $numberOfSeconds seconds\n")
                                Thread.sleep(1000 * numberOfSeconds)

                                tabToNextPost(it)
                            } else {
                                logger.debug("\t\t\tpost contains admin response")
                                tabToNextPost(it)
                            }
                        } else {
                            logger.debug("\t\tshared post $postNumber/$totalPostNumber written by $postAuthor can't be commented")
                            tabToNextPost(it)
                        }

                        postNumber++
                        pageSource = removeTopPostSourceCode(it, pageSource)
                    }
                } else {
                    logger.info("\t\tpost doesn't have any shared posts")
                }
            }
        }
    }

    private fun removeTopPostSourceCode(it: SharedPostStrategy, pageSource: String) = when (it) {
        SharedPostStrategy.CLICK_ON_SHARED_POSTS,
        SharedPostStrategy.USE_SHARED_ENDPOINT -> pageSource.substringAfter("<a aria-label=\"")

        SharedPostStrategy.COMMENTS_OF_POSTS -> pageSource.substringAfter("aria-label=\"Comment by ")
    }

    private fun tabToNextPost(it: SharedPostStrategy) {
        when (it) {
            SharedPostStrategy.CLICK_ON_SHARED_POSTS,
            SharedPostStrategy.USE_SHARED_ENDPOINT -> tabUntilGivenLabelIsFocussed(
                "aria-label",
                "Send this to friends or post it on your profile."
            )

            SharedPostStrategy.COMMENTS_OF_POSTS -> tabUntilGivenLabelIsFocussed(
                "aria-label",
                "Like"
            )
        }
    }

    /*
    // TODO stupid workaround until I figure out how to query for a single post using restFb
    fun openPostComments(restFbPost: com.restfb.types.Post) {
        val facebook4jPost: Post = facebook.getPost(restFbPost.id)
        openPostComments(facebook4jPost)
    }

    fun openPostComments(post: Post) {

        val id = post.id.substringAfter("_")
        driver["https://www.facebook.com/Kuba.Dobrowolski.Nowakowski/posts/$id"]
        Thread.sleep(5000)

        logger.info("\t\ttrying to click Most relevant")
        try {
            clickElementIfOneInListExists(
                listOf(
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[2]/div/div",
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[2]/div/div",
                ), true
            )
            logger.info("\t\ttrying to click and switch to All comments")
            clickElementIfOneInListExists(
                listOf(
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div/div[3]",
                ), true
            )
            Thread.sleep(5000)
        } catch (e: Exception) {
            logger.info("\t\tdidn't find switch between Most relevant and All coments: $e")
        }

        // TODO if below can be reused with function above
        val numberOfDaysSincePost: Long = TimeUnit.DAYS.convert(java.util.Date().time - post.createdTime.time, TimeUnit.MILLISECONDS)
        val postAgeCoefficient: Double = (1/((ln((numberOfDaysSincePost).toDouble())).coerceAtLeast(1.0)))
        val minimumAmountOfScrolls: Long = 2
        val maximumAmountOfScrolls = 30 // was 100 but produced too many temporary block of /shares endpoint
        // scroll down to bottom of page to load all posts (lazy loading)
        var scrollTimeout: Long = max((maximumAmountOfScrolls *  postAgeCoefficient).toLong(), minimumAmountOfScrolls)
        logger.info("\t\tpost is $numberOfDaysSincePost days old, will scroll maximum of $scrollTimeout times")
        if (facebookProperties.getProperty("developer-mode-enabled") == "true") {
            scrollTimeout = 2
            logger.info("\t\tdeveloper-mode is enabled will only scroll maxium of $scrollTimeout times")
        }
        var previousScrollHeight: Long = -1
        var previousNumberOfSegments: Int = -1
        var currentNumberOfSegments: Int
        var numberOfConfirmations: Long = 0

        for (scrollNumber in 1..scrollTimeout) {
            // TODO after every page_down or tab check if there's modal about temporarily blocked feature and then switch to next strategy
            // for /shares/ strategy this decetcs it: driver.pageSource.contains("You’re Temporarily Blocked")
            driver.findElement(By.cssSelector("body"))
                .sendKeys(Keys.PAGE_DOWN)

            Thread.sleep(5000) // was 500 but on rare occasion wasn't enough time to load ajax response with new posts. now much longer to also try avoid temporary block of /shares endpoint
            // TODO below works for shared enpoint strategy but for click on shared posts return document.body.scrollHeight isn't probably lenth of modal, check if xpath lenght will be enough
            val currentScrollHeight: Long = js.executeScript("return document.body.scrollHeight") as Long
            if (currentScrollHeight <= previousScrollHeight) {
                currentNumberOfSegments = driver.pageSource.split("<a aria-label=\"").size
                if (currentNumberOfSegments <= previousNumberOfSegments) {
                    numberOfConfirmations++
                    if (numberOfConfirmations > 2) {
                        logger.info("\t\treached bottom of the page after ${scrollNumber}th time out of $scrollTimeout tries")
                        break
                    }
                } else {
                    numberOfConfirmations = 0
                }
                previousNumberOfSegments = currentNumberOfSegments
            }
            previousScrollHeight = currentScrollHeight
            if (scrollNumber % 20 == 0L) {
                // should be debugged but can't set netty to info then
                logger.debug("\t\tscrolling for ${scrollNumber}th time out of $scrollTimeout tries")
            }
        }

        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
        Thread.sleep(5000) // was 500 but slowing down to not get banned
        //TODO end of if below can be reused with function above

        //driver.pageSource.substringAfter("shares</span>")
        var pageSource = driver.pageSource.substringAfter("aria-label=\"Comment by ")

        val totalPostNumber = pageSource.split("aria-label=\"Comment by ").size
        var postNumber = 1
        while (true) {
            // TODO fix edge case for last comment
            val nextPostStartPosition: Int = pageSource.indexOf("aria-label=\"Comment by ")
            if (nextPostStartPosition == -1) {
                return
            }
            val postSource: String = pageSource.substringBefore("aria-label=\"Comment by ")
            val postAuthor: String = postSource.substringBefore(" ago\"")

            logger.debug("\t\tcomment under post $postNumber/$totalPostNumber written by $postAuthor can be commented")
            val adminUsernamePosition = postSource.indexOf("Dobrowolski-Nowakowski")
            if (adminUsernamePosition == -1) {
                // no comment from admin of fan page
                logger.debug("\t\t\tcomment doesn't contain admin response")
                val replyMessage: String = FacebookReplies.randomizeThankYouReply(false)
                logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                logger.info("\t\t\ttrying to click Reply button to reveal text box")
                clickElementIfOneInListExists(
                    listOf(
                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div[2]/div[2]/div[2]/ul/li[3]/div/div",
                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[2]/ul/li[3]/div/div",
                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[2]/ul/li[3]/div/div",
                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[3]/ul/li[3]/div/div",
                        "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[1]/div/div[2]/div[3]/ul/li[3]/div/div"
                    ), true
                )

                logger.info("\t\t\ttrying to press Tab comment text box")
                var commentTextFieldPossibleXpaths: XpathElementFound = XpathElementFound(found = false)
                commentTextFieldPossibleXpaths = clickElementIfOneInListExists(
                        listOf(
                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div[2]/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div",
                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[5]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div",
                            "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[13]/div/div/div[4]/div/div/div[2]/div[3]/div[${postNumber+1}]/div/div/div/div[2]/div/div/div/div/div/div[2]/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div",
                        ), false
                    )

                try {
                    if (facebookProperties.getProperty("username").contains("kuba")) {
                        // chrome
//                              driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[5]/div/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[$commentNumber]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[2]/div[5]/div/div[2]/div[1]/form/div/div/div[1]/div/div[1]"))
//                                    .sendKeys(replyMessage.replace("\n", Keys.chord(Keys.SHIFT, Keys.ENTER)))
                        // firefox
                        for (letter in replyMessage.replace("\n", " ")) {
                            driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                .sendKeys(letter.toString())
                            Thread.sleep(500) // was 50 but slowing down to not get banned
                        }
                        Thread.sleep(500)
                        driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                            .sendKeys(Keys.RETURN)

                        // TODO check source for "Unable to post comment." because it's there when there is a ban

                        commentedPosts++
                    } else {
                        // firefox
                        for (letter in replyMessage.replace("\n", " ")) {
                            driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                .sendKeys(letter.toString())
                            Thread.sleep(500) // was 50 but slowing down to not get banned
                        }
                        Thread.sleep(500)
                        driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                            .sendKeys(Keys.RETURN)

                        commentedPosts++
                    }
                } catch (e: NoSuchElementException) {
                    logger.error(e.message)
                    logger.error("NoSuchElementException exception has been thrown during processing of $id post on $postNumber post written by $postAuthor")
                } catch (e: Exception) {
                    logger.error(e.message)
                    logger.error("Exception exception has been thrown during processing of $id post on $postNumber post written by $postAuthor")
                }

                val numberOfSeconds: Long = (10..120).random().toLong()
                logger.info("\t\t\tsleeping for $numberOfSeconds seconds\n")
                Thread.sleep(1000 * numberOfSeconds)

                // TODO at some point it focuses new post icon at the bottom and loops. maybe need to tab by some div
                tabUntilGivenLabelIsFocussed(
                    "aria-label",
                    "Like"
                )
            } else {
                logger.debug("\t\t\tcomment contains admin response")
                // TODO at some point it focuses new post icon at the bottom and loops. maybe need to tab by some div
                tabUntilGivenLabelIsFocussed(
                    "aria-label",
                    "Like"
                )
            }
            postNumber++
            pageSource = pageSource.substringAfter("aria-label=\"Comment by ")
        }
    }
*/

    public fun checkIfNewAdPostHasBeenAdded(fanPagePostIds: List<String>) {

        logger.info("will be checking for new ad post id's")
        if (facebookProperties.getProperty("username").contains("kuba")) {
            driver["https://business.facebook.com/latest/inbox/facebook?asset_id=${facebook4jProperties.getProperty("fanpage.id")}&mailbox_id=${facebook4jProperties.getProperty("fanpage.id")}&selected_item_id=355255717411408&thread_type=FB_AD_POST"]
        } else {
            // TODO hardcoded business_id
            driver["https://business.facebook.com/latest/inbox/facebook?asset_id=${facebook4jProperties.getProperty("fanpage.id")}&business_id=876903966691457&mailbox_id=${facebook4jProperties.getProperty("fanpage.id")}&selected_item_id=355255717411408&thread_type=FB_AD_POST"]
        }
        Thread.sleep(5000)

        val file = File(System.getProperty("user.dir") + "/src/main/resources/adPosts.txt")
        val totalNumberOfComments = driver.pageSource.split("bottom: -1px; right: -1px;").size - 1
        logger.info("\tthere're $totalNumberOfComments comments")
        var fanPagePosts = 0
        var adPostsAlreadyExisted = 0
        var adPostsDidntExist = 0
        for (i in 1..totalNumberOfComments) {
            logger.info("\t\ttrying to click on comment $i/$totalNumberOfComments")
            // since we're going from top inbox message and we're dismissing it afterwards next inbox message will be always on the top
            clickElementIfOneInListExists(listOf("/html/body/div[1]/div/div[1]/div/div[2]/div/div/div[1]/div[1]/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div[1]/div/div/div/div/div[2]/div/div/div/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/div/div/div/div/div/div[1]/div"))

            val postId =
                driver.pageSource.substringAfter("href=\"https://www.facebook.com/${facebook4jProperties.getProperty("fanpage.name")}/posts/")
                    .substringBefore("?")

            val postIdWithFanpagePrefix: String = facebook4jProperties.getProperty("fanpage.id") + "_" + postId
            val shortPostId : String? = adPostsProcessor.getShortId(postIdWithFanpagePrefix)
            val postText: String = driver.pageSource.replaceBeforeLast("role=\"heading\">","").replaceAfter("</","").replaceBefore(">","").replaceAfter("<","").replace("<","").replace(">","")
            logger.info("\t\tgot post notification [${postText.substring(0, min(postText.length, 30))}...] shortPostId=$shortPostId longPostId=$postId")

            // check if post is fanpage post or ad post
            if (!fanPagePostIds.contains(shortPostId)) {
                // post is an ad post, not fanpage post
                logger.info("\t\t\tpost is an ad post")
                // check if ad post is already in the list
                if (!BufferedReader(FileReader(file)).lines().toList()
                        .contains(shortPostId)
                ) {
                    adPostsDidntExist++
                    // ad post list doesn't contain post id
                    logger.info("\t\t\tad post is not in our list, trying to add")
                    val output: OutputStream = FileOutputStream(file, true)
                    output.write((shortPostId + System.lineSeparator()).toByteArray())
                    output.close()
                } else {
                    adPostsAlreadyExisted++
                    logger.info("\t\t\tad post is already on our list")
                }
            } else {
                fanPagePosts++
                logger.info("\t\t\tpost is fanpage post, moving on")
            }

            logger.info("\t\t\ttrying to click on move to done")
            val builder = Actions(driver)
            try {
//                "/html/body/div[1]/div/div[1]/div/div[2]/div/div/div[1]/div[1]/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div[1]/div/div/div/div/div[2]/div/div/div/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/div/div/div/div/div/div[$i]/div/div/div[2]/div[2]/div[2]/div[1]/a",
                builder.moveToElement(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div/div/div[1]/div[1]/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div[1]/div/div/div/div/div[2]/div/div/div/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/div/div/div/div/div/div[1]/div/div[1]/div[2]/div[2]/div[2]/div[2]/a")))
                builder.click(driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[2]/div/div/div[1]/div[1]/div/div[1]/div[1]/div/div/div/div/div/div/div[1]/div[1]/div/div/div/div/div[2]/div/div/div/div/div[1]/div[2]/div[2]/div[1]/div/div[2]/div[1]/div/div/div/div/div/div[1]/div/div[1]/div[2]/div[2]/div[2]/div[2]/a")))
                builder.release()
                builder.perform()
            } catch (e: Exception) {
                logger.error(e.toString())
            }
        }
        logger.info("got $fanPagePosts fan page posts, $adPostsAlreadyExisted ad posts that were already added, $adPostsDidntExist ad posts that were new")
    }

    private fun doesStringContainAnySubstringInList(postSource: String, substringList: List<String>): Boolean {

        for (substring: String in substringList) {
            if (postSource.indexOf(substring) > -1) {
                return true
            }
        }
        return false
    }

    private fun scalePage(scale: Int) {
        // https://github.com/SeleniumHQ/selenium/issues/4244#issuecomment-371533758
        js.executeScript("document.body.style.MozTransform = \"scale(0.$scale)\";")
        js.executeScript("document.body.style.MozTransformOrigin = \"0 0\";")
    }
}