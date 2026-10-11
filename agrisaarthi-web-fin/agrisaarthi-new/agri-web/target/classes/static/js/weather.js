document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("weatherLocationForm");
  const message = document.getElementById("weatherMessage");
  const locationInput = form?.querySelector("#weatherLocation");
  const output = document.getElementById("weatherResult");
  if (!form || !message || !locationInput || !output) return;

  async function requestWeather(place, button) {
    button.disabled = true;
    output.hidden = true;
    message.className = "form-note";
    message.textContent = "Looking up current weather and preparing your field checklist…";

    try {
      const currentResponse = await fetch(`/api/weather/current?location=${encodeURIComponent(place)}`, {
        headers: { Accept: "application/json" },
        signal: AbortSignal.timeout(20000),
      });
      const data = await currentResponse.json();
      if (!currentResponse.ok) throw new Error(data.error || `Request failed (${currentResponse.status}).`);
      const heading = document.createElement("strong");
      heading.append(document.createTextNode(`${data.location} · `));
      const weatherDescription = document.createElement("span");
      weatherDescription.textContent = data.weatherDescription;
      heading.appendChild(weatherDescription);
      const conditions = document.createElement("p");
      conditions.className = "weather-current-summary";
      const conditionParts = [
        `${data.temperatureC}°C`,
        `Feels like ${data.apparentTemperatureC}°C`,
        `Humidity ${data.relativeHumidityPercent}%`,
        `Rain ${data.precipitationMm} mm`,
        `Wind ${data.windSpeedKmh} km/h`,
      ];
      conditionParts.forEach((part, index) => {
        if (index > 0) conditions.append(document.createTextNode(" · "));
        const segment = document.createElement("span");
        segment.textContent = part;
        conditions.appendChild(segment);
      });
      const time = document.createElement("p");
      time.className = "weather-current-time";
      time.append(document.createTextNode("Observation time: "));
      const observationTime = document.createElement("span");
      observationTime.textContent = data.observationTime;
      time.appendChild(observationTime);
      const checklistTitle = document.createElement("strong");
      checklistTitle.textContent = "Field checklist";
      const advice = document.createElement("p");
      advice.textContent = data.advisory;
      const note = document.createElement("p");
      note.className = "weather-source-note";
      note.append(document.createTextNode(`${data.note} `));
      const sourceLabel = document.createElement("span");
      sourceLabel.textContent = "Source:";
      note.append(sourceLabel, document.createTextNode(` ${data.source}.`));
      output.replaceChildren(heading, conditions, time, checklistTitle, advice, note);
      output.hidden = false;
      message.textContent = "Current weather retrieved and field checklist prepared.";
      if (window.AgriActivity) {
        try {
          window.AgriActivity.add("Current weather checklist", data.location, data.advisory);
        } catch (error) {
          message.textContent += ` Activity could not be saved in this browser: ${error.message}`;
        }
      }
    } catch (error) {
      message.className = "form-note error";
      message.textContent = `Could not prepare the checklist: ${error.message}`;
    } finally {
      button.disabled = false;
    }
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    requestWeather(locationInput.value.trim(), form.querySelector('button[type="submit"]'));
  });
});
