document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("chatForm");
  const input = document.getElementById("messageInput");
  const messages = document.getElementById("chatMessages");
  const status = document.getElementById("statusMessage");
  const sendButton = document.getElementById("sendButton");
  const suggestions = document.getElementById("suggestions");

  if (!form || !input || !messages || !status || !sendButton) return;

  function addMessage(text, sender, category = "") {
    const bubble = document.createElement("article");
    bubble.className = `message ${sender}`;

    const label = document.createElement("span");
    label.className = "message-label";
    label.textContent = sender === "user" ? "YOU" : `AGRISAARTHI${category ? ` · ${category}` : ""}`;

    const content = document.createElement("div");
    content.textContent = text;
    bubble.append(label, content);
    messages.appendChild(bubble);
    bubble.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  async function submitQuestion(question) {
    const message = question.trim();
    if (!message || sendButton.disabled) return;

    addMessage(message, "user");
    input.value = "";
    if (suggestions) suggestions.hidden = true;
    sendButton.disabled = true;
    status.className = "status-message";
    status.textContent = "Preparing practical guidance…";

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message,
          language: document.getElementById("languageSelect")?.value || "en",
        }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || `Request failed (${response.status}).`);
      if (typeof data.answer !== "string" || typeof data.category !== "string") {
        throw new Error("The advisory service returned an invalid response.");
      }

      addMessage(data.answer, "assistant", data.category);
if (Array.isArray(data.sources) && data.sources.length > 0) {
  const sourcesContainer = document.createElement("div");
  sourcesContainer.className = "sources-list";
  sourcesContainer.innerHTML = "<small style='display:block; margin-top:8px; font-weight:bold; color:#174c38;'>📚 Sources & References:</small>";
  
  data.sources.forEach((src) => {
    const link = document.createElement("a");
    link.href = src.url || "#";
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = src.title || "Agricultural Reference";
    link.style.cssText = "display:inline-block; margin:4px 6px 0 0; padding:2px 8px; background:#e8f5e9; color:#2e7d32; border-radius:12px; font-size:12px; text-decoration:none;";
    sourcesContainer.appendChild(link);
  });
  messages.appendChild(sourcesContainer);
}
      status.textContent = "";
      if (window.AgriActivity) {
        try {
          window.AgriActivity.add("Assistant", message, data.answer);
        } catch (error) {
          status.textContent = `Answer received, but could not save it to this browser: ${error.message}`;
        }
      }
    } catch (error) {
      status.className = "status-message error";
      status.textContent = error.message || "Could not retrieve advice. Please try again.";
    } finally {
      sendButton.disabled = false;
      input.focus();
    }
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    submitQuestion(input.value);
  });

  input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      form.requestSubmit();
    }
  });

  document.querySelectorAll(".suggestion[data-question]").forEach((button) => {
    button.addEventListener("click", () => {
      input.value = button.dataset.question || "";
      input.focus();
    });
  });

  document.querySelectorAll(".new-chat").forEach((button) => {
    button.addEventListener("click", (event) => {
      if (button.getAttribute("href") !== "#") return;
      event.preventDefault();
      messages.replaceChildren();
      input.value = "";
      status.textContent = "";
      status.className = "status-message";
      if (suggestions) suggestions.hidden = false;
      input.focus();
    });
  });
});
