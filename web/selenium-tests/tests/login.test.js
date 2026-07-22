const { Builder, By, Key, until } = require('selenium-webdriver');
const chrome = require('selenium-webdriver/chrome');
const { expect } = require('chai');

describe('Nuerocheck Login E2E Tests', function() {
    let driver;

    before(async function() {
        let options = new chrome.Options();
        // options.addArguments('--headless'); // Uncomment for headless execution
        driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build();
    });

    after(async function() {
        await driver.quit();
    });

    it('Should load the welcome page', async function() {
        await driver.get('http://localhost:5173'); // Adjust URL if needed
        const title = await driver.getTitle();
        // expect(title).to.include('Nuerocheck'); // Adjust based on actual <title> tag
    });

    it('Should navigate to login page from welcome screen', async function() {
        await driver.get('http://localhost:5173');
        const accessPortalBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 10000);
        await accessPortalBtn.click();

        const loginHeader = await driver.wait(until.elementLocated(By.xpath("//h2[contains(text(), 'Welcome Back')]")), 10000);
        expect(await loginHeader.isDisplayed()).to.be.true;
    });

    it('Should show error with invalid credentials', async function() {
        // Navigate to login
        await driver.get('http://localhost:5173');
        const accessPortalBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
        await accessPortalBtn.click();

        // Fill credentials
        const emailInput = await driver.findElement(By.css("input[type='email']"));
        const passwordInput = await driver.findElement(By.css("input[type='password']"));

        await emailInput.sendKeys('invalid@doctor.com');
        await passwordInput.sendKeys('WrongPass123!');

        const loginBtn = await driver.findElement(By.xpath("//button[text()='Login']"));
        await loginBtn.click();

        // Check for toast error message
        const toast = await driver.wait(until.elementLocated(By.className('toast-message')), 10000);
        const toastText = await toast.getText();
        expect(toastText.toLowerCase()).to.satisfy(msg =>
            msg.includes('incorrect') || msg.includes('failed') || msg.includes('invalid')
        );
    });

    it('Should successfully login with correct credentials', async function() {
        // This test assumes a user 'test@example.com' with password 'Test@123' exists in your DB
        await driver.get('http://localhost:5173');
        const accessPortalBtn = await driver.wait(until.elementLocated(By.xpath("//button[contains(text(), 'Access Portal')]")), 5000);
        await accessPortalBtn.click();

        const emailInput = await driver.findElement(By.css("input[type='email']"));
        const passwordInput = await driver.findElement(By.css("input[type='password']"));

        await emailInput.clear();
        await emailInput.sendKeys('test@example.com');
        await passwordInput.clear();
        await passwordInput.sendKeys('Test@123'); // Ensure this matches your registration policy

        const loginBtn = await driver.findElement(By.xpath("//button[text()='Login']"));
        await loginBtn.click();

        // Should see dashboard sidebar
        const sidebar = await driver.wait(until.elementLocated(By.className('app-sidebar')), 15000);
        expect(await sidebar.isDisplayed()).to.be.true;

        const welcomeText = await driver.findElement(By.className('navbar-title'));
        expect(await welcomeText.getText()).to.equal('dashboard');
    });
});
