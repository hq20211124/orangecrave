// 表格 + 分页渲染（见 06 篇 §3.3）
const Table = {
  async render({ el, url, columns, query }) {
    const container = document.querySelector(el);
    if (!container) return;
    container.innerHTML = '<div class="loading">加载中…</div>';
    const params = Object.assign({ pageNum: 1, pageSize: 20 }, query ? query() : {});
    const page = await http.get(url, params);
    if (!page.list || page.list.length === 0) {
      container.innerHTML = '<div class="empty">暂无数据</div>';
      return;
    }
    const head = columns.map(c => `<th>${c.title}</th>`).join('');
    const rows = page.list.map(row => {
      const tds = columns.map(c => {
        // render 自己负责拼 HTML；直接取字段的值一律转义，用户上传的文本是唯一入口
        const value = c.render ? c.render(row) : esc(row[c.field] ?? '');
        return `<td>${value}</td>`;
      }).join('');
      return `<tr>${tds}</tr>`;
    }).join('');

    const totalPages = Math.max(1, Math.ceil(page.total / page.pageSize));
    container.innerHTML = `
      <div class="table-wrap">
        <table><thead><tr>${head}</tr></thead><tbody>${rows}</tbody></table>
        <div class="pagination">
          <span>共 ${page.total} 条</span>
          <button class="btn" data-page="${page.pageNum - 1}" ${page.pageNum <= 1 ? 'disabled' : ''}>上一页</button>
          <span>${page.pageNum} / ${totalPages}</span>
          <button class="btn" data-page="${page.pageNum + 1}" ${page.pageNum >= totalPages ? 'disabled' : ''}>下一页</button>
        </div>
      </div>`;

    container.querySelectorAll('[data-page]').forEach(btn => {
      btn.onclick = () => {
        const next = parseInt(btn.dataset.page, 10);
        if (next >= 1 && next <= totalPages) {
          Table.render({ el, url, columns, query: () => Object.assign(query ? query() : {}, { pageNum: next }) });
        }
      };
    });
    applyPerms(container);
  }
};
