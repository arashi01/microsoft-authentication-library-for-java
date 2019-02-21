package com.microsoft.aad.msal4j;

import Infrastructure.SeleniumExtensions;
import lapapi.LabResponse;
import lapapi.LabUser;
import lapapi.LabUserProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

@Test
public class DeviceCodeIT {

    private final static Logger LOG = LoggerFactory.getLogger(DeviceCodeIT.class);

    private LabUserProvider labUserProvider;
    private WebDriver seleniumDriver;

    @BeforeClass
    public void setUp(){
        labUserProvider = new LabUserProvider();
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    @Test
    public void DeviceCodeFlowTest() throws MalformedURLException, InterruptedException,
            ExecutionException {
        LabResponse labResponse = labUserProvider.getDefaultUser();
        labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();

        DeviceCode deviceCode = pca.acquireDeviceCode(TestConstants.GRAPH_DEFAULT_SCOPE).get();
        runAutomatedDeviceCodeFlow(deviceCode, labResponse.getUser());
        AuthenticationResult result = pca.acquireTokenByDeviceCode(deviceCode).get();

        Assert.assertNotNull(result);
        Assert.assertTrue(!Strings.isNullOrEmpty(result.getAccessToken()));
    }

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, LabUser user){
        try{

            LOG.info("Loggin in ... Entering device code");
            seleniumDriver.navigate().to(deviceCode.getVerificationUrl());
            seleniumDriver.findElement(new By.ById("otc")).sendKeys(deviceCode.getUserCode());
            LOG.info("Loggin in ... click continue");
            WebElement continueBtn = SeleniumExtensions.waitForElementToBeVisibleAndEnable(
                   seleniumDriver,
                   new By.ById("idSIButton9"));
            continueBtn.click();

            SeleniumExtensions.performLogin(seleniumDriver, user);
        } catch(Exception e){
            LOG.error("Browser automation failed: " + e.getMessage());
            throw new RuntimeException("Browser automation failed: " + e.getMessage());
        }
    }

    @AfterClass
    public void cleanUp(){
        if( seleniumDriver != null){
            seleniumDriver.close();
        }
    }
}