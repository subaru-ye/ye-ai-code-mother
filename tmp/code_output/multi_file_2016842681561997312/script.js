// Handle form submission
const form = document.getElementById('loginForm');
form.addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent page reload
    const username = this.querySelector('input[type="text"]').value;
    const password = this.querySelector('input[type="password"]').value;
    // Simple validation and mock login
    if (username && password) {
        alert(`Login attempt for user: ${username}`);
        // In a real app, you would send data to a server here
    } else {
        alert('Please fill in both fields.');
    }
});