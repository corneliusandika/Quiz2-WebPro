const path = require('path');
const fs = require('fs');
const puppeteer = require('puppeteer-core');

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const BASE = 'http://localhost:8080/dompetku';
const OUT = path.resolve(__dirname, '..', 'report', 'screenshots');

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

(async () => {
    fs.mkdirSync(OUT, { recursive: true });
    const browser = await puppeteer.launch({
        executablePath: CHROME,
        headless: 'new',
        args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars', '--font-render-hinting=none'],
        defaultViewport: { width: 1440, height: 1000, deviceScaleFactor: 1 }
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 });

    async function settle(ms = 1400) {
        await page.evaluate(() => document.fonts && document.fonts.ready);
        await wait(ms);
    }

    async function shot(name, url, options = {}) {
        if (options.viewport) {
            await page.setViewport(options.viewport);
        } else {
            await page.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 1 });
        }
        await page.goto(BASE + url, { waitUntil: 'networkidle2' });
        await settle(options.wait || 1400);
        await page.screenshot({
            path: path.join(OUT, name),
            fullPage: Boolean(options.fullPage)
        });
        console.log('captured', name);
    }

    await page.evaluateOnNewDocument(() => {
        try {
            if (!localStorage.getItem('dompetku-theme')) {
                localStorage.setItem('dompetku-theme', 'light');
            }
        } catch (e) { /* ignore */ }
    });

    await shot('login.png', '/login');
    await shot('register.png', '/register');

    await page.goto(BASE + '/login', { waitUntil: 'networkidle2' });
    await page.type('input[name="email"]', 'demo@dompetku.test', { delay: 15 });
    await page.type('input[name="password"]', 'password', { delay: 15 });
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle2' }),
        page.click('button[type="submit"]')
    ]);
    await settle(2200);

    await shot('dashboard.png', '/app/dashboard');
    await shot('dashboard-full.png', '/app/dashboard', { fullPage: true, wait: 1800 });
    await shot('transactions.png', '/app/transactions');
    await shot('transaction-form.png', '/app/transactions/edit/1');
    await shot('categories.png', '/app/categories');
    await shot('wallets.png', '/app/wallets');
    await shot('budgets.png', '/app/budgets');
    await shot('goals.png', '/app/goals');
    await shot('goal-detail.png', '/app/goals/view/1');
    await shot('goal-deposit-edit.png', '/app/goals/deposit/edit/1');
    await shot('debts.png', '/app/debts');
    await shot('recurring.png', '/app/recurring');
    await shot('reports.png', '/app/reports');
    await shot('profile.png', '/app/profile');

    await page.evaluate(() => localStorage.setItem('dompetku-theme', 'dark'));
    await shot('dashboard-dark.png', '/app/dashboard');
    await page.evaluate(() => localStorage.setItem('dompetku-theme', 'light'));

    await shot('dashboard-mobile.png', '/app/dashboard', {
        viewport: { width: 390, height: 844, deviceScaleFactor: 1 },
        wait: 1600
    });

    await browser.close();
    console.log('done ->', OUT);
})().catch((error) => {
    console.error(error);
    process.exit(1);
});
