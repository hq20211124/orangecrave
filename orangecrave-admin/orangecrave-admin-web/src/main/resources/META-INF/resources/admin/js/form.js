// 表单序列化与校验
const Form = {
  serialize(selector) {
    const form = document.querySelector(selector);
    if (!form) return {};
    const data = {};
    form.querySelectorAll('input[name], select[name], textarea[name]').forEach(el => {
      if (el.type === 'checkbox') {
        data[el.name] = el.checked;
      } else {
        data[el.name] = el.value;
      }
    });
    return data;
  }
};
