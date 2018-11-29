#!/usr/bin/env python

from appium import webdriver
from time import sleep
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import os
import unittest

class StartupTest(unittest.TestCase):
    def setUp(self):
        desired_caps = {}
        self.driver = webdriver.Remote('http://127.0.0.1:4723/wd/hub', desired_caps)
        self.driver.get('https://matchat.org')

    def tearDown(self):
        self.driver.quit()
    
    def test_startup(self):
        element = WebDriverWait(self.driver, 30).until(
            EC.visibility_of_element_located((By.CSS_SELECTOR, "won-topnav button[ng-click=\"self.account__acceptDisclaimer()\"]"))
        )
        screenshot_folder = os.getenv('SCREENSHOT_PATH', '/tmp')
        self.driver.save_screenshot(screenshot_folder + '/disclaimer.png')
        element.click()
        WebDriverWait(self.driver, 5).until(
            lambda driver: not element.is_displayed()
        )
        
        WebDriverWait(self.driver, 5).until(
            EC.visibility_of_element_located((By.CSS_SELECTOR, "div.cpi__item:nth-child(2)"))
        )
        
        self.driver.save_screenshot(screenshot_folder + '/homescreen.png')
    

if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(StartupTest)
    unittest.TextTestRunner(verbosity=2).run(suite)