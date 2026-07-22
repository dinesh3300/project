/**
 * Appium UiAutomator2 Configuration for NeuroCheck Android App
 */
module.exports = {
  hostname: '127.0.0.1',
  port: 4723,
  path: '/',
  capabilities: {
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:deviceName': 'Android Emulator',
    'appium:appPackage': 'com.example.brainhemorrhage',
    'appium:appActivity': 'com.example.brainhemorrhage.MainActivity',
    'appium:noReset': false,
    'appium:fullReset': false,
    'appium:autoGrantPermissions': true,
    'appium:newCommandTimeout': 120,
    'appium:ensureWebviewsHavePages': true,
    'appium:nativeWebScreenshot': true,
    'appium:connectHardwareKeyboard': true
  }
};
