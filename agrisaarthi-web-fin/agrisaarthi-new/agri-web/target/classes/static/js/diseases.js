document.addEventListener("DOMContentLoaded", () => {
  const input = document.getElementById("leafImage");
  const dropZone = document.getElementById("dropZone");
  const preview = document.getElementById("imagePreview");
  const uploadTitle = document.getElementById("uploadTitle");
  const analyseButton = document.getElementById("analyseBtn");
  const resetButton = document.getElementById("resetBtn");
  const status = document.getElementById("statusBox");
  if (!input || !dropZone || !preview || !uploadTitle || !analyseButton || !resetButton || !status) return;

  const maxFileSize = 10 * 1024 * 1024;
  const allowedTypes = new Set(["image/jpeg", "image/png", "image/webp", "image/bmp"]);
  let imageUrl = null;
  let selectedFile = null;

  function showStatus(message, isError = false) {
    status.textContent = message;
    status.classList.toggle("error-box", isError);
  }

  function clearImage(message = "Select a leaf image to begin.") {
    if (imageUrl) URL.revokeObjectURL(imageUrl);
    imageUrl = null;
    selectedFile = null;
    input.value = "";
    preview.removeAttribute("src");
    preview.style.display = "none";
    uploadTitle.textContent = "Choose an image or drop it here";
    analyseButton.disabled = true;
    showStatus(message);
  }

  function selectImage(file) {
    if (!file) return;
    if (!allowedTypes.has(file.type)) {
      clearImage("Choose a JPG, PNG, WEBP, or BMP image.");
      showStatus("Choose a JPG, PNG, WEBP, or BMP image.", true);
      return;
    }
    if (file.size > maxFileSize) {
      clearImage("The image must be 10 MB or smaller.");
      showStatus("The image must be 10 MB or smaller.", true);
      return;
    }

    if (imageUrl) URL.revokeObjectURL(imageUrl);
    selectedFile = file;
    imageUrl = URL.createObjectURL(file);
    preview.src = imageUrl;
    preview.style.display = "block";
    uploadTitle.textContent = file.name;
    analyseButton.disabled = false;
    showStatus("Image preview is ready. No image has been uploaded.");
  }

  input.addEventListener("change", () => selectImage(input.files?.[0]));

  for (const eventName of ["dragenter", "dragover"]) {
    dropZone.addEventListener(eventName, (event) => {
      event.preventDefault();
      dropZone.classList.add("drag-over");
    });
  }
  for (const eventName of ["dragleave", "drop"]) {
    dropZone.addEventListener(eventName, (event) => {
      event.preventDefault();
      dropZone.classList.remove("drag-over");
    });
  }
  dropZone.addEventListener("drop", (event) => {
    selectImage(event.dataTransfer?.files?.[0]);
  });

  analyseButton.addEventListener("click", () => {
    if (!selectedFile) {
      showStatus("Choose a leaf image first.", true);
      return;
    }
    showStatus("Image preview works, but no disease model is configured. No diagnosis was made.", true);
  });

  resetButton.addEventListener("click", () => clearImage());

  window.addEventListener("beforeunload", () => {
    if (imageUrl) URL.revokeObjectURL(imageUrl);
  }, { once: true });
});
