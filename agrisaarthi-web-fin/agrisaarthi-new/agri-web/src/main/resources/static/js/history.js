document.addEventListener("DOMContentLoaded", () => {
  const list = document.getElementById("activityList");
  const empty = document.getElementById("activityEmpty");
  const clearButton = document.getElementById("clearActivity");
  const status = document.getElementById("activityStatus");
  if (!list || !empty || !clearButton || !status || !window.AgriActivity) return;

  function render() {
    list.replaceChildren();
    try {
      const records = window.AgriActivity.list();
      empty.hidden = records.length > 0;
      records.forEach((record) => {
        const item = document.createElement("article");
        item.className = "activity-item";
        const heading = document.createElement("div");
        heading.className = "activity-heading";
        const title = document.createElement("h3");
        title.textContent = record.title;
        const type = document.createElement("span");
        type.className = "pill pill-green";
        type.textContent = record.type;
        heading.append(title, type);
        const detail = document.createElement("p");
        detail.textContent = record.detail;
        const date = document.createElement("time");
        date.dateTime = record.createdAt;
        date.textContent = new Intl.DateTimeFormat(window.AgriLanguage?.locale || undefined, {
          dateStyle: "medium",
          timeStyle: "short",
        }).format(new Date(record.createdAt));
        item.append(heading, detail, date);
        list.appendChild(item);
      });
      status.textContent = "";
    } catch (error) {
      empty.hidden = true;
      status.className = "status-message error";
      status.textContent = `Could not read saved activity: ${error.message}`;
    }
  }

  clearButton.addEventListener("click", () => {
    try {
      window.AgriActivity.clear();
      status.className = "status-message";
      status.textContent = "Activity cleared from this browser.";
      render();
    } catch (error) {
      status.className = "status-message error";
      status.textContent = `Could not clear activity: ${error.message}`;
    }
  });

  document.addEventListener("agri-language-change", () => window.setTimeout(render, 0));
  render();
});
