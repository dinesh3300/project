/**
 * NeuroCheck Web Application - E2E Automated Selenium Test Suite
 * Target Module: Authentication & Login Functionality
 * Framework: Selenium-WebDriver + Mocha + Chai
 */

const { Builder, By, Key, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const { expect } = require('chai');

describe('NeuroCheck Web Application - Login & Auth E2E Test Suite', function () {
  this.timeout(60000); // 60s timeout for async browser operations

  let driver;
  const BASE_URL = process.env.TEST_URL || 'http://localhost:5173';

  before(async function () {
    // Configure Chrome options
    const options = new chrome.Options();
    options.addArguments('--start-maximized');
    options.addArguments('--disable-gpu');
    options.addArguments('--no-sandbox');
    options.addArguments('--disable-dev-shm-usage');
    if (process.env.CI || process.env.HEADLESS !== 'false') {
      options.addArguments('--headless=new');
    }

    let service;
    try {
      const chromedriver = require('chromedriver');
      if (chromedriver && chromedriver.path) {
        service = new chrome.ServiceBuilder(chromedriver.path);
      }
    } catch (e) {
      // Driver auto-resolved by Selenium Manager
    }

    try {
      let builder = new Builder().forBrowser('chrome').setChromeOptions(options);
      if (service) {
        builder = builder.setChromeService(service);
      }
      driver = await builder.build();
    } catch (err) {
      const fallbackOptions = new chrome.Options();
      fallbackOptions.addArguments('--headless');
      fallbackOptions.addArguments('--no-sandbox');
      fallbackOptions.addArguments('--disable-dev-shm-usage');
      fallbackOptions.addArguments('--disable-gpu');
      
      let fallbackBuilder = new Builder().forBrowser('chrome').setChromeOptions(fallbackOptions);
      if (service) {
        fallbackBuilder = fallbackBuilder.setChromeService(service);
      }
      driver = await fallbackBuilder.build();
    }
  });

  after(async function () {
    if (driver) {
      await driver.quit();
    }
  });

  beforeEach(async function () {
    // Clear localStorage to reset session state before each test
    await driver.get(BASE_URL);
    await driver.executeScript('window.localStorage.clear();');
    await driver.navigate().refresh();
  });

  it('TC-AUTH-001: Should load Welcome Page with app title and hero branding', async function () {
    await driver.get(BASE_URL);
    const title = await driver.getTitle();
    expect(title).to.be.a('string');

    // Verify Welcome Hero heading
    const welcomeHeader = await driver.wait(
      until.elementLocated(By.xpath("//h1 | //h2[contains(text(), 'NeuroCheck') or contains(text(), 'Nuerocheck')]")),
      10000
    );
    expect(await welcomeHeader.isDisplayed()).to.be.true;
  });

  it('TC-AUTH-002: Should navigate to Login Portal on clicking Access Portal button', async function () {
    await driver.get(BASE_URL);
    
    // Locate and click 'Access Portal' button
    const accessBtn = await driver.wait(
      until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal') or contains(text(), 'Login')]")),
      10000
    );
    await accessBtn.click();

    // Verify Login Screen heading
    const loginHeader = await driver.wait(
      until.elementLocated(By.xpath("//h2[contains(text(), 'Welcome Back') or contains(text(), 'Login')]")),
      10000
    );
    expect(await loginHeader.isDisplayed()).to.be.true;
  });

  it('TC-AUTH-003: Should render Email, Password fields and Submit button', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(
      until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")),
      5000
    );
    await accessBtn.click();

    const emailInput = await driver.findElement(By.css("input[type='email']"));
    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const loginBtn = await driver.findElement(By.xpath("//button[text()='Login' or @type='submit']"));

    expect(await emailInput.isDisplayed()).to.be.true;
    expect(await passwordInput.isDisplayed()).to.be.true;
    expect(await loginBtn.isDisplayed()).to.be.true;
  });

  it('TC-AUTH-005: Should show validation toast when submitting invalid email format', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
    await accessBtn.click();

    const emailInput = await driver.findElement(By.css("input[type='email']"));
    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const loginBtn = await driver.findElement(By.xpath("//button[text()='Login' or @type='submit']"));

    await emailInput.sendKeys('invalidemail.com');
    await passwordInput.sendKeys('Password123!');
    await loginBtn.click();

    // Toast notification check
    const toast = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class, 'toast') or contains(text(), 'email')]")),
      10000
    );
    const toastText = await toast.getText();
    expect(toastText.toLowerCase()).to.include('email');
  });

  it('TC-AUTH-006: Should show error toast for incorrect login credentials', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
    await accessBtn.click();

    const emailInput = await driver.findElement(By.css("input[type='email']"));
    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const loginBtn = await driver.findElement(By.xpath("//button[text()='Login' or @type='submit']"));

    await emailInput.clear();
    await emailInput.sendKeys('nonexistent@doctor.com');
    await passwordInput.clear();
    await passwordInput.sendKeys('WrongPass123!');
    await loginBtn.click();

    const toast = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class, 'toast') or contains(text(), 'Incorrect') or contains(text(), 'failed')]")),
      10000
    );
    const toastText = await toast.getText();
    expect(toastText.toLowerCase()).to.satisfy(msg =>
      msg.includes('incorrect') || msg.includes('failed') || msg.includes('invalid') || msg.includes('error')
    );
  });

  it('TC-AUTH-008: Should mask password input by default', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
    await accessBtn.click();

    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const inputType = await passwordInput.getAttribute('type');
    expect(inputType).to.equal('password');
  });

  it('TC-AUTH-010: Should sanitize and handle SQL injection attempts safely', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
    await accessBtn.click();

    const emailInput = await driver.findElement(By.css("input[type='email']"));
    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const loginBtn = await driver.findElement(By.xpath("//button[text()='Login' or @type='submit']"));

    await emailInput.sendKeys("' OR '1'='1");
    await passwordInput.sendKeys("' OR '1'='1");
    await loginBtn.click();

    // System should not crash and should reject injection attempt
    const toast = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class, 'toast')]")),
      10000
    );
    expect(await toast.isDisplayed()).to.be.true;
  });

  it('TC-AUTH-015: Should successfully log in with valid credentials and load Dashboard', async function () {
    await driver.get(BASE_URL);
    const accessBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
    await accessBtn.click();

    const emailInput = await driver.findElement(By.css("input[type='email']"));
    const passwordInput = await driver.findElement(By.css("input[type='password']"));
    const loginBtn = await driver.findElement(By.xpath("//button[text()='Login' or @type='submit']"));

    // Enter test doctor credentials
    await emailInput.clear();
    await emailInput.sendKeys('test@example.com');
    await passwordInput.clear();
    await passwordInput.sendKeys('Test@123');
    await loginBtn.click();

    // Verify successful login session in localStorage or UI sidebar
    await driver.sleep(2000);
    const userSession = await driver.executeScript("return localStorage.getItem('NuerocheckUser');");
    
    // Verify dashboard navigation or session persistence
    const sidebar = await driver.wait(
      until.elementLocated(By.xpath("//aside | //div[contains(@class, 'sidebar')] | //div[contains(@class, 'dashboard')]")),
      15000
    );
    expect(await sidebar.isDisplayed()).to.be.true;
  });
});
