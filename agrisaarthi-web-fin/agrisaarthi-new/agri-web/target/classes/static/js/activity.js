(function () {
  const storageKey = "agrisaarthi.activity.v1";

  function read() {
    const saved = localStorage.getItem(storageKey);
    if (!saved) return [];
    const records = JSON.parse(saved);
    if (!Array.isArray(records)) throw new Error("Saved activity has an invalid format.");
    return records;
  }

  window.AgriActivity = {
    list() {
      return read();
    },
    add(type, title, detail) {
      const record = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
        type,
        title: String(title).slice(0, 200),
        detail: String(detail).slice(0, 2000),
        createdAt: new Date().toISOString(),
      };
      const records = read();
      records.unshift(record);
      localStorage.setItem(storageKey, JSON.stringify(records.slice(0, 100)));
      return record;
    },
    clear() {
      localStorage.removeItem(storageKey);
    },
  };
})();
