
const uiTranslations = {
    en: {
        language: "Language",
        welcomeTitle: "How can we help your farm?",
        welcomeText: "Ask a farming question to receive practical guidance.",
        placeholder: "Ask your farming question...",
        send: "Send ↑",
        crop: "Crop nutrition",
        pest: "Pest management",
        water: "Water management",
        assistant: "AGRISAARTHI",
        you: "YOU",
        waiting: "Waiting for the advisory service...",
        failed: "Could not connect to the advisory service.",
        noEndpoint: "The backend API endpoint has not been configured yet."
    },
    ta: {
        language: "மொழி",
        welcomeTitle: "உங்கள் விவசாயத்திற்கு எப்படி உதவலாம்?",
        welcomeText: "விவசாயம் தொடர்பான கேள்விகளைக் கேட்டு வழிகாட்டுதலைப் பெறுங்கள்.",
        placeholder: "உங்கள் விவசாயக் கேள்வியை உள்ளிடவும்...",
        send: "அனுப்பு ↑",
        crop: "பயிர் ஊட்டச்சத்து",
        pest: "பூச்சி மேலாண்மை",
        water: "நீர் மேலாண்மை",
        assistant: "அக்ரிசார்த்தி",
        you: "நீங்கள்",
        waiting: "விவசாய சேவையின் பதிலுக்காக காத்திருக்கிறது...",
        failed: "விவசாய சேவையுடன் இணைக்க முடியவில்லை.",
        noEndpoint: "Backend API முகவரி இன்னும் அமைக்கப்படவில்லை."
    }
};

function updateLanguage(language) {
    const t = uiTranslations[language] || uiTranslations.en;

    document.documentElement.lang = language === "ta" ? "ta" : "en";

    document.getElementById("welcomeTitle").textContent = t.welcomeTitle;
    document.getElementById("welcomeText").textContent = t.welcomeText;
    document.getElementById("messageInput").placeholder = t.placeholder;
    document.getElementById("sendButton").textContent = t.send;

    document.querySelectorAll(".suggestion span").forEach((label, index) => {
        const labels = [t.crop, t.pest, t.water];
        label.textContent = labels[index] || "";
    });

    document.querySelector(".language-control > span").textContent =
        t.language;
}

document.addEventListener("DOMContentLoaded", () => {
    const selector = document.getElementById("languageSelect");

    selector.addEventListener("change", () => {
        updateLanguage(selector.value);
    });

    updateLanguage(selector.value);
});
