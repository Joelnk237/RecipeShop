document.addEventListener("DOMContentLoaded", function () {

  if (localStorage.getItem("darkMode") === "true") {// on überprüfen si si le DarkMode est activé dans localStorage
    document.body.classList.add("dark-mode");                                // cest ajouté au body
  }


  const toggleButton = document.getElementById("darkModeToggle");
  if (toggleButton) {
    toggleButton.addEventListener("click", function () {
      document.body.classList.toggle("dark-mode");
      localStorage.setItem("darkMode", document.body.classList.contains("dark-mode"));
    });
  }
});
