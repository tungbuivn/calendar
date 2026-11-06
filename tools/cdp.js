// CDP helper: dump visible text + collect console/log entries from Chrome on device.
// Usage: node cdp.js <port> <mode> [targetFilter]
//   modes: text | console | dom | both
const port = process.argv[2] || '9333';
const mode = process.argv[3] || 'both';
const filter = process.argv[4] || '';

const http = require('http');

function httpJson(path) {
  return new Promise((resolve, reject) => {
    http.get({ host: '127.0.0.1', port: Number(port), path }, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

function connect(wsUrl) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl);
    let id = 0;
    const pending = new Map();
    const events = [];
    ws.addEventListener('open', () => resolve({
      send(method, params = {}) {
        const msgId = ++id;
        return new Promise((res, rej) => {
          pending.set(msgId, { res, rej });
          ws.send(JSON.stringify({ id: msgId, method, params }));
        });
      },
      events,
      close() { ws.close(); },
    }));
    ws.addEventListener('error', (e) => reject(new Error('ws error: ' + (e.message || 'unknown'))));
    ws.addEventListener('message', (ev) => {
      let msg;
      try { msg = JSON.parse(ev.data); } catch { return; }
      if (msg.id && pending.has(msg.id)) {
        const { res, rej } = pending.get(msg.id);
        pending.delete(msg.id);
        if (msg.error) rej(new Error(msg.error.message));
        else res(msg.result);
      } else if (msg.method) {
        events.push(msg);
      }
    });
  });
}

(async () => {
  const targets = await httpJson('/json/list');
  const pages = targets.filter((t) => t.type === 'page');
  const selected = filter ? pages.filter((p) => (p.title + ' ' + p.url).toLowerCase().includes(filter.toLowerCase())) : pages;

  for (const page of selected) {
    console.log('\n' + '='.repeat(90));
    console.log('TARGET id=' + page.id);
    console.log('TITLE  ' + page.title);
    console.log('URL    ' + page.url.slice(0, 300));
    console.log('-'.repeat(90));
    try {
      const cdp = await connect(page.webSocketDebuggerUrl);
      await cdp.send('Runtime.enable');
      await cdp.send('Log.enable');
      await cdp.send('Network.enable');

      if (mode === 'text' || mode === 'both' || mode === 'dom') {
        const r = await cdp.send('Runtime.evaluate', {
          expression: `(function(){
            const clean = s => (s||'').replace(/\\u00a0/g,' ').replace(/[ \\t]+\\n/g,'\\n').replace(/\\n{3,}/g,'\\n\\n');
            let out = clean(document.body ? document.body.innerText : '');
            return out;
          })()`,
          returnByValue: true,
          awaitPromise: false,
        });
        console.log(r.result && r.result.value ? r.result.value : '(no text)');
      }

      if (mode === 'console' || mode === 'both') {
        const interesting = cdp.events.filter((e) =>
          ['Log.entryAdded', 'Runtime.consoleAPICalled', 'Runtime.exceptionThrown'].includes(e.method));
        console.log('\n--- CONSOLE / LOG ENTRIES (collected after attach) ---');
        if (!interesting.length) console.log('(none captured post-attach)');
        for (const e of interesting) {
          if (e.method === 'Log.entryAdded') {
            console.log(`[${e.params.entry.level.toUpperCase()}] [${e.params.entry.source}] ${e.params.entry.text}` +
              (e.params.entry.url ? ` @ ${e.params.entry.url}` : ''));
          } else if (e.method === 'Runtime.consoleAPICalled') {
            const args = (e.params.args || []).map((a) => a.value ?? a.description ?? a.type).join(' ');
            console.log(`[${e.params.type.toUpperCase()}] ${args}`);
          } else if (e.method === 'Runtime.exceptionThrown') {
            const d = e.params.exceptionDetails || {};
            console.log(`[EXCEPTION] ${d.text || ''} ${d.exception ? d.exception.description || d.exception.value : ''}`);
          }
        }
      }
      cdp.close();
    } catch (e) {
      console.log('ERROR talking to target: ' + e.message);
    }
  }
  process.exit(0);
})();
