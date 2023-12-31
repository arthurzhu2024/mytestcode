package com.saucelabs.appium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSElement;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * <a href="https://github.com/appium/appium">Appium</a> test which runs against a local Appium instance deployed
  * with the 'UICatalog' iPhone project which is included in the Appium source distribution.
 *
 * @author Ross Rowe
 * 
 * Running below Test Cases on Simulator needs few steps as below
 * Unzip UICatalog.zip and build for simulator per below blog
 * http://samwize.com/2015/03/11/xcode-commands-to-build-app-and-run-on-simulator/
 */
@SuppressWarnings("deprecation")
public class UICatalogTest {

    private AppiumDriver<IOSElement> driver;

    private WebElement row;

    @Before
    public void setUp() throws Exception {
        // set up appium
        File classpathRoot = new File(System.getProperty("user.dir"));
        File appDir = new File(classpathRoot, "../../../apps/UICatalog/build/release-iphonesimulator");
        File app = new File(appDir, "UICatalog.app");
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformVersion", "9.3");
        capabilities.setCapability("deviceName", "iPhone 6");
        capabilities.setCapability("app", app.getAbsolutePath());
        driver = new IOSDriver<>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

    private void openMenuPosition(int index) {
        //populate text fields with two random number
        MobileElement table = (MobileElement)driver.findElementByClassName("UIATableView");
        row = table.findElementsByClassName("UIATableCell").get(index);
        row.click();
    }

    private Point getCenter(WebElement element) {

      Point upperLeft = element.getLocation();
      Dimension dimensions = element.getSize();
      return new Point(upperLeft.getX() + dimensions.getWidth()/2, upperLeft.getY() + dimensions.getHeight()/2);
    }

    @Test
    public void testFindElement() throws Exception {
        //first view in UICatalog is a table
        IOSElement table = driver.findElementByClassName("UIATableView");
        assertNotNull(table);
        //is number of cells/rows inside table correct
        List<MobileElement> rows = table.findElementsByClassName("UIATableCell");
        assertEquals(18, rows.size());
        //is first one about buttons
        assertEquals("Action Sheets", rows.get(0).getAttribute("name"));
        //navigationBar is not inside table
        WebElement nav_bar = null;
        try {
            nav_bar = table.findElementByClassName("UIANavigationBar");
        } catch (NoSuchElementException e) {
            //expected
        }
        assertNull(nav_bar);
        //there is nav bar inside the app
        driver.getPageSource();
        nav_bar = driver.findElementByClassName("UIANavigationBar");
        assertNotNull(nav_bar);
    }


    @Test
    public void test_location() {
        //get third row location
        row = driver.findElementsByClassName("UIATableCell").get(2);
        assertEquals(0, row.getLocation().getX());
        assertEquals(152, row.getLocation().getY());
    }

    @Test
    public void testScreenshot() {
        //make screenshot and get is as base64
        WebDriver augmentedDriver = new Augmenter().augment(driver);
        String screenshot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BASE64);

        assertNotNull(screenshot);
        //make screenshot and save it to the local filesystem
        File file = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.FILE);
        assertNotNull(file);
    }

    @Test
    public void testAlertInteraction() {
        //go to the alerts section
        openMenuPosition(2);

        //trigger modal alert with cancel & ok buttons
        List<IOSElement> triggerOkCancel = driver.findElementsByXPath("//UIATableCell[2]/UIAStaticText[1]");
        triggerOkCancel.get(0).click();
        Alert alert = driver.switchTo().alert();
        //check if title of alert is correct
        assertEquals("A Short Title Is Best A message should be a short, complete sentence.", alert.getText());
        alert.accept();
    }

    @Test
    public void testScroll() {
        //scroll menu
        //get initial third row location
        row = driver.findElementsByClassName("UIATableCell").get(2);
        Point location1 = row.getLocation();
        Point center = getCenter(row);
        //perform swipe gesture
        driver.swipe(center.getX(), center.getY(), center.getX(), center.getY()-20, 1);
        //get new row coordinates
        Point location2 = row.getLocation();
        assertEquals(location1.getX(), location2.getX());
        assertNotSame(location1.getY(), location2.getY());
    }

    @Test
    public void testSlider() {
      //go to controls
      openMenuPosition(10);
      //get the slider
      WebElement slider = driver.findElementByClassName("UIASlider");
      assertEquals("42%", slider.getAttribute("value"));
      Point sliderLocation = getCenter(slider);
      driver.swipe(sliderLocation.getX(), sliderLocation.getY(), sliderLocation.getX()-sliderLocation.getX(), sliderLocation.getY(), 1);
      assertEquals("0%", slider.getAttribute("value"));
    }

    @Test
    public void testSessions() throws Exception {
      HttpGet request = new HttpGet("http://localhost:4723/wd/hub/sessions");
      @SuppressWarnings("resource")
	  HttpClient httpClient = new DefaultHttpClient();
      HttpResponse response = httpClient.execute(request);
      HttpEntity entity = response.getEntity();
      JSONObject jsonObject = (JSONObject) new JSONParser().parse(EntityUtils.toString(entity));

      JSONArray lang= (JSONArray) jsonObject.get("value");
      JSONObject innerObj = (JSONObject) lang.iterator().next();
      
      String sessionId = driver.getSessionId().toString();
      assertEquals(innerObj.get("id"), sessionId);
    }

    @Test
    public void testSize() {
        Dimension table = driver.findElementByClassName("UIATableView").getSize();
        Dimension cell = driver.findElementsByClassName("UIATableCell").get(0).getSize();
        assertEquals(table.getWidth(), cell.getWidth());
        assertNotSame(table.getHeight(), cell.getHeight());
    }

    @Test
    public void testSource() {
        //get main view soruce
        String source_main = driver.getPageSource();
        assertTrue(source_main.contains("UIATableView"));
        assertTrue(source_main.contains("Text Fields"));

        //got to text fields section
        openMenuPosition(13);
        String source_textfields = driver.getPageSource();
        //assertTrue(source_textfields.contains("UIAStaticText"));
        assertTrue(source_textfields.contains("UIATextField"));

        assertNotSame(source_main, source_textfields);
    }
}
