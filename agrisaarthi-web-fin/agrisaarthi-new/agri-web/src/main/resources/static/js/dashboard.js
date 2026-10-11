document.addEventListener("DOMContentLoaded", () => {
  const menuToggle = document.getElementById("menuToggle");
  const sidebar = document.getElementById("sidebar");

  if (menuToggle && sidebar) {
    menuToggle.addEventListener("click", () => {
      const isOpen = sidebar.classList.toggle("sidebar-open");
      menuToggle.setAttribute("aria-expanded", String(isOpen));
    });

    sidebar.querySelectorAll(".nav-item").forEach((link) => {
      link.addEventListener("click", () => {
        sidebar.classList.remove("sidebar-open");
        menuToggle.setAttribute("aria-expanded", "false");
      });
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        sidebar.classList.remove("sidebar-open");
        menuToggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  const todayLabel = document.getElementById("todayLabel");

  if (todayLabel) {
    const updateDate = () => {
      todayLabel.textContent = new Intl.DateTimeFormat(window.AgriLanguage?.locale || undefined, {
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric",
      }).format(new Date());
    };
    updateDate();
    document.addEventListener("agri-language-change", updateDate);
  }
});