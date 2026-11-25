const fs = require('fs');
const http = require('http');
const path = require('path');
const puppeteer = require('puppeteer-core');

const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const ROOT = path.resolve(__dirname, '..', 'report');
const OUT = path.join(ROOT, 'Dompetku-JSP-Report.pdf');
const PORT = 8899;
const MIME = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml'
};

const server = http.createServer((request, response) => {
    const pathname = decodeURIComponent(request.url.split('?')[0]);
    const relative = pathname === '/' ? 'report.html' : pathname.replace(/^\/+/, '');
    const file = path.resolve(ROOT, relative);
    if (!file.startsWith(ROOT)) {
        response.writeHead(403);
        response.end();
        return;
    }
    fs.readFile(file, (error, data) => {
        if (error) {
            response.writeHead(404);
            response.end();
            return;
        }
        response.writeHead(200, { 'Content-Type': MIME[path.extname(file)] || 'application/octet-stream' });
        response.end(data);
    });
});

(async () => {
    await new Promise((resolve) => server.listen(PORT, '127.0.0.1', resolve));
    let browser;
    try {
        browser = await puppeteer.launch({
            executablePath: CHROME,
            headless: 'new',
            args: ['--no-sandbox', '--disable-gpu', '--font-render-hinting=none']
        });
        const page = await browser.newPage();
        await page.setViewport({ width: 1200, height: 1600, deviceScaleFactor: 1 });
        await page.goto(`http://127.0.0.1:${PORT}/report.html`, { waitUntil: 'networkidle0', timeout: 60000 });
        await page.waitForFunction('window.__DIAGRAMS_READY === true', { timeout: 30000 }).catch(() => {});
        await page.evaluate(async () => {
            if (document.fonts) { await document.fonts.ready; }
            await Promise.all(Array.from(document.images).map((img) => {
                if (img.complete) { return Promise.resolve(); }
                return new Promise((resolve) => {
                    img.onload = resolve;
                    img.onerror = resolve;
                });
            }));
        });
        await new Promise((resolve) => setTimeout(resolve, 1000));

        await page.pdf({
            path: OUT,
            format: 'A4',
            printBackground: true,
            displayHeaderFooter: true,
            headerTemplate: '<div></div>',
            footerTemplate: '<div style="width:100%;padding:0 14mm;font-family:Arial,sans-serif;font-size:7.5px;color:#94a3b8;display:flex;justify-content:space-between;"><span>Dompetku &mdash; Laporan Exam 2 &middot; Cornelius Andika</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>',
            margin: { top: '14mm', bottom: '16mm', left: '14mm', right: '14mm' }
        });
        console.log('PDF written:', OUT);
    } finally {
        if (browser) {
            await browser.close();
        }
        await new Promise((resolve) => server.close(resolve));
    }
})().catch((error) => {
    console.error(error);
    process.exit(1);
});
