document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const username = this.querySelector('input[type="text"]').value;
    const password = this.querySelector('input[type="password"]').value;
    if(username && password) {
        alert(`欢迎回来，${username}！`);
        this.reset();
    } else {
        alert('请填写完整信息！');
    }
});