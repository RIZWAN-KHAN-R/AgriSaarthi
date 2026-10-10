
/*
 * BACKEND INTEGRATION:
 * Replace CHAT_API_URL and the request/response mapping below
 * after Friend 1 shares the actual Microservice 1 API contract.
 */

const CHAT_API_URL = ""; // Example format only: "/api/chat"

const form = document.getElementById("chatForm");
const input = document.getElementById("messageInput");
const messages = document.getElementById("chatMessages");
const status = document.getElementById("statusMessage");
const sendButton = document.getElementById("sendButton");
const suggestions = document.getElementById("suggestions");

function addMessage(text, sender, sources = []) {
    const bubble = document.createElement("div");
    bubble.className = `message ${sender}`;

    const label = document.createElement("span");
    label.className = "message-label";
    label.textContent = sender === "user" ? "YOU" : "AGRISAARTHI";

    const content = document.createElement("div");
    content.textContent = text;

    bubble.append(label, content);

    if (sender === "assistant" && Array.isArray(sources) && sources.length) {
        const sourceBox = document.createElement("div");
        sourceBox.className = "source-list";

        const heading = document.createElement("strong");
        heading.textContent = "Sources";
        sourceBox.appendChild(heading);

        sources.forEach(source => {
            if (!source || typeof source.url !== "string") return;

            try {
                const url = new URL(source.url);

                if (!["http:", "https:"].includes(url.protocol)) return;

                const link = document.createElement("a");
                link.href = url.href;
                link.target = "_blank";
                link.rel = "noopener noreferrer";
                link.textContent = source.title || url.hostname;

                sourceBox.appendChild(link);
            } catch {
                // Ignore invalid source URLs.
            }
        });

        if (sourceBox.querySelector("a")) {
            bubble.appendChild(sourceBox);
        }
    }

    messages.appendChild(bubble);
    bubble.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function sendQuestion(question) {
    const message = question.trim();

    if (!message || sendButton.disabled) return;

    if (!CHAT_API_URL) {
        status.textContent =
            "The backend API is not configured yet. Ask your teammate for the endpoint.";
        status.className = "status-message error";
        return;
    }

    addMessage(message, "user");

    input.value = "";
    suggestions.hidden = true;
    sendButton.disabled = true;
    status.textContent = "Waiting for the advisory service...";
    status.className = "status-message";

    try {
        const response = await fetch(CHAT_API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },

            // Confirm these field names with Friend 1.
            body: JSON.stringify({
                message: message,
                language: document.getElementById("languageSelect").value
            })
        });

        if (!response.ok) {
            throw new Error(`Request failed: HTTP ${response.status}`);
        }

        const data = await response.json();

        // Confirm these response fields with Friend 1.
        if (typeof data.answer !== "string") {
            throw new Error("The response does not contain an answer field.");
        }

        addMessage(data.answer, "assistant", data.sources || []);
        status.textContent = "";

    } catch (error) {
        status.textContent =
            "Could not retrieve an answer. Check the service and try again.";
        status.className = "status-message error";

        const retryButton = document.createElement("button");
        retryButton.type = "button";
        retryButton.textContent = "Retry";
        retryButton.addEventListener("click", () => sendQuestion(message));

        status.appendChild(document.createTextNode(" "));
        status.appendChild(retryButton);

    } finally {
        sendButton.disabled = false;
        input.focus();
    }
}

form.addEventListener("submit", event => {
    event.preventDefault();
    sendQuestion(input.value);
});

input.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        form.requestSubmit();
    }
});

document.querySelectorAll(".suggestion").forEach(button => {
    button.addEventListener("click", () => {
        input.value = button.dataset.question || "";
        input.focus();
    });
});

document.querySelector(".new-chat").addEventListener("click", event => {
    event.preventDefault();

    messages.replaceChildren();
    input.value = "";
    status.textContent = "";
    status.className = "status-message";
    suggestions.hidden = false;
    input.focus();
});
