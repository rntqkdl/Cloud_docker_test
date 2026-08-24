const express = require('express');
const mysql   = require('mysql2');
const os      = require('os');
const app     = express();

// DB를 'IP 주소'가 아니라 '컨테이너 이름(mariadb-3tier)'으로 찾는다!
const db = mysql.createConnection({
    host:     'mariadb-3tier',
    user:     'user',
    password: 'password123',
    database: 'skala'
});

app.get('/users', (req, res) => {
    db.query('SELECT id, username, email FROM users', (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({
            container: os.hostname(),
            message: "3-Tier Web-WAS-DB 연동 성공!",
            users: rows
        });
    });
});

app.get('/', (req, res) => {
    res.json({ status: "ok", host: os.hostname(), message: "WAS is running" });
});

app.listen(8080, () => console.log('WAS started on port 8080: ' + os.hostname()));
