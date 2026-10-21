// 弹窗 / 确认框 / 提示（见 06 篇 §3.4）。敏感操作的确认框必须写明后果。
const Dialog = {
  toast(msg) {
    const el = document.createElement('div');
    el.className = 'toast';
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2200);
  },

  confirm(message, title = '提示') {
    return new Promise(resolve => {
      const mask = document.createElement('div');
      mask.className = 'dialog-mask';
      mask.innerHTML = `
        <div class="dialog">
          <div class="dialog-header">${title}</div>
          <div class="dialog-body">${message}</div>
          <div class="dialog-footer">
            <button class="btn" data-act="cancel">取消</button>
            <button class="btn btn-primary" data-act="ok">确定</button>
          </div>
        </div>`;
      document.body.appendChild(mask);
      mask.querySelector('[data-act=cancel]').onclick = () => { mask.remove(); resolve(false); };
      mask.querySelector('[data-act=ok]').onclick = () => { mask.remove(); resolve(true); };
    });
  },

  open({ title, url, width = 900 }) {
    const mask = document.createElement('div');
    mask.className = 'dialog-mask';
    mask.innerHTML = `
      <div class="dialog" style="width:${width}px">
        <div class="dialog-header">${title}</div>
        <div class="dialog-body" style="padding:0">
          <iframe src="${url}" style="width:100%;height:60vh;border:0"></iframe>
        </div>
        <div class="dialog-footer">
          <button class="btn" data-act="close">关闭</button>
        </div>
      </div>`;
    document.body.appendChild(mask);
    mask.querySelector('[data-act=close]').onclick = () => mask.remove();
    return mask;
  }
};
