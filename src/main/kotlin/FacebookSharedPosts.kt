import mu.KotlinLogging
import org.openqa.selenium.*
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.FirefoxProfile
import java.io.File
import org.apache.commons.io.FileUtils
import kotlin.NoSuchElementException


class FacebookSharedPosts {

    private var driver: WebDriver
    private val js: JavascriptExecutor
    private val facebookProperties: FacebookProperties = FacebookProperties()
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
            Thread.sleep(5)
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
        driver["https://business.facebook.com/latest/home?asset_id=105161449087504&nav_ref=aymt_reaction_inviter_tip&notif_id=1679842962374398&notif_t=aymt_bizapp_invite_reactors_to_like_page_notif&ref=notif"]
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
        Thread.sleep(1500)
        if (facebookProperties.getProperty("username").contains("kuba")) {
            // account icon
            logger.info("trying to click on account icon")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[2]/div[3]/div[1]/span/div/div[1]",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[1]/span/div/div[1]",
            ))

            Thread.sleep(1000)
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
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[1]/span/div/div[1]",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[1]/span/div/div[1]"
            ))
            Thread.sleep(500)
            // switch profile to fan page
            logger.info("trying to click on switch profile to fan page")
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[2]/div[5]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/div/div[1]/div/div[2]/span/div/div[1]/div[1]/span/div",
                "/html/body/div[1]/div[1]/div[1]/div/div[2]/div[4]/div[2]/div/div[2]/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
                "/html/body/div[1]/div/div[1]/div/div[2]/div[4]/div[2]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div[1]/div/div/div[1]/div[1]/div/a/div[1]/div[3]/span/div",
            ))
        }
        Thread.sleep(2000)
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
        while (iter.hasNext()) {
            xpath = iter.next()
            if (this.canElementBeReachedAndPressTabOnIt(xpath)) {
                if (clickOnElement) {
                    driver.findElement(By.xpath(xpath))
                        .click()
                    Thread.sleep(10000)
                }
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

    fun openSharedPosts(postId: String) {

        val id = postId.substringAfter("_")
        driver["https://www.facebook.com/Kuba.Dobrowolski.Nowakowski/posts/$id"]
        Thread.sleep(5000)

//        scalePage(55)

        try {
            clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[5]/div/div/div[1]/div/div[1]/div/div[2]/div[3]/span/div",
            ), true)
        } catch(exception: Exception) {
            logger.error("\t\tcouldn't click link to shared posts")
            return
        }

        // scroll down to bottom of page to load all posts (lazy loading)
        var scrollTimeout = 100 // was 150 but produced too many temporary block of /shares endpoint
        var previousScrollHeight: Long = -1
        var previousNumberOfSegments: Int = -1
        var currentNumberOfSegments: Int
        var numberOfConfirmations: Long = 0
        var chosenXpathElementFound: XpathElementFound
        try {
            chosenXpathElementFound = clickElementIfOneInListExists(listOf(
                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]",
                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[1]",
                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[1]/div",
                "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div",
            ), false)
        } catch(exception: Exception) {
            logger.error("\t\tcouldn't scroll to load all posts")
            return
        }
        for (scrollNumber in 1..scrollTimeout) {
            // TODO get xpath from above
            driver.findElement(By.xpath(chosenXpathElementFound.xpath)).sendKeys(Keys.PAGE_DOWN)

            Thread.sleep(5000) // was 500 but on rare occasion wasn't enough time to load ajax response with new posts. now much longer to also try avoid temporary block of /shares endpoint
            // TODO return document.body.scrollHeight isn't probably lenth of modal, check if xpath lenght will be enough
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
            if (scrollNumber % 50 == 0) {
                // should be debugged but can't set netty to info then
                logger.info("\t\tscrolling for ${scrollNumber}th time out of $scrollTimeout tries")
            }
        }

        // scroll to the top of page (focus is still at the bottom)
        // TODO return document.body.scrollHeight isn't probably lenth of modal, check if xpath lenght will be enough
//        js.executeScript("window.scrollTo(0, -document.body.scrollHeight)")
//        Thread.sleep(4000)

        for (scrollNumber in 1..scrollTimeout) {
            driver.findElement(By.xpath("/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]")).sendKeys(Keys.PAGE_UP)
        }

        var pageSource: String
        if (driver.pageSource.indexOf("People who shared this") > -1) {
            if (facebookProperties.getProperty("username").contains("kuba")) {
                // send tab from like of first post should bring back focus to the top
                logger.info("\t\ttrying to press Tab on like in first post")
                clickElementIfOneInListExists(listOf(
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div[1]/div[1]/div/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                    "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[3]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                ), false)

                // TODO check if locale of accounts are different and this causes below
                pageSource = driver.pageSource.substringAfter("People who shared this")
            } else {
                // send tab from like of first post should bring back focus to the top
                logger.info("\t\ttrying to press Tab on like in first post")
                clickElementIfOneInListExists(listOf(
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[1]/div[1]",            // like button
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div/div/div[1]/div[1]",               // like button
                    "/html/body/div[1]/div/div[1]/div/div[3]/div/div/div[1]/div[1]/div/div/div/div/div/div/div/div[2]/div[1]/div/div/div/div/div/div/div/div/div/div[8]/div/div/div[4]/div/div/div[1]/div/div[2]/div/div[3]/div",                   // share button
                ))

                // TODO check if locale of accounts are different and this causes below - UK?
                pageSource = driver.pageSource.substringAfter("People Who Shared This")
            }

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
                .replace("<a aria-label=\"Click to view attachment\"", "")
                .substringAfter("<a aria-label=\"")

            val totalPostNumber = pageSource.split("<a aria-label=\"").size
            var postNumber = 1
            while (true) {
                // TODO fix edge case for last comment
                val nextPostStartPosition: Int = pageSource.indexOf("<a aria-label=\"")
                if (nextPostStartPosition == -1) {
                    break
                }
                //pageSource.substringAfter("permalink.php?story_fbid=").substringBefore("&") // id of post, wont' use it since API permission is needed to accessed post made by others
                val postSource: String = pageSource.substringBefore("<a aria-label=\"")
                val postAuthor: String = postSource.substringBefore("\"")
                if (doesStringContainAnySubstringInList(postSource, listOf(
                        "Write a comment",
                        "Write a public comment",
                        "Submit your first comment",
                    ))) {
                    // can be commented
                    logger.debug("\t\tshared post $postNumber/$totalPostNumber written by $postAuthor can be commented")
                    val adminUsernamePosition = postSource.indexOf("Kuba Dobrowolski-Nowakowski")
                    if (adminUsernamePosition == -1) {
                        // no comment from admin of fan page
                        logger.debug("\t\t\tpost doesn't contain admin response")
                        val replyMessage: String = FacebookReplies.randomizeThankYouReply(false)
                        logger.info("\t\t\ttrying replying with '${replyMessage.replace("\n", "")}'")

                        logger.info("\t\t\ttrying to press Tab comment text box")
                        var commentTextFieldPossibleXpaths: XpathElementFound
                        try {
                            commentTextFieldPossibleXpaths = clickElementIfOneInListExists(listOf(
                            "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[1]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div[2]/div/div[2]/form/div/div/div[1]/div/div[1]",
                            "/html/body/div[1]/div/div[1]/div/div[4]/div/div/div[1]/div/div[2]/div/div/div/div[3]/div[1]/div/div[$postNumber]/div/div/div/div/div[4]/div/div/div[2]/div[3]/div[2]/div/div/div/div/div/div/div[2]/form/div/div/div[1]/div/div[1]",

                        ), false)
                        } catch(exception: Exception) {
                            logger.error("\t\t\tcouldn't click on comment text box")
                            // TODO repeat with below, move to function
                            postNumber++
                            pageSource = pageSource.substringAfter("<a aria-label=\"")
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
                                    Thread.sleep(50)
                                }
                                Thread.sleep(500)
                                driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                    .sendKeys(Keys.RETURN)

                                commentedPosts++
                            } else {
                                // firefox
                                for (letter in replyMessage.replace("\n", " ")) {
                                    driver.findElement(By.xpath(commentTextFieldPossibleXpaths.xpath))
                                        .sendKeys(letter.toString())
                                    Thread.sleep(50)
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
                    } else {
                        logger.debug("\t\t\tpost contains admin response")
                        tabUntilGivenLabelIsFocussed("aria-label", "Send this to friends or post it on your profile.")
                    }
                } else {
                    logger.debug("\t\tshared post $postNumber/$totalPostNumber written by $postAuthor can't be commented")
                    tabUntilGivenLabelIsFocussed("aria-label", "Send this to friends or post it on your profile.")
                }

                postNumber++
                pageSource = pageSource.substringAfter("<a aria-label=\"")
            }
        } else {
            logger.info("\t\tpost doesn't have any shared posts")
        }
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