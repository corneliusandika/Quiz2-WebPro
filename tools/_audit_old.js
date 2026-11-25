const path = require('path');
const fs = require('fs');
const puppeteer = require('puppeteer-core');

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const OUT = path.resolve(__dirname, '..', 'report', '_audit');
const SEO = 'https://www.seobility.net/en/seocheck/check/?url=https%3A%2F%2Fdompetku-gwa4akc5dzgbgdet.indonesiacentral-01.azurewebsites.net%2Flogin&mode=standard';
const PSI = 'https://pagespeed.web.dev/analysis/https-dompetku-gwa4akc5dzgbgdet-indonesiacentral-01-azurewebsites-net-login/vogks0ttme?form_factor=desktop';

const extractText = () => {
    function deepText(root) {
        let text = root.innerText || '';
        for (const element of root.querySelectorAll('*')) {
            if (element.shadowRoot) {
                text += '\n' + deepText(element.shadowRoot);
            }
        }
        return text;
    }
    return deepText(document.documentElement);
};

(async () => {
    fs.mkdirSync(OUT, { recursive: true });
    const browser = await puppeteer.launch({
        executablePath: CHROME,
        headless: 'new',
        args: ['--no-sandbox', '--disable-gpu', '--hide-scrollbars']
    });
    const page = await browser.newPage();
    await page.setViewport({ width: 1440, height: 1100, deviceScaleFactor: 1 });

    const apiResponses = [];
    page.on('response', async (response) => {
        const url = response.url();
        const type = response.headers()['content-type'] || '';
        if (type.includes('json') && (url.includes('seobility') || url.includes('pagespeed') || url.includes('googleapis'))) {
            try {
                apiResponses.push({ url, status: response.status(), body: (await response.text()).slice(0, 30000) });
            } catch (error) {
                apiResponses.push({ url, status: response.status(), error: String(error) });
            }
        }
    });

    console.log('=== SEOBILITY ===');
    await page.goto(SEO, { waitUntil: 'domcontentloaded', timeout: 90000 });
    await new Promise((resolve) => setTimeout(resolve, 30000));
    const seoText = await page.evaluate(extractText);
    console.log(seoText.slice(0, 12000));
    await page.screenshot({ path: path.join(OUT, 'seobility.png'), fullPage: true });

    console.log('\n=== SEOBILITY API RESPONSES ===');
    apiResponses.forEach((item) => {
        console.log(item.url, item.status);
        console.log(item.body || item.error || '');
    });
    apiResponses.length = 0;

    console.log('\n=== PAGESPEED ===');
    await page.goto(PSI, { waitUntil: 'domcontentloaded', timeout: 90000 });
    await new Promise((resolve) => setTimeout(resolve, 25000));
    const psiText = await page.evaluate(extractText);
    console.log(psiText.slice(0, 12000));
    await page.screenshot({ path: path.join(OUT, 'pagespeed.png'), fullPage: true });

    console.log('\n=== PAGESPEED API RESPONSES ===');
    apiResponses.forEach((item) => {
        console.log(item.url, item.status);
        console.log(item.body || item.error || '');
    });

    await browser.close();
})().catch((error) => {
    console.error(error);
    process.exit(1);
});
