document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("cropForm");
  const result = document.getElementById("cropResult");
  if (!form || !result) return;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;

    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    result.className = "notice-box";
    result.textContent = "Screening the measurements…";
    const values = Object.fromEntries(new FormData(form).entries());
    for (const key of ["nitrogen", "phosphorus", "potassium", "ph", "temperature", "humidity", "rainfall"]) {
      values[key] = Number(values[key]);
    }

    try {
      const response = await fetch("/api/crops/recommend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || `Request failed (${response.status}).`);

      const heading = document.createElement("strong");
      heading.textContent = "Broad-range screening";
      const list = document.createElement("ol");
      list.className = "recommendation-list";
      data.recommendations.forEach((recommendation) => {
        const item = document.createElement("li");
        const crop = document.createElement("strong");
        const cropName = document.createElement("span");
        cropName.textContent = recommendation.crop;
        const separator = document.createTextNode(" — ");
        const fit = document.createElement("span");
        fit.textContent = recommendation.fit;
        crop.append(cropName, separator, fit);
        const reason = document.createElement("p");
        reason.textContent = recommendation.reason;
        item.append(crop, reason);
        list.appendChild(item);
      });
      const note = document.createElement("p");
      note.textContent = data.note;
      result.replaceChildren(heading, list, note);

      if (window.AgriActivity) {
        try {
          window.AgriActivity.add("Crop screening", values.location || "Farm inputs submitted", data.recommendations.map((item) => item.crop).join(", "));
        } catch (error) {
          note.textContent += ` Activity could not be saved in this browser: ${error.message}`;
        }
      }
    } catch (error) {
      result.className = "notice-box error";
      result.textContent = `Could not prepare a screening: ${error.message}`;
    } finally {
      button.disabled = false;
    }
  });

  form.addEventListener("reset", () => {
    result.className = "notice-box";
    result.textContent = "Enter your measured values to see a broad-range screening.";
  });
});
