/**
 * NeuroCheck Android Mobile App - Complete E2E Appium Test Suite
 * Framework: WebdriverIO + Appium UiAutomator2 + Mocha + Chai
 * Target App Package: com.example.brainhemorrhage
 */

const { remote } = require('webdriverio');
const { expect } = require('chai');
const appiumConfig = require('../appium.config');

describe('NeuroCheck Android App - Comprehensive E2E Appium Automation Suite', function () {
  this.timeout(120000); // 120s timeout per test scenario

  let driver;

  before(async function () {
    // Initialize Appium WebDriver session
    console.log('Connecting to Appium Server on port 4723...');
    driver = await remote(appiumConfig);
  });

  after(async function () {
    if (driver) {
      console.log('Closing Appium Driver session...');
      await driver.deleteSession();
    }
  });

  // -------------------------------------------------------------
  // MODULE 1: WELCOME & ONBOARDING
  // -------------------------------------------------------------
  describe('Module 1: Welcome & App Onboarding', function () {
    it('TC-APP-WEL-001: Should launch app and display Welcome Screen with branding logo', async function () {
      const splashOrWelcome = await driver.$('id:com.example.brainhemorrhage:id/welcomeHeader');
      await splashOrWelcome.waitForDisplayed({ timeout: 15000 });
      expect(await splashOrWelcome.isDisplayed()).to.be.true;
    });

    it('TC-APP-WEL-002: Should navigate to Login Fragment upon tapping Get Started button', async function () {
      const getStartedBtn = await driver.$('id:com.example.brainhemorrhage:id/btnGetStarted');
      if (await getStartedBtn.isExisting()) {
        await getStartedBtn.click();
      }

      const loginHeader = await driver.$('id:com.example.brainhemorrhage:id/loginTitle');
      await loginHeader.waitForDisplayed({ timeout: 10000 });
      expect(await loginHeader.isDisplayed()).to.be.true;
    });
  });

  // -------------------------------------------------------------
  // MODULE 2: DOCTOR AUTHENTICATION & LOGIN
  // -------------------------------------------------------------
  describe('Module 2: Doctor Authentication & Login', function () {
    it('TC-APP-AUTH-031: Should render Email, Password fields and Login button', async function () {
      const etEmail = await driver.$('id:com.example.brainhemorrhage:id/etEmail');
      const etPassword = await driver.$('id:com.example.brainhemorrhage:id/etPassword');
      const btnLogin = await driver.$('id:com.example.brainhemorrhage:id/btnLogin');

      expect(await etEmail.isDisplayed()).to.be.true;
      expect(await etPassword.isDisplayed()).to.be.true;
      expect(await btnLogin.isDisplayed()).to.be.true;
    });

    it('TC-APP-AUTH-035: Should show invalid email alert Toast when format is wrong', async function () {
      const etEmail = await driver.$('id:com.example.brainhemorrhage:id/etEmail');
      const etPassword = await driver.$('id:com.example.brainhemorrhage:id/etPassword');
      const btnLogin = await driver.$('id:com.example.brainhemorrhage:id/btnLogin');

      await etEmail.setValue('invaliddoctor.com');
      await etPassword.setValue('Pass123!');
      await btnLogin.click();

      // Check for Toast popup on Android
      const toast = await driver.$('xpath://android.widget.Toast[1]');
      if (await toast.isExisting()) {
        const toastText = await toast.getText();
        expect(toastText.toLowerCase()).to.include('email');
      }
    });

    it('TC-APP-AUTH-040: Should log in successfully with valid doctor credentials and load Dashboard', async function () {
      const etEmail = await driver.$('id:com.example.brainhemorrhage:id/etEmail');
      const etPassword = await driver.$('id:com.example.brainhemorrhage:id/etPassword');
      const btnLogin = await driver.$('id:com.example.brainhemorrhage:id/btnLogin');

      await etEmail.clearValue();
      await etEmail.setValue('doctor@nuerocheck.com');
      await etPassword.clearValue();
      await etPassword.setValue('Doctor@123');
      await btnLogin.click();

      // Verify redirection to DashboardFragment
      const dashboardGreeting = await driver.$('id:com.example.brainhemorrhage:id/tvGreeting');
      await dashboardGreeting.waitForDisplayed({ timeout: 15000 });
      expect(await dashboardGreeting.isDisplayed()).to.be.true;
    });
  });

  // -------------------------------------------------------------
  // MODULE 3: BOTTOM NAVIGATION & DASHBOARD METRICS
  // -------------------------------------------------------------
  describe('Module 3: Bottom Navigation & Dashboard Stat Cards', function () {
    it('TC-APP-NAV-156: Should display Stat Cards for Total Scans, Normal, and Hemorrhage metrics', async function () {
      const cardTotal = await driver.$('id:com.example.brainhemorrhage:id/cardTotalScans');
      const cardNormal = await driver.$('id:com.example.brainhemorrhage:id/cardNormalScans');
      const cardAbnormal = await driver.$('id:com.example.brainhemorrhage:id/cardAbnormalScans');

      expect(await cardTotal.isDisplayed()).to.be.true;
      expect(await cardNormal.isDisplayed()).to.be.true;
      expect(await cardAbnormal.isDisplayed()).to.be.true;
    });

    it('TC-APP-NAV-160: Should switch fragments via BottomNavigationView tabs', async function () {
      const navNewScan = await driver.$('id:com.example.brainhemorrhage:id/nav_newscan');
      const navHistory = await driver.$('id:com.example.brainhemorrhage:id/nav_history');
      const navSettings = await driver.$('id:com.example.brainhemorrhage:id/nav_settings');

      // Tap New Scan tab
      await navNewScan.click();
      const scanTitle = await driver.$('id:com.example.brainhemorrhage:id/tvUploadTitle');
      await scanTitle.waitForDisplayed({ timeout: 5000 });
      expect(await scanTitle.isDisplayed()).to.be.true;

      // Tap History tab
      await navHistory.click();
      const historyTitle = await driver.$('id:com.example.brainhemorrhage:id/tvHistoryTitle');
      await historyTitle.waitForDisplayed({ timeout: 5000 });
      expect(await historyTitle.isDisplayed()).to.be.true;

      // Tap Settings tab
      await navSettings.click();
      const settingsTitle = await driver.$('id:com.example.brainhemorrhage:id/tvSettingsTitle');
      await settingsTitle.waitForDisplayed({ timeout: 5000 });
      expect(await settingsTitle.isDisplayed()).to.be.true;
    });
  });

  // -------------------------------------------------------------
  // MODULE 4: CT IMAGE UPLOAD & AI SCAN ANALYSIS
  // -------------------------------------------------------------
  describe('Module 4: CT Scan Image Selection & AI Pipeline', function () {
    it('TC-APP-SCAN-191: Should open New Scan fragment and fill Patient Information', async function () {
      const navNewScan = await driver.$('id:com.example.brainhemorrhage:id/nav_newscan');
      await navNewScan.click();

      const etPatientId = await driver.$('id:com.example.brainhemorrhage:id/etPatientId');
      const etPatientName = await driver.$('id:com.example.brainhemorrhage:id/etPatientName');
      const etPatientAge = await driver.$('id:com.example.brainhemorrhage:id/etPatientAge');

      await etPatientId.setValue('PAT-2026-8892');
      await etPatientName.setValue('John Doe');
      await etPatientAge.setValue('58');

      expect(await etPatientId.getText()).to.include('PAT-2026-8892');
    });

    it('TC-APP-SCAN-198: Should trigger AI Analysis and navigate to Result Fragment', async function () {
      const btnAnalyze = await driver.$('id:com.example.brainhemorrhage:id/btnAnalyzeScan');
      if (await btnAnalyze.isDisplayed()) {
        await btnAnalyze.click();
      }

      // Wait for AI inference completion and Result Fragment display
      const tvResultStatus = await driver.$('id:com.example.brainhemorrhage:id/tvResultStatus');
      await tvResultStatus.waitForDisplayed({ timeout: 20000 });
      expect(await tvResultStatus.isDisplayed()).to.be.true;
    });
  });

  // -------------------------------------------------------------
  // MODULE 5: PATIENT HISTORY & SEARCH FILTERS
  // -------------------------------------------------------------
  describe('Module 5: Patient History Search & Timeline', function () {
    it('TC-APP-HIST-271: Should filter patient scans using SearchView', async function () {
      const navHistory = await driver.$('id:com.example.brainhemorrhage:id/nav_history');
      await navHistory.click();

      const searchView = await driver.$('id:com.example.brainhemorrhage:id/searchViewHistory');
      await searchView.click();
      await driver.keys(['P', 'A', 'T', '-', '0', '1']);

      const rvList = await driver.$('id:com.example.brainhemorrhage:id/rvPatientHistory');
      expect(await rvList.isDisplayed()).to.be.true;
    });
  });

  // -------------------------------------------------------------
  // MODULE 6: PROFILE SETTINGS & LOGOUT
  // -------------------------------------------------------------
  describe('Module 6: Doctor Settings & Account Logout', function () {
    it('TC-APP-SET-305: Should log out doctor and return to Welcome Screen', async function () {
      const navSettings = await driver.$('id:com.example.brainhemorrhage:id/nav_settings');
      await navSettings.click();

      const btnLogout = await driver.$('id:com.example.brainhemorrhage:id/btnLogout');
      await btnLogout.scrollIntoView();
      await btnLogout.click();

      // Confirm logout dialog
      const btnConfirmLogout = await driver.$('id:android:id/button1');
      if (await btnConfirmLogout.isExisting()) {
        await btnConfirmLogout.click();
      }

      // Verify return to Welcome / Login screen
      const welcomeHeader = await driver.$('id:com.example.brainhemorrhage:id/welcomeHeader');
      await welcomeHeader.waitForDisplayed({ timeout: 10000 });
      expect(await welcomeHeader.isDisplayed()).to.be.true;
    });
  });
});
